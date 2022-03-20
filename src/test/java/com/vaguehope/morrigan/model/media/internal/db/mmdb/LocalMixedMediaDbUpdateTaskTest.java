package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.media.ILocalMixedMediaDb;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.MediaTag;
import com.vaguehope.morrigan.model.media.MediaTagClassification;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.util.FileSystem;

public class LocalMixedMediaDbUpdateTaskTest {

	private TestMixedMediaDb testDb;
	private PlaybackEngineFactory playbackEngineFactory;
	private MediaFactory mediaFactory;
	private TaskEventListener taskEventListener;
	private LocalMixedMediaDbUpdateTask undertest;

	private Map<String, File> mockFiles;
	private FileSystem fileSystem;

	@Before
	public void before() throws Exception {
		this.testDb = new TestMixedMediaDb();
		this.testDb.setHideMissing(false);

		this.mediaFactory = mock(MediaFactory.class);
		when(this.mediaFactory.getLocalMixedMediaDbTransactional(this.testDb)).thenAnswer(new Answer<ILocalMixedMediaDb>() {
			@Override
			public ILocalMixedMediaDb answer(final InvocationOnMock invocation) throws Throwable {
				return new TestMixedMediaDb(LocalMixedMediaDbUpdateTaskTest.this.testDb.getListName(), false);
			}
		});

		this.mockFiles = new HashMap<>();
		this.fileSystem = mock(FileSystem.class);
		doAnswer(new Answer<File>() {
			@Override
			public File answer(final InvocationOnMock i) throws Throwable {
				final Object p = i.getArgument(0);
				return getMockFile(p);
			}
		}).when(this.fileSystem).makeFile(anyString());
		doAnswer(new Answer<File>() {
			@Override
			public File answer(final InvocationOnMock i) throws Throwable {
				final File f = i.getArgument(0);
				final String p = i.getArgument(1);
				final String k = f.getAbsolutePath() + "/" + p;
				System.out.println("new File(" + f + "," + p + ") = " + k);
				return getMockFile(k);
			}
		}).when(this.fileSystem).makeFile(any(File.class), anyString());

		this.taskEventListener = mock(TaskEventListener.class);
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(final InvocationOnMock i) throws Throwable {
				System.out.println("TASK: " + i.getArgument(0));
				return null;
			}
		}).when(this.taskEventListener).subTask(anyString());
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(final InvocationOnMock i) throws Throwable {
				System.out.println("LOG:" + i.getArgument(0) + ": " + i.getArgument(1));
				return null;
			}
		}).when(this.taskEventListener).logMsg(anyString(), anyString());
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(final InvocationOnMock i) throws Throwable {
				System.out.println("ERROR:" + i.getArgument(0) + ": " + i.getArgument(1) + "  " + i.getArgument(2));
				return null;
			}
		}).when(this.taskEventListener).logError(anyString(), anyString(), any(Throwable.class));

		this.playbackEngineFactory = mock(PlaybackEngineFactory.class);
		this.undertest = new LocalMixedMediaDbUpdateTask(this.testDb, this.playbackEngineFactory, this.mediaFactory);
		this.undertest.setFileSystem(this.fileSystem);
	}

	@Test
	public void itDoesNotMarkAnExistingFileAsMissing() throws Exception {
		final File f1 = mockFile();
		this.testDb.addTestTrack(f1);

		runUpdateTask();
		assertHasFile(f1, false, true);
	}

	@Test
	public void itMarksAMissingFileAsMissing() throws Exception {
		final File f1 = mockFile();
		this.testDb.addTestTrack(f1);
		when(f1.exists()).thenReturn(false);

		runUpdateTask();
		assertHasFile(f1, true, true);
	}

	@Test
	public void itHandlesRemovingAnAlbumWithMissingItem() throws Exception {
		final File sourceDir = mockDir("/dir0/dir1");
		this.testDb.addSource(sourceDir.getAbsolutePath());

		final File d1 = mockDir(sourceDir, "album1");
		final File f1 = mockFileInDir(d1, "foo.wav", BigInteger.valueOf(2));
		final File a1 = mockFileInDir(d1, ".album", BigInteger.valueOf(1));
		runUpdateTask();
		verify(this.taskEventListener).subTask("Found 1 albums");

		when(f1.exists()).thenReturn(false);
		when(a1.exists()).thenReturn(false);
		runUpdateTask();
		verify(this.taskEventListener).logMsg(anyString(), eq("Removed 1 albums"));
	}

	@Test
	public void itMergesAMovedAlbumWithTags() throws Exception {
		final File sourceDir = mockDir("/dir0/dir1");
		this.testDb.addSource(sourceDir.getAbsolutePath());
		final File mediaDir1 = mockDir(sourceDir, "media1");

		final File d1 = mockDir(mediaDir1, "album");
		final File f1 = mockFileInDir(d1, "foo.wav", BigInteger.valueOf(2));
		final File a1 = mockFileInDir(d1, ".album", BigInteger.valueOf(1));
		runUpdateTask();
		verify(this.taskEventListener).subTask("Found 1 albums");

		final IMixedMediaItem i1 = this.testDb.getByFile(f1);
		this.testDb.addTag(i1, "my tag", MediaTagType.MANUAL, (MediaTagClassification) null);
		this.testDb.addTag(i1, "auto tag", MediaTagType.AUTOMATIC, "bar");
		this.testDb.addTag(i1, "auto tag2", MediaTagType.AUTOMATIC, "foo");

		this.testDb.addTag(i1, "deleted tag 1", MediaTagType.MANUAL, (MediaTagClassification) null);
		this.testDb.addTag(i1, "deleted tag 2", MediaTagType.MANUAL, (MediaTagClassification) null);
		this.testDb.addTag(i1, "deleted tag 3", MediaTagType.MANUAL, (MediaTagClassification) null);
		final List<MediaTag> tags = this.testDb.getTags(i1);
		int rmCount = 0;
		for (final MediaTag t : tags) {
			if (t.getTag().startsWith("deleted")) {
				this.testDb.removeTag(t);
				rmCount += 1;
			}
		}
		assertEquals(3, rmCount);

		when(f1.exists()).thenReturn(false);
		when(a1.exists()).thenReturn(false);
		final File mediaDir2 = mockDir(sourceDir, "media2");
		final File d2 = mockDir(mediaDir2, "album");
		final File f2 = mockFileInDir(d2, "foo.wav", BigInteger.valueOf(2));
		final File a2 = mockFileInDir(d2, ".album", BigInteger.valueOf(1));
		runUpdateTask();
		verify(this.taskEventListener, times(2)).subTask("Found 1 albums");
		verify(this.taskEventListener).logMsg(anyString(), contains("Merged 1 "));
		final IMixedMediaItem i2 = this.testDb.getByFile(f2);
		assertEquals("my tag", this.testDb.getTags(i2).get(0).getTag());
		assertEquals("auto tag", this.testDb.getTags(i2).get(1).getTag());
	}

	@Test
	public void itMergesAMovedAlbumWithOnlyDeletedTags() throws Exception {
		final File sourceDir = mockDir("/dir0/dir1");
		this.testDb.addSource(sourceDir.getAbsolutePath());

		final File f1 = mockFileInDir(sourceDir, "foo.wav", BigInteger.valueOf(2));
		runUpdateTask();

		final IMixedMediaItem i1 = this.testDb.getByFile(f1);
		this.testDb.addTag(i1, "deleted tag 1", MediaTagType.MANUAL, (MediaTagClassification) null);
		final List<MediaTag> tags = this.testDb.getTags(i1);
		int rmCount = 0;
		for (final MediaTag t : tags) {
			if (t.getTag().startsWith("deleted")) {
				this.testDb.removeTag(t);
				rmCount += 1;
			}
		}
		assertEquals(1, rmCount);

		when(f1.exists()).thenReturn(false);
		final File f2 = mockFileInDir(sourceDir, "bar.wav", BigInteger.valueOf(2));
		runUpdateTask();
		verify(this.taskEventListener).logMsg(anyString(), contains("Merged 1 "));
	}

	private void runUpdateTask() {
		this.testDb.printContent("Before");
		final TaskResult result = this.undertest.run(this.taskEventListener);
		this.testDb.printContent("After");

		if (result.getErrThr() != null) result.getErrThr().printStackTrace();
		assertEquals("Msg: " + result.getErrMsg() + " Ex: " + result.getErrThr(), TaskOutcome.SUCCESS, result.getOutcome());
	}

	private void assertHasFile(final File f1, final boolean missing, final boolean enabled) throws DbException {
		final IMixedMediaItem a = this.testDb.getByFile(f1);
		assertNotNull("Not found: " + f1.getAbsolutePath(), a);
		assertEquals(f1.length(), a.getFileSize());
		assertEquals(missing, a.isMissing());
		assertEquals(enabled, a.isEnabled());
	}

	private File mockFile() throws Exception {
		final String dirPath = "/dir0/dir1/dir2";
		final File dir = mockDir(dirPath);
		return mockFileInDir(dir);
	}

	private File mockFileInDir(final File dir) throws Exception {
		final int n = TestMixedMediaDb.getTrackNumber();
		final long mtime = 1234567890000L + n;
		final BigInteger md5 = BigInteger.valueOf(mtime);
		return mockFileInDir(dir, String.format("/some_media_file_%s.ext", n), mtime, md5);
	}

	private File mockFileInDir(final File dir, final String fileName, final BigInteger md5) throws Exception {
		final int n = TestMixedMediaDb.getTrackNumber();
		final long mtime = 1234567890000L + n;
		return mockFileInDir(dir, fileName, mtime, md5);
	}

	private File mockDir(final String dirPath) {
		return mockDir(null, dirPath);
	}

	private File mockDir(final File parent, final String dirPath) {
		String parentPath = parent != null ? parent.getAbsolutePath() : "";
		if (parentPath.endsWith("/")) parentPath = parentPath.substring(0, parentPath.length() - 1);
		final String fullDirPath = parentPath + (dirPath.startsWith("/") ? "" : "/") + dirPath;

		final File d = mock(File.class);
		when(d.isDirectory()).thenReturn(true);
		when(d.exists()).thenReturn(true);
		when(d.canRead()).thenReturn(true);
		when(d.getName()).thenReturn(fullDirPath.substring(fullDirPath.lastIndexOf("/")));
		when(d.getAbsolutePath()).thenReturn(fullDirPath);
		when(d.listFiles()).thenReturn(new File[] {});

		if (parent != null) {
			when(d.getParentFile()).thenReturn(parent);

			final List<File> dirFiles = new ArrayList<>(Arrays.asList(parent.listFiles()));
			dirFiles.add(d);
			when(parent.listFiles()).thenReturn(dirFiles.toArray(new File[dirFiles.size()]));
		}

		putMockFile(fullDirPath, d);
		return d;
	}

	private File mockFileInDir(final File dir, final String fileName, final long mtime, final BigInteger md5) throws Exception {
		String dirPath = dir.getAbsolutePath();
		if (!dirPath.endsWith("/")) dirPath += "/";
		final String absPath = dirPath + fileName;

		if (this.mockFiles.get(absPath) != null) throw new IllegalStateException("Already mocked: " + absPath);

		final File f = mock(File.class);
		when(f.getParentFile()).thenReturn(dir);
		when(f.isFile()).thenReturn(true);
		when(f.exists()).thenReturn(true);
		when(f.canRead()).thenReturn(true);
		when(f.getName()).thenReturn(fileName);
		when(f.getAbsolutePath()).thenReturn(absPath);
		when(f.lastModified()).thenReturn(mtime);

		when(this.fileSystem.generateMd5Checksum(eq(f), any(ByteBuffer.class))).thenReturn(md5);

		putMockFile(absPath, f);

		final List<File> dirFiles = new ArrayList<>(Arrays.asList(dir.listFiles()));
		dirFiles.add(f);
		when(dir.listFiles()).thenReturn(dirFiles.toArray(new File[dirFiles.size()]));

		return f;
	}

	private void putMockFile(final String absPath, final File f) {
		final File prev = this.mockFiles.put(absPath, f);
		if (prev != null) throw new IllegalStateException("Path already mocked: " + absPath);
	}

	private File getMockFile(final Object p) {
		final File f = this.mockFiles.get(p);
		if (f != null) return f;

		System.out.println("File not found: " + p);
		return mock(File.class);
	}

}
