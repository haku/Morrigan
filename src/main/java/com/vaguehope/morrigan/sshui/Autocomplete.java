package com.vaguehope.morrigan.sshui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaguehope.morrigan.util.Objs;

public class Autocomplete {

	private static final Pattern TERM = Pattern.compile("(t[=~][^ ]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern NEEDS_QUOTING = Pattern.compile("[ '\"]");

	public static PartialQuery extractPartialSearchTerm(final String text, final int caretPos) {
		final int termStart = findTermStart(text, caretPos);
		if (termStart < 0) return new PartialQuery(text, null, -1);

		final Matcher tm = TERM.matcher(text);
		if (!tm.find(termStart)) return new PartialQuery(text, null, -1);

		final String term = tm.group();
		return new PartialQuery(text, term, termStart);
	}

	private static int findTermStart(final String text, final int caretPos) {
		for (int i = caretPos - 1; i > 0; i--) {
			final char c = text.charAt(i);
			if (c == '=' || c == '~') {
				if (i == 1 || Character.isWhitespace(text.charAt(i - 2))) {
					return i - 1;
				}
				break;
			}
			if (Character.isWhitespace(c)) break;
		}
		return -1;
	}

	public static MergedResult mergeResult(final String currentText, final PartialQuery pq, final String result) {
		if (!currentText.regionMatches(pq.termStart, pq.activeTerm, 0, pq.activeTerm.length())) return null;

		String escapedResult = result;
		final String quote;
		if (NEEDS_QUOTING.matcher(result).find()) {
			if (result.indexOf('"') >= 0) {
				if (result.indexOf('\'') >= 0) {
					escapedResult = escapedResult.replace("'", "\\'");
				}
				quote = "'";
			}
			else {
				quote = "\"";
			}
		}
		else {
			quote = "";
		}

		final String leftHalf = currentText.substring(0, pq.termStart) + "t=" + quote + escapedResult + quote;
		final String rightHalf = currentText.substring(pq.termStart + pq.activeTerm.length());
		return new MergedResult(leftHalf + rightHalf, leftHalf.length());
	}

	public static class PartialQuery {
		public final String fullText;
		public final String activeTerm;
		public final int termStart;

		public PartialQuery(final String fullText, final String activeTerm, final int termStart) {
			this.fullText = fullText;
			this.activeTerm = activeTerm;
			this.termStart = termStart;
		}

		@Override
		public int hashCode() {
			return Objs.hash(this.fullText, this.activeTerm, this.termStart);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof PartialQuery)) return false;
			final PartialQuery that = (PartialQuery) obj;

			return Objs.equals(this.fullText, that.fullText)
					&& Objs.equals(this.activeTerm, that.activeTerm)
					&& Objs.equals(this.termStart, that.termStart);
		}

		@Override
		public String toString() {
			return String.format("PartialQuery{%s, %s, %s}", this.fullText, this.activeTerm, this.termStart);
		}
	}

	public static class MergedResult {
		public final String result;
		public final int caretPos;

		public MergedResult(final String result, final int caretPos) {
			this.result = result;
			this.caretPos = caretPos;
		}

		@Override
		public int hashCode() {
			return Objs.hash(this.result, this.caretPos);
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == null) return false;
			if (this == obj) return true;
			if (!(obj instanceof MergedResult)) return false;
			final MergedResult that = (MergedResult) obj;

			return Objs.equals(this.result, that.result)
					&& Objs.equals(this.caretPos, that.caretPos);
		}

		@Override
		public String toString() {
			return String.format("MergedResult{%s, %s}", this.result, this.caretPos);
		}
	}

}
