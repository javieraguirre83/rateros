#!/usr/bin/env bash
# Descarga el MDK de Forge 26.2, mete el código del mod, compila y deja registros en ./registro
set -u
REPO="$(pwd)"
REG="$REPO/registro"
mkdir -p "$REG" /tmp/mdk-out
exec > >(tee "$REG/log.txt") 2>&1


BASE=https://maven.minecraftforge.net/net/minecraftforge/forge
VERSIONES=$(curl -sSf "$BASE/maven-metadata.xml" | grep -oE '<version>26\.2-[^<]+' | sed 's/<version>//' | tac)
echo "Versiones 26.2 encontradas:"; echo "$VERSIONES" | head -5
FORGE=""
for V in $VERSIONES; do
  if curl -sSfL -o /tmp/mdk.zip "$BASE/$V/forge-$V-mdk.zip"; then FORGE=$V; break; fi
done
[ -n "$FORGE" ] || { echo "No se encontró MDK de Forge 26.2"; exit 1; }
echo "Usando Forge $FORGE"; echo "$FORGE" > "$REG/forge-version.txt"

rm -rf /tmp/mdk && mkdir /tmp/mdk && unzip -q /tmp/mdk.zip -d /tmp/mdk
MDK=$(dirname "$(find /tmp/mdk -maxdepth 3 -name gradlew | head -1)")
echo "MDK en $MDK"

# Información del MDK (para saber cómo está organizado)
{
  echo "### Archivos"; (cd "$MDK" && find . -type f | grep -v '/.git/' | sort)
  for f in gradle.properties build.gradle settings.gradle src/main/templates/META-INF/mods.toml src/main/resources/META-INF/mods.toml; do
    [ -f "$MDK/$f" ] && { echo; echo "### $f"; cat "$MDK/$f"; }
  done
  echo; echo "### Código de ejemplo"; find "$MDK/src/main/java" -name '*.java' -exec sh -c 'echo "---- $1"; cat "$1"' _ {} \;
} > "$REG/mdk-info.txt" 2>&1

# Meter el código del mod
if [ -d "$REPO/src/main" ]; then
  rm -rf "$MDK/src/main/java/com/example"
  cp -r "$REPO/src/." "$MDK/src/"
  sed -i -E "s/^version = .*/version = '1.0.0-forge-26.2'/; s/^group = .*/group = 'rateros'/; s/examplemod/rateros/g" "$MDK/build.gradle"
  printf "\ntasks.withType(JavaCompile).configureEach { options.compilerArgs += ['-Xmaxerrs', '1000'] }\n" >> "$MDK/build.gradle"
  sed -i -E "s/^rootProject.name = .*/rootProject.name = 'Rateros'/" "$MDK/settings.gradle"
  if [ -f "$REPO/mod.properties" ]; then
    while IFS='=' read -r k v; do
      [ -z "$k" ] && continue
      if grep -qE "^$k=" "$MDK/gradle.properties"; then
        sed -i -E "s|^$k=.*|$k=$v|" "$MDK/gradle.properties"
      else
        echo "$k=$v" >> "$MDK/gradle.properties"
      fi
    done < "$REPO/mod.properties"
  fi
fi

cd "$MDK"
chmod +x gradlew
./gradlew build --no-daemon --console=plain > "$REG/build.txt" 2>&1
RESULTADO=$?
grep -E -A3 "error:|FAILED|BUILD|What went wrong" "$REG/build.txt" | head -400 > "$REG/errores.txt"

# Firmas reales de las clases de Minecraft/Forge 26.2 (para escribir el código correcto)
if [ -f "$REPO/.github/scripts/clases.txt" ]; then
  JARS=$(find ~/.gradle "$MDK" /tmp -name '*.jar' 2>/dev/null | while read -r j; do
    unzip -l "$j" 2>/dev/null | grep -qE 'net/minecraft/world/item/Item.class|net/minecraftforge/event/entity/player/PlayerEvent.class|net/minecraft/client/Minecraft.class|net/minecraftforge/eventbus/api/bus/EventBus.class|net/minecraftforge/fml/javafmlmod/FMLJavaModLoadingContext.class|net/minecraftforge/fml/loading/FMLEnvironment.class' && echo "$j"; done | sort -u)
  echo "$JARS" > "$REG/jars.txt"
  CP=$(echo "$JARS" | tr '\n' ':')
  : > "$REG/api.txt"
  while read -r C; do
    [ -z "$C" ] && continue
    case "$C" in
      \#*) continue ;;
      codigo:*)
        C2=${C#codigo:}
        echo "### codigo $C2" >> "$REG/api.txt"
        javap -c -p -cp "$CP" "$C2" >> "$REG/api.txt" 2>&1 ;;
      todo:*)
        P=${C#todo:}
        echo "### todo $P" >> "$REG/api.txt"
        for j in $JARS; do unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep -E "^$P" ; done | sort -u >> "$REG/api.txt" ;;
      ls:*)
        P=${C#ls:}
        echo "### ls $P" >> "$REG/api.txt"
        for j in $JARS; do unzip -l "$j" 2>/dev/null | awk '{print $4}' | grep -E "^$P[^/]*\.class$" ; done | sort -u >> "$REG/api.txt" ;;
      *)
        echo "### $C" >> "$REG/api.txt"
        javap -cp "$CP" "$C" >> "$REG/api.txt" 2>&1 ;;
    esac
  done < "$REPO/.github/scripts/clases.txt"
fi

if [ ! -d "$REPO/src/main" ]; then
  echo "Sin código del mod todavía: solo se recopiló información."
  RESULTADO=2
fi

if [ $RESULTADO -eq 0 ]; then
  cp build/libs/*.jar /tmp/mdk-out/ 2>/dev/null
  ls -la build/libs > "$REG/jar.txt"
  echo "COMPILÓ BIEN"
else
  echo "FALLÓ LA COMPILACIÓN"
fi
exit $RESULTADO
