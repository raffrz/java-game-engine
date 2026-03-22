package com.farias.rengine.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

public class Launcher {
    public static void main(String[] args) {

        Map<String, Runnable> demos = new HashMap<>();

        demos.put("1 - SpriteMovement", SpriteMovement::launch);
        demos.put("2 - BasicTopDownMovement", BasicTopDownMovement::launch);
        demos.put("3 - Simple2DGame", Simple2DGame::launch);
        demos.put("4 - SpriteEditor", SpriteEditor::launch);
        demos.put("5 - Snake", SnakeGame::launch);

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
