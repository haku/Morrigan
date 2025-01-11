package com.vaguehope.morrigan.dlna.players;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.model.MediaInfo;
import org.jupnp.support.model.PositionInfo;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportState;
import org.jupnp.support.model.TransportStatus;
import org.mockito.InOrder;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.dlna.DlnaException;
import com.vaguehope.morrigan.dlna.MediaFormat;
import com.vaguehope.morrigan.dlna.players.DlnaPlayingParamsFactory.DlnaPlayingParams;
import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.MediaItem;
import com.vaguehope.morrigan.model.media.test.TestMediaDb;
import com.vaguehope.morrigan.player.PlayItem;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;
import com.vaguehope.morrigan.sqlitewrapper.DbException;

public class GoalSeekingDlnaPlayerTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	private static final String TEST_FILE_1_ID = "123456abcdef";
	private static final int TEST_FILE_1_SIZE = 5 * 1024 * 1024;
	private static final int TEST_FILE_1_DURATION = 123;
	private static final String TEST_FILE_1_URI = "http://10.20.30.40:12345/media/" + TEST_FILE_1_ID;
	private static final String TEST_COVER_1_URI = "http://10.20.30.40:12345/covers/" + TEST_FILE_1_ID;

	private static final String TEST_FILE_2_ID = "987654qwert";
	private static final int TEST_FILE_2_SIZE = 8 * 1024 * 1024;
	private static final int TEST_FILE_2_DURATION = 456;
	private static final String TEST_FILE_2_URI = "http://10.20.30.40:12345/media/" + TEST_FILE_2_ID;
	private static final String TEST_COVER_2_URI = "http://10.20.30.40:12345/covers/" + TEST_FILE_2_ID;

	private Config config;
	private DlnaPlayingParamsFactory dlnaPlayingParamsFactory;
	private PlayerRegister playerRegister;
	private PlayerStateStorage playerStateStorage;
	private ControlPoint controlPoint;
	private RemoteService avTransportSvc;
	private AvTransportActions avTransport;
	private RenderingControlActions renderingControl;
	private ScheduledExecutorService scheduledExecutor;
	private final Queue<Runnable> scheduledActions = new ConcurrentLinkedQueue<>();
	private TestMediaDb testDb;
	private GoalSeekingDlnaPlayer undertest;

	@Before
	public void before () throws Exception {
		this.config = new Config(this.tmp.getRoot());
		this.dlnaPlayingParamsFactory = mock(DlnaPlayingParamsFactory.class);
		this.playerRegister = mock(PlayerRegister.class);
		this.playerStateStorage = mock(PlayerStateStorage.class);
		this.controlPoint = mock(ControlPoint.class);
		this.scheduledExecutor = mock(ScheduledExecutorService.class);
		doAnswer((inv) -> {
				GoalSeekingDlnaPlayerTest.this.scheduledActions.add(inv.getArgument(0, Runnable.class));
				return null;
		}).when(this.scheduledExecutor).execute(any(Runnable.class));
		doAnswer((inv) -> {
				GoalSeekingDlnaPlayerTest.this.scheduledActions.add(inv.getArgument(0, Runnable.class));
				return null;
		}).when(this.scheduledExecutor).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
		makeTransportService();
		this.avTransport = mock(AvTransportActions.class);
		this.renderingControl = mock(RenderingControlActions.class);
		this.testDb = new TestMediaDb();
		this.undertest = new GoalSeekingDlnaPlayer(
				this.playerRegister, this.controlPoint, this.avTransportSvc,
				this.dlnaPlayingParamsFactory, this.scheduledExecutor,
				this.playerStateStorage,
				this.config,
				this.avTransport, this.renderingControl);
	}

	@Test
	public void itDoesNothingWhenThereIsNoInput () throws Exception {
		this.undertest.readEventQueue();
		assertEquals(PlayState.STOPPED, runEventLoop());
	}

	@Test
	public void itLoadsAndPlaysAndSkipsToNextTrack () throws Exception {
		// Initial renderer state.
		final TransportInfo transportInfo = mock(TransportInfo.class);
		when(this.avTransport.getTransportInfo()).thenReturn(transportInfo);
		when(transportInfo.getCurrentTransportState()).thenReturn(TransportState.STOPPED);
		when(transportInfo.getCurrentTransportStatus()).thenReturn(TransportStatus.OK);

		final MediaInfo mediaInfo = mock(MediaInfo.class);
		when(this.avTransport.getMediaInfo()).thenReturn(mediaInfo);

		final PositionInfo positionInfo = mock(PositionInfo.class);
		when(this.avTransport.getPositionInfo()).thenReturn(positionInfo);

		// start playback.
		final PlayItem item1 = makeItem();
		final DlnaPlayingParams item1Params = new DlnaPlayingParams(TEST_FILE_1_ID, TEST_FILE_1_URI, item1.getTrack().getTitle(), MediaFormat.MP3.toMimeType(), TEST_FILE_1_SIZE, TEST_COVER_1_URI, TEST_FILE_1_DURATION);
		this.undertest.dlnaPlay(item1, item1Params);

		// verify starts loading.
		assertEquals(PlayState.LOADING, runEventLoop());
		final InOrder ord = inOrder(this.avTransport);
		ord.verify(this.avTransport).stop();
		ord.verify(this.avTransport).getMediaInfo();
		ord.verify(this.avTransport).getTransportInfo();
		ord.verify(this.avTransport).getPositionInfo();
		ord.verify(this.avTransport).setUri(
				TEST_FILE_1_ID, TEST_FILE_1_URI, item1.getTrack().getTitle(),
				MediaFormat.MP3.toMimeType(), TEST_FILE_1_SIZE, TEST_COVER_1_URI, TEST_FILE_1_DURATION);
		ord.verify(this.avTransport).play();
		ord.verifyNoMoreInteractions();

		// renderer has the item.
		when(transportInfo.getCurrentTransportState()).thenReturn(TransportState.TRANSITIONING);
		when(mediaInfo.getCurrentURI()).thenReturn(TEST_FILE_1_URI);

		// rerun the loop, nothing should happen.
		for (int i = 0; i < 3; i++) {
			assertEquals(PlayState.LOADING, runEventLoop());
			ord.verify(this.avTransport).getMediaInfo();
			ord.verify(this.avTransport).getTransportInfo();
			ord.verify(this.avTransport).getPositionInfo();
			ord.verifyNoMoreInteractions();
		}

		// renderer finishes loading.
		when(transportInfo.getCurrentTransportState()).thenReturn(TransportState.PLAYING);
		when(positionInfo.getTrackDurationSeconds()).thenReturn((long) TEST_FILE_1_DURATION);
		when(positionInfo.getTrackElapsedSeconds()).thenReturn(1L);

		// verify playback started and that no other actions are taken.
		for (int i = 0; i < 3; i++) {
			assertEquals(PlayState.PLAYING, runEventLoop());
			ord.verify(this.avTransport).getMediaInfo();
			ord.verify(this.avTransport).getTransportInfo();
			ord.verify(this.avTransport).getPositionInfo();
			ord.verifyNoMoreInteractions();
		}

		// advance track position and verify track start was recorded.
		when(positionInfo.getTrackElapsedSeconds()).thenReturn(30L); // more than minimums.
		assertEquals(PlayState.PLAYING, runEventLoop());
		assertEquals(1, runScheduledExecutor());
		assertEquals(1, item1.getTrack().getStartCount());
		ord.verify(this.avTransport).getMediaInfo();
		ord.verify(this.avTransport).getTransportInfo();
		ord.verify(this.avTransport).getPositionInfo();
		ord.verifyNoMoreInteractions();

		// Skip to a new item.
		final PlayItem item2 = makeItem();
		final DlnaPlayingParams item2Params = new DlnaPlayingParams(TEST_FILE_2_ID, TEST_FILE_2_URI, item2.getTrack().getTitle(), MediaFormat.OGG.toMimeType(), TEST_FILE_2_SIZE, TEST_COVER_2_URI, TEST_FILE_2_DURATION);
		this.undertest.dlnaPlay(item2, item2Params);

		// verify starts loading.
		assertEquals(PlayState.LOADING, runEventLoop());
		ord.verify(this.avTransport).stop();
		ord.verify(this.avTransport).getMediaInfo();
		ord.verify(this.avTransport).getTransportInfo();
		ord.verify(this.avTransport).getPositionInfo();
		ord.verify(this.avTransport).setUri(
				TEST_FILE_2_ID, TEST_FILE_2_URI, item2.getTrack().getTitle(),
				MediaFormat.OGG.toMimeType(), TEST_FILE_2_SIZE, TEST_COVER_2_URI, TEST_FILE_2_DURATION);
		ord.verify(this.avTransport).play();
		ord.verifyNoMoreInteractions();

		// renderer finishes loading and some time passes.
		when(mediaInfo.getCurrentURI()).thenReturn(TEST_FILE_2_URI);
		when(transportInfo.getCurrentTransportState()).thenReturn(TransportState.PLAYING);
		when(positionInfo.getTrackDurationSeconds()).thenReturn((long) TEST_FILE_2_DURATION);
		when(positionInfo.getTrackElapsedSeconds()).thenReturn(30L);

		// verify playback started and that no other actions are taken.
		for (int i = 0; i < 3; i++) {
			assertEquals(PlayState.PLAYING, runEventLoop());
			ord.verify(this.avTransport).getMediaInfo();
			ord.verify(this.avTransport).getTransportInfo();
			ord.verify(this.avTransport).getPositionInfo();
			ord.verifyNoMoreInteractions();
		}
	}

	private PlayState runEventLoop () throws DlnaException {
		this.undertest.readEventQueue();
		return this.undertest.readStateAndSeekGoal();
	}

	private int runScheduledExecutor () {
		int n = 0;
		Runnable a;
		while((a = this.scheduledActions.poll()) != null) {
			a.run();
			n += 1;
		}
		return n;
	}

	private PlayItem makeItem () throws MorriganException, DbException {
		final MediaItem track = this.testDb.addTestTrack();
		return new PlayItem(this.testDb, track);
	}

	@SuppressWarnings("unused")
	private void makeTransportService () throws Exception {
		final InetAddress addr = InetAddress.getLocalHost();

		this.avTransportSvc = new RemoteService(
				ServiceType.valueOf("urn:schemas-upnp-org:service:AVTransport:1"),
				ServiceId.valueOf("urn:upnp-org:serviceId:AVTransport"),
				new URI("/dev/8bee114e-919e-1603-ffff-ffffa44fffff/svc/upnp-org/AVTransport/desc"),
				new URI("/dev/8bee114e-919e-1603-ffff-ffffa44fffff/svc/upnp-org/AVTransport/action"),
				new URI("/dev/8bee114e-919e-1603-ffff-ffffa44fffff/svc/upnp-org/AVTransport/event"));
		RemoteDeviceIdentity identity = new RemoteDeviceIdentity(
				new UDN("uuid:8bee114e-919e-1603-ffff-ffffa44fffff"),
				60,
				new URL("http://" + addr.getHostAddress() + ":12345/dev/8bee114e-919e-1603-ffff-ffffa44fffff/desc"),
				null,
				addr);
		DeviceType type = DeviceType.valueOf("urn:schemas-upnp-org:device:MediaRenderer:1");
		DeviceDetails details = new DeviceDetails("Very Friendly Name");
		new RemoteDevice(identity, type, details, this.avTransportSvc);
	}

}
