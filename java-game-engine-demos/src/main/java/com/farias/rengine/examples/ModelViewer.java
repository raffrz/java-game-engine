package com.farias.rengine.examples;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static com.farias.rengine.GameEngine.*;
import static org.lwjgl.glfw.GLFW.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.Game;
import com.farias.rengine.GameEngine;
import com.farias.rengine.io.InputSystem;
import com.farias.rengine.io.Window;
import com.farias.rengine.render.RenderSystem;
import com.farias.rengine.render.TexturedModel;

public class ModelViewer extends Game {
    
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGTH = 960;
    private static final int FONT_SIZE = 20;

    public static void main(String[] args) {
        Window window = new Window(WINDOW_WIDTH, WINDOW_HEIGTH);
        long windowId = window.create();
        ModelViewer game = new ModelViewer(window);
        game.addSystem(new InputSystem(windowId));
        game.addSystem(new RenderSystem());
        GameEngine.initGame(game);
    }

    TexturedModel earth;
    TexturedModel clouds;
    private int cam_x = 0;
    private int cam_y = 0;
    private int cam_z = -5;
    private static final float camera_speed = 2.8f;
    private static final float rotationSpeed = 16f;

    public ModelViewer(Window window) {
        super("Model Viewer", window);
    }

    @Override
    public void onUserCreate() {
        perspectiveMode(WINDOW_WIDTH, WINDOW_HEIGTH);
        setCamera(0, 0, -5);
        earth = OBJLoader.loadModel("earth");
        clouds = OBJLoader.loadModel("clouds");
    }

    @Override
    public void onUserUpdate(float deltaTime) {
        if (earth.getRotation().y > 360)
            earth.getRotation().y-=360;
        earth.getRotation().y-=0.7f * deltaTime * rotationSpeed;

        if (clouds.getRotation().y > 360)
            clouds.getRotation().y-=360;
        clouds.getRotation().y-=1f * deltaTime * rotationSpeed;

        // ATMOSPHERE SIZE
        if (getInput().isKeyDown(GLFW_KEY_A)) {
            if (getInput().isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                clouds.getScale().sub(0.1f, 0.1f, 0.1f);
            } else {
                clouds.getScale().add(0.1f, 0.1f, 0.1f);
            }
        }

        // CAMERA CONTROL
        if (getInput().isKeyDown(GLFW_KEY_UP)) {
            cam_y -= 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_DOWN)) {
            cam_y += 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_LEFT)) {
            cam_x += 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_RIGHT)) {
            cam_x -= 32 * deltaTime * camera_speed;
        }
        if (getInput().isKeyDown(GLFW_KEY_Z)) {
            if (getInput().isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
                cam_z --;
            } else {
                cam_z ++;
            }
        }
        setCamera(cam_x, cam_y, cam_z);
    }

    @Override
    public void onGfxUpdate(float deltaTime) {
        perspectiveMode(WINDOW_WIDTH, WINDOW_HEIGTH);

        drawTexturedModel(earth);
        drawTexturedModel(clouds);

        orthographicMode(WINDOW_WIDTH, WINDOW_HEIGTH);
        drawText("ABC", 40, -50, FONT_SIZE, FONT_SIZE);
    }
}

class OBJLoader {

    public static TexturedModel loadModel(String fileName) {
        FileReader fr = null;
        try {
            fr = new FileReader(new File("resources/models/" + fileName + ".obj"));
        } catch (FileNotFoundException e) {
            System.err.println("Couldn't load the file!");
            return null;
        }
        BufferedReader reader = new BufferedReader(fr);
        String line;
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        float[] verticesArray = null;
        float[] normalsArray = null;
        float[] textureArray = null;
        int[] indicesArray = null;
        try {
            while(true) {
                line = reader.readLine();
                String[] currLine = line.split(" ");
                if (line.startsWith("v ")) {
                    Vector3f vertex = new Vector3f(parseFloat(currLine[1]), parseFloat(currLine[2]), parseFloat(currLine[3]));
                    vertices.add(vertex);
                } else if (line.startsWith("vt ")) {
                    Vector2f texture = new Vector2f(parseFloat(currLine[1]), parseFloat(currLine[2]));
                    textures.add(texture);
                } else if (line.startsWith("vn ")) {
                    Vector3f normal = new Vector3f(parseFloat(currLine[1]), parseFloat(currLine[2]), parseFloat(currLine[3]));
                    normals.add(normal);
                } else if (line.startsWith("f ")) {
                    textureArray = new float[vertices.size() * 2];
                    normalsArray = new float[vertices.size() * 3];
                    break;
                }
            }

            while (line != null) {
                if (!line.startsWith("f")) {
                    line = reader.readLine();
                    continue;
                }
                String[] currLine = line.split(" ");
                String[] vertex1 = currLine[1].split("/");
                String[] vertex2 = currLine[2].split("/");
                String[] vertex3 = currLine[3].split("/");

                processVertex(vertex1, indices, textures, normals, textureArray, normalsArray);
                processVertex(vertex2, indices, textures, normals, textureArray, normalsArray);
                processVertex(vertex3, indices, textures, normals, textureArray, normalsArray);
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        verticesArray = new float[vertices.size()*3];
        indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (Vector3f vertex : vertices) {
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        return TexturedModel.load(fileName, verticesArray, textureArray, indicesArray);
    }

    private static void processVertex(String[] vertexData, List<Integer> indices, List<Vector2f> textures,
            List<Vector3f> normals, float[] textureArray, float[] normalsArray) {
        int currentVertexPointer = parseInt(vertexData[0]) -1;
        indices.add(currentVertexPointer);
        Vector2f currentTex = textures.get(parseInt(vertexData[1])-1);
        textureArray[currentVertexPointer*2] = currentTex.x;
        textureArray[currentVertexPointer*2+1] = 1 - currentTex.y;
        Vector3f currentNorm = normals.get(parseInt(vertexData[2])-1);
        normalsArray[currentVertexPointer*3] = currentNorm.x;
        normalsArray[currentVertexPointer*3+1] = currentNorm.y;
        normalsArray[currentVertexPointer*3+2] = currentNorm.z;
    }

}
