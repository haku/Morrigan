package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
	public void itGetsRemoteRootAndSubNode() throws Exception {
		final ListRef rootRef = ListRef.forRpcNode("id" + RND.nextLong(), "0");
		final MediaList rootList = mock(MediaList.class);
		when(rootList.getListRef()).thenReturn(rootRef);

		this.undertest.addExternalList(rootList);
		assertEquals(rootList, this.undertest.getList(rootRef));

		final MediaList nodeList = mock(MediaList.class);
		when(rootList.resolveRef(any(ListRef.class))).thenCallRealMethod();
		when(rootList.makeNode("some-node-id", null)).thenReturn(nodeList);
		when(rootList.hasNodes()).thenReturn(true);
		final ListRef nodeRef = ListRef.forRpcNode(rootRef.getListId(), "some-node-id");
		assertEquals(nodeList, this.undertest.getList(nodeRef));

		final MediaList searchList = mock(MediaList.class);
		when(rootList.makeView("some search")).thenReturn(searchList);
		when(rootList.canMakeView()).thenReturn(true);
		final ListRef searchRef = ListRef.forRpcSearch(rootRef.getListId(), "some search");
		assertEquals(searchList, this.undertest.getList(searchRef));
	}

}
