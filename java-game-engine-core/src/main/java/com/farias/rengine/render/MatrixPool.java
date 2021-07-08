package com.farias.rengine.render;

import org.joml.Matrix4f;

public class MatrixPool {
	
//	private static int size = (int) Math.pow(2, 24);
//	
//	private static final Matrix4f[] pool = new Matrix4f[size];
//	
//	private static int index = 0;
//	
//	private static final Matrix4f EMPTY_MATRIX = new Matrix4f();
//	
//	static {
//		for (int i = 0; i < pool.length; i++) {
//			pool[i] = new Matrix4f();
//		}
//	}
	
	public static final Matrix4f getInstance() {
//		if (index >= size) {
//			index = 0;
//		}
//		return pool[index++];
		return new Matrix4f();
	}
	
	public static void clear() {
//		for (int i = 0; i < pool.length; i++) {
//			pool[i].set(EMPTY_MATRIX);
//		}
	}

}
