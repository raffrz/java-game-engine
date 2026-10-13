package com.farias.jengine.assets;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.farias.jengine.renderer.Renderer;
import com.farias.jengine.renderer.Texture;

/**
 * Servico de assets: resolve um caminho relativo primeiro a partir da raiz
 * configurada em {@link AssetsModule} e, se nao existir la, no classpath.
 * Texturas sao cacheadas por caminho normalizado, entao pedir o mesmo arquivo
 * duas vezes devolve a mesma Texture (e uma unica textura na GPU).
 */
public final class Assets {
    private final Renderer renderer;
    private final Path root;
    private final Map<String, Texture> textures = new HashMap<>();

    Assets(Renderer renderer, Path root) {
        this.renderer = renderer;
        this.root = root;
    }

    /**
     * Carrega (ou devolve do cache) a textura do caminho dado. A textura e
     * propriedade do Renderer, que a libera em dispose.
     */
    public Texture texture(String path) {
        String key = normalize(path);
        Texture cached = textures.get(key);
        if (cached != null) {
            return cached;
        }
        try (InputStream in = open(key)) {
            Texture texture = TextureLoader.load(renderer, in, key);
            textures.put(key, texture);
            return texture;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load texture '" + key + "'", e);
        }
    }

    /**
     * Abre o asset para leitura (raiz, depois classpath). Nao cacheia; quem
     * chama fecha o stream.
     */
    public InputStream open(String path) {
        String key = normalize(path);
        Path file = root.resolve(key);
        try {
            if (Files.isRegularFile(file)) {
                return Files.newInputStream(file);
            }
            InputStream in = Assets.class.getClassLoader().getResourceAsStream(key);
            if (in == null) {
                throw new IOException("Asset not found: '" + key + "' (looked in "
                        + root.toAbsolutePath() + " and on the classpath)");
            }
            return in;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Esvazia o cache. Nao libera GPU: as texturas pertencem ao Renderer.
     */
    void clear() {
        textures.clear();
    }

    private static String normalize(String path) {
        return Path.of(path).normalize().toString().replace('\\', '/');
    }
}
