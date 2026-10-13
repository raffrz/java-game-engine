package com.farias.rengine.renderer;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

public class Shader implements Disposable {

    private int program;
    private final Map<String, Integer> uniforms = new HashMap<>();

    // Reutilizados a cada uniform de matriz: alocar um direct buffer por draw
    // call e caro. Seguro porque o contexto GL e de uma thread so.
    private final FloatBuffer matrix4Buffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer matrix3Buffer = BufferUtils.createFloatBuffer(9);

    public Shader(String vertexSource, String fragmentSource) {
        // 1. Compile Vertex Shader
        int vertexShader = createShader(vertexSource, GL20.GL_VERTEX_SHADER);

        // 2. Compile Fragment Shader
        int fragmentShader = createShader(fragmentSource, GL20.GL_FRAGMENT_SHADER);

        // 3. Link Program
        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Failed to link program: " + GL20.glGetProgramInfoLog(program));
        }

        // Clean up individual shaders (they are linked into the program now)
        GL20.glDetachShader(program, vertexShader);
        GL20.glDetachShader(program, fragmentShader);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private int createShader(String vertexSource, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, vertexSource);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL20.GL_FALSE) {
            throw new RuntimeException("Failed to compile shader: " + GL20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void setInt(String name, int value) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        GL20.glUniform1i(location, value);
    }

    public void setFloat(String name, float value) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        GL20.glUniform1f(location, value);
    }

    public void setVector3f(String name, Vector3f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform3f(location,
                    value.x,
                    value.y,
                    value.z);
        }
    }

    public void setVector2f(String name, Vector2f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform2f(location,
                    value.x,
                    value.y);
        }
    }

    public void setMatrix4f(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        matrix.get(matrix4Buffer);
        GL20.glUniformMatrix4fv(location, false, matrix4Buffer);
    }

    public void setMatrix3f(String name, Matrix3f matrix) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        matrix.get(matrix3Buffer);
        GL20.glUniformMatrix3fv(location, false, matrix3Buffer);
    }
    
    private int getUniformLocation(String name) {
        Integer location = uniforms.get(name);
        if (location != null) {
            return location;
        }
        int uniform = GL20.glGetUniformLocation(program, name);
        if (uniform == -1) {
            uniforms.put(name, uniform);
            System.err.println("Warning: Uniform '" + name + "' not found in shader program.");
            return -1;
        }
        uniforms.put(name, uniform);
        return uniform;
    }

    public void bind() {
        GL20.glUseProgram(program);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    @Override
    public void dispose() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
    }
}
