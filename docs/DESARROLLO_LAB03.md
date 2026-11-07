# Laboratorio 3 – Búsqueda adversaria: Conecta 4

**Curso:** Principios y Tecnologías de IA 2026-2 – Escuela Colombiana de Ingeniería
**Autor:** Sebastián Granados

> **Estado del documento: parcial.**
> ✅ Parte I (lógica del juego Conecta 4): implementada y explicada.
> ⏳ Parte II (MiniMax con poda α-β), pruebas de partidas y retrospectiva: pendientes.

Este documento explica cómo se desarrolló el notebook `AdvSearch_PTIA_2026_2.ipynb`. Para
cada método se muestra el código Python y su **equivalente en Java** (carpeta
[`java/`](../java)), con la explicación de lo que hace y del algoritmo que usa.

---

## 1. Estructura del repositorio

```
Lab03-PTIA/
├── AdvSearch_PTIA_2026_2.ipynb   ← notebook (Parte I resuelta)
├── docs/
│   └── DESARROLLO_LAB03.md       ← este documento
└── java/                          ← Parte I en Java, como referencia
    ├── StateGame.java             (interfaz = clase abstracta del notebook)
    ├── Connect4.java              (el juego)
    ├── AlphaBetaEngine.java       (Parte II: solo firmas, pendiente)
    └── Main.java                  (verificación de las 8 reglas)
```

Cómo ejecutarlo:

* **Notebook:** abrirlo en Colab → *Entorno de ejecución → Ejecutar todas*.
* **Java:** `cd java && javac *.java && java Main`

---

## 2. Enunciado: reglas oficiales

Hay que diseñar desde cero la lógica de **Conecta 4** y separar tres cosas: el **estado**
del juego, las **reglas de transición** y la **interfaz** que necesitan los algoritmos de
búsqueda adversaria (MiniMax / Alfa-Beta).

| # | Regla | Dónde se cumple | Estado |
|---|---|---|---|
| 1 | Tablero de 6 filas × 7 columnas | `ROWS, COLS = 6, 7` | ✅ |
| 2 | Jugador 1 y Jugador 2; empieza el Jugador 1 | `start()` | ✅ |
| 3 | Turnos estrictamente alternos | `result()` → `rival(player)` | ✅ |
| 4 | Acción = columna 0..6 | validación en `result()` | ✅ |
| 5 | Gravedad; si la columna está llena, la acción es inválida | `heights[c]` en `result()` | ✅ |
| 6 | 4 fichas consecutivas en horizontal, vertical o diagonal | `_checkWinner()` | ✅ |
| 7 | Empate cuando se colocan las 42 fichas | `isTerminal()` | ✅ |
| 8 | Finalización inmediata | `legalActions()` / `result()` | ✅ |

---

## 3. Paso a paso de lo que se hizo

| Paso | Qué se hizo | Por qué |
|---|---|---|
| 1 | Se corrigió la celda de *imports* (`from __future__ import annotations` primero) | Si no va de primera, Python lanza `SyntaxError`. |
| 2 | Se corrigió `StateGame`: se agregaron `self`, `@abstractmethod` y los `:` que faltaban | La plantilla original no compilaba. |
| 3 | Se diseñó la representación del estado de Conecta 4 | Para que cada jugada y la detección de victoria sean rápidas. |
| 4 | Se implementó `Connect4(StateGame)` con todos los métodos | Lo pide la Parte I. |
| 5 | Se escribió una celda de pruebas para las 8 reglas | Para demostrar que la implementación es correcta. |
| 6 | Parte II | ⏳ *Pendiente* |

---

## 4. Diseño del estado

```
fila 5 | . . . . . . . |      índice = fila * 7 + columna
fila 4 | . . . . . . . |      (la fila 0 es la de ABAJO)
fila 3 | . . . . . . . |
fila 2 | . . . . . . . |
fila 1 | . . . . . . . |
fila 0 | . . . . . . . |
       +---------------+
         0 1 2 3 4 5 6
```

