package net.sparktank.morrigan.player;

import java.util.Date;
import java.util.List;
import java.util.Random;

import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.tracks.IMediaTrackList;
import net.sparktank.morrigan.model.tracks.MediaTrack;

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
	public static MediaTrack getNextTrack (IMediaTrackList list, MediaTrack track, PlaybackOrder mode) {
		if (list.getCount() <= 0) return null;
		
		switch (mode) {
			case SEQUENTIAL:
				return getNextTrackSequencial(list, track);
			
			case RANDOM:
				return getNextTrackRandom(list, track);
			
			case BYSTARTCOUNT:
				return getNextTrackByStartCount(list, track);
				
			case BYLASTPLAYED:
				return getNextTrackByLastPlayedDate(list, track);
				
			default:
				throw new IllegalArgumentException();
			
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static MediaTrack getNextTrackSequencial (IMediaTrackList list, MediaItem track) {
		MediaTrack ret = null;
		List<? extends MediaTrack> mediaTracks = list.getMediaTracks();
		
		int i;
		if (track != null && mediaTracks.contains(track)) {
			i = mediaTracks.indexOf(track) + 1;
			
		} else {
			// With no other info, might as well start at the beginning.
			i = 0;
		}
		
		int tracksTried = 0;
		while (true) {
			if (i >= mediaTracks.size()) i = 0;
			
			if (mediaTracks.get(i).isEnabled() && !mediaTracks.get(i).isMissing()) {
				break;
			}
			
			tracksTried++;
			if (tracksTried >= list.getCount()) {
				i = -1;
				break;
			}
			
			i++;
		}
		
		if (i > 0) {
			ret = mediaTracks.get(i);
		}
		
		return ret;
	}
	
	private static MediaTrack getNextTrackRandom (IMediaTrackList list, MediaItem current) {
		Random generator = new Random();
		List<? extends MediaTrack> mediaTracks = list.getMediaTracks();
		
		int n = 0;
		for (MediaItem mi : mediaTracks) {
			if (mi.isEnabled() && !mi.isMissing() && mi != current) {
				n++;
			}
		}
		if (n == 0) return null;
		
		long x = Math.round(generator.nextDouble() * n);
		for (MediaTrack mi : mediaTracks) {
			if (mi.isEnabled() && !mi.isMissing() && mi != current) {
				x--;
				if (x<=0) {
					return mi;
				}
			}
		}
		
		throw new RuntimeException("Failed to find next track.  This should not happen.");
	}
	
	private static MediaTrack getNextTrackByStartCount (IMediaTrackList list, MediaItem current) {
		MediaTrack ret = null;
		List<? extends MediaTrack> tracks = list.getMediaTracks();
		
		// Find highest play count.
		long maxPlayCount = -1;
		for (MediaTrack i : tracks) {
			if (i.getStartCount() > maxPlayCount && i.isEnabled() && !i.isMissing() && i != current) {
				maxPlayCount = i.getStartCount();
			}
		}
		if (maxPlayCount < 0) { // No playable items.
			return null;
		}
		maxPlayCount = maxPlayCount + 1;
		
		// Find sum of all selection indicies.
		long selIndexSum = 0;
		for (MediaTrack i : tracks) {
			if (i.isEnabled() && !i.isMissing() && i != current) {
				selIndexSum = selIndexSum + (maxPlayCount - i.getStartCount());
			}
		}
		
		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * selIndexSum);
		
		// Find the target item.
		for (MediaTrack i : tracks) {
			if (i.isEnabled() && !i.isMissing() && i != current) {
				targetIndex = targetIndex - (maxPlayCount - i.getStartCount());
				if (targetIndex <= 0) {
					ret = i;
					break;
				}
			}
		}
		
		if (ret == null) {
			throw new RuntimeException("Failed to find next track.  This should not happen.");
		}
		
		return ret;
	}
	
	private static MediaTrack getNextTrackByLastPlayedDate (IMediaTrackList list, MediaTrack current) {
		MediaTrack ret = null;
		List<? extends MediaTrack> tracks = list.getMediaTracks();
		Date now = new Date();
		
		// Find oldest date.
		Date maxAge = new Date();
		int n = 0;
		for (MediaTrack i : tracks) {
			if (i.isEnabled() && !i.isMissing() && i != current) {
				if (i.getDateLastPlayed() != null && i.getDateLastPlayed().before(maxAge)) {
					maxAge = i.getDateLastPlayed();
				}
				n++;
			}
		}
		if (n == 0) { // No playable items.
			return null;
		}
		long maxAgeDays = dateDiffDays(maxAge, now);
		
		// Build sum of all selection-indicies in units of days.
		long sumAgeDays = 0;
		for (MediaTrack i : tracks) {
			if (i.isEnabled() && !i.isMissing() && i != current) {
				if (i.getDateLastPlayed() != null) {
					sumAgeDays = sumAgeDays + dateDiffDays(i.getDateLastPlayed(), now);
				} else {
					sumAgeDays = sumAgeDays + maxAgeDays;
				}
			}
		}
		
		// Generate target selection index.
		Random generator = new Random();
		long targetIndex = Math.round(generator.nextDouble() * sumAgeDays);
		
		// Find the target item.
		for (MediaTrack i : tracks) {
			if (i.isEnabled() && !i.isMissing() && i != current) {
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
