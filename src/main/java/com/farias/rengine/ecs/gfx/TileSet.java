package com.farias.rengine.ecs.gfx;

import com.farias.rengine.ecs.Component;
import com.farias.rengine.render.Texture;

public class TileSet extends Component {
	String path;
	Texture texture;
	int width;
	int height;
	
	public static TileSet load(String filename, int width, int heigth) {
		TileSet t = new TileSet();
		t.height = heigth;
		t.width = width;
		t.path = filename;
		return t;
	}
	
	@Override
	public void onInit() {
		texture = new Texture(path);
	}
	
	public void bindTexture(int sampler) {
		this.texture.bind(sampler);
	}
}