# Java Game Engine
An OpenGL game engine made in Java using LWJGL library.

## Features
 - Sprite drawing;
 - Texture drawing;
 - Text drawing;
 - Projections;
 - Window management;
 - Input Handling;
 - Modular renderer (render passes, materials, shadow mapping);
 - Engine/Module/service-context composition.

## Trivia
This engine is still in development.
At the momment I'm creating a simple spaceship game to figure out what features should be implemented next.

## Build & run
Requires JDK 17+ (Maven is provided by the wrapper, `./mvnw`). LWJGL natives are picked automatically from your OS/arch.

```bash
./mvnw -DskipTests install
./mvnw -pl java-game-engine-demos exec:exec                      # demo launcher
./mvnw -pl java-game-engine-demos exec:exec \
    -Dexec.mainClass=com.farias.rengine.examples.SnakeGame    # a specific demo
```

See `MIGRATION_CHECKPOINT.md` for architecture notes and current status.
