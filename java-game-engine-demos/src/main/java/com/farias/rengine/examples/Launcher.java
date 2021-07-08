package com.farias.rengine.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;

public class Launcher {
    public static void main(String[] args) {
        Map<String, Consumer<String[]>> demos = new HashMap<>();

        demos.put("1-AmorousAdventures", AmorousAdventures::launch);
        demos.put("2-BasicTopDownMovement", BasicTopDownMovement::launch);
        demos.put("3-Simple2DGame", Simple2DGame::launch);
        demos.put("4-SpriteEditor", SpriteEditor::launch);

        System.out.println("Selecione uma opção:");
        for (String entry: demos.keySet()) {
            System.out.println(entry);
        }

        Scanner scanner = new Scanner(System.in);
        String line = scanner.next();

        Optional<String> option = demos.keySet().stream()
            .filter(k -> k.startsWith(String.valueOf(line)))
            .findFirst();

        option.ifPresent(o -> {
            demos.get(o).accept(args);
        });
        
        scanner.close();
    }
}
