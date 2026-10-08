package com.farias.rengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import com.farias.rengine.camera.Camera;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.render.Shaders;
import com.farias.rengine.render.Sprite;
import com.farias.rengine.render.TexturedModel;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.OpenGLBackend;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;
import com.farias.rengine.util.TextureLoader;

/**
 * Fachada estatica da engine. Por baixo, delega toda a renderizacao para o
 * Renderer modular do modulo java-game-engine-renderer (passes, Material,
 * Mesh) em vez de chamadas OpenGL imediatas.
 */
public class GameEngine {
    private static Game gameInstance;

    public static void initGame(Game game) {
        if (gameInstance != null) {
            throw new IllegalStateException("Cannot create a more than one game instance.");
        }
        gameInstance = game;
        gameInstance.run();
    }

    public static Game getGameInstance() {
        if (gameInstance == null) {
            throw new IllegalStateException("Cannot obtain a game instance. The game must be created first.");
        }
        return gameInstance;
    }

    public static InputSystem getInput() {
        return getGameInstance().getInputSystem();
    }

    // WINDOW

    public static int getWindowWidth() {
        return windowWidth;
    }

    public static int getWindowHeight() {
        return windowHeight;
    }

    private static int windowWidth;
    private static int windowHeight;

    // RENDERER (internal - driven by Game's loop)

    private static Renderer renderer;
    private static Shader spriteShader;
    private static Shader litShader;
    private static Mesh spriteQuadMesh;

    private static RenderPassNode currentPass;

    /**
     * Marca que a configuracao de render (estado, modo de projecao, camera)
     * mudou desde o ultimo draw e um novo pass precisa ser aberto antes do
     * proximo comando ser submetido. Ver {@link #ensurePass()}.
     */
    private static boolean dirty = true;

    /**
     * Se o color/depth buffer ja foi limpo neste frame. So o primeiro pass
     * aberto no frame deve limpar; os demais (abertos por causa de uma troca
     * de modo de projecao ou de estado no meio do frame) desenham por cima.
     */
    private static boolean clearedThisFrame;

    private enum RenderMode {
        MODE_2D,
        MODE_3D
    }

    private static RenderMode renderMode = RenderMode.MODE_3D;

    /**
     * Chamado uma vez por {@link Game#init()}, com o contexto GL ja corrente.
     */
    static void initRenderer(int width, int height) {
        windowWidth = width;
        windowHeight = height;

        GL.createCapabilities();

        renderer = new Renderer(new OpenGLBackend());
        renderer.initialize();

        spriteShader = renderer.createShader(Shaders.SPRITE_VERTEX, Shaders.SPRITE_FRAGMENT);
        litShader = renderer.createShader(Shaders.LIT_VERTEX, Shaders.LIT_FRAGMENT);
        spriteQuadMesh = renderer.createMesh(spriteQuadData());
    }

    static void beginFrame() {
        renderer.beginFrame();
        currentPass = null;
        clearedThisFrame = false;
        dirty = true;
    }

    static void endFrame() {
        renderer.endFrame();
    }

    static void dispose() {
        renderer.dispose();
    }

    public static Renderer getRenderer() {
        return renderer;
    }

    static Shader getLitShader() {
        return litShader;
    }

