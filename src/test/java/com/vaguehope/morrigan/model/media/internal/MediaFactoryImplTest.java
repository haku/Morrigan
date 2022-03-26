package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaDb;
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
		final ILocalMixedMediaDb db = this.undertest.createLocalMixedMediaDb("test" + RND.nextLong());
		final MediaListReference ref = this.undertest.getAllLocalMixedMediaDbs().iterator().next();
		assertEquals(db, this.undertest.getMixedMediaDbByMid(ref.getMid(), null));
		assertEquals(db, this.undertest.getMixedMediaDbByRef(ref));

		final String filter = "some-filter" + RND.nextLong();
		final ILocalMixedMediaDb dbWithFilter = this.undertest.getLocalMixedMediaDb(db.getDbPath(), filter);
		assertEquals(dbWithFilter, this.undertest.getMixedMediaDbByMid(ref.getMid(), filter));
		assertEquals(dbWithFilter, this.undertest.getMixedMediaDbByRef(ref, filter));
	}

	@Test
	public void itGetsRemoteByMid() throws Exception {
		final IMixedMediaDb external = mock(IMixedMediaDb.class);
		final String id = "id" + RND.nextLong();
		when(external.getListId()).thenReturn(id);
		when(external.getListName()).thenReturn("name for " + id);
		this.undertest.addExternalDb(external);

		final MediaListReference ref = this.undertest.getExternalDbs().iterator().next();
		assertEquals(external, this.undertest.getMixedMediaDbByMid(ref.getMid(), null));
		assertEquals(external, this.undertest.getMixedMediaDbByRef(ref));
	}

}
