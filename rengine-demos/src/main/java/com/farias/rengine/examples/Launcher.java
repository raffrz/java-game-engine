package com.farias.rengine.examples;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.farias.rengine.examples.chess.IsometricChess;

public class Launcher {
    public static void main(String[] args) {

        Map<String, Runnable> demos = new LinkedHashMap<>();

        demos.put("1 - Snake", SnakeGame::launch);
        demos.put("2 - SpriteEditor", SpriteEditor::launch);
        demos.put("3 - ModelViewer", ModelViewer::launch);
        demos.put("4 - IsometricChess", IsometricChess::launch);
        demos.put("5 - ShadowCubeDemo", ShadowCubeDemo::launch);

        System.out.println("Selecione uma opção:");
        demos.keySet().forEach(System.out::println);

        var scanner = new Scanner(System.in);
        var line = scanner.next();

        demos.entrySet().stream()
                .filter(k -> k.getKey().startsWith(String.valueOf(line)))
                .findFirst().map(Entry::getValue)
                .ifPresent(Runnable::run);

        scanner.close();
    }
}
