package com.vaguehope.morrigan.rpc.client;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.InclusiveByteRange;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaRequest.Builder;
import com.vaguehope.morrigan.rpc.client.TransientContentIds.TargetAndItemIds;

public class RpcContentServlet extends HttpServlet {

	private static final long serialVersionUID = 7894980654998189201L;

	private final TransientContentIds transientContentIds;
	private final RpcClient rpcClient;

	public RpcContentServlet(final TransientContentIds transientContentIds, final RpcClient rpcClient) {
		this.transientContentIds = transientContentIds;
		this.rpcClient = rpcClient;
	}

	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		final String pathId = StringUtils.removeStart(req.getPathInfo(), "/");
		final TargetAndItemIds ids = this.transientContentIds.resolve(pathId);
		if (ids == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid ID: " + pathId);
			return;
		}

		final MediaBlockingStub stub = this.rpcClient.getMediaBlockingStub(ids.targetId);
		final HasMediaReply hasMedia = stub.hasMedia(HasMediaRequest.newBuilder().setId(ids.itemId).build());
		if (hasMedia.getExistance() != FileExistance.EXISTS) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Media not found in remote system: " + ids.itemId);
			return;
		}

		final List<MediaToadProto.Range> rpcRanges;
		final Enumeration<String> rangesHeaders = req.getHeaders(HttpHeader.RANGE.asString());
		if (rangesHeaders != null && rangesHeaders.hasMoreElements()) {
			final List<InclusiveByteRange> headerRanges = InclusiveByteRange.satisfiableRanges(rangesHeaders, hasMedia.getItem().getFileLength());

			if (headerRanges == null || headerRanges.size() == 0) {
				resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return;
			}

			rpcRanges = headerRanges.stream().map((r) -> MediaToadProto.Range.newBuilder().setFirst(r.getFirst()).setLast(r.getLast()).build())
					.collect(Collectors.toList());
		}
		else {
			rpcRanges = null;
		}

		// TODO convert StatusRuntimeException to 404, etc.
		// TODO better timeout / deadline handling.

		final Builder rpcReq = ReadMediaRequest.newBuilder().setId(ids.itemId);
		if (rpcRanges != null && rpcRanges.size() > 0) rpcReq.addAllRange(rpcRanges);

		final Iterator<ReadMediaReply> replies = stub
				.withDeadlineAfter(15, TimeUnit.MINUTES)
				.readMedia(rpcReq.build());
		final ReadMediaReply first = replies.next();

		resp.setContentType(first.getMimeType());
		resp.setContentLengthLong(first.getTotalFileLength());

		@SuppressWarnings("resource")
		final ServletOutputStream outputStream = resp.getOutputStream();
		first.getContent().writeTo(outputStream);
		while (replies.hasNext()) {
			replies.next().getContent().writeTo(outputStream);
		}
	}

}
