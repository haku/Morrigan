package net.sparktank.morrigan.server;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.exceptions.MorriganException;
import net.sparktank.morrigan.helpers.ErrorHelper;
import net.sparktank.morrigan.model.media.HeadlessHelper;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDb;
import net.sparktank.morrigan.model.media.impl.LocalMixedMediaDbHelper;
import net.sparktank.morrigan.model.tracks.playlist.MediaPlaylist;
import net.sparktank.morrigan.model.tracks.playlist.PlaylistHelper;
import net.sparktank.morrigan.server.feedwriters.MediaExplorerFeed;
import net.sparktank.morrigan.server.feedwriters.MediaItemDbSrcFeed;
import net.sparktank.morrigan.server.feedwriters.MediaTrackListFeed;
import net.sparktank.morrigan.server.feedwriters.MixedMediaListFeed;
import net.sparktank.sqlitewrapper.DbException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.xml.sax.SAXException;

public class MediaHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target + ", m=" + request.getMethod());
		
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		PrintWriter out = response.getWriter();
		
		try {
			if (target.equals("/")) {
				new MediaExplorerFeed().process(out);
			}
			else {
				String r = target.substring(1);
				Map<?,?> paramMap = request.getParameterMap();
				
				String[] split = r.split("/");
				String type = split[0].toLowerCase();
				
				if (split.length > 1) {
					String id = split[1];
					if (type.equals("mmdb")) {
						handleMmdbRequest(response, out, paramMap, split, id);
					}
					else if (type.equals("playlist")) {
						handlePlaylistRequest(out, id);
					}
				}
				else if (type.equals("newmmdb")) {
					if (paramMap.containsKey("name")) {
						String[] v = (String[]) paramMap.get("name");
						LocalMixedMediaDbHelper.createMmdb(v[0]);
					} else {
						out.print("To create a MMDB, POST with param 'name' set.");
					}
				}
			} 
		}
		catch (Throwable t) {
			out.print(ErrorHelper.getStackTrace(t));
		}
	}
	
	private void handleMmdbRequest(HttpServletResponse response, PrintWriter out, Map<?, ?> paramMap, String[] split, String id)
		throws DbException, SAXException, MorriganException, UnsupportedEncodingException, IOException {
		
		String f = LocalMixedMediaDbHelper.getFullPathToMmdb(id);
		LocalMixedMediaDb mmdb = LocalMixedMediaDb.LOCAL_MMDB_FACTORY.manufacture(f);
		
		if (split.length > 2) {
			String param = split[2];
			if (param.equals("src")) {
				if (split.length > 3) {
					String cmd = split[3];
					if (cmd.equals("add")) {
						if (paramMap.containsKey("dir")) {
							String[] v = (String[]) paramMap.get("dir");
							mmdb.addSource(v[0]);
							out.print("Added src '"+v[0]+"'.");
						}
						else {
							out.print("To add a src, POST with param 'dir' set.");
						}
					}
					else if (cmd.equals("remove")) {
						if (paramMap.containsKey("dir")) {
							String[] v = (String[]) paramMap.get("dir");
							mmdb.removeSource(v[0]);
							out.print("Removed src '"+v[0]+"'.");
						}
						else {
							out.print("To remove a src, POST with param 'dir' set.");
						}
					}
				}
				else {
					new MediaItemDbSrcFeed(mmdb).process(out);
				}
			}
			else if (param.equals("scan")) {
				if (HeadlessHelper.scheduleMmdbScan(mmdb)) {
					out.print("Scan scheduled.");
				}
				else {
					out.print("Failed to schedule scan.");
				}
			}
			else {
				String filename = URLDecoder.decode(param, "UTF-8");
				File file = new File(filename);
				if (file.exists()) {
					System.err.println("About to send file '"+file.getAbsolutePath()+"'.");
					returnFile(file, response);
				}
			}
		}
		else {
			MixedMediaListFeed<LocalMixedMediaDb> libraryFeed = new MixedMediaListFeed<LocalMixedMediaDb>(mmdb);
			libraryFeed.process(out);
		}
	}
	
	private void handlePlaylistRequest(PrintWriter out, String id) throws MorriganException, SAXException {
		String f = PlaylistHelper.getFullPathToPlaylist(id);
		MediaPlaylist ml = MediaPlaylist.FACTORY.manufacture(f);
		MediaTrackListFeed<MediaPlaylist> libraryFeed = new MediaTrackListFeed<MediaPlaylist>(ml);
		libraryFeed.process(out);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void returnFile (File file, HttpServletResponse response) throws IOException {
		response.reset();
		
		response.setContentType("application/octet-stream");
		response.addHeader("Content-Description", "File Transfer");
		response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
		response.addHeader("Content-Transfer-Encoding", "binary");
		response.addHeader("Expires", "0");
		response.addHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		response.addHeader("Pragma", "public");
		response.addHeader("Content-Length", String.valueOf(file.length()));
		
		FileInputStream fileInputStream = null;
		ServletOutputStream outputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			outputStream = response.getOutputStream();
			
			BufferedInputStream buf = new BufferedInputStream(fileInputStream);
			// FIXME this could be done better?
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = buf.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.flush();
			
			response.flushBuffer();
			
		} finally {
			if (fileInputStream != null) fileInputStream.close();
			if (outputStream != null) outputStream.close();
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