| Atributo | Python | Java | Para qué sirve |
|---|---|---|---|
| `board` | `tuple` de 42 | `String[42]` | Contenido de cada casilla: `'1'`, `'2'` o `'.'` |
| `heights` | `tuple` de 7 | `int[7]` | Fichas en cada columna, que es también la fila donde cae la siguiente → **gravedad en O(1)** |
| `player` | `str` | `String` | Jugador con el turno |
| `move` | `int` o `None` | `int` (−1 = inicial) | Última columna jugada |
| `winnerId` | `str` o `None` | `String` o `null` | Ganador; se calcula una sola vez, al crear el estado |

**Decisiones de diseño**

* **Fila 0 abajo:** la gravedad se reduce a "la ficha cae en la fila `heights[c]`". No hay
  que recorrer la columna buscando un hueco.
* **Estado inmutable:** cada jugada crea un objeto nuevo. En Java se logra con campos
  `final` y copias con `clone()`. Así un buscador puede explorar el árbol sin "deshacer"
  jugadas, y dos ramas nunca comparten un tablero que se modifique.
* **El ganador se calcula al construir el estado:** `isTerminal()` y `winner()` solo
  consultan un atributo, en O(1). Un buscador los llama millones de veces, así que esto importa.

---

## 5. Métodos implementados (Parte I)

### 5.1 `start(moves)` – estado inicial

```python
@classmethod
def start(cls, moves=None) -> Connect4:
    s = cls()
    for m in (moves or []):
        s = s.result(int(m))
    return s
```
```java
public static Connect4 start(String moves) {
    String[] b = new String[ROWS * COLS];
    Arrays.fill(b, EMPTY);
    Connect4 s = new Connect4(b, new int[COLS], P1, -1);
    for (char ch : moves.toCharArray()) s = s.result(ch - '0');
    return s;
}
```
**Qué hace:** es un *factory method* (`static` en Java). Crea el tablero vacío con el
turno del Jugador 1 (regla 2). Si recibe una secuencia de columnas como `"3324"`, la juega
con `result()`. Así **cualquier estado inicial se construye con jugadas legales** y no se
puede crear un tablero imposible, por ejemplo con fichas flotando o con turnos repetidos.

### 5.2 `result(column)` – regla de transición

```python
def result(self, column: int) -> Connect4:
    if self.isTerminal():
        raise ValueError("El juego ya terminó: no se permiten jugadas adicionales")
    if not isinstance(column, int) or not 0 <= column < Connect4.COLS:
        raise ValueError(f"Columna inválida: {column} (debe estar entre 0 y 6)")
    r = self.heights[column]
    if r >= Connect4.ROWS:
        raise ValueError(f"La columna {column} está llena")
    board = list(self.board)
    board[r * Connect4.COLS + column] = self.player
    heights = list(self.heights)
    heights[column] += 1
    return Connect4(board, heights, self.rival(self.player), column)
```
```java
public Connect4 result(int column) {
    if (isTerminal()) throw new IllegalStateException("El juego ya terminó...");
    if (column < 0 || column >= COLS) throw new IllegalArgumentException("Columna inválida...");
    int r = heights[column];
    if (r >= ROWS) throw new IllegalArgumentException("La columna ... está llena");
    String[] nb = board.clone();
    nb[r * COLS + column] = player;
    int[] nh = heights.clone();
    nh[column]++;
    return new Connect4(nb, nh, rival(player), column);
}
```
**Qué hace:** es el corazón de las reglas. En orden:
1. **Regla 8:** si el juego ya terminó, rechaza la jugada.
2. **Regla 4:** valida que la columna esté entre 0 y 6.
3. **Regla 5:** si la columna está llena, la jugada es inválida. Si no, la ficha cae en la fila `heights[column]`.
4. Copia el tablero y las alturas (inmutabilidad) y coloca la ficha.
5. **Regla 3:** el nuevo estado tiene el turno del rival.

Complejidad: O(42) por la copia del tablero. La gravedad en sí es O(1).

### 5.3 `legalActions()` y `actionResults()` – sucesores

