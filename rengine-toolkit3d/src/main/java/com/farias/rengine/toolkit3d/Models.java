package com.farias.rengine.toolkit3d;

import static java.lang.Integer.parseInt;
import static java.lang.Float.parseFloat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.assets.Assets;
import com.farias.rengine.renderer.Mesh;
import com.farias.rengine.renderer.MeshData;
import com.farias.rengine.renderer.Renderer;

/**
 * Carrega modelos Wavefront OBJ via {@link Assets}; os caminhos sao relativos
 * a raiz de assets, como em {@code Assets.texture}. Falhas de leitura/parse
 * viram excecao, nunca um modelo nulo.
 *
 * <p>Cada arquivo e lido e enviado a GPU uma vez: pedir o mesmo .obj de novo
 * devolve o mesmo {@link Mesh}, entao varios modelos podem compartilha-lo.
 */
public final class Models {
    private final Assets assets;
    private final Renderer renderer;
    private final LitMaterials materials;
    private final Map<MeshKey, Mesh> meshes = new HashMap<>();

    Models(Assets assets, Renderer renderer, LitMaterials materials) {
        this.assets = assets;
        this.renderer = renderer;
        this.materials = materials;
    }

    /**
     * O modelo do .obj com um {@link LitMaterial} novo para a textura dada;
     * refine-o com {@link TexturedModel#lit()}. Para outro tipo de material,
     * use {@link #mesh(String)} e monte o {@link TexturedModel} voce mesmo.
     */
    public TexturedModel load(String objPath, String texturePath) {
        return load(objPath, texturePath, false);
    }

    /** Idem; ver {@link #mesh(String, boolean)} sobre {@code flipNormals}. */
    public TexturedModel load(String objPath, String texturePath, boolean flipNormals) {
        return new TexturedModel(mesh(objPath, flipNormals), materials.create(assets.texture(texturePath)));
    }

    public Mesh mesh(String objPath) {
        return mesh(objPath, false);
    }

    /**
     * @param flipNormals inverte as normais lidas do arquivo - alguns .obj sao
     *                    exportados com as normais apontando "pra dentro";
     *                    nesses casos a iluminacao fica invertida a menos que
     *                    as normais sejam invertidas aqui.
     */
    public Mesh mesh(String objPath, boolean flipNormals) {
        return meshes.computeIfAbsent(new MeshKey(objPath, flipNormals),
                key -> renderer.createMesh(read(key.objPath(), key.flipNormals())));
    }

    /** Esvazia o cache. Nao libera GPU: os meshes pertencem ao Renderer. */
    void clear() {
        meshes.clear();
    }

    private MeshData read(String objPath, boolean flipNormals) {
        String line;
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> textures = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector3f> outVertices = new ArrayList<>();
        List<Vector2f> outTextures = new ArrayList<>();
        List<Vector3f> outNormals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        Map<String, Integer> vertexCache = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(assets.open(objPath), StandardCharsets.UTF_8))) {
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
                    // Faces com mais de 3 vertices (quads, n-gons) viram um
                    // leque de triangulos a partir do primeiro vertice.
                    for (int i = 2; i < parts.length - 1; i++) {
                        for (String corner : new String[] { parts[1], parts[i], parts[i + 1] }) {
                            processVertex(corner.split("/"), vertices, textures, normals, outVertices, outTextures,
                                    outNormals, indices, vertexCache);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read model '" + objPath + "'", e);
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

        return new MeshData(verticesArray, textureArray, normalsArray, indicesArray);
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

    private record MeshKey(String objPath, boolean flipNormals) {
    }
}
