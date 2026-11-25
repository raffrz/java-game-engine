package com.farias.jengine.examples.animation;

import java.nio.file.Path;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.jengine.animation.AnimationClip;
import com.farias.jengine.animation.Animator;
import com.farias.jengine.animation.Pose;
import com.farias.jengine.animation.ik.FabrikIk;
import com.farias.jengine.animation.ik.LookAtIk;
import com.farias.jengine.animation.ik.TwoBoneIk;
import com.farias.jengine.assets.Assets;
import com.farias.jengine.assets.AssetsModule;
import com.farias.jengine.core.Engine;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Timer;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;
import com.farias.jengine.platform.Window;
import com.farias.jengine.platform.glfw.GlfwPlatformModule;
import com.farias.jengine.renderer.RenderPass;
import com.farias.jengine.renderer.RenderPassDescriptor;
import com.farias.jengine.renderer.RenderView;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.RendererModule;
import com.farias.jengine.toolkit2d.Toolkit2D;
import com.farias.jengine.toolkit2d.Toolkit2DModule;
import com.farias.jengine.toolkit2d.Ui;
import com.farias.jengine.toolkit3d.Camera;
import com.farias.jengine.toolkit3d.DirectionalLight;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.ModelAsset;
import com.farias.jengine.toolkit3d.Models;
import com.farias.jengine.toolkit3d.Picking;
import com.farias.jengine.toolkit3d.Projections;
import com.farias.jengine.toolkit3d.Ray;
import com.farias.jengine.toolkit3d.ShadowMap;
import com.farias.jengine.toolkit3d.Shadows;
import com.farias.jengine.toolkit3d.Shapes;
import com.farias.jengine.toolkit3d.SkinnedModel;
import com.farias.jengine.toolkit3d.TexturedModel;
import com.farias.jengine.toolkit3d.Toolkit3DModule;

/**
 * Animação esquelética e IK: a raposa do glTF ({@code models/fox.glb}) toca
 * os clips do arquivo com crossfade, e um alvo que segue o mouse é perseguido
 * por três solvers de IK aplicados sobre a pose animada: a pata dianteira
 * (two-bone), a cabeça (look-at) e um tentáculo gerado em código (FABRIK).
 *
 * <p>
 * Controles: 1-3 trocam o clip, ESPAÇO pausa, P/L/T ligam e desligam pata,
 * olhar e tentáculo, Q/E sobem e descem o alvo, setas giram a câmera, roda =
 * zoom, Esc sai.
 */
public final class AnimationDemo {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 800;
    private static final float CROSS_FADE_SECONDS = 0.3f;
    /** O Fox está em centímetros. */
    private static final float FOX_SCALE = 0.02f;
    /**
     * Quanto o pole do cotovelo fica atrás do cotovelo (-Z no espaço do
     * modelo, em centímetros do Fox): a pata dianteira dobra para trás.
     */
    private static final float ELBOW_POLE_BEHIND = 40f;

    // juntas do Fox usadas pelo IK
    private static final String SHOULDER = "b_LeftUpperArm_09";
    private static final String ELBOW = "b_LeftForeArm_010";
    private static final String PAW = "b_LeftHand_011";
    private static final String HEAD = "b_Head_05";

    private static final int TENTACLE_SEGMENTS = 8;
    private static final float TENTACLE_SEGMENT_LENGTH = 0.22f;

    private final Window window;
    private final Input input;
    private final Renderer renderer;
    private final Ui ui;

    private final ModelAsset fox;
    private final SkinnedModel foxModel;
    private final Animator animator;
    private final Pose foxPose;
    private final int shoulder;
    private final int elbow;
    private final int paw;
    private final int head;
    private final Vector3f headForward;

    private final SkinnedModel tentacle;
    private final Pose tentaclePose;
    private final int[] tentacleChain = Tentacle.chain(TENTACLE_SEGMENTS);

    private final TexturedModel ground;
    private final TexturedModel marker;
    private final ShadowMap shadow;

    private final TwoBoneIk pawIk = new TwoBoneIk();
    private final LookAtIk lookAt = new LookAtIk();
    private final FabrikIk fabrik = new FabrikIk();

    private final Camera camera = new Camera();
    private final RenderPassDescriptor sceneDescriptor = new RenderPassDescriptor("animation-scene")
            .clearColor(0.18f, 0.22f, 0.27f, 1f)
            .depthTest(true)
            .cullFace(false)
            .blend(false);

