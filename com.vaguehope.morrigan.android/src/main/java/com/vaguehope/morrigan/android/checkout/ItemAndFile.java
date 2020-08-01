package com.vaguehope.morrigan.android.checkout;

import java.io.File;

import com.vaguehope.morrigan.android.model.MlistItem;

class ItemAndFile {

	private final MlistItem item;
	private final File localFile;

	public ItemAndFile (final MlistItem item, final File localFile) {
		this.item = item;
		this.localFile = localFile;
	}

	public MlistItem getItem () {
		return this.item;
	}

	public File getLocalFile () {
		return this.localFile;
	}
}