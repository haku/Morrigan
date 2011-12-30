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

package com.vaguehope.morrigan.android.state;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.ServerReferenceList;
import com.vaguehope.morrigan.android.model.impl.ServerReferenceImpl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ConfigDb extends SQLiteOpenHelper implements ServerReferenceList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final String DB_NAME = "config";
	private static final int DB_VERSION = 2;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String TBL_HOSTS = "hosts";
	private static final String TBL_HOSTS_ID = "_id";
	private static final String TBL_HOSTS_NAME = "name";
	private static final String TBL_HOSTS_URL = "url";
	private static final String TBL_HOSTS_PASS = "pass";
	
	private static final String TBL_HOSTS_CREATE =
			"CREATE TABLE " + TBL_HOSTS + " ("
					+ TBL_HOSTS_ID + " integer primary key autoincrement,"
					+ TBL_HOSTS_NAME + " text,"
					+ TBL_HOSTS_URL + " text,"
					+ TBL_HOSTS_PASS + " text"
					+ ");";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public ConfigDb (Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	@Override
	public void onCreate (SQLiteDatabase db) {
		db.execSQL(TBL_HOSTS_CREATE);
	}
	
	@Override
	public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion <= 1) {
			db.execSQL("ALTER TABLE " + TBL_HOSTS + " ADD COLUMN " + TBL_HOSTS_NAME + " text;");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServerReferenceList methods.
	
	@Override
	public List<? extends Artifact> getArtifactList () {
		return getServerReferenceList();
	}
	
	@Override
	public List<? extends ServerReference> getServerReferenceList () {
		SQLiteDatabase db = null;
		try {
			db = this.getReadableDatabase();
			db.beginTransaction();
			try {
				Cursor c = null;
				try {
					List<ServerReference> ret = new LinkedList<ServerReference>();
					
					c = db.query(true, TBL_HOSTS,
							new String[] { TBL_HOSTS_ID, TBL_HOSTS_NAME, TBL_HOSTS_URL, TBL_HOSTS_PASS },
							null, null, null, null,
							TBL_HOSTS_NAME + " DESC", null);
					
					if (c.moveToFirst()) {
						int col_id = c.getColumnIndex(TBL_HOSTS_ID);
						int col_name = c.getColumnIndex(TBL_HOSTS_NAME);
						int col_url = c.getColumnIndex(TBL_HOSTS_URL);
						int col_pass = c.getColumnIndex(TBL_HOSTS_PASS);
						
						do {
							int id = c.getInt(col_id);
							String name = c.getString(col_name);
							String url = c.getString(col_url);
							String pass = c.getString(col_pass);
							ServerReferenceImpl i = new ServerReferenceImpl(id, name, url, pass);
							ret.add(i);
						}
						while (c.moveToNext());
					}
					
					return Collections.unmodifiableList(ret);
				}
				finally {
					if (c != null) c.close();
				}
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			if (db != null) db.close();
		}
	}
	
	public ServerReference getServer (int id) {
		if (id < 0) throw new IllegalArgumentException("ID is < 0.");
		
		SQLiteDatabase db = null;
		try {
			db = this.getReadableDatabase();
			db.beginTransaction();
			try {
				Cursor c = null;
				try {
					c = db.query(true, TBL_HOSTS,
							new String[] { TBL_HOSTS_ID, TBL_HOSTS_NAME, TBL_HOSTS_URL, TBL_HOSTS_PASS },
							TBL_HOSTS_ID + "=?", new String[] { String.valueOf(id) },
							null, null,
							TBL_HOSTS_NAME + " DESC", null
							);
					if (c.moveToFirst()) {
						int col_id = c.getColumnIndex(TBL_HOSTS_ID);
						int col_name = c.getColumnIndex(TBL_HOSTS_NAME);
						int col_url = c.getColumnIndex(TBL_HOSTS_URL);
						int col_pass = c.getColumnIndex(TBL_HOSTS_PASS);
						
						int inId = c.getInt(col_id);
						String name = c.getString(col_name);
						String url = c.getString(col_url);
						String pass = c.getString(col_pass);
						ServerReferenceImpl srvRef = new ServerReferenceImpl(inId, name, url, pass);
						
						return srvRef;
					}
					throw new UnsupportedOperationException("ID '" + id + "' not found.");
				}
				finally {
					if (c != null) c.close();
				}
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			if (db != null) db.close();
		}
	}
	
	public void addServer (ServerReference sr) {
		if (sr.getName() == null || sr.getName().length() < 1) throw new IllegalArgumentException("Illegal name.");
		if (sr.getBaseUrl() == null || sr.getBaseUrl().length() < 1) throw new IllegalArgumentException("Illegal baseUrl.");
		
		ContentValues values = new ContentValues();
		values.put(TBL_HOSTS_NAME, sr.getName());
		values.put(TBL_HOSTS_URL, sr.getBaseUrl());
		values.put(TBL_HOSTS_PASS, sr.getPass());
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();
			try {
				db.insert(TBL_HOSTS, null, values);
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			if (db != null) db.close();
		}
	}
	
	public void updateServer (ServerReference sr) {
		ContentValues values = new ContentValues();
		values.put(TBL_HOSTS_NAME, sr.getName());
		values.put(TBL_HOSTS_URL, sr.getBaseUrl());
		values.put(TBL_HOSTS_PASS, sr.getPass());
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();
			try {
				db.update(TBL_HOSTS, values, TBL_HOSTS_ID + "=?", new String[] { String.valueOf(sr.getId()) });
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			if (db != null) db.close();
		}
	}
	
	public boolean removeServer (ServerReference sr) {
		if (!(sr instanceof ServerReferenceImpl)) {
			throw new IllegalArgumentException("sr must be instanceof ServerReferenceImpl");
		}
		
		ServerReferenceImpl sri = (ServerReferenceImpl) sr;
		
		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();
			try {
				int n = db.delete(TBL_HOSTS, TBL_HOSTS_ID + "=?", new String[] { String.valueOf(sri.getId()) });
				if (n > 0) {
					db.setTransactionSuccessful();
					return true;
				}
				
				return false;
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			if (db != null) db.close();
		}
		
	}
	
	@Override
	public String getSortKey () {
		return ""; // This should never be relevant.
	}
	
	@Override
	public int compareTo (ArtifactList another) {
		return this.getSortKey().compareTo(another.getSortKey());
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
