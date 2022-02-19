package com.vaguehope.morrigan.model.media.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaguehope.morrigan.model.media.IMediaItem;

public class CoverArtHelperTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private IMediaItem item;

	@Before
	public void before () throws Exception {
		this.item = new TestItem(this.tmp.newFile("testItem.ogg").getAbsolutePath());
	}

	@Test
	public void itReturnsNullForNoMatch () throws Exception {
		assertEquals(null, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itReturnsNullWhenNoBetterOptions () throws Exception {
		givenNoise();
		assertEquals(null, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverForDirectory () throws Exception {
		givenNoise();
		final File cover = this.tmp.newFile("cover.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.tmp.getRoot()));
	}

	@Test
	public void itFindsCoverWithSameNameButJpgExt () throws Exception {
		givenCoverNoise();
		this.tmp.newFile("testItem thumb.jpg");
		final File cover = this.tmp.newFile("testItem.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameButJpgExtMixedCase () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("testItem.jpG");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameButMixedCaseAndJpgExt () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("TestItem.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameStartAndJpgExt () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("testItem thumb.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameButJpegExt () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("testItem.jpeg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameButGifExt () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("testItem.gif");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverWithSameNameButPngExt () throws Exception {
		givenCoverNoise();
		final File cover = this.tmp.newFile("testItem.png");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverCalledCover () throws Exception {
		givenNoise();
		final File cover = this.tmp.newFile("cover.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverCalledCoverMixedCase () throws Exception {
		givenNoise();
		final File cover = this.tmp.newFile("coVeR.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverCalledAlbum () throws Exception {
		givenNoise();
		final File cover = this.tmp.newFile("album.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverCalledFolderOverCover () throws Exception {
		givenNoise();
		this.tmp.newFile("cover.jpg");
		this.tmp.newFile("album.jpg");
		final File cover = this.tmp.newFile("folder.jpg");
		assertEquals(cover, CoverArtHelper.findCoverArt(this.item));
	}

	@Test
	public void itFindsCoverForAlbum () throws Exception {
		Collection<IMediaItem> albumPics = new LinkedHashSet<IMediaItem>();
		albumPics.add(new TestItem(givenNoise().getAbsolutePath()));

		final File cover = this.tmp.newFile("cover.jpg");
		albumPics.add(new TestItem(cover.getAbsolutePath()));

		assertEquals(cover, CoverArtHelper.findCoverArt(albumPics));
	}

	@Test
	public void itFindsCoverForAlbumByFallingBackToFirstPic () throws Exception {
		Collection<IMediaItem> albumPics = new LinkedHashSet<IMediaItem>();

		final File cover = this.tmp.newFile("somethinG.jPg");
		albumPics.add(new TestItem(cover.getAbsolutePath()));

		assertEquals(cover, CoverArtHelper.findCoverArt(albumPics));
	}

	private void givenCoverNoise () throws IOException {
		givenNoise();
		for (final String ext : new String[] { "jpg", "png", "gif" }) {
			for (final String name : new String[] { "cover", "album", "folder" }) {
				this.tmp.newFile(name + "." + ext);
			}
		}
	}

	private File givenNoise () throws IOException {
		return this.tmp.newFile("something.jpg");
	}

	private static class TestItem extends MediaItem {

		public TestItem (final String filePath) {
			super(filePath);
		}

	}

}
