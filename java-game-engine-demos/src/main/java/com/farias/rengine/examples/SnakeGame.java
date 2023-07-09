package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joml.Vector2i;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.GameEngine.Sprite;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;

public class SnakeGame extends Game {

    private static final Random random = new Random();

    private final float updateTime = 0.2f;
    private float time = 0;
    private final int rows;
    private final int cols;
    Snake snake;
    Apple apple;
    private Direction direction;
    private Direction nexDirection;
    int score;
    private SnakeGameRenderer renderer;
    boolean gameOver = false;

    public static void launch(String[] args) {
        int windowWidth = 512;
        int windowHeight = 512;
        Window window = new Window(windowWidth, windowHeight);
        long windowId = window.create();
        SnakeGame game = new SnakeGame(window, 10, 10);
        game.addSystem(new InputSystem(windowId));
        game.addSystem(RenderSystem.renderSystem2D());
        GameEngine.initGame(game);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public SnakeGame(Window window, int rows, int cols) {
        super("New Snake Game", window);
        this.rows = rows;
        this.cols = cols;

        this.resetGame();
    }

    private void resetGame() {
        this.score = 0;
        int snakeX = this.cols / 3;
        int snakeY = this.rows / 2;
        int snakeSize = 3;
        this.snake = new Snake(snakeX, snakeY, snakeSize);

        int appleX = cols / 3 * 2;
        int appleY = rows / 2;
        this.apple = new Apple(appleX, appleY);
        this.nexDirection = Direction.RIGHT;
    }

    @Override
    public void onUserCreate() {
        this.renderer = new SnakeGameRenderer(rows, cols);
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        this.time += deltaTime;
        if (!this.gameOver) {
            if (getInputSystem().isKeyPressed(GLFW_KEY_UP)) {
                this.changeDirection(Direction.UP);
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_DOWN)) {
                this.changeDirection(Direction.DOWN);
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_LEFT)) {
                this.changeDirection(Direction.LEFT);
            }
            if (getInputSystem().isKeyPressed(GLFW_KEY_RIGHT)) {
                this.changeDirection(Direction.RIGHT);
            }
            if (time >= updateTime) {
                this.direction = nexDirection;
                this.moveSnake();
                this.time = 0;
            }
        } else {
            if (getInputSystem().isKeyPressed(GLFW_KEY_ENTER)) {
                this.gameOver = false;
                this.resetGame();
            }
        }

    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        this.renderer.render(this);
    }

    public void changeDirection(Direction direction) {
        if (isOppositeDirection(direction) || this.direction == direction) {
            return;
        }
        this.nexDirection = direction;
    }

    public boolean isOppositeDirection(Direction direction) {
        return this.direction.getOpposite() == direction;
    }

    public void moveSnake() {
        final var head = Optional.of(snake.getHead());
        final Optional<Vector2i> newHead;
        switch (direction) {
            case UP:
                newHead = head.map((pos) -> new Vector2i(pos.x, pos.y - 1));
                break;
            case DOWN:
                newHead = head.map((pos) -> new Vector2i(pos.x, pos.y + 1));
                break;
            case LEFT:
                newHead = head.map((pos) -> new Vector2i(pos.x - 1, pos.y));
                break;
            case RIGHT:
                newHead = head.map((pos) -> new Vector2i(pos.x + 1, pos.y));
                break;
            default:
                newHead = Optional.empty();
        }

        newHead.ifPresent(pos -> {
            if (!this.isValid(pos)) {
                this.gameOver = true;
                return;
            }
            snake.add(pos.x, pos.y);
            if (!apple.isPositionEqualTo(pos)) {
                snake.remove();
            } else {
                var applePosition = randomPosition();
                apple = new Apple(applePosition.x, applePosition.y);
                score++;
            }
        });
    }

    public boolean isValid(Vector2i position) {
        return position.x >= 0 && position.x < this.cols && position.y >= 0 && position.y < this.rows
                && !this.snake.contains(position.x, position.y);
    }

    public Vector2i randomPosition() {
        var freePositions = IntStream.rangeClosed(1, cols)
                .boxed()
                .flatMap(x -> IntStream.rangeClosed(1, rows)
                        .mapToObj(y -> new Vector2i(x - 1, y - 1)))
                .filter(pos -> !this.snake.contains(pos.x, pos.y))
                .collect(Collectors.toList());

        return getRandomElement(freePositions);
    }

    public static <T> T getRandomElement(List<T> list) {
        var randomIndex = getRandomNumber(0, list.size() - 1);
        return list.get(randomIndex);
    }

    public static int getRandomNumber(int start, int end) {
        return random.nextInt(end - start + 1) + start;
    }

}

