package net.sparktank.morrigan.gui.editors;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;

public class MediaColumn {
	
	private final String humanName;
	private final ColumnLayoutData columnLayoutData;
	private final CellLabelProvider cellLabelProvider;
	private final int alignment;

	public MediaColumn (String humanName, ColumnLayoutData columnLayoutData, CellLabelProvider cellLabelProvider, int alignment) {
		this.humanName = humanName;
		this.columnLayoutData = columnLayoutData;
		this.cellLabelProvider = cellLabelProvider;
		this.alignment = alignment;
	}
	
	public MediaColumn (String humanName, ColumnLayoutData columnLayoutData, CellLabelProvider cellLabelProvider) {
		this.humanName = humanName;
		this.columnLayoutData = columnLayoutData;
		this.cellLabelProvider = cellLabelProvider;
		this.alignment = -1;
	}
	
	public ColumnLayoutData getColumnLayoutData() {
		return this.columnLayoutData;
	}
	
	public CellLabelProvider getCellLabelProvider() {
		return this.cellLabelProvider;
	}
	
	public int getAlignment() {
		return this.alignment;
	}
	
	@Override
	public String toString() {
		return this.humanName;
	}
	
}