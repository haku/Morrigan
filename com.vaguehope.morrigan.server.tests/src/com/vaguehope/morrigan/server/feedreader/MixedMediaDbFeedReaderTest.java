package com.vaguehope.morrigan.server.feedreader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.test.Tag;
import com.vaguehope.morrigan.model.media.test.TestRemoteDb;
import com.vaguehope.morrigan.tasks.AsyncTaskEventListener;
import com.vaguehope.morrigan.util.IoHelper;

public class MixedMediaDbFeedReaderTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private IRemoteMixedMediaDb remote;
	private AsyncTaskEventListener eventListener;

	@Before
	public void before () throws Exception {
		this.remote = new TestRemoteDb();
		this.eventListener = new AsyncTaskEventListener();
	}

	@Test
	public void itParsesASingleItemFeed () throws Exception {
		final File file = this.tmp.newFile("input.xml");
		IoHelper.write(getClass().getResourceAsStream("/mlist-single-item.xml"), file);
		this.remote.setUri(new URI("file://" + file.getAbsolutePath()));

		MixedMediaDbFeedReader.read(this.remote, this.eventListener);

		final List<IMixedMediaItem> items = this.remote.getAllDbEntries();
		final IMixedMediaItem item = items.get(0);
		assertEquals("Some Artist - Some Track.ogg", item.getTitle());
//		assertEquals(7575894L, item.getFileSize());
		assertEquals("/home/media/music/Some Artist - Some Track.ogg", item.getFilepath());
		assertEquals("Fri Apr 25 12:00:06 BST 2008", item.getDateAdded().toString());
		assertEquals("Sat Jan 19 18:19:36 GMT 2008", item.getDateLastModified().toString());
		assertEquals("Sat Oct 08 08:12:45 BST 2016", item.getDateLastPlayed().toString());
		assertEquals("892fde64e2af9ac6390e35b2af05326b", item.getHashcode().toString(16));
		assertEquals(true, item.isEnabled());
		assertEquals(false, item.isMissing());
		assertEquals(49L, item.getStartCount());
		assertEquals(36L, item.getEndCount());
		assertEquals(337, item.getDuration());
		assertEquals(IMixedMediaItem.MediaType.TRACK, item.getMediaType());

		Tag.assertTags(this.remote.getTagsIncludingDeleted(item),
				new Tag("My Tag", MediaTagType.MANUAL, null, null, false),
				new Tag("My Other Tag", MediaTagType.MANUAL, null, 1488299277000L, false),
				new Tag("My Deleted Tag", MediaTagType.MANUAL, null, 1488299156000L, true),
				new Tag("Some Album", MediaTagType.AUTOMATIC, "ALBUM", 1486169231000L, false),
				new Tag("Some Artist", MediaTagType.AUTOMATIC, "ARTIST", 1486169231000L, false),
				new Tag("Lyrics line 1\nLyrics line 2\nLyrics line 3", MediaTagType.AUTOMATIC, "LYRICS", 1486169231000L, false));
	}

}
