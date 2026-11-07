import java.util.List;

/**
 * Contrato (equivalente Java de la clase abstracta StateGame del notebook)
 * que debe cumplir cualquier juego por turnos de dos jugadores para poder
 * ser resuelto con {@link AlphaBetaEngine}.
 */
public interface StateGame {

    /** Sucesores: un estado nuevo por cada movimiento legal del jugador en turno. */
    List<StateGame> actionResults();

    /** true si el juego terminó (alguien ganó o no hay más jugadas). */
    boolean isTerminal();

    /** Identificador del jugador que tiene el turno. */
    char toMove();

    /** Identificador del rival de {@code player}. */
    char rival(char player);

    /** Utilidad (exacta si es terminal, heurística si no) desde el punto de vista de {@code player}. */
    int heuristicUtility(char player);

    /** Representación del estado como texto. */
    String toString();
}
