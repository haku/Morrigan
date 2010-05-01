package net.sparktank.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.MediaItem;
import net.sparktank.morrigan.model.MediaList;
import net.sparktank.morrigan.model.playlist.PlayItem;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	private final IPlayerEventHandler eventHandler;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Main.
	
	public Player (IPlayerEventHandler eventHandler) {
		this.eventHandler = eventHandler;
	}
	
	public void dispose () {
		PlayerRegister.removePlayer(this);
		setCurrentItem(null);
		finalisePlaybackEngine();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ID.
	
	private int id = -1;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Current selection.
	
	private PlayItem _currentItem = null;
	
	/**
	 * This is called at the start of each track.
	 * Must call this with list a null before this
	 * object is disposed so as to remove listener.
	 */
	private void setCurrentItem (PlayItem item) {
		if (_currentItem != null && _currentItem.list != null) {
			_currentItem.list.removeChangeEvent(listChangedRunnable);
		}
		
		_currentItem = item;
		
		if (_currentItem != null && _currentItem.list != null) {
			_currentItem.list.addChangeEvent(listChangedRunnable);
			
			if (_currentItem.item != null) {
				addToHistory(_currentItem);
			}
		}
		
		eventHandler.currentItemChanged();
	}
	
	private Runnable listChangedRunnable = new Runnable() {
		@Override
		public void run() {
			validateHistory();
		}
	};
	
	public PlayItem getCurrentItem () {
		// TODO check item is still valid.
		return _currentItem;
	}
	
	public MediaList getCurrentList () {
		MediaList ret = null;
		
		PlayItem currentItem = getCurrentItem();
		if (currentItem != null && currentItem.list != null) {
			ret = currentItem.list;
			
		} else {
			ret = eventHandler.getCurrentList();
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track order methods.
	
	private PlaybackOrder _playbackOrder = PlaybackOrder.SEQUENTIAL;
	
	public PlaybackOrder getPlaybackOrder () {
		return _playbackOrder;
	}
	
	public void setPlaybackOrder (PlaybackOrder order) {
		_playbackOrder = order;
	}
	
	private PlayItem getNextItemToPlay () {
		PlayItem nextItem = null;
		
		if (isQueueHasItem()) {
			nextItem = readFromQueue();
			
		} else if (getCurrentItem() != null && getCurrentItem().list != null) {
			if (getCurrentItem().item != null) {
				MediaItem nextTrack = OrderHelper.getNextTrack(getCurrentItem().list, getCurrentItem().item, _playbackOrder);
				if (nextTrack != null) {
					nextItem = new PlayItem(getCurrentItem().list, nextTrack);
				}
			}
			
		} else {
			MediaList currentList = getCurrentList();
			if (currentList != null) {
				MediaItem nextTrack = OrderHelper.getNextTrack(currentList, null, _playbackOrder);
				if (nextTrack != null) {
					nextItem = new PlayItem(currentList, nextTrack);
				}
			}
		}
		
		return nextItem;
	}
	
	private class NextTrackRunner implements Runnable {
		
		private final PlayItem item;
		
		public NextTrackRunner (PlayItem item) {
			this.item = item;
		}
		
		@Override
		public void run() {
			loadAndStartPlaying(item);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.
	
	static private final int HISTORY_LENGTH = 10;
	
	private List<PlayItem> _history = new ArrayList<PlayItem>();
	
	public List<PlayItem> getHistory () {
		return Collections.unmodifiableList(_history);
	}
	
	private void addToHistory (PlayItem item) {
		synchronized (_history) {
			if (_history.contains(item)) {
				_history.remove(item);
			}
			_history.add(0, item);
			if (_history.size() > HISTORY_LENGTH) {
				_history.remove(_history.size()-1);
			}
			eventHandler.historyChanged();
		}
	}
	
	private void validateHistory () {
		synchronized (_history) {
			boolean changed = false;
			
			for (int i = _history.size() - 1; i >= 0; i--) {
				if (!_history.get(i).list.getMediaTracks().contains(_history.get(i).item)) {
					_history.remove(_history.get(i));
					changed = true;
				}
			}
			
			if (changed) {
				eventHandler.historyChanged();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.
	
	private List<PlayItem> _queue = new ArrayList<PlayItem>();
	private List<Runnable> _queueChangeListeners = new ArrayList<Runnable>();
	
	public void addToQueue (PlayItem item) {
		_queue.add(item);
		callQueueChangedListeners();
	}
	
	public void removeFromQueue (PlayItem item) {
		_queue.remove(item);
		callQueueChangedListeners();
	}
	
	public void moveInQueue (List<PlayItem> items, boolean moveDown) {
		if (items == null || items.isEmpty()) return;
		
		for (	int i = (moveDown ? _queue.size() - 1 : 0);
				(moveDown ? i >= 0 : i < _queue.size());
				i = i + (moveDown ? -1 : 1)
			) {
			if (items.contains(_queue.get(i))) {
				int j;
				if (moveDown) {
					if (i == _queue.size() - 1 ) {
						j = -1;
					} else {
						j = i + 1;
					}
				} else {
					if (i == 0) {
						j = -1;
					} else {
						j = i - 1;
					}
				}
				if (j != -1 && !items.contains(_queue.get(j))) {
					PlayItem a = _queue.get(i);
					PlayItem b = _queue.get(j);
					_queue.set(i, b);
					_queue.set(j, a);
				}
			}
		}
		
		callQueueChangedListeners();
	}
	
	private boolean isQueueHasItem () {
		return !_queue.isEmpty();
	}
	
	private PlayItem readFromQueue () {
		if (!_queue.isEmpty()) {
			PlayItem item = _queue.remove(0);
			callQueueChangedListeners();
			return item;
		} else {
			return null;
		}
	}
	
	private void callQueueChangedListeners () {
		for (Runnable r : _queueChangeListeners) {
			r.run();
		}
	}
	
	public List<PlayItem> getQueueList () {
		return _queue;
	}
	
	static public class DurationData {
		public long duration = 0;
		public boolean complete;
	}
	
	public DurationData getQueueTotalDuration () {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (PlayItem pi : _queue) {
			if (pi.item.getDuration() > 0) {
				ret.duration = ret.duration + pi.item.getDuration();
			} else {
				ret.complete = false;
			}
		}
		return ret;
	}
	
	public void addQueueChangeListener (Runnable listener) {
		_queueChangeListeners.add(listener);
	}
	
	public void removeQueueChangeListener (Runnable listener) {
		_queueChangeListeners.remove(listener);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback engine.
	
	IPlaybackEngine playbackEngine = null;
	
	public boolean isPlaybackEngineReady () {
		return (playbackEngine != null);
	}
	
	private IPlaybackEngine getPlaybackEngine () throws ImplException {
		return getPlaybackEngine(true);
	}
	
	private IPlaybackEngine getPlaybackEngine (boolean create) throws ImplException {
		if (playbackEngine == null && create) {
			playbackEngine = EngineFactory.makePlaybackEngine();
			playbackEngine.setStatusListener(playbackStatusListener);
		}
		
		return playbackEngine;
	}
	
	private void finalisePlaybackEngine () {
		IPlaybackEngine eng = null;
		
		try {
			eng = getPlaybackEngine(false);
		} catch (ImplException e) {
			e.printStackTrace();
		}
		
		if (eng!=null) {
			try {
				eng.stopPlaying();
			} catch (PlaybackException e) {
				e.printStackTrace();
			}
			eng.unloadFile();
			eng.finalise();
		}
	}
	
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback management.
	
	private long _currentPosition = -1; // In seconds.
	private int _currentTrackDuration = -1; // In seconds.
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (MediaList list, MediaItem track) {
		loadAndStartPlaying(new PlayItem(list, track));
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (PlayItem item) {
		try {
			setCurrentItem(item);
			
			File file = new File(item.item.getFilepath());
			if (!file.exists()) throw new FileNotFoundException(item.item.getFilepath());
			
			getPlaybackEngine().setFile(item.item.getFilepath());
			Composite currentMediaFrameParent = eventHandler.getCurrentMediaFrameParent();
			getPlaybackEngine().setVideoFrameParent(currentMediaFrameParent);
			// FIXME there must be a tidier way to do this.
			if (currentMediaFrameParent != null) {
				currentMediaFrameParent.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						try {
							getPlaybackEngine().startPlaying();
						} catch (Exception e) {
							eventHandler.asyncThrowable(e);
						}
					}
				});
			} else {
				getPlaybackEngine().startPlaying();
			}
			_currentTrackDuration = getPlaybackEngine().getDuration();
			System.out.println("Started to play " + item.item.getTitle());
			
			item.list.incTrackStartCnt(item.item);
			if (item.item.getDuration() <= 0) {
				if (_currentTrackDuration > 0) {
					item.list.setTrackDuration(item.item, _currentTrackDuration);
				}
			}
			
		} catch (Exception e) {
			eventHandler.asyncThrowable(e);
		}
		
		eventHandler.updateStatus();
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void pausePlaying () {
		try {
			internal_pausePlaying();
		} catch (PlaybackException e) {
			eventHandler.asyncThrowable(e);
		}
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying () {
		try {
			internal_stopPlaying();
		} catch (PlaybackException e) {
			eventHandler.asyncThrowable(e);
		}
	}
	
	public void nextTrack () {
		PlayItem nextItemToPlay = getNextItemToPlay();
		if (nextItemToPlay != null) {
			loadAndStartPlaying(nextItemToPlay);
		}
	}
	
	public PlayState getPlayState () {
		try {
			IPlaybackEngine eng = getPlaybackEngine(false);
			if (eng!=null) {
				return eng.getPlaybackState();
			} else {
				return PlayState.Stopped;
			}
		} catch (ImplException e) {
			return PlayState.Stopped;
		}
	}
	
	public long getCurrentPosition () {
		return _currentPosition;
	}
	
	public int getCurrentTrackDuration () {
		return _currentTrackDuration;
	}
	
	public void seekTo (double d) {
		try {
			internal_seekTo(d);
		} catch (PlaybackException e) {
			eventHandler.asyncThrowable(e);
		}
	}
	
	private void internal_pausePlaying () throws PlaybackException {
		// Don't go and make a player engine instance.
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			PlayState playbackState = eng.getPlaybackState();
			
			if (playbackState == PlayState.Paused) {
				eng.resumePlaying();
				
			} else if (playbackState == PlayState.Playing) {
				eng.pausePlaying();
				
			} else if (playbackState == PlayState.Stopped) {
				loadAndStartPlaying(getCurrentItem());
				
			} else {
				eventHandler.asyncThrowable(new PlaybackException("Don't know what to do.  Playstate=" + playbackState + "."));
			}
			eventHandler.updateStatus();
		}
	}
	
	/**
	 * For internal use.  Does not update GUI.
	 * @throws ImplException
	 * @throws PlaybackException
	 */
	private void internal_stopPlaying () throws ImplException, PlaybackException {
		/* Don't go and make a player engine instance
		 * just to call stop on it.
		 */
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			eng.stopPlaying();
			eng.unloadFile();
			
			eventHandler.updateStatus();
		}
	}
	
	protected void internal_seekTo (double d) throws PlaybackException {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			eng.seekTo(d);
		}
	}
	
	private IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {
		
		@Override
		public void positionChanged(long position) {
			_currentPosition = position;
			eventHandler.updateStatus();
		}
		
		@Override
		public void durationChanged(int duration) {
			_currentTrackDuration = duration;
			
			if (duration > 0) {
				PlayItem c = getCurrentItem();
				if (c != null && c.list != null && c.item != null) {
					try {
						System.out.println("duration=" + duration);
						c.list.setTrackDuration(c.item, duration);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
			
			eventHandler.updateStatus();
		};
		
		@Override
		public void statusChanged(PlayState state) {
			
		}
		
		@Override
		public void onEndOfTrack() {
			// Inc. stats.
			try {
				getCurrentItem().list.incTrackEndCnt(getCurrentItem().item);
			} catch (MorriganException e) {
				eventHandler.asyncThrowable(e);
			}
			
			// Play next track?
			PlayItem nextItemToPlay = getNextItemToPlay();
			if (nextItemToPlay != null) {
				NextTrackRunner r = new NextTrackRunner(nextItemToPlay);
				if (!eventHandler.doOnForegroudThread(r)) {
					r.run();
				}
				
			} else {
				System.out.println("No more tracks to play.");
				eventHandler.updateStatus();
			}
		};
		
		@Override
		public void onError(Exception e) {
			eventHandler.asyncThrowable(e);
		}
		
		@Override
		public void onKeyPress(int keyCode) {
			if (keyCode == SWT.ESC) {
				eventHandler.videoAreaClose();
			}
		}
		
		@Override
		public void onMouseClick(int button, int clickCount) {
			System.out.println("Mouse click "+button+"*"+clickCount);
			if (clickCount > 1) {
				eventHandler.videoAreaSelected();
			}
		}
		
	};
	
	public void setVideoFrameParent(Composite cmfp) {
		try {
			getPlaybackEngine(false).setVideoFrameParent(cmfp);
		} catch (ImplException e) {
			throw new RuntimeException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}