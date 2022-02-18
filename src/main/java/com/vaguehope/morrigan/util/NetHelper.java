package com.vaguehope.morrigan.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public final class NetHelper {

	private NetHelper () {
		throw new AssertionError();
	}

	public static List<InetAddress> getIpAddresses () throws SocketException {
		final List<InetAddress> addresses = new ArrayList<InetAddress>();
		for (final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
			final NetworkInterface iface = interfaces.nextElement();
			if (!isUseable(iface)) continue;
			for (final InterfaceAddress ifaceAddr : iface.getInterfaceAddresses()) {
				final InetAddress inetAddr = ifaceAddr.getAddress();
				if (!(inetAddr instanceof Inet4Address)) continue;
				addresses.add(inetAddr);
			}
		}
		return addresses;
	}

	public static List<String> getIpAddressesAsStrings () throws SocketException {
		final List<String> ret = new ArrayList<String>();
		for (InetAddress addr : getIpAddresses()) {
			ret.add(addr.getHostAddress());
		}
		return ret;
	}

	private static boolean isUseable (final NetworkInterface iface) throws SocketException {
		if (iface.isLoopback()) return false;

		final String name = iface.getName().toLowerCase(Locale.ROOT);
		if (name == null) return false;
		if (name.startsWith("docker")) return false;
		if (name.startsWith("br-")) return false;
		if (name.startsWith("tailscale")) return false;

		return true;
	}

}
