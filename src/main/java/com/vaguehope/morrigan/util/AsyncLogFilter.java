package com.vaguehope.morrigan.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class AsyncLogFilter extends Filter<ILoggingEvent> {

	@Override
	public FilterReply decide(ILoggingEvent event) {
		// Could call prepareForDeferredProcessing(), but only care about thread name.
		event.getThreadName();
		return FilterReply.NEUTRAL;
	}

}
