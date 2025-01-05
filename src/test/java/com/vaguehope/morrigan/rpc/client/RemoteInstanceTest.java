package com.vaguehope.morrigan.rpc.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.vaguehope.common.rpc.RpcTarget;
import com.vaguehope.morrigan.Args;

public class RemoteInstanceTest {

	@Test
	public void itParsesRemotes() throws Exception {
		final List<String> input = Arrays.asList(
				"http://example.com|my-http-name",
				"https://example.com|my-https-name",
				"https://example.com:12345|my-name_with-port");
		final List<RemoteInstance> expected = Arrays.asList(
				new RemoteInstance("my-http-name", new RpcTarget("dns:///example.com:80/", true)),
				new RemoteInstance("my-https-name", new RpcTarget("dns:///example.com:443/", false)),
				new RemoteInstance("my-name_with-port", new RpcTarget("dns:///example.com:12345/", false)));

		final Args args = mock(Args.class);
		when(args.getRemotes()).thenReturn(input);
		assertEquals(expected, RemoteInstance.fromArgs(args));
	}

}
