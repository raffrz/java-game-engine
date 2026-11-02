package com.farias.jengine.examples.city;

import java.util.Map;

import com.farias.procgeo.core.math.Vec3;
import com.farias.procgeo.graph.GraphAsset;
import com.farias.procgeo.graph.GraphBuilder;
import com.farias.procgeo.graph.GraphInterface;
import com.farias.procgeo.graph.GraphLibrary;
import com.farias.procgeo.graph.GraphRef;
import com.farias.procgeo.graph.NodeInputDefinition;
import com.farias.procgeo.graph.PortType;
import com.farias.procgeo.graph.ValueKind;

/**
 * Os grafos da cidade, todos compostos so de nos genericos da biblioteca
 * procedural-geometry (o nucleo da lib nao conhece "cidade" nem "predio").
 * Cada asset e um subgrafo reutilizavel com interface publica, pensado para ser exportado e reaproveitado em outros jogos:
 *
 * <pre>
 * -- pecas genericas (sem nada de "cidade"): usadas por todos os assets abaixo --
 * box_part, cylinder_part   caixa (com bevel e suavizacao opcionais) e cilindro com posicao e material
 * line_points, grid_points, step_count   pontos em linha; pontos nos vertices de uma grade; quantidade de passos num comprimento
 * parapet, roof_gear, roof_mast, water_tank   laje com parapeito; aparelhos de teto; mastro; caixa d'agua
 * house_*, fence_*, hedge_unit   partes da casa (telhado, telhas, tabeiras, lucarnas, chamine, porta, varanda, quintal, cerca...)
 *
 * -- assets compostos --
 * windows       moldura, requadro em relevo, caixilho e vidro recuado (3 insets + 2 extrudes), vitrines com caixilho,
 *               marquises, peitoris, sacadas e venezianas com floreira (instancias nos centros dos vidros)
 * sill, balcony, shutters, awning   acessorios de janela
 * facade        grade vertical (cols x rows) com janelas e pilastras
 * tower_body    4 fachadas + pilares de canto + faixas de piso em todos os andares + soco de pedra
 * tower         corpo + cornija + laje com parapeito + casa de maquinas com porta + mastro, caixa d'agua e aparelhos de teto
 * stepped_tower torre em recuos coroada por uma torre menor de teto plano
 * round_tower   cilindro (circulo varrido por perfil vertical) + janelas + discos de piso + laje, parapeito e equipamentos
 * dormer        lucarna com janela
 * house         subgrafos: paredes + telhado de 2 aguas de telhas individuais + cumeeira, tabeiras, lucarnas, chamine, varanda com
 *               degraus/colunas/balaustrada, porta, caminho de lajotas, quintal com mureta, cerca de ripas, sebes e canteiros
 * house_block   quatro casas por quarteirao
 * bench, fountain, courtyard   banco; fonte; quatro blocos em volta de um patio com fonte, caminhos, bancos, postes e arvores
 * tree, broadleaf, park   arvores; parque com caminhos, lago com borda de pedras, bancos, postes, canteiros e arvores
 * lamp, signal, street_kit, street_tree   poste; semaforo; poste + hidrante + lixeira; arvore de rua com canteiro
 * car           carroceria, cabine trapezoidal, farois, lanternas, para-choques e rodas cilindricas (instancias)
 * street, pavement, road   faixa de rua; calcada com lajotas em relevo; faixa com tracejado, linhas laterais, meio-fio,
 *               faixas de pedestre, linhas de retencao, bueiros e carros
 * *_grid        ruas nas duas direcoes (instancias, com variantes sorteadas)
 * city          terreno + calcadas + ruas + mobiliario + arvores de rua + semaforos + um lote variado por quarteirao
 * </pre>
 */
final class CityGraphs {

    private CityGraphs() {
    }

    static GraphLibrary library() {
        return new GraphLibrary()
                .add(windows()).add(awning()).add(sill()).add(balcony()).add(shutters()).add(facade()).add(towerBody()).add(tower())
                .add(steppedTower()).add(roundTower()).add(dormer()).add(houseBlock()).add(bench()).add(fountain())
                .add(courtyard()).add(tree()).add(broadleaf()).add(park()).add(lamp()).add(signal()).add(streetKit()).add(streetTree())
                .add(car()).add(street()).add(pavement()).add(road()).add(sidewalkGrid()).add(roadGrid()).add(city())
                // house e seus subgrafos
                .add(windowPanesAsset()).add(shopfrontsAsset()).add(paneSpotsAsset()).add(windowFittingsAsset())
                .add(blockPointsAsset()).add(cityLotsAsset()).add(cityBlocksAsset()).add(cityCornerLampsAsset())
                .add(cityStreetTreesAsset()).add(citySignalsAsset())
                .add(parkPondAsset()).add(parkFurnitureAsset()).add(parkPlantableAsset()).add(parkScatterAsset())
                .add(parkTreesAsset()).add(parkBedsAsset())
                .add(roadZonesAsset()).add(roadDashesAsset()).add(roadEdgesAsset()).add(roadCurbsAsset()).add(roadCrosswalksAsset())
                .add(roadStopLinesAsset()).add(roadTrafficAsset())
                .add(linePointsAsset()).add(gridPointsAsset()).add(stepCountAsset()).add(parapetAsset()).add(roofGearAsset()).add(roofMastAsset()).add(waterTankAsset())
                .add(boxPartAsset()).add(cylinderPartAsset()).add(houseColumnAsset()).add(houseGableWindowAsset())
                .add(houseRoofShellAsset()).add(houseRoofTilesAsset()).add(houseRoofTrimAsset()).add(houseDormersAsset())
                .add(houseChimneyAsset()).add(houseDoorAsset()).add(houseBalustradeAsset()).add(housePorchAsset())
                .add(houseWalkwayAsset()).add(houseYardAsset()).add(fenceRailAsset()).add(fencePicketsAsset())
                .add(fenceRailsAsset()).add(fencePostsAsset()).add(houseFenceAsset()).add(hedgeUnitAsset())
                .add(houseHedgesAsset()).add(houseMailboxAsset()).add(houseBackTreesAsset()).add(houseAsset());
    }

    // ---- helpers para escrever grafos de forma enxuta ----

    /**
     * Porta de saida de um no ({@code GraphInterface.INPUT_NODE} = entrada publica do grafo). Quando {@code lazy} nao e nulo, e
     * o resultado de um {@code scalar_math} com constante que ainda nao virou no: {@code vec()} o embute no
     * {@code combine_vec3_transform} ((x, y, z) * scale + offset) e qualquer outro consumidor o materializa como no normal.
     */
    private record Ref(String node, String port, Lazy lazy) {
        Ref(String node, String port) {
            this(node, port, null);
        }
    }

    /** {@code result = a op b} (b constante) ainda sem no; equivale a {@code base * scale + offset}. */
    private static final class Lazy {
        final String op;
        final double b;
        final Ref a;
        final Ref base;
        final double scale;
        final double offset;
        boolean made;

        Lazy(String op, double b, Ref a, Ref base, double scale, double offset) {
            this.op = op;
            this.b = b;
            this.a = a;
            this.base = base;
            this.scale = scale;
            this.offset = offset;
        }
    }

    /** Cria o no de um valor adiado (e os de que ele depende), uma unica vez; devolve a porta real. */
    private static Ref real(GraphBuilder g, Ref r) {
        Lazy l = r.lazy();
        if (l == null) {
            return r;
        }
        if (!l.made) {
            l.made = true;
            node(g, r.node(), "core.scalar_math", "operation", l.op, "a", real(g, l.a), "b", l.b);
        }
        return new Ref(r.node(), r.port());
    }

    private static Ref in(String name) {
        return new Ref(GraphInterface.INPUT_NODE, name);
    }

    private static Ref out(String node, String port) {
        return new Ref(node, port);
    }

    /** Cria um no; pares (porta, valor): um Ref liga uma conexao, qualquer outro valor vira literal. */
    private static String node(GraphBuilder g, String id, String type, Object... kv) {
        wire(g, id, g.node(id, type), kv);
        return id;
    }

    private static String sub(GraphBuilder g, String id, String asset, Object... kv) {
        wire(g, id, g.subgraph(id, GraphRef.latest(asset)), kv);
        return id;
    }

    /** Chamada de subgrafo cuja saida publica e {@code geometry}. */
    private static Ref use(GraphBuilder g, String id, String asset, Object... kv) {
        sub(g, id, asset, kv);
        return out(id, "geometry");
    }

    private static void wire(GraphBuilder g, String id, GraphBuilder.NodeHandle handle, Object[] kv) {
        for (int i = 0; i < kv.length; i += 2) {
            String port = (String) kv[i];
            if (kv[i + 1] instanceof Ref r0) {
                Ref r = real(g, r0);
                g.connect(r.node(), r.port(), id, port);
            } else {
                handle.set(port, kv[i + 1]);
            }
        }
    }

    /**
     * Aritmetica de valores (FLOAT). Com {@code a} ligado e {@code b} constante em add/subtract/multiply/divide, o no so e
     * criado se alguem alem do {@link #vec} usar o resultado.
     */
    private static Ref math(GraphBuilder g, String id, String op, Object a, Object b) {
        if (a instanceof Ref ra && b instanceof Number nb) {
            double k = nb.doubleValue();
            Lazy inner = ra.lazy();
            Ref base = inner == null ? ra : inner.base;
            double s0 = inner == null ? 1 : inner.scale, o0 = inner == null ? 0 : inner.offset;
            double[] so = switch (op) {
                case "multiply" -> new double[] {s0 * k, o0 * k};
                case "add" -> new double[] {s0, o0 + k};
                case "subtract" -> new double[] {s0, o0 - k};
                case "divide" -> k == 0 ? null : new double[] {s0 / k, o0 / k};
                default -> null;
            };
            if (so != null) {
                return new Ref(id, "result", new Lazy(op, k, ra, base, so[0], so[1]));
            }
        }
        node(g, id, "core.scalar_math", "operation", op, "a", a, "b", b);
        return out(id, "result");
    }

    /** Aritmetica de Fields (por elemento). */
    private static Ref fmath(GraphBuilder g, String id, String op, Object a, Object b) {
        node(g, id, "core.math", "operation", op, "a", a, "b", b);
        return out(id, "result");
    }

    /** Vetor de tres componentes; os valores adiados de {@link #math} viram escala e deslocamento de um {@code combine_vec3_transform}. */
    private static Ref vec(GraphBuilder g, String id, Object x, Object y, Object z) {
        Object[] c = {x, y, z};
        String[] names = {"x", "y", "z"};
        double[] scale = {1, 1, 1}, offset = {0, 0, 0};
        boolean folded = false;
        for (int i = 0; i < 3; i++) {
            if (c[i] instanceof Ref r && r.lazy() != null) {
                scale[i] = r.lazy().scale;
                offset[i] = r.lazy().offset;
                c[i] = r.lazy().base;
                folded = true;
            }
        }
        if (!folded) {
            node(g, id, "core.combine_vec3", "x", x, "y", y, "z", z);
            return out(id, "vector");
        }
        node(g, id, "core.combine_vec3_transform", "x", c[0], "y", c[1], "z", c[2],
                "scale", new Vec3(scale[0], scale[1], scale[2]), "offset", new Vec3(offset[0], offset[1], offset[2]));
        return out(id, "vector");
    }

    private static Ref cmp(GraphBuilder g, String id, String op, Object a, Object b) {
        node(g, id, "core.compare", "operation", op, "a", a, "b", b);
        return out(id, "result");
    }

    private static Ref and(GraphBuilder g, String id, Ref a, Ref b) {
        node(g, id, "core.boolean_math", "operation", "and", "a", a, "b", b);
        return out(id, "result");
    }

    private static Ref not(GraphBuilder g, String id, Ref a) {
        node(g, id, "core.boolean_math", "operation", "not", "a", a);
        return out(id, "result");
    }

    /** Atributo booleano (FACE) lido como Field. */
    private static Ref flag(GraphBuilder g, String id, Object name) {
        node(g, id, "core.attribute", "name", name);
        return out(id, "bool");
    }

    /** Componentes da posicao do elemento: [x, y, z]. */
    private static Ref[] position(GraphBuilder g, String prefix) {
        node(g, prefix + "Pos", "core.position");
        node(g, prefix + "PosXyz", "core.separate_xyz", "vector", out(prefix + "Pos", "position"));
        return new Ref[] {out(prefix + "PosXyz", "x"), out(prefix + "PosXyz", "y"), out(prefix + "PosXyz", "z")};
    }

    /** Componentes da normal do elemento: [x, y, z]. */
    private static Ref[] normal(GraphBuilder g, String prefix) {
        node(g, prefix + "Nrm", "core.normal");
        node(g, prefix + "NrmXyz", "core.separate_xyz", "vector", out(prefix + "Nrm", "normal"));
        return new Ref[] {out(prefix + "NrmXyz", "x"), out(prefix + "NrmXyz", "y"), out(prefix + "NrmXyz", "z")};
    }

    /**
     * Raios de bevel (e facetas) por peca, para "quebrar" as bordas duras de pontos estrategicos. Para desligar todos os
     * bevels (volta as caixas de 8 vertices), ponha {@link #BEVEL_SCALE} = 0.
     */
    static final double BEVEL_SCALE = 1.0;

    private static final java.util.Map<String, double[]> BEVELS = java.util.Map.ofEntries(
            java.util.Map.entry("pier", new double[] {0.16, 1}),
            java.util.Map.entry("plinth", new double[] {0.07, 1}),
            java.util.Map.entry("cornice", new double[] {0.08, 1}),
            java.util.Map.entry("hedge", new double[] {0.28, 1}),
            java.util.Map.entry("rock", new double[] {0.14, 1}),
            java.util.Map.entry("gardenBed", new double[] {0.07, 1}),
            java.util.Map.entry("gardenFlowers", new double[] {0.12, 1}),
            java.util.Map.entry("carBody", new double[] {0.22, 1}),
            java.util.Map.entry("housing", new double[] {0.06, 1}),
            java.util.Map.entry("slabPart", new double[] {0.18, 1}),
            java.util.Map.entry("unit", new double[] {0.25, 1}),
            java.util.Map.entry("ac", new double[] {0.14, 1}),
            java.util.Map.entry("chimney", new double[] {0.1, 1}),
            java.util.Map.entry("porch", new double[] {0.1, 1}),
            java.util.Map.entry("body", new double[] {0.3, 1}),
            java.util.Map.entry("cabin", new double[] {0.3, 1}));

    /**
     * Marca todas as faces como suaves (atributo FACE booleano "smooth"): o adaptador da demo so mistura normais entre faces
     * marcadas (e dentro do angulo de crease), entao superficies curvas ficam lisas e o resto continua chapado.
     */
    private static Ref smooth(GraphBuilder g, String id, Ref geometry) {
        node(g, id, "core.set_attribute", "geometry", geometry, "name", "smooth", "domain", "face", "type", "bool", "flag", true);
        return out(id, "geometry");
    }

    /**
     * Caixa com material, centrada em {@code at} (Vec3 ou Ref): chamada do subgrafo {@code box_part}; o bevel vem de
     * {@link #BEVELS} pelo id da peca (e deixa a caixa suave).
     */
    private static Ref part(GraphBuilder g, String id, Object size, Object at, String material) {
        double bevel = BEVELS.getOrDefault(id, new double[] {0, 1})[0] * BEVEL_SCALE;
        sub(g, id, "box_part", "size", size, "position", at, "material", material, "bevel", bevel);
        return out(id, "geometry");
    }

    private static GraphBuilder asset(String id) {
        return GraphBuilder.create(id, 1).name(id);
    }

    private static NodeInputDefinition num(String name, double def) {
        return NodeInputDefinition.of(name, PortType.FLOAT).withDefault(def);
    }

    private static NodeInputDefinition integer(String name, int def) {
        return NodeInputDefinition.of(name, PortType.INT).withDefault(def);
    }

    private static NodeInputDefinition material(String name, String def) {
        return NodeInputDefinition.of(name, PortType.MATERIAL).withDefault(def);
    }

    private static void output(GraphBuilder g, String name, PortType type, Ref from) {
        Ref r = real(g, from);
        g.output(name, type, r.node(), r.port());
    }

    /** Junta as saidas de {@code parts} num no novo {@code id} (entrada multi do Join). */
    private static Ref join(GraphBuilder g, String id, Ref... parts) {
        node(g, id, "core.join_geometry");
        for (Ref p0 : parts) {
            Ref p = real(g, p0);
            g.connect(p.node(), p.port(), id, "geometry");
        }
        return out(id, "geometry");
    }

    private static Ref translate(GraphBuilder g, String id, Ref geometry, Object offset) {
        node(g, id, "core.translate", "geometry", geometry, "offset", offset);
        return out(id, "geometry");
    }

    private static Ref rotate(GraphBuilder g, String id, Ref geometry, Object rotation) {
        node(g, id, "core.rotate", "geometry", geometry, "rotation", rotation);
        return out(id, "geometry");
    }

    /** Gira ({@code rotation}, graus) e depois move ({@code translation}) num so no ({@code core.transform_geometry}). */
    private static Ref place(GraphBuilder g, String id, Ref geometry, Object translation, Object rotation) {
        node(g, id, "core.transform_geometry", "geometry", geometry, "translation", translation, "rotation", rotation);
        return out(id, "geometry");
    }

    /** Espelha em torno do plano perpendicular a {@code axis} ("x", "y" ou "z") que passa pela origem local; o original fica. */
    private static Ref mirror(GraphBuilder g, String id, Ref geometry, String axis) {
        node(g, id, "core.mirror", "geometry", geometry, "axis", axis);
        return out(id, "geometry");
    }

    // ---- pecas compostas reutilizadas por varios assets ----

    /** Pontos em linha reta de {@code start} a {@code end} ({@code count} pontos, count &gt;= 2): chamada do subgrafo {@code line_points}. */
    private static Ref linePoints(GraphBuilder g, String id, Object start, Object end, Object count) {
        sub(g, id, "line_points", "start", start, "end", end, "count", count);
        return out(id, "points");
    }

    /** Cilindro de eixo Y com a base em {@code at}: chamada do subgrafo {@code cylinder_part} (radius/height aceitam Ref). */
    private static Ref cylinder(GraphBuilder g, String id, Object radius, Object height, int segments, String material, Object at) {
        sub(g, id, "cylinder_part", "radius", radius, "height", height, "segments", segments, "position", at, "material", material);
        return out(id, "geometry");
    }

    private static Ref cylinder(GraphBuilder g, String id, Object radius, Object height, int segments, String material) {
        return cylinder(g, id, radius, height, segments, material, new Vec3(0, 0, 0));
    }

    /** Laje com parapeito: a face de cima ganha inset e o anel sobe. */
    private static Ref parapet(GraphBuilder g, String id, Ref slab, Object inset, Object rise) {
        Ref[] n = normal(g, id + "Slab");
        Ref top = cmp(g, id + "Top", "gt", n[1], 0.95);
        node(g, id + "Frame", "core.inset_faces", "geometry", slab, "selection", top, "amount", inset, "inner_attribute", id + "_inner");
        Ref ring = and(g, id + "Ring", top, not(g, id + "NotInner", flag(g, id + "Inner", id + "_inner")));
        node(g, id, "core.extrude", "geometry", out(id + "Frame", "geometry"), "selection", ring, "distance", rise);
        return out(id, "geometry");
    }

