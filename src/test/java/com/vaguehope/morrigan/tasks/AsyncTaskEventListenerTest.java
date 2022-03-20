package com.vaguehope.morrigan.tasks;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

public class AsyncTaskEventListenerTest {

	private Clock clock;

	@Before
	public void before() throws Exception {
		// Fri 13 Feb 23:31:30 GMT 2009
		this.clock = Clock.fixed(Instant.ofEpochSecond(1234567890L), ZoneOffset.UTC);
	}

	@Test
	public void itHasOneLineSummary() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		assertEquals(TaskState.UNSTARTED, l.state());
		assertEquals("123 [UNSTARTED] (no title)", l.oneLineSummary());

		l.onStart();
		assertEquals("123 [RUNNING 23:31] (no title)", l.oneLineSummary());
		l.setName("This is my task");
		assertEquals("123 [RUNNING 23:31] This is my task", l.oneLineSummary());

		l.beginTask("Second task", 69);
		assertEquals("123 [RUNNING 23:31] 0 of 69 Second task", l.oneLineSummary());

		l.subTask("Some sub task");
		assertEquals("123 [RUNNING 23:31] 0 of 69 Second task: Some sub task", l.oneLineSummary());

		l.worked(13);
		assertEquals("123 [RUNNING 23:31] 13 of 69 Second task: Some sub task", l.oneLineSummary());

		l.done(TaskOutcome.SUCCESS);
		assertEquals("123 [SUCCESS 23:31] Second task", l.oneLineSummary());
	}

	@Test
	public void itSummariesFailure() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		l.done(TaskOutcome.FAILED);
		assertEquals("123 [FAILED 23:31] (no title)", l.oneLineSummary());
	}

	@Test
	public void itSummariesCancelled() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		l.done(TaskOutcome.CANCELLED);
		assertEquals("123 [CANCELLED 23:31] (no title)", l.oneLineSummary());
	}

	@Test
	public void itTracksCancel() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		assertEquals(false, l.isCanceled());
		l.cancel();
		assertEquals(true, l.isCanceled());
	}

	@Test
	public void itHandlesFutureSuccessful() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		final Future<?> future = mock(Future.class);
		l.setFuture(future);
		assertEquals("123 [RUNNING 23:31] (no title)", l.oneLineSummary());

		when(future.isDone()).thenReturn(true);
		assertEquals("123 [RUNNING 23:31] (no title)", l.oneLineSummary());
	}

	@Test
	public void itHandlesFutureCancelled() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		final Future<?> future = mock(Future.class);
		l.setFuture(future);
		assertEquals("123 [RUNNING 23:31] (no title)", l.oneLineSummary());

		when(future.isCancelled()).thenReturn(true);
		when(future.isDone()).thenReturn(true);
		assertEquals("123 [CANCELLED 23:31] (no title)", l.oneLineSummary());
	}

	@Test
	public void itHandlesFutureFailure() throws Exception {
		final AsyncTaskEventListener l = new AsyncTaskEventListener(123, this.clock);
		l.onStart();
		final Future<?> future = mock(Future.class);
		l.setFuture(future);
		assertEquals("123 [RUNNING 23:31] (no title)", l.oneLineSummary());

		final Exception actualFailure = new Exception("Unhappy ending");
		final ExecutionException wrapper = new ExecutionException("Wrapper", actualFailure);
		when(future.get()).thenAnswer((a) -> {
			throw wrapper;
		});

		when(future.isDone()).thenReturn(true);
		assertEquals("123 [FAILED 23:31] (no title)", l.oneLineSummary());

		final List<String> msgs = l.getAllMessages();
		assertEquals("0213-233130.000 Failed: ", msgs.get(msgs.size() - 2));
		assertEquals("java.lang.Exception: Unhappy ending", msgs.get(msgs.size() - 1));
	}

}
