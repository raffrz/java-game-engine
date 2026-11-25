# R-Engine 2.0 - Roadmap de Transição para Arquitetura Funcional

## Objetivo

Transformar a engine de um modelo orientado a framework:

```java
GameEngine.initGame(game);

class MyGame extends Game {
    onCreate();
    onUpdate();
    onRender();
}
```

para um modelo orientado a biblioteca:

```java
Engine engine = new Engine();

while(engine.running()) {

    float dt = engine.deltaTime();

    update(dt);

    render();
}
```

Onde o usuário possui o game loop e a engine fornece apenas serviços.

---

# Princípios

## 1. O jogo controla o loop

A engine não deve mais executar:

```java
game.run();
```

A engine deve fornecer:

```java
engine.running();
engine.deltaTime();
engine.beginFrame();
engine.endFrame();
```

O usuário controla completamente a execução.

---

## 2. Eliminar a classe Game

Remover gradualmente:

```java
abstract class Game
```

e seus callbacks:

```java
onCreate()
onUpdate()
onGfxUpdate()
onUIUpdate()
```

Substituir por funções comuns:

```java
init();

while(engine.running()) {

    update(dt);

    render();
}
```

---

## 3. Transformar GameEngine em Engine

Evitar singleton global.

Hoje:

```java
GameEngine.drawSprite(...)
```

Objetivo:

```java
engine.drawSprite(...)
```

ou

```java
renderer.drawSprite(...)
```

A engine passa a ser um objeto de contexto.

---

# Nova Estrutura

## Engine

Responsável por:

* janela
* delta time
* loop auxiliar
* input
* renderer
* assets

Exemplo:

```java
Engine engine = new Engine(window);
```

---

## Renderer

Responsável por:

* projeções
* câmeras
* estados OpenGL
* draw calls

Exemplo:

```java
Renderer renderer =
    engine.renderer();
```

---

## Assets

Responsável por:

```java
loadModel(...)
loadTexture(...)
loadShader(...)
```

---

# Eliminar GameState

Remover:

```java
SCENE
UI
```

e substituir por Render Passes.

---

# Introduzir Render Modes

Criar:

```java
enum RenderMode {

    WORLD_3D,
    WORLD_2D,
    UI,
    DEBUG
}
```

---

Cada modo configura automaticamente:

## WORLD_3D

```text
Depth Test = ON
Cull Face = ON
Perspective Projection
World Camera
```

---

## WORLD_2D

```text
Depth Test = OFF
Cull Face = OFF
Orthographic Projection
2D Camera
```

---

## UI

```text
Depth Test = OFF
Cull Face = OFF
Blend = ON
UI Projection
UI Camera
```

---

## DEBUG

```text
Depth Test = OFF
Blend = ON
```

---

# API Desejada

Em vez de:

```java
GameEngine.perspectiveMode(...);
GameEngine.orthographicMode(...);
GameEngine.setState(...);
```

Usar:

```java
renderer.begin3D();

renderer.begin2D();

renderer.beginUI();
```

ou

```java
renderer.begin(RenderMode.WORLD_3D);
```

---

# Loop Final Desejado

## Exemplo mínimo

```java
Engine engine =
    new Engine(960, 960);

init();

while(engine.running()) {

    float dt =
        engine.deltaTime();

    update(dt);

    engine.beginFrame();

    render();

    engine.endFrame();
}
```

---

## Exemplo completo

```java
while(engine.running()) {

    float dt =
        engine.deltaTime();

    update(dt);

    engine.beginFrame();

    renderer.begin3D();

    drawWorld();

    renderer.begin2D();

    drawSprites();

    renderer.beginUI();

    drawHUD();

    engine.endFrame();
}
```

---

# Objetivo de Design

O usuário nunca deve precisar chamar:

```java
glEnable(...)
glDisable(...)
```

Nem saber:

```java
depth test
culling
projection matrix
blend state
```

Esses detalhes devem estar encapsulados nos Render Modes.

---

# Ordem de Implementação

## Fase 1

Manter a engine atual funcionando.

Adicionar:

```java
renderer.begin3D();
renderer.beginUI();
```

Internamente ainda usando GameState.

Objetivo:
não quebrar compatibilidade.

---

## Fase 2

Mover lógica de:

```java
orthographicMode()
perspectiveMode()
```

para dentro dos Render Modes.

Objetivo:
o usuário não manipula projeções diretamente.

---

## Fase 3

Remover dependência de:

```java
Game
```

Permitir:

```java
while(engine.running())
```

Objetivo:
usuário controla o loop.

---

## Fase 4

Remover singleton:

```java
GameEngine
```

Transformar em:

```java
Engine
Renderer
Assets
Input
```

Objetivo:
arquitetura orientada a contexto.

---

# Estado Final

Arquitetura inspirada em:

* Raylib
* SDL
* Pico-8

Filosofia:

```text
Engine = biblioteca

Game = código do usuário

Loop = do usuário

Renderização = Render Passes

OpenGL = detalhe interno
```