    /** Aparelhos de teto (ar-condicionado com ventoinhas e exaustores) espalhados sobre {@code area}, uma malha no nivel do teto. */
    private static Ref roofGear(GraphBuilder g, String id, Ref area, Object density) {
        node(g, id + "Scatter", "core.distribute_points", "geometry", area, "density", density);
        Ref box = part(g, id + "Ac", new Vec3(1.6, 0.9, 1.1), new Vec3(0, 0.45, 0), "metal");
        Ref fanA = translate(g, id + "FanA", cylinder(g, id + "FanDiscA", 0.36, 0.06, 10, "tire"), new Vec3(-0.4, 0.9, 0));
        Ref fanB = translate(g, id + "FanB", cylinder(g, id + "FanDiscB", 0.36, 0.06, 10, "tire"), new Vec3(0.4, 0.9, 0));
        Ref ac = join(g, id + "AcUnit", box, fanA, fanB);
        Ref shaft = cylinder(g, id + "Shaft", 0.2, 1.1, 8, "metal");
        Ref hood = translate(g, id + "HoodAt", cylinder(g, id + "Hood", 0.34, 0.1, 8, "metal"), new Vec3(0, 1.1, 0));
        Ref vent = join(g, id + "VentUnit", shaft, hood);
        node(g, id + "AcI", "core.geometry_to_instance", "geometry", ac);
        node(g, id + "AcI2", "core.geometry_to_instance", "geometry", ac);
        node(g, id + "VentI", "core.geometry_to_instance", "geometry", vent);
        Ref kinds = join(g, id + "Kinds", out(id + "AcI", "instances"), out(id + "VentI", "instances"), out(id + "AcI2", "instances"));
        node(g, id + "Pick", "core.random_value", "min_int", 0, "max_int", 2);
        node(g, id + "Spin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, id + "Size", "core.random_value", "min_vector", new Vec3(0.8, 0.8, 0.8), "max_vector", new Vec3(1.25, 1.25, 1.25));
        node(g, id, "core.instance_on_points", "points", out(id + "Scatter", "points"), "instance", kinds, "pick_instance", true,
                "instance_index", out(id + "Pick", "int"), "rotation", out(id + "Spin", "vector"), "scale", out(id + "Size", "vector"));
        return out(id, "instances");
    }

    /** Mastro de antena com travessas e luz de aeronave no topo (base em y = 0). */
    private static Ref mast(GraphBuilder g, String id) {
        Ref pole = cylinder(g, id + "Pole", 0.1, 7.5, 8, "metal");
        Ref base = part(g, id + "Base", new Vec3(0.5, 0.5, 0.5), new Vec3(0, 0.25, 0), "metal");
        Ref armLow = part(g, id + "ArmLow", new Vec3(1.4, 0.07, 0.07), new Vec3(0, 4.8, 0), "metal");
        Ref armMid = part(g, id + "ArmMid", new Vec3(0.9, 0.07, 0.07), new Vec3(0, 5.9, 0), "metal");
        Ref armCross = part(g, id + "ArmCross", new Vec3(0.07, 0.07, 1.0), new Vec3(0, 5.35, 0), "metal");
        Ref beacon = part(g, id + "Beacon", new Vec3(0.22, 0.22, 0.22), new Vec3(0, 7.6, 0), "light_red");
        return join(g, id, pole, base, armLow, armMid, armCross, beacon);
    }

    /** Caixa d'agua sobre quatro pernas (base em y = 0). */
    private static Ref waterTank(GraphBuilder g, String id) {
        Ref legA = part(g, id + "LegA", new Vec3(0.1, 0.9, 0.1), new Vec3(-0.55, 0.45, -0.55), "metal");
        Ref legB = part(g, id + "LegB", new Vec3(0.1, 0.9, 0.1), new Vec3(0.55, 0.45, -0.55), "metal");
        Ref legC = part(g, id + "LegC", new Vec3(0.1, 0.9, 0.1), new Vec3(-0.55, 0.45, 0.55), "metal");
        Ref legD = part(g, id + "LegD", new Vec3(0.1, 0.9, 0.1), new Vec3(0.55, 0.45, 0.55), "metal");
        Ref barrel = translate(g, id + "BarrelAt", cylinder(g, id + "Barrel", 0.8, 1.6, 12, "tank"), new Vec3(0, 0.9, 0));
        Ref lid = translate(g, id + "LidTop", cylinder(g, id + "Cap", 0.88, 0.12, 12, "metal"), new Vec3(0, 2.5, 0));
        Ref hoopLow = translate(g, id + "HoopLowAt", cylinder(g, id + "HoopLow", 0.83, 0.08, 12, "metal"), new Vec3(0, 1.3, 0));
        Ref hoopHigh = translate(g, id + "HoopHighAt", cylinder(g, id + "HoopHigh", 0.83, 0.08, 12, "metal"), new Vec3(0, 2.0, 0));
        return join(g, id, legA, legB, legC, legD, barrel, lid, hoopLow, hoopHigh);
    }

    // ---- utilitarios genericos (assets) ----

    /** Pontos igualmente espacados de {@code start} a {@code end}. */
    private static GraphAsset linePointsAsset() {
        GraphBuilder g = asset("line_points");
        g.input(vec3In("start", new Vec3(0, 0, 0))).input(vec3In("end", new Vec3(0, 0, 1))).input(integer("count", 2));
        node(g, "line", "core.create_line", "start", in("start"), "end", in("end"), "count", in("count"));
        node(g, "points", "core.curve_to_points", "geometry", out("line", "curve"));
        g.output("points", PortType.GEOMETRY, "points", "points");
        return g.build();
    }

    /** Pontos nos vertices de uma grade XZ centrada na origem. */
    private static GraphAsset gridPointsAsset() {
        GraphBuilder g = asset("grid_points");
        g.input(num("size_x", 1.0)).input(num("size_z", 1.0)).input(integer("vertices_x", 2)).input(integer("vertices_z", 2));
        node(g, "grid", "core.create_grid", "size_x", in("size_x"), "size_z", in("size_z"), "vertices_x", in("vertices_x"),
                "vertices_z", in("vertices_z"));
        node(g, "points", "core.mesh_to_points", "geometry", out("grid", "mesh"));
        g.output("points", PortType.GEOMETRY, "points", "points");
        return g.build();
    }

    /** Quantidade de pontos para cobrir {@code length} em passos de {@code step}: round(length / step), depois {@code operation} com {@code extra} (add 1 = vertices; max 2 = minimo). */
    private static GraphAsset stepCountAsset() {
        GraphBuilder g = asset("step_count");
        g.input(num("length", 10)).input(num("step", 1)).input(NodeInputDefinition.of("operation", PortType.STRING).withDefault("add"))
                .input(integer("extra", 1));
        Ref steps = math(g, "steps", "divide", in("length"), in("step"));
        node(g, "rounded", "core.to_int", "value", steps, "mode", "round");
        node(g, "result", "core.int_math", "operation", in("operation"), "a", out("rounded", "result"), "b", in("extra"));
        g.output("result", PortType.INT, "result", "result");
        return g.build();
    }

    // ---- pecas de telhado reutilizaveis (assets) ----

    /** Laje com parapeito: a face de cima ganha inset e o anel sobe. */
    private static GraphAsset parapetAsset() {
        GraphBuilder g = asset("parapet");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("inset", 0.5)).input(num("rise", 0.8));
        return finish(g, parapet(g, "rim", in("geometry"), in("inset"), in("rise")));
    }

    /** Aparelhos de teto (ar-condicionado e exaustores) espalhados sobre {@code area}. */
    private static GraphAsset roofGearAsset() {
        GraphBuilder g = asset("roof_gear");
        g.input(NodeInputDefinition.of("area", PortType.GEOMETRY)).input(num("density", 0.07));
        return finish(g, roofGear(g, "gear", in("area"), in("density")));
    }

    /** Mastro de antena com travessas e luz de aeronave (base em y = 0). */
    private static GraphAsset roofMastAsset() {
        GraphBuilder g = asset("roof_mast");
        return finish(g, mast(g, "mast"));
    }

    /** Caixa d'agua sobre quatro pernas (base em y = 0). */
    private static GraphAsset waterTankAsset() {
        GraphBuilder g = asset("water_tank");
        return finish(g, waterTank(g, "tank"));
    }

    // ================= predios =================

