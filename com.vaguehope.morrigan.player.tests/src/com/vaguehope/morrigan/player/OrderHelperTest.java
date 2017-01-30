package com.vaguehope.morrigan.player;

import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.player.OrderHelper.PlaybackOrder;

public class OrderHelperTest {

	private TestMixedMediaDb testDb;

	@Before
	public void before () throws Exception {
		this.testDb = new TestMixedMediaDb();
	}

	@Test
	public void itPicksTheOneTrackWhenThereIsOnlyOneTrack () throws Exception {
		final IMixedMediaItem expected = this.testDb.addTestTrack();
		final IMediaTrack actual = OrderHelper.getNextTrack(this.testDb, null, PlaybackOrder.BYLASTPLAYED);
		assertSame(expected, actual);
	}

}
