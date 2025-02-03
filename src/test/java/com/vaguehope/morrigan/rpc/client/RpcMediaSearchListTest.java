package com.vaguehope.morrigan.rpc.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.AboutReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.AboutRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SearchRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.SortField;
import com.vaguehope.morrigan.dlna.extcd.MetadataStorage;
import com.vaguehope.morrigan.model.media.ListRef;
import com.vaguehope.morrigan.model.media.SortColumn;

public class RpcMediaSearchListTest {

	private MediaBlockingStub blockingStub;
	private RpcMediaSearchList undertest;

	@Before
	public void before() throws Exception {
		final ListRef ref = ListRef.forRpcSearch("target", "my search");
		final RemoteInstance ri = mock(RemoteInstance.class);
		final RpcClient rpcClient = mock(RpcClient.class);
		final RpcItemCache itemCache = new RpcItemCache();
		final RpcContentServlet contentServer = mock(RpcContentServlet.class);
		final MetadataStorage metadataStorage = mock(MetadataStorage.class);
		this.blockingStub = mock(MediaBlockingStub.class);
		when(ri.getLocalIdentifier()).thenReturn("target");
		when(rpcClient.getMediaBlockingStub("target")).thenReturn(this.blockingStub);
		this.undertest = new RpcMediaSearchList(ref, ri, rpcClient, itemCache, contentServer, metadataStorage);
	}

	@Test
	public void itImplementsEquals() throws Exception {
		final RpcMediaSearchList other = new RpcMediaSearchList(this.undertest.getListRef(), null, null, null, null, null);
		assertEquals(other, this.undertest);
	}

	@Test
	public void itCachesSupportedSortOrders() throws Exception {
		when(this.blockingStub.about(any(AboutRequest.class))).thenReturn(AboutReply.newBuilder()
				.addSupportedSortField(SortField.FILE_PATH)
				.addSupportedSortField(SortField.DATE_ADDED)
				.build());
		final List<SortColumn> actual = this.undertest.getSuportedSortColumns();
		assertThat(actual, contains(SortColumn.FILE_PATH, SortColumn.DATE_ADDED));

		this.undertest.getSuportedSortColumns();
		verify(this.blockingStub, times(1)).about(any(AboutRequest.class));
	}

	@Test
	public void itDefaultsToUnspecifiedSortOrder() throws Exception {
		when(this.blockingStub.about(any(AboutRequest.class))).thenReturn(AboutReply.newBuilder().build());
		when(this.blockingStub.search(any(SearchRequest.class))).thenReturn(SearchReply.newBuilder().build());
		this.undertest.search(null, "my search", 100);
		verify(this.blockingStub).search(SearchRequest.newBuilder()
				.setQuery("(type=audio OR type=video) AND ( my search )")
				.setMaxResults(100)
				.build());

		final List<SortColumn> actual = this.undertest.getSuportedSortColumns();
		assertThat(actual, contains(SortColumn.UNSPECIFIED));
	}

}
