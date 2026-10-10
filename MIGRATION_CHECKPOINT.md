# Checkpoint: migração do core pro renderer modular

Branch: `feature/migrate-core-to-render-module` (a partir de `feature/render-module`).

## O que foi feito

Reescrita completa de `java-game-engine-core` e dos exemplos em `java-game-engine-demos` por cima
do renderer modular de `java-game-engine-renderer` (`Renderer`/`Material`/`Mesh`/`RenderPassDescriptor`
etc., que já existia). Não sobrou nenhuma classe da engine antiga (`GameEngine`, `GameState`, o
pacote `render.*` antigo, o subsistema ECS inteiro).

### Arquitetura nova do `core`

- **`com.farias.rengine.engine`**: `Engine`, `EngineContext`, `Module`, `Timer` — o padrão de
  registro de módulos que já existia só dentro de `renderer/example/Example.java`, promovido a
  classes reutilizáveis de verdade.
- **`com.farias.rengine.engine.modules`**: `WindowModule`, `InputModule`, `RenderModule` — criam
  `Window`/`InputSystem`/`Renderer` e registram no `EngineContext`.
- **`Game`** (em `com.farias.rengine`): casca fina sobre `Engine`/`EngineContext`/`Timer`.
  `onCreate()` → `onUpdate(dt)` → `onRender(dt)`, sem `GameState`, sem passes implícitos. Cada jogo
  monta seus próprios `RenderPassDescriptor`/`RenderView`/`RenderCommand` em `onRender` (mesmo
  estilo do `Example.java`).
- **`com.farias.rengine.gfx`**: helpers de instância (não singleton) pra não duplicar lógica em
  cada demo — `SpriteBatch2D` (quad compartilhado + uniforms `uvOffset`/`uvScale` pra recorte de
  spritesheet, em vez de reescrever o buffer de UV do mesh), `BitmapFont`, `TexturedModel`,
  `Materials` (fábrica de material "lit" com luz direcional), `Meshes` (primitivas, hoje só
  `plane`), `Projections` (ortho2D/ortho3D/perspectiva), `Shaders` (fonte GLSL embutido).
- **`util.ModelLoader`**: parser de `.obj` (inalterado na lógica), agora recebendo
  `Renderer`/`Shader` explícitos em vez de puxar de um facade estático. Ganhou um parâmetro opcional
  `flipNormals` (ver `java-game-engine-demos/CHESS_MODELS.md`).
- **`util.TextureLoader`**: carrega PNG via STB e cria `Texture` do renderer modular.

### Os 5 exemplos (`java-game-engine-demos`)

Removidos: `Simple2DGame`, `BasicTopDownMovement`, `SpriteMovement`, `TopDownCamera2D` (ECS, sem
substituto — não tem mais ECS no motor).

Mantidos, todos reescritos contra a API nova:

- **`SnakeGame`** — 2D, uma pass só (sprites + texto, sem depth test).
- **`SpriteEditor`** — 2D, editor de pixel art via mouse, mesma ideia.
- **`ModelViewer`** — 3D: pass de câmera em perspectiva (`earth`/`clouds`, modelos OBJ) + pass de UI
  (ortho2D flat) por cima.
- **`IsometricChess`** — 3D: pass isométrica (tabuleiro + peças) + pass de UI. Precisou de ajustes
  extras nos modelos de peças (ver seção de pendências).
- **`ShadowCubeDemo`** — novo exemplo, migrado de `renderer/example/Example.java`. Não usa `Game`
  nem a fachada nenhuma — monta `Engine`/`WindowModule`/`RenderModule` e fala com o `Renderer`
  direto, pra mostrar o nível mais baixo de composição também (shadow mapping multi-pass).

`Launcher` atualizado pra listar os 5.

## Estado de verificação visual

Rodei todos os 5 (sem crash, smoke test de alguns segundos cada) logo após a reescrita. Depois,
**verificação visual de verdade (rodando e olhando)** só aconteceu pra:

- ✅ **ModelViewer** — confirmado funcionando (mundo visível, UI legível, câmera em perspectiva,
  zoom funcionando via `cameraDistance`).
- ✅ **IsometricChess** — confirmado funcionando (tabuleiro visível, peças opacas e do lado certo
  pra fora, iluminação correta) depois de uma sequência de bugs (ver abaixo).

**Nunca verificados visualmente nesta sessão** (só smoke test, sem crash, mas ninguém olhou a tela):

- ⚠️ **SnakeGame**
- ⚠️ **SpriteEditor**
- ⚠️ **ShadowCubeDemo**

Vale a pena rodar os três e conferir antes de considerar o port 100% fechado — a sequência de bugs
no ModelViewer/IsometricChess (abaixo) mostra que "compila e não crasha" não significa "renderiza
certo".

## Bugs encontrados e corrigidos (nessa ordem)

1. Viewport calculado com o tamanho lógico da janela em vez do framebuffer real — em tela HiDPI
   (Retina) só aparecia 1/4 da cena. Corrigido com `Window.getFramebufferWidth/Height`.
2. Culling 3D desligado globalmente (tentativa de corrigir o item 1) quebrou o `ModelViewer`: a
   esfera de nuvem semi-transparente desenhava frente e verso, dando artefato visual. Culling
   voltou a ligado por padrão.
