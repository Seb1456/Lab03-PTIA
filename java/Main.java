/** Ejemplo de uso de Connect4 (Parte I). */
public class Main {
    public static void main(String[] args) {
        Connect4 s = Connect4.start().play(3);   // el Jugador 1 juega en la columna 3
        System.out.println(s);
        System.out.println("Turno de: " + s.toMove() + " | Utilidad para X: " + s.heuristicUtility('X'));
    }
}
