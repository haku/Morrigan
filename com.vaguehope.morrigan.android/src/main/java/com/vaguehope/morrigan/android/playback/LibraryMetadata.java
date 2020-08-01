package com.vaguehope.morrigan.android.playback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.net.Uri;

public class LibraryMetadata {

	private final long id;
	private final String name;
	private final List<Uri> sources;

	public LibraryMetadata (final long id, final String name, final String sourcesJson) throws JSONException {
		this(id, name, parseSources(sourcesJson));
	}

	public LibraryMetadata (final long id, final String name, final List<Uri> sources) {
		this.id = id;
		this.name = name;
		this.sources = sources;
	}

	public long getId () {
		return this.id;
	}

	public String getName () {
		return this.name;
	}

	public List<Uri> getSources () {
		return this.sources;
	}

	public JSONArray getSourcesJson () {
		final JSONArray arr = new JSONArray();
		for (final Uri source : this.sources) {
			arr.put(source.toString());
		}
		return arr;
	}

	private static List<Uri> parseSources (final String json) throws JSONException {
		if (json == null) return Collections.emptyList();
		return parseSources((JSONArray) new JSONTokener(json).nextValue());
	}

	private static List<Uri> parseSources (final JSONArray arr) throws JSONException {
		final List<Uri> ret = new ArrayList<Uri>();
		for (int i = 0; i < arr.length(); i++) {
			ret.add(Uri.parse(arr.getString(i)));
		}
		return ret;
	}

	@Override
	public String toString () {
		return String.format("Library{%s, %s}", this.id, this.name);
	}

	public LibraryMetadata withName (final String newName) {
		return new LibraryMetadata(this.id, newName, this.sources);
	}

	public LibraryMetadata withSource(final Uri sourceToAdd) {
		final ArrayList<Uri> l = new ArrayList<Uri>(this.sources);
		l.add(sourceToAdd);
		return new LibraryMetadata(this.id, this.name, l);
	}

	public LibraryMetadata withoutSource(final Uri sourceToAdd) {
		final ArrayList<Uri> l = new ArrayList<Uri>(this.sources);
		l.remove(sourceToAdd);
		return new LibraryMetadata(this.id, this.name, l);
	}

}
