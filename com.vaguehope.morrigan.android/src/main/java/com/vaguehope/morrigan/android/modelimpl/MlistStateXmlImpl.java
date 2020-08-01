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


import java.util.List;

import org.xml.sax.SAXException;

import com.vaguehope.morrigan.android.R;
import com.vaguehope.morrigan.android.helper.XmlParser;
import com.vaguehope.morrigan.android.model.MlistReference;
import com.vaguehope.morrigan.android.model.MlistState;
import com.vaguehope.morrigan.android.model.ServerReference;

public class MlistStateXmlImpl extends XmlParser implements MlistState {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String SELF = "self";
	private static final String TITLE = "title";
	private static final String COUNT = "count";
	private static final String DURATION = "duration";
	private static final String DURATIONCOMPLETE = "durationcomplete";

	private final static String[] nodes = new String[] {
		TITLE,
		COUNT,
		DURATION,
		DURATIONCOMPLETE
		};

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final MlistReference mlistReference;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public MlistStateXmlImpl (final String xmlString, final ServerReference host) throws SAXException {
		super(xmlString, nodes, host);
		this.mlistReference = null;
	}

	public MlistStateXmlImpl (final String xmlString, final MlistReference mlistReference) throws SAXException {
		super(xmlString, nodes, mlistReference.getServerReference());
		this.mlistReference = mlistReference;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getId() {
		throw new UnsupportedOperationException("Not used.");
	}

	@Override
	public int getImageResource() {
		return R.drawable.db;
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public MlistReference getMlistReference () {
		return this.mlistReference;
	}

	@Override
	public String getRelativePath () {
		return this.getNode(SELF);
	}

	@Override
	public String getTitle() {
		return this.getNode(TITLE);
	}

	@Override
	public int getCount() {
		return this.getNodeInt(COUNT);
	}

	@Override
	public long getDuration() {
		return this.getNodeLong(DURATION);
	}

	@Override
	public boolean isDurationComplete() {
		return this.getNodeBoolean(DURATIONCOMPLETE);
	}

	@Override
	public List<String> getSrcs () {
		return getRepeatingNode("src");
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	@Override
	public String getUiTitle () {
		return getTitle();
	}

}
