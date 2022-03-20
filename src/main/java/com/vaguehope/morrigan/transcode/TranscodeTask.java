package com.vaguehope.morrigan.transcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskOutcome;
import com.vaguehope.morrigan.tasks.TaskResult;

public class TranscodeTask implements MorriganTask {

	private static final int MAX_CONSECUTIVE_FAILURES = 5;

	private final Transcoder transcoder;
	private final Transcode transcode;
	private final IMediaTrackList<? extends IMediaTrack> db;
	private final Integer maxNumber;
	private final Config config;

	public TranscodeTask (final Transcoder transcoder, final Transcode transcode, final IMediaTrackList<? extends IMediaTrack> db, final Integer maxNumber, final Config config) {
		this.transcoder = transcoder;
		this.transcode = transcode;
		this.db = db;
		this.maxNumber = maxNumber;
		this.config = config;
	}

	@Override
	public String getTitle () {
		return String.format("Tanscoding %s to %s (max %s items)", this.db.getListName(), this.transcode, this.maxNumber);
	}

	@Override
	public TaskResult run (final TaskEventListener taskEventListener) {
		taskEventListener.onStart();
		taskEventListener.setName(getTitle());

		try {
			final List<TranscodeProfile> profiles = new ArrayList<>();
			this.db.read();
			for (final IMediaTrack item : this.db.getMediaItems()) {
				if (taskEventListener.isCanceled()) break;
				if (profiles.size() >= this.maxNumber) break;
				if (!item.isEnabled()) continue;

				final TranscodeProfile profile = this.transcode.profileForItem(this.config, this.db, item);
				if (profile == null) continue;
				if (profile.getCacheFileIfFresh() != null) continue;
				profiles.add(profile);
			}

			int totalFailures = 0;
			if (profiles.size() > 0) {
				taskEventListener.beginTask(getTitle(), profiles.size());
				int consecutiveFailures = 0;
				for (final TranscodeProfile profile : profiles) {
					if (taskEventListener.isCanceled()) break;

					final String topic = profile.getItem().getTitle();
					taskEventListener.subTask(topic);
					try {
						this.transcoder.transcodeToFile(profile);
						consecutiveFailures = 0;
					}
					catch (final IOException e) {
						taskEventListener.logError(topic, "Transcode failed.", e);
						totalFailures += 1;
						consecutiveFailures += 1;
						if (consecutiveFailures > MAX_CONSECUTIVE_FAILURES) {
							taskEventListener.logMsg(consecutiveFailures + " consecutive failures, aborting task.");
							throw e;
						}
					}
					taskEventListener.worked(1);
				}
			}

			if (totalFailures > 0) {
				return new TaskResult(TaskOutcome.FAILED, totalFailures + " transcodes failed.", null);
			}

			if (taskEventListener.isCanceled()) {
				return new TaskResult(TaskOutcome.CANCELLED);
			}

			return new TaskResult(TaskOutcome.SUCCESS);
		}
		catch (final Exception e) {
			taskEventListener.done(TaskOutcome.FAILED);
			return new TaskResult(TaskOutcome.FAILED, "Throwable while transcoding.", e);
		}
	}

}
