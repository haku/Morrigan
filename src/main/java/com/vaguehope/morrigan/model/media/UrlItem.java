package com.vaguehope.morrigan.model.media;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.vaguehope.morrigan.model.exceptions.MorriganException;

import io.jsonwebtoken.lang.Collections;

public class UrlItem extends EphemeralItem {

	private final String url;

	public UrlItem(final String url) {
		if (StringUtils.isBlank(url)) throw new IllegalArgumentException("missing url.");
		try {
			this.url = new URI(url).toString();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("invalid url: " + url);
		}
	}

	@Override
	public String getTitle() {
		return this.url;
	}

	@Override
	public MediaType getMediaType() {
		return null;
	}

	@Override
	public boolean hasRemoteLocation() {
		return true;
	}

	@Override
	public String getRemoteLocation() {
		return this.url;
	}

	@Override
	public boolean isPlayable() {
		return true;
	}

	@Override
	public int hashCode () {
		return Objects.hash(this.url);
	}

	@Override
	public boolean equals (final Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if (!(obj instanceof UrlItem)) return false;
		final UrlItem that = (UrlItem) obj;
		return Objects.equals(this.url, that.url);
	}

// - - - - - - - -

	@Override
	public long getFileSize() {
		return 0;
	}

	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getDateAdded() {
		return null;
	}

	@Override
	public String getRemoteId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MediaTag> getTags() throws MorriganException {
		return Collections.emptyList();
	}

	@Override
	public int getDuration() {
		return 0;
	}

	@Override
	public String getCoverArtRemoteLocation() {
		return null;
	}

	@Override
	public boolean isPicture() {
		return false;
	}

	@Override
	public int getWidth() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 0;
	}

}
