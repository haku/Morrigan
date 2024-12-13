package com.vaguehope.morrigan.transcode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.ItemTags;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.internal.ItemTagsImpl;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.util.MimeType;

public class TranscodeTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();
	private TestMixedMediaDb testDb;
	private FfprobeCache ffprobeCache;
	private TranscodeContext context;

	@Before
	public void before () throws Exception {
		this.testDb = new TestMixedMediaDb();
		this.ffprobeCache = mock(FfprobeCache.class);
		this.context = new TranscodeContext(new Config(this.tmp.getRoot()), this.ffprobeCache);
	}

	@Test
	public void itDoesNotTranscodeIfAlreadyAllowedType() throws Exception {
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.MP3), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.M4A), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGA), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGG), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OPUS), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.FLAC), ItemTags.EMPTY));
		assertEquals(null, Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, this.testDb.addTestTrack(MimeType.WAV), ItemTags.EMPTY));

		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.MP3), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.M4A), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGA), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGG), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OPUS), ItemTags.EMPTY));
	}

	@Test
	public void itUsesCorrectTranscodeOutputType() throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack(MimeType.AVI);
		mockFfprobe(item, null);
		final TranscodeProfile profile = Transcode.COMMON_AUDIO_ONLY.profileForItem(this.context, item, ItemTags.EMPTY);
		assertEquals(MimeType.M4A, profile.getMimeType());
		assertEquals(".m4a", profile.getTmpFileExt());
	}

	@Test
	public void itDoesExtractionIfVideoWithAllowedType() throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack(MimeType.MKV);
		mockFfprobe(item, "opus");
		final TranscodeProfile profile = Transcode.MOBILE_AUDIO.profileForItem(this.context, item, ItemTags.EMPTY);
		assertEquals(MimeType.OPUS, profile.getMimeType());
		assertEquals(".ogg", profile.getTmpFileExt());

		final String[] cmd = profile.transcodeCmd(this.tmp.newFile("foo" + profile.getTmpFileExt()));
		assertThat(Arrays.toString(cmd), containsString(", -vn, -acodec, copy,"));
	}

	@Test
	public void itAlwaysUsesFallbackTypeWhenUsingFilters() throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack(MimeType.MKV);
		this.testDb.addTag(item, "af=volume=10dB", MediaTagType.MANUAL, "");
		mockFfprobe(item, "opus");
		final TranscodeProfile profile = Transcode.MOBILE_AUDIO.profileForItem(this.context, item, ItemTagsImpl.forItem(this.testDb, item));
		assertEquals(MimeType.M4A, profile.getMimeType());
		assertEquals(".m4a", profile.getTmpFileExt());

		final String[] cmd = profile.transcodeCmd(this.tmp.newFile("foo" + profile.getTmpFileExt()));
		assertThat(Arrays.toString(cmd), containsString(", -vn, -c:a, libfdk_aac, -vbr, 5,"));
	}

	@Test
	public void itTranscodesFromOtherType() throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack(MimeType.FLAC);
		mockFfprobe(item, null);
		final TranscodeProfile profile = Transcode.MOBILE_AUDIO.profileForItem(this.context, item, ItemTags.EMPTY);
		assertEquals(MimeType.M4A, profile.getMimeType());
		assertEquals(".m4a", profile.getTmpFileExt());

		final String[] cmd = profile.transcodeCmd(this.tmp.newFile("foo" + profile.getTmpFileExt()));
		assertThat(Arrays.toString(cmd), containsString(", -vn, -c:a, libfdk_aac, -vbr, 5,"));
	}

	private void mockFfprobe(final IMixedMediaItem item, String codec) throws IOException {
		final HashSet<String> codecs = new HashSet<>();
		if (codec != null) codecs.add(codec);
		when(this.ffprobeCache.inspect(item.getFile())).thenReturn(new FfprobeInfo(1234567890L, codecs, Collections.emptySet(), 1000L));
	}

}
