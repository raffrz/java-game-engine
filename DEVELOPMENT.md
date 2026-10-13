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
   jogo não faz `new` desses componentes, e o código garante isso: os construtores são
   package-private. Quando o jogo precisa de várias instâncias com parâmetros próprios, o serviço
   é uma fábrica (`Toolkit2D.canvas(w, h)`, `Shadows.create(...)`). `static` fica para funções
   **puras** (sem serviço, GPU ou disco): `Projections`, `ShapeData`, `Picking`. Um `static` que carrega arquivo ou
   cria recurso de GPU esconde a dependência e impede cache.
4. **Direção de dependência.** `core` não depende de nada (sem gráficos, sem biblioteca de
   janela, sem LWJGL). Módulos da biblioteca nunca dependem de `platform-*`; só o jogo (ou um
   demo) depende. `toolkit2d` e `toolkit3d` são irmãos e nunca dependem um do outro: um jogo 3D
   que quer HUD registra o `Toolkit2DModule` e usa `Ui`. Nada específico de uma API gráfica entra
   no `core` nem na API pública dos toolkits.
5. **Plataforma é contrato.** `Window` e `Input` são interfaces no `core` (pacote `platform`). Um módulo de
   plataforma (hoje `platform-glfw`) as implementa e registra. `Key` e `MouseButton` são enums
   puros; traduzi-los para códigos GLFW é trabalho do módulo de plataforma.
6. **Um frame, um ciclo de vida.** `window.pollEvents()` → `input.update()` →
   `renderer.beginFrame()` → criar/preencher passes → `renderer.endFrame()` (executa os passes e
   apresenta). Nada disso é chamado por baixo dos panos: o `Input` não processa eventos. Os passes executam na ordem de
   criação, então um overlay (`Ui`) precisa começar depois do pass da cena. O `Renderer` impõe
   isso: chamar fora de ordem lança exceção.
7. **Posse de recursos.** O `Renderer` é dono dos recursos de GPU: só ele os cria (`Mesh`,
   `Texture` e `Shader` não têm construtor público) e os libera, no `dispose` ou antes com
   `destroy(recurso)`. `Assets` resolve caminhos (raiz, depois classpath) e cacheia texturas, e
   `Models` cacheia meshes, então o mesmo arquivo é um recurso só na GPU. Tipos de valor (`SpriteSheet`, `BitmapFont`) são dados sobre uma `Texture`, sem I/O.
8. **Convenções não vazam.** Convenções internas (clip space, projeção do motor, winding/UV do
   quad) ficam atrás do toolkit. O jogo vê Y para baixo e positivo, e rotação horária.
9. **Onde vai o código novo.**
   1. Precisa de API gráfica? → `renderer` (ou um futuro módulo de backend), nunca o `core`.
   2. Precisa de biblioteca de janela/SO? → um módulo `platform-*`, atrás dos contratos do `core`.
   3. É só 2D ou só 3D? → `toolkit2d` ou `toolkit3d`; nunca os dois.
   4. Precisa de serviço? → exponha por um módulo e use `require`; sem `new` escondido.
   5. É matemática/dado puro? → tipo de valor ou função `static` no módulo dono.

## Estrutura

Build Maven multi-módulo, com POM raiz (`java-game-engine`) que centraliza versões, plugins e os profiles de
natives. Artefato e diretório são `java-game-engine-<módulo>`; o pacote é `com.farias.jengine.<módulo>`.

| Módulo | Papel | Depende de |
|---|---|---|
| `java-game-engine-core` | `core`: `Engine`/`Module`/`EngineContext`, `Timer`. `platform`: contratos `Window` e `Input` (interfaces), `Key`, `MouseButton` | nada |
| `java-game-engine-renderer` | Renderer modular (OpenGL): `Renderer`, `RendererModule`, `RenderPassDescriptor`, `RenderPass`, `RenderView`, `RenderCommand`, `Material`, `Mesh`, `Bounds`, `Shader`, `Texture` | core, LWJGL (OpenGL), JOML |
| `java-game-engine-platform-glfw` | `GlfwPlatformModule` (janela + input via GLFW) | core |
| `java-game-engine-assets` | `Assets` (resolução raiz → classpath, cache de texturas), `AssetsModule`, `TextureLoader` (STB, interno) | core, renderer |
| `java-game-engine-toolkit2d` | `Toolkit2D` (fábrica de `Canvas` e `Ui`), `SpriteSheet`, `BitmapFont`, `SpriteRenderer`, `Toolkit2DModule` | core, renderer, assets |
| `java-game-engine-toolkit3d` | `Shapes`, `ShapeData`, `LitMaterials`, `LitMaterial`, `Models`, `TexturedModel`, `Shadows`, `ShadowMap`, `DirectionalLight`, `Camera`, `IsometricCamera`, `Projections`, `Ray`, `Picking`, `Shaders`, `Toolkit3DModule` | core, renderer, assets |
| `java-game-engine-demos` | 5 exemplos + `Launcher` | todos os anteriores (+ natives, em runtime) |

