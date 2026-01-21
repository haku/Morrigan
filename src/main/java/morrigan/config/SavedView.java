package morrigan.config;

import java.util.Objects;

public class SavedView {

	private final String name;
	private final String listref;
	private final String query;

	public SavedView(final String name, final String listref, final String query) {
		this.name = name;
		this.listref = listref;
		this.query = query;
	}

	public String getName() {
		return this.name;
	}

	public String getListRef() {
		return this.listref;
	}

	public String getQuery() {
		return this.query;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.listref, this.query);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof SavedView)) return false;
		final SavedView that = (SavedView) obj;

		return Objects.equals(this.name, that.name)
				&& Objects.equals(this.listref, that.listref)
				&& Objects.equals(this.query, that.query);
	}

}
