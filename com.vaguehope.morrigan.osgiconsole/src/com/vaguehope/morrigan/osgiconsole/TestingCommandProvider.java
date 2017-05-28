package com.vaguehope.morrigan.osgiconsole;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import com.vaguehope.morrigan.model.exceptions.MorriganException;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.player.OrderResolver;
import com.vaguehope.morrigan.player.PlaybackOrder;
import com.vaguehope.morrigan.util.ErrorHelper;

public class TestingCommandProvider implements CommandProvider {

	private final CliHelper cliHelper;

	public TestingCommandProvider (final CliHelper cliHelper) {
		this.cliHelper = cliHelper;
	}

	@Override
	public String getHelp () {
		return "---Morrigan Testing---\n" +
				"\tmt followtags <q1>\n";
	}

	public void _mt (final CommandInterpreter ci) {
		try {
			mnUnsafe(ci);
		}
		catch (final Exception e) {
			ci.println(ErrorHelper.getCauseTrace(e));
		}
	}

	public void mnUnsafe (final CommandInterpreter ci) throws ArgException, MorriganException {
		final String arg = ci.nextArgument();
		if ("followtags".equalsIgnoreCase(arg)) {
			simFollowTags(ci);
		}
	}

	private void simFollowTags (final CommandInterpreter ci) throws ArgException, MorriganException {
		final IMediaTrackList<? extends IMediaTrack> list = this.cliHelper.argQ1(ci.nextArgument());

		final OrderResolver or = new OrderResolver();
		IMediaTrack track = null;
		for (int i = 0; i < 100; i++) {
			track = or.getNextTrack(list, track, PlaybackOrder.FOLLOWTAGS);
		}
	}

}
