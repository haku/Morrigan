package morrigan.dlna.players;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jupnp.UpnpService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import morrigan.config.Config;
import morrigan.dlna.UpnpHelper;
import morrigan.model.media.MediaFactory;
import morrigan.player.Player;
import morrigan.player.PlayerRegister;
import morrigan.player.PlayerStateStorage;
import morrigan.util.StringHelper;

public class PlayerHolder {

	private static final Logger LOG = LoggerFactory.getLogger(PlayerHolder.class);

	private final PlayerRegister playerRegister;
	private final UpnpService upnpService;
	private final DlnaPlayingParamsFactory dlnaPlayingParamsFactory;
	private final PlayerStateStorage stateStorage;
	private final MediaFactory mediaFactory;
	private final Config config;
	private final ScheduledExecutorService scheduledExecutor;

	private final AtomicBoolean alive = new AtomicBoolean(true);
	private final Map<UDN, RemoteService> avTransports = new ConcurrentHashMap<>();
	private final ConcurrentMap<UDN, Set<AbstractDlnaPlayer>> players = new ConcurrentHashMap<>();
	private final Map<String, PlayerState> backedupPlayerState = new ConcurrentHashMap<>();

	public PlayerHolder(
			final PlayerRegister playerRegister,
			final UpnpService upnpService,
			final DlnaPlayingParamsFactory dlnaPlayingParamsFactory,
			final PlayerStateStorage playerStateStorage,
			final MediaFactory mediaFactory,
			final Config config,
			final ScheduledExecutorService scheduledExecutor) {
		this.playerRegister = playerRegister;
		this.upnpService = upnpService;
		this.dlnaPlayingParamsFactory = dlnaPlayingParamsFactory;
		this.stateStorage = playerStateStorage;
		this.mediaFactory = mediaFactory;
		this.config = config;
		this.scheduledExecutor = scheduledExecutor;
	}

	public Collection<AbstractDlnaPlayer> getPlayers() {
		final List<AbstractDlnaPlayer> ret = new ArrayList<>();
		for (final Set<AbstractDlnaPlayer> sp : this.players.values()) {
			ret.addAll(sp);
		}
		return ret;
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
					this.upnpService.getControlPoint(), avTransport, this.dlnaPlayingParamsFactory,
					this.scheduledExecutor, this.stateStorage, this.mediaFactory, this.config);
		}
		else {
			player = new DlnaPlayer(this.playerRegister,
					this.upnpService.getControlPoint(), avTransport, this.dlnaPlayingParamsFactory,
					this.scheduledExecutor, this.stateStorage, this.mediaFactory, this.config);
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
		if (playersFor == null) this.players.putIfAbsent(udn, new HashSet<>());
		playersFor = this.players.get(udn);
		if (playersFor == null) throw new IllegalStateException();
		playersFor.add(player);

		LOG.info("Registered {}: {}.", player.getUid(), player, udn, this.playerRegister);
	}

}
