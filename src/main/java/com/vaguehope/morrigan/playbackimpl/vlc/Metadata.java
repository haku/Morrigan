package com.vaguehope.morrigan.playbackimpl.vlc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaEventListener;
import uk.co.caprica.vlcj.media.MediaParsedStatus;

class Metadata {

	private static final int READ_TIMEOUT_SECONDS = 5;

	public static long getDurationMilliseconds(final MediaPlayerFactory factory, final String mrl) throws InterruptedException {
		final Media media = factory.media().newMedia(mrl);
		final AtomicLong ret = new AtomicLong(0L);
		try {
			final CountDownLatch latch = new CountDownLatch(1);
			final MediaEventListener listener = new MediaEventAdapter() {
				@Override
				public void mediaDurationChanged(final Media m, final long newDuration) {
					ret.set(newDuration);
					latch.countDown();
				}

				@Override
				public void mediaParsedChanged(final Media m, final MediaParsedStatus newStatus) {
					switch (newStatus) {
					case DONE:
					case FAILED:
					case SKIPPED:
					case TIMEOUT:
						latch.countDown();
						break;
					default:
						break;
					}
				}
			};

			try {
				media.events().addMediaEventListener(listener);
				if (media.parsing().parse()) {
					latch.await(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				}
				return ret.get();
			}
			finally {
				media.events().removeMediaEventListener(listener);
			}
		}
		finally {
			media.release();
		}
	}

}