enum Direction {
    UP, DOWN, LEFT, RIGHT;

    public Direction getOpposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                throw new IllegalArgumentException("Invalid direction");
        }
    }
}

class Snake {
    final Deque<Vector2i> positions;

    public Snake(int x, int y, int size) {
        positions = new LinkedList<Vector2i>();
        positions.offerFirst(new Vector2i(x, y));
        for (int i = 0; i < size; i++) {
            positions.offerLast(new Vector2i(x, y));
        }
    }

    public Vector2i getHead() {
        return new Vector2i(positions.peekFirst());
    }

    public void add(int x, int y) {
        this.positions.offerFirst(new Vector2i(x, y));
    }

    public Vector2i remove() {
        return this.positions.pollLast();
    }

    public boolean contains(int x, int y) {
        return positions.contains(new Vector2i(x, y));
    }

    public Deque<Vector2i> getPositions() {
        return positions;
    }

    public Iterator<Vector2i> positionsIterator() {
        return positions.iterator();
    }
}

class Apple {
    final Vector2i position;

    public Apple(int x, int y) {
        this.position = new Vector2i(x, y);
    }

    public boolean isPositionEqualTo(Vector2i pos) {
        return this.position.equals(pos);
    }
}

class SnakeGameRenderer {
    private static final int BACKGROUND_COLOR = 7;
    private static final int TAIL_TILE = 0;
    private static final int BODY_TILE = 1;
    private static final int CORNER_TILE = 2;
    private static final int HEAD_TILE = 3;

    private static final int PALLETE_SPRITE_INDEX = 0;
    private static final int PALLETE_SPRITE_WIDTH = 256;
    private static final int PALLETE_SPRITE_HEIGHT = 64;
    private static final int PALLETE_SPRITE_TILE_SIZE = 32;
    private static final int SNAKE_SPRITE_INDEX = 0;
    private static final int SNAKE_SPRITE_WIDTH = 32;
    private static final int SNAKE_SPRITE_HEIGHT = 8;
    private static final int SNAKE_SPRITE_TILE_SIZE = 8;
    private static final int APPLE_SPRITE_INDEX = 0;
    private static final int APPLE_SPRITE_WIDTH = 8;
    private static final int APPLE_SPRITE_HEIGHT = 8;
    private static final int APPLE_SPRITE_TILE_SIZE = 8;

    public static final int FONT_SIZE = 12;

    final Sprite palleteSprite;
    final Sprite snakeSprite;
    final Sprite appleSprite;
    final int width = 640;
    final int height = 640;
    final int rows;
    final int cols;

    public SnakeGameRenderer(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        GameEngine.orthographicMode(width, height);
        this.palleteSprite = GameEngine.createSprite("resources/palletes/default.png", PALLETE_SPRITE_INDEX,
                PALLETE_SPRITE_WIDTH, PALLETE_SPRITE_HEIGHT);
        this.snakeSprite = GameEngine.createSprite("resources/snakegame/snake-pixel-art.png", SNAKE_SPRITE_INDEX,
                SNAKE_SPRITE_WIDTH, SNAKE_SPRITE_HEIGHT);
        this.appleSprite = GameEngine.createSprite("resources/snakegame/apple-pixel-art.png", APPLE_SPRITE_INDEX,
                APPLE_SPRITE_WIDTH, APPLE_SPRITE_HEIGHT);
    }

