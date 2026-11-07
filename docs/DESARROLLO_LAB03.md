# Laboratorio 3 – Búsqueda adversaria: MiniMax con poda α-β

**Curso:** Principios y Tecnologías de IA 2026-2 – Escuela Colombiana de Ingeniería
**Autor:** Sebastián Granados

Este documento explica **cómo se desarrolló** el laboratorio `AdvSearch_PTIA_2026_2.ipynb`:
las decisiones de diseño, qué hace cada método, los algoritmos que se usaron y los
resultados. Para cada método se muestra el código Python del notebook y su
**equivalente en Java** (carpeta [`java/`](../java)), para que quede claro lo que significa.

---

## 1. Estructura del repositorio

```
Lab03-PTIA/
├── AdvSearch_PTIA_2026_2.ipynb   ← notebook resuelto (Python, ya ejecutado)
├── docs/
│   └── DESARROLLO_LAB03.md       ← este documento
└── java/                          ← misma solución en Java, como referencia
    ├── StateGame.java             (interfaz = clase abstracta del notebook)
    ├── TicTacToe.java             (el juego)
    ├── AlphaBetaEngine.java       (MiniMax + poda α-β)
    └── Main.java                  (partidas de prueba)
```

Cómo ejecutarlo:

* **Notebook:** abrirlo en Colab (botón "Open in Colab") → *Entorno de ejecución → Ejecutar todas*.
* **Java:**
  ```bash
  cd java
  javac *.java
  java Main
  ```
  Las dos versiones dan exactamente las mismas jugadas, valores y número de nodos.

---

## 2. Paso a paso de lo que se hizo

| Paso | Qué se hizo | Por qué |
|------|-------------|---------|
| 1 | Se corrigió la celda de *imports* (`from __future__ import annotations` primero) | Python exige que `from __future__` sea la primera instrucción; si no, da `SyntaxError`. |
| 2 | Se corrigió la clase abstracta `StateGame`: se agregó `self`, `@abstractmethod` y los `:` que faltaban en `heuristicUtility` y `toString` | La plantilla original no compilaba y sus métodos no podían llamarse sobre un objeto. |
| 3 | Se eligió el juego **Tres en raya** y se implementó `TicTacToe(StateGame)` | Es el juego de dos jugadores, suma cero e información perfecta más sencillo, y su árbol completo (≈550 000 nodos) cabe en memoria: así se puede verificar que la IA juega de forma perfecta. |
| 4 | Se implementó `AlphaBetaEngine` (`search`, `maxValue`, `minValue`) | Lo pide la Parte II. Se añadieron contadores de nodos y de podas para medir la eficiencia. |
| 5 | Se jugaron partidas agente vs agente desde **dos estados iniciales** | Lo pide la celda de pruebas. |
| 6 | Experimentos extra: detección de victoria/bloqueo, MiniMax sin poda vs con poda y agente débil vs fuerte | Para comprobar que la decisión es correcta y que la poda sirve. |
| 7 | Se respondió la retrospectiva y se escribió esta documentación | Entregable. |

---

## 3. Modelo del juego (Parte I)

Un juego se formaliza como un problema de búsqueda con:

| Elemento formal (Russell & Norvig) | Método | En Tres en raya |
|---|---|---|
| Estado inicial *S₀* | `start()` | Tablero vacío, empieza `X` (o uno dado) |
| `TO-MOVE(s)` | `toMove()` | Quién tiene el turno |
| `ACTIONS(s)` + `RESULT(s,a)` | `actionResults()` | Un estado nuevo por cada casilla vacía |
| `IS-TERMINAL(s)` | `isTerminal()` | Hay 3 en línea o el tablero está lleno |
| `UTILITY(s,p)` | `heuristicUtility(p)` | ±(100 + vacías), 0 o heurística |

### 3.1 Representación del estado

```
 0 | 1 | 2
---+---+---
 3 | 4 | 5
---+---+---
 6 | 7 | 8
```

