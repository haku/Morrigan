package com.vaguehope.morrigan.dlna.players;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class AvSubscriberTest {

	@Before
	public void before() throws Exception {
		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		final ConsoleAppender<ILoggingEvent> appender = (ConsoleAppender<ILoggingEvent>) context.getLogger(Logger.ROOT_LOGGER_NAME)
				.getAppender("CONSOLE");
		appender.clearAllFilters();
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void itDoesSomething() throws Exception {
		AbstractDlnaPlayer dlnaPlayer = mock(AbstractDlnaPlayer.class);
		AvSubscriber undertest = new AvSubscriber(dlnaPlayer, null, null, 0);

		GENASubscription event = mock(GENASubscription.class);
		when(event.getCurrentSequence()).thenReturn(new UnsignedIntegerFourBytes(0));
		when(event.getCurrentValues()).thenReturn(Map.of("LastChange", XML));
		undertest.eventReceived(event);
	}

	// here from testing a bug in jupnp.
	@SuppressWarnings("unused")
	@Test
	public void itAVTransportLastChangeParser() throws Exception {
		new AVTransportLastChangeParser();
	}

	//97aaaa31-3aa6-6aa4-12f3-caaaaaaaaaf0-AVTransport
	final static String XML = "<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\">\n"
			+ "  <InstanceID val=\"0\">\n"
			+ "    <CurrentPlayMode val=\"NORMAL\"/>\n"
			+ "    <RecordStorageMedium val=\"NOT_IMPLEMENTED\"/>\n"
			+ "    <CurrentTrackURI val=\"http://192.168.1.9:29085/757465ad0aaaaaaaaaaaaaaaaaaaaa0413655953-261aaaaaaaaaaaaaaaaaa8a060ba712d_common_audio_only_mp3\"/>\n"
			+ "    <CurrentTrackDuration val=\"00:00:00\"/>\n"
			+ "    <CurrentRecordQualityMode val=\"NOT_IMPLEMENTED\"/>\n"
			+ "    <CurrentMediaDuration val=\"00:00:00\"/>\n"
			+ "    <AVTransportURI val=\"http://192.168.1.9:29085/757465aaaaaaaaaaaaaaaaaaaaaaaa0413655953-261aaaaaaaaaaaaaaaaaa8a060ba712d_common_audio_only_mp3\"/>\n"
			+ "    <TransportState val=\"STOPPED\"/>\n"
			+ "    <CurrentTrackMetaData val=\"&lt;DIDL-Lite xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/&quot; xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; "
			+ "xmlns:upnp=&quot;urn:schemas-upnp-org:metadata-1-0/upnp/&quot; xmlns:dlna=&quot;urn:schemas-dlna-org:metadata-1-0/&quot; xmlns:sec=&quot;http://www.sec.co.kr/&quot; "
			+ "xmlns:xbmc=&quot;urn:schemas-xbmc-org:metadata-1-0/&quot;&gt;&lt;item id=&quot;http://192.168.1.9:29085/757465ad0aaaaaaaaaaaaaaaaaaaaa0413655953-261aaaaaaaaaaaaaaaaaa8a060ba712d_common_audio_only_mp3&quot; "
			+ "parentID=&quot;&quot; restricted=&quot;1&quot;&gt;&lt;dc:title&gt;PMMM_Dely.wmv&lt;/dc:title&gt;&lt;dc:creator&gt;Unknown&lt;/dc:creator&gt;&lt;upnp:artist&gt;Unknown&lt;/upnp:artist&gt;&lt;upnp:artist "
			+ "role=&quot;Performer&quot;&gt;Unknown&lt;/upnp:artist&gt;&lt;upnp:artist "
			+ "role=&quot;AlbumArtist&quot;&gt;Unknown&lt;/upnp:artist&gt;&lt;dc:publisher&gt;Unknown&lt;/dc:publisher&gt;&lt;upnp:genre&gt;Unknown&lt;/upnp:genre&gt;&lt;upnp:albumArtURI "
			+ "dlna:profileID=&quot;JPEG_TN&quot;&gt;http://192.168.1.10:1581/thumb?path=image%3A%2F%2FDefaultAlbumCover.png%2F&lt;/upnp:albumArtURI&gt;"
			+ "&lt;upnp:lastPlaybackTime&gt;1969-12-31T23:59:59+04:36&lt;/upnp:lastPlaybackTime&gt;&lt;upnp:episodeSeason&gt;0&lt;/upnp:episodeSeason&gt;&lt;xbmc:rating&gt;0.0&lt;/xbmc:rating&gt;"
			+ "&lt;xbmc:userrating&gt;0&lt;/xbmc:userrating&gt;&lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;\"/>\n"
			+ "    <NextAVTransportURI val=\"\"/>\n"
			+ "    <PossibleRecordQualityModes val=\"NOT_IMPLEMENTED\"/>\n"
			+ "    <CurrentTrack val=\"0\"/>\n"
			+ "    <NextAVTransportURIMetaData val=\"\"/>\n"
			+ "    <PlaybackStorageMedium val=\"NONE\"/>\n"
			+ "    <CurrentTransportActions val=\"Play,Pause,Stop,Seek,Next,Previous\"/>\n"
			+ "    <RecordMediumWriteStatus val=\"NOT_IMPLEMENTED\"/>\n"
			+ "    <PossiblePlaybackStorageMedia val=\"NONE,NETWORK,HDD,CD-DA,UNKNOWN\"/>\n"
			+ "    <AVTransportURIMetaData val=\"&lt;DIDL-Lite xmlns=&quot;urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/&quot; xmlns:dc=&quot;http://purl.org/dc/elements/1.1/&quot; "
			+ "xmlns:upnp=&quot;urn:schemas-upnp-org:metadata-1-0/upnp/&quot; xmlns:dlna=&quot;urn:schemas-dlna-org:metadata-1-0/&quot; xmlns:sec=&quot;http://www.sec.co.kr/&quot; "
			+ "xmlns:xbmc=&quot;urn:schemas-xbmc-org:metadata-1-0/&quot;&gt;&lt;item id=&quot;http://192.168.1.9:29085/757465ad0aaaaaaaaaaaaaaaaaaaaa0413655953-261aaaaaaaaaaaaaaaaaa8a060ba712d_common_audio_only_mp3&quot; "
			+ "parentID=&quot;&quot; restricted=&quot;1&quot;&gt;&lt;dc:title&gt;PMMM_Dely.wmv&lt;/dc:title&gt;&lt;dc:creator&gt;Unknown&lt;/dc:creator&gt;&lt;upnp:artist&gt;Unknown&lt;/upnp:artist&gt;&lt;upnp:artist "
			+ "role=&quot;Performer&quot;&gt;Unknown&lt;/upnp:artist&gt;&lt;upnp:artist "
			+ "role=&quot;AlbumArtist&quot;&gt;Unknown&lt;/upnp:artist&gt;&lt;dc:publisher&gt;Unknown&lt;/dc:publisher&gt;&lt;upnp:genre&gt;Unknown&lt;/upnp:genre&gt;&lt;upnp:albumArtURI "
			+ "dlna:profileID=&quot;JPEG_TN&quot;&gt;http://192.168.1.10:1581/thumb?path=image%3A%2F%2FDefaultAlbumCover.png%2F&lt;/upnp:albumArtURI&gt;"
			+ "&lt;upnp:lastPlaybackTime&gt;1969-12-31T23:59:59+04:36&lt;/upnp:lastPlaybackTime&gt;&lt;upnp:episodeSeason&gt;0&lt;/upnp:episodeSeason&gt;&lt;xbmc:rating&gt;0.0&lt;/xbmc:rating&gt;"
			+ "&lt;xbmc:userrating&gt;0&lt;/xbmc:userrating&gt;&lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;\"/>\n"
			+ "    <NumberOfTracks val=\"0\"/>\n"
			+ "    <PossibleRecordStorageMedia val=\"NOT_IMPLEMENTED\"/>\n"
			+ "    <TransportStatus val=\"OK\"/>\n"
			+ "    <TransportPlaySpeed val=\"1\"/>\n"
			+ "  </InstanceID>\n"
			+ "</Event>";

}
