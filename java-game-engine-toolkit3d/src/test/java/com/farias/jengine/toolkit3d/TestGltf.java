package com.farias.jengine.toolkit3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Um glTF pequeno montado em código, nos três formatos (data URI, .bin
 * externo e .glb), para os testes do leitor.
 *
 * <pre>
 * nó 3 "Armature" (escala 2, não é junta)  -- cena
 *   nó 2 "base" (junta)
 *     nó 1 "tip" (junta, translação 0,1,0)  -- listada antes do pai
 *       nó 4 "hat" (malha 1 rígida, translação 0,0.5,0)
 * nó 0 "body" (malha 0 com skin 0; translação ignorada)  -- cena
 * skin 0: joints [1 (tip), 2 (base)]
 * malha 0: triângulo (0,0,0) base, (1,0,0) metade/metade, (0,2,0) tip;
 *          POSITION+NORMAL intercalados (stride 24), pesos UBYTE normalizados
 * malha 1: triângulo sem normais e sem índices
 * animação "wave": tip gira 0 -> 90 graus em Z (LINEAR, 0..1 s),
 *          Armature translação STEP, e um canal "weights" (ignorado)
 * </pre>
 */
final class TestGltf {

    static final int BIN_LENGTH = 348;

    private TestGltf() {
    }

