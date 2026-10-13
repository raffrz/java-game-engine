# Java Game Engine

A small, modular game engine for Java built on [LWJGL](https://www.lwjgl.org/) (OpenGL + GLFW).

It is a **library, not a framework**: your game owns `main` and the game loop, and picks the
pieces it needs by registering modules. There is no base class to extend.

> Status: in development. Tested on macOS (arm64). The Linux/Windows native profiles are written
> but have not been exercised.

## What you get

- Window and input (GLFW), behind small interfaces.
- A modular, pass-based OpenGL renderer: render passes, materials, depth/shadow mapping.
- A 2D toolkit: sprite sheets, bitmap text, a `Canvas` for 2D scenes and a `Ui` overlay for HUDs.
- A 3D toolkit: OBJ models, lit materials, shadow maps, cameras, projections and picking.
- An asset service with a texture cache.
- A tiny kernel (`Engine`, `Module`, `EngineContext`) that wires everything together.

## Quick start

Requires **JDK 17+**. Maven is provided by the wrapper (`./mvnw`); LWJGL natives are picked
from your OS/architecture automatically.

```bash
./mvnw -DskipTests install                      # build and install all modules

# run the demo launcher (uses -XstartOnFirstThread on macOS automatically)
./mvnw -pl rengine-demos exec:exec

# or a specific demo
./mvnw -pl rengine-demos exec:exec \
    -Dexec.mainClass=com.farias.rengine.examples.SnakeGame
```

Demos read `resources/...` relative to the working directory, so they must run from
`rengine-demos/` (`exec:exec` already does that).

To smoke-run every demo for a few seconds each (it only checks that they open and stay up):

```bash
scripts/run-demos.sh          # 5 s per demo
scripts/run-demos.sh -t 10 SnakeGame chess.IsometricChess
```

| Demo | Shows |
|---|---|
| `SnakeGame` | 2D with `Canvas` |
| `SpriteEditor` | 2D pixel-art editor with `Canvas` and mouse input |
| `ModelViewer` | 3D perspective scene + `Ui` overlay |
| `chess.IsometricChess` | 3D isometric scene, picking, shadows + `Ui` overlay |
| `ShadowCubeDemo` | `Engine` + `Renderer` used directly, two-pass shadow mapping |

## A minimal game

```java
public static void main(String[] args) {
    Engine engine = new Engine()
            .registerModule(new GlfwPlatformModule(640, 480, "Hello"))  // Window + Input
            .registerModule(new RendererModule())                       // Renderer (OpenGL)
            .registerModule(new AssetsModule(Path.of("resources")))     // Assets
            .registerModule(new Toolkit2DModule());                     // Toolkit2D (Canvas, Ui)

    EngineContext context = engine.start();          // cleans up after itself if a module fails
    try {
        Window window = context.require(Window.class);
        Input input = context.require(Input.class);
        Renderer renderer = context.require(Renderer.class);
        Canvas canvas = context.require(Toolkit2D.class).canvas(320, 240);

        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            window.pollEvents();
            input.update();                  // samples keys/mouse for this frame
            timer.update();
            // update(timer.deltaTime());

            renderer.beginFrame();
            canvas.begin();
            canvas.textCentered(canvas.defaultFont(), "Hello", 160, 100);
            renderer.endFrame();             // executes the passes and presents
        }
    } finally {
        engine.terminate();                  // reverse order of registration
    }
}
```

Coordinates in `Canvas`/`Ui` are virtual units (the size given to `Toolkit2D.canvas`/`ui`; each
surface has its own), origin at the **top-left**, **Y grows downward**, rotation in degrees
**clockwise**. `canvas.toVirtualX/Y` converts a mouse position to those units.

VSync is on by default; `window.setVSync(false)` uncaps the frame rate.

## Modules

| Module | Responsibility | Depends on |
|---|---|---|
| `core` | Kernel: `Engine`, `Module`, `EngineContext`, `Timer`; contracts `Window`, `Input`, `Key`, `MouseButton` | nothing |
| `platform-glfw` | `GlfwPlatformModule`: window + input through GLFW | core |
| `renderer` | `Renderer`, `RendererModule`, passes, materials, meshes, shaders, textures (OpenGL) | core |
| `assets` | `Assets`, `AssetsModule`: path resolution and texture cache | core, renderer |
| `toolkit2d` | `Toolkit2D` (creates `Canvas` and `Ui`), `SpriteSheet`, `BitmapFont`, `Toolkit2DModule` | core, renderer, assets |
| `toolkit3d` | `Shapes`, `LitMaterials`, `Models`, `Shadows`, cameras, picking, `Toolkit3DModule` | core, renderer, assets |
| `demos` | Example games | all of the above |

Maven artifacts and directories are `rengine-<module>` (group `com.farias.rengine`); each module's
code lives in the package `com.farias.rengine.<module>` (`platform-glfw` in `platform.glfw`, and the
`Window`/`Input` contracts of `core` in `platform`).

Services each module registers (what `context.require(...)` can return):

| Module class | Services |
|---|---|
| `GlfwPlatformModule` | `Window`, `Input` |
| `RendererModule` | `Renderer` |
| `AssetsModule` | `Assets` |
| `Toolkit2DModule` | `Toolkit2D`, `SpriteRenderer` (low level) |
| `Toolkit3DModule` | `Shapes`, `LitMaterials`, `Models`, `Shadows` |

```mermaid
graph BT
    core
    platform-glfw --> core
    renderer --> core
    assets --> core
    assets --> renderer
    toolkit2d --> core
    toolkit2d --> renderer
    toolkit2d --> assets
    toolkit3d --> core
    toolkit3d --> renderer
    toolkit3d --> assets
    demos --> platform-glfw
    demos --> toolkit2d
    demos --> toolkit3d
```

## Architecture rules

These rules are the architectural proposal of the engine. Breaking one should be a deliberate
decision, not an accident. The reasoning behind each rule is in
[DEVELOPMENT.md](DEVELOPMENT.md#regras-de-arquitetura-o-porquê).

1. **Library, not framework:** the game owns `main` and the loop; there is no base class to extend.
2. **Composition through modules:** the game registers the modules it needs; services come from
   `EngineContext.require(...)`.
3. **Components with dependencies come from modules:** the game does not `new` them (their
   constructors are not public); `static` is for pure functions only.
4. **Dependency direction:** `core` depends on nothing; library modules never depend on
   `platform-*`; `toolkit2d` and `toolkit3d` never depend on each other.
5. **Platform is a contract:** `Window` and `Input` are interfaces in `core`, implemented by
   `platform-*` modules.
6. **One frame lifecycle:** `pollEvents` → `input.update` → `beginFrame` → passes → `endFrame`;
   overlays are begun after the scene.
7. **Resource ownership:** the `Renderer` owns GPU resources (only it can create them, and
   `destroy` frees one early); `Assets` caches textures and `Models` caches meshes.
8. **Conventions do not leak:** clip space, UV and winding stay inside the toolkits.
9. **Where new code goes:** graphics API → `renderer`; OS library → `platform-*`; 2D-only or
   3D-only → its toolkit; anything with a service → a module.

## Known limitations (deliberate, not forgotten)
- The renderer is **OpenGL-only**: `Mesh`, `Shader` and `Texture` are concrete OpenGL classes
  and the `Renderer` binds programs, sets uniforms by name and draws directly. A second backend
  would need opaque resource handles plus a `RenderBackend` SPI that executes a whole frame
  (`execute(frame)`); that is intentionally not built until it is actually needed.
- The OpenGL 3.3 core context request still lives in `platform-glfw`.
- Shader sources are GLSL 330 strings inside `toolkit2d` and `toolkit3d`.
- `Matrix4f` (JOML) appears in the renderer API.

## Project notes

- `DEVELOPMENT.md`: module details, build decisions, lessons from past bugs and open items (pt-BR).
- `rengine-demos/CHESS_MODELS.md`: why the chess models need `flipNormals`.
