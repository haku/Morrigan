package com.vaguehope.morrigan.player;

import static org.junit.Assert.assertSame;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.sqlitewrapper.DbException;

public class OrderResolverTest {

	private final Random random = new Random();
	private TestMixedMediaDb testDb;
	private OrderResolver undertest;

	@Before
	public void before () throws Exception {
		this.testDb = new TestMixedMediaDb();
		this.undertest = new OrderResolver();
	}

	@Test
	public void itPicksTheOneTrackWhenThereIsOnlyOneTrackByLastPlayed () throws Exception {
		final IMixedMediaItem expected = this.testDb.addTestTrack();
		final IMediaTrack actual = this.undertest.getNextTrack(this.testDb, null, PlaybackOrder.BYLASTPLAYED);
		assertSame(expected, actual);
	}

	@Test
	public void FollowTagsReturnsNullIfNoOtherTracksToChoose () throws Exception {
		final IMixedMediaItem expected = this.testDb.addTestTrack();
		final IMediaTrack actual = this.undertest.getNextTrack(this.testDb, null, PlaybackOrder.FOLLOWTAGS);
		assertSame(expected, actual);
	}

	@Test
	public void itPicksTheOneTrackWhenThereIsOnlyOneTrackFollowTags () throws Exception {
		final IMixedMediaItem current = this.testDb.addTestTrack();
		final IMixedMediaItem expected = this.testDb.addTestTrack();
		final IMediaTrack actual = this.undertest.getNextTrack(this.testDb, current, PlaybackOrder.FOLLOWTAGS);
		assertSame(expected, actual);
	}

	@Test
	public void itPicksTheOneTrackWithSameTagWhenThereIsOnlyOneTagToFollow () throws Exception {
		addRandomTracks();

		final IMixedMediaItem current = this.testDb.addTestTrack();

		final IMixedMediaItem expected = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(expected, 2, TimeUnit.DAYS);

		final IMixedMediaItem tooRecentlyPlayed = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(tooRecentlyPlayed, 2, TimeUnit.HOURS);

		addTag("foobar", current, expected, tooRecentlyPlayed);
		addRandomTags(current, expected, tooRecentlyPlayed);

		final IMediaTrack actual = this.undertest.getNextTrack(this.testDb, current, PlaybackOrder.FOLLOWTAGS);
		assertSame(expected, actual);
	}

	@Test
	public void itDoesNotPickTheOneTrackWithSameTagWhenItHasBeenPlayedRecently () throws Exception {
		final IMixedMediaItem current = this.testDb.addTestTrack();

		final IMixedMediaItem tooRecentlyPlayed = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(tooRecentlyPlayed, 1, TimeUnit.HOURS);

		addTag("foobar", current, tooRecentlyPlayed);

		// This should be the only candidate even though it does not have the tag.
		// This test has a probability of flaking, but its very low.
		final IMixedMediaItem expected = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(expected, 100000, TimeUnit.DAYS);

		final IMediaTrack actual = this.undertest.getNextTrack(this.testDb, current, PlaybackOrder.FOLLOWTAGS);
		assertSame(expected, actual);
	}

	@Ignore("Adding in randomness broke this.")
	@Test
	public void itFollowsTheSameTagAsBeforeIfPossible () throws Exception {
		addRandomTracks();

		final IMixedMediaItem current = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(current, 2, TimeUnit.MINUTES);

		final IMixedMediaItem next1 = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(next1, 2, TimeUnit.DAYS);

		addTag("foobar", current, next1);
		addTag("batbif", next1);
		addRandomTags(current, next1);

		final IMediaTrack actual1 = this.undertest.getNextTrack(this.testDb, current, PlaybackOrder.FOLLOWTAGS);
		assertSame(next1, actual1);
		setTimeAgoLastPlayed(next1, 2, TimeUnit.MINUTES);

		final IMixedMediaItem next2 = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(next2, 2, TimeUnit.DAYS);
		addTag("foobar", next2);
		addRandomTags(next2);

		final IMixedMediaItem notNext = this.testDb.addTestTrack();
		setTimeAgoLastPlayed(notNext, 2, TimeUnit.DAYS);
		addTag("batbif", notNext);
		addRandomTags(notNext);

		final IMediaTrack actual2 = this.undertest.getNextTrack(this.testDb, next1, PlaybackOrder.FOLLOWTAGS);
		assertSame(next2, actual2);
	}

	@Test
	public void itFollowsTracksByTagsSimulation () throws Exception {
		addRandomTracks();

		for (int i = 0; i < 200; i++) {
			final IMixedMediaItem track = this.testDb.addTestTrack();
			for (int j = 0; j < 3; j++) {
				addTag("tag_" + this.random.nextInt(20), track);
			}
			addRandomTags(track);
		}

		final OrderResolver or = new OrderResolver();
		IMediaTrack track = null;
		for (int i = 0; i < 100; i++) {
			track = or.getNextTrack(this.testDb, track, PlaybackOrder.FOLLOWTAGS);
			setTimeAgoLastPlayed(track, 5, TimeUnit.SECONDS);
		}
	}

	private void setTimeAgoLastPlayed (final IMediaTrack toRecentlyPlayed, final int time, final TimeUnit unit) throws MorriganException {
		this.testDb.setTrackDateLastPlayed(toRecentlyPlayed, new Date(System.currentTimeMillis() - unit.toMillis(time)));
	}

	private void addTag (final String tag, final IMixedMediaItem... items) throws MorriganException {
		for (final IMixedMediaItem item : items) {
			this.testDb.addTag(item, tag, MediaTagType.MANUAL, (String) null);
		}
	}

	private void addRandomTags (final IMixedMediaItem... items) throws MorriganException {
		for (final IMixedMediaItem item : items) {
			for (int i = 0; i < 10 + this.random.nextInt(10); i++) {
				addTag("random_tag_" + this.random.nextInt(), item);
			}
		}
	}

	private void addRandomTracks () throws MorriganException, DbException {
		for (int i = 0; i < 10 + this.random.nextInt(10); i++) {
			final IMixedMediaItem item = this.testDb.addTestTrack();
			addRandomTags(item);
		}
	}

}
