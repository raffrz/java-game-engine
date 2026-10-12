#!/usr/bin/env bash
# Roda cada demo por N segundos (padrao 5) e reporta se ele ficou de pe
# (OK) ou morreu antes do tempo (FALHA, com o fim do log).
#
# Uso: scripts/run-demos.sh [-t segundos] [demo ...]
#   -t N   segundos por demo (padrao 5)
#   demo   nomes das classes em com.farias.rengine.examples
#          (padrao: SnakeGame SpriteEditor ModelViewer IsometricChess ShadowCubeDemo)
set -u

SECONDS_PER_DEMO=5
while getopts "t:" opt; do
    case "$opt" in
        t) SECONDS_PER_DEMO=$OPTARG ;;
        *) echo "uso: $0 [-t segundos] [demo ...]" >&2; exit 2 ;;
    esac
done
shift $((OPTIND - 1))

DEMOS=("$@")
if [ ${#DEMOS[@]} -eq 0 ]; then
    DEMOS=(SnakeGame SpriteEditor ModelViewer IsometricChess ShadowCubeDemo)
fi

ROOT=$(cd "$(dirname "$0")/.." && pwd)
DEMOS_DIR="$ROOT/java-game-engine-demos"
PKG=com.farias.rengine.examples

# JDK 17+ (o enforcer do projeto exige); no macOS procura via java_home.
if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE 'version "(1[7-9]|[2-9][0-9])'; then
    if [ -x /usr/libexec/java_home ]; then
        JAVA_HOME=$(/usr/libexec/java_home -v 17+ 2>/dev/null) || {
            echo "JDK 17+ nao encontrado (defina JAVA_HOME)" >&2; exit 2; }
        export JAVA_HOME
    fi
fi
JAVA="$JAVA_HOME/bin/java"

PLATFORM_ARGS=()
[ "$(uname -s)" = "Darwin" ] && PLATFORM_ARGS=(-XstartOnFirstThread)

# Build e classpath na mesma invocacao: os modulos irmaos so resolvem pelo
# reactor (nao estao no ~/.m2), entao "package" e "build-classpath" precisam
# rodar juntos. O ultimo modulo do reactor (demos) escreve o arquivo final.
CP_FILE=$(mktemp)
LOG_DIR=$(mktemp -d)
trap 'rm -rf "$CP_FILE" "$LOG_DIR"' EXIT
echo ">> build"
(cd "$ROOT" && ./mvnw -q -pl java-game-engine-demos -am package dependency:build-classpath \
    -DskipTests -Dmdep.outputFile="$CP_FILE") || { echo "build falhou" >&2; exit 1; }
CP="$DEMOS_DIR/target/classes:$(cat "$CP_FILE")"

fail=0
for demo in "${DEMOS[@]}"; do
    log="$LOG_DIR/$demo.log"
    # cwd = demos/, onde ficam os "resources/..." usados pelos jogos.
    (cd "$DEMOS_DIR" && exec "$JAVA" "${PLATFORM_ARGS[@]}" -cp "$CP" "$PKG.$demo") >"$log" 2>&1 &
    pid=$!

    alive=1
    for ((i = 0; i < SECONDS_PER_DEMO; i++)); do
        sleep 1
        if ! kill -0 "$pid" 2>/dev/null; then alive=0; break; fi
    done

    if [ "$alive" -eq 1 ]; then
        kill "$pid" 2>/dev/null
        wait "$pid" 2>/dev/null
        printf "OK     %-20s (%ss)\n" "$demo" "$SECONDS_PER_DEMO"
    else
        wait "$pid" 2>/dev/null
        code=$?
        printf "FALHA  %-20s (saiu antes do tempo, codigo %s)\n" "$demo" "$code"
        tail -n 15 "$log" | sed 's/^/    | /'
        fail=1
    fi
done

exit $fail
