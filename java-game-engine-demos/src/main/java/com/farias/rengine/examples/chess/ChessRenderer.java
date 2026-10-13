package com.farias.rengine.examples.chess;

import com.farias.rengine.engine3d.IsometricCamera;
import com.farias.rengine.engine3d.ShadowMap;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.ui.Ui;

/**
 * Monta os passes de um frame: sombra, cena 3D e UI. Os passes executam na
 * ordem de criação, então a UI vem depois da cena.
 *
 * <p>
 * O passe 3D não usa culling: os .obj das peças têm winding e normais
 * invertidos (ver CHESS_MODELS.md), por isso o carregamento usa
 * {@code flipNormals} e, sem blend, o depth test sozinho escolhe a face
 * visível. Só as peças projetam sombra.
 */
final class ChessRenderer {

    private static final int TITLE_FONT_SIZE = 20;
    private static final int HINT_FONT_SIZE = 14;

    private final Renderer renderer;
    private final Ui ui;
    private final Window window;
    private final int uiWidth;
    private final IsometricCamera camera;
    private final ShadowMap shadow;
    private final RenderPassDescriptor scene3DDescriptor = new RenderPassDescriptor("isometric-chess-3d")
            .clearColor(0f, 0f, 0f, 1f)
            .depthTest(true)
            .cullFace(false)
            .blend(false);

    ChessRenderer(Renderer renderer, Ui ui, Window window, int uiWidth, IsometricCamera camera,
            ShadowMap shadow) {
        this.renderer = renderer;
        this.ui = ui;
        this.window = window;
        this.uiWidth = uiWidth;
        this.camera = camera;
        this.shadow = shadow;
    }

    void render(ChessScene scene) {
        renderer.beginFrame();
        drawShadows(scene);
        drawScene(scene);
        drawUi();
        renderer.endFrame();
    }

    private void drawShadows(ChessScene scene) {
        RenderPassNode pass = renderer.createPass(shadow.descriptor(), shadow.view());
        scene.pieceModels().forEach(model -> pass.submit(shadow.caster(model)));
    }

    private void drawScene(ChessScene scene) {
        RenderPassNode pass = renderer.createPass(scene3DDescriptor, camera.view(window));
        scene.tiles().forEach(tile -> pass.submit(tile.command()));
        scene.pieceModels().forEach(model -> pass.submit(model.command()));
    }

    private void drawUi() {
        int centerX = uiWidth / 2;
        ui.begin();
        ui.textCentered(ui.defaultFont(), "ISOMETRIC CHESS - LWJGL RENDER ENGINE", centerX, 20,
                TITLE_FONT_SIZE);
        ui.textCentered(ui.defaultFont(), "ARROWS = MOVE | Z = ZOOM | SHIFT+Z = ZOOM OUT | CLICK = SELECT",
                centerX, 50, HINT_FONT_SIZE);
    }
}