```python
ORDER = (3, 2, 4, 1, 5, 0, 6)

def legalActions(self) -> list:
    if self.isTerminal():
        return []
    return [c for c in Connect4.ORDER if self.heights[c] < Connect4.ROWS]

def actionResults(self) -> list:
    return [self.result(c) for c in self.legalActions()]
```
```java
public List<Integer> legalActions() {
    List<Integer> acts = new ArrayList<>();
    if (isTerminal()) return acts;
    for (int c : ORDER) if (heights[c] < ROWS) acts.add(c);
    return acts;
}
public List<StateGame> actionResults() {
    List<StateGame> out = new ArrayList<>();
    for (int c : legalActions()) out.add(result(c));
    return out;
}
```
**Qué hace:** `legalActions` devuelve las columnas que no están llenas. Si el juego
terminó, devuelve una lista vacía (regla 8). `actionResults` convierte cada acción en su
estado resultante: es la **función sucesora** que usa MiniMax para expandir el árbol.

**Orden de las columnas (*move ordering*):** se generan del centro hacia los bordes
(3, 2, 4, 1, 5, 0, 6). La columna central participa en más líneas de 4 y suele ser la
mejor jugada. Explorar primero las mejores jugadas hace que la poda α-β corte más ramas.

### 5.4 `_checkWinner()` – detección de victoria (regla 6)

