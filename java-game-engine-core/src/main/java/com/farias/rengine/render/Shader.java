package com.farias.rengine.render;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

public class Shader {

    private final int program;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public Shader(String filename) {
        program = glCreateProgram();

        int vs = createShader(readFile(filename + ".vs"), GL_VERTEX_SHADER);
        int fs = createShader(readFile(filename + ".fs"), GL_FRAGMENT_SHADER);
        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                "Shader link error:\n" +
                glGetProgramInfoLog(program)
            );
        }

        // Pode apagar após o link
        glDetachShader(program, vs);
        glDetachShader(program, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private int createShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException(
                    "Erro ao compilar shader:\n" +
                    glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private int getUniformLocation(String name) {
        Integer location = uniformCache.get(name);
        if (location != null) {
            return location;
        }
        int uniform = glGetUniformLocation(program, name);
        if (uniform == -1) {
            System.err.println(
                    "Uniform não encontrado: " + name);
        }
        uniformCache.put(name, uniform);
        return uniform;
    }

    public void setUniform(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }

    public void setUniform(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public void setUniform(String name, Vector3f value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform3f(location,
                    value.x,
                    value.y,
                    value.z);
        }
    }

    public void setUniform(String name, Matrix3f value) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(9);
        value.get(buffer);
        glUniformMatrix3fv(location, false, buffer);
    }

    public void setUniform(String name, Matrix4f value) {
        int location = getUniformLocation(name);
        if (location == -1)
            return;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        value.get(buffer);
        glUniformMatrix4fv(location, false, buffer);
    }

    public void bind() {
        glUseProgram(program);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void destroy() {
        glDeleteProgram(program);
    }

    private String readFile(String filename) {
        StringBuilder string = new StringBuilder();
        InputStream in = getClass().getClassLoader()
                        .getResourceAsStream("shaders/" + filename);
        if (in == null) {
            throw new RuntimeException(
                    "Shader não encontrado: shaders/" + filename);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                string.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Erro ao ler shader: " + filename, e);
        }
        return string.toString();
    }
}