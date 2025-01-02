package com.vaguehope.morrigan.rpc.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.protobuf.ByteString;
import com.vaguehope.common.servlet.MockHttpServletRequest;
import com.vaguehope.common.servlet.MockHttpServletResponse;
import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.MediaItem;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaRequest;

public class RpcContentServletTest {

	private static final String TARGET_ID = "my-target";

	private TransientContentIds transientContentIds;
	private RpcClient rpcClient;
	private RpcContentServlet undertest;
	private MockHttpServletRequest req;
	private MockHttpServletResponse resp;
	private MediaBlockingStub stub;

	@Before
	public void before() throws Exception {
		this.transientContentIds = new TransientContentIds();
		this.rpcClient = mock(RpcClient.class);
		this.stub = mock(MediaBlockingStub.class);
		this.undertest = new RpcContentServlet(this.transientContentIds, this.rpcClient);

		this.req = new MockHttpServletRequest();
		this.resp = new MockHttpServletResponse();

		when(this.rpcClient.getMediaBlockingStub(TARGET_ID)).thenReturn(this.stub);
		when(this.stub.withDeadlineAfter(anyLong(), any(TimeUnit.class))).thenReturn(this.stub);
	}

	@Test
	public void itServesRequest() throws Exception {
		final String id = this.transientContentIds.makeId(TARGET_ID, "my-item");
		this.req.setPathInfo("/" + id);

		when(this.stub.hasMedia(HasMediaRequest.newBuilder().setId("my-item").build())).thenReturn(HasMediaReply.newBuilder()
				.setExistance(FileExistance.EXISTS)
				.build());
		when(this.stub.readMedia(ReadMediaRequest.newBuilder().setId("my-item").build())).thenReturn(List.of(
				ReadMediaReply.newBuilder()
						.setMimeType("video/whatever")
						.setTotalFileLength(10)
						.setContent(ByteString.copyFrom("0123456789", StandardCharsets.UTF_8))
						.build())
				.iterator());

		this.undertest.service(this.req, this.resp);
		assertEquals(200, this.resp.getStatus());
		assertEquals("0123456789", this.resp.getOutputAsString());
	}

	@Test
	public void itServesRangeRequest() throws Exception {
		final String id = this.transientContentIds.makeId(TARGET_ID, "my-item");
		this.req.setPathInfo("/" + id);
		this.req.addHeader("Range", "bytes=3-7");

		when(this.stub.hasMedia(HasMediaRequest.newBuilder().setId("my-item").build())).thenReturn(HasMediaReply.newBuilder()
				.setExistance(FileExistance.EXISTS)
				.setItem(MediaItem.newBuilder().setFileLength(10).build())
				.build());
		when(this.stub.readMedia(ReadMediaRequest.newBuilder()
				.setId("my-item")
				.addRange(MediaToadProto.Range.newBuilder().setFirst(3).setLast(7).build())
				.build())).thenReturn(List.of(
						ReadMediaReply.newBuilder()
								.setMimeType("video/whatever")
								.setTotalFileLength(5)
								.setContent(ByteString.copyFrom("34567", StandardCharsets.UTF_8))
								.build())
						.iterator());

		this.undertest.service(this.req, this.resp);
		assertEquals(200, this.resp.getStatus());
		assertEquals("34567", this.resp.getOutputAsString());
	}

}