    private final Vector3f target = new Vector3f(0.6f, 0.45f, 1.2f);
    private final Vector3f modelTarget = new Vector3f();
    private final Vector3f elbowPosition = new Vector3f();
    private float cameraYaw = (float) Math.toRadians(35);
    private float cameraDistance = 4.5f;
    private boolean paused;
    private boolean pawOn = true;
    private boolean lookOn = true;
    private boolean tentacleOn = true;

    private AnimationDemo(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(Input.class);
        renderer = context.require(Renderer.class);
        ui = context.require(Toolkit2D.class).ui(WINDOW_WIDTH, WINDOW_HEIGHT);
        LitMaterials materials = context.require(LitMaterials.class);
        Assets assets = context.require(Assets.class);
        Shapes shapes = context.require(Shapes.class);

        DirectionalLight light = DirectionalLight.covering(new Vector3f(0.5f, 2f, 1.2f), new Vector3f(0, 0.5f, 0), 3f);
        shadow = context.require(Shadows.class).create(light, 2048);

        fox = context.require(Models.class).loadGltf("models/fox.glb");
        foxModel = fox.instance();
        foxModel.getScale().set(FOX_SCALE);
        foxModel.lit(0).receiving(shadow);
        animator = new Animator(fox.skeleton()).play(fox.clips().get(0), true);
        foxPose = new Pose(fox.skeleton());
        shoulder = joint(SHOULDER);
        elbow = joint(ELBOW);
        paw = joint(PAW);
        head = joint(HEAD);
        headForward = localAxis(foxPose, head, new Vector3f(0, 0, 1));

        tentacle = new SkinnedModel(
                renderer.createMesh(Tentacle.mesh(TENTACLE_SEGMENTS, TENTACLE_SEGMENT_LENGTH, 0.12f, 0.025f)),
                materials.createSkinned(assets.solidColor(0.55f, 0.25f, 0.7f, 1f)).receiving(shadow)
                        .glossy(0.4f, 32f, new Vector3f(1, 1, 1).normalize()),
                Tentacle.skeleton(TENTACLE_SEGMENTS, TENTACLE_SEGMENT_LENGTH));
        tentacle.getPosition().set(-1.3f, 0f, 0.5f);
        tentaclePose = new Pose(tentacle.skeleton());

        ground = new TexturedModel(shapes.floor(), materials.create(assets.solidColor(0.42f, 0.5f, 0.38f, 1f)).receiving(shadow));
        ground.getScale().set(8f, 1f, 8f);
        marker = new TexturedModel(shapes.cube(), materials.create(assets.solidColor(0.95f, 0.2f, 0.15f, 1f)));
        marker.getScale().set(0.08f);
    }

    private int joint(String name) {
        int index = fox.skeleton().indexOf(name);
        if (index < 0) {
            throw new IllegalStateException("fox.glb has no joint '" + name + "'");
        }
        return index;
    }

    /** O eixo {@code modelAxis} (espaço do modelo, na pose de repouso) escrito no espaço da junta. */
    private static Vector3f localAxis(Pose rest, int joint, Vector3f modelAxis) {
        rest.computeModelSpace();
        Quaternionf rotation = rest.modelSpace(joint).getNormalizedRotation(new Quaternionf());
        return rotation.conjugate().transform(modelAxis).normalize();
    }

