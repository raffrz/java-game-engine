package com.farias.jengine.examples.chess;

import java.nio.file.Path;
import java.util.stream.Stream;

import com.farias.jengine.examples.chess.model.ChessBoard;
import com.farias.jengine.examples.chess.model.ChessLogic;
import com.farias.jengine.examples.chess.model.ChessPiece;
import com.farias.jengine.examples.chess.model.ChessState;
import com.farias.jengine.examples.chess.model.PlayerInput;
import com.farias.jengine.assets.Assets;
import com.farias.jengine.assets.AssetsModule;
import com.farias.jengine.core.Engine;
import com.farias.jengine.core.EngineContext;
import com.farias.jengine.core.Timer;
import com.farias.jengine.toolkit3d.Toolkit3DModule;
import com.farias.jengine.toolkit3d.IsometricCamera;
import com.farias.jengine.toolkit3d.LitMaterials;
import com.farias.jengine.toolkit3d.Models;
import com.farias.jengine.toolkit3d.Shadows;
import com.farias.jengine.toolkit3d.Shapes;
import com.farias.jengine.platform.Input;
import com.farias.jengine.platform.Key;
import com.farias.jengine.platform.Window;
import com.farias.jengine.platform.glfw.GlfwPlatformModule;
import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.RendererModule;
import com.farias.jengine.toolkit2d.Toolkit2D;
import com.farias.jengine.toolkit2d.Toolkit2DModule;

/**
 * Xadrez isométrico: monta as peças e roda o loop principal. Cada frame tem
 * três passos, cada um com um dono:
 * <ol>
 * <li><b>input</b>: {@link ChessControls} move a câmera e traduz o mouse num
 * {@link PlayerInput} (a casa apontada e se clicou);</li>
 * <li><b>lógica</b>: {@link ChessLogic#update} aplica as regras, avança as
 * animações e atualiza o {@link ChessState};</li>
 * <li><b>render</b>: {@link ChessRenderer#render} lê o estado e desenha.</li>
 * </ol>
 *
 * <h2>Quem faz o quê</h2>
 * <ul>
 * <li>pacote {@code model}: {@link ChessState} (o jogo num instante, inclusive
 * a pose animada de cada peça), {@link ChessLogic} (regras e animações),
 * {@link ChessBoard}, {@link ChessPiece} e as peças em {@code model.pieces}
 * (o padrão de movimento de cada uma). Não depende da engine.</li>
 * <li>{@link ChessControls} e {@link ChessCameraControls}: teclado e mouse.</li>
 * <li>{@link ChessScene}: mapa de sombras e modelos 3D, só dados, criados
 * por {@link ChessSceneFactory}, que também configura a câmera.</li>
 * <li>{@link ChessRenderer} e {@link ModelOutline}: posicionam os modelos
 * conforme o estado, picking e passes de render.</li>
 * </ul>
 *
 * <p>
 * As animações são do estado, mas não bloqueiam o jogo: dá para jogar a
 * próxima peça com a capturada ainda no ar.
 *
 * <h2>Controles</h2>
 * Setas esquerda/direita movem a câmera para os lados da tela, cima/baixo
 * sobem e descem, a roda do mouse aproxima e afasta, e arrastar na horizontal
 * com o botão do meio gira a câmera em volta do centro do tabuleiro. Passar o
 * mouse sobre uma peça desenha um contorno branco em volta dela. O clique
 * esquerdo seleciona a peça, que sobe e fica girando; clicar de novo nela, ou
 * fora de qualquer peça, desfaz a seleção. Com uma peça selecionada, passar o
 * mouse sobre um destino válido destaca (tint) o caminho até ele, junto com a
 * casa de origem, e clicar nele move a peça (capturando a adversária que
 * estiver lá). ESC fecha.
 *
 * <h2>Picking</h2>
 * {@link IsometricCamera#rayAt} gera o raio da mesma câmera que desenha a
 * cena, e cada peça é testada com a matriz de modelo do próprio modelo, a mesma
 * usada para desenhá-la; por isso a caixa acompanha a peça elevada e girando.
 *
 * <h2>Sombras</h2>
 * A luz que gera o mapa de sombras é a mesma do sombreamento difuso; casas e
 * peças recebem sombra, só as peças projetam.
 */
public class IsometricChess {

    private static final int WINDOW_WIDTH = 960;
    private static final int WINDOW_HEIGHT = 960;

    private final Window window;
    private final Input input;

    private final ChessState state = new ChessState();
    private final ChessLogic logic = new ChessLogic();
    private final ChessControls controls;
    private final ChessRenderer renderer;

    public static void main(String[] args) {
        launch();
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new GlfwPlatformModule(WINDOW_WIDTH, WINDOW_HEIGHT, "Isometric Chess"))
                .registerModule(new RendererModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Toolkit3DModule())
                .registerModule(new Toolkit2DModule());

        EngineContext context = engine.start();
        try {
            new IsometricChess(context).run();
        } finally {
            engine.terminate();
        }
    }

    public IsometricChess(EngineContext context) {
        requireServices(context);
        window = context.require(Window.class);
        input = context.require(Input.class);

        IsometricCamera camera = ChessSceneFactory.camera();
        ChessScene scene = ChessSceneFactory.create(context, state, camera);
        renderer = new ChessRenderer(context, scene, camera);
        controls = new ChessControls(context, camera, renderer);
    }

    /**
     * Falha cedo, com o nome do serviço, se algum módulo de que as partes do
     * jogo dependem não foi registrado em launch(). As partes buscam os
     * serviços com resolve e não fazem essa checagem.
     */
    private static void requireServices(EngineContext context) {
        Stream.of(Assets.class, Shapes.class, LitMaterials.class, Models.class, Shadows.class,
                Renderer.class, Toolkit2D.class).forEach(context::require);
    }

    private void run() {
        Timer timer = new Timer();
        while (!shouldQuit()) {
            float deltaTime = beginFrame(timer);
            PlayerInput playerInput = controls.read(state, deltaTime);
            logic.update(state, playerInput, deltaTime);
            renderer.render(state);
        }
    }

    /** Avança o relógio e amostra os eventos e o input do frame; devolve o deltaTime. */
    private float beginFrame(Timer timer) {
        timer.update();
        window.pollEvents();
        input.update();
        return timer.deltaTime();
    }

    private boolean shouldQuit() {
        return window.shouldClose() || input.isKeyPressed(Key.ESCAPE);
    }
}
