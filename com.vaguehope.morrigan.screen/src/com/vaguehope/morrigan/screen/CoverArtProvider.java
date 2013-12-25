package com.vaguehope.morrigan.screen;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import com.vaguehope.morrigan.model.media.IMediaTrack;

public class CoverArtProvider {

	private final Display display;
	private final ExecutorService executorService;

	private volatile boolean alive = true;
	private final AtomicReference<TrackImage> currentImage = new AtomicReference<TrackImage>();

	public CoverArtProvider (final Display display, final ExecutorService executorService) {
		this.display = display;
		this.executorService = executorService;
	}

	/**
	 * If the image for this item is ready it will be returned. If not, null
	 * will be returned and the callback invoked when its ready. <br>
	 * This should only be called on the UI thread.
	 */
	public Image getImage (final IMediaTrack track, final Runnable onImagedLoaded) {
		if (track == null) return null;
		checkAlive();

		final TrackImage ti = this.currentImage.get();
		if (ti != null && ti.forTrack(track)) return ti.image;

		this.executorService.submit(new ImageLoader(track, this.currentImage, onImagedLoaded, this.display));
		return null;
	}

	public void dispose () {
		checkAlive();
		this.alive = false;

		final TrackImage old = this.currentImage.getAndSet(null);
		if (old != null) old.dispose();
	}

	private void checkAlive () {
		if (!this.alive) throw new IllegalStateException("CoverArtProvider has been disposed.");
	}

	private static class TrackImage {
		final IMediaTrack track;
		final Image image;

		public TrackImage (final IMediaTrack track, final Image image) {
			this.track = track;
			this.image = image;
		}

		public boolean forTrack(final IMediaTrack t) {
			return (this.track.equals(t) && !this.image.isDisposed());
		}

		public void dispose () {
			if (this.image != null) this.image.dispose();
		}

	}

	private static class ImageLoader implements Runnable {

		private final IMediaTrack track;
		private final AtomicReference<TrackImage> currentImage;
		private final Runnable onImagedLoaded;
		private final Display display;

		public ImageLoader (final IMediaTrack track, final AtomicReference<TrackImage> currentImage, final Runnable onImagedLoaded, final Display display) {
			this.track = track;
			this.currentImage = currentImage;
			this.onImagedLoaded = onImagedLoaded;
			this.display = display;
		}

		@Override
		public void run () {
			try {
				runUnsafe();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void runUnsafe () {
			final TrackImage currentTi = this.currentImage.get();
			if(currentTi != null && currentTi.forTrack(this.track)) {
				this.onImagedLoaded.run();
				return;
			}

			final File file = this.track.findCoverArt();
			final TrackImage freshTi;
			if (file != null) {
				System.err.println("Loading art for " + this.track + ": " + file + " ...");
				final Image image = new Image(this.display, file.getAbsolutePath());
				freshTi = new TrackImage(this.track, image);
			}
			else {
				freshTi = null;
			}

			final TrackImage oldTi = this.currentImage.getAndSet(freshTi);
			this.onImagedLoaded.run();
			if(oldTi != null) oldTi.dispose();
		}

	}

}
