package com.farias.rengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.farias.rengine.ecs.event.EventSystem;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.render.Model;
import com.farias.rengine.render.Shader;
import com.farias.rengine.render.Texture;
import com.farias.rengine.render.TexturedModel;
import com.farias.rengine.util.ModelLoader;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.farias.rengine.camera.Camera;
import com.farias.rengine.render.Sprite;

/**
 * Manages a game instance and provides useful methods that can be invoked from
 * any part of the system
 * 
 * @author rafarias
 *
 */
public class GameEngine {
    private static Game game_instance;

    public static void initGame(Game game) {
        if (game_instance != null) {
            throw new IllegalStateException("Cannot create a more tha one game instance.");
        }
        game_instance = game;
        game_instance.run();
    }

    public static Game getGameInstance() {
        if (game_instance == null) {
            throw new IllegalStateException("Cannot obtain a game instance. The game must be created first.");
        }
        return game_instance;
    }

    public static InputSystem getInput() {
        return getGameInstance().getInputSystem();
    }

    public static EventSystem getEventSystem() {
        return getGameInstance().getEventSystem();
    }

    // WINDOW
    public static int getWindowWidth() {
        return getGameInstance().getWindow().getWidth();
    }

    public static int getWindowHeight() {
        return getGameInstance().getWindow().getHeight();
    }

    // GAME STATE
    private static GameState state = GameState.SCENE;

    public static void setState(GameState newState) {
        state = newState;
    }

    public static GameState getState() {
        return state;
    }

    // CAMERA
    private static final Camera camera = new Camera();
    private static final Camera uiCamera = new Camera(0, 0, -5);

    public static void setCamera(int x, int y, int z) {
        camera.disableLookAt();
        camera.position.x = x;
        camera.position.y = y;
        camera.position.z = z;
    }

    public static void setCamera(int x, int y) {
        camera.disableLookAt();
        camera.position.x = x;
        camera.position.y = y;
    }

    public static void rotateCamera(float x, float y) {
        camera.disableLookAt();
        camera.rotation.x = x;
        camera.rotation.y = y;
    }

    public static void setCameraZoom(float zoom) {
        camera.scale.x = zoom;
        camera.scale.y = zoom;
    }

    public static void setCameraLookAt(float eyeX, float eyeY, float eyeZ,
            float targetX, float targetY, float targetZ,
            float upX, float upY, float upZ) {

        camera.lookAt(eyeX, eyeY, eyeZ,
                targetX, targetY, targetZ,
                upX, upY, upZ);
    }

    private static Camera getCurrentCamera() {
        if (state == GameState.SCENE) {
            return camera;
        } else if (state == GameState.UI) {
            return uiCamera;
        } else {
            throw new IllegalStateException("Invalid game state: " + state);
        }
    }

    // PROJECTIONS

    private static final Matrix4f projection_matrix = new Matrix4f();
    private static final Matrix4f ui_projection_matrix = new Matrix4f();

    /**
     * Sets the current projection to a simple orthographic projection. Works well
     * with 2D Games
     * 
     * @param width
     * @param height
     */
    public static void orthographicMode(float width, float height) {
        // projecao alternativa com o centro da tela sendo (width/2, height/2) o início
        // da tela em (0, 0)
        // e final da tela em (width, -height)
        projection_matrix.setOrtho(0, width, -height, 0, 1000, -1000);
        ui_projection_matrix.setOrtho(0, width, -height, 0, 1000, -1000);
        // .rotateX((float) Math.toRadians(-45f))
        // .rotateZ((float) Math.toRadians(-30f));

        // projecao padrao com o centro da tela sendo (0, 0) o início da tela em
        // (-width/2, height/2)
        // e final da tela em (width/2, -height/2)
        // projection_matrix.setOrtho(-width/2, width/2, -height/2, height/2, 1000,
        // -1000);
        // projection_matrix.setOrtho(-width/2, width/2, -height/2, height/2, -width,
        // width);
    }

    public static void orthographic3DMode(float size, float aspect) {
        projection_matrix.setOrtho(
                -size * aspect,
                size * aspect,
                -size,
                size,
                -1000f,
                1000f);
    }

    private static final int FOV = 70;
    private static final float Z_FAR = 1000f;
    private static final float Z_NEAR = 1.0f;

    public static void perspectiveMode(int width, int height) {
        float aspectRatio = (float) width / (float) height;
        projection_matrix.setPerspective((float) Math.toRadians(FOV), aspectRatio, Z_NEAR, Z_FAR);
    }

    private static Matrix4f getModelViewMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
        Matrix4f modelMatrix = getModelMatrix(position, rotation, scale);
        
