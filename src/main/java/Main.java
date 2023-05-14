public class Main {

    public static Integer[][] solution1 = {
            {4,7,5,8,1,2,6,9,3},
            {8,2,9,6,3,7,1,4,5},
            {3,1,6,9,5,4,7,2,8},
            {5,6,3,2,4,1,9,8,7},
            {1,4,8,3,7,9,5,6,2},
            {7,9,2,5,8,6,4,3,1},
            {9,5,1,4,2,3,8,7,6},
            {6,3,7,1,9,8,2,5,4},
            {2,8,4,7,6,5,3,1,9},
    };

    static Integer[][] solution2 = {
            {7,1,3,8,9,2,6,5,4},
            {2,8,5,3,6,4,7,9,1},
            {6,4,9,1,5,7,8,3,2},
            {5,6,8,2,3,1,9,4,7},
            {9,7,4,5,8,6,1,2,3},
            {1,3,2,4,7,9,5,8,6},
            {4,2,6,9,1,5,3,7,8},
            {8,9,7,6,2,3,4,1,5},
            {3,5,1,7,4,8,2,6,9},
    };

    static Integer[][] solution3 = {
            {0,0,4,8,3,0,7,1,5},
            {8,3,1,0,7,5,4,0,6},
            {5,7,0,4,1,6,8,0,3},
            {2,0,0,0,0,0,3,0,0},
            {0,0,0,3,6,0,0,0,0},
            {0,0,3,0,0,0,0,0,4},
            {3,0,7,5,4,1,6,8,0},
            {4,0,5,6,9,0,1,3,7},
            {1,0,0,7,0,3,5,4,0},
    };

    static Integer[][] sudoku1 = {
            {4, 7, null, 8, null, null, null, 9, null},
            {null, null, null, null, 3, null, 1, null, null},
            {null, null, 6, null, null, null, null, null, null},
            {5, 6, null, null, null, 1, 9, null, null},
            {null, null, 8, null, null, null, null, 6, null},
            {null, null, 2, 5, null, null, null, null, null},
            {9, 5, null, 4, null, null, null, 7, null},
            {null, null, null, null, null, 8, null, null, 4},
            {2, null, null, null, null, null, null, null, null},
    };

    static Integer[][] sudoku2 = {
            {null, null, 3, null, 9, null, null, null, null},
            {2, 8, null, 3, null, null, null, null, null},
            {6, 4, null, 1, null, 7, 8, 3, null},
            {null, 6, null, null, null, 1, null, null, 7},
            {9, null, null, null, 8, null, 1, null, 3},
            {1, null, null, 4, null, 9, null, 8, null},
            {4, 2, 6, 9, 1, 5, 3, 7, 8},
            {null, null, null, null, null, 3, 4, 1, 5},
            {null, null, null, null, 4, null, 2, null, null},
    };

    static Integer[][] sudoku3 = {
            {null, null, 4, 8, 3, null, 7, 1, 5},
            {8, 3, 1, null, 7, 5, 4, null, 6},
            {5, 7, null, 4, 1, 6, 8, null, 3},
            {2, null, null, null, null, null, 3, null, null},
            {null, null, null, 3, 6, null, null, null, null},
            {null, null, 3, null, null, null, null, null, 4},
            {3, null, 7, 5, 4, 1, 6, 8, null},
            {4, null, 5, 6, 9, null, 1, 3, 7},
            {1, null, null, 7, null, 3, 5, 4, null},
    };

    public static Integer[][] solution = null;

    public static void main(String... args) {

        Integer[][] sudoku = sudoku3;

        SudokuBoard sudokuBoard = new SudokuBoard(sudoku);
        sudokuBoard.printBoard();
        sudokuBoard.solveSudoku();
        sudokuBoard.printBoard();

//        for (int i = 0; i < 9; i++) {
//            for (int j = 0; j < 9; j++) {
//                Point a = sudokuBoard.getPossibilities(new Point(i,j, 3));
//                if (a.getCurrentPossibilities() == 0)
//                    System.out.println("Sí afecta: " + a);
//            }
//        }
    }

}