    static byte[] bin() {
        ByteBuffer b = ByteBuffer.allocate(BIN_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        // view 0 @0: posição + normal intercalados
        float[][] positions = { { 0, 0, 0 }, { 1, 0, 0 }, { 0, 2, 0 } };
        for (float[] p : positions) {
            b.putFloat(p[0]).putFloat(p[1]).putFloat(p[2]).putFloat(0).putFloat(0).putFloat(1);
        }
        // view 1 @72: UV
        b.putFloat(0).putFloat(0).putFloat(1).putFloat(0).putFloat(0).putFloat(1);
        // view 2 @96: índices ushort + 2 bytes de padding
        b.putShort((short) 0).putShort((short) 1).putShort((short) 2).putShort((short) 0);
        // view 3 @104: JOINTS_0 ubyte (índices na lista da skin: 0 = tip, 1 = base)
        b.put(new byte[] { 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 });
        // view 4 @116: WEIGHTS_0 ubyte normalizado; o 2o soma 256/255
        b.put(new byte[] { (byte) 255, 0, 0, 0, (byte) 128, (byte) 128, 0, 0, (byte) 255, 0, 0, 0 });
        // view 5 @128: inversas de bind (tip, base)
        Matrix4f baseWorld = new Matrix4f().scale(2);
        Matrix4f tipWorld = new Matrix4f(baseWorld).translate(0, 1, 0);
        float[] matrix = new float[16];
        new Matrix4f(tipWorld).invert().get(matrix);
        for (float f : matrix) {
            b.putFloat(f);
        }
        new Matrix4f(baseWorld).invert().get(matrix);
        for (float f : matrix) {
            b.putFloat(f);
        }
        // view 6 @256: malha rígida
        b.putFloat(0).putFloat(0).putFloat(0).putFloat(1).putFloat(0).putFloat(0).putFloat(0).putFloat(0).putFloat(1);
        // view 7 @292: tempos 0, 1
        b.putFloat(0).putFloat(1);
        // view 8 @300: rotações identidade e 90 graus em Z
        Quaternionf q = new Quaternionf().rotationZ((float) Math.toRadians(90));
        b.putFloat(0).putFloat(0).putFloat(0).putFloat(1).putFloat(q.x).putFloat(q.y).putFloat(q.z).putFloat(q.w);
        // view 9 @332: tempo 0; view 10 @336: translação (5, 0, 0)
        b.putFloat(0);
        b.putFloat(5).putFloat(0).putFloat(0);
        if (b.position() != BIN_LENGTH) {
            throw new IllegalStateException("bin layout: " + b.position());
        }
        return b.array();
    }

    /** O JSON; {@code bufferUri} null = buffer do chunk BIN do .glb. */
    static String json(String bufferUri) {
        String buffer = bufferUri == null
                ? "{\"byteLength\": " + BIN_LENGTH + "}"
                : "{\"byteLength\": " + BIN_LENGTH + ", \"uri\": \"" + bufferUri + "\"}";
        return """
                {
                  "asset": {"version": "2.0"},
                  "scene": 0,
                  "scenes": [{"nodes": [3, 0]}],
                  "nodes": [
                    {"name": "body", "mesh": 0, "skin": 0, "translation": [9, 9, 9]},
                    {"name": "tip", "translation": [0, 1, 0], "children": [4]},
                    {"name": "base", "children": [1]},
                    {"name": "Armature", "scale": [2, 2, 2], "children": [2]},
                    {"name": "hat", "mesh": 1, "translation": [0, 0.5, 0]}
                  ],
                  "skins": [{"joints": [1, 2], "inverseBindMatrices": 6}],
                  "meshes": [
                    {"primitives": [{"attributes": {"POSITION": 0, "NORMAL": 1, "TEXCOORD_0": 2,
                        "JOINTS_0": 4, "WEIGHTS_0": 5}, "indices": 3, "material": 0}]},
                    {"primitives": [{"attributes": {"POSITION": 7}}]}
                  ],
                  "materials": [{"pbrMetallicRoughness": {"baseColorFactor": [1, 0, 0, 1]}}],
                  "animations": [{"name": "wave",
                    "samplers": [{"input": 8, "output": 9}, {"input": 10, "output": 11, "interpolation": "STEP"}],
                    "channels": [
                      {"sampler": 0, "target": {"node": 1, "path": "rotation"}},
                      {"sampler": 1, "target": {"node": 3, "path": "translation"}},
                      {"sampler": 1, "target": {"node": 0, "path": "weights"}}
                    ]}],
                  "buffers": [%s],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 72, "byteStride": 24},
                    {"buffer": 0, "byteOffset": 72, "byteLength": 24},
                    {"buffer": 0, "byteOffset": 96, "byteLength": 6},
                    {"buffer": 0, "byteOffset": 104, "byteLength": 12},
                    {"buffer": 0, "byteOffset": 116, "byteLength": 12},
                    {"buffer": 0, "byteOffset": 128, "byteLength": 128},
                    {"buffer": 0, "byteOffset": 256, "byteLength": 36},
                    {"buffer": 0, "byteOffset": 292, "byteLength": 8},
                    {"buffer": 0, "byteOffset": 300, "byteLength": 32},
                    {"buffer": 0, "byteOffset": 332, "byteLength": 4},
                    {"buffer": 0, "byteOffset": 336, "byteLength": 12}
                  ],
                  "accessors": [
                    {"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 0, "byteOffset": 12, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 1, "componentType": 5126, "count": 3, "type": "VEC2"},
                    {"bufferView": 2, "componentType": 5123, "count": 3, "type": "SCALAR"},
                    {"bufferView": 3, "componentType": 5121, "count": 3, "type": "VEC4"},
                    {"bufferView": 4, "componentType": 5121, "normalized": true, "count": 3, "type": "VEC4"},
                    {"bufferView": 5, "componentType": 5126, "count": 2, "type": "MAT4"},
                    {"bufferView": 6, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 7, "componentType": 5126, "count": 2, "type": "SCALAR"},
                    {"bufferView": 8, "componentType": 5126, "count": 2, "type": "VEC4"},
                    {"bufferView": 9, "componentType": 5126, "count": 1, "type": "SCALAR"},
                    {"bufferView": 10, "componentType": 5126, "count": 1, "type": "VEC3"}
                  ]
                }
                """.formatted(buffer);
    }

    static byte[] embedded() {
        return json("data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bin()))
                .getBytes(StandardCharsets.UTF_8);
    }

    static byte[] external(String binName) {
        return json(binName).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] glb() {
        byte[] json = padded(json(null).getBytes(StandardCharsets.UTF_8), (byte) ' ');
        byte[] bin = padded(bin(), (byte) 0);
        int length = 12 + 8 + json.length + 8 + bin.length;
        ByteBuffer b = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(0x46546C67).putInt(2).putInt(length);
        b.putInt(json.length).putInt(0x4E4F534A).put(json);
        b.putInt(bin.length).putInt(0x004E4942).put(bin);
        return b.array();
    }

    private static byte[] padded(byte[] data, byte pad) {
        int length = (data.length + 3) & ~3;
        byte[] out = new byte[length];
        System.arraycopy(data, 0, out, 0, data.length);
        for (int i = data.length; i < length; i++) {
            out[i] = pad;
        }
        return out;
    }
}
