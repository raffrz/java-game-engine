package com.farias.rengine.examples;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.joml.Matrix4f;

import com.farias.rengine.engine3d.Game;
import com.farias.rengine.toolkit2d.legacy.BitmapFont;
import com.farias.rengine.toolkit2d.legacy.Projections2D;
import com.farias.rengine.toolkit2d.legacy.Sprite;
import com.farias.rengine.toolkit2d.legacy.SpriteBatch2D;
import com.farias.rengine.renderer.RenderPassDescriptor;
import com.farias.rengine.renderer.RenderPassNode;
import com.farias.rengine.renderer.RenderView;

public class SpriteEditor extends Game {

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

    private SpriteBatch2D batch;
    private BitmapFont font;
    private RenderPassDescriptor passDescriptor;

    Sprite pallete;
    int hover_x = 0;
    int hover_y = -32;
    int selected;
    int color = 0;
    String message = "";

    public SpriteEditor() {
        super("Sprite Editor", WINDOW_SIZE, WINDOW_SIZE);
    }

    @Override
    protected void onCreate() {
        batch = new SpriteBatch2D(renderer);
        font = new BitmapFont(batch);
        pallete = batch.createSprite("resources/palletes/default.png", 0, 256, 64);

        passDescriptor = new RenderPassDescriptor("sprite-editor")
                .clearColor(0f, 0f, 0f, 1f)
                .depthTest(false)
                .cullFace(true)
                .blend(true);
    }

    @Override
    protected void onUpdate(float deltaTime) {
        int mouse_x = (int) input.getMouseX() / 4 / 8;
        int mouse_y = -(int) input.getMouseY() / 4 / 8;

        hover_x = mouse_x * 8;
        hover_y = mouse_y * 8;

        if (input.isMouseButtonDown(0)) {
            int x = mouse_x - 4;
            int y = -mouse_y - 2;
            selected = 8 * y + x;
            if (selected >= 0 && selected < 64)
                draw[selected] = color;
            if (selected >= 72 && selected < 88)
                color = selected - 72;
        }

        if (input.isKeyPressed(GLFW_KEY_F1)) {
            System.out.println("hoverx=" + hover_x);
            System.out.println("hovery=" + hover_y);
            System.out.println("selected=" + selected);
            System.out.println("color=" + color);
        }

        if (input.isKeyPressed(GLFW_KEY_S)) {
            save();
        }
        // clear messages
        if (input.isKeyPressed(GLFW_KEY_ENTER)) {
            message = "";
        }
    }

    @Override
    protected void onRender(float deltaTime) {
        RenderView view = new RenderView(window.getFramebufferWidth(), window.getFramebufferHeight(),
                Projections2D.orthographic2D(VIRTUAL_SIZE, VIRTUAL_SIZE), new Matrix4f());
        RenderPassNode pass = renderer.createPass(passDescriptor, view);

        // title
        font.drawText(pass, "Sprite Editor", 64, -4, 4, 4);
        // draw area
        for (int i = 0; i < draw.length; i++) {
            int cellColor = draw[i];
            int x = 8 * (i % 8) + 32;
            int y = -8 * (i / 8) - 16;
            batch.draw(pass, pallete, cellColor, 32, 32, SpriteBatch2D.rectTransform(x, y, 8, 8));
        }
        // color bar
        batch.draw(pass, pallete, 0, 256, 64, SpriteBatch2D.rectTransform(32, -88, 64, 16));
        // hover
        batch.draw(pass, pallete, color, 32, 32, SpriteBatch2D.rectTransform(hover_x, hover_y, 8, 8));
        // info message
        font.drawText(pass, message, 64, -112, 4, 4);
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
        new SpriteEditor().run();
    }

    public static void main(String[] args) {
        launch();
    }
}
