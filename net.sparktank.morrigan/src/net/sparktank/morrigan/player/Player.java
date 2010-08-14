package net.sparktank.morrigan.player;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sparktank.morrigan.engines.EngineFactory;
import net.sparktank.morrigan.engines.common.ImplException;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine;
import net.sparktank.morrigan.engines.playback.IPlaybackEngine.PlayState;
import net.sparktank.morrigan.engines.playback.IPlaybackStatusListener;
import net.sparktank.morrigan.engines.playback.PlaybackException;
import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrack;
import net.sparktank.morrigan.model.media.interfaces.IMediaTrackList;
import net.sparktank.morrigan.model.tracks.playlist.PlayItem;
import net.sparktank.morrigan.player.OrderHelper.PlaybackOrder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class Player {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

	final IPlayerEventHandler eventHandler;
	
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
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Current selection.
	
	private Object _currentItemLock = new Object();
	private PlayItem _currentItem = null;
	
	/**
	 * This is called at the start of each track.
	 * Must call this with list a null before this
	 * object is disposed so as to remove listener.
	 */
	private void setCurrentItem (PlayItem item) {
		synchronized (this._currentItemLock) {
			if (this._currentItem != null && this._currentItem.list != null) {
				this._currentItem.list.removeChangeEvent(this.listChangedRunnable);
			}
			
			this._currentItem = item;
			
			if (this._currentItem != null && this._currentItem.list != null) {
				this._currentItem.list.addChangeEvent(this.listChangedRunnable);
				
				if (this._currentItem.item != null) {
					addToHistory(this._currentItem);
				}
			}
			
			this.eventHandler.currentItemChanged();
		}
	}
	
	private Runnable listChangedRunnable = new Runnable() {
		@Override
		public void run() {
			validateHistory();
		}
	};
	
	public PlayItem getCurrentItem () {
		// TODO check item is still valid.
		return this._currentItem;
	}
	
	public IMediaTrackList<IMediaTrack> getCurrentList () {
		IMediaTrackList<IMediaTrack> ret = null;
		
		PlayItem currentItem = getCurrentItem();
		if (currentItem != null && currentItem.list != null) {
			ret = currentItem.list;
			
		} else {
			ret = this.eventHandler.getCurrentList();
		}
		
		return ret;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Track order methods.
	
	private PlaybackOrder _playbackOrder = PlaybackOrder.SEQUENTIAL;
	
	public PlaybackOrder getPlaybackOrder () {
		return this._playbackOrder;
	}
	
	public void setPlaybackOrder (PlaybackOrder order) {
		this._playbackOrder = order;
	}
	
	PlayItem getNextItemToPlay () {
		PlayItem nextItem = null;
		
		if (isQueueHasItem()) {
			nextItem = readFromQueue();
			
		} else if (getCurrentItem() != null && getCurrentItem().list != null) {
			if (getCurrentItem().item != null) {
				IMediaTrack nextTrack = OrderHelper.getNextTrack(getCurrentItem().list, getCurrentItem().item, this._playbackOrder);
				if (nextTrack != null) {
					nextItem = new PlayItem(getCurrentItem().list, nextTrack);
				}
			}
			
		} else {
			IMediaTrackList<IMediaTrack> currentList = getCurrentList();
			if (currentList != null) {
				IMediaTrack nextTrack = OrderHelper.getNextTrack(currentList, null, this._playbackOrder);
				if (nextTrack != null) {
					nextItem = new PlayItem(currentList, nextTrack);
				}
			}
		}
		
		return nextItem;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	History.
	
	static private final int HISTORY_LENGTH = 10;
	
	private List<PlayItem> _history = new ArrayList<PlayItem>();
	
	public List<PlayItem> getHistory () {
		return Collections.unmodifiableList(this._history);
	}
	
	private void addToHistory (PlayItem item) {
		synchronized (this._history) {
			if (this._history.contains(item)) {
				this._history.remove(item);
			}
			this._history.add(0, item);
			if (this._history.size() > HISTORY_LENGTH) {
				this._history.remove(this._history.size()-1);
			}
			this.eventHandler.historyChanged();
		}
	}
	
	void validateHistory () {
		synchronized (this._history) {
			boolean changed = false;
			
			for (int i = this._history.size() - 1; i >= 0; i--) {
				if (!this._history.get(i).list.getMediaItems().contains(this._history.get(i).item)) {
					this._history.remove(this._history.get(i));
					changed = true;
				}
			}
			
			if (changed) {
				this.eventHandler.historyChanged();
			}
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Queue.
	
	private List<PlayItem> _queue = new ArrayList<PlayItem>();
	private List<Runnable> _queueChangeListeners = new ArrayList<Runnable>();
	
	public void addToQueue (PlayItem item) {
		this._queue.add(item);
		callQueueChangedListeners();
	}
	
	public void removeFromQueue (PlayItem item) {
		this._queue.remove(item);
		callQueueChangedListeners();
	}
	
	public void moveInQueue (List<PlayItem> items, boolean moveDown) {
		synchronized (this._queue) {
			if (items == null || items.isEmpty()) return;
			
			for (int i = (moveDown ? this._queue.size() - 1 : 0);
			(moveDown ? i >= 0 : i < this._queue.size());
			i = i + (moveDown ? -1 : 1)
			) {
				if (items.contains(this._queue.get(i))) {
					int j;
					if (moveDown) {
						if (i == this._queue.size() - 1 ) {
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
					if (j != -1 && !items.contains(this._queue.get(j))) {
						PlayItem a = this._queue.get(i);
						PlayItem b = this._queue.get(j);
						this._queue.set(i, b);
						this._queue.set(j, a);
					}
				}
			}
			
			callQueueChangedListeners();
		}
	}
	
	private boolean isQueueHasItem () {
		synchronized (this._queue) {
			return !this._queue.isEmpty();
		}
	}
	
	private PlayItem readFromQueue () {
		synchronized (this._queue) {
			if (!this._queue.isEmpty()) {
				PlayItem item = this._queue.remove(0);
				callQueueChangedListeners();
				return item;
			}
			
			return null;
		}
	}
	
	private void callQueueChangedListeners () {
		for (Runnable r : this._queueChangeListeners) {
			r.run();
		}
	}
	
	public List<PlayItem> getQueueList () {
		return Collections.unmodifiableList(this._queue);
	}
	
	static public class DurationData {
		public long duration = 0;
		public boolean complete;
	}
	
	public DurationData getQueueTotalDuration () {
		DurationData ret = new DurationData();
		ret.complete = true;
		for (PlayItem pi : this._queue) {
			if (pi.item.getDuration() > 0) {
				ret.duration = ret.duration + pi.item.getDuration();
			} else {
				ret.complete = false;
			}
		}
		return ret;
	}
	
	public void addQueueChangeListener (Runnable listener) {
		this._queueChangeListeners.add(listener);
	}
	
	public void removeQueueChangeListener (Runnable listener) {
		this._queueChangeListeners.remove(listener);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Playback engine.
	
	IPlaybackEngine playbackEngine = null;
	
	public boolean isPlaybackEngineReady () {
		return (this.playbackEngine != null);
	}
	
	synchronized private IPlaybackEngine getPlaybackEngine (boolean create) throws ImplException {
		if (this.playbackEngine == null && create) {
			this.playbackEngine = EngineFactory.makePlaybackEngine();
			this.playbackEngine.setStatusListener(this.playbackStatusListener);
		}
		
		return this.playbackEngine;
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
	
	long _currentPosition = -1; // In seconds.
	int _currentTrackDuration = -1; // In seconds.
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (IMediaTrackList<IMediaTrack> list) {
		IMediaTrack nextTrack = OrderHelper.getNextTrack(list, null, this._playbackOrder);
		loadAndStartPlaying(list, nextTrack);
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (IMediaTrackList<IMediaTrack> list, IMediaTrack track) {
		if (track == null) throw new NullPointerException();
		loadAndStartPlaying(new PlayItem(list, track));
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void loadAndStartPlaying (PlayItem item) {
		try {
			File file = new File(item.item.getFilepath());
			if (!file.exists()) throw new FileNotFoundException(item.item.getFilepath());
			
			IPlaybackEngine engine = getPlaybackEngine(true);
			synchronized (engine) {
				System.err.println("Loading '" + item.item.getTitle() + "'...");
				setCurrentItem(item);
				engine.setFile(item.item.getFilepath());
				Composite currentMediaFrameParent = this.eventHandler.getCurrentMediaFrameParent();
				engine.setVideoFrameParent(currentMediaFrameParent);
				
				engine.loadTrack();
				engine.startPlaying();
				
				this._currentTrackDuration = engine.getDuration();
				System.err.println("Started to play '" + item.item.getTitle() + "'...");
				
				item.list.incTrackStartCnt(item.item);
				if (item.item.getDuration() <= 0) {
					if (this._currentTrackDuration > 0) {
						item.list.setTrackDuration(item.item, this._currentTrackDuration);
					}
				}
			} // END synchronized.
			
		} catch (Exception e) {
			this.eventHandler.asyncThrowable(e);
		}
		
		this.eventHandler.updateStatus();
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void pausePlaying () {
		try {
			internal_pausePlaying();
		} catch (PlaybackException e) {
			this.eventHandler.asyncThrowable(e);
		}
	}
	
	/**
	 * For UI handlers to call.
	 */
	public void stopPlaying () {
		try {
			internal_stopPlaying();
		} catch (PlaybackException e) {
			this.eventHandler.asyncThrowable(e);
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
			}
			
			return PlayState.Stopped;
		}
		catch (ImplException e) {
			return PlayState.Stopped;
		}
	}
	
	public long getCurrentPosition () {
		return this._currentPosition;
	}
	
	public int getCurrentTrackDuration () {
		return this._currentTrackDuration;
	}
	
	public void seekTo (double d) {
		try {
			internal_seekTo(d);
		} catch (PlaybackException e) {
			this.eventHandler.asyncThrowable(e);
		}
	}
	
	private void internal_pausePlaying () throws PlaybackException {
		// Don't go and make a player engine instance.
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			synchronized (eng) {
				PlayState playbackState = eng.getPlaybackState();
				if (playbackState == PlayState.Paused) {
					eng.resumePlaying();
				}
				else if (playbackState == PlayState.Playing) {
					eng.pausePlaying();
				}
				else if (playbackState == PlayState.Stopped) {
					loadAndStartPlaying(getCurrentItem());
				}
				else {
					this.eventHandler.asyncThrowable(new PlaybackException("Don't know what to do.  Playstate=" + playbackState + "."));
				}
			} // END synchronized.
			this.eventHandler.updateStatus();
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
			synchronized (eng) {
				eng.stopPlaying();
				eng.unloadFile();
			}
			this.eventHandler.updateStatus();
		}
	}
	
	protected void internal_seekTo (double d) throws PlaybackException {
		IPlaybackEngine eng = getPlaybackEngine(false);
		if (eng!=null) {
			synchronized (eng) {
				eng.seekTo(d);
			}
		}
	}
	
	private IPlaybackStatusListener playbackStatusListener = new IPlaybackStatusListener () {
		
		@Override
		public void positionChanged(long position) {
			Player.this._currentPosition = position;
			Player.this.eventHandler.updateStatus();
		}
		
		@Override
		public void durationChanged(int duration) {
			Player.this._currentTrackDuration = duration;
			
			if (duration > 0) {
				PlayItem c = getCurrentItem();
				if (c != null && c.list != null && c.item != null) {
					try {
						System.err.println("duration=" + duration);
						c.list.setTrackDuration(c.item, duration);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
			
			Player.this.eventHandler.updateStatus();
		};
		
		@Override
		public void statusChanged(PlayState state) {
			/* UNUSED */
		}
		
		@Override
		public void onEndOfTrack() {
			System.err.println("Player received endOfTrack event.");
			// Inc. stats.
			try {
				getCurrentItem().list.incTrackEndCnt(getCurrentItem().item);
			} catch (MorriganException e) {
				Player.this.eventHandler.asyncThrowable(e);
			}
			
			// Play next track?
			PlayItem nextItemToPlay = getNextItemToPlay();
			if (nextItemToPlay != null) {
				loadAndStartPlaying(nextItemToPlay);
			}
			else {
				System.err.println("No more tracks to play.");
				Player.this.eventHandler.updateStatus();
			}
		};
		
		@Override
		public void onError(Exception e) {
			Player.this.eventHandler.asyncThrowable(e);
		}
		
		@Override
		public void onKeyPress(int keyCode) {
			if (keyCode == SWT.ESC) {
				Player.this.eventHandler.videoAreaClose();
			}
		}
		
		@Override
		public void onMouseClick(int button, int clickCount) {
			System.err.println("Mouse click "+button+"*"+clickCount);
			if (clickCount > 1) {
				Player.this.eventHandler.videoAreaSelected();
			}
		}
		
	};
	
	public void setVideoFrameParent(Composite cmfp) {
		try {
			IPlaybackEngine engine = getPlaybackEngine(false);
			synchronized (engine) {
				engine.setVideoFrameParent(cmfp);
			}
		}
		catch (ImplException e) {
			throw new RuntimeException(e);
		}
	}

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}