/** Verificación de las 8 reglas oficiales de Conecta 4 (Parte I). */
public class Main {

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "  [OK] " : "  [FALLA] ") + msg);
    }

    static void expectError(Runnable r, String msg) {
        try {
            r.run();
            System.out.println("  [FALLA] no se lanzó error: " + msg);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("  [OK] " + msg + " -> " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Connect4 s = Connect4.start();
        System.out.println(s + "\n");
        System.out.println("Reglas 1-2: tablero 6x7 y empieza el Jugador 1");
        check(s.toMove().equals("1") && s.actionResults().size() == 7, "turno inicial: " + s.toMove());

        System.out.println("Regla 3: turnos alternos");
        check(Connect4.start("3").toMove().equals("2") && Connect4.start("30").toMove().equals("1"), "J1 -> J2 -> J1");

        System.out.println("Regla 4: columna 0..6");
        expectError(() -> Connect4.start().result(7), "columna 7");
        expectError(() -> Connect4.start().result(-1), "columna -1");

        System.out.println("Regla 5: gravedad y columna llena");
        expectError(() -> Connect4.start("000000").result(0), "columna llena");

        System.out.println("Regla 6: victoria en las 4 direcciones");
        String[][] cases = {{"horizontal", "0011223"}, {"vertical", "0101010"},
                            {"diagonal /", "01122323363"}, {"diagonal \\", "65544343303"}};
        for (String[] t : cases)
            check("1".equals(Connect4.start(t[1]).winner()), t[0] + " -> gana Jugador 1");
        check("2".equals(Connect4.start("6001122").result(3).winner()), "J2 gana horizontal");

        System.out.println("Regla 7: empate con 42 fichas");
        Connect4 d = Connect4.start("556350650065250346406463302124323422411111");
        check(d.isTerminal() && d.winner() == null && d.piecesPlayed() == 42, "empate, utilidad = " + d.heuristicUtility("1"));
        System.out.println(d);

        System.out.println("Regla 8: finalización inmediata");
        Connect4 w = Connect4.start("0011223");
        check(w.legalActions().isEmpty(), "sin acciones legales tras ganar");
        expectError(() -> w.result(4), "jugar después de una victoria");
    }
}
