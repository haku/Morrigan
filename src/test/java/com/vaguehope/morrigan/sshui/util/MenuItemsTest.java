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
	private int nonSelectableItems;

	@Before
	public void before() throws Exception {
		this.groupA = Arrays.asList("A1", "A2", "A3");
		this.groupB = Arrays.asList("B1", "B2");
		this.groupC = Arrays.asList("C1");
		this.groupD = Arrays.asList();
		this.undertest = MenuItems.builder()
				.addHeading("Title")
				.addHeading("")
				.addHeading("Group A")
				.addList(this.groupA, "(empty A)", a -> a)
				.addHeading("Group B")
				.addList(this.groupB, "(empty B)", a -> a)
				.addHeading("Group C")
				.addList(this.groupC, "(empty C)", a -> a)
				.addHeading("Group D")
				.addList(this.groupD, "(empty D)", a -> a)
				.build();
		this.nonSelectableItems = 7;
	}

	@Test
	public void itCountsAllItemsAndTitles() throws Exception {
		assertEquals(allGroupsList().size() + this.nonSelectableItems, this.undertest.size());
	}

	@Test
	public void itMovesSelectionDownTheList() throws Exception {
		SelectionAndScroll sas = new SelectionAndScroll(null, 0);
		final List<String> allGroupsList = allGroupsList();

		for (final String expected : allGroupsList) {
			sas = this.undertest.moveSelection(sas, 8, 1);
			assertEquals(expected, sas.selectedItem);
		}

		sas = this.undertest.moveSelection(sas, 8, 1);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), sas.selectedItem);
	}

	@Test
	public void itMovesSelectionUpTheList() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		Collections.reverse(allGroupsList);
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.remove(0), 0);

		for (final String expected : allGroupsList) {
			sas = this.undertest.moveSelection(sas, 8, -1);
			assertEquals(expected, sas.selectedItem);
		}

		sas = this.undertest.moveSelection(sas, 8, -1);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), sas.selectedItem);
	}

	@Test
	public void itMovesSelectToTheFirstItem() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(allGroupsList.size() - 1), 0);

		sas = this.undertest.moveSelectionToEnd(sas, 8, VDirection.UP);
		assertEquals(allGroupsList.get(0), sas.selectedItem);
	}

	@Test
	public void itMovesSelectToTheLastItem() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(0), 0);

		sas = this.undertest.moveSelectionToEnd(sas, 8, VDirection.DOWN);
		assertEquals(allGroupsList.get(allGroupsList.size() - 1), sas.selectedItem);
	}

	@Test
	public void itScrollsAllTheWayToTheTopToRevealTheTitle() throws Exception {
		int visibleRows = 3;
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(0), 3);

		sas = this.undertest.moveSelection(sas, visibleRows, -1);
		assertEquals(allGroupsList.get(0), sas.selectedItem);
		assertEquals(2, sas.scrollTop);

		sas = this.undertest.moveSelection(sas, visibleRows, -1);
		assertEquals(allGroupsList.get(0), sas.selectedItem);
		assertEquals(1, sas.scrollTop);

		sas = this.undertest.moveSelection(sas, visibleRows, -1);
		assertEquals(allGroupsList.get(0), sas.selectedItem);
		assertEquals(0, sas.scrollTop);
	}

	@Test
	public void itScrollsAllTheWayToTheBottomToRevealTheEmptyList() throws Exception {
		final int visibleRows = 8;
		final List<String> allGroupsList = allGroupsList();
		int topWhereLastItemOffTheScreen = allGroupsList().size() + this.nonSelectableItems - visibleRows - 1;
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(allGroupsList.size() - 1), topWhereLastItemOffTheScreen);

		printUndertest("Before Move:", visibleRows, sas);
		sas = this.undertest.moveSelection(sas, visibleRows, 1);
		printUndertest("After Move:", visibleRows, sas);

		assertEquals(allGroupsList.get(allGroupsList.size() - 1), sas.selectedItem);
		assertEquals(topWhereLastItemOffTheScreen + 1, sas.scrollTop);

		// Second time should not change anything.
		sas = this.undertest.moveSelection(sas, visibleRows, 1);
		assertEquals(topWhereLastItemOffTheScreen + 1, sas.scrollTop);
	}

	@Test
	public void itJumpsAllTheWayToTheTopToRevealTheTitle() throws Exception {
		final List<String> allGroupsList = allGroupsList();
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(0), 1);

		sas = this.undertest.moveSelectionToEnd(sas, 8, VDirection.UP);
		assertEquals(allGroupsList.get(0), sas.selectedItem);
		assertEquals(0, sas.scrollTop);
	}

	@Test
	public void itJumpsAllTheWayToTheBottomToRevealTheEmptyList() throws Exception {
		final int visibleRows = 8;
		final List<String> allGroupsList = allGroupsList();
		int topWhereLastItemOffTheScreen = allGroupsList().size() + this.nonSelectableItems - visibleRows - 1;
		SelectionAndScroll sas = new SelectionAndScroll(allGroupsList.get(0), 0);

		printUndertest("Before Jump:", visibleRows, sas);
		sas = this.undertest.moveSelectionToEnd(sas, visibleRows, VDirection.DOWN);
		printUndertest("After Jump:", visibleRows, sas);

		assertEquals(allGroupsList.get(allGroupsList.size() - 1), sas.selectedItem);
		assertEquals(topWhereLastItemOffTheScreen + 1, sas.scrollTop);
	}

	private List<String> allGroupsList() {
		final List<String> r = new ArrayList<>();
		r.addAll(this.groupA);
		r.addAll(this.groupB);
		r.addAll(this.groupC);
		r.addAll(this.groupD);
		return r;
	}

	private void printUndertest(String title, final int visibleRows, SelectionAndScroll sas) {
		System.out.println();
		System.out.println(title);
		int printed = 0;
		for (int i = sas.scrollTop; i < this.undertest.size(); i++) {
			if (i - sas.scrollTop >= visibleRows) break;
			System.out.println(this.undertest.get(i));
			printed += 1;
		}
		assertEquals(visibleRows, printed);
	}

}