* `board`: 9 casillas con `'X'`, `'O'` o `' '`. En Python es una `tuple` y en Java un
  `char[]` que se copia en el constructor (`board.clone()`).
* `player`: jugador con el turno.
* `move`: casilla jugada para llegar al estado (sirve para mostrar la jugada elegida).

**Decisión clave: el estado es inmutable.** Cada jugada crea un objeto nuevo en lugar de
modificar el actual. Así el árbol de búsqueda nunca tiene que "deshacer" jugadas y no
hay errores porque se comparten referencias. En Java se logra con campos `final` y copia defensiva.

### 3.2 Métodos, uno por uno

#### `start()` – estado inicial

```python
@classmethod
def start(cls, board=None, player='X') -> TicTacToe:
    if isinstance(board, str):
        board = [c if c in cls.PLAYERS else cls.EMPTY for c in board]
    return cls(board, player)
```
```java
public static TicTacToe start(String cells, char player) {
    char[] b = new char[9];
    for (int i = 0; i < 9; i++) {
        char c = cells.charAt(i);
        b[i] = (c == 'X' || c == 'O') ? c : EMPTY;
    }
    return new TicTacToe(b, player, -1);
}
```
**Qué hace:** es un *factory method* (en Java un método `static`). Sin parámetros
devuelve el tablero vacío. Con una cadena de 9 caracteres, como `"XO       "`, construye
una partida que ya va empezada, y así se pueden probar distintos estados iniciales.

#### `actionResults()` – función sucesora

```python
def actionResults(self) -> list:
    if self.isTerminal():
        return []
    order = (4, 0, 2, 6, 8, 1, 3, 5, 7)
    successors = []
    for i in order:
        if self.board[i] == TicTacToe.EMPTY:
            newBoard = list(self.board)
            newBoard[i] = self.player
            successors.append(TicTacToe(newBoard, self.rival(self.player), i))
    return successors
```
```java
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
```
**Qué hace:** por cada casilla vacía copia el tablero, coloca la ficha del jugador en
turno y crea el estado hijo, donde el turno pasa al rival. Si el estado es terminal
devuelve una lista vacía.

**Ordenamiento de jugadas (*move ordering*):** las casillas se recorren en el orden
*centro → esquinas → bordes*. En Tres en raya las mejores jugadas suelen ser el centro y
las esquinas. La poda α-β corta más ramas cuando la mejor jugada se explora primero,
así que este orden reduce bastante los nodos visitados.

#### `winner()` (auxiliar) e `isTerminal()`

```python
LINES = ((0,1,2),(3,4,5),(6,7,8),(0,3,6),(1,4,7),(2,5,8),(0,4,8),(2,4,6))

def winner(self):
    for a, c, d in LINES:
        if b[a] != EMPTY and b[a] == b[c] == b[d]:
            return b[a]
    return None

def isTerminal(self) -> bool:
    return self.winner() is not None or EMPTY not in self.board
```
```java
public char winner() {
    for (int[] l : LINES)
        if (board[l[0]] != EMPTY && board[l[0]] == board[l[1]] && board[l[1]] == board[l[2]])
            return board[l[0]];
    return EMPTY;            // en Java se usa EMPTY en lugar de None/null
}
public boolean isTerminal() { return winner() != EMPTY || countEmpty() == 0; }
```
**Qué hace:** `winner` revisa las 8 líneas ganadoras (3 filas, 3 columnas y 2
diagonales). El estado es terminal si alguien ganó o si ya no quedan casillas vacías (empate).

#### `toMove()` y `rival()`

```java
public char toMove()          { return player; }
public char rival(char p)     { return p == 'X' ? 'O' : 'X'; }
```
`toMove` dice quién juega en este estado y `rival` devuelve al otro jugador. El motor
usa `toMove()` en la raíz para saber quién es **MAX**.

#### `heuristicUtility(player)` – función de utilidad / evaluación

