package com.vaguehope.morrigan.rpc.client;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.InclusiveByteRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.dlnatoad.rpc.MediaGrpc.MediaBlockingStub;
import com.vaguehope.dlnatoad.rpc.MediaToadProto;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.FileExistance;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.HasMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaReply;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaRequest;
import com.vaguehope.dlnatoad.rpc.MediaToadProto.ReadMediaRequest.Builder;
import com.vaguehope.morrigan.player.contentproxy.ContentServer;

public class RpcContentServlet implements ContentServer {

	private static final Logger LOG = LoggerFactory.getLogger(RpcContentServlet.class);

	private final RpcClient rpcClient;

	public RpcContentServlet(final RpcClient rpcClient) {
		this.rpcClient = rpcClient;
	}

	@Override
	public void doGet(final HttpServletRequest req, final HttpServletResponse resp, final String listId, final String itemId) throws IOException {
		final MediaBlockingStub stub = this.rpcClient.getMediaBlockingStub(listId);
		final HasMediaReply hasMedia = stub.hasMedia(HasMediaRequest.newBuilder().setId(itemId).build());
		if (hasMedia.getExistence() != FileExistance.EXISTS) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Media not found in remote system: " + itemId);
			return;
		}

		final List<InclusiveByteRange> httpRanges;
		final List<MediaToadProto.Range> rpcRanges;
		final Enumeration<String> rangesHeaders = req.getHeaders(HttpHeader.RANGE.asString());
		if (rangesHeaders != null && rangesHeaders.hasMoreElements()) {
			httpRanges = InclusiveByteRange.satisfiableRanges(rangesHeaders, hasMedia.getItem().getFileLength());

			if (httpRanges == null || httpRanges.size() == 0) {
				resp.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return;
			}

			if (httpRanges.size() > 1) {
				// TODO implement https://www.rfc-editor.org/rfc/rfc2616#section-19.2
				resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Multiple range headers not supported.");
				LOG.error("Multiple values in Range header not implemented: {}", httpRanges);
				return;
			}

			rpcRanges = httpRanges.stream().map((r) -> MediaToadProto.Range.newBuilder().setFirst(r.getFirst()).setLast(r.getLast()).build())
					.collect(Collectors.toList());
		}
		else {
			httpRanges = null;
			rpcRanges = null;
		}

		// TODO convert StatusRuntimeException to 404, etc.
		// TODO better timeout / deadline handling.

		final Builder rpcReq = ReadMediaRequest.newBuilder().setId(itemId);
		if (rpcRanges != null && rpcRanges.size() > 0) rpcReq.addAllRange(rpcRanges);

		final Iterator<ReadMediaReply> replies = stub
				.withDeadlineAfter(15, TimeUnit.MINUTES)
				.readMedia(rpcReq.build());
		final ReadMediaReply first = replies.next();

		resp.setContentType(first.getMimeType());

		if (httpRanges != null && httpRanges.size() > 0) {
			if (!first.hasRangeIndex()) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "range_index missing from ReadMediaReply.");
				LOG.warn("range_index missing from ReadMediaReply from {} for: {}", listId, itemId);
				return;
			}

			resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
			final InclusiveByteRange range = httpRanges.get(first.getRangeIndex());
			resp.setContentLengthLong(range.getSize());
			resp.setHeader(HttpHeader.ACCEPT_RANGES.asString(), "bytes");
			resp.setHeader(HttpHeader.CONTENT_RANGE.asString(), range.toHeaderRangeString(first.getTotalFileLength()));
		}
		else {
			resp.setContentLengthLong(first.getTotalFileLength());
		}

		@SuppressWarnings("resource")
		final ServletOutputStream outputStream = resp.getOutputStream();
		first.getContent().writeTo(outputStream);
		while (replies.hasNext()) {
			replies.next().getContent().writeTo(outputStream);
		}
	}

}
