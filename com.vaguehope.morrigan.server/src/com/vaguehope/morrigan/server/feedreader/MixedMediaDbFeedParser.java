package com.vaguehope.morrigan.server.feedreader;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMixedMediaItem;
import com.vaguehope.morrigan.model.media.IMixedMediaItem.MediaType;
import com.vaguehope.morrigan.model.media.IRemoteMixedMediaDb;
import com.vaguehope.morrigan.model.media.MediaTagType;
import com.vaguehope.morrigan.server.util.XmlHelper;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.util.StringHelper;
import com.vaguehope.sqlitewrapper.DbException;

public class MixedMediaDbFeedParser extends DefaultHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IRemoteMixedMediaDb rmmdb;
	private final TaskEventListener taskEventListener;
	private final Stack<String> stack;

	MixedMediaDbFeedParser(final IRemoteMixedMediaDb rmmdb, final TaskEventListener taskEventListener) {
		this.taskEventListener = taskEventListener;
		this.stack = new Stack<String>();
		this.rmmdb = rmmdb;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static class TagAttrs {
		final MediaTagType type;
		final String cls;
		final Date modified;
		final boolean deleted;
		public TagAttrs (final MediaTagType type, final String cls, final Date modified, final boolean deleted) {
			this.type = type;
			this.cls = cls;
			this.modified = modified;
			this.deleted = deleted;
		}
	}

	private long entryCount = 0;
	private long entriesProcessed = 0;
	private int progress = 0;

	private IMixedMediaItem currentItem;
	private StringBuilder currentText;

	private Date enabledLastModified;
	private final List<String> tagValues = new ArrayList<String>();
	private final List<TagAttrs> tagAttrs = new ArrayList<TagAttrs>();

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
		this.stack.push(localName);
		if (this.stack.size() == 2 && localName.equals("entry")) {
			this.currentItem = this.rmmdb.getDbLayer().getNewT(null);
			this.enabledLastModified = null;
			this.tagValues.clear();
			this.tagAttrs.clear();
		}
		else if (this.stack.size() == 3 && localName.equals("link")) {
			String relVal = attributes.getValue("rel");
			if (relVal != null && relVal.equals("self")) {
				String hrefVal = attributes.getValue("href");
				if (hrefVal != null && hrefVal.length() > 0) {
					this.currentItem.setRemoteLocation(hrefVal);
					try {
						String remotePath = URLDecoder.decode(hrefVal, "UTF-8");
						this.currentItem.setFilepath(remotePath);
					} catch (UnsupportedEncodingException e) {
						throw new SAXException(e);
					}
				}
			}
		}
		else if (this.stack.size() == 3 && localName.equals("enabled")) {
			final String enabledLastModifiedString = StringHelper.trimToNull(attributes.getValue("m"));
			this.enabledLastModified = enabledLastModifiedString == null ? null : new Date(Long.parseLong(enabledLastModifiedString));
		}
		else if (this.stack.size() == 3 && localName.equals("tag")) {
			final String typeString = attributes.getValue("t");
			final String cls = StringHelper.trimToNull(attributes.getValue("c"));
			final String modifiedString = StringHelper.trimToNull(attributes.getValue("m"));
			final String deletedString = StringHelper.trimToNull(attributes.getValue("d"));

			final MediaTagType type = typeString == null ? null : MediaTagType.getFromIndex(Integer.parseInt(typeString));
			final Date modified = modifiedString == null ? null : new Date(Long.parseLong(modifiedString));
			final boolean deleted = "true".equalsIgnoreCase(deletedString);

			this.tagAttrs.add(new TagAttrs(type, cls, modified, deleted));
		}

		// If we need a new StringBuilder, make one.
		if (this.currentText == null || this.currentText.length() > 0) {
			this.currentText = new StringBuilder();
		}
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		if (this.stack.size() == 2 && localName.equals("count")) {
			this.entryCount = Long.parseLong(this.currentText.toString());
		}
		else if (this.stack.size() == 2 && localName.equals("entry")) {
			try {
				// Returned item should have DB row ID filed in.
				IMixedMediaItem realItem = this.rmmdb.updateItem(this.currentItem);
				if (realItem.getDbRowId() < 0) throw new IllegalStateException("Can not add tags without DB row id for '"+realItem+"'.");
				if (this.tagValues.size() > 0) {
					if (this.tagValues.size() != this.tagAttrs.size()) {
    					throw new IllegalArgumentException("Unbalanced tag lists.");
    				}

					// addTag() will update modified/deleted flags as needed.
					for (int i = 0; i < this.tagValues.size(); i++) {
    					final String value = this.tagValues.get(i);
    					final TagAttrs attrs = this.tagAttrs.get(i);
    					this.rmmdb.addTag(realItem, value, attrs.type, attrs.cls, attrs.modified, attrs.deleted);
    				}
				}
			}
			catch (MorriganException e) {
				throw new SAXException(e);
			}
			catch (DbException e) {
				throw new SAXException(e);
			}

			if (this.taskEventListener != null) {
				this.entriesProcessed++;
				int p = (int) ((this.entriesProcessed * 100) / this.entryCount);
				if (p > this.progress) {
					this.taskEventListener.worked(p - this.progress);
					this.progress = p;
				}
			}
		}
		else if (this.stack.size() == 3 && localName.equals("type")) {
			int v = Integer.parseInt(this.currentText.toString());
			MediaType type = MediaType.parseInt(v);
			this.currentItem.setMediaType(type);
		}
		else if (this.stack.size() == 3 && localName.equals("dateadded")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateAdded(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"' for item '"+this.currentItem.getFilepath()+"'.", e);
			}
		}
		else if (this.stack.size() == 3 && localName.equals("datelastmodified")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateLastModified(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"' for item '"+this.currentItem.getFilepath()+"'.", e);
			}
		}
		else if (this.stack.size() == 3 && localName.equals("duration")) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setDuration(v);
		}
		else if (this.stack.size() == 3 && localName.equals("hash")) {
			BigInteger v = new BigInteger(this.currentText.toString().trim(), 16);
			this.currentItem.setHashcode(v);
		}
		else if (this.stack.size() == 3 && localName.equals("enabled")) {
			boolean v = Boolean.parseBoolean(this.currentText.toString().trim());
			this.currentItem.setEnabled(v, this.enabledLastModified);
		}
		else if (this.stack.size() == 3 && localName.equals("missing")) {
			boolean v = Boolean.parseBoolean(this.currentText.toString().trim());
			this.currentItem.setMissing(v);
		}
		else if (this.stack.size() == 3 && localName.equals("startcount")) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setStartCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals("endcount")) {
			long v = Long.parseLong(this.currentText.toString());
			this.currentItem.setEndCount(v);
		}
		else if (this.stack.size() == 3 && localName.equals("datelastplayed")) {
			try {
				Date d = XmlHelper.getIso8601UtcDateFormatter().parse(this.currentText.toString());
				this.currentItem.setDateLastPlayed(d);
			}
			catch (Exception e) {
				throw new SAXException(flattenStack(this.stack) + " Exception parsing date '"+this.currentText.toString()+"' for item '"+this.currentItem.getFilepath()+"'.", e);
			}
		}
		else if (this.stack.size() == 3 && localName.equals("width")) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setWidth(v);
		}
		else if (this.stack.size() == 3 && localName.equals("height")) {
			int v = Integer.parseInt(this.currentText.toString());
			this.currentItem.setHeight(v);
		}
		else if (this.stack.size() == 3 && localName.equals("tag")) {
			String v = this.currentText.toString();
			this.tagValues.add(v);
		}

		this.stack.pop();
	} // endElement().

	@Override
	public void characters(final char[] ch, final int start, final int length) {
		this.currentText.append( ch, start, length );
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static String flattenStack (final Stack<String> stack) {
		StringBuilder sb = new StringBuilder();

		for (String s : stack) {
			sb.append(s);
			sb.append('/');
		}

		return sb.toString();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
