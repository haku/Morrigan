package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class MediaTagImplTest {

	@Test
	public void itCompairsNewer () throws Exception {
		testIsNewer(1000000L, 2000000L);
		testIsNewer(2000000L, 2000000L);
		testIsNewer(null, 2000000L);
		testIsNewer(null, null);
	}

	private void testIsNewer(final Long aMod, final Long bMod) {
		final MediaTagImpl a = new MediaTagImpl(0, null, null, null, aMod != null ? new Date(aMod) : null, false);
		final MediaTagImpl b = new MediaTagImpl(0, null, null, null, bMod != null ? new Date(bMod) : null, false);

		assertTrue(a.isNewerThan(null));
		assertTrue(b.isNewerThan(null));

		assertFalse("b should be newer than a", a.isNewerThan(b));

		if ((aMod == bMod || (aMod != null && aMod.equals(bMod)))) {
			assertFalse("a should be newer than b", b.isNewerThan(a));
		}
		else {
			assertTrue("a should not newer than a", b.isNewerThan(a));
		}
	}

}