```java
public int heuristicUtility(char p) {
    char w = winner();
    int empties = countEmpty();
    if (w != EMPTY) return (w == p) ? 100 + empties : -(100 + empties);   // (a) terminal
    if (empties == 0) return 0;                                          //     empate
    char opp = rival(p);
    int score = 0;                                                       // (b) heurística
    for (int[] l : LINES) {
        int mine = 0, theirs = 0;
        for (int i : l) { if (board[i] == p) mine++; else if (board[i] == opp) theirs++; }
        if (theirs == 0)      score += LINE_VALUE[mine];    // {0, 1, 10}
        else if (mine == 0)   score -= LINE_VALUE[theirs];
    }
    return score;
}
```

**(a) Estados terminales → utilidad exacta**

| Situación | Valor |
|---|---|
| `player` gana | `+100 + casillas vacías` |
| `player` pierde | `−100 − casillas vacías` |
| Empate | `0` |

El término `+ casillas vacías` hace que el agente **prefiera ganar cuanto antes** (una
victoria en la jugada 5 vale más que una en la jugada 7) y que, si va a perder, **retrase la
derrota**. Sin ese término todas las victorias valdrían lo mismo y el agente podría
"dar vueltas" antes de ganar.

**(b) Estados no terminales → heurística de líneas abiertas.** Solo se usa cuando la
búsqueda se corta por profundidad (`depth = 0`). Una línea está *abierta* para un jugador
si el rival no tiene fichas en ella:

* 1 ficha propia en línea abierta → **+1**
* 2 fichas propias en línea abierta (amenaza de ganar) → **+10**
* lo mismo para el rival, restando.

`utilidad = Σ(valor de mis líneas abiertas) − Σ(valor de las líneas abiertas del rival)`

Ejemplo: con `X` en el centro y tablero vacío, X tiene 4 líneas abiertas con 1 ficha (fila
del medio, columna del medio y 2 diagonales), así que `utilidad(X) = 4`. Por eso, incluso
con profundidad 1, el agente prefiere el centro.

#### `toString()`
Devuelve el tablero con separadores `---+---+---`. En Java es la sobrescritura de
`Object.toString()`, que `println` llama automáticamente.

---

## 4. Búsqueda adversaria (Parte II)

### 4.1 MiniMax

Hay dos jugadores. **MAX** (quien busca la jugada) quiere maximizar la utilidad y **MIN**
(el rival) quiere minimizarla. Se supone que **ambos juegan de forma óptima**. El valor
de un nodo se define recursivamente:

```
MINIMAX(s) = UTILITY(s, MAX)                         si s es terminal o depth = 0
           = max  { MINIMAX(hijo) }                  si en s juega MAX
           = min  { MINIMAX(hijo) }                  si en s juega MIN
```

Es un **recorrido en profundidad (DFS)** del árbol de juego. El valor de las hojas
"sube" hacia la raíz: en los niveles de MAX se toma el máximo y en los de MIN el mínimo.

* Tiempo: **O(b^m)** (b = factor de ramificación, m = profundidad).
* Espacio: **O(b·m)**, porque solo guarda el camino actual (es DFS).

### 4.2 Poda α-β

MiniMax revisa nodos que no pueden cambiar la decisión. La poda α-β lleva dos cotas:

* **α (alfa):** el mejor valor que **MAX** ya tiene asegurado en el camino actual (empieza en −∞).
* **β (beta):** el mejor valor que **MIN** ya tiene asegurado en el camino actual (empieza en +∞).

Si en algún nodo **α ≥ β**, los hermanos que faltan no pueden cambiar la decisión de la
raíz y **no se exploran**:

* **Poda β** (en un nodo MAX): `v ≥ β`. MIN, más arriba, ya tiene una opción que vale β o
  menos, así que nunca va a dejar que se llegue a este nodo.
