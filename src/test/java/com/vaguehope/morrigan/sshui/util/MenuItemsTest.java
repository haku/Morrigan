package com.vaguehope.morrigan.sshui.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vaguehope.morrigan.sshui.MenuHelper.VDirection;

public class MenuItemsTest {

	private MenuItems undertest;
	private List<String> groupA;
	private List<String> groupB;
	private List<String> groupC;
	private List<String> groupD;

	@Before
	public void before() throws Exception {
		this.groupA = Arrays.asList("A1", "A2", "A3");
		this.groupB = Arrays.asList("B1", "B3");
		this.groupC = Arrays.asList("C1");
		this.groupD = Arrays.asList();
		this.undertest = MenuItems.builder()
				.addHeading("Group A")
				.addList(this.groupA, "(empty A)", a -> a)
				.addHeading("Group B")
				.addList(this.groupB, "(empty B)", a -> a)
				.addHeading("Group C")
				.addList(this.groupC, "(empty C)", a -> a)
				.addHeading("Group D")
				.addList(this.groupD, "(empty D)", a -> a)
				.build();
	}

	@Test
	public void itCountsAllItemsAndTitles() throws Exception {
		assertEquals(allGroupsList().size() + 5, this.undertest.size());
	}

	@Test
	public void itMovesSelectionDownTheList() throws Exception {
		SelectionAndScroll selected = new SelectionAndScroll(null, 0);
		final List<String> allGroupsList = allGroupsList();

		for (final String expected : allGroupsList) {
			selected = this.undertest.moveSelection(selected, 8, 1);
			assertEquals(expected, selected.selectedItem);
		}

		selected = this.undertest.moveSelection(selected, 8, 1);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), selected.selectedItem);
	}

	@Test
	public void itMovesSelectionUpTheList() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		Collections.reverse(allGroupsList);
		SelectionAndScroll selected = new SelectionAndScroll(allGroupsList.remove(0), 0);

		for (final String expected : allGroupsList) {
			selected = this.undertest.moveSelection(selected, 8, -1);
			assertEquals(expected, selected.selectedItem);
		}

		selected = this.undertest.moveSelection(selected, 8, -1);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), selected.selectedItem);
	}

	@Test
	public void itMovesSelectToTheFirstItem() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll selected = new SelectionAndScroll(allGroupsList.get(allGroupsList.size() - 1), 0);

		selected = this.undertest.moveSelectionToEnd(selected, 8, VDirection.UP);
		assertEquals(allGroupsList.get(0), selected.selectedItem);
	}

	@Test
	public void itMovesSelectToTheLastItem() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll selected = new SelectionAndScroll(allGroupsList.get(0), 0);

		selected = this.undertest.moveSelectionToEnd(selected, 8, VDirection.DOWN);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), selected.selectedItem);
	}

	private List<String> allGroupsList() {
		final List<String> r = new ArrayList<>();
		r.addAll(this.groupA);
		r.addAll(this.groupB);
		r.addAll(this.groupC);
		r.addAll(this.groupD);
		return r;
	}

}
