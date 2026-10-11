package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joml.Matrix4f;
import org.joml.Vector2i;

import com.farias.rengine.engine3d.Game;
import com.farias.rengine.toolkit2d.legacy.BitmapFont;
import com.farias.rengine.toolkit2d.legacy.Projections2D;
import com.farias.rengine.toolkit2d.legacy.Sprite;
import com.farias.rengine.toolkit2d.legacy.SpriteBatch2D;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;

public class SnakeGame extends Game {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 640;

    private static final Random random = new Random();

    private static final float UPDATE_TIME = 0.6f;
    private float time = 0;
    private final int rows;
    private final int cols;
    Deque<Vector2i> snake;
    Vector2i apple;
    private Direction direction;
    private Direction nexDirection;
    int score;
    boolean gameOver = false;

    private SpriteBatch2D batch;
    private BitmapFont font;
    private SnakeGameRenderer renderer2D;
    private RenderPassDescriptor passDescriptor;

    public static void launch() {
        new SnakeGame(10, 10).run();
    }

    public static void main(String[] args) {
        launch();
    }

    public SnakeGame(int rows, int cols) {
        super("New Snake Game", WIDTH, HEIGHT);
        this.rows = rows;
        this.cols = cols;

        this.resetGame();
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

    @Override
    protected void onCreate() {
        batch = new SpriteBatch2D(renderer);
        font = new BitmapFont(batch);
        renderer2D = new SnakeGameRenderer(batch, WIDTH, HEIGHT, rows, cols);

        passDescriptor = new RenderPassDescriptor("snake")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(false)
                .cullFace(true)
                .blend(true);
    }

    @Override
    protected void onUpdate(float deltaTime) {
        this.time += deltaTime;
        if (!this.gameOver) {
            if (input.isKeyPressed(GLFW_KEY_UP)) {
                this.changeDirection(Direction.UP);
            }
            if (input.isKeyPressed(GLFW_KEY_DOWN)) {
                this.changeDirection(Direction.DOWN);
            }
            if (input.isKeyPressed(GLFW_KEY_LEFT)) {
                this.changeDirection(Direction.LEFT);
            }
            if (input.isKeyPressed(GLFW_KEY_RIGHT)) {
                this.changeDirection(Direction.RIGHT);
            }
            if (time >= UPDATE_TIME) {
                this.direction = nexDirection;
                this.moveSnake();
                this.time = 0;
            }
        } else {
            if (input.isKeyPressed(GLFW_KEY_ENTER)) {
                this.gameOver = false;
                this.resetGame();
            }
        }
    }

    @Override
    protected void onRender(float deltaTime) {
        RenderView view = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections2D.orthographic2D(WIDTH, HEIGHT), new Matrix4f());
        RenderPassNode pass = renderer.createPass(passDescriptor, view);

        renderer2D.render(pass, this);
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

        public static final int FONT_SIZE = 14;

        private final SpriteBatch2D batch;
        final Sprite palleteSprite;
        final Sprite snakeSprite;
        final Sprite appleSprite;
        final int width;
        final int height;
        final int rows;
        final int cols;

        public SnakeGameRenderer(SpriteBatch2D batch, int width, int height, int rows, int cols) {
            this.batch = batch;
            this.rows = rows;
            this.cols = cols;
            this.width = width;
            this.height = height;

            this.palleteSprite = batch.createSprite("resources/palletes/default.png", PALLETE_SPRITE_INDEX,
                    PALLETE_SPRITE_WIDTH, PALLETE_SPRITE_HEIGHT);
            this.snakeSprite = batch.createSprite("resources/snakegame/snake-pixel-art.png", SNAKE_SPRITE_INDEX,
                    SNAKE_SPRITE_WIDTH, SNAKE_SPRITE_HEIGHT);
            this.appleSprite = batch.createSprite("resources/snakegame/apple-pixel-art.png", APPLE_SPRITE_INDEX,
                    APPLE_SPRITE_WIDTH, APPLE_SPRITE_HEIGHT);
        }

        public void render(RenderPassNode pass, SnakeGame game) {
            renderBackground(pass);

            int cellWidth = width / cols;
            int cellHeight = height / rows;

            renderSnake(pass, game.snake, cellWidth, cellHeight);
            renderApple(pass, game.apple, cellWidth, cellHeight);

            renderUI(pass, game);
        }

        private void renderUI(RenderPassNode pass, SnakeGame game) {
            font.drawText(pass, "score: " + game.score, width / 2, -32, FONT_SIZE, FONT_SIZE);
            if (game.gameOver) {
                font.drawText(pass, "game over!", width / 2, -height / 2, FONT_SIZE, FONT_SIZE);
                font.drawText(pass, "press [enter] to try again", width / 2, -height / 2 - 32, FONT_SIZE, FONT_SIZE);
            }
        }

        private void renderApple(RenderPassNode pass, Vector2i apple, int cellWidth, int cellHeight) {
            batch.draw(pass, appleSprite, APPLE_SPRITE_INDEX, APPLE_SPRITE_TILE_SIZE, APPLE_SPRITE_TILE_SIZE,
                    SpriteBatch2D.rectTransform(apple.x * cellWidth, -apple.y * cellHeight, cellWidth, cellHeight));
        }

        private void renderBackground(RenderPassNode pass) {
            batch.draw(pass, palleteSprite, BACKGROUND_COLOR, PALLETE_SPRITE_TILE_SIZE, PALLETE_SPRITE_TILE_SIZE,
                    SpriteBatch2D.rectTransform(0, 0, width, height));
        }

        private void renderSnake(RenderPassNode pass, Deque<Vector2i> snake, int cellWidth, int cellHeight) {
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
                batch.draw(pass, snakeSprite, snakeTile, SNAKE_SPRITE_TILE_SIZE, SNAKE_SPRITE_TILE_SIZE,
                        SpriteBatch2D.rectTransform(currentPos.x * cellWidth, -currentPos.y * cellHeight, cellWidth,
                                cellHeight, rotation));
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
                angle += 45;
            } else {
                angle -= 135;
            }
            return angle;
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
