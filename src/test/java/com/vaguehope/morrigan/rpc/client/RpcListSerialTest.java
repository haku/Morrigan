package com.vaguehope.morrigan.rpc.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RpcListSerialTest {

	@Test
	public void itDoesNodeList() throws Exception {
		RpcListSerial expected = new RpcListSerial("identif", null);
		RpcListSerial actual = RpcListSerial.parse(expected.serialise());
		assertEquals(expected, actual);
	}

	@Test
	public void itDoesSearchList() throws Exception {
		RpcListSerial expected = new RpcListSerial("identif", "search term goes here t=fooobar");
		RpcListSerial actual = RpcListSerial.parse(expected.serialise());
		assertEquals(expected, actual);
	}

}
