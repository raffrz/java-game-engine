package com.farias.jengine.examples.city;

import java.util.Random;

import com.farias.procarch.buildings.Buildings;
import com.farias.procarch.buildings.GroundFloor;
import com.farias.procarch.buildings.SteppedTowerPreset;
import com.farias.procarch.buildings.TowerPreset;
import com.farias.procarch.city.Cities;
import com.farias.procarch.generation.GeneratedAsset;
import com.farias.procarch.generation.GenerationContext;
import com.farias.procarch.houses.Houses;
import com.farias.procarch.lots.LotSide;
import com.farias.procarch.lots.LotSurface;
import com.farias.procarch.lots.Lots;
import com.farias.procarch.parks.Courtyards;
import com.farias.procarch.parks.Parks;
import com.farias.procarch.props.StreetFurniture;
import com.farias.procarch.props.Vehicles;
import com.farias.procarch.streets.Streets;
import com.farias.procarch.vegetation.Trees;
import com.farias.procgeo.core.geometry.GeometrySet;

/**
 * Tipos de cidade montados so com a API publica da procedural-architecture: o primeiro e a cidade pronta
 * ({@code Cities.grid()}); os outros compoem predios, casas, parques, ruas e mobiliario com opcoes, presets e seeds
 * diferentes. Cada tipo usa a seed para sortear o que vai em cada quarteirao.
 */
enum CityStyles {

    GRID("Cidade em grade (Cities.grid)") {
        @Override
        GeometrySet build(long seed) {
            return Cities.grid().blocks(5).seed(seed).generate().geometry();
        }
    },

    DOWNTOWN("Centro financeiro") {
        @Override
        GeometrySet build(long seed) {
            Random rnd = new Random(seed);
            StreetGrid grid = new StreetGrid(4, 36, 12);
            Scene scene = grid.streets(new Scene(), seed);
            for (int i = 0; i < grid.blocks; i++) {
                for (int j = 0; j < grid.blocks; j++) {
                    double x = grid.blockCenter(i), z = grid.blockCenter(j);
                    double yaw = 90 * rnd.nextInt(4);
                    // quanto mais perto do centro, mais alto (silhueta de centro)
                    double centrality = 1 - (Math.abs(x) + Math.abs(z)) / (grid.size());
                    int extra = (int) Math.round(centrality * 14);
                    int pick = rnd.nextInt(10);
                    GeneratedAsset building;
                    if (pick < 4) {
                        building = Buildings.steppedTower()
                                .preset(rnd.nextBoolean() ? SteppedTowerPreset.SKYSCRAPER : SteppedTowerPreset.SKYSCRAPER_WIDE)
                                .floors(14 + extra + rnd.nextInt(6)).seed(rnd.nextLong()).generate();
                    } else if (pick < 7) {
                        building = Buildings.tower().preset(TowerPreset.MIDRISE)
                                .footprint(18 + 3 * rnd.nextInt(3), 15 + 3 * rnd.nextInt(3))
                                .floors(10 + extra + rnd.nextInt(8)).windowDensity(0.6 + 0.3 * rnd.nextDouble())
                                .seed(rnd.nextLong()).generate();
                    } else if (pick < 9) {
                        building = Buildings.roundTower().radius(7 + rnd.nextInt(4)).floors(14 + extra + rnd.nextInt(10))
                                .segments(24 + 4 * rnd.nextInt(3)).seed(rnd.nextLong()).generate();
                    } else {
                        building = Parks.urban().size(30).treeDensity(0.035).seed(rnd.nextLong()).generate();
                    }
                    scene.add(building, x, z, yaw, rnd.nextDouble());
                }
            }
            return scene.build();
        }
    },

