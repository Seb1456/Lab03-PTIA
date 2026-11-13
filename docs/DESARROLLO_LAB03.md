# Laboratorio 3 – Conecta 4 (Búsqueda adversaria)

**Estado:** Parte I, Parte II (MiniMax α-β) y retrospectiva hechas ✅

## Parte I – Clase `Connect4`

`StateGame` (celda *GAME THEORY*) es la clase abstracta del profesor: solo define **qué**
métodos debe tener un juego, como una `interface` en Java. `Connect4` hereda de ella y los
implementa. A la plantilla solo se le corrigieron dos `:` que faltaban y el orden de los imports.

**Estado:** `board` (6 filas × 7 columnas, la fila 0 es la de arriba) y `player` (`'X'` = Jugador 1, `'O'` = Jugador 2).

| Método | Qué hace | En Java |
|---|---|---|
| `start()` | Tablero vacío, empieza `X` | `static Connect4 start()` |
| `play(col)` | Copia el tablero y deja caer la ficha en la casilla vacía más baja de `col` | `Connect4 play(int col)` |
| `lines()` | Devuelve todas las líneas de 4 casillas seguidas (69 en total) | `List<char[]> lines()` |
| `winner()` | Busca una línea con 4 fichas iguales | `char winner()` |
| `actionResults()` | Un estado nuevo por cada columna no llena | `List<StateGame> actionResults()` |
| `isTerminal()` | Hay ganador o el tablero está lleno | `boolean isTerminal()` |
| `toMove()` / `rival(p)` | Quién juega / el otro jugador | `char toMove()` / `char rival(char p)` |
| `heuristicUtility(p)` | Valor del estado para `p` (ver abajo) | `int heuristicUtility(char p)` |
| `toString()` | Tablero como texto | `String toString()` |

**Algoritmos**

- **Gravedad (`play`):** se recorre la columna de abajo (fila 5) hacia arriba hasta encontrar `'.'`.
  Se trabaja sobre una copia del tablero, así el estado anterior no cambia y la búsqueda puede explorar varias jugadas.
- **Líneas (`lines`):** desde cada casilla se avanza 4 pasos en las 4 direcciones (→, ↓, ↘, ↙), siempre que no se salga del tablero.
- **Heurística:** `+1000` si `p` gana y `−1000` si pierde. Si nadie ha ganado, se recorren
  las líneas: si en una línea solo hay fichas de `p`, se suma cuántas son; si solo hay fichas del rival, se restan.

Ejemplo: con una sola `X` en la columna 3, la utilidad para `X` es **7**, porque esa ficha está en 7 líneas libres.

## Parte II – MiniMax con poda α-β

`search` prueba cada jugada y se queda con la de mayor valor; `maxValue` y `minValue` se llaman entre sí hasta la profundidad dada o un estado terminal, donde se usa `heuristicUtility`. Idea: MAX elige el máximo y MIN
el mínimo. Con α (lo mejor que MAX ya tiene asegurado) y β (lo mejor que MIN ya tiene asegurado) se dejan de explorar las ramas cuando α ≥ β.

Prueba: `jugar(s)` pone a los dos jugadores a decidir con MiniMax (profundidad 4) desde dos estados iniciales: el tablero vacío (termina en empate) y una partida ya empezada (gana `O`).

## Archivos

- `AdvSearch_PTIA_2026_2.ipynb`: notebook.
- `java/`: la Parte I en Java (`javac *.java && java Main`).

## Referencias

- Russell, S., & Norvig, P. (2021). *Artificial Intelligence: A Modern Approach* (4th ed.), Cap. 5.
