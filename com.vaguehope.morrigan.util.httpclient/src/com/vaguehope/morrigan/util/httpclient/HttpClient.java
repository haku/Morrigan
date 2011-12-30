package com.vaguehope.morrigan.util.httpclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HttpClient {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static final Logger logger = Logger.getLogger(HttpClient.class.getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int HTTP_CONNECT_TIMEOUT_SECONDS = 60;
	private static final int HTTP_READ_TIMEOUT_SECONDS = 600;
	private static final int DOWNLOADBUFFERSIZE = 8192;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private HttpClient() { /* Static helper. */ }
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Do a simple GET request.
	 * @throws HttpStreamHandlerException 
	 */
	public static HttpResponse doHttpRequest(URL url) throws IOException, HttpStreamHandlerException {
		return doHttpRequest(url, null, null, null, null, null);
	}
	
	public static HttpResponse doHttpRequest(URL url, HttpStreamHandler httpStreamHandler) throws IOException, HttpStreamHandlerException {
		return doHttpRequest(url, null, null, null, null, httpStreamHandler);
	}
	
	public static HttpResponse doHttpRequest(URL url, Map<String, String> headers, HttpStreamHandler httpStreamHandler) throws IOException, HttpStreamHandlerException {
		return doHttpRequest(url, null, null, null, headers, httpStreamHandler);
	}
	
	/**
	 * 
	 * @param urlString
	 * @param httpRequestMethod e.g. GET, POST, PUT, DELETE
	 * @param encodedData e.g. the REST xml body.
	 * @param contentType e.g. "text/xml"
	 * @param headers e.g. pass in the Etag.
	 * @return
	 * @throws IOException
	 * @throws HttpStreamHandlerException 
	 */
	public static HttpResponse doHttpRequest(URL url, String httpRequestMethod, String encodedData, String contentType, Map<String, String> headers, HttpStreamHandler httpStreamHandler) throws IOException, HttpStreamHandlerException {
		logger.finest("doHttpRequest(" + (httpRequestMethod==null ? "GET" : httpRequestMethod) + " " + url + "):");
		
		StringBuilder sb = null;
		int responseCode = -1;
		Map<String, List<String>> headerFields = null;
		String etag = null;
		
		HttpURLConnection huc = (HttpURLConnection) url.openConnection();
		huc.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		huc.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
		
		try {
			// Set headers before making request.
			if (headers!=null) {
				for (String header : headers.keySet()) {
					huc.setRequestProperty(header, headers.get(header));
				}
			}
			
			// Any data to send?
			if (httpRequestMethod!=null) {
				huc.setDoOutput(true);
				huc.setRequestMethod(httpRequestMethod);
				if (contentType!=null) huc.setRequestProperty("Content-Type", contentType);
				
				OutputStreamWriter out = new OutputStreamWriter(huc.getOutputStream());
				try {
					out.write(encodedData);
					out.flush();
				} finally {
					out.close();
				}
			}
			
			// Start receiving.
			headerFields = huc.getHeaderFields();
			etag = huc.getHeaderField("Etag");
			responseCode = huc.getResponseCode();
			
			InputStream is = huc.getInputStream();
			try {
				if (httpStreamHandler != null) {
					httpStreamHandler.handleStream(is);
				}
				else {
					int v;
					sb = new StringBuilder();
					while( (v = is.read()) != -1){
						sb.append((char)v);
					}
				}
			} finally {
				is.close();
			}
		}
		finally {
			huc.disconnect();
		}
		
		HttpResponse hr;
		if (sb == null) {
			hr = new HttpResponse(responseCode, null, etag, headerFields);
		}
		else {
			hr = new HttpResponse(responseCode, sb.toString(), etag, headerFields);
		}
		return hr;
	}
	
//	/**
//	 * TODO This should probably be made more specific, but for now
//	 * just ignoring mis-matched certificates will do. 
//	 * @param hsuc
//	 */
//	private void disableSSLCertificateChecking(HttpsURLConnection hsuc) {
//		TrustManager[] trustAllCerts = new TrustManager[] {
//				new X509TrustManager() {
//					@Override
//					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//					}
//					@Override
//					public void checkServerTrusted(X509Certificate[] chain,String authType) throws CertificateException {
//					}
//					@Override
//					public X509Certificate[] getAcceptedIssuers() {
//						return null;
//					}
//				}
//		};
//		
//		try {
//			SSLContext sc = SSLContext.getInstance("SSL");
//			sc.init(null, trustAllCerts, new java.security.SecureRandom());
//			hsuc.setSSLSocketFactory(sc.getSocketFactory());
//		} catch (KeyManagementException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//		
//		HostnameVerifier hv = new HostnameVerifier() {
//		    public boolean verify(String urlHostName, SSLSession session) {
//		        return true;
//		    }
//		};
//		hsuc.setHostnameVerifier(hv);
//	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * TODO remove HttpStreamHandlerException
	 * 
	 * This will flush the OutputStream.
	 * This will not close the output stream.
	 */
	public static void downloadFile (URL url, final OutputStream os) throws IOException, HttpStreamHandlerException {
		HttpStreamHandler httpStreamHandler = new HttpStreamHandler () {
			@Override
			public void handleStream (InputStream is) throws IOException, HttpStreamHandlerException {
				BufferedInputStream bis = new BufferedInputStream(is);
				byte[] buffer = new byte[DOWNLOADBUFFERSIZE];
				int bytesRead;
				while ((bytesRead = bis.read(buffer)) != -1) {
					os.write(buffer, 0, bytesRead);
				}
				os.flush();
			}
		};
		doHttpRequest(url, httpStreamHandler);
	}
	
	/**
	 * TODO remove HttpStreamHandlerException
	 */
	public static void downloadFile (URL url, final File file) throws IOException, HttpStreamHandlerException {
		BufferedOutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(file));
			downloadFile(url, os);
		}
		finally {
			if (os != null) os.close();
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
