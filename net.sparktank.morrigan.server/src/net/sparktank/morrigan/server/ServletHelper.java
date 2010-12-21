package net.sparktank.morrigan.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

public class ServletHelper {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static void returnFile (File file, HttpServletResponse response) throws IOException {
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
		}
		finally {
			if (fileInputStream != null) fileInputStream.close();
			if (outputStream != null) outputStream.close();
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
