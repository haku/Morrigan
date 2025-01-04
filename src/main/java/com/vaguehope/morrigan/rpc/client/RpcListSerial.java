package com.vaguehope.morrigan.rpc.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

public class RpcListSerial {

	private final static Gson GSON = new Gson();

	private final String listLocalIdentifier;
	private final String searchTerm;

	public RpcListSerial(final String listLocalIdentifier, final String searchTerm) {
		this.listLocalIdentifier = listLocalIdentifier;
		this.searchTerm = searchTerm;
	}

	public String getListLocalIdentifier() {
		return this.listLocalIdentifier;
	}

	public String getSearchTerm() {
		return this.searchTerm;
	}

	public String serialise() {
		final Map<String, String> map = new HashMap<>();
		map.put("id", this.listLocalIdentifier);
		if (this.searchTerm != null) map.put("search", this.searchTerm);
		return GSON.toJson(map);
	}

	public static RpcListSerial parse(final String serial) {
		final Map<String, String> map = GSON.fromJson(serial, Map.class);
		final String id = map.get("id");
		if (StringUtils.isBlank("id")) throw new IllegalArgumentException("Invalid serial: " + serial);
		final String search = StringUtils.trimToNull(map.get("search"));
		return new RpcListSerial(id, search);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.listLocalIdentifier, this.searchTerm);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof RpcListSerial)) return false;
		final RpcListSerial that = (RpcListSerial) obj;
		return Objects.equals(this.listLocalIdentifier, that.listLocalIdentifier)
				&& Objects.equals(this.searchTerm, that.searchTerm);
	}

}
