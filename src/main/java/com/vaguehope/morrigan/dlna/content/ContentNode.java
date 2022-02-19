package com.vaguehope.morrigan.dlna.content;

import java.util.concurrent.TimeUnit;

import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

/**
 * Either a Container or an Item.
 */
public class ContentNode {

	private final Container container;
	private final Item item;
	private final long created;

	public ContentNode (final Container container) {
		this.container = container;
		this.item = null;
		this.created = now();
	}

	public ContentNode (final Item item) {
		this.container = null;
		this.item = item;
		this.created = now();
	}

	public boolean isItem () {
		return this.item != null;
	}

	public Item getItem () {
		return this.item;
	}

	public Container getContainer () {
		return this.container;
	}

	public long age(final TimeUnit unit) {
		return unit.convert(now() - this.created, TimeUnit.NANOSECONDS);
	}

	private static final long NANO_ORIGIN = System.nanoTime();

	protected static long now () {
		return System.nanoTime() - NANO_ORIGIN;
	}

}
