package net.sparktank.morrigan.android.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sparktank.morrigan.android.model.Artifact;
import net.sparktank.morrigan.android.model.ArtifactList;

public class ArtifactListGroupImpl implements ArtifactList {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	Map<String, ArtifactList> artifactLists = new HashMap<String, ArtifactList>();
	List<? extends Artifact> cache = null;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public List<? extends Artifact> getArtifactList() {
		return this.cache;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void addList (String key, ArtifactList list) {
		this.artifactLists.put(key, list);
		updateCache();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void updateCache () {
		List<Artifact> list = new LinkedList<Artifact>();
		
		Collection<ArtifactList> lists = this.artifactLists.values();
		List<ArtifactList> sortedList = new LinkedList<ArtifactList>(lists);
		Collections.sort(sortedList);
		
		for (ArtifactList l : sortedList) {
			list.addAll(l.getArtifactList());
		}
		
		this.cache = list;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public String getSortKey() {
		return "0";
	}
	
	@Override
	public int compareTo(ArtifactList another) {
		return 0; // There is no data to sort on.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