* **Poda α** (en un nodo MIN): `v ≤ α`. MAX, más arriba, ya tiene una opción que vale α o
  más, así que nunca va a elegir este camino.

La poda **no cambia el resultado**: devuelve la misma decisión que MiniMax. Con un orden de
jugadas perfecto el tiempo baja a **O(b^(m/2))**, lo que permite buscar casi el doble de
profundo en el mismo tiempo (Knuth & Moore, 1975).

**Ejemplo de poda:**

```
            MAX
          /     \
       MIN       MIN
      / | \      / \
     3 12  8    2   ?   ?
```
El primer MIN vale 3, así que en la raíz α = 3. En el segundo MIN la primera hoja vale 2,
entonces ese nodo valdrá ≤ 2 < α = 3. MAX nunca lo va a elegir y las hojas `?` se podan.

### 4.3 Implementación

```java
public StateGame search(StateGame s, int depth) {
    player = s.toMove();                       // quien llama es MAX
    int alpha = Integer.MIN_VALUE, beta = Integer.MAX_VALUE;
    StateGame best = null; int bestValue = Integer.MIN_VALUE;
    for (StateGame child : s.actionResults()) {
        int v = minValue(child, depth - 1, alpha, beta);   // tras mi jugada, juega MIN
        if (best == null || v > bestValue) { bestValue = v; best = child; }
        alpha = Math.max(alpha, bestValue);
    }
    return best;                               // el estado sucesor elegido
}

public int maxValue(StateGame s, int depth, int alpha, int beta) {
    if (s.isTerminal() || depth <= 0) return s.heuristicUtility(player);
    int v = Integer.MIN_VALUE;
    for (StateGame child : s.actionResults()) {
        v = Math.max(v, minValue(child, depth - 1, alpha, beta));
        if (v >= beta) return v;               // poda β
        alpha = Math.max(alpha, v);
    }
    return v;
}

public int minValue(StateGame s, int depth, int alpha, int beta) {
    if (s.isTerminal() || depth <= 0) return s.heuristicUtility(player);
    int v = Integer.MAX_VALUE;
    for (StateGame child : s.actionResults()) {
        v = Math.min(v, maxValue(child, depth - 1, alpha, beta));
        if (v <= alpha) return v;              // poda α
        beta = Math.min(beta, v);
    }
    return v;
}
```

Qué hace cada método:

* **`search(s, depth)`**: es la raíz. Fija a MAX como el jugador en turno, evalúa cada
  sucesor con `minValue` (después de jugar MAX, le toca a MIN) y devuelve el **estado**
  sucesor con mayor valor. La plantilla pide devolver un `StateGame`, no un índice de jugada.
* **`maxValue`**: valor de un nodo donde juega MAX. Toma el máximo de los hijos y hace la poda β.
* **`minValue`**: valor de un nodo donde juega MIN. Toma el mínimo de los hijos y hace la poda α.
* **Límite de profundidad (`depth`)**: cada llamada recursiva resta 1. Al llegar a 0 se
  usa la heurística. Con `depth = 9` se recorre el árbol completo y el resultado es exacto.
* **El jugador MAX como atributo**: las firmas de la plantilla (`maxValue(s, depth, alpha,
  beta)`) no reciben el jugador. Por eso `search` lo guarda en `self.player` / `this.player`
  y las hojas siempre se evalúan desde el punto de vista de MAX. Este es el esquema
  MiniMax clásico (no NegaMax).
* En Python ±∞ se representa con `math.inf`. En Java se usa
  `Integer.MIN_VALUE` / `Integer.MAX_VALUE`.

---

## 5. Pruebas y resultados

### 5.1 Estado inicial 1 – tablero vacío, inicia X, ambos con profundidad 9

```
Jugada 1: X -> casilla 4 | valor=0 | nodos=7865 | podas=2854
Jugada 2: O -> casilla 0 | valor=0 | nodos=2174 | podas=780
...
 O | O | X
---+---+---
 X | X | O
---+---+---
 O | X | X
RESULTADO: empate
```
El valor 0 desde la primera jugada coincide con el resultado teórico conocido: con juego
perfecto, Tres en raya termina en **empate**.

