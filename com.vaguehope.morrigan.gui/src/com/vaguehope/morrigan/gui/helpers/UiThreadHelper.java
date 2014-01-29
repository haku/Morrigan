package com.vaguehope.morrigan.gui.helpers;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;

public final class UiThreadHelper {

	private UiThreadHelper () {
		throw new AssertionError();
	}

	public static <T> T callForResult (final Display display, final Callable<T> callable) {
		final RunnableCallable<T> w = new RunnableCallable<T>(callable);
		display.syncExec(w);
		return w.get();
	}

	private static class RunnableCallable<T> implements Runnable {

		private final AtomicReference<T> ref = new AtomicReference<T>();
		private final AtomicReference<Exception> ex = new AtomicReference<Exception>();
		private final Callable<T> callable;

		public RunnableCallable (final Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		public void run () {
			try {
				this.ref.set(this.callable.call());
			}
			catch (final Exception e) {
				this.ex.set(e);
			}
		}

		public T get () {
			final Exception e = this.ex.get();
			if (e != null) {
				if (e instanceof RuntimeException) throw (RuntimeException) e;
				throw new IllegalStateException(e);
			}
			return this.ref.get();
		}

	}

	public static void tryAsyncExec (final IWorkbenchPartSite site, final Runnable runnable) {
		if (site == null) return;
		final Shell shell = site.getShell();
		if (shell == null) return;
		final Display display = shell.getDisplay();
		if (display == null) return;
		display.asyncExec(runnable);
	}

}
