package com.vaguehope.morrigan.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class MockServletOutputStream extends ServletOutputStream {

	private final ByteArrayOutputStream outputStream;

	public MockServletOutputStream (final ByteArrayOutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	public void write (final int b) throws IOException {
		this.outputStream.write(b);
	}

}
