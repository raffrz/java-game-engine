package com.farias.rengine.renderer;

public interface RenderBackend {
    
    void beginPass(RenderPassDescriptor descriptor);

    void endPass();
}
