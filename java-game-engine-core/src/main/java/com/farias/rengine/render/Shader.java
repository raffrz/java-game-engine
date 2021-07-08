package com.farias.rengine.render;

import static org.lwjgl.opengl.GL20.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

public class Shader {
	
	private int program;
	private int vs;
	private int fs;
	
	public Shader(String filename) {
		program = glCreateProgram();
		
		vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, readFile(filename + ".vs"));
		glCompileShader(vs);
		if (glGetShaderi(vs, GL_COMPILE_STATUS) != 1) {
			java.lang.System.err.println(glGetShaderInfoLog(vs));
			java.lang.System.exit(1);
		}
		
		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, readFile(filename + ".fs"));
		glCompileShader(fs);
		if (glGetShaderi(fs, GL_COMPILE_STATUS) != 1) {
			java.lang.System.err.println(glGetShaderInfoLog(fs));
			java.lang.System.exit(1);
		}
		
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		
		glBindAttribLocation(program, 0, "vertices");
		glBindAttribLocation(program, 1, "textures");
		
		glLinkProgram(program);
		if (glGetProgrami(program, GL_LINK_STATUS) != 1) {
			java.lang.System.err.println(glGetProgramInfoLog(program));
			java.lang.System.exit(0);
		}
		glValidateProgram(program);
		if (glGetProgrami(program, GL_VALIDATE_STATUS) != 1) {
			java.lang.System.err.println(glGetProgramInfoLog(program));
			java.lang.System.exit(0);
		}
	}
	
	public void setUniform(String name, int value) {
		int location = glGetUniformLocation(program, name);
		if (location != -1) {
			glUniform1i(location, value);
		}
	}
	
	//sets the matrix responsible for the scale, traslation, rotation and projection in the shader
	public void setUniform(String name, Matrix4f value) {
		int location = glGetUniformLocation(program, name);
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		value.get(buffer);
		if (location != -1) {
			glUniformMatrix4fv(location, false, buffer);
		}
	}
	
	public void bind() {
		glUseProgram(program);
	}
	
	private String readFile(String filename) {
		StringBuilder string = new StringBuilder();
		BufferedReader br;
		InputStream in = getClass().getClassLoader().getResourceAsStream("shaders/" + filename);
		try {
			br = new BufferedReader(new InputStreamReader(in));
			String line;
			while ((line = br.readLine()) != null) {
				string.append(line);
				string.append("\n");
			}
			br.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return string.toString();
	}

}
