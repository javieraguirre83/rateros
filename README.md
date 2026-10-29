# Rateros para Minecraft 26.2 (Forge)

Bandas de **rateros** que buscan y roban tus **cosas ya trabajadas**: lingotes, tablones, comida cocinada, herramientas, armaduras, diamantes… No saben dónde vives, pero **olfatean el botín**, así que llegan solos a donde guardas tus cosas.

## ⬇️ Descargar

**[Descargar el mod (.jar)](https://github.com/javieraguirre83/rateros/releases/download/ultima/Rateros-1.0.1-forge-26.2.jar)**

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

**La banda** (máximo 8)

| Días del mundo | Rateros por banda |
|---|---|
| 1 a 10 | 2 a 3 |
| 11 a 30 | 3 a 5 |
| 31 en adelante | 4 a 8 |

| Rol | Arma | Carga (Normal) | Probabilidad de llevarse algo valioso |
|---|---|---|---|
| 🟢 **Ladrón** | Espada de piedra o hierro (es más rápido) | 3 montones | 10% |
| 🔵 **Ballestero** | Ballesta | 1 montón | 10% |
| 🔴 **Forzador** | Hacha (rompe el doble de rápido) | 2 montones | 25% |
| 🟣 **Jefe** (bandas grandes, día 31+) | Espada, abrigo largo, doble de vida | 4 montones | 50% |

- Cada rol tiene su ropa: pañuelo en la cara, chaleco y un **costal en la espalda**. El jefe trae abrigo largo morado con dorado.
- Las armas **mejoran con los días**: piedra y hierro al inicio, luego algunos encantamientos y, rara vez, diamante (sobre todo el jefe).
- Los **ladrones** no te atacan a menos que les pegues; los demás sí pelean.

**El robo**
- Buscan **baúles, baúles dobles, barriles, hornos y cajas de shulker** con cosas trabajadas. Los **abren** (con su animación y su sonido), sacan cosas y los cierran.
- **Todos roban de todo.** El rol solo cambia cuánto cargan y qué tan probable es que vayan por lo **valioso** (diamantes, esmeraldas, netherita, objetos encantados…). Un ladrón también puede llevarse un diamante, solo es menos probable.
- También recogen cosas trabajadas **tiradas en el suelo**.
- **Nunca te quitan lo que traes encima.**
- **No pueden abrir el cofre de ender:** es tu lugar seguro.
- **Cofre trampa:** al abrirlo un ratero, **se activa la redstone**, igual que contigo. ¡Pon trampas!
- No tocan cofres de estructuras que todavía no has abierto.
- Cuando se llenan (o si los lastimas mucho), **huyen**. **Si los matas, sueltan todo lo que robaron.** Si se alejan unos 48 bloques de ti o pasan 2 minutos, **escapan** y lo robado se pierde.
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

- **General:** activar o desactivar, horario, cuánto más probable de noche, % de banda por día (inicio y avanzado), día de mundo avanzado, aviso, si roban en aldeas.
- **La banda:** máximo de rateros, tamaños por etapa, días de cada etapa, qué roles pueden venir, si las armas mejoran, si pueden traer diamante o encantamientos.
- **El robo:** nivel de hurto, cuánto carga cada rol, % de ir por lo valioso de cada rol, segundos para alcanzarlos, distancia a la que escapan y radio en el que olfatean.
- **Cómo entran:** si rompen bloques y cuánto tardan. Abajo: **Abrir carpeta de ajustes** y **Recargar archivo**.

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

Para activar trucos en un mundo que no los tiene: **Esc → Abrir en LAN → Permitir trucos: Sí → Iniciar mundo LAN**.
