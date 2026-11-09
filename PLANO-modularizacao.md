# Plano: modularização da engine (core, platform-glfw, assets, toolkit2d, engine3d)

> **Status final (concluído):** Commits 1 a 8 e 9a/9b implementados (módulos `assets`,
> `toolkit2d`, `engine3d`, `platform-glfw`; `RenderModule` movido para `renderer`; `core` sem
> dependência externa; demos com `main` e loop próprios). **Adiados de propósito:** handles
> opacos + `RenderBackend`/`NullBackend` (9.1/9.2), hints de contexto parametrizáveis (9.4),
> uniforms como dados e shaders portáveis (9.5). Seguimento registrado: `Materials` ser dono do
> shader lit; NPE pré-existente no `SnakeGame`. Ver README (regras de arquitetura).

Status: v2, revisada. Nenhuma linha de código foi alterada.
Base: branch `feature/chess-piece-picking-raycast` (módulos `renderer`, `core`, `demos`).
v2 incorpora a revisão técnica (Opus) e as decisões do autor.

## 0. Decisões tomadas (resumo)

| Tema | Decisão |
|---|---|
| Módulos | `toolkit2d`, `assets` e `engine3d` são **módulos Maven próprios**, cada um com seu pacote-base. Nada de `gfx` dentro do core |
| Pacote `gfx` | **some inteiro do `core`** |
| Y invertido | Transformação aplicada **dentro** do toolkit (ortho e quad atuais intactos, o Canvas nega o Y). Não vaza para o jogo |
| Rotação | Convenção do Canvas: graus **horário na tela**. O Snake (exemplo) ajusta o próprio sinal |
| HUD / UI | **Não usa `Canvas`.** Componente próprio (`Ui`) em `toolkit2d.ui`; candidato a módulo Maven `ui` no futuro |
| ModelLoader | Vai para `engine3d` (e usa `Assets`); isso elimina o problema core→assets |
| `SpriteBatch2D` / `BitmapFont` antigos | Removidos ao fim da migração dos demos |
| Loop | Sempre do jogo. `Game` sai do core (ver §6) |

## 1. Objetivo

1. `toolkit2d`: sprites, texto bitmap, `Canvas` 2D e `Ui` de overlay, sem herdar
   conceitos 3D, para suportar jogos 2D (estilo Pico-8) sobre a engine.
2. O jogo é dono do loop. Sem `Game2D`, sem `Desktop.run`: o toolkit só oferece
   `Module`s e peças.
3. Carga de assets e cache em um serviço (`Assets`), não em métodos estáticos.
4. Direção de longo prazo: `core` sem nenhuma dependência de gfx/lwjgl-opengl,
   abrindo caminho para outros backends.

## 2. Princípios (restrições do plano)

- **Biblioteca, não framework:** `Engine`/`Module`/`EngineContext` compõem
  serviços; o loop é do jogo (como em `ShadowCubeDemo`).
- **Dependência de serviço sempre via `EngineContext.require`.**
- **`static` só para funções puras** (sem serviço, GPU ou disco).
- **Componentes com dependência de serviço vêm do módulo**: o módulo cria e
  registra (`Engine3DModule`: `Meshes`, `Materials`, `ModelLoader`;
  `Toolkit2DModule`: `Canvas`, `Ui`) e o jogo só chama
  `context.require(X.class)`. O jogo não faz `new` de componente que receba
  `Renderer`/`Assets`. Fica fora da regra o que é função/valor puro
  (`Camera`, `TexturedModel`, `Projections`).
- **Fatia estreita do renderer** no toolkit: `createShader`, `createMesh` (um
  quad), `createMaterial`, `createPass`/`submit`. Nenhum tipo 3D
  (`Mesh`, `RenderView`, `Matrix4f`) na API de jogo do `Canvas`/`Ui`.
- **2D e 3D são irmãos**: `toolkit2d` e `engine3d` nunca dependem um do outro.
  Um jogo 3D registra `Toolkit2DModule` e usa `Ui` para HUD.
