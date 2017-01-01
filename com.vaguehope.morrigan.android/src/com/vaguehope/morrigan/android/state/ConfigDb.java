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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.ServerReference;
import com.vaguehope.morrigan.android.model.ServerReferenceList;
import com.vaguehope.morrigan.android.modelimpl.ServerReferenceImpl;

public class ConfigDb extends SQLiteOpenHelper implements ServerReferenceList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String DB_NAME = "config";
	private static final int DB_VERSION = 6;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static final String TBL_HOSTS = "hosts";
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

	private static final String TBL_CHECKOUTS = "checkouts";
	private static final String TBL_CHECKOUTS_ID = "_id";
	private static final String TBL_CHECKOUTS_HOST_ID = "host_id";
	private static final String TBL_CHECKOUTS_DB_REL_PATH = "db_rel_path";
	private static final String TBL_CHECKOUTS_QUERY = "query";
	private static final String TBL_CHECKOUTS_LOCAL_DIR = "local_dir";
	private static final String TBL_CHECKOUTS_STATUS = "status";

	private static final String TBL_CHECKOUTS_CREATE =
			"CREATE TABLE " + TBL_CHECKOUTS + " ("
					+ TBL_CHECKOUTS_ID + " integer primary key autoincrement,"
					+ TBL_CHECKOUTS_HOST_ID + " integer,"
					+ TBL_CHECKOUTS_DB_REL_PATH + " text,"
					+ TBL_CHECKOUTS_QUERY + " text,"
					+ TBL_CHECKOUTS_LOCAL_DIR + " text"
					+ TBL_CHECKOUTS_STATUS + " text"
					+ ");";

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ConfigDb (final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate (final SQLiteDatabase db) {
		db.execSQL(TBL_HOSTS_CREATE);
	}

	@Override
	public void onUpgrade (final SQLiteDatabase db, final int oldVersion, final int newVersion) {
		Log.i(C.LOGTAG, String.format("Upgrading DB from version %d to %d...", oldVersion, newVersion));
		if (oldVersion <= 1) {
			db.execSQL("ALTER TABLE " + TBL_HOSTS + " ADD COLUMN " + TBL_HOSTS_NAME + " text;");
		}
		if (oldVersion <= 2) {
			db.execSQL(TBL_CHECKOUTS_CREATE);
		}
		if (oldVersion <= 4) {
			db.execSQL("ALTER TABLE " + TBL_CHECKOUTS + " ADD COLUMN " + TBL_CHECKOUTS_DB_REL_PATH + " text;");
		}
		if (oldVersion <= 5) {
			db.execSQL("ALTER TABLE " + TBL_CHECKOUTS + " ADD COLUMN " + TBL_CHECKOUTS_STATUS + " text;");
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ServerReferenceList methods.

	@Override
	public List<? extends Artifact> getArtifactList () {
		return getHosts();
	}

	@Override
	public List<? extends ServerReference> getServerReferenceList () {
		return getHosts();
	}

	public List<ServerReference> getHosts () {
		final SQLiteDatabase db = this.getReadableDatabase();
		try {
			db.beginTransaction();
			try {
				final Cursor c = db.query(true, TBL_HOSTS,
						new String[] { TBL_HOSTS_ID, TBL_HOSTS_NAME, TBL_HOSTS_URL, TBL_HOSTS_PASS },
						null, null, null, null,
						TBL_HOSTS_NAME + " ASC", null);
				try {
					final List<ServerReference> ret = new ArrayList<ServerReference>();
					if (c.moveToFirst()) {
						final int col_id = c.getColumnIndex(TBL_HOSTS_ID);
						final int col_name = c.getColumnIndex(TBL_HOSTS_NAME);
						final int col_url = c.getColumnIndex(TBL_HOSTS_URL);
						final int col_pass = c.getColumnIndex(TBL_HOSTS_PASS);
						do {
							final String id = c.getString(col_id);
							final String name = c.getString(col_name);
							final String url = c.getString(col_url);
							final String pass = c.getString(col_pass);
							ret.add(new ServerReferenceImpl(id, name, url, pass));
						}
						while (c.moveToNext());
					}
					return Collections.unmodifiableList(ret);
				}
				finally {
					c.close();
				}
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			db.close();
		}
	}

	public ServerReference getServer (final String id) {
		if (id == null) throw new IllegalArgumentException("id is null.");

		SQLiteDatabase db = null;
		try {
			db = this.getReadableDatabase();
			db.beginTransaction();
			try {
				Cursor c = null;
				try {
					c = db.query(true, TBL_HOSTS,
							new String[] { TBL_HOSTS_ID, TBL_HOSTS_NAME, TBL_HOSTS_URL, TBL_HOSTS_PASS },
							TBL_HOSTS_ID + "=?", new String[] { id },
							null, null,
							TBL_HOSTS_NAME + " ASC", null);

					if (c.moveToFirst()) {
						final int col_id = c.getColumnIndex(TBL_HOSTS_ID);
						final int col_name = c.getColumnIndex(TBL_HOSTS_NAME);
						final int col_url = c.getColumnIndex(TBL_HOSTS_URL);
						final int col_pass = c.getColumnIndex(TBL_HOSTS_PASS);

						final String inId = c.getString(col_id);
						final String name = c.getString(col_name);
						final String url = c.getString(col_url);
						final String pass = c.getString(col_pass);

						return new ServerReferenceImpl(inId, name, url, pass);
					}
					return null; // If id not found.
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

	public void addServer (final ServerReference sr) {
		if (sr.getName() == null || sr.getName().length() < 1) throw new IllegalArgumentException("Illegal name.");
		if (sr.getBaseUrl() == null || sr.getBaseUrl().length() < 1) throw new IllegalArgumentException("Illegal baseUrl.");

		final ContentValues values = new ContentValues();
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

	public void updateServer (final ServerReference sr) {
		final ContentValues values = new ContentValues();
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

	public boolean removeServer (final ServerReference sr) {
		if (!(sr instanceof ServerReferenceImpl)) {
			throw new IllegalArgumentException("sr must be instanceof ServerReferenceImpl");
		}

		final ServerReferenceImpl sri = (ServerReferenceImpl) sr;

		SQLiteDatabase db = null;
		try {
			db = this.getWritableDatabase();
			db.beginTransaction();
			try {
				final int n = db.delete(TBL_HOSTS, TBL_HOSTS_ID + "=?", new String[] { String.valueOf(sri.getId()) });
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
	public int compareTo (final ArtifactList another) {
		return this.getSortKey().compareTo(another.getSortKey());
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private static ContentValues checkoutToContentValues (final Checkout checkout) {
		final ContentValues values = new ContentValues();
		if (checkout.getId() != null) values.put(TBL_CHECKOUTS_ID, checkout.getId());
		values.put(TBL_CHECKOUTS_HOST_ID, checkout.getHostId());
		values.put(TBL_CHECKOUTS_DB_REL_PATH, checkout.getDbRelativePath());
		values.put(TBL_CHECKOUTS_QUERY, checkout.getQuery());
		values.put(TBL_CHECKOUTS_LOCAL_DIR, checkout.getLocalDir());
		values.put(TBL_CHECKOUTS_STATUS, checkout.getStatus());
		return values;
	}

	public void addCheckout (final Checkout checkout) {
		if (checkout == null) throw new IllegalArgumentException("Checkout must not be null.");
		if (checkout.getId() != null) throw new IllegalArgumentException("Checkout already has an ID.");
		final SQLiteDatabase db = this.getWritableDatabase();
		try {
			db.beginTransaction();
			try {
				db.insert(TBL_CHECKOUTS, null, checkoutToContentValues(checkout));
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			db.close();
		}
	}

	public List<Checkout> getCheckouts () {
		final SQLiteDatabase db = this.getReadableDatabase();
		try {
			db.beginTransaction();
			try {
				final Cursor c = db.query(true, TBL_CHECKOUTS,
						new String[] { TBL_CHECKOUTS_ID, TBL_CHECKOUTS_HOST_ID, TBL_CHECKOUTS_DB_REL_PATH,
								TBL_CHECKOUTS_QUERY, TBL_CHECKOUTS_LOCAL_DIR, TBL_CHECKOUTS_STATUS },
						null, null, null, null,
						TBL_CHECKOUTS_ID + " ASC", null);
				try {
					return readCheckouts(c);
				}
				finally {
					c.close();
				}
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			db.close();
		}
	}

	public Checkout getCheckout (final String id) {
		final SQLiteDatabase db = this.getReadableDatabase();
		try {
			db.beginTransaction();
			try {
				final Cursor c = db.query(true, TBL_CHECKOUTS,
						new String[] { TBL_CHECKOUTS_ID, TBL_CHECKOUTS_HOST_ID, TBL_CHECKOUTS_DB_REL_PATH,
						TBL_CHECKOUTS_QUERY, TBL_CHECKOUTS_LOCAL_DIR, TBL_CHECKOUTS_STATUS },
						TBL_CHECKOUTS_ID + "=?", new String[] { id },
						null, null,
						TBL_CHECKOUTS_ID + " ASC", null);
				try {
					final List<Checkout> r = readCheckouts(c);
					return r.size() > 0 ? r.get(0) : null;
				}
				finally {
					c.close();
				}
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			db.close();
		}
	}

	private List<Checkout> readCheckouts (final Cursor c) {
		final List<Checkout> ret = new ArrayList<Checkout>();
		if (c.moveToFirst()) {
			final int colId = c.getColumnIndex(TBL_CHECKOUTS_ID);
			final int colHostId = c.getColumnIndex(TBL_CHECKOUTS_HOST_ID);
			final int colDbRelPath = c.getColumnIndex(TBL_CHECKOUTS_DB_REL_PATH);
			final int colQuery = c.getColumnIndex(TBL_CHECKOUTS_QUERY);
			final int colLocalDir = c.getColumnIndex(TBL_CHECKOUTS_LOCAL_DIR);
			final int colStatus = c.getColumnIndex(TBL_CHECKOUTS_STATUS);
			do {
				final String id = c.getString(colId);
				final String hostId = c.getString(colHostId);
				final String dbRelPath = c.getString(colDbRelPath);
				final String query = c.getString(colQuery);
				final String localDir = c.getString(colLocalDir);
				final String status = c.getString(colStatus);
				ret.add(new Checkout(id, hostId, dbRelPath, query, localDir, status));
			}
			while (c.moveToNext());
		}
		return Collections.unmodifiableList(ret);
	}

	public void updateCheckout (final Checkout checkout) {
		if (checkout == null) throw new IllegalArgumentException("Checkout must not be null.");
		if (checkout.getId() == null) throw new IllegalArgumentException("Checkout does not have an ID.");
		final SQLiteDatabase db = this.getWritableDatabase();
		try {
			db.beginTransaction();
			try {
				db.update(TBL_CHECKOUTS, checkoutToContentValues(checkout),
						TBL_CHECKOUTS_ID + "=?",
						new String[] { String.valueOf(checkout.getId()) });
				db.setTransactionSuccessful();
			}
			finally {
				db.endTransaction();
			}
		}
		finally {
			db.close();
		}
	}

	public boolean removeCheckout (final Checkout checkout) {
		if (checkout == null) throw new IllegalArgumentException("Checkout must not be null.");
		if (checkout.getId() == null) throw new IllegalArgumentException("Checkout does not have an ID.");
		final SQLiteDatabase db = this.getWritableDatabase();
		try {
			db.beginTransaction();
			try {
				final int n = db.delete(TBL_CHECKOUTS,
						TBL_CHECKOUTS_ID + "=?",
						new String[] { String.valueOf(checkout.getId()) });
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
			db.close();
		}

	}

}
