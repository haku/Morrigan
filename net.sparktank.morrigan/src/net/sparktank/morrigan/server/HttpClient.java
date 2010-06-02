package net.sparktank.morrigan.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class HttpClient {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	static private HttpClient _httpClient;
	
	// TODO make thread safe?
	static public HttpClient getHttpClient () {
		if (_httpClient == null) {
			_httpClient = new HttpClient();
		}
		return _httpClient;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static final int HTTP_CONNECT_TIMEOUT_SECONDS = 60;
	private static final int HTTP_READ_TIMEOUT_SECONDS = 600;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Handy class for returning all the bits of a HTTP request.
	 */
	public static class HttpResponse {
		
		private int responseCode;
		private String responseBody;
		private String etag;
		private Map<String, List<String>> headerFields;
		
		public HttpResponse (int code, String body, String etag, Map<String, List<String>> headerFields) {
			responseCode = code;
			responseBody = body;
			this.etag = etag;
			this.headerFields = headerFields;
		}
		
		public int getCode () { return responseCode; }
		public String getBody () { return responseBody; }
		public String getEtag () { return etag; }
		public Map<String, List<String>> getHeaderFields () { return headerFields; }
		
		@Override
		public String toString () {
			return responseCode+": "+responseBody;
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private CookieManager cookieManager;
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public HttpClient () {
		cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(new CookiePolicy() {
			@Override
			public boolean shouldAccept(URI uri, HttpCookie cookie) {
				logger.fine("Accpting cookie " + uri +": " + cookie.getName() + "[maxage="+cookie.getMaxAge()+"]");
				return true;
			}
		});
		CookieHandler.setDefault(cookieManager); // FIXME strictly speaking, we should not be setting this as default but per connection.
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	/**
	 * Do a simple GET request.
	 */
	public HttpResponse doHttpRequest(URL url) throws IOException {
		return doHttpRequest(url, null, null, null, null);
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
	 */
	public HttpResponse doHttpRequest(URL url, String httpRequestMethod, String encodedData, String contentType, Map<String, String> headers) throws IOException {
		logger.finest("doHttpRequest(" + (httpRequestMethod==null ? "GET" : httpRequestMethod) + " " + url + "):");
		
		HttpURLConnection hsuc = (HttpURLConnection) url.openConnection();
//		disableSSLCertificateChecking(hsuc);
		hsuc.setConnectTimeout(HTTP_CONNECT_TIMEOUT_SECONDS * 1000);
		hsuc.setReadTimeout(HTTP_READ_TIMEOUT_SECONDS * 1000);
//		hsuc.setChunkedStreamingMode(0);
		
		if (httpRequestMethod!=null) {
			hsuc.setDoOutput(true);
			hsuc.setRequestMethod(httpRequestMethod);
			if (contentType!=null) hsuc.setRequestProperty("Content-Type", contentType);
			
			if (headers!=null) {
				for (String header : headers.keySet()) {
					hsuc.setRequestProperty(header, headers.get(header));
				}
			}
			
			OutputStreamWriter out = new OutputStreamWriter(hsuc.getOutputStream());
			out.write(encodedData);
			out.flush();
			out.close();
		}
		
		Map<String, List<String>> headerFields = hsuc.getHeaderFields();
		String etag = hsuc.getHeaderField("Etag");
		int responseCode = hsuc.getResponseCode();
		
		InputStream is = hsuc.getInputStream();
        int v;
        StringBuilder sb = new StringBuilder();
        while( (v = is.read()) != -1){
                sb.append((char)v);
        }
        is.close();
        hsuc.disconnect();
        
        HttpResponse hr = new HttpResponse(responseCode, sb.toString(), etag, headerFields);
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
	
}
