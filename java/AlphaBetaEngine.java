/**
 * MiniMax con poda alfa-beta.
 *  - MAX = jugador en turno en la raíz; MIN = su rival.
 *  - alpha: mejor valor garantizado para MAX en el camino actual.
 *  - beta : mejor valor garantizado para MIN en el camino actual.
 *  - Si alpha >= beta se dejan de explorar los hermanos restantes (poda).
 */
public class AlphaBetaEngine {

    private char player;      // jugador MAX
    private long nodes;       // nodos visitados en la última búsqueda
    private long prunes;      // cortes realizados
    private int lastValue;    // valor MiniMax de la jugada elegida

    /** Devuelve el mejor estado sucesor de s (null si s es terminal). */
    public StateGame search(StateGame s, int depth) {
        player = s.toMove();
        nodes = 1;
        prunes = 0;
        int alpha = Integer.MIN_VALUE, beta = Integer.MAX_VALUE;
        StateGame best = null;
        int bestValue = Integer.MIN_VALUE;
        for (StateGame child : s.actionResults()) {
            int v = minValue(child, depth - 1, alpha, beta);
            if (best == null || v > bestValue) {
                bestValue = v;
                best = child;
            }
            alpha = Math.max(alpha, bestValue);
        }
        lastValue = bestValue;
        return best;
    }

    /** Valor de un nodo donde juega MAX (elige el máximo de sus hijos). */
    public int maxValue(StateGame s, int depth, int alpha, int beta) {
        nodes++;
        if (s.isTerminal() || depth <= 0) return s.heuristicUtility(player);
        int v = Integer.MIN_VALUE;
        for (StateGame child : s.actionResults()) {
            v = Math.max(v, minValue(child, depth - 1, alpha, beta));
            if (v >= beta) { prunes++; return v; }   // poda beta
            alpha = Math.max(alpha, v);
        }
        return v;
    }

    /** Valor de un nodo donde juega MIN (elige el mínimo de sus hijos). */
    public int minValue(StateGame s, int depth, int alpha, int beta) {
        nodes++;
        if (s.isTerminal() || depth <= 0) return s.heuristicUtility(player);
        int v = Integer.MAX_VALUE;
        for (StateGame child : s.actionResults()) {
            v = Math.min(v, maxValue(child, depth - 1, alpha, beta));
            if (v <= alpha) { prunes++; return v; }  // poda alfa
            beta = Math.min(beta, v);
        }
        return v;
    }

    public long getNodes()  { return nodes; }
    public long getPrunes() { return prunes; }
    public int getLastValue() { return lastValue; }
}
