package com.farias.jengine.renderer;

public final class RenderPassDescriptor {

    private final String name;

    private ClearColor clearColor;

    private boolean clearColorBuffer = true;
    private boolean clearDepthBuffer = true;

    private boolean depthTest = true;
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

    public boolean cullFace() {
        return cullFace;
    }

    public Texture depthAttachment() {
        return depthAttachment;
    }
}
