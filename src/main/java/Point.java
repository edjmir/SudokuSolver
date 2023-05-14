import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Point {

	private final int row;
	private final int col;
	private final int quadrant;
	private final HashSet<Integer> possibilities;

	public Point(int row, int col, int n) {
		this.row = row;
		this.col = col;
		this.quadrant = col / n + ((row / n) % n) * n;
		this.possibilities = (HashSet<Integer>) IntStream.rangeClosed(1, n * n)
				.boxed()
				.collect(Collectors.toSet());
	}

	public Point(int row, int col, int n, int val) {
		this.row = row;
		this.col = col;
		this.quadrant = col / n + ((row / n) % n) * n;
		this.possibilities = new HashSet<>(1);
		this.possibilities.add(val);
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public int getQuadrant() {
		return quadrant;
	}

	/**
	 * @return possibilities as an Array
	 */
	public HashSet<Integer> getPossibilities() {
		return possibilities;
	}

	public Integer getUniquePossibility() {
		if (possibilities.size() > 1)
			throw new IllegalStateException("The HashSet contains more than one possibility");
		return (Integer) possibilities.toArray()[0];
	}

	/**
	 * Serves to verify if this point has only one
	 * possibility
	 * @return true if there's only one element in the
	 * hashset possibilities, false otherwise
	 */
	public boolean uniquePossibility() {
		return this.possibilities.size() == 1;
	}

	/**
	 * @return the number of possibilities for this point
	 */
	public int getCurrentPossibilities() {
		return this.possibilities.size();
	}

	/**
	 * @return the number of possibilities for this point
	 */
	public boolean isXYWingCandidate(@NotNull Point b) {
		if (this.equals(b))
			return false;
		if (b.getPossibilities().size() != 2)
			return false;
		if (!this.interferes(b))
			return false;
		for (Integer c : b.getPossibilities()) {
			if (this.containsPossibility(c))
				return true;
		}

		return false;
	}

	/**
	 *
	 * @param number Number to verify existence
	 * @return true if the number exists in the hashset,
	 * false otherwise
	 */
	public boolean containsPossibility(Integer number) {
		return this.possibilities.contains(number);
	}

	public boolean containsOnePossibilityInTwo(int[] a) {
		if (a.length != 2)
			throw new IllegalArgumentException("Expected 2 values, got " + a.length);
		return this.possibilities.contains(a[0]) ^ this.possibilities.contains(a[1]);
	}

	/**
	 * @param possibilities Numbers to verify existence
	 * @return true if hashset is a subset of possibilities,
	 * false otherwise
	 */
	public boolean containsOnlyThisPossibilities(Integer... possibilities) {
		int count = 0;
		for (Integer num: possibilities) {
			if (!this.possibilities.contains(num))
				++count;
		}
		return count == possibilities.length - this.possibilities.size();
	}

	public int findCommon(Point point) {
		return possibilities.stream().filter(point::containsPossibility).findFirst().orElse(0);
	}

	/**
	 * Removes the value from the hashset
	 * @param value number to remove
	 * @return true if the value is removed from the HashSet, false otherwise
	 */
	public boolean discardPossibilities(Integer value) {
		return this.possibilities.remove(value);
	}

	/**
	 * Removes the values from the hashset
	 * @param values numbers to remove
	 * @return true if any values is removed from the HashSet, false otherwise
	 */
	public boolean discardPossibilities(Integer... values) {
		int size = possibilities.size();
		for (Integer value: values) {
			this.possibilities.remove(value);
		}
		return possibilities.size() < size;
	}

	/**
	 * Removes all the other values and sets the value
	 * as the unique possibility
	 * @param value number to remove
	 * @return true if changes were made, false if the value was already set as unique
	 */
	public boolean setUniquePossibility(Integer value) {
		if (uniquePossibility())
			return false;
		if (!this.containsPossibility(value))
			return false;
		this.possibilities.clear();
		this.possibilities.add(value);
		return true;
	}

	/**
	 * Removes all the other values and sets the values
	 * as the uniques possibility.
	 * @param possibilities number to remove
	 * @return true if changes were made, false otherwise
	 */
	public boolean setPossibilities(Integer... possibilities) {
		if (Arrays.equals(Arrays.stream(possibilities).sorted().toArray(), this.getPossibilities().toArray()))
			return false;
		this.possibilities.clear();
		this.possibilities.addAll(List.of(possibilities));
		return true;
	}

	@Override
	public boolean equals(@NotNull Object o) {
		if (o instanceof Point)
			return this == o || row == ((Point) o).row && col == ((Point) o).col;
		return false;
	}

	@Override
	public int hashCode() {
		return row << 16 + col;
	}

	@Override
	public String toString() {
		return "Point(" + row + ", " + col + "):\t" + Arrays.toString(getPossibilities().toArray());
	}

	/**
	 * Serves to know if a point interferes with this point
	 * @param b Point to compare
	 * @return true if the point is at the same row, column or quadrant
	 */
	public boolean interferes(Point b) {
		return !this.equals(b) && (b.col == col || b.row == row || b.quadrant == quadrant);
	}
}