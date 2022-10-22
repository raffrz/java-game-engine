package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import static com.farias.rengine.GameEngine.*;
import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;

public class SnakeGame extends Game {

    public static final int WINDOW_WIDTH = 1024;
    public static final int  WINDOW_HEIGHT= 1024;
    public static final int GRID_WIDTH = 128;
    public static final int GRID_HEIGTH = 128;
    public static final int GRID_CELL_SIZE = 8;
    public static final int FONT_SIZE = 4;
    public static final int BACKGROUND_COLOR = 5;

    Snake snake;
    List<Apple> apples = new ArrayList<>();
    int score = 0;
    Sprite pallete;

    public SnakeGame(Window window) {
        super("Snake", window);
    }

    public static void launch(String[] args) {
        Window window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT);
        long windowId = window.create();
        SnakeGame game = new SnakeGame(window);
        game.addSystem(new InputSystem(game, windowId));
        game.addSystem(new RenderSystem(game) {
        });
        GameEngine.initGame(game);
    }

    @Override
    public void onUserCreate() {
        pallete = createSprite("resources/palletes/default.png", 0, 256, 64);
        orthographicMode(GRID_WIDTH, GRID_HEIGTH);
        this.snake = new Snake(GRID_CELL_SIZE);
        this.apples.add(new Apple(GRID_CELL_SIZE, GRID_WIDTH, -GRID_HEIGTH));
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        if (!snake.dead) {
            if (getInputSystem().isKeyPressed(GLFW_KEY_UP)) {
                this.snake.dx = 0;
                this.snake.dy = 1;
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_DOWN)) {
                this.snake.dx = 0;
                this.snake.dy = -1;
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_LEFT)) {
                this.snake.dx = -1;
                this.snake.dy = 0;
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_RIGHT)) {
                this.snake.dx = 1;
                this.snake.dy = 0;
            }
            this.snake.update(deltaTime);
            Apple eated = null;
            for (Apple a : apples) {
                if (snake.x == a.x && snake.y == a.y) {
                    eated = a;
                }
            }
            if (eated != null) {
                apples.remove(eated);
                snake.onEat();
                apples.add(new Apple(GRID_CELL_SIZE, GRID_WIDTH, -GRID_HEIGTH));
                score++;
            }
        } else {
            if (getInputSystem().isKeyPressed(GLFW_KEY_ENTER)) {
                this.snake = new Snake(GRID_CELL_SIZE);
                this.apples.clear();
                this.apples.add(new Apple(GRID_CELL_SIZE, GRID_WIDTH, -GRID_HEIGTH));
                this.score = 0;
            }
        }
    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        //background
        drawSprite(pallete, BACKGROUND_COLOR, 32, 32, 0, 0, GRID_WIDTH, GRID_HEIGTH);

        // draw snake
        this.snake.draw();

        // draw apples
        for (Apple apple : apples) {
            apple.draw();
        }

        // draw ui
        drawText("score: " + score, 64, -112, FONT_SIZE, FONT_SIZE);
        if (snake.dead) {
            drawText("game over!", 64, -64, FONT_SIZE, FONT_SIZE);
            drawText("press [enter] to try again", 64, -70, FONT_SIZE, FONT_SIZE);
        }
    }
}

class Snake {
    boolean dead = false;
    int x = 24;
    int y = -24;
    int dx = 1;
    int dy = 0;
    int size;
    Sprite sprite;
    Tile tile = Tile.HEAD_RIGHT;
    int color = 9;
    float time = 0;
    static final float update_time = 0.10f;
    List<Body> body = new ArrayList<>();

    enum Tile {
        HEAD_RIGHT(2), BODY_RIGHT(1), TAIL_RIGHT(0),
        HEAD_LEFT(5), BODY_LEFT(6), TAIL_LEFT(7),
        HEAD_UP(3), BODY_UP(8), TAIL_UP(19),
        HEAD_DOWN(14), BODY_DOWN(9), TAIL_DOWN(4),
        CORNER_TOP_LEFT(10),CORNER_TOP_RIGHT(11),
        CORNER_TOP_LEFT_VARIANT(12),CORNER_TOP_RIGHT_VARIANT(13),
        CORNER_BOTTOM_LEFT(15),CORNER_BOTTOM_RIGHT(16),
        CORNER_BOTTOM_LEFT_VARIANT(17),CORNER_BOTTOM_RIGHT_VARIANT(18);

        int value;

        Tile(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

    }

    class Body {
        int x;
        int y;
        int dx;
        int dy;
        Tile tile;
        Body prev;
        Body next;

        Body(int x, int y, int dx, int dy) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
        }