- **Detalhes de convenção (Y, UV, winding) não vazam**: o jogo vê Y para baixo,
  origem no canto superior esquerdo, valores positivos.

## 3. Grafo de módulos

Estado atual:

```
renderer ◄── core ◄── demos
```

Estado alvo desta fase (Commit 1 em diante):

```
renderer ◄── core ◄── assets ◄─┬── toolkit2d ◄── demos
                               └── engine3d  ◄── demos
```

- `core → renderer` permanece **apenas** por causa de `RenderModule`/`WindowModule`
  (GL). Sai na fase final (§7, Commit 9).
- `assets → core + renderer` (Module/EngineContext + Texture/MeshData).
- `toolkit2d → core + renderer + assets`; `engine3d → core + renderer + assets`.
- Sem ciclo: `ModelLoader` e `TextureLoader` deixam o core.
- `.gitignore` contém `core`, que ignora qualquer diretório com esse nome:
  conferir antes de criar pastas novas.

### Conteúdo de cada módulo (fim do Commit 1)

| Módulo | Pacote-base | Conteúdo |
|---|---|---|
| `core` | `com.farias.rengine` | `Engine`, `Module`, `EngineContext`, `Timer`, `io/Window`, `io/InputSystem`, `engine/modules/*` |
| `renderer` | `com.farias.rengine.renderer` | como hoje |
| `assets` | `com.farias.rengine.assets` | `AssetsModule`, `Assets`, `TextureLoader` (STB; sai do core) |
| `toolkit2d` | `com.farias.rengine.toolkit2d` | `SpriteBatch2D`, `Sprite`, `BitmapFont`, `Projections2D` (legados, em `toolkit2d.legacy`, até o Commit 8); `SpriteShaders` em `toolkit2d.draw`; depois `SpriteSheet`, `Canvas`, `Ui`, `Toolkit2DModule` |
| `engine3d` | `com.farias.rengine.engine3d` | `Game` (transitório), `Camera`, `TexturedModel`, `Materials`, `Meshes`, `Shaders` (LIT), `Projections` (3D), `ModelLoader` |

Nomes distintos para evitar sombreamento de classes: o toolkit usa
`SpriteShaders` e `Projections2D`, e o 3D mantém `Shaders`/`Projections`
em pacote próprio. Os legados ficam em `toolkit2d.legacy` porque a `BitmapFont`
nova (Commit 5) tem o mesmo nome simples e iria para o mesmo pacote-base que a
legada; com subpacote, as duas coexistem nos Commits 5-7. `Projections2D` e
`SpriteShaders` precisam ser **públicos** até o Commit 8: os demos ainda
importam `Projections2D` (Snake, SpriteEditor, HUD de IsometricChess/ModelViewer)
e o `SpriteBatch2D` legado importa `SpriteShaders` de outro pacote.

## 4. Exemplo-alvo (jogo)

```java
public static void main(String[] args) {
    Engine engine = new Engine()
        .registerModule(new WindowModule(640, 640, "Snake"))
        .registerModule(new InputModule())
        .registerModule(new RenderModule())                       // liga endFrame -> swapBuffers
        .registerModule(new AssetsModule(Path.of("resources")))
        .registerModule(new Toolkit2DModule(640, 640));           // Canvas + Ui

    EngineContext ctx = engine.start();
    try {
        new SnakeGame(ctx, 10, 10).run();
    } finally {
        engine.terminate();                                       // WindowModule faz glfwTerminate
    }
}

// SnakeGame
window   = ctx.require(Window.class);
input    = ctx.require(InputSystem.class);
renderer = ctx.require(Renderer.class);
assets   = ctx.require(Assets.class);
canvas   = ctx.require(Canvas.class);

palette    = new SpriteSheet(assets.texture("palletes/default.png"), 32, 32);
snakeSheet = new SpriteSheet(assets.texture("snakegame/snake-pixel-art.png"), 8, 8);
appleSheet = new SpriteSheet(assets.texture("snakegame/apple-pixel-art.png"), 8, 8);
font       = canvas.defaultFont();

void run() {
    Timer timer = new Timer();
    while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
        timer.update();
        input.update();
        update(timer.deltaTime());

        renderer.beginFrame();
        canvas.begin();
        draw(canvas);
        renderer.endFrame();            // executa passes e apresenta
    }
}

// draw: y positivo para baixo, sem sinais negativos.
// Rotação do Canvas é horária; a conta do Snake continua produzindo
// o ângulo anti-horário antigo, então o call site nega uma vez:
canvas.spr(snakeSheet, tile, x * cw, y * ch, cw, ch, -rotation);
canvas.textCentered(font, "score: " + score, width / 2, 32, 14);  // 14 = FONT_SIZE atual
```

