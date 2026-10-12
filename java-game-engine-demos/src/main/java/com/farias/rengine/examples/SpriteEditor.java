package com.farias.rengine.examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.nio.file.Path;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.assets.AssetsModule;
import com.farias.rengine.engine.Engine;
import com.farias.rengine.engine.EngineContext;
import com.farias.rengine.engine.Timer;
import com.farias.rengine.engine.modules.InputModule;
import com.farias.rengine.renderer.RenderModule;
import com.farias.rengine.engine.modules.WindowModule;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Key;
import com.farias.rengine.io.MouseButton;
import com.farias.rengine.io.Window;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.toolkit2d.BitmapFont;
import com.farias.rengine.toolkit2d.Canvas;
import com.farias.rengine.toolkit2d.SpriteSheet;
import com.farias.rengine.toolkit2d.Toolkit2DModule;

public class SpriteEditor {

    private static final int WINDOW_SIZE = 512;
    private static final int VIRTUAL_SIZE = 128;

    // 8x8 draw area
    static int[] draw = new int[] {
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
            8, 8, 8, 8, 8, 8, 8, 8,
    };

    // 16 colors
    static int[] colors = new int[] {
            0x000000, 0x0743ac, 0x3d7ef0, 0xcc3030, 0xf4abab, 0xd08a8a, 0x898989, 0x6c0be6,
            0xffffff, 0x3a8216, 0x73bb4f, 0x744531, 0x592d1b, 0xf1c71a, 0xe6970b, 0x411350
    };

    private final Window window;
    private final InputSystem input;
    private final Renderer renderer;
    private final Canvas canvas;
    private final BitmapFont font;
    // 32x32 tiles: uma cor da paleta por tile
    private final SpriteSheet palleteTiles;
    // a paleta inteira como um unico tile (barra de cores)
    private final SpriteSheet palleteBar;

    private int hover_x = 0;
    private int hover_y = 32;
    private int selected;
    private int color = 0;
    private String message = "";

    public SpriteEditor(EngineContext context) {
        window = context.require(Window.class);
        input = context.require(InputSystem.class);
        renderer = context.require(Renderer.class);
        canvas = context.require(Canvas.class);
        Assets assets = context.require(Assets.class);

        font = canvas.defaultFont();
        var pallete = assets.texture("palletes/default.png");
        palleteTiles = new SpriteSheet(pallete, 32, 32);
        palleteBar = new SpriteSheet(pallete, 256, 64);

        canvas.clearColor(0f, 0f, 0f);
    }

    private void run() {
        Timer timer = new Timer();
        while (!window.shouldClose() && !input.isKeyPressed(Key.ESCAPE)) {
            timer.update();
            input.update();
            update(timer.deltaTime());

            renderer.beginFrame();
            canvas.begin();
            render(canvas);
            renderer.endFrame();
        }
    }

    private void update(float deltaTime) {
        int mouse_x = (int) input.getMouseX() / 4 / 8;
        int mouse_y = (int) input.getMouseY() / 4 / 8;

        hover_x = mouse_x * 8;
        hover_y = mouse_y * 8;

        if (input.isMouseButtonDown(MouseButton.LEFT)) {
            int x = mouse_x - 4;
            int y = mouse_y - 2;
            selected = 8 * y + x;
            if (selected >= 0 && selected < 64)
                draw[selected] = color;
            if (selected >= 72 && selected < 88)
                color = selected - 72;
        }

        if (input.isKeyPressed(Key.F1)) {
            System.out.println("hoverx=" + hover_x);
            System.out.println("hovery=" + hover_y);
            System.out.println("selected=" + selected);
            System.out.println("color=" + color);
        }

        if (input.isKeyPressed(Key.S)) {
            save();
        }
        // clear messages
        if (input.isKeyPressed(Key.ENTER)) {
            message = "";
        }
    }

    private void render(Canvas g) {
        // title
        g.textCentered(font, "Sprite Editor", 64, 4, 4);
        // draw area
        for (int i = 0; i < draw.length; i++) {
            int cellColor = draw[i];
            int x = 8 * (i % 8) + 32;
            int y = 8 * (i / 8) + 16;
            g.spr(palleteTiles, cellColor, x, y, 8, 8);
        }
        // color bar
        g.spr(palleteBar, 0, 32, 88, 64, 16);
        // hover
        g.spr(palleteTiles, color, hover_x, hover_y, 8, 8);
        // info message
        g.textCentered(font, message, 64, 112, 4);
    }

    // TODO Refactor
    void save() {
        System.out.println("salvando sprite no arquivo my_sprite.png");
        if (saveImage(encodeImage())) {
            message = "Salvo com sucesso! :)";
        } else {
            message = "falha ao salvar a imagem :(";
            System.err.println("falha ao salvar a imagem :(");
        }
    }

    BufferedImage encodeImage() {
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < draw.length; i++) {
            int color_hexa = colors[draw[i]];
            int[] color_rgb = hexToRgb(color_hexa);
            int a = 255; // alpha
            int r = color_rgb[0]; // red
            int g = color_rgb[1]; // green
            int b = color_rgb[2]; // blue

            int pixel = (a << 24) | (r << 16) | (g << 8) | b;
            img.setRGB(i % 8, i / 8, pixel);
        }

        return img;
    }

    boolean saveImage(BufferedImage image) {
        File f = new File("my_sprite.png");
        try {
            ImageIO.write(image, "png", f);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    int[] hexToRgb(int hex) {
        int r = (hex >> 16) & 255;
        int g = (hex >> 8) & 255;
        int b = hex & 255;
        return new int[] { r, g, b };
    }

    public static void launch() {
        Engine engine = new Engine()
                .registerModule(new WindowModule(WINDOW_SIZE, WINDOW_SIZE, "Sprite Editor"))
                .registerModule(new InputModule())
                .registerModule(new RenderModule())
                .registerModule(new AssetsModule(Path.of("resources")))
                .registerModule(new Toolkit2DModule(VIRTUAL_SIZE, VIRTUAL_SIZE));

        EngineContext context = engine.start();
        try {
            new SpriteEditor(context).run();
        } finally {
            engine.terminate();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