        void update(int x, int y, int dx, int dy) {
            int prevx = this.x;
            int prevy = this.y;
            int prevdx = this.dx;
            int prevdy = this.dy;
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            if (prev != null) {
                this.updateSprite(prevdx, prevdy, false);
                prev.update(prevx, prevy, prevdx, prevdy);
            } else {
                this.updateSprite(prevdx, prevdy, true);
            }
        }

        void updateSprite(int prevdx, int prevdy, boolean last) {
            if (last) {
                if (this.dx > 0) {
                    this.tile = Tile.TAIL_RIGHT;
                } else if (this.dx < 0) {
                    this.tile = Tile.TAIL_LEFT;
                } else if (this.dy > 0) {
                    this.tile = Tile.TAIL_UP;
                } else if (this.dy < 0) {
                    this.tile = Tile.TAIL_DOWN;
                }
            }
            else {
                if (dx > 0 && prevdy == 0) {
                    this.tile = Tile.BODY_RIGHT;
                } else if (dx > 0 && prevdy > 0) {
                    this.tile = Tile.CORNER_TOP_LEFT;
                } else if (dx > 0 && prevdy < 0) {
                    this.tile = Tile.CORNER_BOTTOM_LEFT_VARIANT;
                } else if (dx < 0 && prevdy > 0) {
                    this.tile = Tile.CORNER_TOP_RIGHT_VARIANT;
                } else if (dx < 0 && prevdy < 0) {
                    this.tile = Tile.CORNER_BOTTOM_RIGHT;
                } else if (dx < 0 && prevdy == 0) {
                    this.tile = Tile.BODY_LEFT;
                } else if (dy > 0 && prevdx > 0) {
                    this.tile = Tile.CORNER_BOTTOM_RIGHT_VARIANT;
                } else if (dy > 0 && prevdx < 0) {
                    this.tile = Tile.CORNER_BOTTOM_LEFT;
                } else if (dy < 0 && prevdx < 0) {
                    this.tile = Tile.CORNER_TOP_LEFT_VARIANT;
                } else if (dy < 0 && prevdx > 0) {
                    this.tile = Tile.CORNER_TOP_RIGHT;
                } else if (dy > 0 && prevdx == 0) {
                    this.tile = Tile.BODY_UP;
                } else if (dy < 0 && prevdx == 0) {
                    this.tile = Tile.BODY_DOWN;
                }
            }
        }
    }

    Snake(int size) {
        this.sprite = createSprite("resources/snakegame/snake.png", 2, 156, 128);
        this.size = size;
        this.body = new ArrayList<>();
    }

    public void onEat() {
        this.addBody(x, y, this.dx, this.dy);
    }

    public void addBody(int x, int y, int dx, int dy) {
        Body b = new Body(x, y, dx, dy);
        if (!this.body.isEmpty()) {
            Body last = body.get(body.size() - 1);
            last.prev = b;
        }
        body.add(b);
    }

    void update(float deltaTime) {
        this.time += deltaTime;
        if (time >= update_time) {
            int prevx = x;
            int prevy = y;
            this.x += size * dx;
            this.y += size * dy;
            this.time = 0;
            this.updateSprite();
            if (body.isEmpty()) {
                this.addBody(x - size, y, dx, dy);
                this.addBody(30, y, dx, dy);
            }
            Body neck = body.get(0);
            neck.update(prevx, prevy, dx, dy);

            //collision
            Body b = neck;
            while (b.prev != null) {
                if (b.prev.x == x && b.prev.y == y) {
                    this.dead = true;
                    return;
                }
                b = b.prev;
            }
        }
    }

    private void updateSprite() {
        if (this.dx > 0) {
            this.tile = Tile.HEAD_RIGHT;
        } else if (this.dx < 0) {
            this.tile = Tile.HEAD_LEFT;
        } else if (this.dy > 0) {
            this.tile = Tile.HEAD_UP;
        } else if (this.dy < 0) {
            this.tile = Tile.HEAD_DOWN;
        }
    }

    void draw() {
        drawSprite(sprite, this.tile.value(), 32, 32, x, y, size, size);
        for (Body b : body) {
            if (b.tile != null) {
                drawSprite(sprite, b.tile.value(), 32, 32, b.x, b.y, size, size);
            }
        }
    }
}

class Apple {
    int x;
    int y;
    Sprite sprite;
    int size;
    int color = 3;

    Apple(int size, int xmax, int ymax) {
        this.sprite = createSprite("resources/snakegame/apple.png", 0, 32, 32);
        this.size = size;
        this.x = (int) (Math.random() * xmax / size) * size;
        this.y = (int) (Math.random() * ymax / size) * size;
    }

    void draw() {
        drawSprite(sprite, color, 32, 32, x, y, size, size);
    }
}