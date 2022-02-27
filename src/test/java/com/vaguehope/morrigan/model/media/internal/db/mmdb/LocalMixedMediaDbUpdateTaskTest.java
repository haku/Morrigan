package com.vaguehope.morrigan.model.media.internal.db.mmdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaguehope.morrigan.engines.playback.PlaybackEngineFactory;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.MediaFactory;
import com.vaguehope.morrigan.model.media.test.TestMixedMediaDb;
import com.vaguehope.morrigan.sqlitewrapper.DbException;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;
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

		this.mockFiles = new HashMap<>();
		this.fileSystem = mock(FileSystem.class);
		doAnswer(new Answer<File>() {
			@Override
			public File answer(final InvocationOnMock i) throws Throwable {
				final Object p = i.getArgument(0);
				return LocalMixedMediaDbUpdateTaskTest.this.mockFiles.get(p);
			}
		}).when(this.fileSystem).makeFile(anyString());
		doAnswer(new Answer<File>() {
			@Override
			public File answer(final InvocationOnMock i) throws Throwable {
				final File f = i.getArgument(0);
				final String p = i.getArgument(1);
				final String k = f.getAbsolutePath() + "/" + p;
				System.out.println("new File(" + f + "," + p + ") = " + k);
				return LocalMixedMediaDbUpdateTaskTest.this.mockFiles.get(k);
			}
		}).when(this.fileSystem).makeFile(any(File.class), anyString());

		this.playbackEngineFactory = mock(PlaybackEngineFactory.class);
		this.mediaFactory = mock(MediaFactory.class);
		this.taskEventListener = mock(TaskEventListener.class);
		this.undertest = new LocalMixedMediaDbUpdateTask(this.testDb, this.playbackEngineFactory, this.mediaFactory);
		this.undertest.setFileSystem(this.fileSystem);
	}

	@Test
	public void itDoesNotMarkAnExistingFileAsMissing() throws Exception {
		final File f1 = mockFile(true);
		this.testDb.addTestTrack(f1);

		runUpdateTask();
		assertHasFile(f1, false, true);
	}

	@Test
	public void itMarksAMissingFileAsMissing() throws Exception {
		final File f1 = mockFile(true);
		this.testDb.addTestTrack(f1);
		when(f1.exists()).thenReturn(false);

		runUpdateTask();
		assertHasFile(f1, true, true);
	}

	// TODO test moved album.

	private void runUpdateTask() {
		this.testDb.printContent("Before");
		final TaskResult result = this.undertest.run(this.taskEventListener);
		this.testDb.printContent("After");
		assertEquals("Msg: " + result.getErrMsg() + " Ex: " + result.getErrThr(), TaskOutcome.SUCCESS, result.getOutcome());
	}

	private void assertHasFile(final File f1, final boolean missing, final boolean enabled) throws DbException {
		final IMixedMediaItem a = this.testDb.getByFile(f1);
		assertNotNull("Not found: " + f1.getAbsolutePath(), a);
		assertEquals(f1.length(), a.getFileSize());
		assertEquals(missing, a.isMissing());
		assertEquals(enabled, a.isEnabled());
	}

	private File mockFile(final boolean exists) {
		final int n = TestMixedMediaDb.getTrackNumber();
		final String absPath = String.format("/dir0/dir1/dir2/some_media_file_%s.ext", n);
		final File f = mock(File.class);
		when(f.isFile()).thenReturn(true);
		when(f.exists()).thenReturn(exists);
		when(f.getAbsolutePath()).thenReturn(absPath);
		when(f.lastModified()).thenReturn(1234567890000L + n);

		this.mockFiles.put(absPath, f);
		return f;
	}

}