```python
def _checkWinner(self):
    if self.move is None:
        return None
    c = self.move
    r = self.heights[c] - 1
    p = self._cell(r, c)
    for dr, dc in ((0, 1), (1, 0), (1, 1), (1, -1)):
        count = 1
        for sign in (1, -1):
            rr, cc = r + sign * dr, c + sign * dc
            while 0 <= rr < ROWS and 0 <= cc < COLS and self._cell(rr, cc) == p:
                count += 1
                rr, cc = rr + sign * dr, cc + sign * dc
        if count >= 4:
            return p
    return None
```
```java
private String checkWinner() {
    if (move < 0) return null;
    int c = move, r = heights[c] - 1;
    String p = cell(r, c);
    for (int[] d : DIRS) {                       // {0,1} {1,0} {1,1} {1,-1}
        int count = 1;
        for (int sign = 1; sign >= -1; sign -= 2) {
            int rr = r + sign * d[0], cc = c + sign * d[1];
            while (rr >= 0 && rr < ROWS && cc >= 0 && cc < COLS && cell(rr, cc).equals(p)) {
                count++; rr += sign * d[0]; cc += sign * d[1];
            }
        }
        if (count >= CONNECT) return p;
    }
    return null;
}
```
**Algoritmo:** una línea de 4 nueva **solo puede pasar por la última ficha colocada**. Por
eso no hace falta revisar todo el tablero. Basta con mirar las 4 direcciones (horizontal,
vertical, diagonal `/` y diagonal `\`) desde esa ficha y contar cuántas fichas iguales hay
seguidas hacia cada lado. Si la suma (incluyendo la ficha) es ≥ 4, hay ganador.

* Costo: a lo sumo 4 direcciones × 6 vecinos = **O(1)**.
* Revisar todo el tablero costaría 69 ventanas × 4 casillas en cada estado.

### 5.5 `isTerminal()` – prueba de terminal (reglas 7 y 8)

```python
def isTerminal(self) -> bool:
    return self.winnerId is not None or self.piecesPlayed() == 42
```
```java
public boolean isTerminal() { return winnerId != null || piecesPlayed() == ROWS * COLS; }
```
El juego termina si hay ganador o si se colocaron las 42 fichas (empate).

### 5.6 `toMove()` y `rival()`

```java
public String toMove()          { return player; }
public String rival(String p)   { return P1.equals(p) ? P2 : P1; }
```
`toMove` dice quién juega y `rival` devuelve al otro jugador. En Java los `String` se
comparan con `equals`, no con `==`.

### 5.7 `heuristicUtility(player)` – función de utilidad

**(a) Estados terminales → valor exacto**

| Situación | Valor |
|---|---|
| `player` gana | `+100000 + casillas vacías` |
| `player` pierde | `−100000 − casillas vacías` |
| Empate | `0` |

El término `+ casillas vacías` hace que se prefiera **ganar antes** y **perder después**.

**(b) Estados no terminales → heurística de ventanas.** Se usa cuando el buscador corta
por profundidad. Conecta 4 tiene unos 4,5·10¹² estados, así que no se puede buscar hasta
el final.

* Hay **69 ventanas** de 4 casillas consecutivas: 24 horizontales, 21 verticales, 12 `/` y 12 `\`.
  Se calculan una sola vez con `_buildWindows`.
* Por cada ventana se cuentan las fichas propias y las del rival:

| Contenido de la ventana | Puntos |
|---|---|
| solo fichas propias: 1 / 2 / 3 | +1 / +5 / +50 |
| solo fichas del rival: 1 / 2 / 3 | −1 / −5 / −50 |
| fichas de ambos | 0 (ya nadie puede ganar con ella) |

* **Bono por el centro:** +3 por cada ficha propia en la columna 3 y −3 por cada una del
  rival, porque esa columna participa en más ventanas que cualquier otra.

```java
for (int[] w : WINDOWS) {
    int mine = 0, theirs = 0;
    for (int i : w) { if (board[i].equals(p)) mine++; else if (board[i].equals(opp)) theirs++; }
    if (theirs == 0) score += WINDOW_WEIGHT[mine];        // {0, 1, 5, 50}
    else if (mine == 0) score -= WINDOW_WEIGHT[theirs];
}
```

### 5.8 `toString()`

Dibuja el tablero de arriba hacia abajo, con `X` = Jugador 1, `O` = Jugador 2 y los índices
de columna en la última línea. En Java es la sobrescritura de `Object.toString()`.

---

## 6. Verificación de las reglas

La celda de pruebas del notebook (y `java/Main.java`) da este resultado:

```
Reglas 1-2: [OK] 42 casillas, turno inicial: 1
Regla 3:    [OK] tras J1 juega J2 y tras J2 juega J1
Regla 4:    [OK] columna 7 / columna -1 -> Columna inválida
Regla 5:    [OK] las fichas se apilan desde abajo; columna llena -> error
Regla 6:    [OK] horizontal, vertical, diagonal /, diagonal \, y gana J2
Regla 7:    [OK] 42 fichas, sin ganador, utilidad = 0
Regla 8:    [OK] jugar después de una victoria -> error
```

Las secuencias de prueba (`"0011223"` horizontal, `"0101010"` vertical, `"01122323363"`
diagonal `/`, `"65544343303"` diagonal `\`) se construyen con jugadas legales. La partida
de empate (`"556350650065250346406463302124323422411111"`) llena las 42 casillas sin que
nadie haga 4 en línea.

---

## 7. Parte II – MiniMax con poda α-β ⏳ *(pendiente)*

> Esta sección se completará cuando se implemente `AlphaBetaEngine`.

Ideas que guiarán la implementación:

* **MiniMax:** recorrido en profundidad del árbol de juego. En los nodos de MAX se toma
  el máximo de los hijos y en los de MIN el mínimo, suponiendo que ambos juegan de forma óptima.
* **Poda α-β:** se llevan dos cotas, α (lo mejor que MAX ya tiene asegurado) y β (lo mejor
  que MIN ya tiene asegurado). Cuando α ≥ β, se dejan de explorar los hermanos restantes.
* **Profundidad limitada:** al llegar a la profundidad máxima se usa `heuristicUtility`.
* Métodos a implementar: `search(s, depth)`, `maxValue(s, depth, α, β)` y `minValue(s, depth, α, β)`.

*Pendiente:* código, explicación detallada, partidas desde dos estados iniciales y
comparación de nodos visitados (MiniMax sin poda frente a α-β).

---

## 8. Retrospectiva ⏳ *(pendiente)*

---

## 9. Referencias

* Russell, S., & Norvig, P. (2021). *Artificial Intelligence: A Modern Approach* (4th ed.), Cap. 5. Pearson.
* Allis, L. V. (1988). *A Knowledge-based Approach of Connect-Four. The Game is Solved: White Wins* (Tesis de maestría). Vrije Universiteit Amsterdam.
