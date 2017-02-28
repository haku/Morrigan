package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.MediaFactoryImpl;
import com.vaguehope.morrigan.model.media.test.Tag;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.model.media.test.TestRemoteDb;
import com.vaguehope.morrigan.tasks.AsyncTaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;

public class SyncMetadataRemoteToLocalTaskTest {

	private static final Random RND = new Random(System.currentTimeMillis());

	private TestMixedMediaDb local;
	private TestRemoteDb remote;
	private SyncMetadataRemoteToLocalTask underTest;
	private AsyncTaskEventListener eventListener;

	@Before
	public void before () throws Exception {
		this.local = new TestMixedMediaDb();
		this.remote = new TestRemoteDb();
		final MediaFactory mediaFactory = new MediaFactoryImpl(null);
		this.underTest = new SyncMetadataRemoteToLocalTask(this.local, this.remote, mediaFactory);
		this.eventListener = new AsyncTaskEventListener();
		addNoise(this.local);
		addNoise(this.remote);
	}

	private void addNoise (final TestMixedMediaDb db) throws Exception {
		final int itemCount = RND.nextInt(5);
		for (int i = 0; i < itemCount; i++) {
			final IMixedMediaItem item = db.addTestTrack();
			final int tagCount = RND.nextInt(5);
			for (int x = 0; x < tagCount; x++) {
				new Tag(rndString(), MediaTagType.AUTOMATIC, rndString(), RND.nextBoolean()).addTo(db, item);
			}
		}
	}

	private String rndString () {
		return new BigInteger(RND.nextInt(32) + 32, RND).toString(32);
	}

	private void runSync () throws Throwable {
		final TaskResult res = this.underTest.run(this.eventListener);
		if (res.getErrThr() != null) throw res.getErrThr();

		if (res.getOutcome() != TaskOutcome.SUCCESS) {
			Assert.fail(res.getErrMsg());
		}

		if (this.eventListener.lastErr().length() > 0) {
			Assert.fail(this.eventListener.summary());
		}
	}

	@Test
	public void itAddsNewRemoteTag () throws Throwable {
		final Tag rTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", false);
		testTagMerge(null, rTag, rTag);
	}

	@Test
	public void itDoesNotAddNewRemoteDeletedTag () throws Throwable {
		final Tag rTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", true);
		testTagMerge(null, rTag, null);
	}

	@Test
	public void itMergesIncomingNewerDeletedTag () throws Throwable {
		final Tag lTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", false);
		final Tag rTag = lTag.withModifiedOffset(+1).withDeleted(true);
		testTagMerge(lTag, rTag, rTag);
	}

	@Test
	public void itDoesNotMergeIncomingOlderExistingTag () throws Throwable {
		final Tag lTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", true);
		final Tag rTag = lTag.withModifiedOffset(-1).withDeleted(false);
		testTagMerge(lTag, rTag, lTag);
	}

	@Test
	public void itMergesIncomingNewerDeletedTagOverNullModified () throws Throwable {
		final Tag lTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", false).withNullModified();
		final Tag rTag = lTag.withCurrentModified().withDeleted(true);
		testTagMerge(lTag, rTag, rTag);
	}

	@Test
	public void itDoesNotMergeIncomingUndatedExistingTag () throws Throwable {
		final Tag lTag = new Tag("artist1", MediaTagType.AUTOMATIC, "ARTIST", true);
		final Tag rTag = lTag.withNullModified().withDeleted(false);
		testTagMerge(lTag, rTag, lTag);
	}

	private void testTagMerge (final Tag localTag, final Tag remoteTag, final Tag expectedLocalTagAfterMerge) throws Throwable {
		final IMixedMediaItem lTrack = this.local.addTestTrack();
		final IMixedMediaItem rTrack = this.remote.addTestTrack(lTrack.getHashcode());

		if (localTag != null) localTag.addTo(this.local, lTrack);
		remoteTag.addTo(this.remote, rTrack);

		runSync();
		assertTags(this.local.getTagsIncludingDeleted(lTrack), expectedLocalTagAfterMerge);
	}

	private void assertTags (final List<MediaTag> actual, final Tag... expected) {
		final Set<String> actualSummary = new HashSet<String>();
		for (final MediaTag a : actual) {
			actualSummary.add(summariseTag(a));
		}
		final Set<String> expectedSummary = new HashSet<String>();
		for (final Tag e : expected) {
			if (e != null) expectedSummary.add(summariseTag(e));
		}
		assertEquals(expectedSummary, actualSummary);
	}

	private String summariseTag (final MediaTag t) {
		return summariseTag(t.getTag(), t.getType(), t.getClassification().getClassification(),
				t.getModified() != null ? t.getModified().getTime() : 0L, t.isDeleted());
	}

	private String summariseTag (final Tag t) {
		return summariseTag(t.getTag(), t.getType(), t.getCls(), t.getModified(), t.isDeleted());
	}

	private String summariseTag (final String tag, final MediaTagType type, final String cls, final long modified, final boolean deleted) {
		return String.format("{%s|%s|%s|%s|%s}", tag, type, cls, modified, deleted);
	}

}
