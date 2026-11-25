package com.farias.jengine.renderer;

/**
 * O estado de um passe: o que limpar, o teste de profundidade (ligado, a comparação e se grava), descarte de faces,
 * blend e o alvo de profundidade. Os padrões são os de uma cena comum: limpa cor e profundidade, testa com
 * {@link DepthCompare#LESS} e grava a profundidade, descarta as faces de trás, sem blend.
 */
public final class RenderPassDescriptor {

    private final String name;

    private ClearColor clearColor;

    private boolean clearColorBuffer = true;
    private boolean clearDepthBuffer = true;

    private boolean depthTest = true;
    private DepthCompare depthCompare = DepthCompare.LESS;
    private boolean depthWrite = true;
    private boolean cullFace = true;
    private boolean blend = false;
    private Texture depthAttachment;

    public RenderPassDescriptor(String name) {
        this.name = name;
    }

    public RenderPassDescriptor clearColor(
        float r,
        float g,
        float b,
        float a
    ) {
        this.clearColor = new ClearColor(r, g, b, a);
        return this;
    }

    public RenderPassDescriptor depthTest(boolean value) {
        this.depthTest = value;
        return this;
    }

    /** Como o teste de profundidade compara (vale só com o teste ligado). */
    public RenderPassDescriptor depthCompare(DepthCompare value) {
        if (value == null) {
            throw new IllegalArgumentException("Depth compare must not be null");
        }
        this.depthCompare = value;
        return this;
    }

    /**
     * Se os fragmentos que passam gravam a profundidade. Desligada, o passe só lê a profundidade dos anteriores (um
     * overlay que não deve esconder nada). Com a escrita desligada, o clear de profundidade do passe não acontece.
     */
    public RenderPassDescriptor depthWrite(boolean value) {
        this.depthWrite = value;
        return this;
    }

    public RenderPassDescriptor cullFace(boolean value) {
        this.cullFace = value;
        return this;
    }

    public RenderPassDescriptor blend(boolean value) {
        this.blend = value;
        return this;
    }

    public boolean blend() {
        return blend;
    }

    public RenderPassDescriptor depthAttachment(Texture texture) {
        if (texture != null && !texture.isDepthTexture()) {
            throw new IllegalArgumentException("Render pass depth attachment must be a depth texture");
        }
        this.depthAttachment = texture;
        return this;
    }

    public String name() {
        return name;
    }

    public ClearColor clearColor() {
        return clearColor;
    }

    public RenderPassDescriptor clearColorBuffer(boolean value) {
        this.clearColorBuffer = value;
        return this;
    }

    public boolean clearColorBuffer() {
        return clearColorBuffer;
    }

    public RenderPassDescriptor clearDepthBuffer(boolean value) {
        this.clearDepthBuffer = value;
        return this;
    }

    public boolean clearDepthBuffer() {
        return clearDepthBuffer;
    }

    public boolean depthTest() {
        return depthTest;
    }

    public DepthCompare depthCompare() {
        return depthCompare;
    }

    public boolean depthWrite() {
        return depthWrite;
    }

    public boolean cullFace() {
        return cullFace;
    }

    public Texture depthAttachment() {
        return depthAttachment;
    }
}
