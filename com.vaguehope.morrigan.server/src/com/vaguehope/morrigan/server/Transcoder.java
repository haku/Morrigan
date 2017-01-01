package com.vaguehope.morrigan.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;

import com.vaguehope.morrigan.model.media.IMediaItem;
import com.vaguehope.morrigan.util.ExceptionHelper;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.MimeType;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

public class Transcoder {

	private static final String TRANSCODE_AUDIO_ONLY = "audio_only";

	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final int ERR_HISTORY_LINES = 100;
	private static final int MAX_IN_PROGRESS_TRANSCODES = 3;

	private static final MnLogger LOG = MnLogger.make(Transcoder.class);

	private final AtomicInteger inProgress = new AtomicInteger(0);
	private final ExecutorService es;

	public Transcoder () {
		this.es = Executors.newCachedThreadPool();
	}

	public static boolean transcodeRequired (final IMediaItem item, final String transcode) {
		if (transcode == null) return false;

		if (TRANSCODE_AUDIO_ONLY.equals(transcode)) {
			return StringHelper.startsWithIgnoreCase(item.getMimeType(), "video");
		}
		else {
			throw new IllegalArgumentException("Unsupported transcode: " + transcode);
		}
	}

	public static String transcodedTitle (final IMediaItem item, final String transcode) {
		if (TRANSCODE_AUDIO_ONLY.equals(transcode)) {
			return item.getTitle() + "." + MimeType.MP3.getExt();
		}
		else {
			throw new IllegalArgumentException("Unsupported transcode: " + transcode);
		}
	}

	public void transcode (final File file, final String name, final HttpServletResponse resp) throws IOException {
		if (!file.exists()) {
			resp.sendError(HttpStatus.BAD_REQUEST_400, "File not found: " + file.getAbsolutePath());
			return;
		}

		while (true) {
			final int n = this.inProgress.get();
			if (n > MAX_IN_PROGRESS_TRANSCODES) {
				resp.sendError(HttpStatus.SERVICE_UNAVAILABLE_503, "Overloaded.");
				LOG.w("Rejected transcode as overloaded: {0}", file.getAbsolutePath());
				return;
			}
			if (this.inProgress.compareAndSet(n, n + 1)) break;
		}
		try {
			runTranscode(file, name, resp);
		}
		catch (final Exception e) {
			LOG.w("Transcode failed.", e);
			if (e instanceof IOException) throw (IOException) e;
			throw new IOException(e.toString(), e);
		}
		finally {
			this.inProgress.decrementAndGet();
		}
	}

	private void runTranscode (final File file, final String name, final HttpServletResponse resp) throws IOException {
		Future<List<String>> errFuture = null;
		boolean procShouldBeRunning = true;
		final Process p = makeProcess(file).start();
		try {
			try {
				errFuture = this.es.submit(new ErrReader(p));

				resp.setContentType(MimeType.MP3.getMimeType());
				resp.addHeader("Content-Description", "File Transfer");
				resp.addHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
				resp.addHeader("Content-Transfer-Encoding", "binary");
				resp.setDateHeader("Last-Modified", file.lastModified());
				resp.addHeader("Pragma", "public");

				final long bytesSend = IoHelper.copy(p.getInputStream(), resp.getOutputStream());
				LOG.i("Transcode complete, served {0} bytes.", bytesSend);
			}
			catch (final IOException e) {
				if (ExceptionHelper.causedBy(e, IOException.class, "Connection reset by peer")) {
					procShouldBeRunning = false;
				}
				else {
					throw e;
				}
			}
		}
		finally {
			p.destroy();
			try {
				final int result = waitFor(p, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				if (procShouldBeRunning && result != 0 && errFuture != null) {
					LOG.i("ffmpeg result: {0}", result);
					logErr(errFuture);
				}
			}
			catch (final IllegalThreadStateException e) {
				LOG.w("ffmpeg did not stop when requested.");
				if (errFuture != null) logErr(errFuture);
			}
		}
	}

	private static ProcessBuilder makeProcess (final File file) {
		final ProcessBuilder pb = new ProcessBuilder(
				"ffmpeg",
				"-hide_banner",
				"-nostats",
				// "-seekable", "1", // Only needed when input is HTTP and not a local file.
				"-fflags", "+genpts",
				"-threads", "0",
				"-i", file.getAbsolutePath(),
				"-vn",
				"-b:a", "320k",
				"-f", "mp3",
				"-");
		LOG.i("cmd: {0}", pb.command());
		return pb;
	}

	private static int waitFor (final Process p, final int timeout, final TimeUnit unit) {
		final long startNanos = System.nanoTime();
		while (true) {
			try {
				return p.exitValue();
			}
			catch (final IllegalThreadStateException e) {
				if (TimeUnit.NANOSECONDS.convert(timeout, unit) < System.nanoTime() - startNanos) {
					throw e; // Timed out.
				}
				try {
					Thread.sleep(1000L);
				}
				catch (final InterruptedException e1) {/* Ignore. */}
			}
		}
	}


	private static void logErr (final Future<List<String>> errFuture) {
		try {
			for (final String line : errFuture.get()) {
				LOG.i("ffmpeg: {0}", line);
			}
		}
		catch (InterruptedException e) {
			LOG.e("Err reader failed.", e);
		}
		catch (ExecutionException e) {
			LOG.e("Err reader failed.", e);
		}
	}

	private static class ErrReader implements Callable<List<String>> {

		private final Process p;

		public ErrReader (final Process p) {
			this.p = p;
		}

		@Override
		public List<String> call () throws Exception {
			final LinkedList<String> err = new LinkedList<String>();
			try {
				readErr(err);
			}
			catch (final Exception e) {
				if (!ignoreException(e)) LOG.e("Err reader died.", e);
			}
			return err;
		}

		private static boolean ignoreException (final Exception e) {
			return e instanceof IOException && "Stream closed".equals(e.getMessage());
		}

		private void readErr (final LinkedList<String> err) throws IOException {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(this.p.getErrorStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				err.add(line);
				if (err.size() > ERR_HISTORY_LINES) err.poll();
			}
		}
	}

}
