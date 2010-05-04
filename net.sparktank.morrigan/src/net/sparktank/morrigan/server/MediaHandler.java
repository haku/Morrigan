package net.sparktank.morrigan.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.helpers.ErrorHelper;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.LibraryUpdateTask;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.library.LibraryUpdateTask.TaskEventListener;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;
import net.sparktank.morrigan.server.helpers.LibrarySrcFeed;
import net.sparktank.morrigan.server.helpers.MediaFeed;
import net.sparktank.morrigan.server.helpers.MediaListFeed;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class MediaHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target + ", m=" + request.getMethod());
		
		StringBuilder sb = new StringBuilder();
		try {
			if (target.equals("/")) {
				sb.append(new MediaFeed().getXmlString());
				
			} else {
				String r = target.substring(1);
				
				if (r.contains("/")) {
					String[] split = r.split("/");
					String type = split[0].toLowerCase();
					String id = split[1];
					
					if (type.equals("library")) {
						if (split.length > 2) {
							String param = split[2];
							if (param.equals("src")) {
								String f = LibraryHelper.getFullPathToLib(id);
								MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
								sb.append(new LibrarySrcFeed(ml).getXmlString());
							}
							
						} else {
							String f = LibraryHelper.getFullPathToLib(id);
							MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
							MediaListFeed libraryFeed = new MediaListFeed(ml);
							sb.append(libraryFeed.getXmlString());
						}
						
					} else if (type.equals("playlist")) {
						String f = PlaylistHelper.getFullPathToPlaylist(id);
						MediaPlaylist ml = MediaListFactory.makeMediaPlaylist(f);
						MediaListFeed libraryFeed = new MediaListFeed(ml);
						sb.append(libraryFeed.getXmlString());
					}
				}
				
			}
			
		} catch (Throwable t) {
			sb.append(ErrorHelper.getStackTrace(t));
		}
		
		if (sb != null) {
			response.setContentType("text/html;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			response.getWriter().println(sb.toString());
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void addLibSrc (String id, String dir) throws DbException {
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		ml.addSource(dir);
		System.err.println("Added src '"+dir+"'.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void scheduleLibScan (String id) throws DbException {
		String f = LibraryHelper.getFullPathToLib(id);
		final MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		
		final LibraryUpdateTask task = LibraryUpdateTask.factory(ml);
		if (task != null) {
			
			Thread t = new Thread () {
				@Override
				public void run() {
					task.run(new LibScanMon(ml.getListName()));
				}
			};
			t.start();
			System.err.println("Scan of " + id + " scheduled on thread " + t.getId() + ".");
			
		} else {
			System.err.println("Failed to get task object from factory method.");
		}
	}
	
	static class LibScanMon implements TaskEventListener {
		
		private final String logPrefix;
		private int totalWork = 0;
		private int workDone = 0;
		private boolean canceled;
		
		public LibScanMon (String logPrefix) {
			this.logPrefix = logPrefix;
		}
		
		@Override
		public void logMsg(String topic, String s) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			sb.append(topic);
			sb.append("] ");
			sb.append(s);
			
			System.out.println(sb.toString());
		}

		@Override
		public void onStart() {}
		
		@Override
		public void beginTask(String name, int totalWork) {
			this.totalWork = totalWork;
			System.out.println("[" + logPrefix + "] starting task: " + name + ".");
		}

		@Override
		public void done() {
			System.out.println("[" + logPrefix + "] done.");
		}

		@Override
		public void subTask(String name) {
			System.out.println("[" + logPrefix + "] sub task: "+name+".");
		}
		
		@Override
		public boolean isCanceled() {
			return canceled;
		}

		@Override
		public void worked(int work) {
			workDone = workDone + work;
			System.out.println("[" + logPrefix + "] worked " + workDone + " of " + totalWork + ".");
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