3. `ModelViewer` desenhava o texto de UI dentro de `onGfxUpdate` (câmera 3D isométrica ainda ativa)
   em vez de `onUIUpdate` — texto saía "torto" (deformado pela view 3D).
4. Luz direcional padrão (`Materials.lit`) apontava pro lado oposto da câmera isométrica usada por
   `ModelViewer`/`IsometricChess` — o lado visível do modelo não recebia luz difusa, ficava quase
   preto.
5. (Depois da reescrita pro padrão `Engine`/`Module`) Texto do `ModelViewer` sumiu de novo: posição
   Y positiva ficava fora da área visível da projeção ortho2D (que vai de `0` a `-height`).
   Corrigido pra Y negativo.
6. Texto do `ModelViewer` aparecia cortado: a convenção de `BitmapFont.drawText` centraliza o texto
   em X, e `x=20` era perto demais da borda esquerda pra uma string longa. Corrigido centralizando
   em `WINDOW_WIDTH/2` (mesma convenção que `IsometricChess` já usava).
7. Trocado o mundo do `ModelViewer` de ortho3D pra perspectiva (pedido explícito) — precisou reduzir
   `cameraDistance` (a Terra tem raio ~1) e reconectar o zoom (`Z`/`Shift+Z`) pra mexer em
   `cameraDistance` em vez de um `fov` que não existe mais em projeção perspectiva.
8. `IsometricChess`: chão do tabuleiro invisível com culling ligado. Causa: `Meshes.plane` tem
   normal fixa, e a rotação de +90° em X usada pra deitar o quad como piso jogava a normal pra
   baixo (longe da câmera/luz) em vez de pra cima.
9. Depois de corrigir só a normal do chão (sem mexer no winding), ele ficou consistente o
   suficiente pra aparecer escurecido mas visível com culling desligado — precisou corrigir também
   o **winding** (ordem dos índices) de `Meshes.plane` pra bater com a normal corrigida, permitindo
   religar culling sem perder o chão.
10. Peças de xadrez apareciam "de dentro pra fora" com culling ligado (winding do `.obj` ao
    contrário do `earth`/`clouds`).
11. Peças de xadrez ficaram "transparentes"/ver-através mesmo com textura 100% opaca confirmada.
    Causa raiz: o shader `LIT_FRAGMENT` fazia `fragColor = texColor * light`, multiplicando o canal
    alpha pela luminosidade — fragmentos escuros ficavam com alpha baixo e, com blending ligado,
    pareciam transparentes. Corrigido pra só multiplicar RGB, alpha vem direto da textura.
    `IsometricChess` também teve blending desligado de vez (não precisa).
12. Depois de corrigir a transparência, ainda via "por dentro" das peças com culling ligado —
    confirmado que o winding delas é mesmo invertido (não é só a transparência mascarando).
    Desligar culling só nessa pass resolve, porque sem blending o depth test sozinho já escolhe a
    superfície certa independente de winding.
13. Iluminação das peças ainda errada (maior parte escura) mesmo com a geometria certa. Tentei um
    fix via `gl_FrontFacing` no shader (inverter a normal do lado "de trás") — não resolveu, porque
    o problema não é o winding, são as **normais do arquivo .obj em si** apontando pra dentro,
    independente de qual lado a GPU classifica como frente. Medido contra o centroide da malha:
    ~75% das normais de `pawn_white.obj` apontam pra dentro. Corrigido com um parâmetro
    `flipNormals` em `ModelLoader.loadWavefrontOBJ`, usado só pros modelos de peça.

## Pendências

- **`java-game-engine-demos/CHESS_MODELS.md`** (não commitado ainda): documento explicando a causa
  raiz dos bugs #10–13 (normais/winding invertidos nos `.obj` das peças, não um bug do motor) e os
  passos pra reexportar os modelos certos no Blender, se/quando isso for feito. Até lá, o motor
  segue com o `cullFace(false)` + `flipNormals=true` como contorno funcional (custo: ~2x triângulos
  rasterizados nas peças, já que os dois lados são desenhados).
- Rodar e verificar visualmente **SnakeGame**, **SpriteEditor** e **ShadowCubeDemo** (só tiveram
  smoke test, nunca foram olhados na tela nesta sessão).
- Se os modelos de xadrez forem reexportados corretamente: reverter `cullFace(false)` →
  `cullFace(true)` em `IsometricChess.onCreate` e tirar o `true` de `flipNormals` na chamada de
  `ModelLoader.loadWavefrontOBJ` em `ChessBoard.createAndPlace`.

## Como rodar

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

cd java-game-engine-renderer && mvn -q -DskipTests install
cd ../java-game-engine-core && mvn -q -DskipTests install
cd ../java-game-engine-demos && mvn -q -DskipTests compile
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -XstartOnFirstThread -cp "target/classes:$(cat /tmp/cp.txt)" com.farias.rengine.examples.Launcher
# ou direto uma classe, ex:
java -XstartOnFirstThread -cp "target/classes:$(cat /tmp/cp.txt)" com.farias.rengine.examples.SnakeGame
```

(`-XstartOnFirstThread` é necessário no macOS pro GLFW/OpenGL funcionar.)

Todos os módulos são Java 17 agora (eram Java 11 antes da migração).
