package com.vaguehope.morrigan.util;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum MimeType {

	AVI("avi", "video/avi"),
	FLV("flv", "video/x-flv"),
	M4V("m4v", "video/mp4"),
	MKV("mkv", "video/x-matroska"),
	MOV("mov", "video/quicktime"),
	MP4("mp4", "video/mp4"),
	MPEG("mpeg", "video/mpeg"),
	MPG("mpg", "video/mpeg"),
	OGM("ogm", "video/ogg"),
	OGV("ogv", "video/ogg"),
	RMVB("rmvb", "application/vnd.rn-realmedia-vbr"),
	WEBM("webm", "video/webm"),
	WMV("wmv", "video/x-ms-wmv"),
	_3GP("3gp", "video/3gpp"),

	GIF("gif", "image/gif"),
	JPEG("jpeg", "image/jpeg"),
	JPG("jpg", "image/jpeg"),
	PNG("png", "image/png"),

	AAC("aac", "audio/aac"),
	AC3("ac3", "audio/ac3"),
	FLAC("flac", "audio/flac"),
	M4A("m4a", "audio/mp4"),
	MP3("mp3", "audio/mpeg"),
	MPGA("mpga", "audio/mpeg"),
	OGA("oga", "audio/ogg"),
	OGG("ogg", "audio/ogg"),
	OPUS("opus", "audio/opus", "ogg"),  // technically the file extension should always be .ogg
	RA("ra", "audio/vnd.rn-realaudio"),
	WAV("wav", "audio/vnd.wave"),
	WMA("wma", "audio/x-ms-wma"),

	SRT("srt", "text/srt"),
	SSA("ssa", "text/x-ssa"),
	ASS("ass", "text/x-ass"),
	;

	private static final Map<String, MimeType> EXT_TO_FORMAT;
	static {
		final Map<String, MimeType> t = new ConcurrentHashMap<>(MimeType.values().length);
		for (MimeType f : MimeType.values()) {
			if (t.put(f.ext, f) != null) throw new IllegalStateException("Duplicate input exts not allow: " + f.ext);
		}
		EXT_TO_FORMAT = Collections.unmodifiableMap(t);
	}

	private final String ext;
	private final String mimeType;
	private final String outputExt;

	private MimeType (final String ext, final String mime) {
		this(ext, mime, ext);  // default to same ext for outputs.
	}

	private MimeType (final String ext, final String mime, final String outputExt) {
		this.ext = ext;
		this.mimeType = mime;
		this.outputExt = outputExt;
	}

	public String getExt () {
		return this.ext;
	}

	public String getOutputExt() {
		return this.outputExt;
	}

	public String getMimeType () {
		return this.mimeType;
	}

	public static MimeType identify (final File file) {
		return identify(file.getName());
	}

	public static MimeType identify (final String name) {
		return EXT_TO_FORMAT.get(name.substring(name.lastIndexOf(".") + 1).toLowerCase());
	}

}