    private void run() {
        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            window.pollEvents();
            input.update();
            timer.update();
            float deltaTime = Math.min(timer.deltaTime(), 0.1f);

            handleKeys(deltaTime);
            RenderView view = view();
            followMouse(view);
            animate(deltaTime);

            renderer.beginFrame();
            render(view);
            renderer.endFrame();
        }
    }

    private void handleKeys(float deltaTime) {
        List<AnimationClip> clips = fox.clips();
        Key[] clipKeys = { Key.NUM_1, Key.NUM_2, Key.NUM_3 };
        for (int i = 0; i < clipKeys.length && i < clips.size(); i++) {
            if (input.isKeyPressed(clipKeys[i]) && animator.current() != clips.get(i)) {
                animator.crossFade(clips.get(i), true, CROSS_FADE_SECONDS);
            }
        }
        if (input.isKeyPressed(Key.SPACE)) {
            paused = !paused;
            animator.speed(paused ? 0f : 1f);
        }
        pawOn ^= input.isKeyPressed(Key.P);
        lookOn ^= input.isKeyPressed(Key.L);
        tentacleOn ^= input.isKeyPressed(Key.T);
        if (input.isKeyDown(Key.Q)) {
            target.y = Math.min(target.y + deltaTime, 2f);
        }
        if (input.isKeyDown(Key.E)) {
            target.y = Math.max(target.y - deltaTime, 0.05f);
        }
        if (input.isKeyDown(Key.LEFT)) {
            cameraYaw -= 1.5f * deltaTime;
        }
        if (input.isKeyDown(Key.RIGHT)) {
            cameraYaw += 1.5f * deltaTime;
        }
        cameraDistance = Math.max(1.5f, Math.min(10f, cameraDistance - (float) input.getScrollY() * 0.3f));
    }

    private RenderView view() {
        camera.lookAt(
                (float) Math.sin(cameraYaw) * cameraDistance, cameraDistance * 0.55f,
                (float) Math.cos(cameraYaw) * cameraDistance,
                0f, 0.6f, 0f,
                0f, 1f, 0f);
        return new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections.perspective(window.getWidth(), Math.max(window.getHeight(), 1)), camera.getViewMatrix());
    }

    /** O alvo fica onde o raio do mouse cruza o plano horizontal na altura dele. */
    private void followMouse(RenderView view) {
        if (window.getWidth() <= 0 || window.getHeight() <= 0) {
            return;
        }
        Vector2f ndc = Picking.toNdc(input.getMouseX(), input.getMouseY(), window.getWidth(), window.getHeight());
        Ray ray = Picking.ray(view, ndc);
        float distance = Picking.rayPlaneY(ray, target.y);
        if (!Float.isNaN(distance)) {
            Vector3f hit = ray.pointAt(distance);
            target.x = Math.max(-4f, Math.min(4f, hit.x));
            target.z = Math.max(-4f, Math.min(4f, hit.z));
        }
        marker.getPosition().set(target);
    }

    /** Clip -> cópia da pose -> IK por cima -> palette. O animator fica com a pose limpa. */
    private void animate(float deltaTime) {
        animator.update(deltaTime);
        foxPose.set(animator.pose());
        foxModel.toModelSpace(target, modelTarget);
        if (pawOn) {
            foxPose.computeModelSpace().modelSpace(elbow).getTranslation(elbowPosition)
                    .add(0f, 0f, -ELBOW_POLE_BEHIND);
            pawIk.solve(foxPose, shoulder, elbow, paw, modelTarget, elbowPosition, 1f);
        }
        if (lookOn) {
            lookAt.solve(foxPose, head, headForward, modelTarget, (float) Math.toRadians(70), 1f);
        }
        foxModel.update(foxPose);

        if (tentacleOn) {
            // a pose do tentáculo persiste entre frames: o FABRIK parte de onde parou
            fabrik.solve(tentaclePose, tentacleChain, tentacle.toModelSpace(target, modelTarget), 10, 1e-3f, 1f);
        } else {
            tentaclePose.setToRest();
        }
        tentacle.update(tentaclePose);
    }

    private void render(RenderView view) {
        RenderPass shadowPass = renderer.createPass(shadow.descriptor(), shadow.view());
        shadow.casters(foxModel).forEach(shadowPass::submit);
        shadow.casters(tentacle).forEach(shadowPass::submit);
        shadowPass.submit(shadow.caster(marker));

        RenderPass scene = renderer.createPass(sceneDescriptor, view);
        scene.submit(ground.command());
        scene.submit(marker.command());
        foxModel.commands().forEach(scene::submit);
        tentacle.commands().forEach(scene::submit);

        ui.begin();
        ui.text(ui.defaultFont(), "ANIMACAO E IK - CLIP " + animator.current().name().toUpperCase()
                + (paused ? " (PAUSADO)" : ""), 16, 16, 14);
        ui.text(ui.defaultFont(), "PATA (P): " + onOff(pawOn) + "   OLHAR (L): " + onOff(lookOn) + "   TENTACULO (T): "
                + onOff(tentacleOn) + "   ALVO Y: " + String.format("%.2f", target.y), 16, 38, 11);
        ui.text(ui.defaultFont(), "MOUSE = ALVO | 1-3 = CLIP | ESPACO = PAUSA | Q/E = ALTURA | SETAS = GIRAR | RODA = ZOOM",
                16, WINDOW_HEIGHT - 28, 10);
    }

    private static String onOff(boolean on) {
        return on ? "LIGADO" : "DESLIGADO";
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new GlfwPlatformModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Animacao e IK"))
                .registerModule(new RendererModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Toolkit3DModule())
                .registerModule(new Toolkit2DModule());
        EngineContext context = engine.start();
        try {
            new AnimationDemo(context).run();
        } finally {
            engine.terminate();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