            return getCurrentCamera().getViewMatrix().mul(modelMatrix, new Matrix4f());
    }

    private static Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
        Matrix4f translationMatrix = new Matrix4f().translation(position);
        Matrix4f rotationMatrix = new Matrix4f().rotationXYZ((float) Math.toRadians(rotation.x),
                (float) Math.toRadians(rotation.y), (float) Math.toRadians(rotation.z));
        Matrix4f scalingMatrix = new Matrix4f().scale(scale);

        return translationMatrix.mul(scalingMatrix.mul(rotationMatrix));
    }

    private static Matrix4f getCurrentProjection() {
        if (state == GameState.SCENE) {
            return projection_matrix;
        } else if (state == GameState.UI) {
            return ui_projection_matrix;
        } else {
            throw new IllegalStateException("Invalid game state: " + state);
        }
    }

    // SPRITES

    public static Sprite createSprite(String file, int tile, float width, float height) {
        return Sprite.createSprite(file, tile, width, height);
    }

    public static void drawSprite(Sprite sprite, Matrix4f projection) {
        sprite.draw(projection);
    }

    public static void drawSprite(Sprite sprite, int x, int y, int width, int height, float rotation) {
        Matrix4f modelViewMatrix = getModelViewMatrix(new Vector3f(x + width / 2, y - height / 2, 0),
                new Vector3f(0, 0, rotation), new Vector3f(width / 2f, height / 2f, 1f));

        Matrix4f projection = getCurrentProjection().mul(modelViewMatrix, new Matrix4f());

        drawSprite(sprite, projection);
    }

    public static void drawSprite(Sprite sprite, int x, int y, int width, int height) {
        drawSprite(sprite, x, y, width, height, 0f);
    }

    public static void drawSprite(Sprite sprite, int tile, int tile_width, int tile_height, int x, int y, int width,
            int height) {
        drawSprite(sprite, tile, tile_width, tile_height, x, y, width, height, 0f);
    }

    public static void drawSprite(Sprite sprite, int tile, int tile_width, int tile_height, int x, int y, int width,
            int height, float rotation) {
        sprite.setTexCoords(Sprite.createTexCoords(sprite.getTexture(), tile, tile_width, tile_height));
        drawSprite(sprite, x, y, width, height, rotation);
    }

    // TEXT
    private static Sprite font;
    private static final String FONTS_FILE_NAME = "default16x16.png";

    private static Sprite getFont() {
        if (font == null) {
            try {
                createFontsFile();
                font = createSprite(FONTS_FILE_NAME, 0, 16, 16);
            } catch (IOException e) {
                java.lang.System.err.println("falha ao gravar o arquivo de fontes");
                java.lang.System.exit(0);
            }
        }
        return font;
    }

    private static void createFontsFile() throws IOException {
        InputStream initialStream = GameEngine.class.getClassLoader().getResourceAsStream("fonts/" + FONTS_FILE_NAME);
        File targetFile = new File(FONTS_FILE_NAME);
        Files.copy(initialStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        initialStream.close();
    }

    public static void drawText(String text, int x, int y, int width, int height) {
        int offset = -text.length() * width / 2;
        for (char c : text.toCharArray()) {
            drawChar(c, x + offset, y, width, height);
            offset += width;
        }
    }

    public static void drawChar(int c, int x, int y, int width, int height) {
        drawSprite(getFont(), c, 16, 16, x, y, width, height);
    }

    // MAP
    public static class Map {
        private Model model;
        private Texture texture;
        private Shader shader;

        public Map(Texture texture, Shader shader, Model model) {
            this.model = model;
            this.shader = shader;
            this.texture = texture;
        }
    }

    public static Map createMap(String file, int tile, float width, float height) {
        Texture texture = new Texture(file);
        Shader shader = new Shader("shader");
        float[] texCoords = Sprite.createTexCoords(texture, tile, width, height);
        Model model = new Model(Sprite.VERTICES, texCoords, Sprite.INDICES);

        return new Map(texture, shader, model);
    }

    public static void drawMap(Map map, int x, int y, int width, int height, int celx, int cely, int celw, int celh) {
        map.shader.bind();
        map.texture.bind(0);
        map.shader.setUniform("sampler", 0);
        for (int row = 0; row < celh; row++) {
            for (int col = 0; col < celw; col++) {
                Matrix4f modelViewMatrix = getModelViewMatrix(
                        new Vector3f(x + width * col + width / 2, y + height * -row - height / 2, 0),
                        new Vector3f(0, 0, 0), new Vector3f(width / 2, -height / 2, 1));

                Matrix4f projection = getCurrentProjection().mul(modelViewMatrix, new Matrix4f());
                map.shader.setUniform("projection", projection);
                map.model.draw();
            }
        }
    }

    // TEXTURES

    // 3D
    public static void drawTexturedModel(TexturedModel texturedModel) {
        Matrix4f modelMatrix = texturedModel.getModelMatrix();
        texturedModel.draw(
                modelMatrix,
                camera.getViewMatrix(),
                getCurrentProjection());
    }

    public static void drawTexturedModel(TexturedModel texturedModel, float x, float y, float z, float scale,
            float rotX, float rotY, float rotZ) {
        Matrix4f modelMatrix = getModelMatrix(
                new Vector3f(x, y, z),
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(scale, scale, scale));
        texturedModel.draw(
                modelMatrix,
                camera.getViewMatrix(),
                getCurrentProjection());
    }
    
    public static TexturedModel createPlane(String textureFile, float width, float height) {
        float[] vertices = new float[] {
            -width / 2, height / 2, 0, // TOP LEFT
            width / 2, height / 2, 0, // TOP RIGHT
            width / 2, -height / 2, 0, // BOTTOM RIGHT
            -width / 2, -height / 2, 0, // BOTTOM LEFT
        };
        float[] texCoords = new float[] {
            0, 0,
            1, 0,
            1, 1,
            0, 1,
        };
        float[] normals = new float[] {
            0, 0, 1
        };
        int[] indices = new int[] {
            0, 3, 2,
            2, 1, 0
        };
        return TexturedModel.load(textureFile, vertices, texCoords, normals, indices);
    }

    // Resource Management
    public static TexturedModel loadTexturedModel(String fileName) {
        return ModelLoader.loadWavefrontOBJ(fileName);
    }

}
