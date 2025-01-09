package com.vaguehope.morrigan.rpc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ListNodeRequest;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.player.PlaybackOrder;

public class RpcMediaNodeListTest {

	private MediaBlockingStub blockingStub;
	private RpcMediaNodeList undertest;

	@Before
	public void before() throws Exception {
		final RemoteInstance ri = mock(RemoteInstance.class);
		final RpcClient rpcClient = mock(RpcClient.class);
		this.blockingStub = mock(MediaBlockingStub.class);
		final MetadataStorage metadataStorage = mock(MetadataStorage.class);
		when(ri.getLocalIdentifier()).thenReturn("target");
		when(rpcClient.getMediaBlockingStub("target")).thenReturn(this.blockingStub);
		this.undertest = new RpcMediaNodeList("nodeid", "title", ri, rpcClient, null, metadataStorage);
	}

	@Test
	public void itReportsSupportedChooseMethods() throws Exception {
		assertThat(this.undertest.getSupportedChooseMethods(), contains(PlaybackOrder.SEQUENTIAL));
	}

	@Test
	public void itChoosedTheNextTrackInSequentialMode() throws Exception {
		final MediaToadProto.MediaItem i1 = MediaToadProto.MediaItem.newBuilder().setId("item 1").build();
		final MediaToadProto.MediaItem i2 = MediaToadProto.MediaItem.newBuilder().setId("item 2").build();
		final MediaToadProto.MediaItem i3 = MediaToadProto.MediaItem.newBuilder().setId("item 3").build();
		when(this.blockingStub.listNode(any(ListNodeRequest.class))).thenReturn(ListNodeReply.newBuilder()
				.addItem(i1).addItem(i2).addItem(i3).build());

		final MediaItem prevItem = mock(MediaItem.class);
		when(prevItem.getRemoteId()).thenReturn(i2.getId());
		assertEquals(i3.getId(), this.undertest.chooseItem(PlaybackOrder.SEQUENTIAL, prevItem).getRemoteId());

		when(prevItem.getRemoteId()).thenReturn(i3.getId());
		assertEquals(i1.getId(), this.undertest.chooseItem(PlaybackOrder.SEQUENTIAL, prevItem).getRemoteId());

		assertEquals(i1.getId(), this.undertest.chooseItem(PlaybackOrder.SEQUENTIAL, null).getRemoteId());
	}

}
