package com.farias.rengine.ecs.gfx;

import org.joml.Vector2f;

import com.farias.rengine.ecs.Component;
import com.farias.rengine.ecs.Transform;
import com.farias.rengine.ecs.Velocity;
import com.farias.rengine.ecs.render.Camera;
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
	float passed;
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
	public void update(float deltaTime) {
		//TODO Create an Animation component
		//animate sprites
		passed += deltaTime;
		if (passed >= 1f / 60f * 10) {
			Velocity v = (Velocity) getGameObject().getComponent("velocity");
			if (v == null)
				return;
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
		int tx = (int) (tile % hTiles);
		int ty = (int) (tile / vTiles);
		float[] texCoords = new float[] {
			sx * tx, sy * ty,
			sx * tx + sx, sy * ty,
			sx * tx + sx, sy * ty + sy,
			sx * tx, sy * ty + sy,
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
