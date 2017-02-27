package com.vaguehope.morrigan.android.checkout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.vaguehope.morrigan.android.helper.FileHelper;
import com.vaguehope.morrigan.android.C;
import com.vaguehope.morrigan.android.helper.DialogHelper.Listener;
import com.vaguehope.morrigan.android.state.Checkout;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

public class CheckoutIndex {

	public static Set<String> findExcessFiles (final Context context, final List<Checkout> checkouts) throws IOException {
		final Set<String> index = new HashSet<String>();
		for (final Checkout checkout : checkouts) {
			index.addAll(read(context, checkout));
		}
		final Set<String> excessFiles = new LinkedHashSet<String>();
		for (final Checkout checkout : checkouts) {
			FileHelper.recursiveList(new File(checkout.getLocalDir()), new Listener<File>() {
				@Override
				public void onAnswer (final File file) {
					final String path = file.getAbsolutePath();
					if (!index.contains(path)) excessFiles.add(path);
				}
			});
		}
		return excessFiles;
	}

	public static void write (final Context context, final Checkout checkout, final List<ItemAndFile> itemsAndFiles) throws IOException {
		final File file = getIndexFile(context, checkout);
		final File tmpFile = new File(file.getAbsolutePath() + ".tmp");
		final JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
		try {
			writer.beginArray();
			for (final ItemAndFile iaf : itemsAndFiles) {
				writeEntry(writer, iaf);
			}
			writer.endArray();
		}
		finally {
			writer.close();
		}
		if (!tmpFile.renameTo(file)) {
			throw new IOException("Rename failed: " + tmpFile.getAbsolutePath() + " --> " + file.getAbsolutePath());
		}
		Log.i(C.LOGTAG, "Wrote index: " + file.getAbsolutePath());
	}

	public static Set<String> read (final Context context, final Checkout checkout) throws IOException {
		final File file = getIndexFile(context, checkout);
		if (!file.exists()) {
			Log.w(C.LOGTAG, "Index not found: " + file.getAbsolutePath());
			return Collections.emptySet();
		}

		final JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		try {
			final Set<String> ret = new HashSet<String>();
			reader.beginArray();
			while (reader.hasNext()) {
				final String entry = readEntry(reader);
				if (entry != null) ret.add(entry);
			}
			reader.endArray();
			return ret;
		}
		finally {
			reader.close();
		}
	}

	private static File getIndexFile (final Context context, final Checkout checkout) {
		final File dir = context.getDir("checkout_indexes", Context.MODE_PRIVATE);
		return new File(dir, "checkout-" + checkout.getId());
	}

	private static void writeEntry (final JsonWriter writer, final ItemAndFile itemAndFile) throws IOException {
		writer.beginObject();
		writer.name("path").value(itemAndFile.getLocalFile().getAbsolutePath());
		writer.endObject();
	}

	private static String readEntry (final JsonReader reader) throws IOException {
		String ret = null;
		reader.beginObject();
		while (reader.hasNext()) {
			final String name = reader.nextName();
			if (name.equals("path")) {
				ret = reader.nextString();
			}
			else {
				reader.skipValue();
			}
		}
		reader.endObject();
		return ret;
	}

}
