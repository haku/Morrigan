package net.sparktank.nemain.controls;

import net.sparktank.nemain.model.NemainDate;
import net.sparktank.nemain.model.NemainEvent;

public interface CalendarCellEditEventHandler {
	
	public void editBtnClicked (NemainDate date, NemainEvent event, boolean anual);
	
}
