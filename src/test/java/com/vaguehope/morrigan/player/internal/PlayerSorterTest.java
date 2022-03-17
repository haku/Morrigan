package com.vaguehope.morrigan.player.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.vaguehope.morrigan.engines.playback.IPlaybackEngine.PlayState;
import com.vaguehope.morrigan.player.Player;

public class PlayerSorterTest {

	@Test
	public void itDoesSomething() throws Exception {
		final Player p0 = mock(Player.class);
		final Player p1 = mock(Player.class);
		final Player p2 = mock(Player.class);

		Mockito.when(p0.getPlayState()).thenReturn(PlayState.STOPPED);
		Mockito.when(p2.getPlayState()).thenReturn(PlayState.PLAYING);
		Mockito.when(p1.getPlayState()).thenReturn(PlayState.PAUSED);

		final List<Player> l = new ArrayList<>(Arrays.asList(p0, p1, p2));
		l.sort(PlayerSorter.STATE);
		assertThat(l, contains(p2, p1, p0));
	}

}
