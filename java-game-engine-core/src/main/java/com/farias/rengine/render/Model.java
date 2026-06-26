package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

public class Model {
	
	private int draw_count;
    private int vao_id;
    private int v_id, t_id, n_id, i_id;

	FloatBuffer tex_coords;
    FloatBuffer normals;
    
    public Model(float[] vertices, float[] tex_coords, int[] indices) {
        this(vertices, tex_coords, new float[vertices.length], indices);
    }
	
	public Model(float[] vertices, float[] tex_coords, float[] normals, int[] indices) {
		draw_count = indices.length;
		
        // 1. Create and bind the VAO
        vao_id = glGenVertexArrays();
        glBindVertexArray(vao_id);

        // 2. Set up Vertex Buffer (VBO)
		v_id = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, v_id);
        glBufferData(GL_ARRAY_BUFFER, createBuffer(vertices), GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		
        // 3. Set up Texture Coordinate Buffer
		t_id = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, t_id);
		this.tex_coords = createBuffer(tex_coords);
        glBufferData(GL_ARRAY_BUFFER, this.tex_coords, GL_STATIC_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        
        // 4. Set up Normals Buffer
        n_id = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, n_id);
        this.normals = createBuffer(normals);
        glBufferData(GL_ARRAY_BUFFER, this.normals, GL_STATIC_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		
        // 5. Set up Index Buffer (IBO / EBO)
		i_id = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, i_id);
		IntBuffer buffer = BufferUtils.createIntBuffer(indices.length);
		buffer.put(indices);
		buffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        
        // 6. Unbind the VAO
        glBindVertexArray(0);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
	
	public void draw() {
        glBindVertexArray(vao_id); // 1. Ativa o VAO
        
        glDrawElements(GL_TRIANGLES, draw_count, GL_UNSIGNED_INT, 0); // 2. Desenha!
        
        glBindVertexArray(0);
    }
	
	public void setTexCoords(float[] tex_coords) {
		this.tex_coords.clear();
		this.tex_coords.put(tex_coords);
		this.tex_coords.flip();
		glBindBuffer(GL_ARRAY_BUFFER, t_id);
		glBufferData(GL_ARRAY_BUFFER, this.tex_coords, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
	
	private FloatBuffer createBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
	public int getV_id() {
		return v_id;
	}
	
	public int getT_id() {
		return t_id;
	}
	
	public int getI_id() {
		return i_id;
	}
	
    public int getDraw_count() {
        return draw_count;
    }
    
    public void cleanUp() {
        glDisableVertexAttribArray(0);
        glDeleteBuffers(v_id);
        glDeleteBuffers(t_id);
        glDeleteBuffers(n_id);
        glDeleteBuffers(i_id);
        glDeleteVertexArrays(vao_id);
    }
}