    public void render(SnakeGame game) {
        renderBackground();
        int cellWidth = width / cols;
        int cellHeight = height / rows;
        renderSnake(game.snake, cellWidth, cellHeight);
        renderApple(game.apple, cellWidth, cellHeight);

        // draw ui
        GameEngine.drawText("score: " + game.score, width / 2, -32, FONT_SIZE, FONT_SIZE);
        if (game.gameOver) {
            GameEngine.drawText("game over!", width / 2, -height / 2, FONT_SIZE, FONT_SIZE);
            GameEngine.drawText("press [enter] to try again", width / 2, -height / 2 - 32, FONT_SIZE, FONT_SIZE);
        }
    }

    private void renderApple(Apple apple, int cellWidth, int cellHeight) {
        GameEngine.drawSprite(appleSprite, APPLE_SPRITE_INDEX, APPLE_SPRITE_TILE_SIZE, APPLE_SPRITE_TILE_SIZE,
                apple.position.x * cellWidth,
                -apple.position.y * cellHeight,
                cellWidth,
                cellHeight);
    }

    private void renderBackground() {
        GameEngine.drawSprite(palleteSprite, BACKGROUND_COLOR, PALLETE_SPRITE_TILE_SIZE, PALLETE_SPRITE_TILE_SIZE, 0, 0,
                width, height);
    }

    private void renderSnake(Snake snake, int cellWidth, int cellHeight) {
        var iterator = snake.positionsIterator();
        if (!iterator.hasNext()) {
            return;
        }

        Vector2i currentPos = iterator.next();
        Vector2i nextPos = currentPos;
        while (nextPos != null) {
            Vector2i previousPos = currentPos;
            currentPos = nextPos;
            nextPos = iterator.hasNext() ? iterator.next() : null;

            float rotation = getSpriteRotation(previousPos, currentPos, nextPos);
            int snakeTile = getSnakeTile(previousPos, currentPos, nextPos);
            GameEngine.drawSprite(snakeSprite, snakeTile, SNAKE_SPRITE_TILE_SIZE, SNAKE_SPRITE_TILE_SIZE,
                    currentPos.x * cellWidth,
                    -currentPos.y * cellHeight, cellWidth, cellHeight, rotation);
        }
    }

    private int getSnakeTile(Vector2i previousPos, Vector2i currentPos, Vector2i nextPos) {
        if (previousPos.equals(currentPos)) {
            return HEAD_TILE;
        } else if (isCorner(previousPos, nextPos)) {
            return CORNER_TILE;
        } else if (nextPos == null) {
            return TAIL_TILE;
        } else {
            return BODY_TILE;
        }
    }

    private float adjustCornerAngle(Vector2i currentPos, Vector2i nextPos, Vector2i previousPos, float angle) {
        if (crossProduct(previousPos, currentPos, nextPos) > 0) {
            return angle += 45;
        } else {
            return angle -= 135;
        }
    }

    public boolean isCorner(Vector2i position1, Vector2i position2) {
        if (position1 == null || position2 == null) {
            return false;
        }
        return position1.x != position2.x && position1.y != position2.y;
    }

    public float getSpriteRotation(Vector2i previousPos, Vector2i currentPos, Vector2i nextPos) {
        if (nextPos == null) {
            nextPos = currentPos;
        }
        int dx = nextPos.x - previousPos.x;
        int dy = previousPos.y - nextPos.y;

        float angleRadians = (float) Math.atan2(dy, dx);
        float angleDegrees = (float) Math.toDegrees(angleRadians);

        if (isCorner(previousPos, nextPos)) {
            angleDegrees = adjustCornerAngle(currentPos, nextPos, previousPos, angleDegrees);
        }

        return angleDegrees;
    }

    public int crossProduct(Vector2i p1, Vector2i p2, Vector2i p3) {
        return (p2.x - p1.x) * (p3.y - p2.y)
                - (p2.y - p1.y) * (p3.x - p2.x);
    }
}