    /**
     * Janelas: ligacao de quatro etapas. {@code window_panes} abre as janelas dos andares de cima, {@code shopfronts} as
     * vitrines do terreo, {@code pane_spots} acha o centro de cada vidro e nele se penduram {@code window_fittings}
     * (peitoris, venezianas, sacadas) e as marquises das vitrines.
     */
    private static GraphAsset windows() {
        GraphBuilder g = asset("windows");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("chance", 0.5)).input(num("ground", 3.5))
                .input(num("depth", 0.3)).input(num("margin", 0.35)).input(num("row", 3.2)).input(num("col", 3.0))
                .input(num("balconies", 0.0))
                .input(NodeInputDefinition.of("awnings", PortType.BOOL).withDefault(true))
                .input(NodeInputDefinition.of("sills", PortType.BOOL).withDefault(true))
                .input(NodeInputDefinition.of("cottage", PortType.BOOL).withDefault(false));
        sub(g, "panes", "window_panes", "geometry", in("geometry"), "chance", in("chance"), "ground", in("ground"),
                "depth", in("depth"), "margin", in("margin"));
        sub(g, "windowSpots", "pane_spots", "geometry", out("panes", "frames"), "attribute", "pane");
        sub(g, "fittings", "window_fittings", "points", out("windowSpots", "points"), "col", in("col"), "margin", in("margin"),
                "row", in("row"), "ground", in("ground"), "balconies", in("balconies"), "sills", in("sills"), "cottage", in("cottage"));
        sub(g, "shops", "shopfronts", "geometry", out("panes", "geometry"), "ground", in("ground"));
        sub(g, "shopSpots", "pane_spots", "geometry", out("shops", "frames"), "attribute", "shop_pane");
        sub(g, "awningPart", "awning");
        node(g, "awningSet", "core.instance_on_points", "points", out("shopSpots", "points"), "instance", out("awningPart", "geometry"),
                "selection", in("awnings"));
        return finish(g, join(g, "withAwnings", out("shops", "geometry"), out("awningSet", "instances"), out("fittings", "geometry")));
    }

    /** Janelas dos andares de cima: moldura, requadro, caixilho e vidro recuado. {@code frames} = a parede antes do recuo (para achar os vidros). */
    private static GraphAsset windowPanesAsset() {
        GraphBuilder g = asset("window_panes");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("chance", 0.5)).input(num("ground", 3.5))
                .input(num("depth", 0.3)).input(num("margin", 0.35));
        Ref[] n = normal(g, "w");
        Ref isWall = cmp(g, "isWall", "lt", fmath(g, "nAbs", "abs", n[1], 0.0), 0.3);
        Ref[] p = position(g, "w");
        Ref above = cmp(g, "above", "gt", p[1], in("ground"));
        node(g, "chanceRnd", "core.random_value", "probability", in("chance"));
        Ref window = and(g, "window", and(g, "wallAbove", isWall, above), out("chanceRnd", "bool"));
        node(g, "framed", "core.inset_faces", "geometry", in("geometry"), "selection", window,
                "amount", in("margin"), "inner_attribute", "inner");
        Ref inner = flag(g, "innerFaces", "inner");
        node(g, "cased", "core.inset_faces", "geometry", out("framed", "geometry"), "selection", inner,
                "amount", 0.1, "inner_attribute", "inner2");
        Ref inner2 = flag(g, "innerFaces2", "inner2");
        node(g, "sashed", "core.inset_faces", "geometry", out("cased", "geometry"), "selection", inner2,
                "amount", 0.07, "inner_attribute", "pane");
        Ref pane = flag(g, "paneFaces", "pane");
        Ref casing = and(g, "casing", inner, not(g, "notInner2", inner2));
        Ref sash = and(g, "sash", inner2, not(g, "notPane", pane));
        node(g, "litRnd", "core.random_value", "probability", 0.4);
        Ref lit = and(g, "lit", pane, out("litRnd", "bool"));
        Ref dark = and(g, "dark", pane, not(g, "notLit", out("litRnd", "bool")));
        node(g, "matCasing", "core.set_material", "geometry", out("sashed", "geometry"), "selection", casing, "material", "trim");
        node(g, "matSash", "core.set_material", "geometry", out("matCasing", "geometry"), "selection", sash, "material", "metal");
        node(g, "matLit", "core.set_material", "geometry", out("matSash", "geometry"), "selection", lit, "material", "window_lit");
        node(g, "matDark", "core.set_material", "geometry", out("matLit", "geometry"), "selection", dark, "material", "window_dark");
        node(g, "casingOut", "core.extrude", "geometry", out("matDark", "geometry"), "selection", casing, "distance", 0.07);
        node(g, "recess", "core.extrude", "geometry", out("casingOut", "geometry"), "selection", pane,
                "distance", math(g, "inward", "multiply", in("depth"), -1.0));
        output(g, "geometry", PortType.GEOMETRY, out("recess", "geometry"));
        output(g, "frames", PortType.GEOMETRY, out("sashed", "geometry"));
        return g.build();
    }

    /** Vitrines do terreo (abaixo de {@code ground}): moldura, caixilho e vidro recuado. {@code frames} = antes do recuo. */
    private static GraphAsset shopfrontsAsset() {
        GraphBuilder g = asset("shopfronts");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("ground", 3.5));
        Ref[] n = normal(g, "w");
        Ref isWall = cmp(g, "isWall", "lt", fmath(g, "nAbs", "abs", n[1], 0.0), 0.3);
        Ref[] p = position(g, "w");
        Ref below = cmp(g, "below", "lt", p[1], in("ground"));
        node(g, "shopRnd", "core.random_value", "probability", 0.8);
        Ref shop = and(g, "shop", and(g, "wallBelow", isWall, below), out("shopRnd", "bool"));
        node(g, "shopFrame", "core.inset_faces", "geometry", in("geometry"), "selection", shop,
                "amount", 0.3, "inner_attribute", "shop_inner");
        Ref shopInner = flag(g, "shopInner", "shop_inner");
        node(g, "shopCased", "core.inset_faces", "geometry", out("shopFrame", "geometry"), "selection", shopInner,
                "amount", 0.09, "inner_attribute", "shop_pane");
        Ref shopPane = flag(g, "shopPane", "shop_pane");
        Ref shopSash = and(g, "shopSash", shopInner, not(g, "notShopPane", shopPane));
        node(g, "shopSashMat", "core.set_material", "geometry", out("shopCased", "geometry"), "selection", shopSash, "material", "metal");
        node(g, "shopMat", "core.set_material", "geometry", out("shopSashMat", "geometry"), "selection", shopPane, "material", "shopfront");
        node(g, "shopRecess", "core.extrude", "geometry", out("shopMat", "geometry"), "selection", shopPane, "distance", -0.25);
        output(g, "geometry", PortType.GEOMETRY, out("shopRecess", "geometry"));
        output(g, "frames", PortType.GEOMETRY, out("shopCased", "geometry"));
        return g.build();
    }

    /** Um ponto no centro de cada face com o atributo booleano {@code attribute}, sem os outros atributos e com um tom de cor sorteado. */
    private static GraphAsset paneSpotsAsset() {
        GraphBuilder g = asset("pane_spots");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(NodeInputDefinition.of("attribute", PortType.STRING).withDefault("pane"));
        node(g, "faces", "core.mesh_to_points", "geometry", in("geometry"), "mode", "faces");
        node(g, "kept", "core.delete_geometry", "geometry", out("faces", "points"),
                "selection", not(g, "notPane", flag(g, "isPane", in("attribute"))));
        node(g, "bare", "core.clear_attributes", "geometry", out("kept", "geometry")); // so a posicao interessa
        node(g, "tone", "core.random_value");
        node(g, "toned", "core.set_attribute", "geometry", out("bare", "geometry"), "name", "tone", "domain", "point",
                "type", "float", "value", out("tone", "float"));
        g.output("points", PortType.GEOMETRY, "toned", "geometry");
        return g.build();
    }

    /** Peitoris, venezianas (casas) e sacadas, uma instancia por ponto de {@code points} (centros dos vidros). */
    private static GraphAsset windowFittingsAsset() {
        GraphBuilder g = asset("window_fittings");
        g.input(NodeInputDefinition.of("points", PortType.GEOMETRY)).input(num("col", 3.0)).input(num("margin", 0.35)).input(num("row", 3.2))
                .input(num("ground", 3.5)).input(num("balconies", 0.0))
                .input(NodeInputDefinition.of("sills", PortType.BOOL).withDefault(true))
                .input(NodeInputDefinition.of("cottage", PortType.BOOL).withDefault(false));
        sub(g, "sillPart", "sill", "col", in("col"), "margin", in("margin"), "row", in("row"));
        node(g, "sillSet", "core.instance_on_points", "points", in("points"), "instance", out("sillPart", "geometry"), "selection", in("sills"));
        sub(g, "shutterPart", "shutters", "col", in("col"), "margin", in("margin"), "row", in("row"));
        node(g, "shutterSet", "core.instance_on_points", "points", in("points"), "instance", out("shutterPart", "geometry"),
                "selection", in("cottage"));
        sub(g, "balconyPart", "balcony", "col", in("col"), "row", in("row"));
        node(g, "balconyRnd", "core.random_value", "probability", in("balconies"));
        Ref[] bp = position(g, "bal");
        Ref upperFloor = cmp(g, "balUpper", "gt", bp[1], math(g, "balMin", "add", in("ground"), math(g, "balRow", "multiply", in("row"), 0.4)));
        node(g, "balconySet", "core.instance_on_points", "points", in("points"), "instance", out("balconyPart", "geometry"),
                "selection", and(g, "balconyWanted", out("balconyRnd", "bool"), upperFloor));
        return finish(g, join(g, "all", out("sillSet", "instances"), out("shutterSet", "instances"), out("balconySet", "instances")));
    }

    /** Marquise: tela inclinada para baixo, com friso frontal, na frente (+Z) e no topo da vitrine. */
    private static GraphAsset awning() {
        GraphBuilder g = asset("awning");
        Ref canopy = part(g, "canopy", new Vec3(2.7, 0.08, 1.2), new Vec3(0, 0, 0.6), "awning");
        Ref valance = part(g, "valance", new Vec3(2.7, 0.22, 0.05), new Vec3(0, -0.12, 1.2), "awning");
        Ref arms = mirror(g, "arms", part(g, "armR", new Vec3(0.05, 0.05, 1.2), new Vec3(1.3, -0.1, 0.6), "metal"), "x");
        Ref tilted = rotate(g, "tilt", join(g, "awningBody", canopy, valance, arms), new Vec3(18, 0, 0));
        output(g, "geometry", PortType.GEOMETRY, translate(g, "hang", tilted, new Vec3(0, 1.1, 0.04)));
        return g.build();
    }

    /** Peitoril de janela (origem = centro do vidro, no plano da parede, olhando para +Z). */
    private static GraphAsset sill() {
        GraphBuilder g = asset("sill");
        g.input(num("col", 3.0)).input(num("margin", 0.35)).input(num("row", 3.2));
        Ref width = math(g, "sillW", "add", math(g, "sillSpan", "subtract", in("col"), math(g, "sillM2", "multiply", in("margin"), 2.0)), 0.4);
        Ref bottom = math(g, "sillBottom", "multiply", math(g, "sillHalf", "subtract", math(g, "rowHalf", "multiply", in("row"), 0.5), in("margin")), -1.0);
        Ref slab = part(g, "sillSlab", vec(g, "sillSize", width, 0.12, 0.38), vec(g, "sillAt", 0.0, math(g, "sillY", "subtract", bottom, 0.04), 0.17), "trim");
        Ref brackets = mirror(g, "sillBrackets", part(g, "sillBracketR", new Vec3(0.1, 0.18, 0.2),
                vec(g, "sillBrRAt", math(g, "sillBrXR", "multiply", math(g, "sillBrHalfR", "multiply", width, 0.5), 0.7), math(g, "sillBrYR", "subtract", bottom, 0.15), 0.1), "trim"), "x");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", slab, brackets));
        return g.build();
    }

    /** Sacada: laje, guarda-corpo de vidro com corrimao e montantes (origem = centro do vidro da janela). */
    private static GraphAsset balcony() {
        GraphBuilder g = asset("balcony");
        g.input(num("col", 3.0)).input(num("row", 3.2));
        Ref width = math(g, "balW", "subtract", in("col"), 0.3);
        Ref half = math(g, "balHalf", "multiply", width, 0.5);
        Ref floorY = math(g, "balFloor", "multiply", in("row"), -0.5);
        Ref slab = part(g, "balSlab", vec(g, "balSlabSize", width, 0.14, 1.1), vec(g, "balSlabAt", 0.0, math(g, "balSlabY", "add", floorY, 0.05), 0.55), "trim");
        Ref rail = part(g, "balRail", vec(g, "balRailSize", width, 0.06, 0.08), vec(g, "balRailAt", 0.0, math(g, "balRailY", "add", floorY, 1.13), 1.06), "railing");
        Ref sideR = part(g, "balSideR", new Vec3(0.06, 0.06, 1.0), vec(g, "balSideRAt", half, math(g, "balSideRY", "add", floorY, 1.13), 0.55), "railing");
        Ref glass = part(g, "balGlass", vec(g, "balGlassSize", math(g, "balGlassW", "subtract", width, 0.12), 0.85, 0.03),
                vec(g, "balGlassAt", 0.0, math(g, "balGlassY", "add", floorY, 0.67), 1.06), "window_dark");
        Ref postR = part(g, "balPostR", new Vec3(0.08, 1.1, 0.08), vec(g, "balPostRAt", half, math(g, "balPostRY", "add", floorY, 0.67), 1.06), "railing");
        Ref sides = mirror(g, "balSides", join(g, "balRightSide", sideR, postR), "x");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", slab, rail, glass, sides));
        return g.build();
    }

    /** Par de venezianas com ripas e uma floreira sob a janela (casas). */
    private static GraphAsset shutters() {
        GraphBuilder g = asset("shutters");
        g.input(num("col", 3.0)).input(num("margin", 0.35)).input(num("row", 3.2));
        Ref height = math(g, "shHeight", "subtract", in("row"), math(g, "shM2", "multiply", in("margin"), 2.0));
        Ref offset = math(g, "shOffset", "add", math(g, "shEdge", "subtract", math(g, "shHalfCol", "multiply", in("col"), 0.5), in("margin")), 0.27);
        Ref right = part(g, "shutterR", vec(g, "shRSize", 0.42, height, 0.07), vec(g, "shRAt", offset, 0.0, 0.035), "shutter");
        // ripas: uma linha de pontos em cada veneziana
        Ref slatsPts = linePoints(g, "slatPts", vec(g, "slatA", 0.0, math(g, "slatLow", "multiply", height, -0.42), 0.0),
                vec(g, "slatB", 0.0, math(g, "slatHigh", "multiply", height, 0.42), 0.0), 7);
        Ref slat = part(g, "slat", new Vec3(0.36, 0.04, 0.03), new Vec3(0, 0, 0.085), "trim");
        node(g, "slatsL", "core.instance_on_points", "points", slatsPts, "instance", slat);
        Ref slatsR = translate(g, "slatsRAt", out("slatsL", "instances"), vec(g, "slatsROffset", offset, 0.0, 0.0));
        Ref pair = mirror(g, "shutterPair", join(g, "rightShutter", right, slatsR), "x");
        Ref bottom = math(g, "shBottom", "multiply", math(g, "shHalfRow", "subtract", math(g, "shRowHalf", "multiply", in("row"), 0.5), in("margin")), -1.0);
        Ref boxWidth = math(g, "flowerW", "subtract", math(g, "flowerSpan", "subtract", in("col"), math(g, "flowerM2", "multiply", in("margin"), 2.0)), -0.1);
        Ref box = part(g, "flowerBox", vec(g, "flowerSize", boxWidth, 0.22, 0.3), vec(g, "flowerAt", 0.0, math(g, "flowerY", "subtract", bottom, 0.36), 0.2), "trunk");
        Ref bloom = part(g, "flowerBloom", vec(g, "bloomSize", math(g, "bloomW", "subtract", boxWidth, 0.08), 0.16, 0.24),
                vec(g, "bloomAt", 0.0, math(g, "bloomY", "subtract", bottom, 0.2), 0.2), "flowers");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", pair, box, bloom));
        return g.build();
    }

    /** Grade vertical (cols x rows) com janelas e pilastras entre as colunas. */
    private static GraphAsset facade() {
        GraphBuilder g = asset("facade");
        g.input(num("width", 9)).input(num("height", 10)).input(integer("cols", 3)).input(integer("rows", 3)).input(num("chance", 0.5))
                .input(num("ground_zone", 1.0)).input(num("margin", 0.35)).input(num("balconies", 0.0))
                .input(NodeInputDefinition.of("cottage", PortType.BOOL).withDefault(false))
                .input(NodeInputDefinition.of("pilasters", PortType.BOOL).withDefault(true));

        node(g, "vx", "core.int_math", "operation", "add", "a", in("cols"), "b", 1);
        node(g, "vz", "core.int_math", "operation", "add", "a", in("rows"), "b", 1);
        node(g, "grid", "core.create_grid", "size_x", in("width"), "size_z", in("height"),
                "vertices_x", out("vx", "result"), "vertices_z", out("vz", "result"));
        // a grade nasce deitada (normal +Y); em pe ela olha para +Z e vai de y = -h/2 a +h/2
        Ref stood = rotate(g, "stand", out("grid", "mesh"), new Vec3(90, 0, 0));
        Ref lift = vec(g, "lift", 0.0, math(g, "half", "multiply", in("height"), 0.5), 0.0);
        Ref raised = translate(g, "up", stood, lift);
        Ref rowHeight = math(g, "rowHeight", "divide", in("height"), in("rows"));
        sub(g, "win", "windows", "geometry", raised, "chance", in("chance"),
                "ground", math(g, "ground", "multiply", rowHeight, in("ground_zone")), "row", rowHeight,
                "col", math(g, "colWidth", "divide", in("width"), in("cols")), "margin", in("margin"),
                "balconies", in("balconies"), "cottage", in("cottage"));

        // pilastras nas divisas das colunas (os cantos ja tem pilares)
        Ref halfW = math(g, "fHalfW", "multiply", in("width"), 0.5);
        Ref pilasterPts = linePoints(g, "pilPts", vec(g, "pilA", math(g, "pilNeg", "multiply", halfW, -1.0), 0.0, 0.0),
                vec(g, "pilB", halfW, 0.0, 0.0), out("vx", "result"));
        Ref[] pp = position(g, "pil");
        Ref inside = cmp(g, "pilInside", "lt", fmath(g, "pilAbs", "abs", pp[0], 0.0), math(g, "pilLimit", "subtract", halfW, 0.1));
        Ref pilaster = part(g, "pilaster", vec(g, "pilSize", 0.34, in("height"), 0.16),
                vec(g, "pilAt", 0.0, math(g, "pilY", "multiply", in("height"), 0.5), 0.08), "trim");
        node(g, "pilasterSet", "core.instance_on_points", "points", pilasterPts, "instance", pilaster,
                "selection", and(g, "pilWanted", inside, in("pilasters")));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("win", "geometry"), out("pilasterSet", "instances")));
        return g.build();
    }

    /**
     * Quatro fachadas + pilares de canto (4 instancias de uma caixa nos vertices de uma grade 2x2) + faixas de piso em todos os
     * andares (instancias ao longo de uma linha vertical) + soco de pedra na base.
     */
    private static GraphAsset towerBody() {
        GraphBuilder g = asset("tower_body");
        g.input(integer("cols_x", 4)).input(integer("cols_z", 4)).input(integer("floors", 8)).input(num("cell", 3.0))
                .input(num("floor_h", 3.2)).input(num("chance", 0.5)).input(num("ground_zone", 1.0)).input(num("margin", 0.35))
                .input(num("balconies", 0.1)).input(NodeInputDefinition.of("cottage", PortType.BOOL).withDefault(false))
                .input(NodeInputDefinition.of("pilasters", PortType.BOOL).withDefault(true)).input(num("plinth", 0.8));

        Ref w = math(g, "w", "multiply", in("cols_x"), in("cell"));
        Ref d = math(g, "d", "multiply", in("cols_z"), in("cell"));
        Ref h = math(g, "h", "multiply", in("floors"), in("floor_h"));
        Ref hw = math(g, "hw", "multiply", w, 0.5);
        Ref hd = math(g, "hd", "multiply", d, 0.5);
        Ref offZ = vec(g, "offZ", 0.0, 0.0, hd);
        Ref offX = vec(g, "offX", 0.0, 0.0, hw);

        String[] names = {"front", "back", "right", "left"};
        double[] yaw = {0, 180, 90, -90};
        Ref[] parts = new Ref[7];
        for (int i = 0; i < 4; i++) {
            boolean alongX = i < 2; // frente/tras tem a largura em X; as laterais, a profundidade
            sub(g, names[i] + "Facade", "facade", "width", alongX ? w : d, "height", h,
                    "cols", alongX ? in("cols_x") : in("cols_z"), "rows", in("floors"), "chance", in("chance"),
                    "ground_zone", in("ground_zone"), "margin", in("margin"), "balconies", in("balconies"),
                    "cottage", in("cottage"), "pilasters", in("pilasters"));
            Ref moved = translate(g, names[i] + "Move", out(names[i] + "Facade", "geometry"), alongX ? offZ : offX);
            parts[i] = rotate(g, names[i], moved, new Vec3(0, yaw[i], 0));
        }

        // pilares de canto: uma instancia por vertice de uma grade 2x2 do tamanho da planta
        sub(g, "corners", "grid_points", "size_x", w, "size_z", d, "vertices_x", 2, "vertices_z", 2);
        Ref pier = part(g, "pier", vec(g, "pierSize", 0.8, h, 0.8), vec(g, "pierAt", 0.0, math(g, "pierY", "multiply", h, 0.5), 0.0), "trim");
        node(g, "piers", "core.instance_on_points", "points", out("corners", "points"), "instance", pier);
        parts[4] = out("piers", "instances");

        // faixas de piso: pontos a cada andar numa linha vertical, menos o chao e o topo
        node(g, "floorCount", "core.int_math", "operation", "add", "a", in("floors"), "b", 1);
        Ref floorPts = linePoints(g, "floorPts", new Vec3(0, 0, 0), vec(g, "floorTop", 0.0, h, 0.0), out("floorCount", "result"));
        Ref[] fp = position(g, "fl");
        Ref midFloor = and(g, "midFloor", cmp(g, "floorAbove", "gt", fp[1], 0.1),
                cmp(g, "floorBelow", "lt", fp[1], math(g, "floorLimit", "subtract", h, 0.1)));
        Ref band = part(g, "floorBand", vec(g, "bandSize", math(g, "bandW", "add", w, 0.35), 0.22, math(g, "bandD", "add", d, 0.35)),
                new Vec3(0, 0, 0), "trim");
        node(g, "bands", "core.instance_on_points", "points", floorPts, "instance", band, "selection", midFloor);
        parts[5] = out("bands", "instances");

        parts[6] = part(g, "plinth", vec(g, "plinthSize", math(g, "plinthW", "add", w, 0.8), in("plinth"), math(g, "plinthD", "add", d, 0.8)),
                vec(g, "plinthAt", 0.0, math(g, "plinthY", "multiply", in("plinth"), 0.5), 0.0), "stone");
        output(g, "geometry", PortType.GEOMETRY, join(g, "walls", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
        output(g, "width", PortType.FLOAT, w);
        output(g, "depth", PortType.FLOAT, d);
        output(g, "height", PortType.FLOAT, h);
        return g.build();
    }

    /**
     * Predio de teto plano: corpo + cornija + laje com parapeito + casa de maquinas com porta + mastro, caixa d'agua e aparelhos
     * de teto (ar-condicionado e exaustores) sorteados.
     */
    private static GraphAsset tower() {
        GraphBuilder g = asset("tower");
        g.input(integer("cols_x", 4)).input(integer("cols_z", 4)).input(integer("floors", 8)).input(num("cell", 3.0))
                .input(num("floor_h", 3.2)).input(num("chance", 0.5));
        sub(g, "body", "tower_body", "cols_x", in("cols_x"), "cols_z", in("cols_z"), "floors", in("floors"),
                "cell", in("cell"), "floor_h", in("floor_h"), "chance", in("chance"));
        Ref w = out("body", "width"), d = out("body", "depth"), h = out("body", "height");
        Ref roofTop = math(g, "roofTop", "add", h, 0.6);

        Ref cornice = part(g, "cornice", vec(g, "corniceSize", math(g, "corniceW", "add", w, 1.2), 0.35, math(g, "corniceD", "add", d, 1.2)),
                vec(g, "corniceAt", 0.0, math(g, "corniceY", "subtract", h, 0.17), 0.0), "trim");

        // laje: caixa um pouco maior; a face de cima ganha inset e o anel sobe como parapeito
        Ref slab = part(g, "slabPart", vec(g, "slabSize", math(g, "slabW", "add", w, 0.6), 0.6, math(g, "slabD", "add", d, 0.6)),
                vec(g, "slabAt", 0.0, math(g, "slabY", "add", h, 0.3), 0.0), "roof");
        Ref rim = use(g, "rim", "parapet", "geometry", slab, "inset", 0.55, "rise", 0.7);

        // casa de maquinas (com porta), mastro e caixa d'agua
        Ref unitD = math(g, "unitD", "multiply", d, 0.3);
        Ref unit = part(g, "unit", vec(g, "unitSize", math(g, "unitW", "multiply", w, 0.3), 1.6, unitD),
                vec(g, "unitAt", 0.0, math(g, "unitY", "add", h, 1.4), 0.0), "metal");
        Ref unitDoor = part(g, "unitDoor", new Vec3(0.9, 1.2, 0.08),
                vec(g, "unitDoorAt", 0.0, math(g, "unitDoorY", "add", h, 1.2), math(g, "unitDoorZ", "add", math(g, "unitHalf", "multiply", unitD, 0.5), 0.04)), "door");
        Ref antenna = translate(g, "antennaAt", use(g, "mast", "roof_mast"),
                vec(g, "antennaPos", math(g, "antennaX", "multiply", w, 0.28), roofTop, math(g, "antennaZ", "multiply", d, 0.28)));
        Ref tank = translate(g, "tankAt", use(g, "tank", "water_tank"),
                vec(g, "tankPos", math(g, "tankX", "multiply", w, -0.33), roofTop, math(g, "tankZ", "multiply", d, -0.33)));

        // aparelhos de teto espalhados na laje (instancias de varios tipos)
        node(g, "roofArea", "core.create_grid", "size_x", math(g, "areaW", "subtract", w, 3.0), "size_z", math(g, "areaD", "subtract", d, 3.0));
        Ref areaUp = translate(g, "areaUp", out("roofArea", "mesh"), vec(g, "areaAt", 0.0, roofTop, 0.0));
        Ref gear = use(g, "gear", "roof_gear", "area", areaUp, "density", 0.07);

        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("body", "geometry"), cornice, rim, unit, unitDoor, antenna, tank, gear));
        output(g, "height", PortType.FLOAT, roofTop);
        output(g, "width", PortType.FLOAT, w);
        return g.build();
    }

    /** Torre em recuos: base, corpo menor e uma ultima torre menor no topo (sem agulha: telhado plano com equipamentos). */
    private static GraphAsset steppedTower() {
        GraphBuilder g = asset("stepped_tower");
        g.input(integer("cols_x", 4)).input(integer("cols_z", 4)).input(integer("floors", 18)).input(num("cell", 3.0));
        sub(g, "base", "tower", "cols_x", in("cols_x"), "cols_z", in("cols_z"), "floors", in("floors"), "cell", in("cell"));

        node(g, "midCx", "core.int_math", "operation", "subtract", "a", in("cols_x"), "b", 2);
        node(g, "midX", "core.int_math", "operation", "max", "a", out("midCx", "result"), "b", 2);
        node(g, "midCz", "core.int_math", "operation", "subtract", "a", in("cols_z"), "b", 2);
        node(g, "midZ", "core.int_math", "operation", "max", "a", out("midCz", "result"), "b", 2);
        node(g, "midFloors", "core.to_int", "value", math(g, "midF", "multiply", in("floors"), 0.6), "mode", "floor");
        sub(g, "mid", "tower", "cols_x", out("midX", "result"), "cols_z", out("midZ", "result"), "floors", out("midFloors", "result"),
                "cell", in("cell"));
        Ref midUp = translate(g, "midUp", out("mid", "geometry"), vec(g, "midAt", 0.0, out("base", "height"), 0.0));

        Ref crownY = math(g, "crownY", "add", out("base", "height"), out("mid", "height"));
        sub(g, "crown", "tower", "cols_x", 2, "cols_z", 2, "floors", 4, "cell", 2.2);
        Ref crownUp = translate(g, "crownUp", out("crown", "geometry"), vec(g, "crownAt", 0.0, crownY, 0.0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("base", "geometry"), midUp, crownUp));
        return g.build();
    }

    /** Torre cilindrica: parede varrida + janelas + discos nos andares + laje com parapeito, casa de maquinas e aparelhos de teto. */
    private static GraphAsset roundTower() {
        GraphBuilder g = asset("round_tower");
        g.input(num("radius", 8)).input(integer("floors", 18)).input(num("floor_h", 3.4)).input(integer("segments", 28))
                .input(num("chance", 0.5));
        Ref h = math(g, "h", "multiply", in("floors"), in("floor_h"));
        node(g, "ring", "core.create_circle", "radius", in("radius"), "segments", in("segments"));
        node(g, "count", "core.int_math", "operation", "add", "a", in("floors"), "b", 1);
        // perfil de cima para baixo: com o circulo anti-horario isso deixa as normais para fora
        node(g, "profile", "core.create_line", "start", vec(g, "top", 0.0, h, 0.0), "end", new Vec3(0, 0, 0), "count", out("count", "result"));
        node(g, "wall", "core.curve_to_mesh", "geometry", out("ring", "curve"), "profile", out("profile", "curve"));
        sub(g, "win", "windows", "geometry", smooth(g, "wallSmooth", out("wall", "mesh")), "chance", in("chance"),
                "ground", in("floor_h"), "awnings", false, "sills", false, "row", in("floor_h"),
                "col", math(g, "colArc", "divide", math(g, "circ", "multiply", in("radius"), 6.2832), in("segments")), "margin", 0.3);

        // discos de piso: um por andar, nos pontos de uma linha vertical (menos o chao e o topo)
        Ref floorPts = linePoints(g, "floorPts", new Vec3(0, 0, 0), vec(g, "floorTop", 0.0, h, 0.0), out("count", "result"));
        Ref[] fp = position(g, "fl");
        Ref midFloor = and(g, "midFloor", cmp(g, "floorAbove", "gt", fp[1], 0.1),
                cmp(g, "floorBelow", "lt", fp[1], math(g, "floorLimit", "subtract", h, 0.1)));
        Ref disc = cylinder(g, "floorDisc", math(g, "discR", "add", in("radius"), 0.22), 0.22, 28, "trim");
        node(g, "floorDiscs", "core.instance_on_points", "points", floorPts, "instance", disc, "selection", midFloor);
        Ref plinth = cylinder(g, "plinthDisc", math(g, "plinthR", "add", in("radius"), 0.5), 0.7, 28, "stone");

        // laje com cornija e parapeito
        Ref cornice = cylinder(g, "corniceDisc", math(g, "corniceR", "add", in("radius"), 0.75), 0.3, 28, "trim", vec(g, "corniceAtPos", 0.0, math(g, "corniceY", "subtract", h, 0.15), 0.0));
        Ref slab = cylinder(g, "slabDisc", math(g, "slabR", "add", in("radius"), 0.45), 0.6, 28, "roof", vec(g, "slabAtPos", 0.0, h, 0.0));
        Ref rim = use(g, "rim", "parapet", "geometry", slab, "inset", 0.5, "rise", 0.8);
        Ref roofTop = math(g, "roofTop", "add", h, 0.6);

        // casa de maquinas cilindrica, mastro, caixa d'agua e aparelhos
        Ref machine = cylinder(g, "machine", math(g, "machineR", "multiply", in("radius"), 0.32), 2.0, 16, "metal", vec(g, "machinePos", 0.0, roofTop, 0.0));
        Ref machineCap = cylinder(g, "machineCap", math(g, "machineCapR", "multiply", in("radius"), 0.36), 0.15, 16, "trim", vec(g, "machineCapPos", 0.0, math(g, "machineCapY", "add", roofTop, 2.0), 0.0));
        Ref antenna = translate(g, "antennaAt", use(g, "mast", "roof_mast"), vec(g, "antennaPos", 0.0, math(g, "antennaY", "add", roofTop, 2.15), 0.0));
        Ref tank = translate(g, "tankAt", use(g, "tank", "water_tank"),
                vec(g, "tankPos", math(g, "tankX", "multiply", in("radius"), -0.55), roofTop, math(g, "tankZ", "multiply", in("radius"), 0.3)));
        node(g, "areaRing", "core.create_circle", "radius", math(g, "areaR", "subtract", in("radius"), 2.2), "segments", 24);
        node(g, "areaFill", "core.curve_to_mesh", "geometry", out("areaRing", "curve"));
        Ref areaUp = translate(g, "areaUp", out("areaFill", "mesh"), vec(g, "areaAt", 0.0, roofTop, 0.0));
        Ref gear = use(g, "gear", "roof_gear", "area", areaUp, "density", 0.05);

        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("win", "geometry"), out("floorDiscs", "instances"), plinth, cornice, rim,
                machine, machineCap, antenna, tank, gear));
        return g.build();
    }

    // ================= casas =================

    /** Lucarna (origem = centro da base sobre a agua do telhado; janela voltada para +X): corpo, teto plano, moldura e vidro. */
    private static GraphAsset dormer() {
        GraphBuilder g = asset("dormer");
        Ref body = part(g, "dormerBody", new Vec3(1.4, 2.1, 1.7), new Vec3(0, 0.3, 0), "wall");
        Ref roof = part(g, "dormerRoof", new Vec3(1.7, 0.14, 2.0), new Vec3(0.1, 1.42, 0), "roof");
        Ref frame = part(g, "dormerFrame", new Vec3(0.07, 1.15, 1.2), new Vec3(0.72, 0.55, 0), "trim");
        Ref glass = part(g, "dormerGlass", new Vec3(0.09, 0.95, 1.0), new Vec3(0.73, 0.55, 0), "window_dark");
        Ref mullion = part(g, "dormerMullion", new Vec3(0.1, 0.95, 0.06), new Vec3(0.74, 0.55, 0), "trim");
        Ref sill = part(g, "dormerSill", new Vec3(0.2, 0.08, 1.4), new Vec3(0.78, -0.07, 0), "trim");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", body, roof, frame, glass, mullion, sill));
        return g.build();
    }

    /** Quatro casas viradas para o centro do quarteirao. */
    private static GraphAsset houseBlock() {
        GraphBuilder g = asset("house_block");
        g.input(integer("wide", 2)).input(integer("deep", 2));
        int[][] at = {{-7, -7}, {7, -7}, {-7, 7}, {7, 7}};
        double[] yaw = {0, 0, 180, 180};
        String[] names = {"a", "b", "c", "d"};
        Ref[] parts = new Ref[4];
        for (int i = 0; i < 4; i++) {
            boolean widePair = i == 0 || i == 3;
            sub(g, "house" + names[i], "house", "cols_x", widePair ? in("wide") : in("deep"), "cols_z", in("deep"));
            parts[i] = place(g, "place" + names[i], out("house" + names[i], "geometry"), new Vec3(at[i][0], 0, at[i][1]), new Vec3(0, yaw[i], 0));
        }
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", parts));
        return g.build();
    }

    /** Banco de praca: assento e encosto de ripas, pes e bracos de metal. */
    private static GraphAsset bench() {
        GraphBuilder g = asset("bench");
        Ref slatPts = linePoints(g, "slatPts", new Vec3(0, 0.95, -0.2), new Vec3(0, 0.95, 0.2), 4);
        Ref seatSlat = part(g, "seatSlat", new Vec3(2.0, 0.06, 0.1), new Vec3(0, 0, 0), "trunk");
        node(g, "seat", "core.instance_on_points", "points", slatPts, "instance", seatSlat);
        Ref backPts = linePoints(g, "backPts", new Vec3(0, 1.15, -0.3), new Vec3(0, 1.5, -0.3), 3);
        Ref backSlat = part(g, "backSlat", new Vec3(2.0, 0.1, 0.05), new Vec3(0, 0, 0), "trunk");
        node(g, "backs", "core.instance_on_points", "points", backPts, "instance", backSlat);
        Ref legR = part(g, "legR", new Vec3(0.08, 0.95, 0.55), new Vec3(0.85, 0.475, -0.02), "metal");
        Ref armR = part(g, "armR", new Vec3(0.08, 0.06, 0.5), new Vec3(0.9, 1.2, 0.05), "metal");
        Ref postR = part(g, "backPostR", new Vec3(0.06, 0.6, 0.06), new Vec3(0.85, 1.2, -0.3), "metal");
        Ref sides = mirror(g, "sides", join(g, "rightSide", legR, armR, postR), "x");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("seat", "instances"), out("backs", "instances"), sides));
        return g.build();
    }

    /** Fonte: bacia de pedra com agua, pedestal, taca superior e jato. */
    private static GraphAsset fountain() {
        GraphBuilder g = asset("fountain");
        Ref basin = cylinder(g, "basin", 2.4, 0.55, 28, "stone");
        Ref basinWater = cylinder(g, "basinWater", 2.15, 0.02, 28, "water", new Vec3(0, 0.47, 0));
        Ref rim = cylinder(g, "basinRim", 2.55, 0.12, 28, "trim", new Vec3(0, 0.55, 0));
        Ref pedestal = cylinder(g, "pedestal", 0.4, 1.5, 12, "stone");
        Ref bowl = cylinder(g, "bowl", 1.1, 0.3, 20, "stone", new Vec3(0, 1.4, 0));
        Ref bowlWater = cylinder(g, "bowlWater", 0.95, 0.02, 20, "water", new Vec3(0, 1.66, 0));
        Ref jet = cylinder(g, "jet", 0.07, 0.8, 8, "water", new Vec3(0, 1.7, 0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", basin, basinWater, rim, pedestal, bowl, bowlWater, jet));
        return g.build();
    }

    /** Patio: duas lajes longas e duas curtas em volta de um gramado com fonte, caminhos, bancos, postes e arvores. */
    private static GraphAsset courtyard() {
        GraphBuilder g = asset("courtyard");
        sub(g, "north", "tower", "cols_x", 8, "cols_z", 2, "floors", 5);
        sub(g, "south", "tower", "cols_x", 8, "cols_z", 2, "floors", 6);
        sub(g, "east", "tower", "cols_x", 5, "cols_z", 2, "floors", 4);
        sub(g, "west", "tower", "cols_x", 5, "cols_z", 2, "floors", 4);
        Ref n = translate(g, "northAt", out("north", "geometry"), new Vec3(0, 0, -10));
        Ref s = place(g, "southAt", out("south", "geometry"), new Vec3(0, 0, 10), new Vec3(0, 180, 0));
        Ref e = place(g, "eastAt", out("east", "geometry"), new Vec3(10, 0, 0), new Vec3(0, 90, 0));
        Ref wst = place(g, "westAt", out("west", "geometry"), new Vec3(-10, 0, 0), new Vec3(0, -90, 0));
        Ref lawn = part(g, "lawn", new Vec3(14, 0.2, 14), new Vec3(0, 0.1, 0), "grass");
        Ref pathX = part(g, "yardPathX", new Vec3(14, 0.06, 1.8), new Vec3(0, 0.22, 0), "sidewalk");
        Ref pathZ = part(g, "yardPathZ", new Vec3(1.8, 0.06, 14), new Vec3(0, 0.22, 0), "sidewalk");
        Ref plaza = cylinder(g, "plaza", 3.6, 0.1, 28, "sidewalk", new Vec3(0, 0.2, 0));
        sub(g, "fountainPart", "fountain");
        Ref fountain = translate(g, "fountainAt", out("fountainPart", "geometry"), new Vec3(0, 0.3, 0));

        // bancos nas quatro diagonais do patio, voltados para a fonte; postes e arvores nos cantos
        sub(g, "benchPart", "bench");
        Ref benchNE = place(g, "benchNEAt", out("benchPart", "geometry"), new Vec3(3.5, 0.2, 3.5), new Vec3(0, 135, 0));
        Ref benchNW = place(g, "benchNWAt", out("benchPart", "geometry"), new Vec3(-3.5, 0.2, 3.5), new Vec3(0, -135, 0));
        Ref benchSE = place(g, "benchSEAt", out("benchPart", "geometry"), new Vec3(3.5, 0.2, -3.5), new Vec3(0, 45, 0));
        Ref benchSW = place(g, "benchSWAt", out("benchPart", "geometry"), new Vec3(-3.5, 0.2, -3.5), new Vec3(0, -45, 0));
        sub(g, "lampPts", "grid_points", "size_x", 13.0, "size_z", 13.0, "vertices_x", 2, "vertices_z", 2);
        sub(g, "treePts", "grid_points", "size_x", 10.0, "size_z", 10.0, "vertices_x", 2, "vertices_z", 2);
        sub(g, "yardLamp", "lamp");
        sub(g, "yardTree", "broadleaf", "radius", 1.5, "squash", 0.9, "trunk", 1.6);
        node(g, "cornerLamps", "core.instance_on_points", "points", out("lampPts", "points"), "instance", out("yardLamp", "geometry"));
        node(g, "cornerTrees", "core.instance_on_points", "points", out("treePts", "points"), "instance", out("yardTree", "geometry"));
        Ref lampUp = translate(g, "lampUp", out("cornerLamps", "instances"), new Vec3(0, 0.2, 0));
        Ref treeUp = translate(g, "treeUp", out("cornerTrees", "instances"), new Vec3(0, 0.2, 0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", n, s, e, wst, lawn, pathX, pathZ, plaza, fountain,
                benchNE, benchNW, benchSE, benchSW, lampUp, treeUp));
        return g.build();
    }

    // ================= vegetacao e mobiliario =================

    private static GraphAsset tree() {
        GraphBuilder g = asset("tree");
        Ref trunk = part(g, "trunk", new Vec3(0.5, 2.2, 0.5), new Vec3(0, 1.1, 0), "trunk");
        node(g, "ring", "core.create_circle", "radius", 1.6, "segments", 8);
        node(g, "fill", "core.curve_to_mesh", "geometry", out("ring", "curve"));
        Ref base = translate(g, "base", out("fill", "mesh"), new Vec3(0, 2.0, 0));
        node(g, "rise", "core.combine_xyz", "y", 4.0);
        node(g, "cone", "core.extrude", "geometry", base, "offset", out("rise", "vector"));
        Ref[] p = position(g, "t");
        node(g, "pull", "core.combine_xyz", "x", fmath(g, "nx", "negate", p[0], 0.0), "z", fmath(g, "nz", "negate", p[2], 0.0));
        node(g, "tip", "core.set_position", "geometry", out("cone", "geometry"), "selection", cmp(g, "high", "gt", p[1], 4.0),
                "offset", out("pull", "vector"));
        node(g, "clean", "core.merge_by_distance", "geometry", out("tip", "geometry"), "distance", 0.01);
        node(g, "leaves", "core.set_material", "geometry", smooth(g, "pineSmooth", out("clean", "geometry")), "material", "leaves");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", trunk, out("leaves", "geometry")));
        return g.build();
    }

    /** Arvore de copa arredondada: cubo subdividido com os vertices projetados numa esfera (Fields), esticado em Y. */
    private static GraphAsset broadleaf() {
        GraphBuilder g = asset("broadleaf");
        g.input(num("radius", 1.9)).input(num("squash", 0.9)).input(num("trunk", 2.0));
        Ref trunk = part(g, "trunk", vec(g, "trunkSize", 0.45, in("trunk"), 0.45),
                vec(g, "trunkAt", 0.0, math(g, "trunkHalf", "multiply", in("trunk"), 0.5), 0.0), "trunk");
        node(g, "cube", "core.create_box", "size", new Vec3(2, 2, 2));
        node(g, "cells", "core.subdivide", "geometry", out("cube", "mesh"), "levels", 2);
        node(g, "bPos", "core.position");
        node(g, "direction", "core.vector_math", "operation", "normalize", "a", out("bPos", "position"));
        node(g, "onSphere", "core.vector_math", "operation", "scale", "a", out("direction", "result"), "factor", in("radius"));
        node(g, "ball", "core.set_position", "geometry", out("cells", "geometry"), "position", out("onSphere", "result"));
        node(g, "squashed", "core.scale", "geometry", out("ball", "geometry"), "scale", vec(g, "squashBy", 1.0, in("squash"), 1.0));
        Ref raised = translate(g, "raised", smooth(g, "roundCrown", out("squashed", "geometry")), vec(g, "raiseBy", 0.0,
                math(g, "canopyY", "add", in("trunk"), math(g, "canopyRise", "multiply", math(g, "canopyH", "multiply", in("radius"), in("squash")), 0.75)), 0.0));
        node(g, "foliage", "core.set_material", "geometry", raised, "material", "foliage");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", trunk, out("foliage", "geometry")));
        return g.build();
    }

    /** Poste de iluminacao: base, colar, fuste cilindrico com anel, braco articulado e luminaria com lente. */
    private static GraphAsset lamp() {
        GraphBuilder g = asset("lamp");
        Ref base = cylinder(g, "base", 0.3, 0.45, 8, "metal");
        Ref collar = cylinder(g, "collar", 0.18, 0.15, 8, "metal", new Vec3(0, 0.45, 0));
        Ref pole = cylinder(g, "pole", 0.11, 5.7, 8, "metal", new Vec3(0, 0.55, 0));
        Ref ring = cylinder(g, "ring", 0.16, 0.1, 8, "metal", new Vec3(0, 3.2, 0));
        Ref cap = cylinder(g, "cap", 0.14, 0.12, 8, "metal", new Vec3(0, 6.25, 0));
        Ref armA = place(g, "armAAt", part(g, "armA", new Vec3(0.9, 0.1, 0.1), new Vec3(0, 0, 0), "metal"), new Vec3(0.4, 6.4, 0), new Vec3(0, 0, 14));
        Ref armB = place(g, "armBAt", part(g, "armB", new Vec3(0.8, 0.09, 0.09), new Vec3(0, 0, 0), "metal"), new Vec3(1.1, 6.6, 0), new Vec3(0, 0, -6));
        Ref housing = part(g, "housing", new Vec3(0.8, 0.16, 0.44), new Vec3(1.5, 6.62, 0), "metal");
        Ref visor = part(g, "visor", new Vec3(0.86, 0.04, 0.5), new Vec3(1.5, 6.72, 0), "metal");
        Ref lens = part(g, "lens", new Vec3(0.62, 0.05, 0.34), new Vec3(1.5, 6.52, 0), "lamp");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", base, collar, pole, ring, cap, armA, armB, housing, visor, lens));
        return g.build();
    }

    /** Semaforo de esquina: poste, cabeca com quatro faces (vermelho, ambar e verde com viseiras). */
    private static GraphAsset signal() {
        GraphBuilder g = asset("signal");
        Ref base = cylinder(g, "sigBase", 0.2, 0.3, 8, "signal");
        Ref pole = cylinder(g, "sigPole", 0.08, 4.2, 8, "signal", new Vec3(0, 0.3, 0));
        Ref head = part(g, "sigHead", new Vec3(0.55, 1.25, 0.55), new Vec3(0, 4.75, 0), "signal");
        Ref red = part(g, "lightRed", new Vec3(0.22, 0.22, 0.05), new Vec3(0, 5.1, 0.28), "light_red");
        Ref amber = part(g, "lightAmber", new Vec3(0.22, 0.22, 0.05), new Vec3(0, 4.75, 0.28), "light_amber");
        Ref green = part(g, "lightGreen", new Vec3(0.22, 0.22, 0.05), new Vec3(0, 4.4, 0.28), "light_green");
        Ref visorA = part(g, "visorRed", new Vec3(0.3, 0.04, 0.14), new Vec3(0, 5.25, 0.33), "signal");
        Ref visorB = part(g, "visorAmber", new Vec3(0.3, 0.04, 0.14), new Vec3(0, 4.9, 0.33), "signal");
        Ref visorC = part(g, "visorGreen", new Vec3(0.3, 0.04, 0.14), new Vec3(0, 4.55, 0.33), "signal");
        Ref face = join(g, "sigFace", red, amber, green, visorA, visorB, visorC);
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", base, pole, head, face,
                rotate(g, "sigFaceB", face, new Vec3(0, 90, 0)), rotate(g, "sigFaceC", face, new Vec3(0, 180, 0)), rotate(g, "sigFaceD", face, new Vec3(0, 270, 0))));
        return g.build();
    }

    /** Mobiliario de calcada em cada esquina: poste de luz, hidrante e lixeira. */
    private static GraphAsset streetKit() {
        GraphBuilder g = asset("street_kit");
        sub(g, "streetLamp", "lamp");
        Ref hydrantBody = cylinder(g, "hydrant", 0.14, 0.55, 8, "hydrant");
        Ref hydrantCap = cylinder(g, "hydrantCap", 0.17, 0.1, 8, "hydrant", new Vec3(0, 0.5, 0));
        Ref hydrantTop = cylinder(g, "hydrantTop", 0.06, 0.1, 6, "metal", new Vec3(0, 0.6, 0));
        Ref nozzles = part(g, "hydrantNozzle", new Vec3(0.46, 0.1, 0.1), new Vec3(0, 0.33, 0), "hydrant");
        Ref hydrant = translate(g, "hydrantAt", join(g, "hydrantAll", hydrantBody, hydrantCap, hydrantTop, nozzles), new Vec3(0.8, 0, 0.9));
        Ref binBody = cylinder(g, "bin", 0.25, 0.8, 10, "bin");
        Ref binLid = cylinder(g, "binLid", 0.28, 0.07, 10, "metal", new Vec3(0, 0.8, 0));
        Ref binBand = cylinder(g, "binBand", 0.26, 0.06, 10, "metal", new Vec3(0, 0.5, 0));
        Ref bin = translate(g, "binAt", join(g, "binAll", binBody, binLid, binBand), new Vec3(-0.8, 0, 0.7));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("streetLamp", "geometry"), hydrant, bin));
        return g.build();
    }

    /** Arvore de rua: canteiro quadrado de pedra com tronco e copa arredondada. */
    private static GraphAsset streetTree() {
        GraphBuilder g = asset("street_tree");
        sub(g, "crown", "broadleaf", "radius", 1.5, "squash", 0.9, "trunk", 2.4);
        Ref pit = part(g, "pit", new Vec3(1.5, 0.08, 1.5), new Vec3(0, 0.04, 0), "stone");
        Ref guardA = mirror(g, "guardsZ", part(g, "guardA", new Vec3(1.3, 0.1, 0.06), new Vec3(0, 0.45, 0.65), "metal"), "z");
        Ref guardC = mirror(g, "guardsX", part(g, "guardC", new Vec3(0.06, 0.1, 1.3), new Vec3(0.65, 0.45, 0), "metal"), "x");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("crown", "geometry"), pit, guardA, guardC));
        return g.build();
    }

    /**
     * Carro apontado para +Z: carroceria, cabine trapezoidal de vidro com teto, farois, lanternas, para-choques, grade, retrovisores,
     * placa e 4 rodas (pneu + calota cilindricos, instancias nos vertices de uma grade 2x2).
     */
    private static GraphAsset car() {
        GraphBuilder g = asset("car");
        Ref body = part(g, "carBody", new Vec3(1.9, 0.55, 4.3), new Vec3(0, 0.62, 0), "car_body");

        // cabine: caixa de vidro com o topo puxado para tras/frente (para-brisa e vidro traseiro inclinados) e teto pintado
        node(g, "cabBox", "core.create_box", "size", new Vec3(1.7, 0.6, 2.4));
        Ref[] cp = position(g, "cab");
        Ref topSide = cmp(g, "cabTop", "gt", cp[1], 0.0);
        Ref frontSide = cmp(g, "cabFront", "gt", cp[2], 0.0);
        node(g, "cabEnds", "core.switch_float", "condition", frontSide, "if_true", -0.5, "if_false", 0.38);
        node(g, "cabShift", "core.switch_float", "condition", topSide, "if_true", out("cabEnds", "result"), "if_false", 0.0);
        node(g, "cabOffset", "core.combine_xyz", "z", out("cabShift", "result"));
        node(g, "cabTaper", "core.set_position", "geometry", out("cabBox", "mesh"), "offset", out("cabOffset", "vector"));
        Ref cabAt = translate(g, "cabAt", out("cabTaper", "geometry"), new Vec3(0, 1.2, -0.15));
        node(g, "cabGlass", "core.set_material", "geometry", cabAt, "material", "car_glass");
        Ref[] cn = normal(g, "cab");
        node(g, "cabRoof", "core.set_material", "geometry", out("cabGlass", "geometry"), "selection", cmp(g, "cabTopFace", "gt", cn[1], 0.9),
                "material", "car_body");

        Ref headR = part(g, "headR", new Vec3(0.36, 0.15, 0.06), new Vec3(0.65, 0.7, 2.17), "headlight");
        Ref tailR = part(g, "tailR", new Vec3(0.4, 0.12, 0.06), new Vec3(0.7, 0.72, -2.17), "taillight");
        Ref bumperF = part(g, "bumperF", new Vec3(1.96, 0.2, 0.16), new Vec3(0, 0.4, 2.15), "tire");
        Ref bumperB = part(g, "bumperB", new Vec3(1.96, 0.2, 0.16), new Vec3(0, 0.4, -2.15), "tire");
        Ref grille = part(g, "grille", new Vec3(0.9, 0.12, 0.05), new Vec3(0, 0.62, 2.17), "tire");
        Ref plate = part(g, "plate", new Vec3(0.5, 0.13, 0.03), new Vec3(0, 0.55, -2.17), "trim");
        Ref mirrorR = part(g, "mirrorR", new Vec3(0.12, 0.1, 0.2), new Vec3(1.02, 1.0, 0.45), "car_body");
        Ref lightsAndMirrors = mirror(g, "lightsAndMirrors", join(g, "rightSide", headR, tailR, mirrorR), "x");

        // rodas: pneu + calota (eixo X), uma instancia por vertice de uma grade 2x2
        Ref tire = cylinder(g, "tire", 0.34, 0.26, 12, "tire", new Vec3(0, -0.13, 0));
        Ref rim = cylinder(g, "rim", 0.2, 0.27, 10, "rim", new Vec3(0, -0.135, 0));
        Ref hub = cylinder(g, "hub", 0.06, 0.3, 6, "metal", new Vec3(0, -0.15, 0));
        Ref wheel = place(g, "wheelAt", join(g, "wheelAll", tire, rim, hub), new Vec3(0, 0.34, 0), new Vec3(0, 0, 90));
        sub(g, "wheelSpots", "grid_points", "size_x", 1.9, "size_z", 2.8, "vertices_x", 2, "vertices_z", 2);
        node(g, "wheels", "core.instance_on_points", "points", out("wheelSpots", "points"), "instance", wheel);
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", body, out("cabRoof", "geometry"), lightsAndMirrors, bumperF, bumperB,
                grille, plate, out("wheels", "instances")));
        return g.build();
    }

    /**
     * Parque: gramado, dois caminhos em cruz, lago com borda de pedras, bancos, postes, canteiros de flores e
     * arvores espalhadas fora do lago e dos caminhos.
     */
    private static GraphAsset park() {
        GraphBuilder g = asset("park");
        g.input(num("size", 26)).input(num("density", 0.025));
        Ref grass = part(g, "grass", vec(g, "grassSize", in("size"), 0.4, in("size")), new Vec3(0, 0.2, 0), "grass");
        Ref pathX = part(g, "pathX", vec(g, "pathXSize", in("size"), 0.12, 2.2), new Vec3(0, 0.4, 0), "sidewalk");
        Ref pathZ = part(g, "pathZ", vec(g, "pathZSize", 2.2, 0.12, in("size")), new Vec3(0, 0.4, 0), "sidewalk");
        sub(g, "pond", "park_pond", "size", in("size"));
        sub(g, "furniture", "park_furniture", "size", in("size"));
        sub(g, "plantable", "park_plantable", "size", in("size"));
        sub(g, "trees", "park_trees", "size", in("size"), "density", in("density"), "plantable", out("plantable", "plantable"));
        sub(g, "beds", "park_beds", "size", in("size"), "plantable", out("plantable", "plantable"));
        return finish(g, join(g, "all", grass, pathX, pathZ, out("pond", "geometry"), out("furniture", "geometry"),
                out("beds", "geometry"), out("trees", "geometry")));
    }

    private static Ref pondRadius(GraphBuilder g, Ref size) {
        return math(g, "pondR", "multiply", size, 0.17);
    }

    /** Lago (disco de agua) com borda de pedras sorteadas em volta (instancias nos pontos de um circulo). */
    private static GraphAsset parkPondAsset() {
        GraphBuilder g = asset("park_pond");
        g.input(num("size", 26));
        Ref pondR = pondRadius(g, in("size"));
        node(g, "pondRing", "core.create_circle", "radius", pondR, "segments", 28);
        node(g, "pondFill", "core.curve_to_mesh", "geometry", out("pondRing", "curve"));
        Ref pondUp = translate(g, "pondUp", out("pondFill", "mesh"), new Vec3(0, 0.47, 0));
        node(g, "pond", "core.set_material", "geometry", pondUp, "material", "water");
        node(g, "rimCircle", "core.create_circle", "radius", math(g, "rimR", "add", pondR, 0.35), "segments", 30);
        node(g, "rimPts", "core.curve_to_points", "geometry", out("rimCircle", "curve"));
        Ref rimUp = translate(g, "rimUp", out("rimPts", "points"), new Vec3(0, 0.4, 0));
        Ref rock = part(g, "rock", new Vec3(0.8, 0.4, 0.6), new Vec3(0, 0.15, 0), "stone");
        node(g, "rockSize", "core.random_value", "min_vector", new Vec3(0.7, 0.6, 0.8), "max_vector", new Vec3(1.4, 1.3, 1.5));
        node(g, "rockSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "rocks", "core.instance_on_points", "points", rimUp, "instance", rock, "rotation", out("rockSpin", "vector"),
                "scale", out("rockSize", "vector"));
        return finish(g, join(g, "all", out("pond", "geometry"), out("rocks", "instances")));
    }

    /** Quatro bancos nos vertices de uma grade em volta do lago e quatro postes nos cantos dos caminhos. */
    private static GraphAsset parkFurnitureAsset() {
        GraphBuilder g = asset("park_furniture");
        g.input(num("size", 26));
        sub(g, "benchPart", "bench");
        sub(g, "benchPoints", "grid_points", "size_x", math(g, "benchSpan", "multiply", pondRadius(g, in("size")), 2.9),
                "size_z", out("benchSpan", "result"), "vertices_x", 2, "vertices_z", 2);
        Ref benchUp = translate(g, "benchUp", out("benchPoints", "points"), new Vec3(0, 0.4, 0));
        node(g, "benches", "core.instance_on_points", "points", benchUp, "instance", out("benchPart", "geometry"),
                "rotation", new Vec3(0, 0, 0));
        sub(g, "parkLamp", "lamp");
        sub(g, "lampPoints", "grid_points", "size_x", math(g, "lampSpan", "multiply", in("size"), 0.78), "size_z", out("lampSpan", "result"),
                "vertices_x", 2, "vertices_z", 2);
        Ref lampUp = translate(g, "lampUp", out("lampPoints", "points"), new Vec3(0, 0.4, 0));
        node(g, "lamps", "core.instance_on_points", "points", lampUp, "instance", out("parkLamp", "geometry"));
        return finish(g, join(g, "all", out("benches", "instances"), out("lamps", "instances")));
    }

    /** Field booleano: onde se pode plantar (fora do lago e dos caminhos). */
    private static GraphAsset parkPlantableAsset() {
        GraphBuilder g = asset("park_plantable");
        g.input(num("size", 26));
        Ref pondR = pondRadius(g, in("size"));
        Ref[] p = position(g, "p");
        Ref r2 = fmath(g, "r2", "add", fmath(g, "px2", "multiply", p[0], p[0]), fmath(g, "pz2", "multiply", p[2], p[2]));
        Ref farFromPond = cmp(g, "farFromPond", "gt", r2, math(g, "pondClear2", "multiply", math(g, "pondClear", "add", pondR, 2.5),
                math(g, "pondClearB", "add", pondR, 2.5)));
        Ref offPathX = cmp(g, "offPathX", "gt", fmath(g, "absX", "abs", p[0], 0.0), 2.0);
        Ref offPathZ = cmp(g, "offPathZ", "gt", fmath(g, "absZ", "abs", p[2], 0.0), 2.0);
        output(g, "plantable", PortType.fieldOf(ValueKind.BOOL), and(g, "plantable", farFromPond, and(g, "offPaths", offPathX, offPathZ)));
        return g.build();
    }

    /** Pontos aleatorios (por densidade) numa grade de lado {@code size - 6}, no nivel do gramado. */
    private static GraphAsset parkScatterAsset() {
        GraphBuilder g = asset("park_scatter");
        g.input(num("size", 26)).input(num("density", 0.025));
        Ref inner = math(g, "inner", "subtract", in("size"), 6.0);
        node(g, "spread", "core.create_grid", "size_x", inner, "size_z", inner);
        Ref spreadUp = translate(g, "spreadUp", out("spread", "mesh"), new Vec3(0, 0.4, 0));
        node(g, "scatter", "core.distribute_points", "geometry", spreadUp, "density", in("density"));
        g.output("points", PortType.GEOMETRY, "scatter", "points");
        return g.build();
    }

    /** Arvores nos pontos plantaveis: tres especies sorteadas (pinheiro, carvalho, choupo), com tom de cor, giro e escala. */
    private static GraphAsset parkTreesAsset() {
        GraphBuilder g = asset("park_trees");
        g.input(num("size", 26)).input(num("density", 0.025)).input(fieldIn("plantable", ValueKind.BOOL));
        sub(g, "scatter", "park_scatter", "size", in("size"), "density", in("density"));
        sub(g, "pine", "tree");
        sub(g, "oak", "broadleaf", "radius", 2.0, "squash", 0.9, "trunk", 2.0);
        sub(g, "poplar", "broadleaf", "radius", 1.0, "squash", 2.4, "trunk", 1.2);
        node(g, "pineI", "core.geometry_to_instance", "geometry", out("pine", "geometry"));
        node(g, "oakI", "core.geometry_to_instance", "geometry", out("oak", "geometry"));
        node(g, "poplarI", "core.geometry_to_instance", "geometry", out("poplar", "geometry"));
        Ref species = join(g, "species", out("pineI", "instances"), out("oakI", "instances"), out("poplarI", "instances"));
        node(g, "species_pick", "core.random_value", "min_int", 0, "max_int", 2);
        node(g, "treeTone", "core.random_value");
        node(g, "tonedTrees", "core.set_attribute", "geometry", out("scatter", "points"), "name", "tone", "domain", "point",
                "type", "float", "value", out("treeTone", "float"));
        node(g, "scale", "core.random_value", "min_vector", new Vec3(0.7, 0.7, 0.7), "max_vector", new Vec3(1.4, 1.4, 1.4));
        node(g, "spin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "trees", "core.instance_on_points", "points", out("tonedTrees", "geometry"), "instance", species, "pick_instance", true,
                "instance_index", out("species_pick", "int"), "selection", in("plantable"), "rotation", out("spin", "vector"),
                "scale", out("scale", "vector"));
        return finish(g, out("trees", "instances"));
    }

    /** Canteiros de flores (base de pedra + terra florida) nos pontos plantaveis, com tom de cor e giro. */
    private static GraphAsset parkBedsAsset() {
        GraphBuilder g = asset("park_beds");
        g.input(num("size", 26)).input(fieldIn("plantable", ValueKind.BOOL));
        sub(g, "scatter", "park_scatter", "size", in("size"), "density", 0.03);
        Ref bed = part(g, "gardenBed", new Vec3(1.6, 0.18, 1.1), new Vec3(0, 0.09, 0), "stone");
        Ref soil = part(g, "gardenFlowers", new Vec3(1.35, 0.3, 0.85), new Vec3(0, 0.2, 0), "flowers");
        node(g, "bedTone", "core.random_value");
        node(g, "tonedBeds", "core.set_attribute", "geometry", out("scatter", "points"), "name", "tone", "domain", "point",
                "type", "float", "value", out("bedTone", "float"));
        node(g, "bedSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 90, 0));
        node(g, "beds", "core.instance_on_points", "points", out("tonedBeds", "geometry"), "instance", join(g, "bedAll", bed, soil),
                "selection", in("plantable"), "rotation", out("bedSpin", "vector"));
        return finish(g, out("beds", "instances"));
    }

    // ================= ruas =================

    private static GraphAsset street() {
        GraphBuilder g = asset("street");
        g.input(num("length", 100)).input(num("width", 10)).input(num("y", 0.1)).input(material("material", "sidewalk"));
        Ref half = math(g, "half", "multiply", in("length"), 0.5);
        Ref negHalf = math(g, "negHalf", "multiply", half, -1.0);
        node(g, "path", "core.create_line", "start", vec(g, "a", 0.0, in("y"), negHalf), "end", vec(g, "b", 0.0, in("y"), half));
        Ref hw = math(g, "hw", "multiply", in("width"), 0.5);
        Ref nhw = math(g, "nhw", "multiply", hw, -1.0);
        node(g, "profile", "core.create_line", "start", vec(g, "pa", nhw, 0.0, 0.0), "end", vec(g, "pb", hw, 0.0, 0.0));
        node(g, "ribbon", "core.curve_to_mesh", "geometry", out("path", "curve"), "profile", out("profile", "curve"));
        node(g, "paint", "core.set_material", "geometry", out("ribbon", "mesh"), "material", in("material"));
        output(g, "geometry", PortType.GEOMETRY, out("paint", "geometry"));
        return g.build();
    }

    /** Calcada: faixa base + duas faixas de lajotas em relevo (inset + extrude em cada celula) junto das bordas. */
    private static GraphAsset pavement() {
        GraphBuilder g = asset("pavement");
        g.input(num("length", 100)).input(num("width", 15)).input(num("y", 0.06));
        sub(g, "base", "street", "length", in("length"), "width", in("width"), "y", in("y"), "material", "sidewalk");
        sub(g, "tileRows", "step_count", "length", in("length"), "step", 1.4);
        node(g, "tileGrid", "core.create_grid", "size_x", 2.5, "size_z", in("length"), "vertices_x", 3, "vertices_z", out("tileRows", "result"));
        node(g, "tileFrame", "core.inset_faces", "geometry", out("tileGrid", "mesh"), "amount", 0.05, "inner_attribute", "tile_inner");
        Ref tileInner = flag(g, "tileInner", "tile_inner");
        node(g, "tileRaise", "core.extrude", "geometry", out("tileFrame", "geometry"), "selection", tileInner, "distance", 0.03);
        node(g, "tilePaint", "core.set_material", "geometry", out("tileRaise", "geometry"), "material", "paving");
        node(g, "groutPaint", "core.set_material", "geometry", out("tilePaint", "geometry"), "selection", not(g, "notTile", tileInner),
                "material", "sidewalk");
        Ref lifted = translate(g, "tileLift", out("groutPaint", "geometry"), vec(g, "tileLiftBy", 0.0, math(g, "tileY", "add", in("y"), 0.004), 0.0));
        Ref offset = math(g, "bandOffset", "subtract", math(g, "halfWidth", "multiply", in("width"), 0.5), 1.25);
        Ref bands = mirror(g, "bands", translate(g, "tilesRightAt", lifted, vec(g, "tilesRightPos", offset, 0.0, 0.0)), "x");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("base", "geometry"), bands));
        return g.build();
    }

    /**
     * Rua asfaltada: faixa + tracejado central + faixas laterais + meio-fio + faixas de pedestre + linhas de retencao + bueiros +
     * carros em duas pistas. Cada elemento e um subgrafo; todos recebem {@code away} (Field: distancia ao cruzamento mais
     * proximo, medida na posicao do elemento), entao marcacoes, meio-fio e carros so aparecem longe dos cruzamentos.
     */
    private static GraphAsset road() {
        GraphBuilder g = asset("road");
        g.input(num("length", 100)).input(num("width", 10)).input(num("y", 0.12)).input(num("pitch", 40)).input(num("size", 200));
        sub(g, "ribbon", "street", "length", in("length"), "width", in("width"), "y", in("y"), "material", "road");
        sub(g, "zones", "road_zones", "pitch", in("pitch"), "size", in("size"));
        Ref away = out("zones", "away");
        sub(g, "dashes", "road_dashes", "length", in("length"), "y", in("y"), "width", in("width"), "away", away);
        sub(g, "edges", "road_edges", "length", in("length"), "y", in("y"), "width", in("width"), "away", away);
        sub(g, "curbs", "road_curbs", "length", in("length"), "y", in("y"), "width", in("width"), "away", away);
        sub(g, "crossings", "road_crosswalks", "length", in("length"), "y", in("y"), "width", in("width"), "away", away);
        sub(g, "stops", "road_stop_lines", "length", in("length"), "y", in("y"), "width", in("width"), "away", away,
                "wrapped", out("zones", "wrapped"));
        sub(g, "traffic", "road_traffic", "geometry", out("ribbon", "geometry"), "width", in("width"), "away", away);
        return finish(g, join(g, "all", out("ribbon", "geometry"), out("dashes", "geometry"), out("edges", "geometry"),
                out("curbs", "geometry"), out("crossings", "geometry"), out("stops", "geometry"), out("traffic", "geometry")));
    }

    private static NodeInputDefinition fieldIn(String name, ValueKind kind) {
        return NodeInputDefinition.of(name, PortType.fieldOf(kind));
    }

    /**
     * Posicao ao longo da rua em relacao aos cruzamentos (a cada {@code pitch}): {@code wrapped} = distancia assinada ao
     * cruzamento mais proximo e {@code away} = o modulo dela. Fields, avaliados onde forem usados.
     */
    private static GraphAsset roadZonesAsset() {
        GraphBuilder g = asset("road_zones");
        g.input(num("pitch", 40)).input(num("size", 200));
        Ref[] p = position(g, "m");
        Ref shifted = fmath(g, "shifted", "add", p[2], math(g, "origin", "add", math(g, "halfSize", "multiply", in("size"), 0.5),
                math(g, "halfPitch", "multiply", in("pitch"), 0.5)));
        Ref wrapped = fmath(g, "wrapped", "subtract", fmath(g, "mod", "modulo", shifted, in("pitch")), math(g, "halfPitch2", "multiply", in("pitch"), 0.5));
        Ref away = fmath(g, "away", "abs", wrapped, 0.0);
        output(g, "away", PortType.fieldOf(ValueKind.FLOAT), away);
        output(g, "wrapped", PortType.fieldOf(ValueKind.FLOAT), wrapped);
        return g.build();
    }

    private static GraphBuilder roadPart(String id) {
        GraphBuilder g = asset(id);
        g.input(num("length", 100)).input(num("y", 0.12)).input(num("width", 10)).input(fieldIn("away", ValueKind.FLOAT));
        return g;
    }

    /** Tracejado central: uma caixa fina por ponto de uma linha, so longe dos cruzamentos. */
    private static GraphAsset roadDashesAsset() {
        GraphBuilder g = roadPart("road_dashes");
        Ref halfLen = math(g, "halfLen", "multiply", in("length"), 0.5);
        Ref dashY = math(g, "dashY", "add", in("y"), 0.03);
        sub(g, "dashCount", "step_count", "length", in("length"), "step", 6.0, "operation", "max", "extra", 2);
        Ref pts = linePoints(g, "dashPoints", vec(g, "dashA", 0.0, dashY, math(g, "dashNeg", "multiply", halfLen, -1.0)),
                vec(g, "dashB", 0.0, dashY, halfLen), out("dashCount", "result"));
        Ref clear = cmp(g, "clear", "gt", in("away"), math(g, "clearance", "add", math(g, "halfW", "multiply", in("width"), 0.5), 3.5));
        Ref dash = part(g, "dash", new Vec3(0.3, 0.03, 2.4), new Vec3(0, 0.015, 0), "marking");
        node(g, "dashes", "core.instance_on_points", "points", pts, "instance", dash, "selection", clear);
        return finish(g, out("dashes", "instances"));
    }

    /** Faixas laterais continuas (tracos longos) perto das bordas. */
    private static GraphAsset roadEdgesAsset() {
        GraphBuilder g = roadPart("road_edges");
        Ref halfLen = math(g, "halfLen", "multiply", in("length"), 0.5);
        Ref dashY = math(g, "dashY", "add", in("y"), 0.03);
        Ref halfW = math(g, "halfW", "multiply", in("width"), 0.5);
        sub(g, "edgeCount", "step_count", "length", in("length"), "step", 3.0, "operation", "max", "extra", 2);
        Ref pts = linePoints(g, "edgePts", vec(g, "edgeA", 0.0, dashY, math(g, "edgeNeg", "multiply", halfLen, -1.0)),
                vec(g, "edgeB", 0.0, dashY, halfLen), out("edgeCount", "result"));
        Ref clear = cmp(g, "clear", "gt", in("away"), math(g, "clearance", "add", halfW, 3.5));
        Ref edgeSide = math(g, "edgeSide", "subtract", halfW, 0.7);
        Ref edgeLine = mirror(g, "edgeLines", part(g, "edgeR", new Vec3(0.14, 0.03, 2.7), vec(g, "edgeRAt", edgeSide, 0.015, 0.0), "marking"), "x");
        node(g, "edges", "core.instance_on_points", "points", pts, "instance", edgeLine, "selection", clear);
        return finish(g, out("edges", "instances"));
    }

    /** Meio-fio nos dois lados, interrompido a meia rua do cruzamento. */
    private static GraphAsset roadCurbsAsset() {
        GraphBuilder g = roadPart("road_curbs");
        Ref halfLen = math(g, "halfLen", "multiply", in("length"), 0.5);
        Ref halfW = math(g, "halfW", "multiply", in("width"), 0.5);
        sub(g, "curbCount", "step_count", "length", in("length"), "step", 1.5);
        Ref pts = linePoints(g, "curbPts", vec(g, "curbA", 0.0, in("y"), math(g, "curbNeg", "multiply", halfLen, -1.0)),
                vec(g, "curbB", 0.0, in("y"), halfLen), out("curbCount", "result"));
        Ref curbOpen = cmp(g, "curbOpen", "gt", in("away"), math(g, "curbClear", "add", halfW, 0.7));
        Ref curbEdge = math(g, "curbEdge", "add", halfW, 0.17);
        Ref curbPair = mirror(g, "curbPair", part(g, "curbR", new Vec3(0.34, 0.2, 1.56), vec(g, "curbRAt", curbEdge, 0.08, 0.0), "curb"), "x");
        node(g, "curbs", "core.instance_on_points", "points", pts, "instance", curbPair, "selection", curbOpen);
        return finish(g, out("curbs", "instances"));
    }

    /** Faixas de pedestre: grade fina de pontos, filtrada por Field para a faixa de 2.6 m antes de cada cruzamento. */
    private static GraphAsset roadCrosswalksAsset() {
        GraphBuilder g = roadPart("road_crosswalks");
        Ref halfW = math(g, "halfW", "multiply", in("width"), 0.5);
        sub(g, "cwCols", "step_count", "length", in("width"), "step", 1.1, "operation", "max", "extra", 2);
        sub(g, "cwRows", "step_count", "length", in("length"), "step", 1.0);
        node(g, "cwGrid", "core.create_grid", "size_x", math(g, "cwSpan", "subtract", in("width"), 1.6), "size_z", in("length"),
                "vertices_x", out("cwCols", "result"), "vertices_z", out("cwRows", "result"));
        Ref cwUp = translate(g, "cwUp", out("cwGrid", "mesh"), vec(g, "cwAt", 0.0, math(g, "dashY", "add", in("y"), 0.03), 0.0));
        node(g, "cwPoints", "core.mesh_to_points", "geometry", cwUp);
        Ref stripe = part(g, "stripe", new Vec3(0.6, 0.03, 1.05), new Vec3(0, 0.015, 0), "marking");
        Ref inBand = and(g, "inBand", cmp(g, "bandNear", "gt", in("away"), math(g, "bandFrom", "add", halfW, 0.8)),
                cmp(g, "bandFar", "lt", in("away"), math(g, "bandTo", "add", halfW, 3.4)));
        node(g, "crossings", "core.instance_on_points", "points", out("cwPoints", "points"), "instance", stripe, "selection", inBand);
        return finish(g, out("crossings", "instances"));
    }

    /** Linhas de retencao: uma caixa por pista, so no lado de quem chega ao cruzamento. */
    private static GraphAsset roadStopLinesAsset() {
        GraphBuilder g = roadPart("road_stop_lines");
        g.input(fieldIn("wrapped", ValueKind.FLOAT));
        Ref halfW = math(g, "halfW", "multiply", in("width"), 0.5);
        Ref[] p = position(g, "m");
        sub(g, "rows", "step_count", "length", in("length"), "step", 1.0);
        node(g, "stopGrid", "core.create_grid", "size_x", math(g, "stopSpan", "multiply", in("width"), 0.5), "size_z", in("length"),
                "vertices_x", 2, "vertices_z", out("rows", "result"));
        Ref stopUp = translate(g, "stopUp", out("stopGrid", "mesh"), vec(g, "stopAt", 0.0, math(g, "dashY", "add", in("y"), 0.03), 0.0));
        node(g, "stopPoints", "core.mesh_to_points", "geometry", stopUp);
        Ref nearStop = and(g, "nearStop", cmp(g, "stopNear", "gt", in("away"), math(g, "stopFrom", "add", halfW, 3.5)),
                cmp(g, "stopFar", "lt", in("away"), math(g, "stopTo", "add", halfW, 4.5)));
        node(g, "arriving", "core.boolean_math", "operation", "or",
                "a", and(g, "rightLane", cmp(g, "laneRightSide", "gt", p[0], 0.0), cmp(g, "beforeCenter", "lt", in("wrapped"), 0.0)),
                "b", and(g, "leftLane", cmp(g, "laneLeftSide", "lt", p[0], 0.0), cmp(g, "afterCenter", "gt", in("wrapped"), 0.0)));
        Ref stopLine = part(g, "stopLine", vec(g, "stopSize", math(g, "stopWidth", "subtract", halfW, 1.4), 0.03, 0.45),
                new Vec3(0, 0.015, 0), "marking");
        node(g, "stops", "core.instance_on_points", "points", out("stopPoints", "points"), "instance", stopLine,
                "selection", and(g, "stopHere", nearStop, out("arriving", "result")));
        return finish(g, out("stops", "instances"));
    }

    /** Bueiros e carros espalhados sobre a faixa {@code geometry}; os carros ocupam uma de duas pistas, longe dos cruzamentos. */
    private static GraphAsset roadTrafficAsset() {
        GraphBuilder g = asset("road_traffic");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("width", 10)).input(fieldIn("away", ValueKind.FLOAT));
        Ref halfW = math(g, "halfW", "multiply", in("width"), 0.5);
        Ref clear = cmp(g, "clear", "gt", in("away"), math(g, "clearance", "add", halfW, 3.5));
        node(g, "manholeSpots", "core.distribute_points", "geometry", in("geometry"), "density", 0.0007);
        Ref manhole = join(g, "manholeAll", cylinder(g, "manhole", 0.5, 0.03, 14, "metal"),
                cylinder(g, "manholeCore", 0.34, 0.036, 10, "tire", new Vec3(0, 0, 0)));
        node(g, "manholes", "core.instance_on_points", "points", out("manholeSpots", "points"), "instance", manhole);

        node(g, "carSpots", "core.distribute_points", "geometry", in("geometry"), "density", 0.0035);
        node(g, "laneRnd", "core.random_value", "min_int", 0, "max_int", 1);
        Ref lane = out("laneRnd", "int");
        Ref[] p = position(g, "m");
        node(g, "laneX", "core.switch_float", "condition", cmp(g, "laneIsA", "gt", lane, 0.5),
                "if_true", math(g, "laneRight", "multiply", in("width"), 0.25), "if_false", math(g, "laneLeft", "multiply", in("width"), -0.25));
        node(g, "lanePos", "core.combine_xyz", "x", out("laneX", "result"), "y", p[1], "z", p[2]);
        node(g, "laned", "core.set_position", "geometry", out("carSpots", "points"), "position", out("lanePos", "vector"));
        node(g, "carColor", "core.random_value");
        node(g, "colored", "core.set_attribute", "geometry", out("laned", "geometry"), "name", "tone", "domain", "point", "type", "float",
                "value", out("carColor", "float"));
        sub(g, "vehicle", "car");
        node(g, "heading", "core.combine_xyz", "y", fmath(g, "headingDeg", "multiply", lane, 180.0));
        node(g, "cars", "core.instance_on_points", "points", out("colored", "geometry"), "instance", out("vehicle", "geometry"),
                "selection", clear, "rotation", out("heading", "vector"));
        return finish(g, join(g, "all", out("manholes", "instances"), out("cars", "instances")));
    }

    /** Uma rua por linha de pontos (so a borda z &lt; 0 de uma grade de 2 linhas), nas duas direcoes; {@code ribbons} = variantes sorteadas. */
    private static GraphAsset ringStreets(String id, String ribbonAsset, int variants) {
        GraphBuilder g = asset(id);
        g.input(integer("blocks", 5)).input(num("pitch", 40)).input(num("width", 10)).input(num("y", 0.1));
        Ref size = math(g, "size", "multiply", in("blocks"), in("pitch"));
        Ref length = math(g, "length", "add", size, in("width"));

        Ref[] choices = new Ref[variants];
        for (int i = 0; i < variants; i++) {
            if (ribbonAsset.equals("road")) {
                sub(g, "ribbon" + i, "road", "length", length, "width", in("width"), "y", in("y"), "pitch", in("pitch"), "size", size);
            } else {
                sub(g, "ribbon" + i, "pavement", "length", length, "width", in("width"), "y", in("y"));
            }
            node(g, "choice" + i, "core.geometry_to_instance", "geometry", out("ribbon" + i, "geometry"));
            choices[i] = out("choice" + i, "instances");
        }
        Ref options = join(g, "options", choices);

        node(g, "count", "core.int_math", "operation", "add", "a", in("blocks"), "b", 1);
        sub(g, "pts", "grid_points", "size_x", size, "size_z", 2.0, "vertices_x", out("count", "result"), "vertices_z", 2);
        Ref[] p = position(g, "line");
        node(g, "which", "core.random_value", "min_int", 0, "max_int", variants - 1);
        node(g, "alongZ", "core.instance_on_points", "points", out("pts", "points"), "instance", options,
                "selection", cmp(g, "oneSide", "lt", p[2], 0.0), "pick_instance", true, "instance_index", out("which", "int"));
        Ref alongX = rotate(g, "alongX", out("alongZ", "instances"), new Vec3(0, 90, 0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("alongZ", "instances"), alongX));
        return g.build();
    }

    private static GraphAsset sidewalkGrid() {
        return ringStreets("sidewalk_grid", "pavement", 1);
    }

    private static GraphAsset roadGrid() {
        return ringStreets("road_grid", "road", 4);
    }

    // ================= cidade =================

    private static GraphAsset city() {
        GraphBuilder g = asset("city");
        g.input(integer("blocks", 5)).input(num("block_size", 30)).input(num("street_width", 10));
        Ref pitch = math(g, "pitch", "add", in("block_size"), in("street_width"));
        Ref size = math(g, "size", "multiply", in("blocks"), pitch);

        // terreno, calcadas (mais largas, mais baixas) e asfalto
        Ref groundSize = math(g, "groundSize", "add", size, pitch);
        node(g, "groundGrid", "core.create_grid", "size_x", groundSize, "size_z", groundSize);
        node(g, "ground", "core.set_material", "geometry", out("groundGrid", "mesh"), "material", "ground");
        sub(g, "sidewalks", "sidewalk_grid", "blocks", in("blocks"), "pitch", pitch, "width",
                math(g, "sidewalkWidth", "add", in("street_width"), 5.0), "y", 0.06);
        sub(g, "roads", "road_grid", "blocks", in("blocks"), "pitch", pitch, "width", in("street_width"), "y", 0.12);

        sub(g, "lotChoices", "city_lots");
        sub(g, "lots", "city_blocks", "blocks", in("blocks"), "size", size, "choices", out("lotChoices", "geometry"));
        sub(g, "lamps", "city_corner_lamps", "blocks", in("blocks"), "size", size, "block_size", in("block_size"));
        sub(g, "trees", "city_street_trees", "blocks", in("blocks"), "size", size, "block_size", in("block_size"));
        sub(g, "signals", "city_signals", "blocks", in("blocks"), "size", size);

        join(g, "world", out("ground", "geometry"), out("sidewalks", "geometry"), out("roads", "geometry"), out("lots", "geometry"),
                out("lamps", "geometry"), out("trees", "geometry"), out("signals", "geometry"));
        node(g, "flat", "core.realize_instances", "geometry", out("world", "geometry"));
        output(g, "geometry", PortType.GEOMETRY, out("flat", "geometry"));
        return g.build();
    }

    /** Pontos da grade de quarteiroes ({@code blocks} x {@code blocks} celulas, lado total {@code size}): centros das celulas ("faces") ou cruzamentos ("vertices"). */
    private static GraphAsset blockPointsAsset() {
        GraphBuilder g = asset("block_points");
        g.input(integer("blocks", 5)).input(num("size", 200)).input(NodeInputDefinition.of("mode", PortType.STRING).withDefault("faces"));
        node(g, "count", "core.int_math", "operation", "add", "a", in("blocks"), "b", 1);
        node(g, "grid", "core.create_grid", "size_x", in("size"), "size_z", in("size"), "vertices_x", out("count", "result"),
                "vertices_z", out("count", "result"));
        node(g, "points", "core.mesh_to_points", "geometry", out("grid", "mesh"), "mode", in("mode"));
        g.output("points", PortType.GEOMETRY, "points", "points");
        return g.build();
    }

    /** As variantes de lote (cada quarteirao sorteia uma), prontas como instancias: torres, galpao, cilindro, parque, casas, patio e podio. */
    private static GraphAsset cityLotsAsset() {
        GraphBuilder g = asset("city_lots");
        sub(g, "skyscraper", "stepped_tower", "cols_x", 4, "cols_z", 4, "floors", 20);
        sub(g, "skyscraper2", "stepped_tower", "cols_x", 5, "cols_z", 5, "floors", 12);
        sub(g, "midrise", "tower", "cols_x", 5, "cols_z", 4, "floors", 14);
        sub(g, "slab", "tower", "cols_x", 8, "cols_z", 3, "floors", 7, "floor_h", 3.4);
        sub(g, "warehouse", "tower", "cols_x", 7, "cols_z", 6, "floors", 2, "floor_h", 4.5, "chance", 0.3);
        sub(g, "cylinder", "round_tower", "radius", 8.0, "floors", 20);
        sub(g, "parkLot", "park");
        sub(g, "housesA", "house_block", "wide", 2, "deep", 2);
        sub(g, "housesB", "house_block", "wide", 3, "deep", 2);
        sub(g, "yard", "courtyard");
        sub(g, "podiumBase", "tower", "cols_x", 7, "cols_z", 7, "floors", 3, "floor_h", 3.4);
        sub(g, "podiumTop", "tower", "cols_x", 4, "cols_z", 4, "floors", 16);
        Ref podium = join(g, "podium", out("podiumBase", "geometry"),
                translate(g, "podiumTopUp", out("podiumTop", "geometry"), new Vec3(0, 10.2, 0)));
        Ref[] lots = {out("skyscraper", "geometry"), out("skyscraper2", "geometry"), out("midrise", "geometry"), out("slab", "geometry"),
                out("warehouse", "geometry"), out("cylinder", "geometry"), out("parkLot", "geometry"), out("housesA", "geometry"),
                out("housesB", "geometry"), out("yard", "geometry"), podium};
        Ref[] variants = new Ref[lots.length];
        for (int i = 0; i < lots.length; i++) {
            node(g, "lot" + i, "core.geometry_to_instance", "geometry", lots[i]);
            variants[i] = out("lot" + i, "instances");
        }
        return finish(g, join(g, "choices", variants));
    }

    /**
     * Um lote por quarteirao: sorteia uma das {@code choices} (com giro de 90 graus, esticada em Y e com tom de cor proprio);
     * o quarteirao central (perto da origem) e sempre um parque.
     */
    private static GraphAsset cityBlocksAsset() {
        GraphBuilder g = asset("city_blocks");
        g.input(integer("blocks", 5)).input(num("size", 200)).input(NodeInputDefinition.of("choices", PortType.GEOMETRY));
        sub(g, "blockPoints", "block_points", "blocks", in("blocks"), "size", in("size"), "mode", "faces");
        // tom de cor por quarteirao: atributo do ponto -> da instancia -> dos vertices (ao materializar)
        node(g, "toneRnd", "core.random_value");
        node(g, "tonedPoints", "core.set_attribute", "geometry", out("blockPoints", "points"), "name", "tone",
                "domain", "point", "type", "float", "value", out("toneRnd", "float"));
        node(g, "pick", "core.random_value", "min_int", 0, "max_int", 10);
        node(g, "quarter", "core.random_value", "min_int", 0, "max_int", 3);
        node(g, "yaw", "core.math", "operation", "multiply", "a", out("quarter", "int"), "b", 90.0);
        node(g, "turn", "core.combine_xyz", "y", out("yaw", "result"));
        node(g, "stretch", "core.random_value", "min_float", 0.85, "max_float", 1.25);
        node(g, "scale", "core.combine_xyz", "x", 1.0, "y", out("stretch", "float"), "z", 1.0);
        Ref[] block = position(g, "blk");
        Ref central = and(g, "central", cmp(g, "nearX", "lt", fmath(g, "absBx", "abs", block[0], 0.0), 1.0),
                cmp(g, "nearZ", "lt", fmath(g, "absBz", "abs", block[2], 0.0), 1.0));
        sub(g, "centralPark", "park", "size", 28.0, "density", 0.03);
        node(g, "centralSpot", "core.instance_on_points", "points", out("tonedPoints", "geometry"), "instance",
                out("centralPark", "geometry"), "selection", central);
        node(g, "lots", "core.instance_on_points", "points", out("tonedPoints", "geometry"), "instance", in("choices"),
                "selection", not(g, "notCentral", central), "pick_instance", true, "instance_index", out("pick", "int"),
                "rotation", out("turn", "vector"), "scale", out("scale", "vector"));
        return finish(g, join(g, "all", out("lots", "instances"), out("centralSpot", "instances")));
    }

    /** Mobiliario (poste, hidrante, lixeira) nos quatro cantos de cada quarteirao. */
    private static GraphAsset cityCornerLampsAsset() {
        GraphBuilder g = asset("city_corner_lamps");
        g.input(integer("blocks", 5)).input(num("size", 200)).input(num("block_size", 30));
        sub(g, "blockPoints", "block_points", "blocks", in("blocks"), "size", in("size"), "mode", "faces");
        sub(g, "streetLamp", "street_kit");
        sub(g, "cornerPoints", "grid_points", "size_x", math(g, "cornerSpan", "subtract", in("block_size"), 2.4),
                "size_z", out("cornerSpan", "result"), "vertices_x", 2, "vertices_z", 2);
        node(g, "cornerLamps", "core.instance_on_points", "points", out("cornerPoints", "points"), "instance", out("streetLamp", "geometry"));
        node(g, "lampCluster", "core.geometry_to_instance", "geometry", out("cornerLamps", "instances"));
        node(g, "lamps", "core.instance_on_points", "points", out("blockPoints", "points"), "instance", out("lampCluster", "instances"));
        return finish(g, out("lamps", "instances"));
    }

    /** Arvores de rua no meio de cada lado do quarteirao (entre os postes). */
    private static GraphAsset cityStreetTreesAsset() {
        GraphBuilder g = asset("city_street_trees");
        g.input(integer("blocks", 5)).input(num("size", 200)).input(num("block_size", 30));
        sub(g, "blockPoints", "block_points", "blocks", in("blocks"), "size", in("size"), "mode", "faces");
        sub(g, "sidewalkTree", "street_tree");
        Ref span = math(g, "cornerSpan", "subtract", in("block_size"), 2.4);
        sub(g, "edgePoints", "grid_points", "size_x", span, "size_z", span, "vertices_x", 3, "vertices_z", 3);
        Ref[] edge = position(g, "edge");
        Ref onEdgeX = cmp(g, "edgeNearX", "lt", fmath(g, "edgeAbsX", "abs", edge[0], 0.0), 0.1);
        Ref onEdgeZ = cmp(g, "edgeNearZ", "lt", fmath(g, "edgeAbsZ", "abs", edge[2], 0.0), 0.1);
        node(g, "edgeXor", "core.boolean_math", "operation", "xor", "a", onEdgeX, "b", onEdgeZ);
        node(g, "edgeTone", "core.random_value");
        node(g, "edgeToned", "core.set_attribute", "geometry", out("edgePoints", "points"), "name", "tone", "domain", "point", "type", "float",
                "value", out("edgeTone", "float"));
        node(g, "edgeSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "edgeScale", "core.random_value", "min_vector", new Vec3(0.85, 0.85, 0.85), "max_vector", new Vec3(1.2, 1.2, 1.2));
        node(g, "edgeTrees", "core.instance_on_points", "points", out("edgeToned", "geometry"), "instance", out("sidewalkTree", "geometry"),
                "selection", out("edgeXor", "result"), "rotation", out("edgeSpin", "vector"), "scale", out("edgeScale", "vector"));
        node(g, "edgeTreeCluster", "core.geometry_to_instance", "geometry", out("edgeTrees", "instances"));
        node(g, "treeRows", "core.instance_on_points", "points", out("blockPoints", "points"), "instance", out("edgeTreeCluster", "instances"));
        return finish(g, translate(g, "treesUp", out("treeRows", "instances"), new Vec3(0, 0.09, 0)));
    }

    /** Semaforos nos quatro cantos de cada cruzamento (os vertices da grade de quarteiroes). */
    private static GraphAsset citySignalsAsset() {
        GraphBuilder g = asset("city_signals");
        g.input(integer("blocks", 5)).input(num("size", 200));
        sub(g, "trafficSignal", "signal");
        sub(g, "signalPoints", "grid_points", "size_x", 12.6, "size_z", 12.6, "vertices_x", 2, "vertices_z", 2);
        node(g, "signalCorners", "core.instance_on_points", "points", out("signalPoints", "points"), "instance", out("trafficSignal", "geometry"));
        node(g, "signalCluster", "core.geometry_to_instance", "geometry", out("signalCorners", "instances"));
        sub(g, "crossings", "block_points", "blocks", in("blocks"), "size", in("size"), "mode", "vertices");
        node(g, "signals", "core.instance_on_points", "points", out("crossings", "points"), "instance", out("signalCluster", "instances"));
        return finish(g, translate(g, "signalsUp", out("signals", "instances"), new Vec3(0, 0.09, 0)));
    }

    // ======================================================================================================
    // house: a casa decomposta em subgrafos pequenos (cada um cabe numa tela do editor e tem poucos
    // parametros). As pecas genericas (box_part, cylinder_part, fence_rail, ...) podem ser reutilizadas.
    // Coordenadas locais: cada subgrafo monta suas pecas com numeros fixos e posiciona o conjunto uma vez so.
    // ======================================================================================================

    private static NodeInputDefinition vec3In(String name, Vec3 def) {
        return NodeInputDefinition.of(name, PortType.VEC3).withDefault(def);
    }

    /** Chamada do subgrafo {@code box_part} (caixa + posicao + material); {@code size}/{@code at} aceitam Ref ou Vec3. */
    private static Ref bp(GraphBuilder g, String id, Object size, Object at, String material, double bevel) {
        sub(g, id, "box_part", "size", size, "position", at, "material", material, "bevel", bevel);
        return out(id, "geometry");
    }

    /** Chamada do subgrafo {@code cylinder_part} (cilindro de eixo Y com base em y = 0). */
    private static Ref cp(GraphBuilder g, String id, double radius, double height, int segments, String material, Object at) {
        sub(g, id, "cylinder_part", "radius", radius, "height", height, "segments", segments, "position", at, "material", material);
        return out(id, "geometry");
    }

    /** Une instancias/geometria e devolve como GEOMETRY (saida publica de um subgrafo). */
    private static GraphAsset finish(GraphBuilder g, Ref result) {
        output(g, "geometry", PortType.GEOMETRY, result);
        return g.build();
    }

    /** Caixa centrada em {@code position}, com material; so e suavizada (normais misturadas) quando {@code bevel} &gt; 0. */
    private static GraphAsset boxPartAsset() {
        GraphBuilder g = asset("box_part");
        g.input(vec3In("size", new Vec3(1, 1, 1))).input(vec3In("position", new Vec3(0, 0, 0)))
                .input(material("material", "wall")).input(num("bevel", 0.0));
        node(g, "box", "core.create_box", "size", in("size"), "bevel", in("bevel"), "bevel_segments", 1);
        node(g, "smooth", "core.set_attribute", "geometry", out("box", "mesh"), "name", "smooth", "domain", "face", "type", "bool",
                "flag", cmp(g, "rounded", "gt", in("bevel"), 0.0));
        Ref at = translate(g, "at", out("smooth", "geometry"), in("position"));
        node(g, "mat", "core.set_material", "geometry", at, "material", in("material"));
        return finish(g, out("mat", "geometry"));
    }

    /** Cilindro de eixo Y com a base em {@code position} (create_cylinder e centrado: sobe metade da altura), lateral suave. */
    private static GraphAsset cylinderPartAsset() {
        GraphBuilder g = asset("cylinder_part");
        g.input(num("radius", 0.5)).input(num("height", 1.0)).input(integer("segments", 8))
                .input(vec3In("position", new Vec3(0, 0, 0))).input(material("material", "wall"));
        node(g, "cyl", "core.create_cylinder", "radius", in("radius"), "height", in("height"), "segments", in("segments"));
        node(g, "half", "core.combine_vec3_transform", "y", in("height"), "scale", new Vec3(1, 0.5, 1));
        Ref raised = translate(g, "up", out("cyl", "mesh"), out("half", "vector"));
        node(g, "mat", "core.set_material", "geometry", raised, "material", in("material"));
        return finish(g, translate(g, "at", out("mat", "geometry"), in("position")));
    }

    /** Coluna (base, fuste e capitel), com a base em {@code position}. */
    private static GraphAsset houseColumnAsset() {
        GraphBuilder g = asset("house_column");
        g.input(vec3In("position", new Vec3(0, 0, 0)));
        Ref base = cp(g, "base", 0.17, 0.2, 8, "trim", new Vec3(0, 0, 0));
        Ref shaft = cp(g, "shaft", 0.11, 2.2, 8, "trim", new Vec3(0, 0.2, 0));
        Ref capital = cp(g, "capital", 0.17, 0.2, 8, "trim", new Vec3(0, 2.4, 0));
        return finish(g, translate(g, "at", join(g, "column", base, shaft, capital), in("position")));
    }

    /** Janela redonda do oitao (moldura, vidro e travessa), deitada para olhar em +Z. */
    private static GraphAsset houseGableWindowAsset() {
        GraphBuilder g = asset("house_gable_window");
        Ref frame = cp(g, "frame", 0.58, 0.06, 14, "trim", new Vec3(0, 0, 0));
        Ref glass = cp(g, "glass", 0.42, 0.06, 14, "window_dark", new Vec3(0, 0.03, 0));
        Ref bar = bp(g, "bar", new Vec3(0.8, 0.05, 0.1), new Vec3(0, 0.06, 0), "trim", 0.0);
        return finish(g, rotate(g, "turn", join(g, "all", frame, glass, bar), new Vec3(90, 0, 0)));
    }

    /** Casco do telhado de 2 aguas: caixa cujos vertices de cima vao ao eixo; as aguas recebem o material "roof". */
    private static GraphAsset houseRoofShellAsset() {
        GraphBuilder g = asset("house_roof_shell");
        g.input(num("roof_w", 9.8)).input(num("roof_d", 6.8)).input(num("roof_h", 4.9)).input(num("h", 5.6));
        Ref roofW = in("roof_w"), roofD = in("roof_d"), roofH = in("roof_h"), h = in("h");
        node(g, "roofBox", "core.create_box", "size", vec(g, "roofSize", roofW, roofH, roofD));
        Ref[] p = position(g, "r");
        Ref upper = cmp(g, "upper", "gt", p[1], 0.0);
        node(g, "pull", "core.switch_float", "condition", upper, "if_true", fmath(g, "negX", "negate", p[0], 0.0), "if_false", 0.0);
        node(g, "pullVec", "core.combine_xyz", "x", out("pull", "result"));
        node(g, "ridge", "core.set_position", "geometry", out("roofBox", "mesh"), "offset", out("pullVec", "vector"));
        node(g, "clean", "core.merge_by_distance", "geometry", out("ridge", "geometry"), "distance", 0.01);
        Ref roofUp = translate(g, "roofUp", out("clean", "geometry"),
                vec(g, "roofAt", 0.0, math(g, "roofY", "add", h, math(g, "halfRoof", "multiply", roofH, 0.5)), 0.0));
        Ref[] n = normal(g, "r");
        node(g, "roof", "core.set_material", "geometry", roofUp, "selection", cmp(g, "slope", "gt", n[1], 0.3), "material", "roof");
        return finish(g, out("roof", "geometry"));
    }

    /** Telhas: uma grade por agua, cada celula com inset + extrude de altura sorteada em [tile_min, tile_max]. */
    private static GraphAsset houseRoofTilesAsset() {
        GraphBuilder g = asset("house_roof_tiles");
        g.input(num("roof_w", 9.8)).input(num("roof_d", 6.8)).input(num("roof_h", 4.9)).input(num("h", 5.6))
                .input(num("tile_min", 0.03)).input(num("tile_max", 0.085));
        Ref roofW = in("roof_w"), roofD = in("roof_d"), roofH = in("roof_h"), h = in("h");
        Ref slopeLen = math(g, "slopeLen", "multiply", roofW, 0.70711);
        sub(g, "tileRows", "step_count", "length", slopeLen, "step", 0.42);
        sub(g, "tileCols", "step_count", "length", roofD, "step", 0.5);
        node(g, "tileGrid", "core.create_grid", "size_x", slopeLen, "size_z", roofD,
                "vertices_x", out("tileRows", "result"), "vertices_z", out("tileCols", "result"));
        node(g, "tileFrame", "core.inset_faces", "geometry", out("tileGrid", "mesh"), "amount", 0.03, "inner_attribute", "tile_inner");
        node(g, "tileHeight", "core.random_value", "min_float", in("tile_min"), "max_float", in("tile_max"));
        node(g, "tileRaise", "core.extrude", "geometry", out("tileFrame", "geometry"), "selection", flag(g, "tileInner", "tile_inner"),
                "distance", out("tileHeight", "float"));
        node(g, "tilePaint", "core.set_material", "geometry", out("tileRaise", "geometry"), "material", "tile");
        Ref slopeMidY = math(g, "slopeMidY", "add", h, math(g, "slopeMidHalf", "multiply", roofH, 0.5));
        Ref slopeX = math(g, "slopeX", "multiply", roofW, 0.25);
        Ref tilesRight = place(g, "tilesRightAt", out("tilePaint", "geometry"), vec(g, "tilesRightPos", math(g, "tilesRightX", "add", slopeX, 0.014), math(g, "tilesRightY", "add", slopeMidY, 0.014), 0.0), new Vec3(0, 0, -45));
        return finish(g, mirror(g, "tiles", tilesRight, "x"));
    }

    /** Cumeeira, tabeiras nos oitoes e janelas redondas dos oitoes. */
    private static GraphAsset houseRoofTrimAsset() {
        GraphBuilder g = asset("house_roof_trim");
        g.input(num("roof_w", 9.8)).input(num("roof_d", 6.8)).input(num("roof_h", 4.9)).input(num("h", 5.6));
        Ref roofW = in("roof_w"), roofD = in("roof_d"), roofH = in("roof_h"), h = in("h");
        Ref slopeLen = math(g, "slopeLen", "multiply", roofW, 0.70711);
        Ref slopeMidY = math(g, "slopeMidY", "add", h, math(g, "slopeMidHalf", "multiply", roofH, 0.5));
        Ref slopeX = math(g, "slopeX", "multiply", roofW, 0.25);
        Ref ridgeY = math(g, "ridgeY", "add", h, roofH);
        Ref ridgeCap = place(g, "ridgeCapAt", bp(g, "ridgeCap", vec(g, "ridgeSize", 0.36, 0.36, math(g, "ridgeLen", "add", roofD, 0.2)), new Vec3(0, 0, 0), "trim", 0.0), vec(g, "ridgeCapPos", 0.0, ridgeY, 0.0), new Vec3(0, 0, 45));
        Ref board = bp(g, "board", vec(g, "boardSize", math(g, "boardLen", "add", slopeLen, 0.25), 0.1, 0.1), new Vec3(0, 0, 0), "trim", 0.0);
        Ref boardR = rotate(g, "boardRTurn", board, new Vec3(0, 0, -45));
        Ref boardOffset = math(g, "boardOffset", "add", slopeX, 0.1);
        Ref boardZ = math(g, "boardZ", "multiply", roofD, 0.5);
        Ref boardRF = translate(g, "boardRF", boardR, vec(g, "boardRFAt", boardOffset, slopeMidY, boardZ));
        sub(g, "gableWindow", "house_gable_window");
        Ref gableY = math(g, "gableY", "add", h, math(g, "gableRise", "multiply", roofH, 0.38));
        Ref gableFront = translate(g, "gableFrontAt", out("gableWindow", "geometry"),
                vec(g, "gableFrontPos", 0.0, gableY, math(g, "gableFrontZ", "add", boardZ, 0.02)));
        // tabeira da direita e janela da frente; o outro lado da agua e os fundos saem dos espelhos em X e em Z
        Ref frontSide = mirror(g, "boardsLR", join(g, "frontRight", boardRF), "x");
        Ref front = join(g, "front", frontSide, gableFront);
        return finish(g, join(g, "trim", ridgeCap, mirror(g, "frontBack", front, "z")));
    }

    /** Uma lucarna por agua, a 30% da largura do telhado. */
    private static GraphAsset houseDormersAsset() {
        GraphBuilder g = asset("house_dormers");
        g.input(num("roof_w", 9.8)).input(num("roof_d", 6.8)).input(num("h", 5.6));
        Ref roofW = in("roof_w"), roofD = in("roof_d"), h = in("h");
        sub(g, "dormerRight", "dormer");
        Ref dormerX = math(g, "dormerX", "multiply", roofW, 0.3);
        Ref dormerY = math(g, "dormerY", "add", h, math(g, "dormerDrop", "subtract", math(g, "dormerHalf", "multiply", roofW, 0.5), dormerX));
        Ref dormerZ = math(g, "dormerZ", "multiply", roofD, 0.2);
        Ref dormerRight = translate(g, "dormerRightAt", out("dormerRight", "geometry"), vec(g, "dormerRightPos", dormerX, dormerY, dormerZ));
        return finish(g, join(g, "dormers", dormerRight, rotate(g, "dormerLeftTurn", dormerRight, new Vec3(0, 180, 0))));
    }

    /** Chamine montada na origem (corpo, faixa, tampa e dois potes) e posicionada uma vez no telhado. */
    private static GraphAsset houseChimneyAsset() {
        GraphBuilder g = asset("house_chimney");
        g.input(num("w", 9.0)).input(num("d", 6.0)).input(num("h", 5.6)).input(num("roof_h", 4.9));
        Ref body = bp(g, "body", new Vec3(0.8, 2.8, 0.8), new Vec3(0, 0, 0), "brick", 0.1);
        Ref band = bp(g, "band", new Vec3(0.92, 0.12, 0.92), new Vec3(0, 0.9, 0), "trim", 0.0);
        Ref cap = bp(g, "cap", new Vec3(1.05, 0.18, 1.05), new Vec3(0, 1.4, 0), "trim", 0.0);
        Ref potA = cp(g, "potA", 0.15, 0.45, 8, "brick", new Vec3(-0.2, 1.49, 0));
        Ref potB = cp(g, "potB", 0.15, 0.45, 8, "brick", new Vec3(0.2, 1.49, 0));
        Ref x = math(g, "x", "multiply", in("w"), 0.22);
        Ref z = math(g, "z", "multiply", in("d"), -0.2);
        Ref y = math(g, "y", "add", in("h"), math(g, "rise", "multiply", in("roof_h"), 0.8));
        return finish(g, translate(g, "placed", join(g, "all", body, band, cap, potA, potB), vec(g, "pos", x, y, z)));
    }

    /** Porta com bandeira, requadro, verga, macaneta e luminaria; {@code front_z} = z da parede da frente. */
    private static GraphAsset houseDoorAsset() {
        GraphBuilder g = asset("house_door");
        g.input(num("front_z", 3.0));
        Ref door = bp(g, "door", new Vec3(1.2, 2.2, 0.15), new Vec3(0, 1.1, 0.05), "door", 0.0);
        Ref light = bp(g, "doorLight", new Vec3(0.7, 0.8, 0.04), new Vec3(0, 1.55, 0.14), "window_dark", 0.0);
        Ref knob = bp(g, "doorKnob", new Vec3(0.09, 0.09, 0.1), new Vec3(0.42, 1.0, 0.17), "lamp", 0.0);
        Ref frame = bp(g, "frame", new Vec3(1.7, 2.45, 0.1), new Vec3(0, 1.225, 0.02), "trim", 0.0);
        Ref lintel = bp(g, "lintel", new Vec3(1.95, 0.2, 0.26), new Vec3(0, 2.55, 0.1), "trim", 0.0);
        Ref lamp = bp(g, "wallLamp", new Vec3(0.18, 0.3, 0.18), new Vec3(-1.2, 1.9, 0.1), "lamp", 0.0);
        return finish(g, translate(g, "placed", join(g, "all", door, light, knob, frame, lintel, lamp), vec(g, "at", 0.0, 0.0, in("front_z"))));
    }

    /** Balaustrada: 13 balaustres numa linha, com um vao central livre (|x| > 0.85 fica fechado). */
    private static GraphAsset houseBalustradeAsset() {
        GraphBuilder g = asset("house_balustrade");
        Ref pts = linePoints(g, "pts", new Vec3(-1.5, 0.25, 1.6), new Vec3(1.5, 0.25, 1.6), 13);
        Ref[] bp = position(g, "bal");
        Ref open = cmp(g, "open", "gt", fmath(g, "abs", "abs", bp[0], 0.0), 0.85);
        Ref baluster = bp(g, "baluster", new Vec3(0.05, 0.75, 0.05), new Vec3(0, 0.375, 0), "trim", 0.0);
        node(g, "balusters", "core.instance_on_points", "points", pts, "instance", baluster, "selection", open);
        return finish(g, join(g, "out", out("balusters", "instances")));
    }

    /** Varanda: laje, degrau, colunas, balaustrada, corrimaos e telhadinho. */
    private static GraphAsset housePorchAsset() {
        GraphBuilder g = asset("house_porch");
        g.input(num("front_z", 3.0));
        Ref slab = bp(g, "porch", new Vec3(3.8, 0.25, 1.8), new Vec3(0, 0.125, 0.9), "trim", 0.1);
        Ref step = bp(g, "step", new Vec3(3.2, 0.14, 0.5), new Vec3(0, 0.07, 2.05), "sidewalk", 0.0);
        sub(g, "colR", "house_column", "position", new Vec3(1.6, 0.25, 1.6));
        sub(g, "balustrade", "house_balustrade");
        Ref railR = bp(g, "handrailR", new Vec3(0.65, 0.07, 0.09), new Vec3(1.275, 1.05, 1.6), "trim", 0.0);
        Ref sidePair = mirror(g, "sidePair", join(g, "rightSide", out("colR", "geometry"), railR), "x");
        Ref roof = bp(g, "roof", new Vec3(4.2, 0.18, 2.2), new Vec3(0, 2.95, 0.95), "roof", 0.0);
        Ref fascia = bp(g, "fascia", new Vec3(4.3, 0.2, 0.08), new Vec3(0, 2.88, 2.07), "trim", 0.0);
        Ref ceiling = bp(g, "ceiling", new Vec3(3.9, 0.05, 1.9), new Vec3(0, 2.82, 0.9), "trim", 0.0);
        Ref all = join(g, "all", slab, step, sidePair, out("balustrade", "geometry"), roof, fascia, ceiling);
        return finish(g, translate(g, "placed", all, vec(g, "at", 0.0, 0.0, in("front_z"))));
    }

    /** Caminho de 4 lajotas da varanda ate o portao. */
    private static GraphAsset houseWalkwayAsset() {
        GraphBuilder g = asset("house_walkway");
        g.input(num("front_z", 3.0));
        Ref pts = linePoints(g, "pts", new Vec3(0, 0.05, 2.7), new Vec3(0, 0.05, 3.7), 4);
        Ref stone = bp(g, "flagstone", new Vec3(1.1, 0.07, 0.5), new Vec3(0, 0.035, 0), "sidewalk", 0.0);
        node(g, "flagstones", "core.instance_on_points", "points", pts, "instance", stone);
        return finish(g, translate(g, "placed", out("flagstones", "instances"), vec(g, "at", 0.0, 0.0, in("front_z"))));
    }

    /** Quintal: gramado com mureta de pedra (o anel do inset sobe). */
    private static GraphAsset houseYardAsset() {
        GraphBuilder g = asset("house_yard");
        g.input(num("yard_w", 13.0)).input(num("yard_d", 11.0));
        node(g, "yard", "core.create_grid", "size_x", in("yard_w"), "size_z", in("yard_d"));
        Ref yardUp = translate(g, "yardUp", out("yard", "mesh"), new Vec3(0, 0.05, 1.0));
        node(g, "yardFrame", "core.inset_faces", "geometry", yardUp, "amount", 0.18, "inner_attribute", "yard_inner");
        node(g, "curbUp", "core.extrude", "geometry", out("yardFrame", "geometry"),
                "selection", not(g, "notYardInner", flag(g, "yardInner", "yard_inner")), "offset", new Vec3(0, 0.2, 0));
        Ref[] yp = position(g, "y");
        node(g, "curbPaint", "core.set_material", "geometry", out("curbUp", "geometry"), "selection", cmp(g, "curbHigh", "gt", yp[1], 0.08),
                "material", "stone");
        node(g, "lawnPaint", "core.set_material", "geometry", out("curbPaint", "geometry"), "selection", cmp(g, "lawnLow", "lt", yp[1], 0.08),
                "material", "grass");
        return finish(g, out("lawnPaint", "geometry"));
    }

    /** Travessa da cerca: duas barras (y = 0.3 e 0.75) ao longo de X, giradas em {@code yaw} e movidas para (x, z). */
    private static GraphAsset fenceRailAsset() {
        GraphBuilder g = asset("fence_rail");
        g.input(num("length", 4.0)).input(num("yaw", 0.0)).input(num("x", 0.0)).input(num("z", 0.0));
        Ref low = bp(g, "low", vec(g, "size", in("length"), 0.05, 0.05), new Vec3(0, 0.3, 0), "fence", 0.0);
        Ref high = translate(g, "high", low, new Vec3(0, 0.45, 0));
        return finish(g, place(g, "placed", join(g, "both", low, high), vec(g, "at", in("x"), 0.0, in("z")), vec(g, "yawVec", 0.0, in("yaw"), 0.0)));
    }

    /** Ripas no perimetro da cerca (menos o vao do portao), uma instancia por ponto de uma grade. */
    private static GraphAsset fencePicketsAsset() {
        GraphBuilder g = asset("fence_pickets");
        g.input(num("fence_w", 12.7)).input(num("fence_d", 10.7));
        Ref fenceW = in("fence_w"), fenceD = in("fence_d");
        sub(g, "picketCols", "step_count", "length", fenceW, "step", 0.45);
        sub(g, "picketRows", "step_count", "length", fenceD, "step", 0.45);
        sub(g, "picketPts", "grid_points", "size_x", fenceW, "size_z", fenceD,
                "vertices_x", out("picketCols", "result"), "vertices_z", out("picketRows", "result"));
        Ref[] fp = position(g, "fence");
        Ref halfFenceW = math(g, "halfFenceW", "multiply", fenceW, 0.5);
        Ref halfFenceD = math(g, "halfFenceD", "multiply", fenceD, 0.5);
        Ref onSide = cmp(g, "onSide", "gt", fmath(g, "fenceAbsX", "abs", fp[0], 0.0), math(g, "sideLimit", "subtract", halfFenceW, 0.05));
        Ref onEnd = cmp(g, "onEnd", "gt", fmath(g, "fenceAbsZ", "abs", fp[2], 0.0), math(g, "endLimit", "subtract", halfFenceD, 0.05));
        Ref inGate = and(g, "inGate", cmp(g, "gateFront", "gt", fp[2], math(g, "gateLimit", "subtract", halfFenceD, 0.05)),
                cmp(g, "gateNear", "lt", fmath(g, "gateAbsX", "abs", fp[0], 0.0), 0.95));
        node(g, "perimeter", "core.boolean_math", "operation", "or", "a", onSide, "b", onEnd);
        Ref picketHere = and(g, "picketHere", out("perimeter", "result"), not(g, "notGate", inGate));
        Ref picket = bp(g, "picket", new Vec3(0.07, 0.95, 0.05), new Vec3(0, 0.475, 0), "fence", 0.0);
        Ref tip = bp(g, "picketTip", new Vec3(0.1, 0.05, 0.08), new Vec3(0, 0.97, 0), "fence", 0.0);
        node(g, "pickets", "core.instance_on_points", "points", out("picketPts", "points"), "instance", join(g, "picketAll", picket, tip),
                "selection", picketHere);
        return finish(g, join(g, "out", out("pickets", "instances")));
    }

    /** Cinco travessas: fundo, lados e as duas metades da frente (separadas pelo vao do portao). */
    private static GraphAsset fenceRailsAsset() {
        GraphBuilder g = asset("fence_rails");
        g.input(num("fence_w", 12.7)).input(num("fence_d", 10.7));
        Ref fenceW = in("fence_w"), fenceD = in("fence_d");
        Ref halfW = math(g, "halfFenceW", "multiply", fenceW, 0.5);
        Ref halfD = math(g, "halfFenceD", "multiply", fenceD, 0.5);
        Ref frontLen = math(g, "frontLen", "subtract", halfW, 0.95);
        Ref frontMid = math(g, "frontMid", "multiply", math(g, "frontSum", "add", halfW, 0.95), 0.5);
        sub(g, "back", "fence_rail", "length", fenceW, "yaw", 0.0, "x", 0.0, "z", math(g, "backZ", "multiply", halfD, -1.0));
        sub(g, "right", "fence_rail", "length", fenceD, "yaw", 90.0, "x", halfW, "z", 0.0);
        sub(g, "frontR", "fence_rail", "length", frontLen, "yaw", 0.0, "x", frontMid, "z", halfD);
        // lado esquerdo e metade esquerda da frente: espelho em X (o fundo e simetrico, entra como esta)
        Ref sides = mirror(g, "sides", join(g, "rightSide", out("right", "geometry"), out("frontR", "geometry")), "x");
        return finish(g, join(g, "rails", out("back", "geometry"), sides));
    }

    /** Postes de canto e os dois postes do portao. */
    private static GraphAsset fencePostsAsset() {
        GraphBuilder g = asset("fence_posts");
        g.input(num("fence_w", 12.7)).input(num("fence_d", 10.7));
        sub(g, "cornerPts", "grid_points", "size_x", in("fence_w"), "size_z", in("fence_d"), "vertices_x", 2, "vertices_z", 2);
        Ref post = join(g, "postAll", bp(g, "post", new Vec3(0.16, 1.25, 0.16), new Vec3(0, 0.625, 0), "fence", 0.0),
                bp(g, "postCap", new Vec3(0.22, 0.06, 0.22), new Vec3(0, 1.28, 0), "fence", 0.0));
        node(g, "postSet", "core.instance_on_points", "points", out("cornerPts", "points"), "instance", post);
        Ref halfD = math(g, "halfFenceD", "multiply", in("fence_d"), 0.5);
        Ref gates = mirror(g, "gatePosts", translate(g, "gatePostR", post, vec(g, "gatePostRPos", 1.0, 0.0, halfD)), "x");
        return finish(g, join(g, "posts", out("postSet", "instances"), gates));
    }

    /** Cerca de ripas: ripas + travessas + postes, assentada no quintal. */
    private static GraphAsset houseFenceAsset() {
        GraphBuilder g = asset("house_fence");
        g.input(num("fence_w", 12.7)).input(num("fence_d", 10.7));
        sub(g, "pickets", "fence_pickets", "fence_w", in("fence_w"), "fence_d", in("fence_d"));
        sub(g, "rails", "fence_rails", "fence_w", in("fence_w"), "fence_d", in("fence_d"));
        sub(g, "posts", "fence_posts", "fence_w", in("fence_w"), "fence_d", in("fence_d"));
        Ref all = join(g, "fence", out("pickets", "geometry"), out("rails", "geometry"), out("posts", "geometry"));
        return finish(g, translate(g, "placed", all, new Vec3(0, 0.25, 1.0)));
    }

    /** Sebe com canteiro e duas flores (uma unidade, centrada no ponto). */
    private static GraphAsset hedgeUnitAsset() {
        GraphBuilder g = asset("hedge_unit");
        Ref hedge = bp(g, "hedge", new Vec3(1.3, 0.8, 0.9), new Vec3(0, 0.4, 0), "leaves", 0.28);
        Ref bed = bp(g, "bed", new Vec3(1.1, 0.14, 0.6), new Vec3(0, 0.07, 0.95), "trunk", 0.05);
        Ref bloomA = bp(g, "bloomA", new Vec3(0.3, 0.2, 0.3), new Vec3(-0.3, 0.2, 0.95), "flowers", 0.12);
        Ref bloomB = bp(g, "bloomB", new Vec3(0.3, 0.26, 0.3), new Vec3(0.3, 0.2, 0.95), "flowers", 0.12);
        return finish(g, join(g, "unit", hedge, bed, bloomA, bloomB));
    }

    /** Fileira de 7 sebes na frente da casa, com o vao central livre para o caminho. */
    private static GraphAsset houseHedgesAsset() {
        GraphBuilder g = asset("house_hedges");
        g.input(num("w", 9.0)).input(num("front_z", 3.0));
        Ref half = math(g, "hedgeHalf", "add", math(g, "hedgeW2", "multiply", in("w"), 0.5), 1.2);
        Ref pts = linePoints(g, "pts", vec(g, "hedgeA", math(g, "hedgeNeg", "multiply", half, -1.0), 0.0, 0.8),
                vec(g, "hedgeB", half, 0.0, 0.8), 7);
        Ref[] hp = position(g, "hedge");
        Ref open = cmp(g, "hedgeOpen", "gt", fmath(g, "hedgeAbs", "abs", hp[0], 0.0), 2.4);
        sub(g, "unit", "hedge_unit");
        node(g, "hedges", "core.instance_on_points", "points", pts, "instance", out("unit", "geometry"), "selection", open);
        return finish(g, translate(g, "placed", out("hedges", "instances"), vec(g, "at", 0.0, 0.0, in("front_z"))));
    }

    /** Caixa de correio perto do portao. */
    private static GraphAsset houseMailboxAsset() {
        GraphBuilder g = asset("house_mailbox");
        g.input(num("front_z", 3.0));
        Ref post = bp(g, "mailPost", new Vec3(0.1, 1.1, 0.1), new Vec3(1.5, 0.55, 3.15), "trunk", 0.0);
        Ref box = bp(g, "mailBox", new Vec3(0.3, 0.25, 0.5), new Vec3(1.5, 1.2, 3.15), "metal", 0.0);
        return finish(g, translate(g, "placed", join(g, "all", post, box), vec(g, "at", 0.0, 0.0, in("front_z"))));
    }

    /** Arvores no fundo do quintal: {@code count} pontos sorteados, cada um pinheiro ou carvalho, com giro aleatorio. */
    private static GraphAsset houseBackTreesAsset() {
        GraphBuilder g = asset("house_back_trees");
        g.input(num("w", 9.0)).input(num("front_z", 3.0)).input(integer("count", 2));
        node(g, "backYard", "core.create_grid", "size_x", math(g, "backW", "add", in("w"), 1.0), "size_z", 1.0);
        Ref backUp = translate(g, "backUp", out("backYard", "mesh"),
                vec(g, "backAt", 0.0, 0.05, math(g, "backZ", "multiply", in("front_z"), -1.0)));
        node(g, "backScatter", "core.distribute_points", "geometry", backUp, "mode", "count", "count", in("count"));
        sub(g, "shrubPine", "tree");
        sub(g, "shrubOak", "broadleaf", "radius", 1.9, "squash", 0.9, "trunk", 1.8);
        node(g, "shrubPineI", "core.geometry_to_instance", "geometry", out("shrubPine", "geometry"));
        node(g, "shrubOakI", "core.geometry_to_instance", "geometry", out("shrubOak", "geometry"));
        Ref kinds = join(g, "shrubKinds", out("shrubPineI", "instances"), out("shrubOakI", "instances"));
        node(g, "shrubPick", "core.random_value", "min_int", 0, "max_int", 1);
        node(g, "shrubSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "shrubs", "core.instance_on_points", "points", out("backScatter", "points"), "instance", kinds, "pick_instance", true,
                "instance_index", out("shrubPick", "int"), "rotation", out("shrubSpin", "vector"), "scale", new Vec3(0.5, 0.5, 0.5));
        return finish(g, join(g, "out", out("shrubs", "instances")));
    }

    /**
     * Casa completa composta so de subgrafos: calcula as medidas derivadas do corpo (telhado, quintal, cerca) e liga uma
     * chamada por parte (paredes com janelas, telhado, telhas, cumeeira e tabeiras, lucarnas, chamine, porta, varanda,
     * caminho, quintal, cerca, sebes, caixa de correio e arvores do fundo), com tom de cor proprio.
     */
    private static GraphAsset houseAsset() {
        GraphBuilder g = asset("house");
        g.input(integer("cols_x", 2)).input(integer("cols_z", 2)).input(num("chance", 0.6));
        sub(g, "body", "tower_body", "cols_x", in("cols_x"), "cols_z", in("cols_z"), "floors", 2, "cell", 3.0,
                "floor_h", 2.8, "chance", in("chance"), "ground_zone", 0.0, "margin", 0.55, "balconies", 0.0,
                "cottage", true, "pilasters", false, "plinth", 0.5);
        Ref w = out("body", "width"), d = out("body", "depth"), h = out("body", "height");
        Ref front = math(g, "frontZ", "multiply", d, 0.5);
        Ref roofW = math(g, "roofW", "add", w, 0.8);
        Ref roofD = math(g, "roofD", "add", d, 0.8);
        Ref roofH = math(g, "roofH", "multiply", roofW, 0.5);
        Ref yardW = math(g, "yardW", "add", w, 4.0);
        Ref yardD = math(g, "yardD", "add", d, 5.0);
        Ref fenceW = math(g, "fenceW", "subtract", yardW, 0.3);
        Ref fenceD = math(g, "fenceD", "subtract", yardD, 0.3);
        sub(g, "roofShell", "house_roof_shell", "roof_w", roofW, "roof_d", roofD, "roof_h", roofH, "h", h);
        sub(g, "roofTiles", "house_roof_tiles", "roof_w", roofW, "roof_d", roofD, "roof_h", roofH, "h", h);
        sub(g, "roofTrim", "house_roof_trim", "roof_w", roofW, "roof_d", roofD, "roof_h", roofH, "h", h);
        sub(g, "dormers", "house_dormers", "roof_w", roofW, "roof_d", roofD, "h", h);
        sub(g, "chimney", "house_chimney", "w", w, "d", d, "h", h, "roof_h", roofH);
        sub(g, "door", "house_door", "front_z", front);
        sub(g, "porch", "house_porch", "front_z", front);
        sub(g, "walkway", "house_walkway", "front_z", front);
        sub(g, "yard", "house_yard", "yard_w", yardW, "yard_d", yardD);
        sub(g, "fence", "house_fence", "fence_w", fenceW, "fence_d", fenceD);
        sub(g, "hedges", "house_hedges", "w", w, "front_z", front);
        sub(g, "mailbox", "house_mailbox", "front_z", front);
        sub(g, "backTrees", "house_back_trees", "w", w, "front_z", front);
        Ref built = join(g, "built", out("body", "geometry"), out("roofShell", "geometry"), out("roofTiles", "geometry"),
                out("roofTrim", "geometry"), out("dormers", "geometry"), out("chimney", "geometry"), out("door", "geometry"),
                out("porch", "geometry"), out("walkway", "geometry"), out("yard", "geometry"), out("fence", "geometry"),
                out("hedges", "geometry"), out("mailbox", "geometry"), out("backTrees", "geometry"));
        // tom de cor proprio da casa (id constante = o mesmo valor para todos os vertices)
        node(g, "toneRnd", "core.random_value", "id", 0);
        node(g, "tinted", "core.set_attribute", "geometry", built, "name", "tone", "domain", "point", "type", "float",
                "value", out("toneRnd", "float"));
        return finish(g, out("tinted", "geometry"));
    }
}
