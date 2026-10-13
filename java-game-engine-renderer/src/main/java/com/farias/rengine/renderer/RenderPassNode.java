package com.farias.rengine.renderer;

import java.util.ArrayList;
import java.util.List;

public final class RenderPassNode {

    private final RenderPassDescriptor descriptor;
    private final RenderView view;
    private final List<RenderCommand> commands = new ArrayList<>();

    RenderPassNode(RenderPassDescriptor descriptor, RenderView view) {
        this.descriptor = descriptor;
        this.view = view;
    }

    public void submit(RenderCommand cmd) {
        commands.add(cmd);
    }

    public RenderPassDescriptor descriptor() {
        return descriptor;
    }

    public List<RenderCommand> commands() {
        return commands;
    }

    public RenderView view() {
        return view;
    }
}