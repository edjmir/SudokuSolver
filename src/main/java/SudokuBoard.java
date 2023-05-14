import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SudokuBoard {
	private final Integer[][] board;
	boolean stop = false;
	private final int size;
	private final static int HIDDEN_TRIPLETS_MIN_UNKNOWNS = 7;
	private final static int TRIPLETS_MAX_APPEARANCES = 3;

	BiFunction<Point, Point, Boolean> columnFilter =
			(A, B) -> A.getCol() == B.getCol();
	BiFunction<Point, Point, Boolean> rowFilter =
			(A, B) -> A.getRow() == B.getRow();
	BiFunction<Point, Point, Boolean> quadrantFilter =
			(A, B) -> A.getQuadrant() == B.getQuadrant();

	Function<String, BiFunction<Point, Point, Boolean>> filter_selector =
			(filter) -> switch (filter) {
				case "col" -> columnFilter;
				case "row" -> rowFilter;
				default -> quadrantFilter;
			};
	private final HashSet<Point> points;

	public SudokuBoard(Integer[][] board) throws IllegalArgumentException {
		this.board = board;
		this.size = board.length;
		int n = (int) Math.sqrt(size);

		if (Math.pow(n, 2) != size)
			throw new IllegalArgumentException("The matrix must be square");

		this.points = new HashSet<>(size * size);

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (board[i][j] != null)
					this.points.add(new Point(i, j, n, board[i][j]));
				else
					this.points.add(new Point(i, j, n));
			}
		}
	}

	/**
	 * Starts solving the sudoku
	 */
	public void solveSudoku() {

		boolean changes = true;

		while (changes) {
			long err = points.stream()
					.filter(p -> p.getPossibilities().size() == 0)
					.count();
			if (err > 0) {
				points.stream()
						.filter(p -> p.getPossibilities().size() == 0)
						.forEach(System.out::println);
				break;
			}
			changes = discardUniquePossibilities();
			if (!changes) {
				changes = discardMandatoryNumberInColOrRowForQuadrant();
				System.out.println("discardMandatoryNumberInColOrRowForQuadrant");
			}
			if (!changes) {
				changes = discardFromQuadrantPossibilities();
				System.out.println("discardFromQuadrantPossibilities");
			}
			if (!changes) {
				changes = discardFromXYWing();
				System.out.println("discardFromXYWing");
			}
			if (!changes) {
				changes = discardFromXWing();
				System.out.println("discardFromXWing");
			}
//			if (!changes)
//				changes = spotHiddenPairs();
//			if (!changes)
//				changes = spotNakedTriplets();
		}
		updateBoard();
	}

	/*
		To DELETE
	 */
	public Point getPossibilities(Point point) {
		return points.stream()
				.filter(p -> p.getCol() == point.getCol())
				.filter(p -> p.getRow() == point.getRow())
				.findFirst().orElse(null);
	}

	/**
	 * If there's one set value, then erases that value from the other points possibilities
	 * @return boolean true if the operation discards at least one item
	 */
	private boolean discardUniquePossibilities() {
		AtomicBoolean changes = new AtomicBoolean(false);
		points.stream()
				.filter(Point::uniquePossibility)
				.forEach(point -> {
					points.stream()
							.filter(point::interferes)
							.forEach(p -> {
								if (p.discardPossibilities(point.getUniquePossibility())) {
									changes.set(true);
									bugCatcher("discardUniquePossibilities", point.getUniquePossibility(), p);
								}

							});
				});
		return changes.get();
	}

	/**
	 * If there's a quadrant where one number can only be in one column or in one row
	 * then that possibility is erased from the same column or row in the other
	 * quadrants
	 * @return true if there was at least one value discarded
	 */
	private boolean discardMandatoryNumberInColOrRowForQuadrant() {
		AtomicBoolean changes = new AtomicBoolean(false);

		for (int i = 0; i < size; i++) {
			int finalI = i;
			List<Point> quadrant = points.stream()
					.filter(point -> point.getQuadrant() == finalI)
					.collect(Collectors.toList());
			IntStream.rangeClosed(1, size).forEachOrdered(number -> {
				Set<Integer> column = quadrant.stream()
						.filter(point -> point.containsPossibility(number))
						.mapToInt(Point::getCol)
						.boxed()
						.collect(Collectors.toSet());
				if (column.size() == 1) {
					points.stream()
							.filter(point -> point.getCol() == column.stream().findFirst().orElse(0))
							.filter(point -> point.getQuadrant() != finalI)
							.forEach(point -> {
								if (point.discardPossibilities(number)) {
									changes.set(true);
									bugCatcher("discardMandatoryNumberInColOrRowForQuadrant", number, point);
								}
							});
				}


				Set<Integer> row = quadrant.stream()
						.filter(point -> point.containsPossibility(number))
						.mapToInt(Point::getRow)
						.boxed()
						.collect(Collectors.toSet());

				if (row.size() == 1) {
					points.stream()
							.filter(point -> point.getRow() == row.stream().findFirst().orElse(0))
							.filter(point -> point.getQuadrant() != finalI)
							.filter(point -> point.containsPossibility(number))
							.forEach(point -> {
								if (point.discardPossibilities(number)) {
									changes.set(true);
									bugCatcher("discardMandatoryNumberInColOrRowForQuadrant", number, point);
								}
							});
				}
			});

		}
		return changes.get();
	}

	/**
	 * If the quadrant has only one possibility for a number to be
	 * then it's set as the unique possibility
	 * @return true if at least one item is discarded, false otherwise
	 */
	private boolean discardFromQuadrantPossibilities() {
		AtomicBoolean changes = new AtomicBoolean(false);
		for (int i = 0; i < size; i++) {
			int finalI = i;
			HashMap<Integer, ArrayList<Point>> map =  new HashMap<>(size);
			points.stream()
				.filter(point -> point.getQuadrant() == finalI)
				.forEach(point -> {
					point.getPossibilities().forEach(possibility -> {
						if (map.containsKey(possibility))
							map.get(possibility).add(point);
						else
							map.put(possibility, new ArrayList<>(List.of(point)));
					});
				});

			map.forEach((k, v) -> {
				if (v.size() == 1) {
					if (v.get(0).setUniquePossibility(k)) {
						changes.set(true);
						bugCatcher(v.get(0), "discardFromQuadrantPossibilities", k);
					}
				}
			});

		}
		return changes.get();
	}

	/**
	 * We need to find a pivot XY that intersects 2 other cells with XZ and YZ numbers
	 * All the cells that intersects both the 2 other cells discard automatically Z
	 * @return true if possibilities were discarded
	 */
	private boolean discardFromXYWing() {
		AtomicBoolean changes = new AtomicBoolean(false);
		List<Point> xy = points.stream()
				.filter(point -> point.getCurrentPossibilities() == 2)
				.collect(Collectors.toList());

		xy.forEach(pivot -> {
			List<Point> wings = xy.stream()
					.filter(p -> p.interferes(pivot) && !p.equals(pivot))
					.filter(p -> p.containsOnePossibilityInTwo(
							pivot.getPossibilities()
									.stream()
									.mapToInt(Integer::valueOf)
									.toArray()
							)
					)
					.collect(Collectors.toList());

			if (wings.size() == 2) {
				int[] xy_values = pivot.getPossibilities().stream()
						.mapToInt(Integer::valueOf)
						.toArray();
//				if (wings.get(0).interferes(wings.get(1)))
//					return;
				boolean conditions = wings.stream().anyMatch(point -> point.containsPossibility(xy_values[0]));
				conditions = conditions && wings.stream().anyMatch(point -> point.containsPossibility(xy_values[1]));

				if (!conditions)
					return;

				int discard = wings.get(0).findCommon(wings.get(1));
				if (discard == 0)
					return;
				points.stream()
						.filter(point -> point.interferes(wings.get(0))
								&& point.interferes(wings.get(1))
						).forEach(point -> {
							if (stop)
								return;
							if (point.discardPossibilities(discard)){
								changes.set(true);
								bugCatcher("discardFromXYWing", discard, point);
							}
						});
			}

		});

		return changes.get();
	}

	/**
	 * If we find that a row can only contain a number "n" in two cells
	 * and also another row that has the same condition in the same cols,
	 * all the other "n" possibilities of the column are discarded
	 * @return true if possibilities were discarded
	 */
	private boolean discardFromXWing() {
		AtomicBoolean changes = new AtomicBoolean(false);
		for (int i = 0; i < size; i++) {
			Map<Integer, List<Point>> row = findRowXWing(i);
			if (row == null)
				continue;
			Integer key = (Integer) row.keySet().toArray()[0];

			Point a = row.get(key).get(0);
			Point b = row.get(key).get(1);
			List<Point> mirror_index = getRowXWing(i + 1, key, a, b);
			if (mirror_index == null)
				continue;

			points.stream()
					.filter(point -> point.getCol() == a.getCol() || point.getCol() == b.getCol())
					.filter(point -> point.containsPossibility(key))
					.filter(point -> !point.equals(a) && !point.equals(b))
					.filter(point -> !mirror_index.contains(point))
					.forEach(point -> {
						if (point.discardPossibilities(key))
							changes.set(true);
					});
		}

		return changes.get();
	}

	/**
	 * Finds if the row match the conditions to apply X-Wing technique
	 * @param row row to check
	 * @return the row if it matches the condition, null otherwise
	 */
	private Map<Integer, List<Point>> findRowXWing(int row) {
		List<Point> wing = getRowMissingPoints(row);
		for (int i = 0; i < size; ++i) {
			int possibility = ++i;
			List<Point> x = wing.stream().filter(point -> point.containsPossibility(possibility))
					.collect(Collectors.toList());
			if (x.size() == 2) {
				Map<Integer, List<Point>> x_wing = new HashMap<>(1);
				x_wing.put(i, x);
				return x_wing;
			}
		}
		return null;
	}

	/**
	 * search for a mirror row in x-wing technique
	 * @param row row to start checking
	 * @return the mirror row index if exists, null otherwise
	 */
	private List<Point> getRowXWing(int row, int number, Point a, Point b) {
		for (int i = row; i < size; ++i) {
			List<Point> mirror = getRowMissingPoints(row);
			List<Point> x = mirror.stream()
					.filter(point -> point.containsPossibility(number))
					.collect(Collectors.toList());
			if (x.size() == 2) {
				return x;
			}
		}
		return null;
	}

	/**
	 * @param row row to find
	 * @return a list with the points with more than one possibility
	 */
	private List<Point> getRowMissingPoints(int row) {
		return points.stream()
				.filter(point -> point.getRow() == row)
				.filter(point -> point.getCurrentPossibilities() > 1)
				.collect(Collectors.toList());
	}

	/**
	 * If we find that a row can only contain a two numbers "x" "y" in two cells
	 * of the same house, all the other "n" possibilities of the house are discarded
	 * @return true if possibilities were discarded
	 */
	private boolean spotHiddenPairs() {
		AtomicBoolean changes = new AtomicBoolean(false);

		IntStream.rangeClosed(1, size).forEach(number -> {
			List<Point> number_positions = points.stream()
					.filter(point -> !point.uniquePossibility())
					.filter(point -> point.containsPossibility(number))
					.collect(Collectors.toList());

			number_positions.forEach(point -> {

				HashMap<String, List<Point>> houses = mapFilters(number_positions, point);

				houses.forEach((key, house) -> {
					if (house.size() == 2) {
						List<Integer> values = new ArrayList<>();
						house.forEach(p -> values.addAll(p.getPossibilities()));
						values.removeIf(n -> n == number);
						values.removeIf(n -> Collections.frequency(values, n) != 2);
						boolean hidden_pair;
						if (values.size() == 2) {
							hidden_pair = points.stream()
									.filter(p -> filter_selector.apply(key).apply(p, point))
									.filter(p -> p.containsPossibility(values.get(0)))
									.count() == 2;
						} else hidden_pair = false;

						if (hidden_pair) {
							house.forEach(p -> {
								if (p.setPossibilities(number, values.get(0)))
									changes.set(true);
							});
						}
					}
				});
			});
		});

		return changes.get();
	}

	/**
	 * If we find that a row contain three numbers "x" "y" "z" in three cells
	 * all the other "x" "y" "z" possibilities of the house are discarded
	 * @return true if possibilities were discarded
	 */
	private boolean spotNakedTriplets() {
		AtomicBoolean changes = new AtomicBoolean(false);

		List<Point> triplets = points.stream()
				.filter(point -> point.getCurrentPossibilities() <= 3)
				.collect(Collectors.toList());

		triplets.stream().filter(point -> !point.uniquePossibility()).forEach(point -> {
			List<Integer> number = new ArrayList<>(point.getPossibilities());
			int x = number.get(0);
			int y = number.get(1);
			AtomicInteger z = new AtomicInteger(0);

			HashMap<String, List<Point>> houses = mapFilters(triplets, point);

			houses.forEach((key, house) -> {

				List<Integer> candidates = new ArrayList<>();
				List<Point> pts = house.stream()
						.filter(p -> p.containsPossibility(x) || p.containsPossibility(y))
						.filter(p -> {
							if (p.getCurrentPossibilities() == 3)
								return p.containsPossibility(x) && p.containsPossibility(y);
							return true;
						}).collect(Collectors.toList());

				pts.forEach(p -> candidates.addAll(p.getPossibilities()));

				Set<Integer> z_candidates = candidates.stream()
						.filter(n -> Collections.frequency(candidates, n) >= 2)
						.filter(n -> n != x)
						.filter(n -> n != y)
						.collect(Collectors.toSet());

				boolean valid_triplet = false;

				if (z_candidates.size() == 1) {
					z.set((int) z_candidates.toArray()[0]);
					HashMap<Integer, Integer> counter =  new HashMap<>(3);
					List<Point> temp = triplets.stream()
							.filter(p -> p.containsOnlyThisPossibilities(x, y, z.get()))
							.filter(p -> filter_selector.apply(key).apply(p, point))
							.collect(Collectors.toList());
					temp.forEach(p -> {
								p.getPossibilities().forEach(possibility -> {
									if (counter.containsKey(possibility))
										counter.put(possibility, counter.get(possibility) + 1);
									else
										counter.put(possibility, 1);
								});
							});

					valid_triplet = counter.values().stream().allMatch(n -> n >= 2) && temp.size() == 3;
				}
				if (valid_triplet) {
					List<Point> removal = points.stream()
							.filter(p -> !p.uniquePossibility())
							.filter(p -> filter_selector.apply(key).apply(p, point))
							.filter(p -> !p.containsOnlyThisPossibilities(x, y, z.get()))
							.collect(Collectors.toList());

					System.out.println(key + " Removing (" + x + ", " + y + ", " + z+") from " + removal);
					System.out.println(triplets.stream()
							.filter(p -> p.containsOnlyThisPossibilities(x, y, z.get()))
							.filter(p -> filter_selector.apply(key).apply(p, point)).collect(Collectors.toSet()));

					removal.forEach(p -> {
						bugCatcher("NakedTriplets (" + x + ", " + y + ", " + z.get() + ")", x, p);
						bugCatcher("NakedTriplets (" + x + ", " + y + ", " + z.get() + ")", y, p);
						bugCatcher("NakedTriplets (" + x + ", " + y + ", " + z.get() + ")", z.get(), p);
						if (p.discardPossibilities(x, y, z.get()))
							changes.set(true);
					});
				}
			});

		});

		return changes.get();
	}

	/**
	 * @param set List of Points to filter by house
	 * @param point Reference point
	 * @return a map with subsets of each house
	 */
	private HashMap<String, List<Point>> mapFilters(List<Point> set, Point point) {
		HashMap<String, List<Point>> houses = new HashMap<>();

		houses.put("col", set.stream()
				.filter(p -> columnFilter.apply(p, point))
				.filter(p -> !p.uniquePossibility())
				.collect(Collectors.toList())
		);
		houses.put("row", set.stream()
				.filter(p -> rowFilter.apply(p, point))
				.filter(p -> !p.uniquePossibility())
				.collect(Collectors.toList())
		);
		houses.put("quadrant", set.stream()
				.filter(p -> quadrantFilter.apply(p, point))
				.filter(p -> !p.uniquePossibility())
				.collect(Collectors.toList())
		);
		return houses;
	}

	/**
	 * Sets all points with only one possibility
	 */
	private void updateBoard() {
		points.stream()
				.filter(Point::uniquePossibility)
				.forEach(point ->
					board[point.getRow()][point.getCol()] = point.getUniquePossibility()
				);
	}

	/**
	 * Prints the board in a good looking format
	 */
	public void printBoard() {
		updateBoard();
		StringBuilder stringBuilder = new StringBuilder(board.length * ((board.length + 1) * 4));
		for (int i = 0; i < board.length; i++) {
			if(i % 3 == 0)
				stringBuilder.append("-".repeat(board.length * 5))
						.append('\n');
			for (int j = 0; j < board.length; j++) {
				if(j % 3 == 0)
					stringBuilder.append("|\t");
				stringBuilder.append(board[i][j] == null ? "-" : board[i][j])
						.append('\t');
			}
			stringBuilder.append('\n');
		}
		System.out.println(stringBuilder);
	}

	/**
	 * @param method Method to test
	 * @param number2discard the number that is goind to be discarded
	 * @param point point from where we are going to remove that possibility
	 */
	private void bugCatcher(String method, Integer number2discard, Point point) {
		if (stop || Objects.isNull(Main.solution))
			return;
		if (Objects.equals(Main.solution[point.getRow()][point.getCol()], number2discard)) {
			System.out.print(method + " does not work");
			System.out.println("\tDiscarding " + number2discard + " from (" + point.getRow() + ", " + point.getCol() + ") = " + Main.solution[point.getRow()][point.getCol()]);
			stop = true;
			printBoard();
		}
	}
	private void bugCatcher(Point point, String method, Integer uniquePossibility) {
		if (stop || Objects.isNull(Main.solution))
			return;
		if (!Objects.equals(Main.solution[point.getRow()][point.getCol()], uniquePossibility)) {
			System.out.print(method + " does not work");
			System.out.println("\tSetting " + uniquePossibility + " as (" + point.getRow() + ", " + point.getCol() + ") = " + Main.solution[point.getRow()][point.getCol()]);
			stop = true;
		}
	}

}

