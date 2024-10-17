package com.vaguehope.morrigan.dlna;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaguehope.morrigan.util.ChecksumHelper;

public class SystemId {

	private static final Logger LOG = LoggerFactory.getLogger(SystemId.class);
	private final long highBits;

	public SystemId(final File file) throws IOException {
		this(file, () -> ModelUtil.getFirstNetworkInterfaceHardwareAddress());
	}

	SystemId(final File f, final Supplier<byte[]> highSeed) throws IOException {
		if (f != null && f.exists()) {
			final List<String> lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
			final String raw = lines.size() > 0 ? lines.get(0) : null;
			if (!StringUtils.isEmpty(raw)) {
				this.highBits = Long.parseLong(raw);
			}
			else {
				this.highBits = generateHighBits(f, highSeed);
			}
		}
		else {
			this.highBits = generateHighBits(f, highSeed);
		}

		LOG.info("partial uniqueSystemIdentifier: {}", new UDN(new UUID(this.highBits, 0)));
	}

	private static long generateHighBits(final File f, Supplier<byte[]> highSeed) throws IOException {
		final long l = ChecksumHelper.md5(highSeed.get()).longValue();

		if (f != null) {
			FileUtils.write(f, String.valueOf(l), StandardCharsets.UTF_8);
		}

		return l;
	}

	public UDN getUsi(final String subSystem) {
		return new UDN(new UUID(this.highBits, ChecksumHelper.md5(subSystem).longValue()));
	}

}