    private static MeshData spriteQuadData() {
        float[] vertices = {
                -1f, 1f, 0f,
                1f, 1f, 0f,
                1f, -1f, 0f,
                -1f, -1f, 0f,
        };
        float[] texCoords = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f,
        };
        float[] normals = {
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
                0f, 0f, 1f,
        };
        int[] indices = { 0, 3, 2, 2, 1, 0 };
        return new MeshData(vertices, texCoords, normals, indices);
    }

    // GAME STATE

    private static GameState state = GameState.SCENE;

    public static void setState(GameState newState) {
        state = newState;
        dirty = true;
    }

    public static GameState getState() {
        return state;
    }

    /**
     * Abre um novo pass se o estado/modo de projecao/camera mudou desde o
     * ultimo draw (ver os setters abaixo, que marcam {@link #dirty}).
     * Reaproveita o pass atual quando nada mudou, para que draws consecutivos
     * compativeis sejam batched no mesmo RenderPassNode.
     */
    private static void ensurePass() {
        if (!dirty && currentPass != null) {
            return;
        }

        boolean is3D = state != GameState.UI && renderMode == RenderMode.MODE_3D;

        RenderPassDescriptor descriptor = new RenderPassDescriptor(state + "-" + renderMode)
                .depthTest(is3D)
                .cullFace(true)
                .blend(true)
                .clearColorBuffer(!clearedThisFrame)
                .clearDepthBuffer(!clearedThisFrame);
        if (!clearedThisFrame) {
            descriptor.clearColor(0f, 0f, 0f, 1f);
        }

        RenderView view = new RenderView(windowWidth, windowHeight,
                new Matrix4f(getCurrentProjection()),
                new Matrix4f(getCurrentCamera().getViewMatrix()));

        currentPass = renderer.createPass(descriptor, view);
        clearedThisFrame = true;
        dirty = false;
    }

    // CAMERA

    private static final Camera camera = new Camera();
    private static final Camera uiCamera = new Camera(0, 0, -5);

    public static void setCamera(int x, int y, int z) {
        camera.disableLookAt();
        camera.position.x = x;
        camera.position.y = y;
        camera.position.z = z;
        dirty = true;
    }

    public static void setCamera(int x, int y) {
        camera.disableLookAt();
        camera.position.x = x;
        camera.position.y = y;
        dirty = true;
    }

    public static void rotateCamera(float x, float y) {
        camera.disableLookAt();
        camera.rotation.x = x;
        camera.rotation.y = y;
        dirty = true;
    }

    public static void setCameraZoom(float zoom) {
        camera.scale.x = zoom;
        camera.scale.y = zoom;
        dirty = true;
    }

    public static void setCameraLookAt(float eyeX, float eyeY, float eyeZ,
            float targetX, float targetY, float targetZ,
            float upX, float upY, float upZ) {
        camera.lookAt(eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX, upY, upZ);
        dirty = true;
    }

    private static Camera getCurrentCamera() {
        return state == GameState.UI ? uiCamera : camera;
    }

    // PROJECTIONS

    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f uiProjectionMatrix = new Matrix4f();

    public static void orthographicMode(float width, float height) {
        projectionMatrix.setOrtho(0, width, -height, 0, 1000, -1000);
        uiProjectionMatrix.setOrtho(0, width, -height, 0, 1000, -1000);
        renderMode = RenderMode.MODE_2D;
        dirty = true;
    }

    public static void orthographic3DMode(float size, float aspect) {
        projectionMatrix.setOrtho(-size * aspect, size * aspect, -size, size, -1000f, 1000f);
        renderMode = RenderMode.MODE_3D;
        dirty = true;
    }

    private static final int FOV = 70;
    private static final float Z_FAR = 1000f;
    private static final float Z_NEAR = 1.0f;

    public static void perspectiveMode(int width, int height) {
        float aspectRatio = (float) width / (float) height;
        projectionMatrix.setPerspective((float) Math.toRadians(FOV), aspectRatio, Z_NEAR, Z_FAR);
        renderMode = RenderMode.MODE_3D;
        dirty = true;
    }

    private static Matrix4f getCurrentProjection() {
        return state == GameState.UI ? uiProjectionMatrix : projectionMatrix;
    }

    private static Matrix4f getModelMatrix(Vector3f position, Vector3f rotation, Vector3f scale) {
        Matrix4f translationMatrix = new Matrix4f().translation(position);
        Matrix4f rotationMatrix = new Matrix4f().rotationXYZ(
                (float) Math.toRadians(rotation.x),
                (float) Math.toRadians(rotation.y),
                (float) Math.toRadians(rotation.z));
        Matrix4f scalingMatrix = new Matrix4f().scale(scale);

        return translationMatrix.mul(scalingMatrix.mul(rotationMatrix));
    }

    // SPRITES

    public static Sprite createSprite(String file, int tile, float width, float height) {
        Texture texture = TextureLoader.load(renderer, file);
        Vector2f offset = new Vector2f();
        Vector2f scale = new Vector2f();
        computeTileUV(texture, tile, width, height, offset, scale);
        return new Sprite(texture, offset, scale);
    }

    private static void computeTileUV(Texture texture, int tile, float tileWidth, float tileHeight,
            Vector2f outOffset, Vector2f outScale) {
        float hTiles = texture.getWidth() / tileWidth;
        float vTiles = texture.getHeight() / tileHeight;
        float sx = 1f / hTiles;
        float sy = 1f / vTiles;
        int tx = (int) (tile % hTiles);
        int ty = (int) (tile / hTiles);
        outScale.set(sx, sy);
        outOffset.set(sx * tx, sy * ty);
    }

    public static void drawSprite(Sprite sprite, int x, int y, int width, int height, float rotation) {
        ensurePass();

        Material material = renderer.createMaterial(spriteShader, sprite.getTexture());
        material.setVector2f("uvOffset", sprite.getUvOffset());
        material.setVector2f("uvScale", sprite.getUvScale());

        Matrix4f modelMatrix = getModelMatrix(
                new Vector3f(x + width / 2f, y - height / 2f, 0),
                new Vector3f(0, 0, rotation),
                new Vector3f(width / 2f, height / 2f, 1f));

        currentPass.submit(new RenderCommand(spriteQuadMesh, material, modelMatrix));
    }

    public static void drawSprite(Sprite sprite, int x, int y, int width, int height) {
        drawSprite(sprite, x, y, width, height, 0f);
    }

    public static void drawSprite(Sprite sprite, int tile, int tileWidth, int tileHeight, int x, int y, int width,
            int height, float rotation) {
        Vector2f offset = new Vector2f();
        Vector2f scale = new Vector2f();
        computeTileUV(sprite.getTexture(), tile, tileWidth, tileHeight, offset, scale);
        sprite.setUv(offset, scale);
        drawSprite(sprite, x, y, width, height, rotation);
    }

    public static void drawSprite(Sprite sprite, int tile, int tileWidth, int tileHeight, int x, int y, int width,
            int height) {
        drawSprite(sprite, tile, tileWidth, tileHeight, x, y, width, height, 0f);
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
                throw new RuntimeException("falha ao gravar o arquivo de fontes", e);
            }
        }
        return font;
    }

    private static void createFontsFile() throws IOException {
        try (InputStream in = GameEngine.class.getClassLoader().getResourceAsStream("fonts/" + FONTS_FILE_NAME)) {
            if (in == null) {
                throw new IOException("fonts/" + FONTS_FILE_NAME + " nao encontrado no classpath");
            }
            Files.copy(in, new File(FONTS_FILE_NAME).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
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

    // 3D TEXTURED MODELS

    public static void drawTexturedModel(TexturedModel texturedModel) {
        submitTexturedModel(texturedModel, texturedModel.getModelMatrix());
    }

    public static void drawTexturedModel(TexturedModel texturedModel, float x, float y, float z, float scale,
            float rotX, float rotY, float rotZ) {
        Matrix4f modelMatrix = getModelMatrix(
                new Vector3f(x, y, z),
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(scale, scale, scale));
        submitTexturedModel(texturedModel, modelMatrix);
    }

    private static void submitTexturedModel(TexturedModel texturedModel, Matrix4f modelMatrix) {
        ensurePass();
        currentPass.submit(new RenderCommand(texturedModel.getMesh(), texturedModel.getMaterial(), modelMatrix));
    }

    public static TexturedModel createPlane(String textureName, float width, float height) {
        float[] vertices = {
                -width / 2, height / 2, 0,
                width / 2, height / 2, 0,
                width / 2, -height / 2, 0,
                -width / 2, -height / 2, 0,
        };
        float[] texCoords = { 0, 0, 1, 0, 1, 1, 0, 1 };
        float[] normals = { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 };
        int[] indices = { 0, 3, 2, 2, 1, 0 };

        Mesh mesh = renderer.createMesh(new MeshData(vertices, texCoords, normals, indices));
        Texture texture = TextureLoader.load(renderer, "resources/textures/" + textureName + ".png");
        Material material = createLitMaterial(texture);

        return new TexturedModel(mesh, material);
    }

    public static Material createLitMaterial(Texture texture) {
        Material material = renderer.createMaterial(litShader, texture);
        material.setVector3f("lightDirection", new Vector3f(-1, -1, -1));
        material.setFloat("ambientStrength", 0.2f);
        material.setFloat("diffuseStrength", 0.8f);
        return material;
    }

    public static TexturedModel loadTexturedModel(String fileName) {
        return com.farias.rengine.util.ModelLoader.loadWavefrontOBJ(fileName);
    }
}
