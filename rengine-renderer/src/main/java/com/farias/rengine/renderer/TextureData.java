package com.farias.rengine.renderer;

import java.nio.ByteBuffer;

public record TextureData(int width, int height, ByteBuffer pixels) {
    
}
