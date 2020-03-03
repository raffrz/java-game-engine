package com.farias.rengine.gfx;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.farias.rengine.Component;
import com.farias.rengine.Transform;
import com.farias.rengine.render.Camera;
import com.farias.rengine.render.Model;
import com.farias.rengine.render.Shader;
import com.farias.rengine.render.Texture;

public class Sprite extends Component {
	Vector2f dimension;
	private Model model;
	private String textureName;
	private Texture texture;
	private Shader shader;
	int tile;
	boolean tileChanged;
	long passed;
	private boolean visible;
	
	//how can i convert this float values to pixels width and height and vice versa?
	static float[] vertices = new float[] {
		-1f, 1f, 0,   //TOP LEFT      0
		1f, 1f, 0,    //TOP RIGHT     1
		1f, -1f, 0,   //BOTTOM RIGHT  2
		-1f, -1f, 0,  //BOTTOM LEFT   3
	};
	
	static int[] indices = new int[] {
		0,1,2,
		2,3,0
	};
	
	public Sprite(String textureName, int tile, float width, float height) {
		this.dimension = new Vector2f(width, height);
		this.tile = tile;
		this.textureName = textureName;
	}
	
	@Override
	public void onInit() {
		this.texture = new Texture(textureName);
		this.shader = new Shader("shader");
		float[] texCoords = createTexCoords(tile);
		this.model = new Model(vertices, texCoords, indices);
		this.show();
	}
	
	@Override
	public void update(long deltaTime) {
		//TODO Create an Animation component
		//animate sprites
		passed += deltaTime;
		if (passed >= 1000 / 60 * 10) {
			Velocity v = (Velocity) getGameObject().getComponent("velocity");
			if (v.getVx() != 0 || v.getVy() != 0) {
				tile++;
			} else {
				tile = 0;
			}
			model.setTexCoords(createTexCoords(tile));
			passed = 0;
		}
	}
	
	public void draw(Camera camera, Transform transform, int sampler) {
		if (!visible) {
			return;
		}
		shader.bind();
		texture.bind(sampler);
//		Matrix4f tile_pos = new Matrix4f().translate(new Vector3f(transform.getPosition().x * 2, 
//				transform.getPosition().y * 2, 0));
//		Matrix4f target = new Matrix4f();
//		
//		camera.getProjection().scale(transform.getScale(), target);
//		target.mul(tile_pos);
		
		shader.setUniform("sampler", 0);
		shader.setUniform("projection", transform.getProjection(camera.getProjection()));
		model.draw();
	}
	
	public void show() {
		this.visible = true;
	}
	
	public void hide() {
		this.visible = false;
	}
	
	private float[] createTexCoords(int tile) {
		float hTiles = texture.getWidth() / dimension.x;
		float vTiles = texture.getHeight() / dimension.y;
		float sx = 1 / hTiles;
		float sy = 1 / vTiles;
		float[] texCoords = new float[] {
			sx * tile, sy * tile,
			sx * tile + sx, sy * tile,
			sx * tile + sx, sy * tile + sy,
			sx * tile, sy * tile + sy,
		};
		return texCoords;
	}

	public Vector2f getDimension() {
		return dimension;
	}
	
	public int getTile() {
		return tile;
	}
	
	public Model getModel() {
		return model;
	}
	
	public Texture getTexture() {
		return texture;
	}
	
	public Shader getShader() {
		return shader;
	}
}
