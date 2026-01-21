package morrigan.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import morrigan.config.Config;
import morrigan.config.SavedView;

public class ConfigTest {

	@Rule public TemporaryFolder tmp = new TemporaryFolder();

	@Test
	public void itLoadsSavedViews() throws Exception {
		final File d = this.tmp.newFolder();

		final File f = new File(d, "savedviews.json");
		final String input = "["
				+ "{\"name\": \"first\", \"listref\":\"LOCAL:l=test1\", \"query\": \"query 1\"},"
				+ "{\"name\": \"second\", \"listref\":\"LOCAL:l=test2\", \"query\": \"query 2\"}"
				+ "]";
		FileUtils.writeStringToFile(f, input, "UTF-8", false);

		final Config c = new Config(d);
		final Collection<SavedView> actual = c.getSavedViews();

		final Iterator<SavedView> iterator = actual.iterator();
		final SavedView first = iterator.next();
		assertEquals("first", first.getName());
		assertEquals("LOCAL:l=test1", first.getListRef());
		assertEquals("query 1", first.getQuery());

		final SavedView second = iterator.next();
		assertEquals("second", second.getName());
		assertEquals("LOCAL:l=test2", second.getListRef());
		assertEquals("query 2", second.getQuery());
	}

}
