package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaListReference;

public class MediaFactoryImplTest {

	private static final Random RND = new Random(System.currentTimeMillis());
	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private MediaFactoryImpl undertest;

	@Before
	public void before() throws Exception {
		final Config config = new Config(this.tmp.getRoot());
		this.undertest = new MediaFactoryImpl(config, null);
	}

	@Test
	public void itGetsLocalByMid() throws Exception {
		final MediaDb db = this.undertest.createLocalMixedMediaDb("test" + RND.nextLong());
		final MediaListReference ref = this.undertest.getAllLocalMixedMediaDbs().iterator().next();
		assertEquals(db, this.undertest.getMediaListByMid(ref.getMid(), null));
		assertEquals(db, this.undertest.getMediaListByRef(ref));

		final String filter = "some-filter" + RND.nextLong();
		final MediaDb dbWithFilter = this.undertest.getLocalMixedMediaDb(db.getDbPath(), filter);
		assertEquals(dbWithFilter, this.undertest.getMediaListByMid(ref.getMid(), filter));
		assertEquals(dbWithFilter, this.undertest.getMediaListByRef(ref, filter));
	}

	@Test
	public void itGetsLocalWithPathThatHasStuffOnTheEnd() throws Exception {
		final String name = "test" + RND.nextLong();
		final MediaDb db = this.undertest.createLocalMixedMediaDb(name);
		assertEquals(db, this.undertest.getMediaListByMid("LOCALMMDB/" + name + ".local.db3/query/*?&column=DATE_LAST_PLAYED&order=desc&_=1736334474459", null));
		assertEquals(db, this.undertest.getMediaListByMid("LOCALMMDB:" + name + ".local.db3/query/*?&column=DATE_LAST_PLAYED&order=desc&_=1736334474459", null));
	}

	@Test
	public void itGetsRemoteByMid() throws Exception {
		final MediaDb external = mock(MediaDb.class);
		final String id = "id" + RND.nextLong();
		when(external.getListId()).thenReturn(id);
		when(external.getListName()).thenReturn("name for " + id);
		this.undertest.addExternalList(external);

		final MediaListReference ref = this.undertest.getExternalLists().iterator().next();
		assertEquals(external, this.undertest.getMediaListByMid(ref.getMid(), null));
		assertEquals(external, this.undertest.getMediaListByRef(ref));
	}

}
