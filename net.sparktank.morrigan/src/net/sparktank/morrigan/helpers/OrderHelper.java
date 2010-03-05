package net.sparktank.morrigan.helpers;

import java.util.List;
import java.util.Random;

import net.sparktank.morrigan.model.media.MediaList;
import net.sparktank.morrigan.model.media.MediaItem;

public class OrderHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static enum PlaybackOrder {
		
		SEQUENTIAL {
			@Override
			public String toString() {
				return "sequential";
			}
		},
		
		RANDOM {
			@Override
			public String toString() {
				return "random";
			}
		},
		
		BYSTARTCOUNT {
			@Override
			public String toString() {
				return "by start-count";
			}
		},
		
//		bylastplayed {
//			@Override
//			public String toString() {
//				return "by last-played";
//			}
//		} 
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static PlaybackOrder parsePlaybackOrder (String s) {
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static MediaItem getNextTrack (MediaList list, MediaItem track, PlaybackOrder mode) {
		if (list.getCount() <= 0) return null;
		
		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);
			
			case RANDOM:
				return getNextTrackRandom(list);
			
			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list);
				
			default:
				throw new IllegalArgumentException();
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static MediaItem getNextTrackSequencial (MediaList list, MediaItem track) {
		MediaItem ret = null;
		List<MediaItem> mediaTracks = list.getMediaTracks();
		
		if (track != null && mediaTracks.contains(track)) {
			int i = mediaTracks.indexOf(track) + 1;
			if (i >= mediaTracks.size()) i = 0;
			ret = mediaTracks.get(i);
			
		} else { // With no other info, might as well start at the beginning.
			ret = list.getMediaTracks().get(0);
		}
		
		return ret;
	}
	
	private static MediaItem getNextTrackRandom (MediaList list) {
		Random generator = new Random();
		List<MediaItem> mediaTracks = list.getMediaTracks();
		int i = generator.nextInt(mediaTracks.size());
		return mediaTracks.get(i);
	}
	
	private static MediaItem getNextTrackByStartCount (MediaList list) {
		MediaItem ret = null;
		List<MediaItem> tracks = list.getMediaTracks();
		
		// Find highest play count.
		long maxPlayCount = 0;
		for (MediaItem i : tracks) {
			if (i.getStartCount() > maxPlayCount) {
				maxPlayCount = i.getStartCount();
			}
		}
		
		// Find sum of all selection indicies.
		long selIndixSum = 0;
		for (MediaItem i : tracks) {
			selIndixSum = selIndixSum + maxPlayCount - i.getStartCount();
		}
		
		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = (long) (generator.nextDouble() * selIndixSum);
		
		// Find the target item.
		for (MediaItem i : tracks) {
			targetIndex = targetIndex - (maxPlayCount - i.getStartCount());
			if (targetIndex <= 0) {
				ret = i;
				break;
			}
		}
		
		if (ret == null) {
			throw new RuntimeException("Failed to find next track.  This should not happen.");
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