## 5. Design do toolkit2d

### 5.1 Estrutura interna

```
com.farias.rengine.toolkit2d
  Toolkit2DModule      Module: require(Renderer, Window, Assets); registra Canvas e Ui
  Canvas               cena 2D de um pass; é dono do clear
  SpriteSheet          record (Texture texture, int tileW, int tileH)
  BitmapFont           (Texture atlas, glyphW, glyphH)  - sem I/O
com.farias.rengine.toolkit2d.draw
  SpriteRenderer       primitiva de baixo nível: quad, shader, cache de Material,
                       conversão de coordenadas (Y, rotação), pool de Matrix4f por frame
  SpriteShaders        GLSL 330 (dívida, ver §8)
com.farias.rengine.toolkit2d.ui
  Ui                   overlay; nunca limpa a tela
```

`Canvas` e `Ui` são fachadas finas sobre `SpriteRenderer`. `ui` só importa
`draw` e os tipos de valor `SpriteSheet`/`BitmapFont`, nunca `Canvas`: assim o
pacote pode virar o módulo Maven `ui` (dependendo de `toolkit2d`) sem
reescrita (basta `draw` ser público e estável).

### 5.2 Canvas

- Descriptor: `depthTest(false)`, `cullFace(false)`, `blend(true)`, clear de cor
  configurável (`clearColor(r,g,b)`).
- `begin()`: cria `RenderView(fbW, fbH, ortho2D(virtualW, virtualH), identity)`
  (framebuffer lido de `Window`) e `renderer.createPass(...)`.
- API: `spr(sheet, tile, x, y, w, h)`, `spr(..., rotationDeg)`,
  `text(font, str, x, y[, size])` (à esquerda), `textCentered(font, str, x, y[, size])`,
  `defaultFont()`. `size` = altura do glifo em unidades virtuais (default:
  `glyphH`); necessário porque os demos desenham o atlas 16x16 em 4, 14 e 20
  (`SpriteEditor.java:116`, `SnakeGame.java:272`, `ModelViewer.java:195`).
  `textCentered` reproduz o `BitmapFont.drawText` legado (centra em x).
- **Convenção Y**: o ortho continua `setOrtho(0,w,-h,0)` e o quad/UV não mudam;
  o `SpriteRenderer` chama `rectTransform(x, -y, w, h, -rotation)`. Winding e UV
  intactos; o jogo nunca vê um Y negativo.
- Valida estado: `begin()` falha com mensagem clara se chamado fora de um frame
  (ver `Renderer` Commit 4); cada draw valida que `begin()` foi chamado neste frame.

### 5.3 Ui (HUD/overlay)

- Pass próprio: `clearColorBuffer(false)`, `clearDepthBuffer(false)`,
  `depthTest(false)`, `blend(true)`, `cullFace(false)`.
- Ordem de execução = ordem de criação do pass: `ui.begin()` vem depois do pass
  3D. Documentar.
- API inicial mínima (apenas o que os demos usam): `text`, `textCentered`,
  `image(sheet, tile, x, y, w, h)`. Widgets, layout e hit-testing ficam para o
  futuro módulo `ui`.

### 5.4 Cache de materiais

