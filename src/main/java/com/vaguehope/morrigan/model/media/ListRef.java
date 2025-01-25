package com.vaguehope.morrigan.model.media;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ListRef implements Comparable<ListRef> {

	public static final String DLNA_ROOT_NODE_ID = "0"; // Root id of '0' is in the DLNA spec.
	public static final String RPC_ROOT_NODE_ID = "0";

	public static ListRef fromUrlForm(final String urlFrom) {
		if (urlFrom == null) return null;

		final int s = urlFrom.indexOf(':');
		if (s < 1) throw new IllegalArgumentException("Invalid: " + urlFrom);

		final String rawType = urlFrom.substring(0, s);
		final ListType type = ListType.valueOf(rawType);  // TODO make more permissive?

		String listId = null, nodeId = null, search = null;
		for (final String nameAndVal : StringUtils.split(urlFrom.substring(s + 1), "&")) {
			final int e = nameAndVal.indexOf('=');
			if (e < 1) throw new IllegalArgumentException("Invalid:" + urlFrom);
			final String name = nameAndVal.substring(0, e);
			final String val = URLDecoder.decode(nameAndVal.substring(e + 1), StandardCharsets.UTF_8);
			switch (name) {
			case "l":
				listId = val;
				break;
			case "n":
				nodeId = val;
				break;
			case "s":
				search = val;
				break;
			default:
				throw new IllegalArgumentException("Invalid:" + urlFrom);
			}
		}

		return new ListRef(type, listId, nodeId, search);
	}

	public static ListRef forLocal(final String dbName) {
		return new ListRef(ListType.LOCAL, dbName, null, null);
	}

	public static ListRef forLocalSearch(final String dbName, final String search) {
		return new ListRef(ListType.LOCAL, dbName, null, search);
	}

	public static ListRef forDlnaNode(final String listId, final String nodeId) {
		return new ListRef(ListType.DLNA, listId, nodeId, null);
	}

	public static ListRef forRpcNode(final String listId, final String nodeId) {
		return new ListRef(ListType.RPC, listId, nodeId, null);
	}

	public static ListRef forRpcSearch(final String listId, final String search) {
		return new ListRef(ListType.RPC, listId, null, search);
	}

	private final ListType type;
	private final String listId;
	private final String nodeId;
	private final String search;

	private ListRef(final ListType type, final String listId, final String nodeId, final String search) {
		if (type == null) throw new IllegalArgumentException("type can not be null.");
		if (StringUtils.isBlank(listId)) throw new IllegalArgumentException("listId can not be null or empty.");
		this.type = type;
		this.listId = listId;
		this.nodeId = nodeId;
		this.search = search;
	}

	public String toUrlForm() {
		final StringBuilder s = new StringBuilder();
		s.append(this.type.name()).append(":");
		s.append("l=").append(URLEncoder.encode(this.listId, StandardCharsets.UTF_8));
		if (this.nodeId != null) s.append("&").append("n=").append(URLEncoder.encode(this.nodeId, StandardCharsets.UTF_8));
		if (this.search != null) s.append("&").append("s=").append(URLEncoder.encode(this.search, StandardCharsets.UTF_8));
		return s.toString();
	}

	public ListType getType() {
		return this.type;
	}

	public String getListId() {
		return this.listId;
	}

	public String getNodeId() {
		return this.nodeId;
	}

	public String getSearch() {
		return this.search;
	}

	public ListRef withNodeId(final String newNodeId) {
		return new ListRef(this.type, this.listId, newNodeId, this.search);
	}

	public ListRef withSearch(final String newSearch) {
		return new ListRef(this.type, this.listId, this.nodeId, newSearch);
	}

	public ListRef toRoot() {
		switch (this.type) {
		case DLNA:
			return forRpcNode(this.listId, DLNA_ROOT_NODE_ID);
		case RPC:
			return forRpcNode(this.listId, RPC_ROOT_NODE_ID);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public boolean isHasRootNodes() {
		return this.type == ListType.RPC || this.type == ListType.DLNA;
	}

	@Override
	public String toString() {
		return String.format("ListRef{%s, %s, %s, %s}", this.type, this.listId, this.nodeId, this.search);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.listId, this.nodeId, this.search);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof ListRef)) return false;
		final ListRef that = (ListRef) obj;
		return Objects.equals(this.type, that.type)
				&& Objects.equals(this.listId, that.listId)
				&& Objects.equals(this.nodeId, that.nodeId)
				&& Objects.equals(this.search, that.search);
	}

	public enum ListType {
		LOCAL("Local"),
		RPC("RPC"),
		DLNA("DLNA");

		private final String uiTitle;

		ListType(final String uiTitle) {
			this.uiTitle = uiTitle;
		}

		public String getUiTitle() {
			return this.uiTitle;
		}
	}

	@Override
	public int compareTo(final ListRef o) {
		return Order.ASC.compare(this, o);
	}

	public enum Order implements Comparator<ListRef> {
		ASC {
			@Override
			public int compare (final ListRef a, final ListRef b) {
				final int c = Integer.compare(a.type.ordinal(), b.type.ordinal());
				if (c != 0) return c;
				return LIST_ID.compare(a, b);
			}
		},
		LIST_ID {
			@Override
			public int compare(final ListRef a, final ListRef b) {
				return compareWithFallThrough(a, b, a.listId, b.listId, NODE_ID);
			}
		},
		NODE_ID {
			@Override
			public int compare(final ListRef a, final ListRef b) {
				return compareWithFallThrough(a, b, a.nodeId, b.nodeId, SEARCH);
			}
		},
		SEARCH {
			@Override
			public int compare(final ListRef a, final ListRef b) {
				return compareWithFallThrough(a, b, a.search, b.search, null);
			}
		};

		@Override
		public abstract int compare (ListRef a, ListRef b);

		int compareWithFallThrough(final ListRef ar, final ListRef br, final String a, final String b, final Order fallthrough) {
			final int c = (a != null ? (b != null ? a.compareTo(b) : 1) : 0);
			if (c != 0 || fallthrough == null) return c;
			return fallthrough.compare(ar, br);
		}
	}

}
