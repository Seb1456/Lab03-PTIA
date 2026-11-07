/** Partidas agente vs agente desde dos estados iniciales distintos. */
public class Main {

    static void play(TicTacToe initial, int depthX, int depthO) {
        AlphaBetaEngine engine = new AlphaBetaEngine();
        TicTacToe s = initial;
        System.out.println("Estado inicial (turno de " + s.toMove() + "):\n" + s + "\n");
        int turn = 1;
        while (!s.isTerminal()) {
            char p = s.toMove();
            s = (TicTacToe) engine.search(s, p == 'X' ? depthX : depthO);
            System.out.printf("Jugada %d: %c -> casilla %d | valor=%d | nodos=%d | podas=%d%n",
                    turn++, p, s.getMove(), engine.getLastValue(), engine.getNodes(), engine.getPrunes());
            System.out.println(s + "\n");
        }
        char w = s.winner();
        System.out.println("RESULTADO: " + (w == TicTacToe.EMPTY ? "empate" : "gana " + w) + "\n");
    }

    public static void main(String[] args) {
        System.out.println("=== ESTADO INICIAL 1: tablero vacío, inicia X ===");
        play(TicTacToe.start(), 9, 9);
        System.out.println("=== ESTADO INICIAL 2: X en 0, O en 1, turno de X ===");
        play(TicTacToe.start("XO       ", 'X'), 9, 9);
    }
}