`uvOffset/uvScale` são uniforms do `Material`, e `Renderer.executePass` só
reaplica uniforms quando o material muda; por isso hoje existe um `Material` por
sprite. Estratégia: `Map<MaterialKey, Material>` com
`record MaterialKey(Texture texture, int tileW, int tileH, int tile)`. Chave por
valor (o `Texture` é estável via `Assets`), limitada ao número de tiles
distintos; esvaziada no `dispose` do renderer/módulo.

### 5.5 Matrizes

`RenderCommand` guarda o `Matrix4f` por referência até `endFrame`, então não há
scratch único. O `SpriteRenderer` mantém um pool de `Matrix4f` resetado **uma
vez por frame** (quando o `frameIndex` do `Renderer` muda), **não** em cada
`begin()`: `Canvas` e `Ui` compartilham o mesmo `SpriteRenderer`, e um reset em
`ui.begin()` sobrescreveria matrizes que o pass do `Canvas` ainda referencia
antes do `endFrame`. Zero `Matrix4f` alocada em regime (o `RenderCommand` em si
continua sendo alocado por draw, como em todo o renderer).

## 6. Papel do `Game`

`Game` impõe um loop e importa `GLFW_KEY_ESCAPE`/`glfwTerminate`. Vai para
`engine3d` como transitório (Commit 1) e é **removido ao fim da migração** (Commit 8)
quando todos os demos tiverem loop próprio, como `ShadowCubeDemo` já tem.
Enquanto existir, é o único consumidor do padrão antigo.

## 7. Commits

Fluxo: commits sequenciais direto na branch de trabalho, sem PRs nem merges
intermediários. Cada commit compila e mantém todos os demos executáveis, então
qualquer ponto da história serve de rollback (`git revert` ou `git reset`).
"Mover" = só mudar de módulo/pacote, sem alterar lógica; commits de mover ficam
separados dos que mudam comportamento para o `git diff -M` ler como rename.

### Commit 1: esqueleto Maven + mover (sem mudar comportamento)

- `pom.xml` raiz: novos `<module>` (`assets`, `toolkit2d`, `engine3d`) e entradas
  em `dependencyManagement`.
- `assets`: `AssetsModule`/`Assets` casca (mapa vazio); recebe `TextureLoader`
  (core perde `lwjgl-stb`).
- `toolkit2d`: recebe `SpriteBatch2D`, `Sprite`, `BitmapFont` e
  `Projections.orthographic2D` (-> `Projections2D`) em `toolkit2d.legacy`;
  `Shaders.SPRITE_*` (-> `SpriteShaders`, público) em `toolkit2d.draw`; o resource
  `fonts/default16x16.png` (`core/src/main/resources` -> `toolkit2d/src/main/resources`).
- `engine3d`: recebe `Game`, `Camera`, `TexturedModel`, `Materials`, `Meshes`,
  `Shaders.LIT_*`, `Projections` 3D, `ModelLoader`.
- `core/gfx`, `core/util`, `core/camera` e `core/Game.java` saem (diretórios removidos).
- Demos: apenas imports/pacotes e dependências Maven.
- Critério: build verde, todos os demos abrem iguais.

### Commit 2: `Assets` de verdade

- `Assets.texture(path)`: resolve a partir da raiz de `AssetsModule(Path)`, com
  fallback de classpath; cache `Map<String,Texture>`; exceção com caminho
  resolvido em falha.
- `Assets.open(path)` (`InputStream`, mesma resolução raiz -> classpath, sem
  cache): necessário para o `ModelLoader` ler o `.obj` sem caminho fixo.
- `TextureLoader`: variante por `InputStream`/`ByteBuffer`; vira interno
  (package-private) ao módulo `assets`. Isso quebra quem o chama de fora do
  pacote, então no mesmo commit migram **todos** os chamadores:
  `SpriteBatch2D.createSprite` legado (`SpriteBatch2D.java:57`),
  `ModelLoader` (`ModelLoader.java:126`) e `IsometricChess` (`IsometricChess.java:109`).
