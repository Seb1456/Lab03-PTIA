import java.util.ArrayList;
import java.util.List;

/** Conecta 4 (6x7). 'X' = Jugador 1 (empieza), 'O' = Jugador 2, '.' = vacía. */
public class Connect4 implements StateGame {

    private final char[][] board;   // 6 filas x 7 columnas (fila 0 = arriba)
    private final char player;      // jugador con el turno

    public Connect4(char[][] board, char player) {
        this.board = board;
        this.player = player;
    }

    /** Estado inicial: tablero vacío, turno de X. */
    public static Connect4 start() {
        char[][] b = new char[6][7];
        for (char[] row : b) java.util.Arrays.fill(row, '.');
        return new Connect4(b, 'X');
    }

    /** Deja caer la ficha en la columna col (casilla vacía más baja). */
    public Connect4 play(int col) {
        if (board[0][col] != '.') throw new IllegalArgumentException("Columna llena");
        char[][] b = new char[6][];
        for (int r = 0; r < 6; r++) b[r] = board[r].clone();   // copia del tablero
        for (int r = 5; r >= 0; r--) {
            if (b[r][col] == '.') { b[r][col] = player; break; }
        }
        return new Connect4(b, rival(player));
    }

    /** Todas las líneas de 4 casillas seguidas (horizontal, vertical y diagonales). */
    private List<char[]> lines() {
        List<char[]> out = new ArrayList<>();
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int r = 0; r < 6; r++)
            for (int c = 0; c < 7; c++)
                for (int[] d : dirs) {
                    int er = r + 3 * d[0], ec = c + 3 * d[1];
                    if (er >= 0 && er < 6 && ec >= 0 && ec < 7) {
                        char[] line = new char[4];
                        for (int i = 0; i < 4; i++) line[i] = board[r + i * d[0]][c + i * d[1]];
                        out.add(line);
                    }
                }
        return out;
    }

    private static int count(char[] line, char p) {
        int n = 0;
        for (char x : line) if (x == p) n++;
        return n;
    }

    /** 'X' u 'O' si alguno tiene 4 en línea; '.' si nadie. */
    public char winner() {
        for (char[] line : lines())
            if (line[0] != '.' && count(line, line[0]) == 4) return line[0];
        return '.';
    }

    @Override
    public List<StateGame> actionResults() {
        List<StateGame> out = new ArrayList<>();
        if (isTerminal()) return out;
        for (int c = 0; c < 7; c++) if (board[0][c] == '.') out.add(play(c));
        return out;
    }

    @Override
    public boolean isTerminal() {
        if (winner() != '.') return true;
        for (int c = 0; c < 7; c++) if (board[0][c] == '.') return false;
        return true;   // tablero lleno: empate
    }

    @Override
    public char toMove() { return player; }

    @Override
    public char rival(char p) { return p == 'X' ? 'O' : 'X'; }

    @Override
    public int heuristicUtility(char p) {
        char w = winner();
        if (w != '.') return w == p ? 1000 : -1000;
        int score = 0;
        for (char[] line : lines()) {
            int mine = count(line, p), theirs = count(line, rival(p));
            if (theirs == 0) score += mine;
            else if (mine == 0) score -= theirs;
        }
        return score;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (char[] row : board) {
            for (char x : row) sb.append(x).append(' ');
            sb.append('\n');
        }
        return sb.append("0 1 2 3 4 5 6").toString();
    }
}
