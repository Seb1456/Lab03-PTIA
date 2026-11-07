import java.util.ArrayList;
import java.util.List;

/**
 * Tres en raya. Estado inmutable: tablero de 9 casillas + jugador en turno.
 *
 *   0 | 1 | 2
 *   3 | 4 | 5
 *   6 | 7 | 8
 */
public final class TicTacToe implements StateGame {

    public static final char EMPTY = ' ';
    private static final int WIN_SCORE = 100;
    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},   // filas
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},   // columnas
        {0, 4, 8}, {2, 4, 6}               // diagonales
    };
    /** Orden de exploración: centro, esquinas, bordes (mejora la poda). */
    private static final int[] ORDER = {4, 0, 2, 6, 8, 1, 3, 5, 7};
    private static final int[] LINE_VALUE = {0, 1, 10};

    private final char[] board;
    private final char player;
    private final int move;          // casilla jugada para llegar aquí (-1 = inicial)

    public TicTacToe(char[] board, char player, int move) {
        if (board.length != 9) throw new IllegalArgumentException("El tablero debe tener 9 casillas");
        if (player != 'X' && player != 'O') throw new IllegalArgumentException("Jugador inválido");
        this.board = board.clone();  // copia defensiva -> inmutabilidad
        this.player = player;
        this.move = move;
    }

    /** start(): tablero vacío, inicia X. */
    public static TicTacToe start() {
        return start("         ", 'X');
    }

    /** start(board, player): estado inicial a partir de una cadena de 9 caracteres. */
    public static TicTacToe start(String cells, char player) {
        char[] b = new char[9];
        for (int i = 0; i < 9; i++) {
            char c = cells.charAt(i);
            b[i] = (c == 'X' || c == 'O') ? c : EMPTY;
        }
        return new TicTacToe(b, player, -1);
    }

    public int getMove() { return move; }

    @Override
    public List<StateGame> actionResults() {
        List<StateGame> successors = new ArrayList<>();
        if (isTerminal()) return successors;
        for (int i : ORDER) {
            if (board[i] == EMPTY) {
                char[] nb = board.clone();
                nb[i] = player;
                successors.add(new TicTacToe(nb, rival(player), i));
            }
        }
        return successors;
    }

    /** 'X' u 'O' si alguno completó una línea; EMPTY si nadie ha ganado. */
    public char winner() {
        for (int[] l : LINES) {
            if (board[l[0]] != EMPTY && board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]]) {
                return board[l[0]];
            }
        }
        return EMPTY;
    }

    private int countEmpty() {
        int n = 0;
        for (char c : board) if (c == EMPTY) n++;
        return n;
    }

    @Override
    public boolean isTerminal() {
        return winner() != EMPTY || countEmpty() == 0;
    }

    @Override
    public char toMove() { return player; }

    @Override
    public char rival(char p) { return p == 'X' ? 'O' : 'X'; }

    @Override
    public int heuristicUtility(char p) {
        char w = winner();
        int empties = countEmpty();
        if (w != EMPTY) return (w == p) ? WIN_SCORE + empties : -(WIN_SCORE + empties);
        if (empties == 0) return 0;
        char opp = rival(p);
        int score = 0;
        for (int[] l : LINES) {
            int mine = 0, theirs = 0;
            for (int i : l) {
                if (board[i] == p) mine++;
                else if (board[i] == opp) theirs++;
            }
            if (theirs == 0) score += LINE_VALUE[mine];
            else if (mine == 0) score -= LINE_VALUE[theirs];
        }
        return score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 3; r++) {
            sb.append(' ').append(board[r * 3]).append(" | ")
              .append(board[r * 3 + 1]).append(" | ").append(board[r * 3 + 2]);
            if (r < 2) sb.append("\n---+---+---\n");
        }
        return sb.toString();
    }
}
