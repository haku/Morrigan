package morrigan.rpc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import mediatoad.rpc.MediaGrpc.MediaBlockingStub;
import mediatoad.rpc.MediaToadProto;
import mediatoad.rpc.MediaToadProto.ListNodeReply;
import mediatoad.rpc.MediaToadProto.ListNodeRequest;
import mediatoad.rpc.MediaToadProto.MediaTag;
import morrigan.dlna.extcd.MetadataStorage;
import morrigan.model.media.ListRef;
import morrigan.model.media.MediaItem;
import morrigan.player.PlaybackOrder;
import morrigan.rpc.client.RemoteInstance;
import morrigan.rpc.client.RpcClient;
import morrigan.rpc.client.RpcItemCache;
import morrigan.rpc.client.RpcMediaNodeList;

public class RpcMediaNodeListTest {

	private MediaBlockingStub blockingStub;
	private RpcMediaNodeList undertest;

	@Before
	public void before() throws Exception {
		final ListRef ref = ListRef.forRpcNode("target", "nodeid");
		final RemoteInstance ri = mock(RemoteInstance.class);
		final RpcClient rpcClient = mock(RpcClient.class);
		final RpcItemCache itemCache = new RpcItemCache();
		this.blockingStub = mock(MediaBlockingStub.class);
		final MetadataStorage metadataStorage = mock(MetadataStorage.class);
		when(ri.getLocalIdentifier()).thenReturn("target");
		when(rpcClient.getMediaBlockingStub("target")).thenReturn(this.blockingStub);
		this.undertest = new RpcMediaNodeList(ref, "title", ri, rpcClient, itemCache, null, metadataStorage);
	}

	@Test
	public void itImplementsEquals() throws Exception {
		final RpcMediaNodeList other = new RpcMediaNodeList(this.undertest.getListRef(), "whatever", null, null, null, null, null);
		assertEquals(other, this.undertest);
	}

	@Test
	public void itHasNodeTitle() throws Exception {
		assertEquals("title", this.undertest.getListName());
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

	@Test
	public void itAddsTag() throws Exception {
		final MediaToadProto.MediaItem i1 = MediaToadProto.MediaItem.newBuilder().setId("item 1").build();
		when(this.blockingStub.listNode(any(ListNodeRequest.class))).thenReturn(ListNodeReply.newBuilder()
				.addItem(i1).build());
		this.undertest.read();
		final MediaItem item = (MediaItem) this.undertest.get(0);

		this.undertest.addTag(item, "new tag");
		final MediaItem newItem = (MediaItem) this.undertest.get(0);
		assertEquals("new tag", newItem.getTags().get(0).getTag());
	}

	@Test
	public void itRemovesTag() throws Exception {
		final MediaToadProto.MediaItem i1 = MediaToadProto.MediaItem.newBuilder()
				.setId("item 1")
				.addTag(MediaTag.newBuilder().setTag("the tag").setModifiedMillis(1234567890L).build())
				.addTag(MediaTag.newBuilder().setTag("the other tag").build())
				.build();
		when(this.blockingStub.listNode(any(ListNodeRequest.class))).thenReturn(ListNodeReply.newBuilder()
				.addItem(i1).build());
		this.undertest.read();
		final MediaItem item = (MediaItem) this.undertest.get(0);

		this.undertest.removeTag(item, item.getTags().get(0));
		final MediaItem newItem = (MediaItem) this.undertest.get(0);
		assertThat(newItem.getTags(), hasSize(1));
		assertEquals("the other tag", newItem.getTags().get(0).getTag());
	}

	@Test
	public void itSetsItemEnabled() throws Exception {
		final MediaToadProto.MediaItem i1 = MediaToadProto.MediaItem.newBuilder().setId("item 1").build();
		when(this.blockingStub.listNode(any(ListNodeRequest.class))).thenReturn(ListNodeReply.newBuilder()
				.addItem(i1).build());
		this.undertest.read();
		final MediaItem item = (MediaItem) this.undertest.get(0);
		assertEquals(true, item.isEnabled());

		this.undertest.setItemEnabled(item, false);
		final MediaItem newItem1 = (MediaItem) this.undertest.get(0);
		assertEquals(false, newItem1.isEnabled());

		this.undertest.setItemEnabled(newItem1, true);
		final MediaItem newItem2 = (MediaItem) this.undertest.get(0);
		assertEquals(true, newItem2.isEnabled());
	}

}
