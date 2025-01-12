package com.vaguehope.morrigan.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.MediaList;
import com.vaguehope.morrigan.transcode.Transcode;

public class PlayerStateStorageTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private MediaFactory mediaFactory;
	private PlayerStateStorage undertest;

	private ListRef listRef0;
	private MediaList mediaList0;
	private ListRef listRef1;
	private MediaList mediaList1;
	private PlayItem playItem0;
	private PlayItem playItem1;

	private Player player;
	private PlayerQueue queue;


	@Before
	public void before() throws Exception {
		this.mediaFactory = mock(MediaFactory.class);
		final Config config = new Config(this.tmp.getRoot());
		this.undertest = new PlayerStateStorage(this.mediaFactory, null, config);

		this.listRef0 = ListRef.forLocal("some-db");
		this.mediaList0 = mock(MediaList.class);
		when(this.mediaList0.getListRef()).thenReturn(this.listRef0);
		when(this.mediaList0.getListName()).thenReturn("list0");
		when(this.mediaFactory.getList(this.listRef0)).thenReturn(this.mediaList0);

		this.listRef1 = ListRef.forRpcNode("some-rpc-list", "some-rpc-node");
		this.mediaList1 = mock(MediaList.class);
		when(this.mediaList1.getListRef()).thenReturn(this.listRef1);
		when(this.mediaList1.getListName()).thenReturn("list1");

		final MediaItem mediaItem0 = mock(MediaItem.class);
		when(mediaItem0.getFilepath()).thenReturn("some-filepath");
		when(mediaItem0.getMd5()).thenReturn(BigInteger.valueOf(0x1234567890abcdefL));
		when(mediaItem0.getTitle()).thenReturn("item0");
		this.playItem0 = PlayItem.makeReady(this.mediaList0, mediaItem0);
		when(this.mediaList0.getByFile("some-filepath")).thenReturn(mediaItem0);

		final MediaItem mediaItem1 = mock(MediaItem.class);
		when(mediaItem1.getRemoteId()).thenReturn("some-remote-id");
		when(mediaItem1.getMd5()).thenReturn(BigInteger.valueOf(0xabcdef123456789L));
		when(mediaItem1.getTitle()).thenReturn("item1");
		when(mediaItem1.isPlayable()).thenReturn(true);
		this.playItem1 = PlayItem.makeReady(this.mediaList1, mediaItem1);

		this.player = mock(Player.class);
		this.queue = new DefaultPlayerQueue();
		when(this.player.getId()).thenReturn("playerid");
		when(this.player.getPlaybackOrder()).thenReturn(PlaybackOrder.BYLASTPLAYED);
		when(this.player.getTranscode()).thenReturn(Transcode.COMMON_AUDIO_ONLY);
		when(this.player.getQueue()).thenReturn(this.queue);
		when(this.player.getCurrentList()).thenReturn(this.mediaList0);
		when(this.player.getCurrentItem()).thenReturn(this.playItem0);

		this.queue.addToQueue(PlayItem.makeAction(PlayItemType.STOP));
		this.queue.addToQueue(this.playItem1);
		this.queue.addToQueue(PlayItem.makeUnresolved(this.listRef1, "un path", "un id", BigInteger.ZERO, "un title"));
	}

	@Test
	public void itDoesEmptyList() throws Exception {
		this.undertest.writeState(this.player);

		final File f = new File(this.tmp.getRoot(), "playerstate/playerid");
		assertEquals("{\"playbackOrder\":\"BYLASTPLAYED\",\"transcode\":\"COMMON_AUDIO_ONLY\",\"position\":0,"
				+ "\"listRef\":\"LOCAL:l\\u003dsome-db\","
				+ "\"item\":{"
				+ "\"listRef\":\"LOCAL:l\\u003dsome-db\",\"filepath\":\"some-filepath\",\"md5\":\"1234567890abcdef\",\"title\":\"list0/item0\""
				+ "},"
				+ "\"queue\":["
				+ "{\"listRef\":\"STOP\"},"
				+ "{\"listRef\":\"RPC:l\\u003dsome-rpc-list\\u0026n\\u003dsome-rpc-node\",\"remoteId\":\"some-remote-id\",\"md5\":\"abcdef123456789\",\"title\":\"list1/item1\"},"
				+ "{\"listRef\":\"RPC:l\\u003dsome-rpc-list\\u0026n\\u003dsome-rpc-node\",\"filepath\":\"un path\",\"remoteId\":\"un id\",\"md5\":\"0\",\"title\":\"Unresolved: un title\"}"
				+ "]}",
				FileUtils.readFileToString(f, StandardCharsets.UTF_8));

		final Player newPlayer = mock(Player.class);
		when(newPlayer.getId()).thenReturn("playerid");
		when(newPlayer.getQueue()).thenReturn(new DefaultPlayerQueue());
		this.undertest.readState(newPlayer);

		verify(newPlayer).setPlaybackOrder(PlaybackOrder.BYLASTPLAYED);
		verify(newPlayer).setTranscode(Transcode.COMMON_AUDIO_ONLY);
		verify(newPlayer).setCurrentList(this.mediaList0);
		verify(newPlayer).setCurrentItem(this.playItem0);

		final PlayItem q0 = PlayItem.makeAction(PlayItemType.STOP);
		q0.setId(0);
		final PlayItem q1 = PlayItem.makeUnresolved(this.listRef1, null, "some-remote-id", BigInteger.valueOf(0xabcdef123456789L), "list1/item1");
		q1.setId(1);
		final PlayItem q2 = PlayItem.makeUnresolved(this.listRef1, "un path", "un id", BigInteger.ZERO, "Unresolved: un title");
		q2.setId(2);
		assertThat(newPlayer.getQueue().getQueueList(), contains(q0, q1, q2));
	}

}
