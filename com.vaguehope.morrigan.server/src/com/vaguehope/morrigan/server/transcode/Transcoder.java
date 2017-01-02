package com.vaguehope.morrigan.server.transcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

import com.vaguehope.morrigan.util.ExceptionHelper;
import com.vaguehope.morrigan.util.FileHelper;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.MnLogger;

public class Transcoder {

	static final String TRANSCODE_AUDIO_ONLY = "audio_only";

	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final int ERR_HISTORY_LINES = 100;
	private static final int MAX_IN_PROGRESS_TRANSCODES = 3;

	private static final MnLogger LOG = MnLogger.make(Transcoder.class);

	private final AtomicInteger inProgress = new AtomicInteger(0);
	private final ExecutorService es;

	public Transcoder () {
		this.es = Executors.newCachedThreadPool();
	}

	public void transcodeToFile (final TranscodeProfile tProfile) throws IOException {
		if (tProfile == null) throw new IllegalArgumentException("tProfile can not be null.");

		final File inFile = tProfile.getItem().getFile();
		if (!inFile.exists()) {
			throw new FileNotFoundException("File not found: " + inFile.getAbsolutePath());
		}

		while (true) {
			final int n = this.inProgress.get();
			if (n > MAX_IN_PROGRESS_TRANSCODES) {
				LOG.w("Rejected transcode as overloaded: {0}", inFile.getAbsolutePath());
				throw new IllegalStateException("Overloaded."); // TODO Better exception class.
			}
			if (this.inProgress.compareAndSet(n, n + 1)) break;
		}

		try {
			final File outFile = tProfile.getCacheFile();
			final File ftmp = File.createTempFile(outFile.getName(), tProfile.getTmpFileExt(), outFile.getParentFile());
			try {
				runTranscodeCmd(tProfile, ftmp);
				FileHelper.rename(ftmp, outFile);
			}
			finally {
				if (ftmp.exists()) ftmp.delete();
			}
		}
		catch (final Exception e) {
			LOG.w("Transcode failed.", e);
			if (e instanceof RuntimeException) throw (RuntimeException) e;
			if (e instanceof IOException) throw (IOException) e;
			throw new IOException(e.toString(), e);
		}
		finally {
			this.inProgress.decrementAndGet();
		}
	}

	private void runTranscodeCmd (final TranscodeProfile tProfile, final File outputFile) throws IOException {
		Future<List<String>> errFuture = null;
		boolean procShouldBeRunning = true;
		final Process p = makeProcess(tProfile, outputFile).start();
		try {
			try {
				errFuture = this.es.submit(new ErrReader(p));
				final long stdOutByteCount = IoHelper.drainStream(p.getInputStream());
				if (stdOutByteCount < 1) {
					LOG.i("Transcode complete, output is {0} bytes.", outputFile.length());
				}
				else {
					LOG.w("Unexpected std out from transcode command: {0} bytes.", stdOutByteCount);
				}
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

	private static ProcessBuilder makeProcess (final TranscodeProfile tProfile, final File outputFile) {
		final ProcessBuilder pb = new ProcessBuilder(tProfile.transcodeCmd(outputFile));
		LOG.i("{0} cmd: {1}", tProfile.getClass().getSimpleName(), pb.command());
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