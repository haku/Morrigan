package morrigan.model.media;

import java.util.Objects;

public class ListRefWithTitle implements Comparable<ListRefWithTitle> {

	private final ListRef listRef;
	private final String title;

	public ListRefWithTitle(final ListRef listRef, final String title) {
		this.listRef = listRef;
		this.title = title;
	}

	public ListRef getListRef() {
		return this.listRef;
	}

	public String getTitle() {
		return this.title;
	}

	@Override
	public int compareTo(final ListRefWithTitle o) {
		return ListRef.Order.ASC.compare(this.listRef, o.listRef);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.listRef);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (!(obj instanceof ListRefWithTitle)) return false;
		final ListRefWithTitle that = (ListRefWithTitle) obj;
		return Objects.equals(this.listRef, that.listRef);
	}

}
