package net.sparktank.morrigan.views;

import net.sparktank.morrigan.model.media.MediaTrack;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class ViewPlayer extends ViewPart {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final String ID = "net.sparktank.morrigan.views.ViewPlayer";
	
	private Label mainLabel;
	
	private MediaTrack currentTrack = null; 

//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	ViewPart methods.
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout ());
		mainLabel = new Label(parent, SWT.WRAP);
		
		updateStatus();
	}
	
	@Override
	public void setFocus() {}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void startPlaying (MediaTrack track) {
		currentTrack = track;
		
		// TODO start playing track.
		
		updateStatus();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void updateStatus () {
		if (currentTrack != null) {
			mainLabel.setText("Now playing: " + currentTrack.toString());
			
		} else {
			mainLabel.setText("Idle.");
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
