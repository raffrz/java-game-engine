package com.farias.rengine.renderer;

public interface RenderBackend {
    
    void beginPass(RenderPassDescriptor descriptor, RenderView view);

    void endPass();

    default void dispose() {
    }
}
