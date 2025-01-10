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
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaDb;
import com.vaguehope.morrigan.model.media.MediaList;

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
		final ListRef ref = this.undertest.allLists().iterator().next().getListRef();
		assertEquals(db, this.undertest.getList(ref));

		final ListRef filterRef = db.makeView("some-filter" + RND.nextLong()).getListRef();
		final MediaList dbWithFilter = this.undertest.getList(filterRef);
		assertEquals(filterRef, dbWithFilter.getListRef());
	}

	@Test
	public void itGetsRemote() throws Exception {
		final ListRef ref = ListRef.forRpcNode("id" + RND.nextLong(), "0");
		final MediaList external = mock(MediaList.class);
		when(external.getListRef()).thenReturn(ref);

		this.undertest.addExternalList(external);
		assertEquals(external, this.undertest.getList(ref));
	}

}
