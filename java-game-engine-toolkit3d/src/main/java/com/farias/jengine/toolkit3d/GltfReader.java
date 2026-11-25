package com.farias.jengine.toolkit3d;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;

import com.farias.jengine.animation.AnimationClip;
import com.farias.jengine.animation.Interpolation;
import com.farias.jengine.animation.Skeleton;
import com.farias.jengine.animation.Track;
import com.farias.jengine.renderer.MeshData;

/**
 * Lê glTF 2.0 ({@code .gltf} com buffers externos ou {@code data:} base64, e
 * {@code .glb}) para um {@link GltfData}. Puro: não toca GPU nem disco (os
 * arquivos externos vêm do {@code resolver}), então é testável sem contexto.
 *
 * <p>
 * Conversão:
 * <ul>
 * <li>Esqueleto: todos os nós necessários da cena (juntas das skins, nós com
 * malha e os ancestrais deles) viram juntas, ordenadas por profundidade (pai
 * antes do filho). Assim nós intermediários (o "Armature" do Blender) e
 * animações de nós sem skin funcionam. A inversa de bind vem da skin; para os
 * demais nós, é a inversa da matriz de repouso.</li>
 * <li>Malha com skin: posições como no arquivo, {@code JOINTS_0} remapeado
 * para o esqueleto, pesos renormalizados. Malha sem skin: presa ao próprio
 * nó com peso 1, posições levadas ao espaço de bind desse nó.</li>
 * <li>UV como no arquivo (origem no topo, a mesma das texturas da engine).</li>
 * <li>Sem NORMAL: normais suaves calculadas pelas faces. Sem índices:
 * 0..n-1.</li>
 * </ul>
 * Não suportado (erro): extensões obrigatórias, accessors esparsos. Ignorado
 * com aviso: morph targets, modos que não são triângulos, canais
 * {@code weights}.
 */
final class GltfReader {

    /** JSON estrito (RFC 8259): objetos viram {@code Map}, arrays {@code List}, números {@code Double}. */
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private static final int GLB_MAGIC = 0x46546C67; // "glTF"
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;
    private static final int MODE_TRIANGLES = 4;
    // componentType dos accessors
    private static final int BYTE = 5120;
    private static final int UNSIGNED_BYTE = 5121;
    private static final int SHORT = 5122;
    private static final int UNSIGNED_SHORT = 5123;
    private static final int UNSIGNED_INT = 5125;
    private static final int FLOAT = 5126;

    private final String path;
    private final String directory;
    private final Function<String, byte[]> resolver;
    private final List<String> warnings = new ArrayList<>();

    private Map<String, Object> root;
    private byte[] glbBinary;
    private ByteBuffer[] buffers;

    private GltfReader(String path, Function<String, byte[]> resolver) {
        this.path = path;
        int slash = path.lastIndexOf('/');
        this.directory = slash < 0 ? "" : path.substring(0, slash + 1);
        this.resolver = resolver;
    }

    /**
     * @param path     caminho do arquivo (nomes de cache e arquivos relativos)
     * @param file     o conteúdo do .gltf ou .glb
     * @param resolver lê um arquivo referenciado (buffer .bin), pelo caminho
     *                 já juntado ao diretório do modelo
     */
    static GltfData read(String path, byte[] file, Function<String, byte[]> resolver) {
        return new GltfReader(path, resolver).read(file);
    }

    // ---- topo ----

    private GltfData read(byte[] file) {
        root = parseJson(jsonText(file));
        List<Object> required = list(root, "extensionsRequired");
        if (!required.isEmpty()) {
            throw unsupported("required extensions " + required);
        }
        loadBuffers();

        int[] parentOf = parentOf();
        NodeTransforms rest = restTransforms(parentOf);
        List<Integer> meshNodes = meshNodes(parentOf);
        List<Integer> order = jointOrder(neededNodes(meshNodes, parentOf), parentOf);
        int[] jointOf = new int[parentOf.length];
        Arrays.fill(jointOf, -1);
        for (int j = 0; j < order.size(); j++) {
            jointOf[order.get(j)] = j;
        }
        Matrix4f[] inverseBindByNode = inverseBindByNode(order, rest.world());
        Skeleton skeleton = skeleton(order, parentOf, jointOf, rest, inverseBindByNode);

        List<GltfData.Primitive> primitives = new ArrayList<>();
        for (int n : meshNodes) {
            readMesh(n, jointOf, inverseBindByNode, primitives);
        }
        List<AnimationClip> clips = readAnimations(jointOf);
        return new GltfData(skeleton, primitives, clips, List.copyOf(warnings));
    }

