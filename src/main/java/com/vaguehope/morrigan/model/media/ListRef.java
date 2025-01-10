package com.vaguehope.morrigan.model.media;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class ListRef {

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

	private final ListType type;
	private final String listId;
	private final String nodeId;
	private final String search;

	public ListRef(final ListType type, final String listId, final String nodeId, final String search) {
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
		REMOTE("Remote"),
		DLNA("DLNA"),
		GRPC("GRPC");

		private final String uiName;

		ListType(final String uiName) {
			this.uiName = uiName;
		}

		public String getUiName() {
			return this.uiName;
		}
	}

}
