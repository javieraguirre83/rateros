# Rateros para Minecraft 26.2 (Forge)

Bandas de **rateros** que buscan y roban tus **cosas ya trabajadas**: lingotes, tablones, comida cocinada, herramientas, armaduras, diamantes… No saben dónde vives, pero **olfatean el botín**, así que llegan solos a donde guardas tus cosas.

## ⬇️ Descargar

**[Descargar el mod (.jar)](https://github.com/javieraguirre83/rateros/releases/download/ultima/Rateros-1.1.2-forge-26.2.jar)**

1. Descarga el archivo `.jar`.
2. Abre la carpeta `mods`:
   - **Windows:** presiona **Windows + R**, escribe `%appdata%\.minecraft\mods` y dale Enter.
   - **Mac:** en Finder presiona **Cmd + Shift + G**, escribe `~/Library/Application Support/minecraft/mods` y dale Enter.
3. Pega ahí el `.jar` y abre Minecraft con el perfil de **Forge 26.2**.

Los textos salen en **español o inglés** según el idioma de tu Minecraft. El comando funciona como `/rateros` o `/bandits`.

---

## 🗡 Cómo funcionan

**Cuándo llegan**
- A **cualquier hora**, pero el **doble de probable de noche** (configurable: cualquier hora, solo de noche o solo de día).
- Al inicio del mundo, más o menos **1 de cada 5 días**; con los días va subiendo hasta **1 de cada 2** (día 60 en adelante).
- Al llegar suena un **cuerno** y sale el aviso **"¡Se acercan rateros!"**.
- Llega una banda a la vez, a unos 24-40 bloques de ti. No llegan en dificultad Pacífica ni fuera del mundo normal.

**La banda: según lo mejor que hayas conseguido**

El tamaño de la banda y sus armas dependen del **material más valioso que hayas conseguido**. Cuenta lo que tengas en tu inventario, tu cofre de ender o los cofres cerca de ti, pero **solo lingotes, bloques y gemas**: armaduras, herramientas, pepitas y mineral crudo no cuentan. Cada nivel se **desbloquea para siempre** la primera vez, aunque luego te lo gastes.

| Lo mejor que has conseguido | Rateros por banda | Quiénes | Armas |
|---|---|---|---|
| Nada valioso todavía (el cobre no cuenta) | 2 | Ladrones | Madera, a veces piedra |
| Hierro o lapislázuli | 2 a 3 | + 1 ballestero | Piedra, a veces hierro |
| Oro, esmeralda o redstone | 3 a 4 | + forzador | Hierro |
| Diamante | 4 a 6 | + puede venir el jefe | Hierro, a veces diamante |
| Netherita | 5 a 8 | + jefe siempre | Diamante, a veces netherita |

- **De vez en cuando llega una banda más chica:** el **30%** de las veces, en lugar de la de tu nivel llega una de cualquier nivel más bajo (al azar). Así, aunque ya tengas diamante, a veces te caen 2 ladrones. Se cambia con `probMenor`.
- **Nunca traen armas encantadas.**
- Con `/rateros` ves en qué nivel vas.
- La tabla se puede cambiar en el archivo de ajustes (ver abajo).

| Rol | Arma | Carga (Normal) | Probabilidad de llevarse algo valioso |
|---|---|---|---|
| 🟢 **Ladrón** | Espada | 3 montones | 10% |
| 🔵 **Ballestero** | Ballesta | 1 montón | 10% |
| 🔴 **Forzador** | Hacha (rompe el doble de rápido) | 2 montones | 25% |
| 🟣 **Jefe** (desde el nivel diamante) | Espada, peto, doble de vida | 4 montones | 50% |

- Cada rol tiene su ropa: pañuelo en la cara, chaleco y un **costal en la espalda**. El jefe trae abrigo largo morado con dorado.
- Los **ladrones** no te atacan solos, pero si le pegas a cualquier ratero, **toda la banda** va contra ti. Los demás pelean en cuanto te ven.
- Corren como un saqueador normal: **caminando no te les escapas, corriendo sí**.

**El robo**
- **Olfatean** tus cosas hasta a **96 bloques** de donde están (configurable). Si no huelen nada, **caminan hacia tu cama** (tu punto de reaparición), que es donde suele estar tu casa, y ahí vuelven a olfatear. Esto se puede apagar.
- Buscan **baúles, baúles dobles, barriles, hornos y cajas de shulker** con cosas trabajadas. Los **abren** (con su animación y su sonido), sacan cosas y los cierran.
- **Todos roban de todo.** El rol solo cambia cuánto cargan y qué tan probable es que vayan por lo **valioso** (diamantes, esmeraldas, netherita, objetos encantados…). Un ladrón también puede llevarse un diamante, solo es menos probable.
- También recogen cosas trabajadas **tiradas en el suelo**.
- **Nunca te quitan lo que traes encima.**
- **No pueden abrir el cofre de ender:** es tu lugar seguro.
- **Cofre trampa:** al abrirlo un ratero, **se activa la redstone**, igual que contigo. ¡Pon trampas!
- No tocan cofres de estructuras que todavía no has abierto.
- Cuando se llenan (o si los lastimas mucho), **huyen**, un poco más lento que tú corriendo. **Si los matas, sueltan todo lo que robaron.** Si te sacan **96 bloques de ventaja**, **escapan** y lo robado se pierde. Mientras los persigas de cerca, no se te escapan.
- **Si te matan:** siguen robando, **recogen lo que soltaste** y luego huyen. Mientras estás en la pantalla de "Has muerto", la ventaja se cuenta desde donde moriste; si tardas en reaparecer, se te van. Cuidado: si reapareces cerca, te pueden volver a atacar.
- El jefe, al morir, suelta además unas esmeraldas.
- Si en 5 minutos no encuentran nada que robar, se van.

**Cómo entran**
- Si algo les bloquea el paso, rompen **puertas de madera y bloques débiles** (madera, vidrio, lana, tierra, hojas…), **poco a poco y con ruido**.
- La **piedra, el ladrillo, la obsidiana y las puertas de hierro los detienen.** Una casa bien construida los frena.
- No rompen por gusto, solo para pasar. Respetan la regla del juego `mobGriefing`: si está apagada, no rompen nada.

### 💡 Cómo defenderte
- Guarda lo más valioso en un **cofre de ender**.
- Construye con **piedra o ladrillo** y usa **puertas de hierro**.
- Ponles **trampas con cofres trampa** (dispensadores con flechas, TNT…).
- **Persíguelos:** si los alcanzas antes de que escapen, recuperas todo.

---

## ⌨️ Pantalla de ajustes (tecla Z)

Presiona **Z** en el juego. Se cambia en **Opciones → Controles → Asignación de teclas → Rateros**. Tiene 4 pestañas y **cada cambio se guarda solo**:

- **General:** activar o desactivar, horario, cuánto más probable de noche, % de banda por día (inicio y avanzado), día de mundo avanzado, aviso, si roban en aldeas. Abajo: **Restaurar valores originales** (deja el interruptor de asaltos como esté).
- **La banda:** máximo de rateros, qué roles pueden venir, **en qué nivel vas** y el % de bandas de nivel menor.
- **El robo:** nivel de hurto, cuánto carga cada rol, % de ir por lo valioso de cada rol, ventaja en bloques para escapar y radio en el que olfatean.
- **Cómo entran:** si rompen bloques, cuánto tardan y si buscan tu cama cuando no huelen nada. Abajo: **Abrir carpeta de ajustes** y **Recargar archivo**.

**Nivel de hurto**

| Nivel | Cuánto cargan | Probabilidad de lo valioso |
|---|---|---|
| Suave | La mitad | La mitad |
| **Normal** (de fábrica) | Lo de la tabla | Lo de la tabla |
| Agresivo | El doble | ×1.5 |
| Despiadado | Sin límite (vacían cofres) | ×2 |

## 📄 El archivo de ajustes

Todo está también en **`config/rateros/ajustes.json`**:
- **Windows:** **Windows + R**, escribe `%appdata%\.minecraft\config\rateros` y dale Enter.
- **Mac:** en Finder **Cmd + Shift + G**, escribe `~/Library/Application Support/minecraft/config/rateros` y dale Enter.
- O desde el juego: **Z → Cómo entran → Abrir carpeta de ajustes**.

Ábrelo con el Bloc de notas (Windows) o TextEdit (Mac; antes elige *Formato → Convertir a texto normal*). Después de guardarlo, dale **Recargar archivo** o usa `/rateros recargar`.

Las **listas** solo se editan en el archivo:

| Lista | Qué es | Ejemplo |
|---|---|---|
| `valiosos` | Lo que cuenta como valioso | `"minecraft:diamond"`, `"encantado"` (cualquier objeto encantado) |
| `nuncaRobar` | Lo que **nunca** roban (protegido) | `"minecraft:diamond"`, `"#minecraft:beds"` |
| `ignorar` | Basura que no les interesa | `"minecraft:dirt"`, `"minecraft:cobblestone"` |
| `bloquesRompibles` | Bloques que pueden romper para entrar | `"#minecraft:planks"`, `"minecraft:glass_pane"` |

Se usan ids (`minecraft:diamond`) o etiquetas con `#` (`#minecraft:planks` = cualquier tablón).

**La tabla de niveles** (`niveles`) también está en el archivo, del nivel más bajo al más alto. Cada nivel tiene:

| Campo | Qué es | Ejemplo |
|---|---|---|
| `materiales` | Lo que desbloquea ese nivel | `["minecraft:diamond", "minecraft:diamond_block"]` |
| `tamano` | Cuántos rateros llegan | `"4-6"` |
| `armaBase` / `armaMejor` | Material de las armas: `madera`, `piedra`, `hierro`, `diamante` o `netherita` | `"hierro"` / `"diamante"` |
| `probMejor` | % de que traigan el arma mejor (el jefe, el doble) | `20` |
| `ballesteros` | Cuántos ballesteros vienen | `1` |
| `forzador` | Si viene un forzador | `true` |
| `jefe` | % de que venga el jefe | `50` |

## 💻 Comandos

| Comando | Qué hace |
|---|---|
| `/rateros` | Ver si están activados, la probabilidad de hoy y si hay rateros cerca |
| `/rateros activar` / `desactivar` | Prender o apagar los asaltos |
| `/rateros ajustes` | Ver todos los ajustes |
| `/rateros ajustes nivelHurto agresivo` | Cambiar un ajuste (cualquier ajuste que no sea lista) |
| `/rateros recargar` | Volver a leer el archivo de ajustes |
| `/rateros invocar` | **(Necesita trucos)** Hace llegar una banda ahora, para probar tus defensas |
| `/rateros invocar 6` | **(Necesita trucos)** Una banda de 6 |
| `/rateros retirar` | **(Necesita trucos)** Quita a los rateros cercanos (sueltan lo que llevaban) |

Los trucos se activan al **crear el mundo** (**Permitir trucos: Sí**). En la versión 26.2, "Abrir en LAN" con comandos no siempre te los da a ti si el mundo se creó sin trucos. Todo lo demás (ajustes, activar y desactivar) funciona sin trucos.
