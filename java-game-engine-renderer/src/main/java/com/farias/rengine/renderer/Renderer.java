package com.farias.rengine.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class Renderer {

    private final OpenGLBackend backend = new OpenGLBackend();
    private boolean initialized;
    private boolean inFrame;
    private long frameIndex;
    private Runnable present = () -> {
    };
    private final List<RenderPassNode> renderPasses = new ArrayList<>();
    private final List<Disposable> resources = new ArrayList<>();

    // Scratch reutilizado a cada draw call (evita uma Matrix3f por comando).
    private final Matrix3f normalMatrix = new Matrix3f();

    public void initialize() {
        if (initialized) {
            return;
        }
        // Configurações globais iniciais (podem ser sobrescritas pelos passes)
        initialized = true;
    }

    public Mesh createMesh(MeshData data) {
        ensureInitialized();
        Mesh mesh = new Mesh(data);
        resources.add(mesh);
        return mesh;
    }

    public Texture createTexture(TextureData data) {
        ensureInitialized();
        Texture texture = new Texture(data);
        resources.add(texture);
        return texture;
    }

    public Texture createDepthTexture(int width, int height) {
        ensureInitialized();
        Texture texture = Texture.createDepthTexture(width, height);
        resources.add(texture);
        return texture;
    }

    public Shader createShader(String vertexSource, String fragmentSource) {
        ensureInitialized();
        Shader shader = new Shader(vertexSource, fragmentSource);
        resources.add(shader);
        return shader;
    }

    public Material createMaterial(Shader shader) {
        return new Material(shader);
    }

    public Material createMaterial(Shader shader, Texture texture) {
        Material material = createMaterial(shader);
        if (texture != null) {
            material.setTexture("uTexture", texture, 0);
        }
        return material;
    }

    /**
     * Cria um passe e o enfileira no frame corrente. A fila e limpa em
     * {@link #beginFrame()}, entao passes devem ser criados a cada frame.
     */
    public RenderPassNode createPass(RenderPassDescriptor descriptor, RenderView view) {
        ensureInitialized();
        if (!inFrame) {
            throw new IllegalStateException(
                    "createPass called outside a frame: call beginFrame() first (passes created earlier are discarded)");
        }
        var node = new RenderPassNode(descriptor, view);
        renderPasses.add(node);
        return node;
    }

    public void beginFrame() {
        ensureInitialized();
        if (inFrame) {
            throw new IllegalStateException("beginFrame called twice without endFrame");
        }
        this.renderPasses.clear();
        inFrame = true;
        frameIndex++;
    }

    public void endFrame() {
        ensureInitialized();
        if (!inFrame) {
            throw new IllegalStateException("endFrame called without beginFrame");
        }

        // Renderiza cada passe sequencialmente
        for (RenderPassNode pass : renderPasses) {
            executePass(pass);
        }

        inFrame = false;
        present.run();
    }

    /**
     * Define o que apresenta o frame ao fim de {@link #endFrame()} (ex.: trocar
     * os buffers da janela). O renderer nao conhece a janela: quem o monta
     * liga isso. Por padrao nao faz nada.
     */
    public void setPresent(Runnable present) {
        this.present = Objects.requireNonNull(present, "present");
    }

    /** true entre beginFrame() e endFrame(). */
    public boolean inFrame() {
        return inFrame;
    }

    /**
     * Contador que incrementa a cada beginFrame(); permite a quem guarda estado
     * por frame (pools, caches) saber que um novo frame comecou.
     */
    public long frameIndex() {
        return frameIndex;
    }

    private void executePass(RenderPassNode pass) {
        RenderView renderView = pass.view();
        List<RenderCommand> commands = pass.commands();

        // Se o passe não tem câmera ou comandos, pula
        if (renderView == null || commands.isEmpty()) {
            return;
        }

        // 1. Inicializa o estado do passe (Clear, Framebuffer bind, etc.)
        backend.beginPass(pass.descriptor(), renderView);

        // 2. Loop de renderização dos comandos do passe
        Material lastMaterial = null;

        for (RenderCommand command : commands) {
            Material currentMaterial = command.material();
            Shader currentShader = currentMaterial.shader();

            if (currentMaterial != lastMaterial) {
                currentMaterial.bind();

                currentMaterial.applyUniforms();

                currentShader.setMatrix4f("uView", renderView.viewMatrix());
                currentShader.setMatrix4f("uProjection", renderView.projectionMatrix());

                lastMaterial = currentMaterial;
            }

            Matrix4f modelMatrix = command.modelMatrix();
            modelMatrix.normal(normalMatrix);
            currentShader.setMatrix4f("uModel", modelMatrix);
            currentShader.setMatrix3f("uNormal", normalMatrix);

            command.mesh().draw();
        }

        if (lastMaterial != null) {
            lastMaterial.unbind();
        }

        // 3. Finaliza o passe
        backend.endPass();
    }

    public void dispose() {
        for (Disposable resource : resources) {
            if (resource != null) {
                resource.dispose();
            }
        }
        resources.clear();
        renderPasses.clear();
        inFrame = false;
        backend.dispose();
        initialized = false;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Renderer not initialized");
        }
    }
}
