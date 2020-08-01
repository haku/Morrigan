package com.vaguehope.morrigan.android.checkout;

import java.io.File;

import com.vaguehope.morrigan.android.helper.Titleable;

class TitleableFile implements Titleable {

	private final File file;

	public TitleableFile (final String path) {
		this.file = new File(path);
	}

	@Override
	public String getUiTitle () {
		return this.file.getName();
	}

	public File getFile () {
		return this.file;
	}

}