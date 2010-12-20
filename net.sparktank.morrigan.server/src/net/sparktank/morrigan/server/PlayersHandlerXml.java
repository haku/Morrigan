package net.sparktank.morrigan.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sparktank.morrigan.player.IPlayerLocal;
import net.sparktank.morrigan.player.PlayItem;
import net.sparktank.morrigan.player.PlayerRegister;

public class PlayersHandlerXml extends HttpServlet {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final long serialVersionUID = -6463380542721345844L;
	
	private static final String ROOTPATH = "/";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String actualTarget = req.getRequestURI().substring(req.getContextPath().length());
//		System.err.println("PlayersHandlerXml:target="+actualTarget);
		
		if (actualTarget.equals(ROOTPATH)) {
			printPlayersList(resp);
		}
		else {
			try {
				int n = Integer.parseInt(actualTarget.substring(ROOTPATH.length()));
				try {
					printPlayer(resp, n);
				}
				catch (IllegalArgumentException e) {
					resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
					resp.setContentType("text/plain");
					resp.getWriter().println("HTTP Error 404 player "+n+" not found desu~");
				}
			}
			catch (NumberFormatException e) {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				resp.setContentType("text/plain");
				resp.getWriter().println("HTTP Error 404 not found '"+actualTarget+"' desu~");
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("POST to PlayersHandlerXml desu~");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private void printPlayersList (HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");
		PrintWriter pw = resp.getWriter();
		
		pw.println("List of players desu~");
		
		List<IPlayerLocal> players = PlayerRegister.getLocalPlayers();
		for (IPlayerLocal p : players) {
			pw.print(p.getId());
			pw.print(":");
			pw.print(p.getPlayState().toString());
			pw.print(":");
			
			PlayItem currentItem = p.getCurrentItem();
			if (currentItem != null && currentItem.item != null) {
				pw.print(currentItem.item.getTitle());
			}
			else {
				pw.print("null");
			}
			
			pw.println();
		}
	}
	
	private void printPlayer (HttpServletResponse resp, int playerNumber) throws IOException {
		IPlayerLocal p = PlayerRegister.getLocalPlayer(playerNumber);
		
		resp.setContentType("text/plain");
		PrintWriter pw = resp.getWriter();
		
		pw.print(p.getId());
		pw.print(":");
		pw.print(p.getPlayState().toString());
		pw.println();
		
		pw.print("list=");
		pw.print(p.getCurrentList());
		pw.println();
		
		pw.print("item=");
		PlayItem currentItem = p.getCurrentItem();
		if (currentItem != null && currentItem.item != null) {
			pw.print(currentItem.item.getTitle());
		}
		else {
			pw.print("null");
		}
		
		pw.println();
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