`toolkit2d` e `toolkit3d` não dependem um do outro; nenhum módulo da biblioteca depende de
`platform-glfw`. Cada POM declara tudo que o módulo usa direto (conferir com
`./mvnw dependency:analyze`; os natives do `demos` aparecem como "unused", é esperado).

Serviços registrados (o que `context.require(...)` devolve):

| Módulo | Serviços |
|---|---|
| `GlfwPlatformModule` | `Window`, `Input` |
| `RendererModule` | `Renderer` |
| `AssetsModule` | `Assets` |
| `Toolkit2DModule` | `Toolkit2D`, `SpriteRenderer` (baixo nível) |
| `Toolkit3DModule` | `Shapes`, `LitMaterials`, `Models`, `Shadows` |

Registrar o mesmo tipo duas vezes lança exceção (`EngineContext.registerService`).

## Arquitetura

### `renderer`

- `Renderer` é a única porta de entrada: cria recursos (`createMesh/Texture/Shader/Material`) e
  passes. `createPass` cria **e enfileira** o passe no frame corrente; `beginFrame` limpa a fila,
  então os passes são criados a cada frame. Não existe `submitPass`. Um passe sem comandos ainda
  executa (o clear dele faz parte do frame).
- Um frame é descrito por dados: `RenderPassDescriptor` (clear, depth, cull, blend, depth
  attachment) + `RenderView` (viewport, view, projection) + `RenderCommand`s (mesh, material,
  model matrix). O `Renderer` executa na ordem de submissão e só reaplica material/uniforms quando
  o material muda entre comandos consecutivos.