- `Engine3DModule` (engine3d) cria e registra `Meshes`, `Materials` e
  `ModelLoader` (instâncias, com `Renderer`/`Assets` injetados); `Game` o
  registra depois do `AssetsModule` e os demos usam `context.require(...)`.
  Os `new SpriteBatch2D(renderer, assets)` dos demos ficam (legado, sai no
  Commit 8; o substituto nasce como serviço no Commit 5).
- `Game` (transitório) registra `AssetsModule(Path.of("resources"))` depois do
  `RenderModule` e expõe `protected Assets assets`: os demos que estendem `Game`
  não têm outro jeito de obter o serviço.
- `SpriteBatch2D` legado passa a receber `Assets` (`new SpriteBatch2D(renderer, assets)`)
  e `createSprite` resolve por ele; os call sites perdem o prefixo `resources/`
  (`SnakeGame.java:251-255`, `SpriteEditor.java:66`).
- `ModelLoader` (engine3d) passa a receber `Assets`; deixa de retornar `null`
  em erro; caminhos fixos `"resources/models/"` e `"resources/textures/"` saem.
- `BitmapFont` legado deixa de gravar `default16x16.png` em disco
  (`extractFontFile` removido) e carrega o atlas `fonts/default16x16.png` pelo
  fallback de classpath do `Assets`. As 2 cópias extraídas deixam de existir:
  a da raiz (não versionada) e `java-game-engine-demos/default16x16.png`
  (versionada: `git rm`). O resource em `toolkit2d/src/main/resources/fonts/` fica.
- Observação: limpar o cache não libera GPU (texturas pertencem ao
  `Renderer.resources`); `Texture.dispose` já é idempotente. Revisar o `flip()`
  em `Texture` (buffer na posição 0) ao introduzir a variante por `ByteBuffer`.

### Commit 3: `Key`/`MouseButton`

- Enums em `core/io` (setas, `ENTER`, `ESCAPE`, `LEFT_SHIFT`, `Z`, `S`, `A`,
  `F1` + o que `IsometricChess`/`ModelViewer` precisarem; `MouseButton`).
- `InputSystem`: overloads por `Key`/`MouseButton` com tabela -> GLFW; os `int`
  ficam `@Deprecated`.
- `Game` (`GLFW_KEY_ESCAPE`) e `isMouseButtonDown(0)` em `IsometricChess` e
  `SpriteEditor` (`SpriteEditor.java:83`) incluídos. Demos trocam `GLFW_KEY_*` e perdem imports `org.lwjgl.glfw`.

### Commit 4: ciclo de frame e plataforma

- `Renderer`: estado de frame (`inFrame`/`frameIndex`), exposto para validação.
  Mudança de API declarada.
- `Renderer`: hook `Present` (`Runnable`), definido no módulo renderer
  (sem depender de `Window`); `RenderModule` liga `window::swapBuffers`.
  `endFrame()` apresenta. `Game` e `ShadowCubeDemo` perdem o swap.
- `WindowModule.onTerminate`: destrói a janela (novo `Window.destroy()`, que hoje
  não existe) e chama `glfwTerminate` (hoje feito por `Game.java:74`, que perde
  essa chamada). Como `Engine.terminate` roda em ordem inversa, isso acontece
  depois do `renderer.dispose()` do `RenderModule`, com o contexto GL ainda vivo.
- `window::swapBuffers` como `Runnable` resolve para a sobrecarga sem argumento
  (`swapInterval(0)`, igual ao comportamento atual de `Game`/`ShadowCubeDemo`).

### Commit 5: `Canvas` / `SpriteSheet` / `Toolkit2DModule` / `SpriteRenderer`

- Implementa §5.1, §5.2, §5.4, §5.5. `BitmapFont` nova (atlas + glyph size).
- `Toolkit2DModule(virtualW, virtualH)`; o legado ainda coexiste.
- Teste visual obrigatório (não há testes automatizados de render): neste
  commit ainda nenhum demo usa `Canvas`, então a comparação lado a lado com o
  legado (orientação, rotação, UV, tamanho do texto) é feita em 7a/7b, antes de
  apagar o código legado do demo.

### Commit 6: `Ui`

