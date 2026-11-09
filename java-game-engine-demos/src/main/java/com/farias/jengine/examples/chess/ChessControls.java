package com.farias.jengine.examples.chess;

import com.farias.jengine.core.EngineContext;
import com.farias.jengine.examples.chess.model.BoardTile;
import com.farias.jengine.examples.chess.model.ChessState;
import com.farias.jengine.examples.chess.model.PlayerInput;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.MouseButton;
import com.farias.jengine.platform.Window;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.Ray;

/**
 * Traduz teclado e mouse do frame: move a câmera ({@link ChessCameraControls})
 * e transforma o cursor e o clique num {@link PlayerInput}. A casa apontada
 * vem do picking no que foi desenhado ({@link ChessRenderer#tileUnder}), que
 * só é consultado.
 */
final class ChessControls {

    private final Input input;
    private final Window window;
    private final IsometricCamera camera;
    private final ChessCameraControls cameraControls;
    private final ChessRenderer renderer;

    /** Resolve em {@code context} o input e a janela. */
    ChessControls(EngineContext context, IsometricCamera camera, ChessRenderer renderer) {
        this.input = context.resolve(Input.class);
        this.window = context.resolve(Window.class);
        this.camera = camera;
        this.cameraControls = new ChessCameraControls(input, camera);
        this.renderer = renderer;
    }

    PlayerInput read(ChessState state, float deltaTime) {
        cameraControls.update(deltaTime);
        Ray ray = camera.rayAt(input.getMouseX(), input.getMouseY(), window);
        BoardTile pointedTile = renderer.tileUnder(state, ray);
        return new PlayerInput(pointedTile, input.isMouseButtonPressed(MouseButton.LEFT));
    }
}
