package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;

import org.joml.Matrix4f;

import com.farias.rengine.engine3d.Game;
import com.farias.rengine.engine3d.Camera;
import com.farias.rengine.toolkit2d.legacy.BitmapFont;
import com.farias.rengine.engine3d.Projections;
import com.farias.rengine.toolkit2d.legacy.Projections2D;
import com.farias.rengine.engine3d.Shaders;
import com.farias.rengine.toolkit2d.legacy.SpriteBatch2D;
import com.farias.rengine.engine3d.TexturedModel;
import com.farias.rengine.renderer.RenderCommand;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.engine3d.ModelLoader;

public class ModelViewer extends Game {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;
    private static final int FONT_SIZE = 20;
    private static final float CAMERA_SPEED = 5.0f;
    private static final float ROTATION_SPEED = 16f;

    private final Camera camera = new Camera();
    private TexturedModel earth;
    private TexturedModel clouds;

    private SpriteBatch2D batch;
    private BitmapFont font;
    private RenderPassDescriptor scene3DDescriptor;
    private RenderPassDescriptor uiDescriptor;

    // ponto observado pela câmera
    private float targetX = 0;
    private float targetY = 0;
    private float targetZ = 0;

    // distância da câmera (terra tem raio ~1, entao perto o bastante pra preencher a tela)
    private float cameraDistance = 3f;

    public ModelViewer() {
        super("Model Viewer - Isometric", WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    @Override
    protected void onCreate() {
        Shader litShader = renderer.createShader(Shaders.LIT_VERTEX, Shaders.LIT_FRAGMENT);
        earth = ModelLoader.loadWavefrontOBJ("earth", renderer, litShader);
        clouds = ModelLoader.loadWavefrontOBJ("clouds", renderer, litShader);

        batch = new SpriteBatch2D(renderer);
        font = new BitmapFont(batch);

        scene3DDescriptor = new RenderPassDescriptor("model-viewer-3d")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(true)
                .cullFace(true)
                .blend(true);

        uiDescriptor = new RenderPassDescriptor("model-viewer-ui")
                .clearColorBuffer(false)
                .clearDepthBuffer(false)
                .depthTest(false)
                .cullFace(true)
                .blend(true);

        updateIsometricCamera();
    }

    private void updateIsometricCamera() {
        camera.lookAt(
                targetX + cameraDistance,
                targetY + cameraDistance,
                targetZ + cameraDistance,

                targetX,
                targetY,
                targetZ,

                0, 1, 0);
    }

    @Override
    protected void onUpdate(float deltaTime) {

        // rotação da terra
        earth.getRotation().y -= 0.7f * deltaTime * ROTATION_SPEED;

        if (earth.getRotation().y < -360f) {
            earth.getRotation().y += 360f;
        }

        // rotação das nuvens
        clouds.getRotation().y -= 1.0f * deltaTime * ROTATION_SPEED;

        if (clouds.getRotation().y < -360f) {
            clouds.getRotation().y += 360f;
        }

        // tamanho da atmosfera
        if (input.isKeyDown(GLFW_KEY_A)) {

            if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                clouds.getScale().sub(0.1f, 0.1f, 0.1f);
            } else {
                clouds.getScale().add(0.1f, 0.1f, 0.1f);
            }
        }

        float move = CAMERA_SPEED * deltaTime;

        /*
         * Movimento isométrico
         *
         * ↑ = nordeste
         * ↓ = sudoeste
         * ← = noroeste
         * → = sudeste
         */

        if (input.isKeyDown(GLFW_KEY_UP)) {
            targetX += move;
            targetZ += move;
        }

        if (input.isKeyDown(GLFW_KEY_DOWN)) {
            targetX -= move;
            targetZ -= move;
        }

        if (input.isKeyDown(GLFW_KEY_LEFT)) {
            targetX -= move;
            targetZ += move;
        }

        if (input.isKeyDown(GLFW_KEY_RIGHT)) {
            targetX += move;
            targetZ -= move;
        }

        // sobe/desce a câmera
        if (input.isKeyDown(GLFW_KEY_UP)) {
            targetY += move;
        }

        if (input.isKeyDown(GLFW_KEY_DOWN)) {
            targetY -= move;
        }

        // zoom (aproxima/afasta a câmera do alvo)
        if (input.isKeyDown(GLFW_KEY_Z)) {

            if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                cameraDistance += 2f * deltaTime * CAMERA_SPEED;
            } else {
                cameraDistance -= 2f * deltaTime * CAMERA_SPEED;
            }

            if (cameraDistance < 2f) {
                cameraDistance = 2f;
            }
        }

        updateIsometricCamera();
    }

    @Override
    protected void onRender(float deltaTime) {
        // Renderização 3D em perspectiva (era ortográfica/isométrica)
        Matrix4f projection3D = Projections.perspective(WINDOW_WIDTH, WINDOW_HEIGHT);
        RenderView view3D = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                projection3D, camera.getViewMatrix());
        RenderPassNode scenePass = renderer.createPass(scene3DDescriptor, view3D);

        scenePass.submit(new RenderCommand(earth.getMesh(), earth.getMaterial(), earth.getModelMatrix()));
        scenePass.submit(new RenderCommand(clouds.getMesh(), clouds.getMaterial(), clouds.getModelMatrix()));

        // UI
        Matrix4f projectionUI = Projections2D.orthographic2D(WINDOW_WIDTH, WINDOW_HEIGHT);
        RenderView viewUI = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                projectionUI, new Matrix4f());
        RenderPassNode uiPass = renderer.createPass(uiDescriptor, viewUI);

        font.drawText(uiPass, "ISOMETRIC VIEW", WINDOW_WIDTH / 2, -20, FONT_SIZE, FONT_SIZE);
        font.drawText(uiPass, "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT", WINDOW_WIDTH / 2, -50, 14, 14);
    }

    public static void launch() {
        new ModelViewer().run();
    }

    public static void main(String[] args) {
        launch();
    }
}
