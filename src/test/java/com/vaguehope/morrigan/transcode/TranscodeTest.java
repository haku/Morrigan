package com.vaguehope.morrigan.transcode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.ItemTags;
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
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.MP3), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.M4A), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGA), ItemTags.EMPTY));
		assertEquals(null, Transcode.MOBILE_AUDIO.profileForItem(this.context, this.testDb.addTestTrack(MimeType.OGG), ItemTags.EMPTY));
	}

	@Test
	public void itTranscodesFromOtherType() throws Exception {
		final IMixedMediaItem item = this.testDb.addTestTrack(MimeType.FLAC);
		when(this.ffprobeCache.inspect(item.getFile())).thenReturn(new FfprobeInfo(1234567890L, new HashSet<String>(), new HashSet<String>(), 1000L));
		final TranscodeProfile profile = Transcode.MOBILE_AUDIO.profileForItem(this.context, item, ItemTags.EMPTY);
		assertEquals(MimeType.M4A, profile.getMimeType());

		final String[] cmd = profile.transcodeCmd(this.tmp.newFile("foo" + profile.getTmpFileExt()));
		assertThat(Arrays.toString(cmd), containsString(", -vn, -c:a, libfdk_aac, -vbr, 5,"));
	}

}
