package net.sparktank.morrigan.helpers;

import java.util.Date;
import java.util.List;
import java.util.Random;

import net.sparktank.morrigan.model.media.MediaItem;
import net.sparktank.morrigan.model.media.MediaList;

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
		
		BYLASTPLAYED {
			@Override
			public String toString() {
				return "by last-played";
			}
		} 
		
	};
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static PlaybackOrder parsePlaybackOrder (String s) {
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.toString())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode toString: " + s);
	}
	
	public static PlaybackOrder parsePlaybackOrderByName (String s) {
		for (PlaybackOrder o : PlaybackOrder.values()) {
			if (s.equals(o.name())) return o;
		}
		throw new IllegalArgumentException("Unknown order mode name: " + s);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	// TODO take into account disabled and missing tracks.
	public static MediaItem getNextTrack (MediaList list, MediaItem track, PlaybackOrder mode) {
		if (list.getCount() <= 0) return null;
		
		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);
			
			case RANDOM:
				return getNextTrackRandom(list);
			
			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list);
				
			case BYLASTPLAYED:
				return getNextTrackByLastPlayedDate(list);
				
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
		maxPlayCount = maxPlayCount + 1;
		
		// Find sum of all selection indicies.
		long selIndexSum = 0;
		for (MediaItem i : tracks) {
			selIndexSum = selIndexSum + (maxPlayCount - i.getStartCount());
		}
		
		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * selIndexSum);
		
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
	
	private static MediaItem getNextTrackByLastPlayedDate (MediaList list) {
		MediaItem ret = null;
		List<MediaItem> tracks = list.getMediaTracks();
		Date now = new Date();
		
		// Find oldest date.
		Date maxAge = new Date();
		for (MediaItem i : tracks) {
			if (i.getDateLastPlayed() != null && i.getDateLastPlayed().before(maxAge)) {
				maxAge = i.getDateLastPlayed();
			}
		}
		long maxAgeDays = dateDiffDays(maxAge, now);
		
		// Build sum of all selection-indicies in units of days.
		long sumAgeDays = 0;
		for (MediaItem i : tracks) {
			if (i.getDateLastPlayed() != null) {
				sumAgeDays = sumAgeDays + dateDiffDays(i.getDateLastPlayed(), now);
			} else {
				sumAgeDays = sumAgeDays + maxAgeDays;
			}
		}
		
		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * sumAgeDays);
		
		// Find the target item.
		for (MediaItem i : tracks) {
			if (i.getDateLastPlayed() != null) {
				targetIndex = targetIndex - dateDiffDays(i.getDateLastPlayed(), now);
			} else {
				targetIndex = targetIndex - maxAgeDays;
			}
			if (targetIndex <= 0) {
				ret = i;
				break;
			}
		}
		
		if (ret == null) {
			throw new RuntimeException("Failed to find next track.  This should not happen.  targetIndex=" + targetIndex);
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private long dateDiffDays (Date olderDate, Date newerDate) {
		long l = (newerDate.getTime() - olderDate.getTime()) / 86400000;
		if (l < 1) l = 1;
		return l;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