- O renderer é **OpenGL-only, sem interface de backend** (decisão consciente, ver
  [Ideia futura](#ideia-futura-backend-de-render-como-módulo)). `OpenGLBackend` é um detalhe
  interno do `Renderer` (package-private) que cuida do estado de pass e do framebuffer de depth.
  `Mesh`, `Shader` e `Texture` chamam OpenGL diretamente.
- Hot path do draw call sem alocação: `Shader` reutiliza `FloatBuffer`s para matrizes, `Renderer`
  reutiliza a `Matrix3f` normal (`Matrix4f.normal`) e `Material` guarda uniforms já tipados
  (sem `instanceof`).
- `Material` tem setters encadeáveis (`setFloat(...).setTexture(...)`).

### `core`

- `core.Engine` registra `Module`s, chama `onStart` na ordem e `onTerminate` na ordem inversa, só
  dos módulos que iniciaram. Se um `onStart` falha, `start()` encerra os anteriores antes de
  propagar a exceção. É biblioteca, não framework: quem tem o loop é o jogo.
- `core.EngineContext` é o registro de serviços. `require(Class)` devolve o serviço ou lança
  `IllegalStateException` com mensagem clara; módulos que dependem de outros chamam isso na
  primeira linha de `onStart`, então registrar na ordem errada falha ali. `resolve(Class)`
  continua existindo e devolve `null` (serviço opcional).
- `Window` e `Input` são interfaces; quem as implementa e registra é um módulo de plataforma (hoje
  `platform-glfw`). `Key`/`MouseButton` são enums puros. `Window.getWidth/getHeight` devolvem o
  tamanho atual (a janela é redimensionável) e `setVSync` liga/desliga o vsync (ligado por padrão).
- `GlfwPlatformModule` (em `platform-glfw`): cria a janela com o contexto corrente e o input;
  **deve ser o primeiro**; em `onTerminate` destrói a janela e encerra o GLFW. Ainda pede um
  contexto OpenGL 3.3 core (parametrizar isso só faz sentido com outro backend).
- `RendererModule` (no módulo `renderer`): `GL.createCapabilities()` e liga
  `Renderer.setPresent(window::swapBuffers)`.
- Ordem usual: GlfwPlatform → Renderer → Assets → Toolkit3D → Toolkit2D.
- `Renderer` tem estado de frame (`inFrame()`, `frameIndex()`): `beginFrame` duplo, `endFrame`
  sem `beginFrame` e `createPass` fora de um frame lançam `IllegalStateException`. `endFrame`
  executa os passes e apresenta (hook `Present`).
- `Input.update()` só amostra teclas/mouse (guarda o frame atual e o anterior, de onde saem
  `isKeyPressed`/`isKeyReleased`); o jogo chama `window.pollEvents()` antes.
- Não existe mais `Game`: cada demo escreve o `main` (módulos) e o loop.

### `assets`

- `Assets.texture(path)` resolve o caminho relativo primeiro na raiz de `AssetsModule(Path)` e
  depois no classpath; cacheia por caminho normalizado (o mesmo arquivo devolve a mesma
  `Texture`). `Assets.open(path)` abre um `InputStream` com a mesma resolução. Falha lança
  `UncheckedIOException` com a raiz e o caminho. Limpar o cache não libera GPU (as texturas são
  do `Renderer`).

### `toolkit2d`

- `Toolkit2DModule` registra `Toolkit2D` e o `SpriteRenderer`. `Toolkit2D` é uma fábrica:
  `canvas(w, h)` e `ui(w, h)` criam superfícies com tamanho virtual próprio (um jogo 128×128 pode
  ter HUD em outra resolução). O jogo as cria uma vez e guarda.
- Convenção exposta ao jogo: origem no canto superior esquerdo, Y para baixo (positivo), rotação
  em graus no sentido horário. A projeção interna continua "Y negativo para baixo"; a inversão
  é feita em `SpriteRenderer`, nunca vaza. `toVirtualX/Y` converte a posição do mouse (pixels
  lógicos da janela) para unidades virtuais.
- `Canvas`: um pass que limpa a tela (`begin()`, `spr`, `text`, `textCentered`).
  `Ui`: overlay sem clear, para HUD por cima de uma cena (`begin()` depois do pass da cena).
  Os dois herdam de `Layer2D` (abstrata, `sealed`, package-private), que tem o pass, o texto e a
  conversão de coordenadas; cada um só acrescenta o descritor e os nomes da própria API. Por isso
  o módulo é um pacote só: `sealed` e construtores package-private exigem o mesmo pacote.
- `SpriteRenderer` cacheia um `Material` por (textura, tile) e reaproveita um pool de
  `Matrix4f` por frame (`RenderCommand` guarda a matriz por referência até o `endFrame`).

### `toolkit3d`

- Componentes com dependência de serviço vêm do módulo: `Toolkit3DModule` registra `Shapes`,
  `LitMaterials`, `Models` e `Shadows`; o jogo faz `context.require(...)`.
- **Ferramentas, não camadas:** o que segue são peças que o jogo chama e combina com o `Renderer`
  (o jogo continua dono dos passes e da ordem); nenhuma delas chama o jogo de volta nem esconde o
  nível baixo.
- `Shapes` cria meshes das formas básicas (`plane`, `cube`, `floor`); a geometria em si é
  `ShapeData`, funções puras, para quem cria o mesh direto no `Renderer` (`ShadowCubeDemo`).
- `Models` lê Wavefront OBJ pelo `Assets`, com caminhos explícitos (sem convenção de pastas):
  `load(obj, textura[, flipNormals])` devolve um `TexturedModel` com `LitMaterial` novo, e
  `mesh(obj[, flipNormals])` só o mesh, para outro tipo de material. O mesh é cacheado por
  arquivo, então vários modelos compartilham um upload. Faces com mais de 3 vértices são
  trianguladas em leque. Erro de leitura ou parse lança exceção.
- **`IsometricCamera`**: câmera ortográfica isométrica (alvo, afastamento, zoom com mínimo).
  `pan(direita, cima)` move o alvo (o jogo não precisa conhecer a diagonal da câmera),
  `view(window)` devolve o `RenderView` do passe 3D e `rayAt(mouseX, mouseY, window)` o raio sob o
  mouse, da mesma matriz, então o que é desenhado e o que é clicado coincidem.
- **`LitMaterial`** (`litMaterials.create(texture)`): é um `Material` com métodos tipados no lugar
  dos nomes de uniform: `light`, `glossy` (brilho especular branco, estilo plástico) e
  `receiving(ShadowMap)`. Por padrão é fosco, não recebe sombra e a luz vem de (1, 1, 1). Usa o
  shader padrão (`Shaders.STANDARD_*`), criado sob demanda e pertencente ao `Renderer`. É o único
  material lit: o par `LIT_*` e as sobrecargas que recebiam um `Shader` foram removidos.
- **`ShadowMap`** (`shadows.create(light, size[, bias])`): o mapa de profundidade, a luz, o
  descritor e a view do passe da luz, e `caster(model)` para o comando de cada objeto que projeta.
  O jogo cria o passe de sombra antes do passe da cena e submete os casters; quem recebe sombra
  usa `LitMaterial.receiving`. O bias é fixo por mapa, porque `receiving` o copia para o material.
- `model.lit()` devolve o `LitMaterial` de um modelo carregado por `Models.load`, para refiná-lo.
- `TexturedModel.command()` é o `RenderCommand` do modelo com a transformação atual (matriz nova a
  cada chamada, então pode ser guardado até o `endFrame`).
- `DirectionalLight` (volume ortográfico + matrizes de light-space): `covering(direção, centro,
  raio)` cria a luz de sombra a partir da esfera que deve ser coberta, derivando distância,
  extensão e planos near/far. Aceita luz a pino (troca o vetor "para cima" do `lookAt`).
- `TexturedModel.getModelMatrix()` é a **única fonte** da matriz de modelo: o que desenha (o
  `RenderCommand`) e o que testa picking usam a mesma. Rotação em graus, ordem XYZ, sentido
  **positivo**. A matriz é T·R·S (escala antes de girar), então escala não uniforme não deforma o
  modelo girado; o `SpriteRenderer` segue a mesma ordem.
- **Picking** (`Ray`, `Picking`): funções `static` puras, no estilo de `Projections` (regra 3).
  `Mesh.bounds()` (renderer) guarda a caixa local calculada na criação; `Bounds` é um tipo de valor
  sem GPU. O `Input` tem `isMouseButtonPressed` (borda de subida do botão). Ver
  [Picking](#picking).

### `demos`

| Demo | O que mostra | Verificado visualmente |
|---|---|---|
| `ModelViewer` | 3D em perspectiva + `Ui` | sim |
| `IsometricChess` | 3D isométrico + `Ui`, seleção de peças por picking, sombras (shadow mapping); pacote `examples.chess`: estado do jogo (`ChessGame`, `ChessBoard`, `ChessPiece`) separado da apresentação (`ChessScene`, `ChessRenderer`) | sim |
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
./mvnw -pl java-game-engine-demos exec:exec                        # Launcher
./mvnw -pl java-game-engine-demos exec:exec \
    -Dexec.mainClass=com.farias.jengine.examples.SnakeGame

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
- **Normal/winding de `Shapes.plane`:** a rotação usada para deitar o quad como piso virava a
  normal para baixo; normal e winding precisam ser corrigidos juntos para o culling funcionar.
- **Luz padrão** (`LitMaterial`) tem que apontar para o lado visível da câmera isométrica, senão
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

- Matemática pura em `toolkit3d` (`Picking`, `Ray`), no estilo de `Projections`; sem módulo nem
  serviço.
- Os limites (`Bounds`) vivem no `Mesh`, calculados na criação; um mesh sem vértices tem caixa
  vazia e nunca é atingido, em vez de falhar na criação.
- O raio sai da mesma view que desenha, e o objeto é testado com a mesma matriz de modelo
  (`TexturedModel.getModelMatrix()`), então a caixa acompanha qualquer animação do modelo.
- "O botão foi pressionado neste frame" faz parte do contrato `Input`
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
  uniforms como dados e shaders portáveis (GLSL 330 em `toolkit2d`/`toolkit3d`).
- `Game` foi removido; cada demo monta o `Engine` e tem o próprio loop (concluído).
- **Apresentação por callback:** `Renderer.endFrame()` ainda apresenta via
  `setPresent(window::swapBuffers)`, ligado pelo `RendererModule`. É o último ponto em que a
  engine chama algo por conta própria; a alternativa é o jogo chamar `window.swapBuffers()` depois
  do `endFrame()`, o que também tira o `Window` do `RendererModule`. Em aberto.
- O teste por pixel dos demos 2D (seção Demos) é anterior à mudança da matriz de sprite para
  T·R·S e à remoção do `flip()` em `Texture`; as duas não mudam o resultado para os demos atuais
  (sprites quadrados, buffer já na posição 0), mas a comparação não foi refeita.
- Testes: `Engine`, `EngineContext`, `Material` e `RenderPass` são testáveis sem contexto GL.
  Ficam para outro momento (exige adicionar JUnit ao build).

**Modelos de xadrez:** se forem reexportados corretamente, reverter para `cullFace(true)` em
`ChessRenderer` e tirar o `flipNormals` da chamada de `models.load` em
`ChessScene.createPieceView`.

**Ideia futura: backend de render como módulo**

Hoje não há necessidade de trocar a API gráfica (Vulkan, etc.), então a interface `RenderBackend`
foi removida. Se a necessidade surgir, a ideia é reaproveitar o mesmo design de módulos da engine:
o módulo `renderer` teria só classes comuns (`Renderer`, `Material`, `RenderPassDescriptor`, ...,
sem GL) e falaria com um backend registrado no `EngineContext` (por exemplo
`context.require(RenderBackend.class)`), sem saber se é OpenGL ou Vulkan. O backend também teria que
criar `Mesh`/`Shader`/`Texture`, que hoje chamam GL direto.
