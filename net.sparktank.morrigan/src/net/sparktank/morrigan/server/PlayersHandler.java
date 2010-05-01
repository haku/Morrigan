package net.sparktank.morrigan.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.gui.helpers.ErrorHelper;
import net.sparktank.morrigan.model.playlist.PlayItem;
import net.sparktank.morrigan.player.Player;
import net.sparktank.morrigan.player.PlayerRegister;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class PlayersHandler extends AbstractHandler {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.err.println("request:t=" + target);
		
		response.setContentType("text/html;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		response.getWriter().println("<h1>Players desu~</h1>");
		response.getWriter().println("<p><a href=\"/\">home</a></p>");
		
		StringBuilder sb = new StringBuilder();
		try {
			if (target.equals("/")) {
				sb = getPlayers();
				
			} else {
				String id = target.substring(1);
				
				if (id.contains("/")) {
					String[] split = id.split("/");
					id = split[0];
					String action = split[1];
					doAction(Integer.parseInt(id), action);
				}
				
				sb = getPlayer(Integer.parseInt(id));
			}
			
		} catch (Throwable t) {
			sb = new StringBuilder();
			sb.append(ErrorHelper.getStackTrace(t));
		}
		
		response.getWriter().println(sb.toString());
	}
	
	private StringBuilder getPlayers () {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2>All players</h2>");
		
		List<Player> players = PlayerRegister.getPlayers();
		sb.append("<ul>");
		for (Player p : players) {
			sb.append("<li><a href=\"/players/"+p.getId()+"\"> p"+p.getId());
			sb.append(" " + p.getPlayState().toString()+ ": ");
			PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.item != null) {
				sb.append(currentItem.item.getTitle());
			}
			sb.append("</a></li>");
		}
		sb.append("</ul>");
		
		return sb;
	}
	
	private StringBuilder getPlayer (int n) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h2><a href=\"/players/"+n+"\">Player "+n+"</a></h2>");
		
		Player player = PlayerRegister.getPlayer(n);
		sb.append("<ul>");
		sb.append("<li>state="+player.getPlayState().toString()+"</li>");
		PlayItem currentItem = player.getCurrentItem();
		String item;
		if (currentItem != null && currentItem.item != null) {
			item = currentItem.item.getTitle();
		} else {
			item = "";
		}
		sb.append("<li>item="+item+"</li>");
		sb.append("<li>list="+player.getCurrentList()+"</li>");
		sb.append("<li><a href=\"/players/"+n+"/playpause\">play | pause</a></li>");
		sb.append("<li><a href=\"/players/"+n+"/next\">next</a></li>");
		sb.append("</ul>");
		
		return sb;
	}
	
	private void doAction (int id, String action) {
		System.err.println("[doAction] id=" + id + ", action=" + action);
		Player player = PlayerRegister.getPlayer(id);
		
		String a = action.toLowerCase();
		if (a.equals("playpause")) {
			player.pausePlaying();
			
		} else if (a.equals("next")) {
			player.nextTrack();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
