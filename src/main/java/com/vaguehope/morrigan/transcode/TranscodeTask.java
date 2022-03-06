package com.vaguehope.morrigan.transcode;

import java.util.ArrayList;
import java.util.List;

import com.vaguehope.morrigan.config.Config;
import com.vaguehope.morrigan.model.media.IMediaTrack;
import com.vaguehope.morrigan.model.media.IMediaTrackList;
import com.vaguehope.morrigan.tasks.MorriganTask;
import com.vaguehope.morrigan.tasks.TaskEventListener;
import com.vaguehope.morrigan.tasks.TaskResult;
import com.vaguehope.morrigan.tasks.TaskResult.TaskOutcome;

public class TranscodeTask implements MorriganTask {

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
			for (final IMediaTrack item : this.db.getMediaItems()) {
				if (taskEventListener.isCanceled()) break;
				if (profiles.size() >= this.maxNumber) break;

				final TranscodeProfile profile = this.transcode.profileForItem(this.config, this.db, item);
				if (profile == null) continue;
				if (profile.getCacheFileIfFresh() != null) continue;
				profiles.add(profile);
			}

			if (profiles.size() > 0) {
				taskEventListener.beginTask(getTitle(), profiles.size());
				for (final TranscodeProfile profile : profiles) {
					if (taskEventListener.isCanceled()) break;

					taskEventListener.subTask(profile.getItem().getTitle());
					this.transcoder.transcodeToFile(profile);
					taskEventListener.worked(1);
				}
			}

			if (taskEventListener.isCanceled()) return new TaskResult(TaskOutcome.CANCELED);
			taskEventListener.done();
			return new TaskResult(TaskOutcome.SUCCESS);
		}
		catch (final Exception e) {
			taskEventListener.done();
			return new TaskResult(TaskOutcome.FAILED, "Throwable while transcoding.", e);
		}
	}

}
