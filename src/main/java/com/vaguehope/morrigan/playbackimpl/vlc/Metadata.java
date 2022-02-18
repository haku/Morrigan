package com.vaguehope.morrigan.playbackimpl.vlc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.Media;
import uk.co.caprica.vlcj.media.MediaEventAdapter;
import uk.co.caprica.vlcj.media.MediaEventListener;

public class Metadata {

	public static long getDurationMilliseconds(final MediaPlayerFactory factory, final String mrl) throws InterruptedException {
		final Media media = factory.media().newMedia(mrl);
		final AtomicLong ret = new AtomicLong();
		try {
			final CountDownLatch latch = new CountDownLatch(1);
			final MediaEventListener listener = new MediaEventAdapter() {
				@Override
				public void mediaDurationChanged(final Media m, final long newDuration) {
					ret.set(newDuration);
					latch.countDown();
				}
			};

			try {
				media.events().addMediaEventListener(listener);
				if (media.parsing().parse()) {
					latch.await();
					return ret.get();
				}
				else {
					return 0;
				}
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