    SUBURB("Bairro residencial") {
        @Override
        GeometrySet build(long seed) {
            Random rnd = new Random(seed);
            StreetGrid grid = new StreetGrid(4, 32, 8);
            Scene scene = grid.streets(new Scene(), seed);
            GeometrySet[] trees = {Trees.tree().generate().geometry(),
                    Trees.broadleaf().crownRadius(2.2).crownStretch(0.8).generate().geometry(),
                    Trees.broadleaf().crownRadius(1.1).crownStretch(2.2).trunkHeight(1.4).generate().geometry()};
            for (int i = 0; i < grid.blocks; i++) {
                for (int j = 0; j < grid.blocks; j++) {
                    double x = grid.blockCenter(i), z = grid.blockCenter(j);
                    int pick = rnd.nextInt(10);
                    if (pick < 6) {
                        // conjunto de quatro casas com tamanhos sorteados
                        scene.add(Houses.block().houseWidth(rnd.nextBoolean() ? 6 : 9).houseDepth(6).seed(rnd.nextLong()).generate(),
                                x, z, 90 * rnd.nextInt(4));
                    } else if (pick < 8) {
                        // duas casas grandes isoladas e um pomar
                        for (int side = -1; side <= 1; side += 2) {
                            scene.add(Houses.detached().footprint(9, 6 + 3 * rnd.nextInt(2)).windowDensity(0.4 + 0.4 * rnd.nextDouble())
                                    .seed(rnd.nextLong()).generate(), x + side * 7, z - 5, 0);
                        }
                        for (int t = 0; t < 5; t++) {
                            scene.add(trees[rnd.nextInt(trees.length)], x - 10 + 5 * t, 0, z + 9, rnd.nextInt(360), rnd.nextDouble());
                        }
                    } else {
                        scene.add(Parks.urban().size(26).treeDensity(0.05).seed(rnd.nextLong()).generate(), x, z, 0);
                    }
                }
            }
            return scene.build();
        }
    },

