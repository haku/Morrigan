package morrigan.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;

import morrigan.model.media.ListRef;
import morrigan.model.media.MediaFactory;
import morrigan.model.media.MediaItem;
import morrigan.model.media.MediaList;

public class PlayItemTest {

	private ListRef listRef;
	private MediaList list;
	private MediaFactory mf;
	private MediaItem item;

	@Before
	public void before() throws Exception {
		this.listRef = ListRef.forLocalSearch("my-db", "t=foo");
		this.list = mock(MediaList.class);

		this.mf = mock(MediaFactory.class);
		when(this.mf.getList(this.listRef)).thenReturn(this.list);

		this.item = mock(MediaItem.class);
		when(this.item.getFilepath()).thenReturn("/path/file.mp3");
		when(this.item.getMd5()).thenReturn(BigInteger.valueOf(1234567890L));
	}

	@Test
	public void itResolvesListOnly() throws Exception {
		final PlayItem unresolved = PlayItem.makeUnresolved(this.listRef, null, null, null, "my-db{t=foo}");
		final PlayItem resolved = unresolved.makeReady(this.mf);

		assertEquals(this.list, resolved.getList());
		assertTrue(resolved.isReady());
		assertFalse(resolved.hasItem());
	}

	@Test
	public void itResolvesListWithItem() throws Exception {
		final PlayItem unresolved = PlayItem.makeUnresolved(this.listRef, this.item.getFilepath(), null, null, "file.mp3");
		when(this.list.getByFile(this.item.getFilepath())).thenReturn(this.item);
		final PlayItem resolved = unresolved.makeReady(this.mf);

		assertEquals(this.list, resolved.getList());
		assertEquals(this.item, resolved.getItem());
		assertTrue(resolved.isReady());
	}

	@Test
	public void itResolvesListWithItemByMd5() throws Exception {
		final PlayItem unresolved = PlayItem.makeUnresolved(this.listRef, "/some/lost/file.mp3", null, this.item.getMd5(), "file.mp3");
		when(this.list.canGetByMd5()).thenReturn(true);
		when(this.list.getByMd5(this.item.getMd5())).thenReturn(this.item);
		final PlayItem resolved = unresolved.makeReady(this.mf);

		assertEquals(this.list, resolved.getList());
		assertEquals(this.item, resolved.getItem());
		assertTrue(resolved.isReady());
	}

}
