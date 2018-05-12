/*
 * Copyright 2010 Fae Hutter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.vaguehope.morrigan.android.modelimpl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.vaguehope.morrigan.android.model.MlistItem;
import com.vaguehope.morrigan.android.model.PlayState;
import com.vaguehope.morrigan.android.model.PlayerReference;
import com.vaguehope.morrigan.android.model.PlayerState;
import com.vaguehope.morrigan.android.playback.MediaTag;
import com.vaguehope.morrigan.android.playback.MediaTagType;

public class PlayerStateXmlImpl implements PlayerState, MlistItem, ContentHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public static final String PLAYERID = "playerid";
	public static final String PLAYERNAME = "playername";
	public static final String PLAYORDER = "playorder";
	public static final String PLAYSTATE = "playstate";
	public static final String PLAYPOSITION = "playposition";

	public static final String TRACKLINKNAME = "track"; // Name of link rel attribute.
	public static final String TRACKTITLE = "tracktitle";
	public static final String TRACKFILE = "trackfile";
	public static final String TRACKFILENAME = "trackfilename";
	public static final String TRACKDURATION = "trackduration";

	public static final String TRACKFILESIZE = "trackfilesize";
	public static final String TRACKHASHCODE = "trackhash";
	public static final String TRACKENABLED = "trackenabled";
	public static final String TRACKMISSING = "trackmissing";

	public static final String TRACKSTARTCOUNT = "trackstartcount";
	public static final String TRACKENDCOUNT = "trackendcount";
	public static final String TRACKTAG = "tracktag";

	public static final String LISTID = "listid";
//	public static final String LISTURL = "list"; // Because its a link.
	public static final String LISTTITLE = "listtitle";

	public static final String QUEUEDURATION = "queueduration";
	public static final String QUEUELENGTH = "queuelength";

	public static final String MONITOR = "monitor";
	private static final String MANUAL_TAG = "0";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final PlayerReference playerReference;

	private String playerId;
	private String playerName;
	private int playerOrder;
	private PlayState playerState;
	private int playerPosition;

	private String trackRelativeUrl;
	private String trackTitle;
	private String trackFile;
	private String trackFileName;
	private int trackDuration;
	private long trackFileSize;
	private BigInteger trackHashCode;
	private boolean trackEnabled;
	private boolean trackMissing;
	private int trackStartCount;
	private int trackEndCount;
	private Collection<MediaTag> trackTags;

	private String listId;
	private String listUrl;
	private String listTitle;

	private int queueLength;
	private long queueDuration;

	Map<Integer, String> monitors;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public PlayerStateXmlImpl (final InputStream dataIs, final PlayerReference playerReference) throws SAXException {
		this.playerReference = playerReference;
		this.monitors = new LinkedHashMap<Integer, String>();

		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		try {
			sp = spf.newSAXParser();
			XMLReader xmlReader = sp.getXMLReader();
			xmlReader.setContentHandler(this);
			try {
				xmlReader.parse(new InputSource(dataIs));
			}
			catch (IOException e) {
				throw new SAXException(e);
			}
		}
		catch (ParserConfigurationException e) {
			throw new SAXException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public PlayerReference getPlayerReference () {
		return this.playerReference;
	}

	@Override
	public String getTitle () {
		return getTrackTitle();
	}

	@Override
	public int getImageResource () {
		return PlayerStateBasicImpl.getImageResource(getPlayState());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getId () {
		return this.playerId;
	}

	@Override
	public String getName () {
		return this.playerName;
	}

	@Override
	public PlayState getPlayState () {
		return this.playerState;
	}

	@Override
	public int getPlayOrder () {
		return this.playerOrder;
	}

	@Override
	public int getPlayerPosition () {
		return this.playerPosition;
	}

	@Override
	public String getListTitle () {
		return this.listTitle;
	}

	@Override
	public String getListId () {
		return this.listId;
	}

	@Override
	public String getListUrl () {
		return this.listUrl;
	}

	@Override
	public String getTrackRelativeUrl () {
		return this.trackRelativeUrl;
	}

	@Override
	public String getTrackTitle () {
		return this.trackTitle;
	}

	@Override
	public String getTrackFile () {
		return this.trackFile;
	}

	@Override
	public String getTrackFileName () {
		return this.trackFileName;
	}

	@Override
	public int getTrackDuration () {
		return this.trackDuration;
	}

	public long getTrackFileSize () {
		return this.trackFileSize;
	}

	@Override
	public BigInteger getTrackHashCode () {
		return this.trackHashCode;
	}

	@Override
	public boolean getTrackEnabled () {
		return this.trackEnabled;
	}

	@Override
	public boolean getTrackMissing () {
		return this.trackMissing;
	}

	@Override
	public int getTrackStartCount () {
		return this.trackStartCount;
	}

	@Override
	public int getTrackEndCount () {
		return this.trackEndCount;
	}

	@Override
	public Collection<MediaTag> getTrackTags () {
		return this.trackTags;
	}

	@Override
	public MlistItem getItem () {
		return this;
	}

	@Override
	public int getQueueLength () {
		return this.queueLength;
	}

	@Override
	public long getQueueDuration () {
		return this.queueDuration;
	}

	@Override
	public Map<Integer, String> getMonitors () {
		return Collections.unmodifiableMap(this.monitors);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	MlistItem methods.

	@Override
	public int getType () {
		return -1;
	}

	@Override
	public String getRelativeUrl () {
		return getTrackRelativeUrl();
	}

	@Override
	public String getFileName () {
		return getTrackFileName();
	}

	@Override
	public long getFileSize () {
		return getTrackFileSize();
	}

	@Override
	public long getLastModified () {
		throw new UnsupportedOperationException("Not implemented.");
	}
	@Override
	public long getLastPlayed () {
		throw new UnsupportedOperationException("Not implemented.");
	}

	@Override
	public BigInteger getOriginalHashCode () {
		return getTrackHashCode(); // FIXME Currently player is always the original hash.
	}

	@Override
	public BigInteger getHashCode () {
		return getTrackHashCode();
	}

	@Override
	public boolean isEnabled () {
		return getTrackEnabled();
	}

	@Override
	public boolean isMissing () {
		return getTrackMissing();
	}

	@Override
	public int getDuration () {
		return getTrackDuration();
	}

	@Override
	public int getStartCount () {
		return getTrackStartCount();
	}

	@Override
	public int getEndCount () {
		return getTrackEndCount();
	}

	@Override
	public Collection<MediaTag> getTags () {
		return getTrackTags();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final Map<String, String> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<String, String>());

	private final Stack<String> stack = new Stack<String>();
	private final Stack<Map<String, String>> currentAttributes = new Stack<Map<String, String>>();
	private StringBuilder currentText;

	@Override
	public void startElement (final String uri, final String localName, final String qName, final Attributes attr) throws SAXException {
		this.stack.push(localName);
		Map<String, String> atMap = EMPTY_MAP;

		if (this.stack.size() == 2 && localName.equals("link")) {
			String relVal = attr.getValue("rel");
			if (relVal != null && relVal.equals("list")) {
				String hrefVal = attr.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.listUrl = this.playerReference.getServerReference().getBaseUrl() + hrefVal;
				}
			}
			else if (relVal != null && relVal.equals(TRACKLINKNAME)) {
				String hrefVal = attr.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.trackRelativeUrl = hrefVal;
				}
			}
		}
		else if (attr.getLength() > 0 && this.stack.size() == 2 && localName.equals(TRACKTAG)) {
			atMap = new HashMap<String, String>();
			for (int i = 0; i < attr.getLength(); i++)
				atMap.put(attr.getLocalName(i), attr.getValue(i));
		}

		// If we need a new StringBuilder, make one.
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
		this.currentAttributes.push(atMap);
	}

	@Override
	public void endElement (final String uri, final String localName, final String qName) throws SAXException {
		Map<String, String> attr = this.currentAttributes.pop();

		if (this.stack.size() == 2) {
			if (localName.equals(PLAYERID)) {
				this.playerId = this.currentText.toString();
			}
			else if (localName.equals(PLAYERNAME)) {
				this.playerName = this.currentText.toString();
			}
			else if (localName.equals(PLAYORDER)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.playerOrder = v;
			}
			else if (localName.equals(PLAYSTATE)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.playerState = PlayState.parseN(v);
			}
			else if (localName.equals(PLAYPOSITION)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.playerPosition = v;
			}
			else if (localName.equals(TRACKTITLE)) {
				this.trackTitle = this.currentText.toString();
			}
			else if (localName.equals(TRACKFILE)) {
				this.trackFile = this.currentText.toString();
			}
			else if (localName.equals(TRACKFILENAME)) {
				this.trackFileName = this.currentText.toString();
			}
			else if (localName.equals(TRACKDURATION)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.trackDuration = v;
			}
			else if (localName.equals(TRACKFILESIZE)) {
				long v = Long.parseLong(this.currentText.toString());
				this.trackFileSize = v;
			}
			else if (localName.equals(TRACKHASHCODE)) {
				BigInteger v = new BigInteger(this.currentText.toString(), 16);
				this.trackHashCode = v;
			}
			else if (localName.equals(TRACKENABLED)) {
				boolean v = Boolean.parseBoolean(this.currentText.toString());
				this.trackEnabled = v;
			}
			else if (localName.equals(TRACKMISSING)) {
				boolean v = Boolean.parseBoolean(this.currentText.toString());
				this.trackMissing = v;
			}
			else if (localName.equals(TRACKSTARTCOUNT)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.trackStartCount = v;
			}
			else if (localName.equals(TRACKENDCOUNT)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.trackEndCount = v;
			}
			else if (localName.equals(TRACKTAG)) {
				final String tag = this.currentText.toString();
				final String t = attr.get("t");
				final String cls = attr.get("c");
				final String m = attr.get("m");
				final String d = attr.get("d");
				final MediaTagType type = MediaTagType.getFromNumber(Integer.parseInt(t));
				final long modified = m != null ? Long.parseLong(m) : -1;
				final boolean deleted = Boolean.parseBoolean(d);
				final MediaTag mediaTag = new MediaTag(tag, cls, type, modified, deleted);

				if (this.trackTags == null) this.trackTags = new LinkedList<MediaTag>();
				this.trackTags.add(mediaTag);
			}
			else if (localName.equals(LISTID)) {
				this.listId = this.currentText.toString();
			}
			else if (localName.equals(LISTTITLE)) {
				this.listTitle = this.currentText.toString();
			}
			else if (localName.equals(QUEUEDURATION)) {
				long v = Long.parseLong(this.currentText.toString());
				this.queueDuration = v;
			}
			else if (localName.equals(QUEUELENGTH)) {
				int v = Integer.parseInt(this.currentText.toString());
				this.queueLength = v;
			}
			else if (localName.equals(MONITOR)) {
				String data = this.currentText.toString();
				int indexOf = data.indexOf(":");
				String idString = data.substring(0, indexOf);
				String desString = data.substring(indexOf + 1);
				Integer id = Integer.valueOf(idString);
				this.monitors.put(id, desString);
			}
		}

		this.stack.pop();
	}

	@Override
	public void characters (final char[] ch, final int start, final int length) throws SAXException {
		this.currentText.append(ch, start, length);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public void endDocument () throws SAXException { /* UNUSED */}

	@Override
	public void endPrefixMapping (final String prefix) throws SAXException { /* UNUSED */}

	@Override
	public void ignorableWhitespace (final char[] ch, final int start, final int length) throws SAXException { /* UNUSED */}

	@Override
	public void processingInstruction (final String target, final String data) throws SAXException { /* UNUSED */}

	@Override
	public void setDocumentLocator (final Locator locator) { /* UNUSED */}

	@Override
	public void skippedEntity (final String name) throws SAXException { /* UNUSED */}

	@Override
	public void startDocument () throws SAXException { /* UNUSED */}

	@Override
	public void startPrefixMapping (final String prefix, final String uri) throws SAXException { /* UNUSED */}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