/*
private boolean spotHiddenTriplets() {
		AtomicBoolean changes = new AtomicBoolean(false);
		IntStream.range(0, size).forEach(i -> {
			List<Point> house = points.stream()
					.filter(point -> point.getQuadrant() == i)
					.filter(point -> point.getCurrentPossibilities() > 1)
					.collect(Collectors.toList());

			List<Point> clone = new ArrayList<>(house.size());

			try {
				for (Point point : house)
					clone.add(point.clone());
			} catch (CloneNotSupportedException e ) {
				return;
			}


//			if (house.size() < HIDDEN_TRIPLETS_MIN_UNKNOWNS)
//				return;

			SortedMap<Integer, Integer> counter = new TreeMap<>();
			house.forEach(point -> {
				point.getPossibilities().forEach(possibility -> {
					if (counter.containsKey(possibility))
						counter.put(possibility, counter.get(possibility) + 1);
					else counter.put(possibility, 1);
				});
			});

			IntStream.rangeClosed(1, size).forEach(n -> {
				if (counter.containsKey(n) && counter.get(n) > TRIPLETS_MAX_APPEARANCES)
					clone.stream()
							.filter(point -> point.containsPossibility(n))
							.forEach(point -> point.discardPossibilities(n));
			});

			if (counter.size() < TRIPLETS_MAX_APPEARANCES)
				return;

			System.out.println("***************************************");
			System.out.println("Quadrant " + i);
			counter.forEach((k, v) -> System.out.println(k + " : " + v));
			System.out.println("***************************************");

		});

		return changes.get();
	}
 */