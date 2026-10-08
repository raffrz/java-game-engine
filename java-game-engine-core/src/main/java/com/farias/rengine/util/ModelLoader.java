package com.farias.rengine.util;

import static java.lang.Integer.parseInt;
import static java.lang.Float.parseFloat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.gfx.Materials;
import com.farias.rengine.gfx.TexturedModel;
import com.farias.rengine.renderer.Material;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.Renderer;
import com.farias.rengine.renderer.Shader;
import com.farias.rengine.renderer.Texture;

public class ModelLoader {

    public static TexturedModel loadWavefrontOBJ(String fileName, Renderer renderer, Shader litShader) {
        return loadWavefrontOBJ(fileName, renderer, litShader, false);
    }

    /**
     * @param flipNormals inverte as normais lidas do arquivo - alguns .obj
     *                     sao exportados com as normais apontando "pra
     *                     dentro" (ao contrario de earth/clouds, que ja vem
     *                     corretas); nesses casos a iluminacao fica invertida
     *                     a menos que as normais sejam invertidas aqui.
     */
    public static TexturedModel loadWavefrontOBJ(String fileName, Renderer renderer, Shader litShader,
            boolean flipNormals) {
        FileReader fr;
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
        List<Vector3f> outVertices = new ArrayList<>();
        List<Vector2f> outTextures = new ArrayList<>();
        List<Vector3f> outNormals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        Map<String, Integer> vertexCache = new HashMap<>();

        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    vertices.add(new Vector3f(parseFloat(parts[1]), parseFloat(parts[2]), parseFloat(parts[3])));
                } else if (line.startsWith("vt ")) {
                    String[] parts = line.split("\\s+");
                    textures.add(new Vector2f(parseFloat(parts[1]), parseFloat(parts[2])));
                } else if (line.startsWith("vn ")) {
                    String[] parts = line.split("\\s+");
                    normals.add(new Vector3f(parseFloat(parts[1]), parseFloat(parts[2]), parseFloat(parts[3])));
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    String[] vertex1 = parts[1].split("/");
                    String[] vertex2 = parts[2].split("/");
                    String[] vertex3 = parts[3].split("/");

                    processVertex(vertex1, vertices, textures, normals, outVertices, outTextures, outNormals, indices,
                            vertexCache);
                    processVertex(vertex2, vertices, textures, normals, outVertices, outTextures, outNormals, indices,
                            vertexCache);
                    processVertex(vertex3, vertices, textures, normals, outVertices, outTextures, outNormals, indices,
                            vertexCache);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        float[] verticesArray = new float[outVertices.size() * 3];
        float[] textureArray = new float[outVertices.size() * 2];
        float[] normalsArray = new float[outVertices.size() * 3];
        int[] indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (Vector3f vertex : outVertices) {
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        int texturePointer = 0;
        for (Vector2f tex : outTextures) {
            textureArray[texturePointer++] = tex.x;
            textureArray[texturePointer++] = 1 - tex.y;
        }

        int normalPointer = 0;
        float normalSign = flipNormals ? -1f : 1f;
        for (Vector3f normal : outNormals) {
            normalsArray[normalPointer++] = normal.x * normalSign;
            normalsArray[normalPointer++] = normal.y * normalSign;
            normalsArray[normalPointer++] = normal.z * normalSign;
        }

        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        Mesh mesh = renderer.createMesh(new MeshData(verticesArray, textureArray, normalsArray, indicesArray));
        Texture texture = TextureLoader.load(renderer, "resources/textures/" + fileName + ".png");
        Material material = Materials.lit(renderer, litShader, texture);

        return new TexturedModel(mesh, material);
    }

    private static void processVertex(String[] vertexData, List<Vector3f> vertices, List<Vector2f> textures,
            List<Vector3f> normals, List<Vector3f> outVertices, List<Vector2f> outTextures,
            List<Vector3f> outNormals, List<Integer> indices, Map<String, Integer> vertexCache) {
        String key = String.join("/", vertexData);
        if (vertexCache.containsKey(key)) {
            indices.add(vertexCache.get(key));
            return;
        }

        int vertexIndex = parseInt(vertexData[0]) - 1;
        Vector3f position = vertices.get(vertexIndex);
        outVertices.add(position);

        if (vertexData.length > 1 && !vertexData[1].isEmpty()) {
            Vector2f texCoord = textures.get(parseInt(vertexData[1]) - 1);
            outTextures.add(texCoord);
        } else {
            outTextures.add(new Vector2f(0, 0));
        }

        if (vertexData.length > 2 && !vertexData[2].isEmpty()) {
            Vector3f normal = normals.get(parseInt(vertexData[2]) - 1);
            outNormals.add(normal);
        } else {
            outNormals.add(new Vector3f(0, 0, 0));
        }

        int newIndex = outVertices.size() - 1;
        vertexCache.put(key, newIndex);
        indices.add(newIndex);
    }

}
