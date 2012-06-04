package com.vaguehope.morrigan.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.widget.ListView;

import com.vaguehope.morrigan.android.model.Artifact;
import com.vaguehope.morrigan.android.model.ArtifactList;
import com.vaguehope.morrigan.android.model.ArtifactListAdaptor;
import com.vaguehope.morrigan.android.modelimpl.ArtifactListAdaptorImpl;

public class ErrorsList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final ErrorList errorList;
	private final ArtifactListAdaptor<ErrorList> listAdapter;

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public ErrorsList (Context context, ListView listView) {
		this.errorList = new ErrorList();
		this.listAdapter = new ArtifactListAdaptorImpl<ErrorList>(context, R.layout.simplelistrow);
		this.listAdapter.setInputData(this.errorList);
		listView.setAdapter(this.listAdapter);
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	public void setError (String tag, Exception e) {
		this.errorList.putError(tag, e);
		this.listAdapter.notifyDataSetChanged();
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private class ErrorList implements ArtifactList {

		private final Map<String, ErrorArtifact> errors = new LinkedHashMap<String, ErrorArtifact>();
		private List<Artifact> cache;

		public ErrorList () {}

		public void putError (String tag, Exception e) {
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
		public int compareTo (ArtifactList another) {
			return this.getSortKey().compareTo(another.getSortKey());
		}

	}

	private class ErrorArtifact implements Artifact {

		private final String title;

		public ErrorArtifact (Exception e) {
			this.title = e.getMessage();
		}

		@Override
		public int getId () {
			return 0;
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
		public boolean equals (Object o) {
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
