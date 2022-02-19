package com.vaguehope.morrigan.dlna.players;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.UpnpHelper;
import com.vaguehope.morrigan.dlna.content.MediaFileLocator;
import com.vaguehope.morrigan.dlna.httpserver.MediaServer;
import com.vaguehope.morrigan.dlna.util.StringHelper;
import com.vaguehope.morrigan.player.Player;
import com.vaguehope.morrigan.player.PlayerRegister;
import com.vaguehope.morrigan.player.PlayerStateStorage;

public class PlayerHolder {

	private static final Logger LOG = LoggerFactory.getLogger(PlayerHolder.class);

	private final PlayerRegister playerRegister;
	private final ControlPoint controlPoint;
	private final MediaServer mediaServer;
	private final MediaFileLocator mediaFileLocator;
	private final PlayerStateStorage stateStorage;
	private final ScheduledExecutorService scheduledExecutor;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Map<UDN, RemoteService> avTransports = new ConcurrentHashMap<>();
	private final ConcurrentMap<UDN, Set<AbstractDlnaPlayer>> players = new ConcurrentHashMap<>();
	private final Map<String, PlayerState> backedupPlayerState = new ConcurrentHashMap<>();



	public PlayerHolder (final PlayerRegister playerRegister, final ControlPoint controlPoint,
			final MediaServer mediaServer, final MediaFileLocator mediaFileLocator,
			final PlayerStateStorage playerStateStorage, final ScheduledExecutorService scheduledExecutor) {
		this.playerRegister = playerRegister;
		this.controlPoint = controlPoint;
		this.mediaServer = mediaServer;
		this.mediaFileLocator = mediaFileLocator;
		this.stateStorage = playerStateStorage;
		this.scheduledExecutor = scheduledExecutor;
	}

	public void addAvTransport (final RemoteDevice device, final RemoteService avTransport) {
		checkAlive();
		final UDN udn = device.getIdentity().getUdn();
		this.avTransports.put(udn, avTransport);
		registerAvTransport(udn, avTransport);
	}

	public void removeAvTransport (final RemoteDevice device) {
		checkAlive();
		final UDN udn = device.getIdentity().getUdn();
		this.avTransports.remove(udn);
		final Set<AbstractDlnaPlayer> playersFor = this.players.remove(udn);
		if (playersFor != null) {
			for (final AbstractDlnaPlayer player : playersFor) {
				final PlayerState backupState = player.backupState();
				this.backedupPlayerState.put(player.getUid(), backupState);
				LOG.info("Backed up {}: {}.", player.getUid(), backupState);
				player.dispose();
			}
		}
	}

	public void dispose () {
		if (this.alive.compareAndSet(true, false)) {
			for (final Set<AbstractDlnaPlayer> playersFor : this.players.values()) {
				for (final Player player : playersFor) {
					player.dispose();
				}
			}
			this.players.clear();
			this.avTransports.clear();
		}
	}

	private void checkAlive () {
		if (!this.alive.get()) throw new IllegalStateException();
	}

	// TODO registerCurrentPlayers() and this.avTransports can be removed?
	public void registerCurrentPlayers () {
		checkAlive();
		final Collection<Entry<UDN, RemoteService>> avTs = this.avTransports.entrySet();
		for (final Entry<UDN, RemoteService> avT : avTs) {
			registerAvTransport(avT.getKey(), avT.getValue());
		}
		LOG.info("Registered {} players in {}.", avTs.size(), this.playerRegister);
	}

	private void registerAvTransport (final UDN udn, final RemoteService avTransport) {
		final AbstractDlnaPlayer player;
		if (StringHelper.blank(System.getenv("DLNA_OLD_PLAYER"))) {
			player = new GoalSeekingDlnaPlayer(this.playerRegister,
					this.controlPoint, avTransport, this.mediaServer, this.mediaFileLocator,
					this.scheduledExecutor);
		}
		else {
			player = new DlnaPlayer(this.playerRegister,
					this.controlPoint, avTransport, this.mediaServer, this.mediaFileLocator,
					this.scheduledExecutor);
		}

		final PlayerState previousState = this.backedupPlayerState.get(UpnpHelper.remoteServiceUid(avTransport));
		if (previousState != null) {
			player.restoreBackedUpState(previousState);
		}
		else {
			this.stateStorage.requestReadState(player);
		}

		this.playerRegister.register(player);

		Set<AbstractDlnaPlayer> playersFor = this.players.get(udn);
		if (playersFor == null) this.players.putIfAbsent(udn, new HashSet<AbstractDlnaPlayer>());
		playersFor = this.players.get(udn);
		if (playersFor == null) throw new IllegalStateException();
		playersFor.add(player);

		LOG.info("Registered {}: {}.", player.getUid(), player, udn, this.playerRegister);
	}

}
