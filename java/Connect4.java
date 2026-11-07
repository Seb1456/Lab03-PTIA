import java.util.ArrayList;
import java.util.List;

/**
 * Conecta 4 (6 filas x 7 columnas). Equivalente Java de la clase Connect4 del notebook.
 *
 * Estado inmutable:
 *  - board   : 42 casillas, índice = fila * 7 + columna, fila 0 ABAJO
 *  - heights : fichas en cada columna (= fila donde cae la siguiente ficha)
 *  - player  : jugador con el turno ("1" o "2")
 *  - move    : columna jugada para llegar a este estado (-1 en el inicial)
 *  - winnerId: ganador o null
 */
public final class Connect4 implements StateGame {

    public static final int ROWS = 6, COLS = 7, CONNECT = 4;
    public static final String P1 = "1", P2 = "2", EMPTY = ".";
    /** Columnas centrales primero (mejora la poda alfa-beta). */
    private static final int[] ORDER = {3, 2, 4, 1, 5, 0, 6};
    private static final int[] WINDOW_WEIGHT = {0, 1, 5, 50};
    private static final int CENTER_WEIGHT = 3;
    private static final int WIN_SCORE = 100000;
    /** Direcciones: horizontal, vertical, diagonal /, diagonal \ */
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
    /** Las 69 ventanas de 4 casillas consecutivas. */
    private static final int[][] WINDOWS = buildWindows();

    private final String[] board;
    private final int[] heights;
    private final String player;
    private final int move;
    private final String winnerId;

    private Connect4(String[] board, int[] heights, String player, int move) {
        this.board = board;
        this.heights = heights;
        this.player = player;
        this.move = move;
        this.winnerId = checkWinner();
    }

    private static int[][] buildWindows() {
        List<int[]> list = new ArrayList<>();
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                for (int[] d : DIRS) {
                    int er = r + (CONNECT - 1) * d[0], ec = c + (CONNECT - 1) * d[1];
                    if (er >= 0 && er < ROWS && ec >= 0 && ec < COLS) {
                        int[] w = new int[CONNECT];
                        for (int i = 0; i < CONNECT; i++) w[i] = (r + i * d[0]) * COLS + (c + i * d[1]);
                        list.add(w);
                    }
                }
        return list.toArray(new int[0][]);
    }

    // ------------------------------------------------------------ auxiliares
    private String cell(int r, int c) { return board[r * COLS + c]; }

    /** Revisa si la última ficha formó 4 en línea (solo hace falta mirar alrededor de ella). */
    private String checkWinner() {
        if (move < 0) return null;
        int c = move, r = heights[c] - 1;
        String p = cell(r, c);
        for (int[] d : DIRS) {
            int count = 1;
            for (int sign = 1; sign >= -1; sign -= 2) {
                int rr = r + sign * d[0], cc = c + sign * d[1];
                while (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS && cell(rr, cc).equals(p)) {
                    count++;
                    rr += sign * d[0];
                    cc += sign * d[1];
                }
            }
            if (count >= CONNECT) return p;
        }
        return null;
    }

    public int piecesPlayed() {
        int n = 0;
        for (int h : heights) n += h;
        return n;
    }

    public List<Integer> legalActions() {
        List<Integer> acts = new ArrayList<>();
        if (isTerminal()) return acts;
        for (int c : ORDER) if (heights[c] < ROWS) acts.add(c);
        return acts;
    }

    /** Aplica una jugada y devuelve un estado NUEVO; valida las reglas 3, 4, 5 y 8. */
    public Connect4 result(int column) {
        if (isTerminal())
            throw new IllegalStateException("El juego ya terminó: no se permiten jugadas adicionales");
        if (column < 0 || column >= COLS)
            throw new IllegalArgumentException("Columna inválida: " + column + " (debe estar entre 0 y 6)");
        int r = heights[column];
        if (r >= ROWS)
            throw new IllegalArgumentException("La columna " + column + " está llena");
        String[] nb = board.clone();
        nb[r * COLS + column] = player;            // gravedad: fila libre más baja
        int[] nh = heights.clone();
        nh[column]++;
        return new Connect4(nb, nh, rival(player), column);
    }

    public String winner() { return winnerId; }
    public int getMove()   { return move; }

    // ------------------------------------------------------------- StateGame
    /** start(): tablero vacío, empieza el Jugador 1. */
    public static Connect4 start() { return start(""); }

    /** start(moves): aplica una secuencia de columnas, p. ej. "3324". */
    public static Connect4 start(String moves) {
        String[] b = new String[ROWS * COLS];
        java.util.Arrays.fill(b, EMPTY);
        Connect4 s = new Connect4(b, new int[COLS], P1, -1);
        for (char ch : moves.toCharArray()) s = s.result(ch - '0');
        return s;
    }

    @Override
    public List<StateGame> actionResults() {
        List<StateGame> out = new ArrayList<>();
        for (int c : legalActions()) out.add(result(c));
        return out;
    }

    @Override
    public boolean isTerminal() {
        return winnerId != null || piecesPlayed() == ROWS * COLS;
    }

    @Override
    public String toMove() { return player; }

    @Override
    public String rival(String p) { return P1.equals(p) ? P2 : P1; }

    @Override
    public int heuristicUtility(String p) {
        int empties = ROWS * COLS - piecesPlayed();
        if (winnerId != null) {
            int bonus = WIN_SCORE + empties;
            return winnerId.equals(p) ? bonus : -bonus;
        }
        if (empties == 0) return 0;
        String opp = rival(p);
        int score = 0;
        for (int[] w : WINDOWS) {
            int mine = 0, theirs = 0;
            for (int i : w) {
                if (board[i].equals(p)) mine++;
                else if (board[i].equals(opp)) theirs++;
            }
            if (theirs == 0) score += WINDOW_WEIGHT[mine];
            else if (mine == 0) score -= WINDOW_WEIGHT[theirs];
        }
        for (int r = 0; r < ROWS; r++) {
            if (cell(r, 3).equals(p)) score += CENTER_WEIGHT;
            else if (cell(r, 3).equals(opp)) score -= CENTER_WEIGHT;
        }
        return score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = ROWS - 1; r >= 0; r--) {
            sb.append("|");
            for (int c = 0; c < COLS; c++) {
                String v = cell(r, c);
                sb.append(' ').append(v.equals(P1) ? "X" : v.equals(P2) ? "O" : ".");
            }
            sb.append(" |\n");
        }
        sb.append("+---------------+\n  0 1 2 3 4 5 6");
        return sb.toString();
    }
}
