package com.vaguehope.morrigan.server;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Objects;

public class PathAndSubPath {

	private final String path;
	private final String subPath;

	PathAndSubPath(final String path, final String subPath) {
		this.path = path;
		this.subPath = subPath;
	}

	public static PathAndSubPath split(String pathInfo) {
		if (pathInfo == null || pathInfo.length() < 1 || "/".equals(pathInfo)) {
			return new PathAndSubPath(null, null);
		}

		if (pathInfo.startsWith("/")) pathInfo = pathInfo.substring(1);

		final int x = pathInfo.indexOf('/');
		if (x < 0) return new PathAndSubPath(pathInfo, null);
		return new PathAndSubPath(
				trimToNull(pathInfo.substring(0, x)),
				trimToNull(removeEnd(pathInfo.substring(x + 1), "/")));
	}

	public boolean hasPath() {
		return this.path != null;
	}

	public boolean hasSubPath() {
		return this.subPath != null;
	}

	public boolean pathIs(final String p) {
		if (this.path == null) return false;
		return this.path.equals(p);
	}

	public boolean subPathIs(final String p) {
		if (this.subPath == null) return false;
		return this.subPath.equals(p);
	}

	public String getPath() {
		if (this.path == null) throw new NullPointerException("path==null");
		return this.path;
	}

	public String getSubPath() {
		if (this.subPath == null) throw new NullPointerException("subPath==null");
		return this.subPath;
	}

	@Override
	public String toString() {
		return String.format("PathAndSubPath{%s, %s}", this.path, this.subPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.path, this.subPath);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		final PathAndSubPath that = (PathAndSubPath) obj;
		return Objects.equals(this.path, that.path)
				&& Objects.equals(this.subPath, that.subPath);
	}

}
