package com.farias.rengine.examples;

import java.nio.file.Path;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joml.Vector2i;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.InputModule;
import com.farias.rengine.engine.modules.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.BitmapFont;
import com.farias.rengine.toolkit2d.Canvas;
import com.farias.rengine.toolkit2d.SpriteSheet;
import com.farias.rengine.toolkit2d.Toolkit2DModule;

public class SnakeGame {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 640;

    private static final Random random = new Random();

    private static final float UPDATE_TIME = 0.6f;

    private static final int BACKGROUND_COLOR = 7;
    private static final int TAIL_TILE = 0;
    private static final int BODY_TILE = 1;
    private static final int CORNER_TILE = 2;
    private static final int HEAD_TILE = 3;

    private static final int PALLETE_TILE_SIZE = 32;
    private static final int SNAKE_TILE_SIZE = 8;
    private static final int APPLE_TILE_SIZE = 8;
    private static final int APPLE_TILE = 0;

    private static final int FONT_SIZE = 14;

    private float time = 0;
    private final int rows;
    private final int cols;
    private Deque<Vector2i> snake;
    private Vector2i apple;
    private Direction direction;
    private Direction nexDirection;
    private int score;
    private boolean gameOver = false;

    private final Window window;
    private final InputSystem input;
    private final Renderer renderer;
    private final Canvas canvas;
    private final SpriteSheet palette;
    private final SpriteSheet snakeSheet;
    private final SpriteSheet appleSheet;
    private final BitmapFont font;

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new WindowModule(WIDTH, HEIGHT, "New Snake Game"))
                .registerModule(new InputModule())
                .registerModule(new RenderModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Toolkit2DModule(WIDTH, HEIGHT));

        EngineContext context = engine.start();
        try {
            new SnakeGame(context, 10, 10).run();
        } finally {
            engine.terminate();
        }
    }

    public static void main(String[] args) {
        launch();
    }

    public SnakeGame(EngineContext context, int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        window = context.require(Window.class);
        input = context.require(InputSystem.class);
        renderer = context.require(Renderer.class);
        canvas = context.require(Canvas.class);
        Assets assets = context.require(Assets.class);

        palette = new SpriteSheet(assets.texture("palletes/default.png"), PALLETE_TILE_SIZE, PALLETE_TILE_SIZE);
        snakeSheet = new SpriteSheet(assets.texture("snakegame/snake-pixel-art.png"), SNAKE_TILE_SIZE,
                SNAKE_TILE_SIZE);
        appleSheet = new SpriteSheet(assets.texture("snakegame/apple-pixel-art.png"), APPLE_TILE_SIZE,
                APPLE_TILE_SIZE);
        font = canvas.defaultFont();

        canvas.clearColor(0f, 0f, 0f);
        this.resetGame();
    }

    private void run() {
        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            timer.update();
            input.update();
            update(timer.deltaTime());

            renderer.beginFrame();
            canvas.begin();
            draw(canvas);
            renderer.endFrame();
        }
    }

    private void resetGame() {
        this.score = 0;
        int snakeX = this.cols / 3;
        int snakeY = this.rows / 2;
        int snakeSize = 3;
        this.snake = createSnake(snakeX, snakeY, snakeSize);

        int appleX = cols / 3 * 2;
        int appleY = rows / 2 + 1;
        this.apple = new Vector2i(appleX, appleY);
        this.nexDirection = Direction.RIGHT;
    }

    private void update(float deltaTime) {
        this.time += deltaTime;
        if (!this.gameOver) {
            if (input.isKeyPressed(Key.UP)) {
                this.changeDirection(Direction.UP);
            }
            if (input.isKeyPressed(Key.DOWN)) {
                this.changeDirection(Direction.DOWN);
            }
            if (input.isKeyPressed(Key.LEFT)) {
                this.changeDirection(Direction.LEFT);
            }
            if (input.isKeyPressed(Key.RIGHT)) {
                this.changeDirection(Direction.RIGHT);
            }
            if (time >= UPDATE_TIME) {
                this.direction = nexDirection;
                this.moveSnake();
                this.time = 0;
            }
        } else {
            if (input.isKeyPressed(Key.ENTER)) {
                this.gameOver = false;
                this.resetGame();
            }
        }
    }

    private void draw(Canvas g) {
        g.spr(palette, BACKGROUND_COLOR, 0, 0, WIDTH, HEIGHT);

        int cellWidth = WIDTH / cols;
        int cellHeight = HEIGHT / rows;

        drawSnake(g, cellWidth, cellHeight);
        g.spr(appleSheet, APPLE_TILE, apple.x * cellWidth, apple.y * cellHeight, cellWidth, cellHeight);

        drawUI(g);
    }

    private void drawUI(Canvas g) {
        g.textCentered(font, "score: " + score, WIDTH / 2, 32, FONT_SIZE);
        if (gameOver) {
            g.textCentered(font, "game over!", WIDTH / 2, HEIGHT / 2, FONT_SIZE);
            g.textCentered(font, "press [enter] to try again", WIDTH / 2, HEIGHT / 2 + 32, FONT_SIZE);
        }
    }

    private void drawSnake(Canvas g, int cellWidth, int cellHeight) {
        var iterator = snake.iterator();
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
            // o Canvas gira no sentido horario; a conta abaixo produz o
            // angulo anti-horario, entao o sinal e invertido aqui.
            g.spr(snakeSheet, snakeTile, currentPos.x * cellWidth, currentPos.y * cellHeight, cellWidth,
                    cellHeight, -rotation);
        }
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
        final var head = Optional.of(snake.peekFirst());
        final Optional<Vector2i> newHead;
        switch (direction) {
            case UP:
                newHead = head.map(pos -> new Vector2i(pos.x, pos.y - 1));
                break;
            case DOWN:
                newHead = head.map(pos -> new Vector2i(pos.x, pos.y + 1));
                break;
            case LEFT:
                newHead = head.map(pos -> new Vector2i(pos.x - 1, pos.y));
                break;
            case RIGHT:
                newHead = head.map(pos -> new Vector2i(pos.x + 1, pos.y));
                break;
            default:
                newHead = Optional.empty();
        }

        newHead.ifPresent(pos -> {
            if (!this.isValid(pos)) {
                this.gameOver = true;
                return;
            }
            snake.offerFirst(pos);
            if (!apple.equals(pos)) {
                snake.pollLast();
            } else {
                var applePosition = randomPosition();
                apple = new Vector2i(applePosition.x, applePosition.y);
                score++;
            }
        });
    }

    public static Deque<Vector2i> createSnake(int x, int y, int size) {
        var positions = new LinkedList<Vector2i>();
        positions.offerFirst(new Vector2i(x, y));
        for (int i = 0; i < size; i++) {
            positions.offerLast(new Vector2i(x - (i + 1), y));
        }
        return positions;
    }

    public boolean isValid(Vector2i position) {
        return position.x >= 0 && position.x < this.cols && position.y >= 0 && position.y < this.rows
                && !this.snake.contains(position);
    }

    public Vector2i randomPosition() {
        var freePositions = IntStream.rangeClosed(1, cols)
                .boxed()
                .flatMap(x -> IntStream.rangeClosed(1, rows)
                        .mapToObj(y -> new Vector2i(x - 1, y - 1)))
                .filter(pos -> !this.snake.contains(pos))
                .collect(Collectors.toList());

        return getRandomElement(freePositions);
    }

    private static <T> T getRandomElement(List<T> list) {
        var randomIndex = getRandomNumber(0, list.size() - 1);
        return list.get(randomIndex);
    }

    private static int getRandomNumber(int start, int end) {
        return random.nextInt(end - start + 1) + start;
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
            angle += 45;
        } else {
            angle -= 135;
        }
        return angle;
    }

    private boolean isCorner(Vector2i position1, Vector2i position2) {
        if (position1 == null || position2 == null) {
            return false;
        }
        return position1.x != position2.x && position1.y != position2.y;
    }

    private float getSpriteRotation(Vector2i previousPos, Vector2i currentPos, Vector2i nextPos) {
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

    private int crossProduct(Vector2i p1, Vector2i p2, Vector2i p3) {
        return (p2.x - p1.x) * (p3.y - p2.y)
                - (p2.y - p1.y) * (p3.x - p2.x);
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
