package com.farias.rengine.renderer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

public final class Mesh implements Disposable {

    private final int vao;
    private final int vbo; // Atua como v_id (Buffer de Posições)
    private final int ebo; // Atua como i_id (Buffer de Índices)
    
    // Armazenam os IDs adicionais gerados para que possamos deletá-los no dispose()
    private final int textureVbo; 
    private final int normalVbo;

    private final int indexCount;
    private final Bounds bounds;

    public Mesh(MeshData data) {
        // 1. Gerar e vincular o VAO global
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // 2. Upload das Posições (VBO principal)
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, createFloatBuffer(data.vertices()), GL_STATIC_DRAW);
        glEnableVertexAttribArray(0); // Atributo 0: Posição
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        // 3. Upload das Coordenadas de Textura
        this.textureVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, textureVbo);
        glBufferData(GL_ARRAY_BUFFER, createFloatBuffer(data.textures()), GL_STATIC_DRAW);
        glEnableVertexAttribArray(1); // Atributo 1: Textura
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

        // 4. Upload dos Vetores Normais
        this.normalVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, normalVbo);
        glBufferData(GL_ARRAY_BUFFER, createFloatBuffer(data.normals()), GL_STATIC_DRAW);
        glEnableVertexAttribArray(2); // Atributo 2: Normais
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

        // 5. Upload dos Índices (EBO)
        this.ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(data.indices().length);
        indexBuffer.put(data.indices()).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // 6. Limpar vinculação
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        this.indexCount = data.indices().length;
        this.bounds = Bounds.of(data.vertices());
    }

    /** Limites do mesh no espaço local (antes da matriz de modelo); vazios se não houver vértices. */
    public Bounds bounds() {
        return bounds;
    }

    public void draw() {
        // Como os atributos foram ativados dentro do escopo do VAO no construtor,
        // o VAO já guardou o estado de ativação deles (0, 1 e 2).
        glBindVertexArray(vao);
        
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        
        glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        // Deleta todos os buffers criados na memória da GPU
        glDeleteBuffers(vbo);
        glDeleteBuffers(textureVbo);
        glDeleteBuffers(normalVbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }
}
