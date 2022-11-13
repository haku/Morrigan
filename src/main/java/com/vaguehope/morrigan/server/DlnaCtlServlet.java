package com.vaguehope.morrigan.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fourthline.cling.UpnpService;
import org.fourthline.cling.binding.xml.DescriptorBindingException;
import org.fourthline.cling.binding.xml.DeviceDescriptorBinder;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.protocol.RetrieveRemoteDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.dlna.DlnaService;
import com.vaguehope.morrigan.dlna.players.AbstractDlnaPlayer;
import com.vaguehope.morrigan.util.httpclient.HttpClient;
import com.vaguehope.morrigan.util.httpclient.HttpResponse;
import com.vaguehope.morrigan.util.httpclient.HttpStreamHandlerException;

public class DlnaCtlServlet extends HttpServlet {

	public static final String CONTEXTPATH = "/dlnactl";
	private static final Logger LOG = LoggerFactory.getLogger(DlnaCtlServlet.class);
	private static final long serialVersionUID = 2570344797123881402L;

	private final DlnaService dlnaSvs;

	public DlnaCtlServlet(final DlnaService dlnaSvs) {
		this.dlnaSvs = dlnaSvs;
	}

	@SuppressWarnings("resource")
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain;charset=utf-8");
		final PrintWriter w = resp.getWriter();

		for (final AbstractDlnaPlayer p : this.dlnaSvs.getPlayerHolder().getPlayers()) {
			final RemoteDevice d = p.getRemoteService().getDevice();

			if (d.getIdentity() instanceof RemoteDeviceIdentity) {
				final RemoteDeviceIdentity rdi = d.getIdentity();
				w.print(rdi.getDescriptorURL());
				w.print(" ");
			}

			w.print(p.getUid());
			w.print(" ");

			final DeviceDetails dd = d.getDetails();
			if (dd.getPresentationURI() != null) w.print(dd.getPresentationURI());

			w.println();
		}
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		if ("addrenderer".equalsIgnoreCase(req.getParameter("action"))) {
			addRenderer(req, resp);
		}
		else {
			ServletHelper.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid action.");
		}
	}

	private void addRenderer(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException {
		try {
			addRendererOrThrow(req, resp);
		}
		catch (ServletException | ValidationException | DescriptorBindingException | IOException | HttpStreamHandlerException e) {
			throw new ServletException(e);
		}
	}

	private void addRendererOrThrow(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, ValidationException, DescriptorBindingException, IOException, HttpStreamHandlerException {
		// eg "http://100.123.456.2:12345/dev/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee/desc"
		final String devDescUrlStr = ServletHelper.readRequiredParam(req, resp, "descurl", 10);
		if (devDescUrlStr == null) return;
		final URL devDescUrl = new URL(devDescUrlStr);

		// only used by ProtocolFactoryImpl.createSendingSubscribe()?
		// likely also needed to make media URLs that the renderer can access.
		final String discoveredOnLocalAddrStr = ServletHelper.readRequiredParam(req, resp, "localaddr", 10);
		if (discoveredOnLocalAddrStr == null) return;
		final InetAddress discoveredOnLocalAddr = InetAddress.getByName(discoveredOnLocalAddrStr);

		LOG.info("Fetching descriptor XML: {}", devDescUrl);
		final HttpResponse descResp = HttpClient.doHttpRequest(devDescUrl);
		if (descResp.getCode() != 200) throw new IOException("HTTP " + descResp.getCode() + " while fetching: " + devDescUrl);
		final String descXml = descResp.getBody();

		final UDN udn = null;  // This might not be needed other than for validation?  UDA10DeviceDescriptorBinderImpl reads it from descXml.
		final Integer maxAgeSeconds = 60;
		final byte[] ifaceMacAddr = null;  // only used for wake on LAN?

		final RemoteDeviceIdentity rdi = new RemoteDeviceIdentity(udn, maxAgeSeconds, devDescUrl, ifaceMacAddr, discoveredOnLocalAddr);
		final RemoteDevice undescribedDev = new RemoteDevice(rdi);

		final UpnpService upnpService = this.dlnaSvs.getUpnpService();
		final DeviceDescriptorBinder deviceDescriptorBinder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();
		final RemoteDevice describedDev = deviceDescriptorBinder.describe(undescribedDev, descXml);
		upnpService.getConfiguration().getAsyncProtocolExecutor().execute(new RetrieveRemoteDescriptors(upnpService, describedDev));
	}

}