    private String jsonText(byte[] file) {
        ByteBuffer data = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        if (file.length < 12 || data.getInt(0) != GLB_MAGIC) {
            String text = new String(file, StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        }
        int version = data.getInt(4);
        if (version != 2) {
            throw unsupported("GLB version " + version);
        }
        int length = Math.min(data.getInt(8), file.length);
        String json = null;
        int offset = 12;
        while (offset + 8 <= length) {
            int chunkLength = data.getInt(offset);
            int type = data.getInt(offset + 4);
            int start = offset + 8;
            if (start + chunkLength > length) {
                throw new IllegalArgumentException("glTF '" + path + "': truncated GLB chunk");
            }
            if (type == CHUNK_JSON && json == null) {
                json = new String(file, start, chunkLength, StandardCharsets.UTF_8);
            } else if (type == CHUNK_BIN && glbBinary == null) {
                glbBinary = Arrays.copyOfRange(file, start, start + chunkLength);
            }
            offset = start + chunkLength;
        }
        if (json == null) {
            throw new IllegalArgumentException("glTF '" + path + "': GLB without JSON chunk");
        }
        return json;
    }

    /** O objeto JSON da raiz do arquivo. */
    private Map<String, Object> parseJson(String text) {
        Object parsed;
        try {
            parsed = GSON.fromJson(text, Object.class);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("glTF '" + path + "': invalid JSON: " + e.getMessage(), e);
        }
        if (!(parsed instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("glTF '" + path + "': the JSON root is not an object");
        }
        return asMap(parsed, "root");
    }

    private void loadBuffers() {
        List<Object> list = list(root, "buffers");
        buffers = new ByteBuffer[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> buffer = asMap(list.get(i), "buffer");
            String uri = string(buffer, "uri", null);
            byte[] bytes;
            if (uri == null) {
                if (glbBinary == null) {
                    throw new IllegalArgumentException("glTF '" + path + "': buffer " + i + " has no uri and no GLB BIN chunk");
                }
                bytes = glbBinary;
            } else {
                bytes = readUri(uri);
            }
            buffers[i] = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    private byte[] readUri(String uri) {
        if (uri.startsWith("data:")) {
            int comma = uri.indexOf(',');
            if (comma < 0 || !uri.substring(0, comma).endsWith(";base64")) {
                throw unsupported("data URI that is not base64");
            }
            return Base64.getDecoder().decode(uri.substring(comma + 1));
        }
        return resolver.apply(resolve(uri));
    }

    private String resolve(String uri) {
        return directory + URLDecoder.decode(uri.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    // ---- nós e esqueleto ----

    /**
     * Transformações de repouso de todos os nós: as locais (T, R, S) e a de
     * mundo.
     */
    private record NodeTransforms(Vector3f[] translations, Quaternionf[] rotations, Vector3f[] scales,
            Matrix4f[] world) {
    }

    private Map<String, Object> node(int index) {
        return asMap(list(root, "nodes").get(index), "node");
    }

    /** O pai de cada nó, ou -1 nas raízes. */
    private int[] parentOf() {
        int nodeCount = list(root, "nodes").size();
        int[] parentOf = new int[nodeCount];
        Arrays.fill(parentOf, -1);
        for (int n = 0; n < nodeCount; n++) {
            for (Object child : list(node(n), "children")) {
                parentOf[toInt(child)] = n;
            }
        }
        return parentOf;
    }

    private NodeTransforms restTransforms(int[] parentOf) {
        int nodeCount = parentOf.length;
        NodeTransforms rest = new NodeTransforms(new Vector3f[nodeCount], new Quaternionf[nodeCount],
                new Vector3f[nodeCount], new Matrix4f[nodeCount]);
        for (int n = 0; n < nodeCount; n++) {
            rest.translations()[n] = new Vector3f();
            rest.rotations()[n] = new Quaternionf();
            rest.scales()[n] = new Vector3f();
            readLocal(node(n), rest.translations()[n], rest.rotations()[n], rest.scales()[n]);
        }
        for (int n = 0; n < nodeCount; n++) {
            worldOf(n, parentOf, rest);
        }
        return rest;
    }

    private static void readLocal(Map<String, Object> node, Vector3f t, Quaternionf r, Vector3f s) {
        if (node.containsKey("matrix")) {
            Matrix4f m = new Matrix4f().set(toFloats(list(node, "matrix"), 16));
            m.getTranslation(t);
            m.getNormalizedRotation(r);
            m.getScale(s);
            return;
        }
        float[] translation = node.containsKey("translation") ? toFloats(list(node, "translation"), 3) : new float[3];
        float[] rotation = node.containsKey("rotation") ? toFloats(list(node, "rotation"), 4) : new float[] { 0, 0, 0, 1 };
        float[] scale = node.containsKey("scale") ? toFloats(list(node, "scale"), 3) : new float[] { 1, 1, 1 };
        t.set(translation[0], translation[1], translation[2]);
        r.set(rotation[0], rotation[1], rotation[2], rotation[3]).normalize();
        s.set(scale[0], scale[1], scale[2]);
    }

    private static Matrix4f worldOf(int n, int[] parentOf, NodeTransforms rest) {
        Matrix4f[] world = rest.world();
        if (world[n] == null) {
            Matrix4f local = new Matrix4f().translationRotateScale(rest.translations()[n], rest.rotations()[n],
                    rest.scales()[n]);
            world[n] = parentOf[n] < 0 ? local : new Matrix4f(worldOf(parentOf[n], parentOf, rest)).mul(local);
        }
        return world[n];
    }

    /** Os nós com malha da cena padrão. */
    private List<Integer> meshNodes(int[] parentOf) {
        List<Integer> meshNodes = new ArrayList<>();
        for (int n : sceneNodes(parentOf)) {
            if (node(n).containsKey("mesh")) {
                meshNodes.add(n);
            }
        }
        return meshNodes;
    }

    /** Os nós que viram juntas: as juntas das skins, os nós com malha e os ancestrais de todos eles. */
    private Set<Integer> neededNodes(List<Integer> meshNodes, int[] parentOf) {
        Set<Integer> needed = new HashSet<>(meshNodes);
        for (Object skin : list(root, "skins")) {
            for (Object joint : list(asMap(skin, "skin"), "joints")) {
                needed.add(toInt(joint));
            }
        }
        for (int n : new ArrayList<>(needed)) {
            for (int p = parentOf[n]; p >= 0; p = parentOf[p]) {
                needed.add(p);
            }
        }
        if (needed.isEmpty()) {
            throw new IllegalArgumentException("glTF '" + path + "' has no meshes");
        }
        return needed;
    }

    /** A ordem das juntas: por profundidade (pai antes do filho), depois pelo índice do nó. */
    private static List<Integer> jointOrder(Set<Integer> needed, int[] parentOf) {
        int[] depth = new int[parentOf.length];
        for (int n : needed) {
            for (int p = parentOf[n]; p >= 0; p = parentOf[p]) {
                depth[n]++;
            }
        }
        List<Integer> order = new ArrayList<>(needed);
        order.sort(Comparator.<Integer>comparingInt(n -> depth[n]).thenComparingInt(n -> n));
        return order;
    }

    /**
     * A inversa de bind de cada nó do esqueleto: a da skin que o lista (se
     * duas skins dão matrizes diferentes, vale a primeira, com aviso) ou, para
     * nós fora das skins, a inversa da matriz de repouso.
     */
    private Matrix4f[] inverseBindByNode(List<Integer> order, Matrix4f[] world) {
        Matrix4f[] byNode = new Matrix4f[world.length];
        List<Object> skins = list(root, "skins");
        for (int s = 0; s < skins.size(); s++) {
            Map<String, Object> skin = asMap(skins.get(s), "skin");
            List<Object> joints = list(skin, "joints");
            float[] matrices = skin.containsKey("inverseBindMatrices")
                    ? floats(toInt(skin.get("inverseBindMatrices")))
                    : null;
            for (int k = 0; k < joints.size(); k++) {
                int n = toInt(joints.get(k));
                Matrix4f matrix = matrices == null ? new Matrix4f() : new Matrix4f().set(matrices, k * 16);
                if (byNode[n] == null) {
                    byNode[n] = matrix;
                } else if (!byNode[n].equals(matrix, 1e-4f)) {
                    warnings.add("skin " + s + ": node " + n
                            + " has a different inverse bind matrix in another skin; using the first one");
                }
            }
        }
        for (int n : order) {
            if (byNode[n] == null) {
                byNode[n] = new Matrix4f(world[n]).invert();
            }
        }
        return byNode;
    }

    private Skeleton skeleton(List<Integer> order, int[] parentOf, int[] jointOf, NodeTransforms rest,
            Matrix4f[] inverseBindByNode) {
        int count = order.size();
        String[] names = new String[count];
        int[] parents = new int[count];
        Vector3f[] translations = new Vector3f[count];
        Quaternionf[] rotations = new Quaternionf[count];
        Vector3f[] scales = new Vector3f[count];
        Matrix4f[] inverseBinds = new Matrix4f[count];
        Set<String> usedNames = new HashSet<>();
        for (int j = 0; j < count; j++) {
            int n = order.get(j);
            String name = string(node(n), "name", "node" + n);
            names[j] = usedNames.add(name) ? name : name + "#" + n;
            usedNames.add(names[j]);
            parents[j] = parentOf[n] < 0 ? -1 : jointOf[parentOf[n]];
            translations[j] = rest.translations()[n];
            rotations[j] = rest.rotations()[n];
            scales[j] = rest.scales()[n];
            inverseBinds[j] = inverseBindByNode[n];
        }
        return new Skeleton(names, parents, translations, rotations, scales, inverseBinds);
    }

    /** Os nós da cena padrão (ou de todas as raízes, se não há cenas), em profundidade. */
    private List<Integer> sceneNodes(int[] parentOf) {
        List<Integer> roots = new ArrayList<>();
        List<Object> scenes = list(root, "scenes");
        if (scenes.isEmpty()) {
            for (int n = 0; n < parentOf.length; n++) {
                if (parentOf[n] < 0) {
                    roots.add(n);
                }
            }
        } else {
            int scene = root.containsKey("scene") ? toInt(root.get("scene")) : 0;
            for (Object n : list(asMap(scenes.get(scene), "scene"), "nodes")) {
                roots.add(toInt(n));
            }
        }
        List<Integer> out = new ArrayList<>();
        List<Integer> stack = new ArrayList<>(roots);
        while (!stack.isEmpty()) {
            int n = stack.remove(stack.size() - 1);
            out.add(n);
            for (Object child : list(node(n), "children")) {
                stack.add(toInt(child));
            }
        }
        return out;
    }

    // ---- malhas ----

    private void readMesh(int nodeIndex, int[] jointOf, Matrix4f[] inverseBindByNode,
            List<GltfData.Primitive> out) {
        Map<String, Object> node = node(nodeIndex);
        int meshIndex = toInt(node.get("mesh"));
        Map<String, Object> mesh = asMap(list(root, "meshes").get(meshIndex), "mesh");
        List<Object> skinJoints = node.containsKey("skin")
                ? list(asMap(list(root, "skins").get(toInt(node.get("skin"))), "skin"), "joints")
                : null;

        List<Object> primitives = list(mesh, "primitives");
        for (int p = 0; p < primitives.size(); p++) {
            Map<String, Object> primitive = asMap(primitives.get(p), "primitive");
            String where = "mesh " + meshIndex + " primitive " + p;
            int mode = primitive.containsKey("mode") ? toInt(primitive.get("mode")) : MODE_TRIANGLES;
            if (mode != MODE_TRIANGLES) {
                warnings.add(where + ": mode " + mode + " is not triangles, skipped");
                continue;
            }
            if (!list(primitive, "targets").isEmpty()) {
                warnings.add(where + ": morph targets ignored");
            }
            Map<String, Object> attributes = asMap(primitive.get("attributes"), "attributes");
            if (!attributes.containsKey("POSITION")) {
                warnings.add(where + ": no POSITION, skipped");
                continue;
            }
            float[] positions = floats(toInt(attributes.get("POSITION")));
            int vertexCount = positions.length / 3;
            int[] indices = primitive.containsKey("indices") ? ints(toInt(primitive.get("indices"))) : sequence(vertexCount);
            float[] normals = attributes.containsKey("NORMAL") ? floats(toInt(attributes.get("NORMAL")))
                    : smoothNormals(positions, indices);
            float[] uvs = attributes.containsKey("TEXCOORD_0") ? floats(toInt(attributes.get("TEXCOORD_0")))
                    : new float[vertexCount * 2];

            int[] joints = new int[vertexCount * 4];
            float[] weights = new float[vertexCount * 4];
            boolean skinned = skinJoints != null && attributes.containsKey("JOINTS_0")
                    && attributes.containsKey("WEIGHTS_0");
            if (skinned) {
                if (attributes.containsKey("JOINTS_1")) {
                    warnings.add(where + ": more than 4 joint influences per vertex, only JOINTS_0/WEIGHTS_0 used");
                }
                int[] rawJoints = ints(toInt(attributes.get("JOINTS_0")));
                float[] rawWeights = floats(toInt(attributes.get("WEIGHTS_0")));
                int fallback = jointOf[nodeIndex];
                for (int v = 0; v < vertexCount; v++) {
                    float sum = 0f;
                    for (int i = 0; i < 4; i++) {
                        sum += rawWeights[v * 4 + i];
                    }
                    for (int i = 0; i < 4; i++) {
                        int k = v * 4 + i;
                        joints[k] = jointOf[toInt(skinJoints.get(rawJoints[k]))];
                        weights[k] = sum > 0f ? rawWeights[k] / sum : 0f;
                    }
                    if (sum <= 0f) {
                        joints[v * 4] = fallback;
                        weights[v * 4] = 1f;
                    }
                }
            } else {
                if (skinJoints != null) {
                    warnings.add(where + ": skin without JOINTS_0/WEIGHTS_0, drawn rigid");
                }
                // rígida: presa ao próprio nó; leva os vértices ao espaço de bind dele
                Matrix4f bind = new Matrix4f(inverseBindByNode[nodeIndex]).invert();
                Matrix3f normalBind = bind.normal(new Matrix3f());
                Vector3f scratch = new Vector3f();
                for (int v = 0; v < vertexCount; v++) {
                    bind.transformPosition(scratch.set(positions[v * 3], positions[v * 3 + 1], positions[v * 3 + 2]));
                    put(scratch, positions, v * 3);
                    normalBind.transform(scratch.set(normals[v * 3], normals[v * 3 + 1], normals[v * 3 + 2]));
                    put(scratch.normalize(), normals, v * 3);
                    joints[v * 4] = jointOf[nodeIndex];
                    weights[v * 4] = 1f;
                }
            }

            MeshData data = new MeshData(positions, uvs, normals, indices, joints, weights);
            float[] baseColor = { 1, 1, 1, 1 };
            GltfData.Image image = null;
            if (primitive.containsKey("material")) {
                Map<String, Object> material = asMap(list(root, "materials").get(toInt(primitive.get("material"))),
                        "material");
                Map<String, Object> pbr = asMap(material.getOrDefault("pbrMetallicRoughness", Map.of()), "pbr");
                if (pbr.containsKey("baseColorFactor")) {
                    baseColor = toFloats(list(pbr, "baseColorFactor"), 4);
                }
                if (pbr.containsKey("baseColorTexture")) {
                    int texture = toInt(asMap(pbr.get("baseColorTexture"), "baseColorTexture").get("index"));
                    image = image(texture);
                }
            }
            out.add(new GltfData.Primitive(data, image, baseColor));
        }
    }

    private GltfData.Image image(int textureIndex) {
        Map<String, Object> texture = asMap(list(root, "textures").get(textureIndex), "texture");
        if (!texture.containsKey("source")) {
            warnings.add("texture " + textureIndex + " has no source (extension image?), ignored");
            return null;
        }
        int imageIndex = toInt(texture.get("source"));
        Map<String, Object> image = asMap(list(root, "images").get(imageIndex), "image");
        String key = path + "#image" + imageIndex;
        String uri = string(image, "uri", null);
        if (uri != null && !uri.startsWith("data:")) {
            return new GltfData.Image(resolve(uri), null, null);
        }
        byte[] bytes = uri != null ? readUri(uri) : viewBytes(toInt(image.get("bufferView")));
        return new GltfData.Image(null, bytes, key);
    }

    private static void put(Vector3f v, float[] out, int offset) {
        out[offset] = v.x;
        out[offset + 1] = v.y;
        out[offset + 2] = v.z;
    }

    private static int[] sequence(int count) {
        int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = i;
        }
        return out;
    }

    /** Normais por vértice: soma das normais das faces (pesadas pela área), normalizada. */
    static float[] smoothNormals(float[] positions, int[] indices) {
        float[] normals = new float[positions.length];
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f c = new Vector3f();
        for (int i = 0; i + 2 < indices.length; i += 3) {
            int ia = indices[i] * 3;
            int ib = indices[i + 1] * 3;
            int ic = indices[i + 2] * 3;
            a.set(positions[ia], positions[ia + 1], positions[ia + 2]);
            b.set(positions[ib], positions[ib + 1], positions[ib + 2]).sub(a);
            c.set(positions[ic], positions[ic + 1], positions[ic + 2]).sub(a);
            b.cross(c);
            for (int corner : new int[] { ia, ib, ic }) {
                normals[corner] += b.x;
                normals[corner + 1] += b.y;
                normals[corner + 2] += b.z;
            }
        }
        for (int i = 0; i < normals.length; i += 3) {
            a.set(normals[i], normals[i + 1], normals[i + 2]);
            if (a.lengthSquared() > 0f) {
                put(a.normalize(), normals, i);
            }
        }
        return normals;
    }

    // ---- animações ----

    private List<AnimationClip> readAnimations(int[] jointOf) {
        List<AnimationClip> clips = new ArrayList<>();
        List<Object> animations = list(root, "animations");
        for (int a = 0; a < animations.size(); a++) {
            Map<String, Object> animation = asMap(animations.get(a), "animation");
            String name = string(animation, "name", "animation" + a);
            List<Object> samplers = list(animation, "samplers");
            List<Track> tracks = new ArrayList<>();
            for (Object channelObject : list(animation, "channels")) {
                Map<String, Object> channel = asMap(channelObject, "channel");
                Map<String, Object> target = asMap(channel.get("target"), "target");
                String targetPath = string(target, "path", "");
                if (!target.containsKey("node")) {
                    continue;
                }
                int node = toInt(target.get("node"));
                Track.Path trackPath = switch (targetPath) {
                    case "translation" -> Track.Path.TRANSLATION;
                    case "rotation" -> Track.Path.ROTATION;
                    case "scale" -> Track.Path.SCALE;
                    default -> null;
                };
                if (trackPath == null) {
                    warnings.add("animation '" + name + "': channel '" + targetPath + "' ignored");
                    continue;
                }
                if (jointOf[node] < 0) {
                    warnings.add("animation '" + name + "': node " + node + " is not part of the model, ignored");
                    continue;
                }
                Map<String, Object> sampler = asMap(samplers.get(toInt(channel.get("sampler"))), "sampler");
                Interpolation interpolation = switch (string(sampler, "interpolation", "LINEAR")) {
                    case "STEP" -> Interpolation.STEP;
                    case "CUBICSPLINE" -> Interpolation.CUBICSPLINE;
                    default -> Interpolation.LINEAR;
                };
                tracks.add(new Track(jointOf[node], trackPath, floats(toInt(sampler.get("input"))),
                        floats(toInt(sampler.get("output"))), interpolation));
            }
            clips.add(new AnimationClip(name, tracks));
        }
        return clips;
    }

    // ---- accessors ----

    /**
     * Onde estão os componentes de um accessor: {@code total} componentes,
     * {@code components} por elemento, elementos a {@code stride} bytes um do
     * outro a partir de {@code start}. {@code data} null: accessor de zeros
     * (sem bufferView).
     */
    private record AccessorView(ByteBuffer data, int start, int total, int components, int stride,
            int componentType) {

        int offset(int i) {
            return start + (i / components) * stride + (i % components) * componentSize(componentType);
        }
    }

    private static int components(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new IllegalArgumentException("unknown accessor type " + type);
        };
    }

    private static int componentSize(int componentType) {
        return switch (componentType) {
            case BYTE, UNSIGNED_BYTE -> 1;
            case SHORT, UNSIGNED_SHORT -> 2;
            case UNSIGNED_INT, FLOAT -> 4;
            default -> throw new IllegalArgumentException("unknown componentType " + componentType);
        };
    }

    /** Os componentes do accessor, achatados; inteiros normalizados viram [0, 1] ou [-1, 1]. */
    float[] floats(int accessorIndex) {
        Map<String, Object> accessor = accessor(accessorIndex);
        boolean normalized = Boolean.TRUE.equals(accessor.get("normalized"));
        AccessorView view = view(accessor);
        float[] out = new float[view.total()];
        if (view.data() == null) {
            return out;
        }
        int type = view.componentType();
        for (int i = 0; i < out.length; i++) {
            int offset = view.offset(i);
            if (type == FLOAT) {
                out[i] = view.data().getFloat(offset);
            } else {
                int value = integer(view.data(), offset, type);
                out[i] = normalized ? normalize(value, type) : value;
            }
        }
        return out;
    }

    /** Os componentes inteiros do accessor (índices, juntas), achatados. */
    int[] ints(int accessorIndex) {
        AccessorView view = view(accessor(accessorIndex));
        if (view.componentType() == FLOAT) {
            throw new IllegalArgumentException("accessor " + accessorIndex + " is float, expected integers");
        }
        int[] out = new int[view.total()];
        if (view.data() == null) {
            return out;
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = integer(view.data(), view.offset(i), view.componentType());
        }
        return out;
    }

    private static int integer(ByteBuffer data, int offset, int componentType) {
        return switch (componentType) {
            case BYTE -> data.get(offset);
            case UNSIGNED_BYTE -> data.get(offset) & 0xFF;
            case SHORT -> data.getShort(offset);
            case UNSIGNED_SHORT -> data.getShort(offset) & 0xFFFF;
            default -> data.getInt(offset);
        };
    }

    /** Inteiro normalizado (definição do glTF): sem sinal em [0, 1], com sinal em [-1, 1]. */
    private static float normalize(int value, int componentType) {
        return switch (componentType) {
            case BYTE -> Math.max(value / 127f, -1f);
            case UNSIGNED_BYTE -> value / 255f;
            case SHORT -> Math.max(value / 32767f, -1f);
            case UNSIGNED_SHORT -> value / 65535f;
            default -> value;
        };
    }

    private Map<String, Object> accessor(int index) {
        Map<String, Object> accessor = asMap(list(root, "accessors").get(index), "accessor");
        if (accessor.containsKey("sparse")) {
            throw unsupported("sparse accessor " + index);
        }
        return accessor;
    }

    private AccessorView view(Map<String, Object> accessor) {
        int count = toInt(accessor.get("count"));
        int components = components(string(accessor, "type", "SCALAR"));
        int componentType = toInt(accessor.get("componentType"));
        int size = componentSize(componentType);
        if (!accessor.containsKey("bufferView")) {
            return new AccessorView(null, 0, count * components, components, components * size, componentType);
        }
        Map<String, Object> bufferView = bufferView(toInt(accessor.get("bufferView")));
        int stride = intOr(bufferView, "byteStride", components * size);
        ByteBuffer buffer = buffers[toInt(bufferView.get("buffer"))];
        int start = intOr(bufferView, "byteOffset", 0) + intOr(accessor, "byteOffset", 0);
        int end = start + (count - 1) * stride + components * size;
        if (count > 0 && end > buffer.capacity()) {
            throw new IllegalArgumentException("glTF '" + path + "': accessor reads past the end of its buffer");
        }
        return new AccessorView(buffer, start, count * components, components, stride, componentType);
    }

    private Map<String, Object> bufferView(int index) {
        return asMap(list(root, "bufferViews").get(index), "bufferView");
    }

    private byte[] viewBytes(int viewIndex) {
        Map<String, Object> view = bufferView(viewIndex);
        ByteBuffer buffer = buffers[toInt(view.get("buffer"))];
        int start = intOr(view, "byteOffset", 0);
        int length = toInt(view.get("byteLength"));
        byte[] out = new byte[length];
        buffer.duplicate().position(start).get(out);
        return out;
    }

    // ---- JSON ----

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value, String what) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("glTF: expected an object for " + what);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Map<String, Object> object, String key) {
        Object value = object.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        throw new IllegalArgumentException("glTF: expected an array for '" + key + "'");
    }

    private static String string(Map<String, Object> object, String key, String fallback) {
        Object value = object.get(key);
        return value instanceof String s ? s : fallback;
    }

    private static int intOr(Map<String, Object> object, String key, int fallback) {
        return object.containsKey(key) ? toInt(object.get(key)) : fallback;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("glTF: expected a number, got " + value);
    }

    private static float[] toFloats(List<Object> values, int expected) {
        if (values.size() != expected) {
            throw new IllegalArgumentException("glTF: expected " + expected + " numbers, got " + values.size());
        }
        float[] out = new float[expected];
        for (int i = 0; i < expected; i++) {
            out[i] = ((Number) values.get(i)).floatValue();
        }
        return out;
    }

    private UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException("glTF '" + path + "': " + what + " not supported");
    }
}
