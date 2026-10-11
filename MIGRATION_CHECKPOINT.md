# Checkpoint: engine modular (core sobre renderer)

Branch: `feature/migrate-core-to-render-module`.

Estado: a migração do `core` e dos demos para o renderer modular está feita e compila. O que
resta está em [Pendências](#pendências).

## Estrutura

Build Maven multi-módulo, com POM raiz (`java-game-engine`) que centraliza versões, plugins e os
profiles de natives.

| Módulo | Papel | Depende de |
|---|---|---|
| `java-game-engine-renderer` | Renderer modular: `Renderer`, `RenderPassDescriptor`, `RenderView`, `RenderCommand`, `Material`, `Mesh`, `Shader`, `Texture` | LWJGL (API), JOML |
| `java-game-engine-core` | `Engine`/`Module`, janela, input, helpers `gfx`, loaders | renderer |
| `java-game-engine-demos` | 5 exemplos + `Launcher` | core (+ natives, em runtime) |

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
- Módulos: `WindowModule` (GLFW + contexto GL; **deve ser o primeiro**), `InputModule`,
  `RenderModule` (depende de contexto GL corrente).
- `Game` é uma casca fina: monta os três módulos, resolve `Window`/`InputSystem`/`Renderer` e
  roda `onCreate` → (`onUpdate` → `onRender`)*. Sem estado de jogo nem passes implícitos.
- `InputSystem.update()` amostra teclas/mouse e delega o polling de eventos a
  `Window.pollEvents()`. Não há mais a interface `System`.
- `gfx`: helpers de instância, não singletons — `SpriteBatch2D`, `BitmapFont`, `TexturedModel`,
  `Materials`, `Meshes`, `Projections`, `Shaders`.
- `util`: `ModelLoader` (`.obj`, com `flipNormals` opcional) e `TextureLoader` (PNG via STB).

### `demos`

| Demo | O que mostra | Verificado visualmente |
|---|---|---|
| `ModelViewer` | 3D em perspectiva + passe de UI | sim |
| `IsometricChess` | 3D isométrico + UI | sim |
| `SnakeGame` | 2D, um passe | não |
| `SpriteEditor` | 2D, editor de pixel art | não |
| `ShadowCubeDemo` | sem `Game`; `Engine` + `Renderer` direto, shadow mapping em 2 passes, estilo funcional | não |

"Não" significa só smoke test (abre e roda alguns segundos sem exceção), sem alguém olhar a tela.

## Build e execução

Requisitos: JDK 17+. O Maven vem do wrapper do projeto (`./mvnw`, fixado em 3.10.0); o `mvn` do
sistema não é usado (o enforcer exige 3.9+). O `java` padrão desta máquina é o 11; use o 17:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

./mvnw -DskipTests install                                # builda os 3 módulos (a partir da raiz)

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
  (`onUIUpdate`), senão sai deformado pela câmera 3D. Na ortho2D do `BitmapFont`, Y vai de `0` a
  `-height` e `drawText` centraliza o texto em X.
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

## Pendências

**Verificação**
- Rodar e olhar de verdade **SnakeGame**, **SpriteEditor** e **ShadowCubeDemo**. Depois da
  otimização de uniforms/matrizes só houve smoke test (abre e roda sem exceção) dos demos.
- Rodar um demo em Linux/Windows para validar os profiles de natives.
- O warning `Uniform 'uNormal' not found in shader program` aparece nos demos 2D e no
  ShadowCubeDemo. O `Renderer` seta `uNormal` em todo draw, e shaders que não o declaram (os 2D e
  o de shadow) geram o aviso. É ruído de log, não foi investigado a fundo.

**Design**
- `Game` fixa os três módulos (`Window`, `Input`, `Render`) e não é composável. Decisão: não vai
  mudar, a classe será descontinuada; quem precisar de outra composição monta o `Engine` direto
  (como o `ShadowCubeDemo`).
- Testes: `Engine`, `EngineContext`, `Material` e `RenderPassNode` são testáveis sem contexto GL.
  Ficam para outro momento (exige adicionar JUnit ao build).

**Modelos de xadrez:** se forem reexportados corretamente, reverter para `cullFace(true)` em
`IsometricChess.onCreate` e tirar o `flipNormals` da chamada de `ModelLoader.loadWavefrontOBJ` em
`ChessBoard.createAndPlace`.

**Ideia futura: backend de render como módulo**

Hoje não há necessidade de trocar a API gráfica (Vulkan, etc.), então a interface `RenderBackend`
foi removida. Se a necessidade surgir, a ideia é reaproveitar o mesmo design de módulos da engine:
o módulo `renderer` teria só classes comuns (`Renderer`, `Material`, `RenderPassDescriptor`, ...,
sem GL) e falaria com um backend registrado no `EngineContext` (por exemplo
`context.require(RenderBackend.class)`), sem saber se é OpenGL ou Vulkan. O backend também teria que
criar `Mesh`/`Shader`/`Texture`, que hoje chamam GL direto.
