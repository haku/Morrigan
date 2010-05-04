package net.sparktank.morrigan.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ErrorHelper;
import net.sparktank.morrigan.model.MediaListFactory;
import net.sparktank.morrigan.model.library.DbException;
import net.sparktank.morrigan.model.library.LibraryHelper;
import net.sparktank.morrigan.model.library.LibraryUpdateTask;
import net.sparktank.morrigan.model.library.MediaLibrary;
import net.sparktank.morrigan.model.library.LibraryUpdateTask.TaskEventListener;
import net.sparktank.morrigan.model.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.playlist.PlaylistHelper;
import net.sparktank.morrigan.server.helpers.MediaFeed;
import net.sparktank.morrigan.server.helpers.MediaListFeed;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class MediaHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target + ", m=" + request.getMethod());
		
//		response.getWriter().println("<h1>Media desu~</h1>");
//		response.getWriter().println("<p><a href=\"/\">home</a> / <a href=\"/media\">media</a></p>");
		
		StringBuilder sb = null;
		try {
			if (target.equals("/")) {
				if (request.getMethod().equals("POST")) {
					Map<?,?> m = request.getParameterMap();
					if (m.containsKey("name")) {
						String[] v = (String[]) m.get("name");
						LibraryHelper.createLib(v[0]);
					}
				}
				sb = getMediaLists();
				
			} else {
				String r = target.substring(1);
				
				if (r.contains("/")) {
					String[] split = r.split("/");
					String type = split[0];
					String id = split[1];
					
					if (type.equals("library")) {
						if (split.length > 2) {
							String param = split[2];
							if (param.equals("src")) {
								if (request.getMethod().equals("POST")) {
									Map<?,?> m = request.getParameterMap();
									if (m.containsKey("dir")) {
										String[] v = (String[]) m.get("dir");
										addLibSrc(id, v[0]);
									} else if (m.containsKey("cmd")) {
										String[] v = (String[]) m.get("cmd");
										if (v[0].equals("scan")) {
											scheduleLibScan(id);
										}
									}
								}
								
								sb = getLibSrc(id);
							}
							
						} else {
							if (request.getMethod().equals("POST")) {
								Map<?,?> m = request.getParameterMap();
								if (m.containsKey("cmd")) {
									String[] v = (String[]) m.get("cmd");
									if (v[0].equals("scan")) {
										scheduleLibScan(id);
									}
								}
							}
							
							sb = getLibrary(id);
						}
						
					} else if (type.equals("playlist")) {
						sb = getPlaylist(id);
					}
				}
				
			}
			
		} catch (Throwable t) {
			sb = new StringBuilder();
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
	
	private StringBuilder getMediaLists () {
		StringBuilder sb = new StringBuilder();
		MediaFeed mediaFeed = new MediaFeed();
		sb.append(mediaFeed.getXmlString());
		return sb;
	}
	
	private StringBuilder getLibrary (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		
		MediaListFeed libraryFeed = new MediaListFeed(ml);
		sb.append(libraryFeed.getXmlString());
		
		return sb;
	}
	
	private StringBuilder getPlaylist (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = PlaylistHelper.getFullPathToPlaylist(id);
		MediaPlaylist ml = MediaListFactory.makeMediaPlaylist(f);
		
		MediaListFeed libraryFeed = new MediaListFeed(ml);
		sb.append(libraryFeed.getXmlString());
		
		return sb;
	}
	
	private StringBuilder getLibSrc (String id) throws MorriganException {
		StringBuilder sb = new StringBuilder();
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		printLibSrc(id, ml, sb);
		return sb;
	}
	
	private void addLibSrc (String id, String dir) throws DbException {
		String f = LibraryHelper.getFullPathToLib(id);
		MediaLibrary ml = MediaListFactory.makeMediaLibrary(f);
		ml.addSource(dir);
		System.err.println("Added src '"+dir+"'.");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void printLibSrc (String id, MediaLibrary ml, StringBuilder sb) throws MorriganException {
		sb.append("<h2>" + ml.getListName() + " src</h2>");
		
		sb.append("<form action=\"\" method=\"POST\">");
		sb.append("<input type=\"text\" name=\"dir\" >");
		sb.append("<input type=\"submit\" name=\"cmd\" value=\"add\">");
		sb.append("</form>");
		
		sb.append("<form action=\"\" method=\"POST\">");
		sb.append("<input type=\"submit\" name=\"cmd\" value=\"scan\">");
		sb.append("</form>");
		
		List<String> src = ml.getSources();
		sb.append("<ul>");
		for (String s : src) {
			sb.append("<li>");
			sb.append(s);
			sb.append("</li>");
		}
		sb.append("</ul>");
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
