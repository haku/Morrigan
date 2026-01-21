package morrigan.dlna.extcd;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.support.contentdirectory.callback.Browse;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.SortCriterion;

public class SyncBrowse extends Browse {

	private final CountDownLatch cdl;
	private final AtomicReference<DIDLContent> ref = new AtomicReference<>();
	private final AtomicReference<String> err = new AtomicReference<>();

	protected SyncBrowse(final CountDownLatch cdl, final Service<?, ?> service, final String objectID,
			final BrowseFlag flag, final String filter, final long firstResult,
			final Long maxResults, final SortCriterion... orderBy) {
		super(service, objectID, flag, filter, firstResult, maxResults, orderBy);
		this.cdl = cdl;
	}

	public DIDLContent getRef() {
		return this.ref.get();
	}

	public String getErr() {
		return this.err.get();
	}

	@Override
	public void failure(final ActionInvocation invocation, final UpnpResponse operation, final String defaultMsg) {
		// TODO add more info to msg.
		this.err.set("DLNA browse() failed: " + defaultMsg);
		this.cdl.countDown();
	}

	@Override
	public void received(final ActionInvocation<?> invocation, final DIDLContent didl) {
		this.ref.set(didl);
		this.cdl.countDown();
	}

	@Override
	public void updateStatus(final Status status) {
		// Unused.
	}

}
