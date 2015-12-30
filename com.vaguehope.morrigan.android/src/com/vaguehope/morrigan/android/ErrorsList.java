package com.vaguehope.morrigan.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;

import android.content.Context;
import android.widget.ListView;

public class ErrorsList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final ErrorList errorList;
	private final ArtifactListAdaptor<ErrorList> listAdapter;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ErrorsList (final Context context, final ListView listView) {
		this.errorList = new ErrorList();
		this.listAdapter = new ArtifactListAdaptorImpl<ErrorList>(context, R.layout.simplelistrow);
		this.listAdapter.setInputData(this.errorList);
		listView.setAdapter(this.listAdapter);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void setError (final String tag, final Exception e) {
		this.errorList.putError(tag, e);
		this.listAdapter.notifyDataSetChanged();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private class ErrorList implements ArtifactList {

		private final Map<String, ErrorArtifact> errors = new LinkedHashMap<String, ErrorArtifact>();
		private List<Artifact> cache;

		public ErrorList () {}

		public void putError (final String tag, final Exception e) {
			if (e != null) {
				this.errors.put(tag, new ErrorArtifact(e));
			}
			else {
				this.errors.remove(tag);
			}
			this.cache = Collections.unmodifiableList(new ArrayList<Artifact>(new HashSet<Artifact>(this.errors.values())));
		}

		@Override
		public List<? extends Artifact> getArtifactList () {
			return this.cache;
		}

		@Override
		public String getSortKey () {
			return ""; // This should never be relevant.
		}

		@Override
		public int compareTo (final ArtifactList another) {
			return this.getSortKey().compareTo(another.getSortKey());
		}

	}

	private class ErrorArtifact implements Artifact {

		private final String title;

		public ErrorArtifact (final Exception e) {
			this.title = e.getMessage();
		}

		@Override
		public String getId () {
			return "";
		}

		@Override
		public String getTitle () {
			return this.title;
		}

		@Override
		public int getImageResource () {
			return R.drawable.exclamation_red;
		}

		@Override
		public boolean equals (final Object o) {
			if (o == null) return false;
			if (o == this) return true;
			if (!(o instanceof ErrorArtifact)) return false;
			ErrorArtifact that = (ErrorArtifact) o;
			return this.title.equals(that.getTitle());
		}

		@Override
		public int hashCode () {
			return this.title.hashCode();
		}

	}

}