- Implementa §5.3 sobre `SpriteRenderer`. Registrado por `Toolkit2DModule`.
- Não toca em `Canvas`.

### Commit 7: migrar demos

Em sub-commits independentes:
- 7a `SnakeGame`: forma do §4 (remove `extends Game`, `SnakeGameRenderer`,
  `RenderView`/`RenderPassDescriptor`/`Matrix4f`, GLFW). Lógica (`moveSnake`,
  `isValid`, `Direction`, tiles) inalterada; só o call site da rotação nega o
  sinal.
- 7b `SpriteEditor` -> `Canvas`. Diferente do Snake, a lógica muda: o mouse e
  o hover usam Y negativo (`SpriteEditor.java:78`, `85`, `121`, `125`, `129`), que passa a
  ser positivo.
- 7c `IsometricChess` e `ModelViewer`: HUD -> `Ui`; loop próprio.

### Commit 8: limpeza

- Remove o pacote `toolkit2d.legacy` inteiro (`SpriteBatch2D`, `Sprite`,
  `BitmapFont` legado, `Projections2D`) e `Game` (com o `AssetsModule` que ele
  registrava). Confirmar com `grep` que nada mais usa.
- Resultado: nenhum pacote `gfx` em lugar nenhum.

### Commit 9: purificação do core (direção final; fora do necessário)

- `core` deixa de depender de `renderer` e `lwjgl-opengl`: `RenderModule` e a
  parte GL de `WindowModule` vão para um backend (`backend-opengl`).
- Interfaces `Window`/`InputSystem` no core; implementação GLFW em módulo de
  plataforma. Hints de contexto no módulo de plataforma,
  `GL.createCapabilities` no backend.
- Handles opacos para `Mesh`/`Texture`/`Shader`; `RenderBackend.execute(frame)`.
- Fora de escopo agora: uniforms como dados, shaders portáveis, `SamplerDesc`.

## 8. Dívidas e riscos registrados

- GLSL 330 dentro de `toolkit2d` prende o toolkit ao OpenGL. A longo prazo o
  shader de sprite deveria ser um pipeline fornecido pelo backend.
- `Matrix4f`/JOML ainda aparece na API do renderer.
- GLFW em `Window`/`InputSystem` até o Commit 9.
- Regressão visual (Y, rotação, UV, ordem de passes) só é detectável rodando os
  demos; sem testes automatizados de render.
- Chave de cache de textura é o caminho; ao surgir `SamplerDesc`/`PixelFormat`
  precisam entrar na chave. Texturas dinâmicas (depth map, futuros
  framebuffers) não passam por `Assets`.
- O pom do `renderer` declara `lwjgl-glfw` sem usá-lo.
- A extração do módulo `ui` exige `toolkit2d.draw` estável e público.

## 8b. Follow-up registrado

- `Materials` ser dono do shader lit (criado uma vez), removendo
  `Shaders.LIT_*` dos demos e o parâmetro `litShader` de
  `ModelLoader.loadWavefrontOBJ` e `Materials.lit`. Muda assinaturas públicas;
  fazer como passo próprio depois do Commit 3.

## 9. Decisões ainda abertas (default assumido)

| # | Decisão | Default |
|---|---|---|
| 1 | Nome do módulo 3D | `java-game-engine-engine3d` |
| 2 | `Canvas.text` | à esquerda; `textCentered` para o Snake |
| 3 | `Game` | removido no Commit 8 (demos com loop próprio) |
| 4 | Ordem Commit 3 (Key) vs Commit 4 (frame/present) | independentes; podem inverter |
| 5 | `ModelLoader.loadWavefrontOBJ`, `Meshes.plane` e `Materials.lit` são `static` mas recebem `Renderer` (e, após o Commit 2, `Assets`) e fazem GPU/disco, o que contraria §2 ("`static` só para funções puras") | mover como estão no Commit 1 (commit de mover); converter `ModelLoader` em instância (`new ModelLoader(renderer, assets)`) no Commit 2 se o autor quiser cumprir §2 já nesta fase |
