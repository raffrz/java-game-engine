# Desenvolvimento

Notas de trabalho de quem mexe na engine: estrutura dos módulos, decisões de build, lições de
bugs passados e pendências. As **regras de arquitetura** (o que não pode ser quebrado) estão no
[README](README.md); este arquivo descreve o estado atual e o que falta.

## Regras de arquitetura: o porquê

A lista curta está no [README](README.md#architecture-rules). Aqui, a justificativa de cada uma.

1. **Biblioteca, não framework.** O jogo é dono do `main` e do loop. A engine nunca chama o jogo
   de volta e não oferece classe base (não existe `Game`/`Application` para estender). O `Engine`
   só registra módulos, inicia-os em ordem e os encerra em ordem inversa. Isso permite pausa,
   vários passes, loop de passo fixo, loop sem janela (testes) e misturar 2D com 3D sem pontos de
   extensão numa hierarquia.
2. **Composição por módulos.** Uma capacidade só existe se o jogo registrar o módulo que a
   fornece. Serviços são obtidos do `EngineContext` com `require(Class)`, que falha com mensagem
   clara se o módulo não foi registrado ou foi registrado na ordem errada. Trocar uma capacidade
   é trocar uma linha do `main`.
3. **Componentes com dependência vêm do módulo.** Tudo que precisa de serviço (`Renderer`,
   `Assets`, ...) é criado e registrado por um módulo, e o jogo faz `context.require(...)`. O
   jogo não faz `new` desses componentes. `static` fica para funções **puras** (sem serviço, GPU
   ou disco): `Projections`, `SpriteRenderer.orthographic`. Um `static` que carrega arquivo ou
   cria recurso de GPU esconde a dependência e impede cache.
4. **Direção de dependência.** `core` não depende de nada (sem gráficos, sem biblioteca de
   janela, sem LWJGL). Módulos da biblioteca nunca dependem de `platform-*`; só o jogo (ou um
   demo) depende. `toolkit2d` e `engine3d` são irmãos e nunca dependem um do outro: um jogo 3D
   que quer HUD registra o `Toolkit2DModule` e usa `Ui`. Nada específico de uma API gráfica entra
   no `core` nem na API pública dos toolkits.
5. **Plataforma é contrato.** `Window` e `InputSystem` são interfaces no `core`. Um módulo de
   plataforma (hoje `platform-glfw`) as implementa e registra. `Key` e `MouseButton` são enums
   puros; traduzi-los para códigos GLFW é trabalho do módulo de plataforma.
6. **Um frame, um ciclo de vida.** `renderer.beginFrame()` → criar/preencher passes →
   `renderer.endFrame()` (executa os passes e apresenta). Os passes executam na ordem de
   criação, então um overlay (`Ui`) precisa começar depois do pass da cena. O `Renderer` impõe
   isso: chamar fora de ordem lança exceção.
7. **Posse de recursos.** O `Renderer` é dono dos recursos de GPU e os libera. `Assets` resolve
   caminhos (raiz, depois classpath) e cacheia texturas, então o mesmo arquivo é uma textura na
   GPU. Tipos de valor (`SpriteSheet`, `BitmapFont`) são dados sobre uma `Texture`, sem I/O.
8. **Convenções não vazam.** Convenções internas (clip space, projeção do motor, winding/UV do
   quad) ficam atrás do toolkit. O jogo vê Y para baixo e positivo, e rotação horária.
9. **Onde vai o código novo.**
   1. Precisa de API gráfica? → `renderer` (ou um futuro módulo de backend), nunca o `core`.
   2. Precisa de biblioteca de janela/SO? → um módulo `platform-*`, atrás dos contratos do `core`.
   3. É só 2D ou só 3D? → `toolkit2d` ou `engine3d`; nunca os dois.
   4. Precisa de serviço? → exponha por um módulo e use `require`; sem `new` escondido.
   5. É matemática/dado puro? → tipo de valor ou função `static` no módulo dono.

## Estrutura

Build Maven multi-módulo, com POM raiz (`java-game-engine`) que centraliza versões, plugins e os
profiles de natives.

| Módulo | Papel | Depende de |
|---|---|---|
| `java-game-engine-renderer` | Renderer modular (OpenGL): `Renderer`, `RenderModule`, `RenderPassDescriptor`, `RenderView`, `RenderCommand`, `Material`, `Mesh`, `Bounds`, `Shader`, `Texture` | core, LWJGL (OpenGL), JOML |
| `java-game-engine-core` | `Engine`/`Module`/`EngineContext`, `Timer`, contratos `Window` e `InputSystem` (interfaces), `Key`, `MouseButton` | nada |
| `java-game-engine-platform-glfw` | `GlfwModule` (janela + input via GLFW; registra `Window`/`InputSystem`) | core |
| `java-game-engine-assets` | `Assets` (resolução raiz → classpath, cache de texturas), `AssetsModule`, `TextureLoader` (STB, interno) | core, renderer |
| `java-game-engine-toolkit2d` | `Canvas`, `Ui` (overlay), `SpriteSheet`, `BitmapFont`, `SpriteRenderer`, `Toolkit2DModule` | core, renderer, assets |
| `java-game-engine-engine3d` | `Camera`, `TexturedModel`, `Meshes`, `Materials`, `ModelLoader`, `Shaders` (lit), `Projections`, `Ray`, `Picking`, `Engine3DModule` | core, renderer, assets |
| `java-game-engine-demos` | 5 exemplos + `Launcher` | platform-glfw, toolkit2d, engine3d (+ natives, em runtime) |

`toolkit2d` e `engine3d` não dependem um do outro; nenhum módulo da biblioteca depende de `platform-glfw`.

## Arquitetura

### `renderer`

- `Renderer` é a única porta de entrada: cria recursos (`createMesh/Texture/Shader/Material`) e
  passes. `createPass` cria **e enfileira** o passe no frame corrente; `beginFrame` limpa a fila,
  então os passes são criados a cada frame, em `onRender`. Não existe `submitPass`.
- Um frame é descrito por dados: `RenderPassDescriptor` (clear, depth, cull, blend, depth
  attachment) + `RenderView` (viewport, view, projection) + `RenderCommand`s (mesh, material,
  model matrix). O `Renderer` agrupa por material e executa.
- O renderer é **OpenGL-only, sem interface de backend** (decisão consciente, ver
  [Ideia futura](#ideia-futura-backend-de-render-como-módulo)). `OpenGLBackend` é um detalhe
  interno do `Renderer` (package-private) que cuida do estado de pass e do framebuffer de depth.
  `Mesh`, `Shader` e `Texture` chamam OpenGL diretamente.
- Hot path do draw call sem alocação: `Shader` reutiliza `FloatBuffer`s para matrizes, `Renderer`
  reutiliza a `Matrix3f` normal (`Matrix4f.normal`) e `Material` guarda uniforms já tipados
  (sem `instanceof`).
- `DirectionalLight`: volume ortográfico + matrizes de light-space para shadow mapping.
- `Material` tem setters encadeáveis (`setFloat(...).setTexture(...)`).

### `core`

- `engine.Engine` registra `Module`s, chama `onRegister` na ordem e `onTerminate` na ordem
  inversa. É biblioteca, não framework: quem tem o loop é o jogo.
- `engine.EngineContext` é o registro de serviços. `require(Class)` devolve o serviço ou lança
  `IllegalStateException` com mensagem clara; módulos que dependem de outros chamam isso na
  primeira linha de `onRegister`, então registrar na ordem errada falha ali. `resolve(Class)`
  continua existindo e devolve `null` (serviço opcional).
- `Window` e `InputSystem` são interfaces; quem as implementa e registra é um módulo de
  plataforma (hoje `platform-glfw`). `Key`/`MouseButton` são enums puros.
- `GlfwModule` (em `platform-glfw`): cria a janela com o contexto corrente e o input; **deve ser o
  primeiro**; em `onTerminate` destrói a janela e encerra o GLFW. Ainda pede um contexto OpenGL 3.3
  core (parametrizar isso só faz sentido com outro backend).
- `RenderModule` (no módulo `renderer`): `GL.createCapabilities()` e liga
  `Renderer.setPresent(window::swapBuffers)`.
- Ordem usual: Glfw → Render → Assets → Engine3D → Toolkit2D.
- `Renderer` tem estado de frame (`inFrame()`, `frameIndex()`): `beginFrame` duplo, `endFrame`
  sem `beginFrame` e `createPass` fora de um frame lançam `IllegalStateException`. `endFrame`
  executa os passes e apresenta (hook `Present`).
- `InputSystem.update()` amostra teclas/mouse e delega o polling a `Window.pollEvents()`.
- Não existe mais `Game`: cada demo escreve o `main` (módulos) e o loop.

### `assets`

- `Assets.texture(path)` resolve o caminho relativo primeiro na raiz de `AssetsModule(Path)` e
  depois no classpath; cacheia por caminho normalizado (o mesmo arquivo devolve a mesma
  `Texture`). `Assets.open(path)` abre um `InputStream` com a mesma resolução. Falha lança
  `UncheckedIOException` com a raiz e o caminho. Limpar o cache não libera GPU (as texturas são
  do `Renderer`).

### `toolkit2d`

- `Toolkit2DModule(virtualW, virtualH)` registra `Canvas`, `Ui` e o `SpriteRenderer`.
- Convenção exposta ao jogo: origem no canto superior esquerdo, Y para baixo (positivo), rotação
  em graus no sentido horário. A projeção interna continua "Y negativo para baixo"; a inversão
  é feita em `SpriteRenderer`, nunca vaza.
- `Canvas`: um pass que limpa a tela (`begin()`, `spr`, `text`, `textCentered`).
  `Ui`: overlay sem clear, para HUD por cima de uma cena (`begin()` depois do pass da cena).
- `SpriteRenderer` cacheia um `Material` por (textura, tile) e reaproveita um pool de
  `Matrix4f` por frame (`RenderCommand` guarda a matriz por referência até o `endFrame`).

### `engine3d`

- Componentes com dependência de serviço vêm do módulo: `Engine3DModule` registra `Meshes`,
  `Materials` e `ModelLoader`; o jogo faz `context.require(...)`.
- `ModelLoader` lê `models/<nome>.obj` e `textures/<nome>.png` pelo `Assets`; erro de leitura
  ou parse lança exceção (não devolve mais `null`).
- `TexturedModel.getModelMatrix()` é a **única fonte** da matriz de modelo: o que desenha (o
  `RenderCommand`) e o que testa picking usam a mesma. Rotação em graus, ordem XYZ, sentido
  **positivo** (antes era negativo; o `ModelViewer` foi ajustado, mesmo giro visual).
- **Picking** (`Ray`, `Picking`): funções `static` puras, no estilo de `Projections` (regra 3).
  `Mesh.bounds()` (renderer) guarda a caixa local calculada na criação; `Bounds` é um tipo de valor
  sem GPU. O `InputSystem` ganhou `isMouseButtonPressed` (borda de subida do botão). Ver
  [Picking](#picking).

### `demos`

| Demo | O que mostra | Verificado visualmente |
|---|---|---|
| `ModelViewer` | 3D em perspectiva + `Ui` | sim |
| `IsometricChess` | 3D isométrico + `Ui`, seleção de peças por picking | sim |
| `SnakeGame` | 2D com `Canvas` | comparado por pixel com o desenho anterior |
| `SpriteEditor` | 2D com `Canvas` (editor de pixel art) | comparado por pixel com o desenho anterior |
| `ShadowCubeDemo` | `Engine` + `Renderer` direto, shadow mapping em 2 passes, estilo funcional | não |

"Comparado por pixel" = na migração, o framebuffer do desenho novo foi comparado com o do código
anterior para a mesma cena (0 bytes diferentes), com harness descartável fora do repositório.
`scripts/run-demos.sh` abre cada demo por alguns segundos e só prova que ele fica de pé.

## Build e execução

Requisitos: JDK 17+. O Maven vem do wrapper do projeto (`./mvnw`, fixado em 3.10.0); o `mvn` do
sistema não é usado (o enforcer exige 3.9+). O `java` padrão desta máquina é o 11; use o 17:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

./mvnw -DskipTests install                                # builda os módulos (a partir da raiz)

# rodar um demo (usa -XstartOnFirstThread automaticamente no macOS)
./mvnw -pl java-game-engine-demos exec:exec               # Launcher
./mvnw -pl java-game-engine-demos exec:exec \
    -Dexec.mainClass=com.farias.rengine.examples.SnakeGame

# ou distribuição executável: jar fino + lib/ (profile opt-in)
./mvnw -Pdist -DskipTests package
cd java-game-engine-demos
java -XstartOnFirstThread -jar target/java-game-engine-demos-1.0.2-SNAPSHOT.jar   # tire a flag fora do macOS
```

Os demos leem `resources/...` por caminho relativo, então precisam rodar com o diretório de
trabalho em `java-game-engine-demos/` (o `exec:exec` já faz isso).

### Decisões de build

- **Maven fixado pelo wrapper** (`mvnw`, `.mvn/wrapper/`, tipo `only-script`, sem jar no repo):
  3.10.0, a última estável. Maven 4.0.0 ainda está em `rc-7` e os plugins core dele em `beta`;
  o build passa com `4.0.0-rc-7` também (testado), então migrar é trocar `distributionUrl` em
  `.mvn/wrapper/maven-wrapper.properties` quando o 4.0 sair em GA.
- **Plugins** todos na última estável, versionados no `pluginManagement` do POM raiz (compiler
  3.16.0, jar 3.5.1, dependency 3.11.0, surefire 3.6.0, resources 3.5.0, install 3.2.0, clean
  3.5.0, enforcer 3.6.3, exec 3.6.4, site/deploy fixados só por exigência do enforcer).
  `versions-maven-plugin` está disponível: `./mvnw versions:display-plugin-updates`.
- **Enforcer:** JDK 17+, Maven 3.9+, todo plugin com versão fixa (sem SNAPSHOT), sem versão
  duplicada de dependência e `dependencyConvergence`.
- **Jars reproduzíveis** via `project.build.outputTimestamp` (dois builds, mesmo hash).
- `.mvn/maven.config` desliga o progresso de download e imprime a versão do Maven.

- **Sem uber jar.** O `jar-with-dependencies` embutia os natives de uma única plataforma, então só
  rodava nela. A distribuição agora é opt-in (`-Pdist`): jar fino com `Class-Path: lib/...` e as
  dependências de runtime copiadas para `target/lib/`. Fica fora do `package` padrão porque só
  interessa a quem vai empacotar (e o `copy-dependencies` não funciona no build incremental da
  IDE, onde o `core` resolve para `target/classes`).
- **Natives por profile, não por módulo.** Os módulos `java-game-engine-deps-{macos,linux,win}`
  foram removidos (o do Linux estava em LWJGL 3.2.3, e o `renderer` dependia direto do de macOS).
  O POM raiz tem profiles ativados por SO/arquitetura que definem `lwjgl.natives`; só o `demos`
  declara os natives, como `runtime`. `renderer` e `core` dependem apenas das APIs.
- LWJGL 3.3.6, JOML 1.10.8 (há 3.4.3 e 1.10.9, não adotados), `maven.compiler.release=17`,
  encoding UTF-8. `lwjgl-assimp` e
  `lwjgl-openal` saíram: nada no código os usa.
- Os profiles de Linux/Windows foram escritos mas **nunca executados** (só macOS arm64 foi
  testado).

## Lições dos bugs já resolvidos

Coisas que custaram caro e valem lembrar ao mexer em renderização:

- **HiDPI:** o viewport vem de `Window.getFramebufferWidth/Height`, não do tamanho lógico da
  janela; em Retina o tamanho lógico mostra 1/4 da cena.
- **UI vs. mundo:** texto de UI tem que ser desenhado num passe próprio com projeção ortho2D
  (`Ui`/`Canvas`), senão sai deformado pela câmera 3D. A ortho2D interna vai de `0` a `-height`
  em Y; o `SpriteRenderer` inverte isso, então o jogo usa Y positivo para baixo.
- **Alpha no shader lit:** multiplicar só RGB pela luz. Multiplicar o alpha fazia fragmentos
  escuros parecerem transparentes com blending ligado.
- **Normal/winding de `Meshes.plane`:** a rotação usada para deitar o quad como piso virava a
  normal para baixo; normal e winding precisam ser corrigidos juntos para o culling funcionar.
- **Luz padrão** (`Materials.lit`) tem que apontar para o lado visível da câmera isométrica, senão
  o modelo fica quase preto.
- **Modelos de xadrez:** os `.obj` das peças têm normais e winding invertidos (≈75% das normais
  apontam para dentro, medido contra o centroide). Não é bug do motor. O contorno atual é
  `cullFace(false)` + `flipNormals=true` (custo: ~2x triângulos nas peças). Detalhes e como
  reexportar no Blender em `java-game-engine-demos/CHESS_MODELS.md`.

## Picking

Clicar em objetos 3D por raycasting, usado pelo `IsometricChess` para selecionar peças.

Fluxo: pixel lógico do mouse → `Picking.toNdc` → `Picking.ray(view, ndc)` → `Picking.rayBounds`
(objeto, contra o `Mesh.bounds()` e a matriz do `TexturedModel`) ou `Picking.rayPlaneY` +
`Ray.pointAt` (ponto num plano, como a casa do tabuleiro).

Decisões:

- Matemática pura em `engine3d` (`Picking`, `Ray`), no estilo de `Projections`; sem módulo nem
  serviço.
- Os limites (`Bounds`) vivem no `Mesh`, calculados na criação; um mesh sem vértices tem caixa
  vazia e nunca é atingido, em vez de falhar na criação.
- O raio sai da mesma view que desenha, e o objeto é testado com a mesma matriz de modelo
  (`TexturedModel.getModelMatrix()`), então a caixa acompanha qualquer animação do modelo.
- "O botão foi pressionado neste frame" faz parte do contrato `InputSystem`
  (`isMouseButtonPressed`).
- Coordenadas: o mouse chega em pixels lógicos; `toNdc` normaliza pelo tamanho lógico da janela,
  então a escala HiDPI não interfere.
- Hoje o teste é raio × caixa alinhada aos eixos (no espaço local do modelo). Interseção por
  triângulo ficaria como refinamento, se algum jogo precisar.

## Pendências

**Verificação**
- Olhar de verdade o **ShadowCubeDemo**: só houve smoke test (abre e roda sem exceção).
- A interação (mouse/teclado) dos demos migrados, exceto o clique/seleção do `IsometricChess`,
  não foi exercitada por um humano nesta migração; só o render foi comparado por pixel.
- Rodar um demo em Linux/Windows para validar os profiles de natives.
- O warning `Uniform 'uNormal' not found in shader program` aparece nos demos 2D e no
  ShadowCubeDemo. O `Renderer` seta `uNormal` em todo draw, e shaders que não o declaram (os 2D e
  o de shadow) geram o aviso. É ruído de log, não foi investigado a fundo.

**Design**
- Adiado de propósito (só se surgir outro backend de render): `Mesh`/`Shader`/`Texture` ainda são
  classes OpenGL concretas e o `Renderer` chama `bind`/uniforms/`draw` diretamente (handles
  opacos + SPI `RenderBackend` com `execute(frame)`); hints de contexto OpenGL em `GlfwWindow`;
  uniforms como dados e shaders portáveis (GLSL 330 em `toolkit2d`/`engine3d`).
- `Game` foi removido; cada demo monta o `Engine` e tem o próprio loop (concluído).
- `SnakeGame`: se uma seta for pressionada antes do primeiro movimento, `changeDirection` lê
  `direction == null` e lança NPE (pré-existente, não corrigido).
- Componentes com `new` solto: `Shaders.LIT_*` (shader lit) ainda é criado pelos demos; a ideia é
  `Materials` ser dono dele (muda assinaturas de `ModelLoader`/`Materials.lit`).
- Testes: `Engine`, `EngineContext`, `Material` e `RenderPassNode` são testáveis sem contexto GL.
  Ficam para outro momento (exige adicionar JUnit ao build).

**Modelos de xadrez:** se forem reexportados corretamente, reverter para `cullFace(true)` em
`IsometricChess` (construtor) e tirar o `flipNormals` da chamada de `ModelLoader.loadWavefrontOBJ` em
`ChessBoard.createAndPlace`.

**Ideia futura: backend de render como módulo**

Hoje não há necessidade de trocar a API gráfica (Vulkan, etc.), então a interface `RenderBackend`
foi removida. Se a necessidade surgir, a ideia é reaproveitar o mesmo design de módulos da engine:
o módulo `renderer` teria só classes comuns (`Renderer`, `Material`, `RenderPassDescriptor`, ...,
sem GL) e falaria com um backend registrado no `EngineContext` (por exemplo
`context.require(RenderBackend.class)`), sem saber se é OpenGL ou Vulkan. O backend também teria que
criar `Mesh`/`Shader`/`Texture`, que hoje chamam GL direto.
