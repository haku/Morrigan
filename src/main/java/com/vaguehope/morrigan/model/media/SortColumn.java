package com.vaguehope.morrigan.model.media;

// conversion to actual DB column is at MixedMediaSqliteLayerInner.columnFromEnum()
public enum SortColumn {
	FILE_PATH("File Path"),
	DATE_ADDED("Date Added"),
	DURATION("Duration"),
//	FILE_SIZE("File Size"),
	DATE_LAST_PLAYED("Date Last Played"),
	START_COUNT("Start Count"),
	END_COUNT("End Count"),
;

	private final String uiName;

	private SortColumn(final String uiName) {
		this.uiName = uiName;
	}

	public String getUiName() {
		return this.uiName;
	}

	public enum SortDirection {
		ASC(0, " ASC"), DESC(1, " DESC");

		private final int n;
		private final String sql;

		SortDirection (final int n, final String sql) {
			this.n = n;
			this.sql = sql;
		}

		public int getN () {
			return this.n;
		}

		/**
		 * Includes leading space.
		 */
		public String getSql () {
			return this.sql;
		}

		public static SortDirection parseN (final int n) {
			switch (n) {
				case 0: return ASC;
				case 1: return DESC;
				default: throw new IllegalArgumentException();
			}
		}

	}

}