    INDUSTRIAL("Distrito industrial") {
        /** Vagas de 3 m; as fileiras de Lots.paved() ficam a 0,8 m da borda e tem 5 m de profundidade. */
        private static final double STALL = 3.0;

        @Override
        GeometrySet build(long seed) {
            Random rnd = new Random(seed);
            StreetGrid grid = new StreetGrid(3, 48, 14);
            Scene scene = grid.streets(new Scene(), seed);
            GeometrySet car = Vehicles.car().generate().geometry();
            GeometrySet lamp = StreetFurniture.lamp().generate().geometry();
            // lote de concreto ocupando o quarteirao ate a calcada (que entra 2,5 m), com meio-fio marcando a divisa;
            // um lote por arranjo de vagas, reaproveitado em todos os quarteiroes iguais
            double lotSize = grid.blockSize - 5;
            GeometrySet plainLot = Lots.paved().size(lotSize, lotSize).surface(LotSurface.CONCRETE).generate().geometry();
            GeometrySet southLot = Lots.paved().size(lotSize, lotSize).parkingRow(LotSide.SOUTH).stallWidth(STALL).generate().geometry();
            GeometrySet bothLot = Lots.paved().size(lotSize, lotSize).parkingRow(LotSide.SOUTH).parkingRow(LotSide.NORTH).stallWidth(STALL)
                    .generate().geometry();
            double rowZ = lotSize / 2 - 0.8 - 2.5;
            for (int i = 0; i < grid.blocks; i++) {
                for (int j = 0; j < grid.blocks; j++) {
                    double x = grid.blockCenter(i), z = grid.blockCenter(j);
                    int pick = rnd.nextInt(3);
                    if (pick == 0) {
                        // galpao grande (terreo sem lojas) com estacionamento ao sul
                        scene.add(southLot, x, 0, z, 0, -1);
                        scene.add(Buildings.tower().preset(TowerPreset.WAREHOUSE).groundFloor(GroundFloor.WINDOWS).balconyDensity(0)
                                .footprint(33, 21).floors(1 + rnd.nextInt(2)).floorHeight(5 + rnd.nextDouble()).seed(rnd.nextLong()).generate(),
                                x, z - 6, 0, rnd.nextDouble());
                        parkCars(scene, car, rnd, lotSize, x, z + rowZ);
                    } else if (pick == 1) {
                        // fabrica: chamines de tijolo atras de um galpao, patio sem vagas
                        scene.add(plainLot, x, 0, z, 0, -1);
                        int stacks = 1 + rnd.nextInt(2);
                        for (int s = 0; s < stacks; s++) {
                            double base = 2 + 0.6 * rnd.nextDouble();
                            scene.add(Buildings.smokestack().height(32 + rnd.nextInt(24)).baseRadius(base).topRadius(base * 0.6).generate(),
                                    stacks == 1 ? x : x - 8 + 16 * s, z - 12, 0, rnd.nextDouble());
                        }
                        scene.add(Buildings.tower().preset(TowerPreset.WAREHOUSE).groundFloor(GroundFloor.WINDOWS).balconyDensity(0)
                                .footprint(30, 18).seed(rnd.nextLong()).generate(), x, z + 8, 0, rnd.nextDouble());
                    } else {
                        // escritorios em lamina no meio, estacionamento ao norte e ao sul
                        scene.add(bothLot, x, 0, z, 0, -1);
                        scene.add(Buildings.tower().preset(TowerPreset.SLAB).groundFloor(GroundFloor.WINDOWS).balconyDensity(0)
                                .floors(5 + rnd.nextInt(4)).seed(rnd.nextLong()).generate(), x, z, 0, rnd.nextDouble());
                        parkCars(scene, car, rnd, lotSize, x, z + rowZ);
                        parkCars(scene, car, rnd, lotSize, x, z - rowZ);
                    }
                    // postes nos cantos do lote, fora das fileiras de vagas
                    double corner = lotSize / 2 - 1;
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sz = -1; sz <= 1; sz += 2) {
                            scene.add(lamp, x + sx * corner, 0.1, z + sz * corner, 0, -1);
                        }
                    }
                }
            }
            return scene.build();
        }

        /** Carros nas vagas de uma fileira centrada em (x, z): mesmas contas do Lots.paved() (vagas inteiras, centradas). */
        private void parkCars(Scene scene, GeometrySet car, Random rnd, double lotSize, double x, double z) {
            int stalls = Math.max(1, (int) Math.floor((lotSize - 2 * 0.8) / STALL));
            double first = -stalls * STALL / 2 + STALL / 2;
            for (int c = 0; c < stalls; c++) {
                if (rnd.nextDouble() < 0.7) {
                    scene.add(car, x + first + c * STALL, 0.1, z, rnd.nextBoolean() ? 0 : 180, rnd.nextDouble());
                }
            }
        }
    },

    BOULEVARD("Bulevar") {
        @Override
        GeometrySet build(long seed) {
            Random rnd = new Random(seed);
            double length = 280;
            Scene scene = new Scene().ground(380);
            scene.add(Streets.road().length(length).width(14).crossingSpacing(70).seed(seed).generate(), 0, 0, 0);
            GeneratedAsset sidewalk = Streets.sidewalk().length(length).width(8).generate();
            scene.add(sidewalk, -11, 0, 0).add(sidewalk, 11, 0, 0);
            GeometrySet streetTree = StreetFurniture.streetTree().generate().geometry();
            GeometrySet kit = StreetFurniture.streetKit().generate().geometry();
            for (double z = -133; z <= 133; z += 14) {
                for (int side = -1; side <= 1; side += 2) {
                    scene.add(streetTree, side * 12.5, 0.06, z, rnd.nextInt(360), rnd.nextDouble());
                }
            }
            for (double z = -126; z <= 126; z += 42) {
                scene.add(kit, -9, 0.06, z, 90, -1).add(kit, 9, 0.06, z, -90, -1);
            }
            // predios dos dois lados, mais altos no meio do bulevar
            for (double z = -126; z <= 126; z += 28) {
                for (int side = -1; side <= 1; side += 2) {
                    int floors = 6 + (int) Math.round((1 - Math.abs(z) / 140) * 18) + rnd.nextInt(4);
                    GeneratedAsset building = switch (rnd.nextInt(3)) {
                        case 0 -> Buildings.tower().footprint(18, 15).floors(floors).floorHeight(3 + 0.4 * rnd.nextDouble())
                                .seed(rnd.nextLong()).generate();
                        case 1 -> Buildings.roundTower().radius(7).floors(floors).seed(rnd.nextLong()).generate();
                        default -> Buildings.steppedTower().footprint(15, 15).floors(Math.max(2, floors)).seed(rnd.nextLong()).generate();
                    };
                    scene.add(building, side * 27, z, side < 0 ? 90 : -90, rnd.nextDouble());
                }
            }
            // um patio e um parque nas pontas
            scene.add(Courtyards.residential().generate(GenerationContext.withSeed(seed)), 0, -165, 0);
            scene.add(Parks.urban().size(40).treeDensity(0.04).seed(seed).generate(), 0, 162, 0);
            return scene.build();
        }
    };

    private final String label;

    CityStyles(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }

    abstract GeometrySet build(long seed);

    /**
     * Grade de ruas no mesmo arranjo do {@code Cities.grid()}: eixos a cada {@code blockSize + streetWidth}, calcadas 5 m
     * mais largas que o asfalto, um conjunto poste/hidrante/lixeira em cada canto de quarteirao, arvores de rua no meio
     * de cada lado e semaforos nos cruzamentos.
     */
    private static final class StreetGrid {
        final int blocks;
        final double blockSize;
        final double streetWidth;
        final double pitch;

        StreetGrid(int blocks, double blockSize, double streetWidth) {
            this.blocks = blocks;
            this.blockSize = blockSize;
            this.streetWidth = streetWidth;
            this.pitch = blockSize + streetWidth;
        }

        double size() {
            return blocks * pitch;
        }

        double blockCenter(int i) {
            return -size() / 2 + (i + 0.5) * pitch;
        }

        double crossing(int i) {
            return -size() / 2 + i * pitch;
        }

        Scene streets(Scene scene, long seed) {
            scene.ground(size() + pitch);
            // as duas grades ficam na mesma altura por padrao: a calcada desce para o asfalto ficar por cima
            scene.add(Streets.sidewalkGrid().blocks(blocks).pitch(pitch).width(streetWidth + 5).generate().geometry(), 0, -0.04, 0, 0, -1);
            scene.add(Streets.roadGrid().blocks(blocks).pitch(pitch).width(streetWidth).seed(seed).generate().geometry(), 0, 0.02, 0, 0, -1);
            GeometrySet kit = StreetFurniture.streetKit().generate().geometry();
            GeometrySet tree = StreetFurniture.streetTree().generate().geometry();
            GeometrySet signal = StreetFurniture.signal().generate().geometry();
            Random rnd = new Random(seed ^ 0x5EED);
            double corner = (blockSize - 2.4) / 2;
            for (int i = 0; i < blocks; i++) {
                for (int j = 0; j < blocks; j++) {
                    double x = blockCenter(i), z = blockCenter(j);
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sz = -1; sz <= 1; sz += 2) {
                            scene.add(kit, x + sx * corner, 0.06, z + sz * corner, 0, -1);
                        }
                    }
                    scene.add(tree, x, 0.09, z - corner, rnd.nextInt(360), rnd.nextDouble())
                            .add(tree, x, 0.09, z + corner, rnd.nextInt(360), rnd.nextDouble())
                            .add(tree, x - corner, 0.09, z, rnd.nextInt(360), rnd.nextDouble())
                            .add(tree, x + corner, 0.09, z, rnd.nextInt(360), rnd.nextDouble());
                }
            }
            double offset = streetWidth / 2 + 1.3;
            for (int i = 0; i <= blocks; i++) {
                for (int j = 0; j <= blocks; j++) {
                    for (int sx = -1; sx <= 1; sx += 2) {
                        for (int sz = -1; sz <= 1; sz += 2) {
                            scene.add(signal, crossing(i) + sx * offset, 0.09, crossing(j) + sz * offset, 0, -1);
                        }
                    }
                }
            }
            return scene;
        }
    }
}