### 5.2 Estado inicial 2 – `X` en 0, `O` en 1 (O jugó un borde, que es un error), turno de X

```
Jugada 1: X -> casilla 4 | valor=102 | ...
Jugada 2: O -> casilla 8 | valor=-102 | ...   (bloquea la diagonal)
Jugada 3: X -> casilla 6 | valor=102 | ...   (doble amenaza: columna 0-3-6 y diagonal 2-4-6)
Jugada 4: O -> casilla 2 | valor=-102 | ...
Jugada 5: X -> casilla 3 | valor=102 | ...
RESULTADO: gana X
```
El agente detecta desde la primera jugada que tiene **victoria forzada**. El valor 102 =
100 + 2 casillas vacías al final significa que gana en su tercera jugada. Para lograrlo
arma una **doble amenaza (*fork*)** que O no puede bloquear por completo.

### 5.3 Victoria inmediata y bloqueo

```
X gana en 1 jugada -> 2        (tablero "XX OO    ")
O bloquea la amenaza -> 2      (tablero "XX O     ")
```

### 5.4 Eficiencia: MiniMax vs α-β (desde el tablero vacío)

| Profundidad | Nodos MiniMax | Nodos α-β | Ahorro |
|---:|---:|---:|---:|
| 1 | 10 | 10 | 0,0 % |
| 2 | 82 | 26 | 68,3 % |
| 3 | 586 | 97 | 83,4 % |
| 4 | 3 610 | 303 | 91,6 % |
| 5 | 18 730 | 1 115 | 94,0 % |
| 6 | 73 450 | 1 802 | 97,5 % |
| 9 (completo) | 549 946 | 7 865 | **98,6 %** |

Con la poda y el orden de jugadas se visita **unas 70 veces menos nodos** y la decisión es la misma.

### 5.5 Agente débil (profundidad 1) vs fuerte (profundidad 9)

Las dos partidas terminan en empate. La heurística de líneas abiertas es tan buena en
este juego que, mirando una sola jugada, el agente débil ya toma el centro o las esquinas
y bloquea las amenazas directas.

---

## 6. Resumen de algoritmos y estructuras usadas

| Concepto | Dónde | Detalle |
|---|---|---|
| Recorrido en profundidad (DFS) recursivo | `maxValue` / `minValue` | Recursión mutua entre ambos métodos |
| MiniMax | `AlphaBetaEngine` | MAX = jugador en la raíz |
| Poda α-β | `maxValue` (β) / `minValue` (α) | Mismo resultado que MiniMax, menos nodos |
| Búsqueda con profundidad limitada | parámetro `depth` | Evaluación heurística en el corte |
| Ordenamiento de jugadas | `actionResults` | Centro → esquinas → bordes |
| Función de evaluación lineal | `heuristicUtility` | Líneas abiertas con pesos 1 y 10 |
| Estado inmutable | `TicTacToe` | `tuple` en Python / `final` + `clone()` en Java |
| Patrón *Template / Strategy* | `StateGame` | El motor funciona con **cualquier** juego que implemente la interfaz |

Como `AlphaBetaEngine` solo usa los métodos de `StateGame`, para resolver otro juego (por
ejemplo Nim o Conecta 4) basta con escribir otra clase que implemente esa interfaz. El
motor no se toca.

---

## 7. Referencias

* Russell, S., & Norvig, P. (2021). *Artificial Intelligence: A Modern Approach* (4th ed.), Cap. 5. Pearson.
* Knuth, D. E., & Moore, R. W. (1975). An analysis of alpha-beta pruning. *Artificial Intelligence, 6*(4), 293–326.
* von Neumann, J. (1928). Zur Theorie der Gesellschaftsspiele. *Mathematische Annalen, 100*, 295–320.
