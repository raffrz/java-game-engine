package com.farias.rengine.util;

import static java.lang.Integer.parseInt;
import static java.lang.Float.parseFloat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.render.TexturedModel;

public class ModelLoader {

    public static TexturedModel loadWavefrontOBJ(String fileName) {
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
