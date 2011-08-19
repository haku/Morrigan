package com.vaguehope.morrigan.playbackimpl.gs;

import java.util.List;
import java.util.logging.Logger;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.Pad;
import org.gstreamer.Structure;
import org.gstreamer.elements.PlayBin;

public class WaitForVideoThread extends Thread {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	private final PlayBin playbin;
	private final Runnable videoNotFound;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public WaitForVideoThread(PlayBin playbin, Runnable videoNotFound) {
		setDaemon(true);
		this.playbin = playbin;
		this.videoNotFound = videoNotFound;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void run() {
		Element decodeElement = null;
		long startTime = System.currentTimeMillis();
		while (decodeElement == null) {
			if (System.currentTimeMillis() - startTime > Constants.WAIT_FOR_decodeElement_TIMEOUT) {
				this.logger.fine("WaitForVideoThread : Timed out waiting for decodeElement to be available.");
			}
			
			try {
				Thread.sleep(Constants.WAIT_FOR_decodeElement_POLL_INTERVAL);
			} catch (InterruptedException e) { /* UNUSED */ }
			
			List<Element> elements = this.playbin.getElements();
			for (Element element : elements) {
				if (element.getName().contains("decodebin")) {
					decodeElement = element;
					break;
				}
			}
		}
		
		while (true) {
			boolean check = checkIfVideoFound(decodeElement);
			if (check) {
				this.logger.fine("WaitForVideoThread : Found all pads in " + (System.currentTimeMillis() - startTime) + " ms.");
				break;
			}
			
			if (System.currentTimeMillis() - startTime > Constants.WAIT_FOR_PADS_TIMEOUT) {
				this.logger.fine("WaitForVideoThread : Timed out waiting for checkIfVideoFound to return true.");
			}
			
			try {
				Thread.sleep(Constants.WAIT_FOR_PADS_POLL_INTERVAL);
			} catch (InterruptedException e) { /* UNUSED */ }
		}
	}
	
	/**
	 * Returns true when search is complete.
	 */
	private boolean checkIfVideoFound (Element decodeElement) {
		int srcCount = 0;
		boolean foundVideo = false;
		boolean noMorePads = false;
		
		List<Pad> pads = decodeElement.getPads();
		for (int i = 0; i < pads.size(); i++) {
			Pad pad = pads.get(i);
			this.logger.fine("checkIfVideoFound() : pad["+i+" of "+pads.size()+"]: " + pad.getName());
			
			Caps caps = pad.getCaps();
			if (caps != null) {
				if (caps.size() > 0) {
					Structure structure = caps.getStructure(0);
					if (structure != null) {
						if (structure.getName().startsWith("video/")) {
							foundVideo = true;
						}
					}
				}
			}
			
			if (pad.getName().contains("src")) {
				srcCount++;
			}
			else if (pad.getName().contains("sink") && srcCount > 0) {
				this.logger.fine("checkIfVideoFound() : Found sink pad and at least 1 src pad, assuming noMorePads.");
				noMorePads = true;
				break;
			}
		}
		
		if (noMorePads) {
			if (!foundVideo) {
				this.logger.fine("Video not found, calling handler...");
				this.videoNotFound.run();
				this.logger.fine("Handler called.");
			}
			return true;
		}
		
		return false;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
