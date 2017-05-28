package com.vaguehope.morrigan.transcode;

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
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaguehope.morrigan.util.ExceptionHelper;
import com.vaguehope.morrigan.util.FileHelper;
import com.vaguehope.morrigan.util.IoHelper;
import com.vaguehope.morrigan.util.Listener;
import com.vaguehope.morrigan.util.MnLogger;
import com.vaguehope.morrigan.util.StringHelper;

public class Transcoder {

	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	private static final int ERR_HISTORY_LINES = 100;
	private static final int MAX_IN_PROGRESS_TRANSCODES = 3;

	private static final MnLogger LOG = MnLogger.make(Transcoder.class);

	private final AtomicInteger inProgress = new AtomicInteger(0);
	private final ExecutorService es;
	private volatile boolean alive = true;

	public Transcoder () {
		// TODO replace with async / shutdown?
		this.es = new ThreadPoolExecutor(
				0, MAX_IN_PROGRESS_TRANSCODES * 2,
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>());
	}

	private void checkAlive () {
		if (!this.alive) throw new IllegalStateException("Transcoder instance is dead.");
	}

	public void dispose () {
		this.alive = false;
		this.es.shutdown();
	}

	public Future<?> transcodeToFileAsync (final TranscodeProfile tProfile, final Listener<Exception> onComplete) throws IOException {
		checkAlive();
		return this.es.submit(new Runnable() {
			@Override
			public void run () {
				try {
					transcodeToFile(tProfile);
					onComplete.onAnswer(null);
				}
				catch (final Exception e) {
					onComplete.onAnswer(e);
				}
			}
		});
	}

	public void transcodeToFile (final TranscodeProfile tProfile) throws IOException {
		checkAlive();
		if (tProfile == null) throw new IllegalArgumentException("tProfile can not be null.");

		final File inFile = tProfile.getItem().getFile();
		if (!inFile.exists()) {
			if (StringHelper.notBlank(tProfile.getItem().getRemoteLocation())) {
				throw new UnsupportedOperationException("TODO implement transcode of remote files.");
			}
			throw new FileNotFoundException("File not found: " + inFile.getAbsolutePath());
		}

		final File outFile = tProfile.getCacheFile();
		if (outFile.exists() && outFile.lastModified() >= inFile.lastModified()) return;

		while (true) {
			final int n = this.inProgress.get();
			if (n > MAX_IN_PROGRESS_TRANSCODES) {
				LOG.w("Rejected transcode as overloaded: {}", inFile.getAbsolutePath());
				throw new IllegalStateException("Overloaded."); // TODO Better exception class.
			}
			if (this.inProgress.compareAndSet(n, n + 1)) break;
		}

		try {
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

		final long buildStartNanos = System.nanoTime();
		final ProcessBuilder pb = makeProcess(tProfile, outputFile);
		final long runStartNanos = System.nanoTime();
		final Process p = pb.start();
		try {
			try {
				errFuture = this.es.submit(new ErrReader(p));
				final long stdOutByteCount = IoHelper.drainStream(p.getInputStream());
				final long endNanos = System.nanoTime();

				final long outputLength = outputFile.length();
				if (stdOutByteCount < 1) {
					LOG.i("Transcode complete: build={}ms run={}ms output={}b.",
							TimeUnit.NANOSECONDS.toMillis(runStartNanos - buildStartNanos),
							TimeUnit.NANOSECONDS.toMillis(endNanos - runStartNanos),
							outputLength);
				}
				else {
					LOG.w("Unexpected std out from transcode command: {} bytes.", stdOutByteCount);
				}
				if (outputLength < 1) throw new IOException("Output file length invalid: " + outputLength);
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
					LOG.i("Transcode cmd result: {}", result);
					logErr(errFuture);
					throw new IOException("Transcode failed: cmd result=" + result);
				}
			}
			catch (final IllegalThreadStateException e) {
				if (errFuture != null) logErr(errFuture);
				throw new IOException("Transcode cmd did not stop when requested.");
			}
		}
	}

	private static ProcessBuilder makeProcess (final TranscodeProfile tProfile, final File outputFile) throws IOException {
		final ProcessBuilder pb = new ProcessBuilder(tProfile.transcodeCmd(outputFile));
		LOG.i("{} cmd: {}", tProfile.getClass().getSimpleName(), pb.command());
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
				LOG.i("ffmpeg: {}", line);
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
