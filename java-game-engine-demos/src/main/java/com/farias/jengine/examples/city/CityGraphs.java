package com.farias.jengine.examples.city;

import com.farias.procgeo.core.math.Vec3;
import com.farias.procgeo.graph.GraphAsset;
import com.farias.procgeo.graph.GraphBuilder;
import com.farias.procgeo.graph.GraphInterface;
import com.farias.procgeo.graph.GraphLibrary;
import com.farias.procgeo.graph.GraphRef;
import com.farias.procgeo.graph.NodeInputDefinition;
import com.farias.procgeo.graph.PortType;

/**
 * Os grafos da cidade, todos compostos so de nos genericos da biblioteca
 * procedural-geometry (o nucleo da lib nao conhece "cidade" nem "predio").
 * Cada asset e um subgrafo reutilizavel com interface publica:
 *
 * <pre>
 * windows       janelas (inset + material + recuo) e vitrines no terreo, por Fields sobre normais/posicoes
 * facade        grade vertical (cols x rows) com janelas
 * tower_body    4 fachadas + pilares de canto (instancias) + friso
 * tower         corpo + laje com parapeito (inset + extrude) + casa de maquinas, antena e aparelhos instanciados
 * spire         piramide/cone (extrude + set_position puxando o topo ao eixo)
 * stepped_tower torre em recuos coroada por spire
 * round_tower   cilindro (circulo varrido por perfil vertical) + frisos + telhado conico
 * house         paredes + telhado de 2 aguas + chamine + varanda + porta + quintal com cerca e arvores
 * house_block   quatro casas por quarteirao
 * courtyard     quatro blocos em volta de um patio
 * tree, park    arvore; parque com caminhos, lago, bancos e arvores espalhadas
 * lamp, car     poste e carro (carro usa instancias para as rodas)
 * street, road  faixa de rua; faixa com tracejado e carros
 * *_grid        ruas nas duas direcoes (instancias, com variantes sorteadas)
 * city          terreno + calcadas + ruas + postes + um lote variado por quarteirao
 * </pre>
 */
final class CityGraphs {

    private CityGraphs() {
    }

    static GraphLibrary library() {
        return new GraphLibrary()
                .add(windows()).add(facade()).add(towerBody()).add(tower()).add(spire()).add(steppedTower())
                .add(roundTower()).add(house()).add(houseBlock()).add(courtyard()).add(tree()).add(park())
                .add(lamp()).add(car()).add(street()).add(road()).add(sidewalkGrid()).add(roadGrid()).add(city());
    }

    // ---- helpers para escrever grafos de forma enxuta ----

    /** Porta de saida de um no ({@code GraphInterface.INPUT_NODE} = entrada publica do grafo). */
    private record Ref(String node, String port) {
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

    private static void wire(GraphBuilder g, String id, GraphBuilder.NodeHandle handle, Object[] kv) {
        for (int i = 0; i < kv.length; i += 2) {
            String port = (String) kv[i];
            if (kv[i + 1] instanceof Ref r) {
                g.connect(r.node(), r.port(), id, port);
            } else {
                handle.set(port, kv[i + 1]);
            }
        }
    }

    /** Aritmetica de valores (FLOAT). */
    private static Ref math(GraphBuilder g, String id, String op, Object a, Object b) {
        node(g, id, "core.scalar_math", "operation", op, "a", a, "b", b);
        return out(id, "result");
    }

    /** Aritmetica de Fields (por elemento). */
    private static Ref fmath(GraphBuilder g, String id, String op, Object a, Object b) {
        node(g, id, "core.math", "operation", op, "a", a, "b", b);
        return out(id, "result");
    }

    private static Ref vec(GraphBuilder g, String id, Object x, Object y, Object z) {
        node(g, id, "core.combine_vec3", "x", x, "y", y, "z", z);
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
    private static Ref flag(GraphBuilder g, String id, String name) {
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

    /** Caixa com material, centrada em {@code at}. */
    private static Ref part(GraphBuilder g, String id, Object size, Object at, String material) {
        node(g, id + "_box", "core.create_box", "size", size);
        node(g, id + "_at", "core.translate", "geometry", out(id + "_box", "mesh"), "offset", at);
        node(g, id, "core.set_material", "geometry", out(id + "_at", "geometry"), "material", material);
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
        g.output(name, type, from.node(), from.port());
    }

    /** Junta as saidas de {@code parts} num no novo {@code id} (entrada multi do Join). */
    private static Ref join(GraphBuilder g, String id, Ref... parts) {
        node(g, id, "core.join_geometry");
        for (Ref p : parts) {
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

    // ================= predios =================

    /**
     * Janelas: paredes (|normal.y| &lt; 0.3) acima do terreo, sorteadas, ganham moldura (inset), material e recuo.
     * Terreo (abaixo de "ground"): vitrines sorteadas, com o mesmo metodo.
     */
    private static GraphAsset windows() {
        GraphBuilder g = asset("windows");
        g.input(NodeInputDefinition.of("geometry", PortType.GEOMETRY)).input(num("chance", 0.5)).input(num("ground", 3.5))
                .input(num("depth", 0.3)).input(num("margin", 0.35));

        Ref[] n = normal(g, "w");
        Ref nAbs = fmath(g, "nAbs", "abs", n[1], 0.0);
        Ref isWall = cmp(g, "isWall", "lt", nAbs, 0.3);
        Ref[] p = position(g, "w");
        Ref above = cmp(g, "above", "gt", p[1], in("ground"));
        Ref below = cmp(g, "below", "lt", p[1], in("ground"));

        // 1) janelas dos andares de cima
        node(g, "chanceRnd", "core.random_value", "probability", in("chance"));
        Ref window = and(g, "window", and(g, "wallAbove", isWall, above), out("chanceRnd", "bool"));
        node(g, "framed", "core.inset_faces", "geometry", in("geometry"), "selection", window,
                "amount", in("margin"), "inner_attribute", "inner");
        Ref inner = flag(g, "innerFaces", "inner");
        node(g, "litRnd", "core.random_value", "probability", 0.4);
        Ref lit = and(g, "lit", inner, out("litRnd", "bool"));
        Ref dark = and(g, "dark", inner, not(g, "notLit", out("litRnd", "bool")));
        node(g, "matLit", "core.set_material", "geometry", out("framed", "geometry"), "selection", lit, "material", "window_lit");
        node(g, "matDark", "core.set_material", "geometry", out("matLit", "geometry"), "selection", dark, "material", "window_dark");
        Ref inward = math(g, "inward", "multiply", in("depth"), -1.0);
        node(g, "recess", "core.extrude", "geometry", out("matDark", "geometry"), "selection", inner, "distance", inward);

        // 2) vitrines no terreo
        node(g, "shopRnd", "core.random_value", "probability", 0.8);
        Ref shop = and(g, "shop", and(g, "wallBelow", isWall, below), out("shopRnd", "bool"));
        node(g, "shopFrame", "core.inset_faces", "geometry", out("recess", "geometry"), "selection", shop,
                "amount", 0.3, "inner_attribute", "shop_inner");
        Ref shopInner = flag(g, "shopInner", "shop_inner");
        node(g, "shopMat", "core.set_material", "geometry", out("shopFrame", "geometry"), "selection", shopInner,
                "material", "shopfront");
        node(g, "shopRecess", "core.extrude", "geometry", out("shopMat", "geometry"), "selection", shopInner, "distance", -0.25);
        output(g, "geometry", PortType.GEOMETRY, out("shopRecess", "geometry"));
        return g.build();
    }

    private static GraphAsset facade() {
        GraphBuilder g = asset("facade");
        g.input(num("width", 9)).input(num("height", 10)).input(integer("cols", 3)).input(integer("rows", 3)).input(num("chance", 0.5))
                .input(num("ground_zone", 1.0));

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
                "ground", math(g, "ground", "multiply", rowHeight, in("ground_zone")));
        output(g, "geometry", PortType.GEOMETRY, out("win", "geometry"));
        return g.build();
    }

    /** Quatro fachadas + pilares de canto (4 instancias de uma caixa nos vertices de uma grade 2x2) + friso do terreo. */
    private static GraphAsset towerBody() {
        GraphBuilder g = asset("tower_body");
        g.input(integer("cols_x", 4)).input(integer("cols_z", 4)).input(integer("floors", 8)).input(num("cell", 3.0))
                .input(num("floor_h", 3.2)).input(num("chance", 0.5)).input(num("ground_zone", 1.0));

        Ref w = math(g, "w", "multiply", in("cols_x"), in("cell"));
        Ref d = math(g, "d", "multiply", in("cols_z"), in("cell"));
        Ref h = math(g, "h", "multiply", in("floors"), in("floor_h"));
        Ref hw = math(g, "hw", "multiply", w, 0.5);
        Ref hd = math(g, "hd", "multiply", d, 0.5);
        Ref offZ = vec(g, "offZ", 0.0, 0.0, hd);
        Ref offX = vec(g, "offX", 0.0, 0.0, hw);

        String[] names = {"front", "back", "right", "left"};
        double[] yaw = {0, 180, 90, -90};
        Ref[] parts = new Ref[6];
        for (int i = 0; i < 4; i++) {
            boolean alongX = i < 2; // frente/tras tem a largura em X; as laterais, a profundidade
            sub(g, names[i] + "Facade", "facade", "width", alongX ? w : d, "height", h,
                    "cols", alongX ? in("cols_x") : in("cols_z"), "rows", in("floors"), "chance", in("chance"),
                    "ground_zone", in("ground_zone"));
            Ref moved = translate(g, names[i] + "Move", out(names[i] + "Facade", "geometry"), alongX ? offZ : offX);
            parts[i] = rotate(g, names[i], moved, new Vec3(0, yaw[i], 0));
        }

        // pilares de canto: uma instancia por vertice de uma grade 2x2 do tamanho da planta
        node(g, "plan", "core.create_grid", "size_x", w, "size_z", d, "vertices_x", 2, "vertices_z", 2);
        node(g, "corners", "core.mesh_to_points", "geometry", out("plan", "mesh"));
        Ref pier = part(g, "pier", vec(g, "pierSize", 0.8, h, 0.8), vec(g, "pierAt", 0.0, math(g, "pierY", "multiply", h, 0.5), 0.0), "trim");
        node(g, "piers", "core.instance_on_points", "points", out("corners", "points"), "instance", pier);
        parts[4] = out("piers", "instances");

        parts[5] = part(g, "belt", vec(g, "beltSize", math(g, "beltW", "add", w, 0.5), 0.4, math(g, "beltD", "add", d, 0.5)),
                vec(g, "beltAt", 0.0, in("floor_h"), 0.0), "trim");
        output(g, "geometry", PortType.GEOMETRY, join(g, "walls", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
        output(g, "width", PortType.FLOAT, w);
        output(g, "depth", PortType.FLOAT, d);
        output(g, "height", PortType.FLOAT, h);
        return g.build();
    }

    /** Predio de teto plano: corpo + laje com parapeito + casa de maquinas + antena + aparelhos de ar sorteados. */
    private static GraphAsset tower() {
        GraphBuilder g = asset("tower");
        g.input(integer("cols_x", 4)).input(integer("cols_z", 4)).input(integer("floors", 8)).input(num("cell", 3.0))
                .input(num("floor_h", 3.2)).input(num("chance", 0.5));
        sub(g, "body", "tower_body", "cols_x", in("cols_x"), "cols_z", in("cols_z"), "floors", in("floors"),
                "cell", in("cell"), "floor_h", in("floor_h"), "chance", in("chance"));
        Ref w = out("body", "width"), d = out("body", "depth"), h = out("body", "height");
        Ref roofTop = math(g, "roofTop", "add", h, 0.6);

        // laje: caixa um pouco maior; a face de cima ganha inset e o anel sobe como parapeito
        Ref slab = part(g, "slabPart", vec(g, "slabSize", math(g, "slabW", "add", w, 0.6), 0.6, math(g, "slabD", "add", d, 0.6)),
                vec(g, "slabAt", 0.0, math(g, "slabY", "add", h, 0.3), 0.0), "roof");
        Ref[] n = normal(g, "slab");
        Ref top = cmp(g, "slabTop", "gt", n[1], 0.5);
        node(g, "rimFrame", "core.inset_faces", "geometry", slab, "selection", top, "amount", 0.55, "inner_attribute", "rim_inner");
        Ref rimSel = and(g, "rimSel", top, not(g, "notRimInner", flag(g, "rimInner", "rim_inner")));
        node(g, "rim", "core.extrude", "geometry", out("rimFrame", "geometry"), "selection", rimSel, "distance", 0.7);

        // casa de maquinas e antena
        Ref unit = part(g, "unit", vec(g, "unitSize", math(g, "unitW", "multiply", w, 0.3), 1.6, math(g, "unitD", "multiply", d, 0.3)),
                vec(g, "unitAt", 0.0, math(g, "unitY", "add", h, 1.4), 0.0), "metal");
        Ref antenna = part(g, "antenna", new Vec3(0.25, 7.0, 0.25),
                vec(g, "antennaAt", math(g, "antennaX", "multiply", w, 0.28), math(g, "antennaY", "add", h, 4.4),
                        math(g, "antennaZ", "multiply", d, 0.28)), "metal");

        // aparelhos de ar-condicionado espalhados na laje (instancias de uma caixa)
        node(g, "roofArea", "core.create_grid", "size_x", math(g, "areaW", "subtract", w, 3.0), "size_z", math(g, "areaD", "subtract", d, 3.0));
        Ref areaUp = translate(g, "areaUp", out("roofArea", "mesh"), vec(g, "areaAt", 0.0, roofTop, 0.0));
        node(g, "scatter", "core.distribute_points", "geometry", areaUp, "density", 0.045);
        Ref ac = part(g, "ac", new Vec3(1.6, 0.9, 1.1), new Vec3(0, 0.45, 0), "metal");
        node(g, "acSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "acSize", "core.random_value", "min_vector", new Vec3(0.7, 0.7, 0.7), "max_vector", new Vec3(1.3, 1.3, 1.3));
        node(g, "acs", "core.instance_on_points", "points", out("scatter", "points"), "instance", ac,
                "rotation", out("acSpin", "vector"), "scale", out("acSize", "vector"));

        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("body", "geometry"), out("rim", "geometry"), unit, antenna,
                out("acs", "instances")));
        output(g, "height", PortType.FLOAT, roofTop);
        output(g, "width", PortType.FLOAT, w);
        return g.build();
    }

    /** Piramide/cone: poligono extrudido para cima, com o anel de cima puxado para o eixo. */
    private static GraphAsset spire() {
        GraphBuilder g = asset("spire");
        g.input(num("radius", 3)).input(num("height", 10)).input(integer("segments", 4)).input(num("yaw", 45))
                .input(material("material", "metal"));
        node(g, "ring", "core.create_circle", "radius", in("radius"), "segments", in("segments"));
        node(g, "fill", "core.curve_to_mesh", "geometry", out("ring", "curve"));
        Ref turned = rotate(g, "turn", out("fill", "mesh"), vec(g, "turnBy", 0.0, in("yaw"), 0.0));
        node(g, "rise", "core.combine_xyz", "y", in("height"));
        node(g, "cone", "core.extrude", "geometry", turned, "offset", out("rise", "vector"));
        Ref[] p = position(g, "s");
        Ref high = cmp(g, "high", "gt", p[1], math(g, "midHeight", "multiply", in("height"), 0.5));
        node(g, "pull", "core.combine_xyz", "x", fmath(g, "nx", "negate", p[0], 0.0), "z", fmath(g, "nz", "negate", p[2], 0.0));
        node(g, "tip", "core.set_position", "geometry", out("cone", "geometry"), "selection", high, "offset", out("pull", "vector"));
        node(g, "clean", "core.merge_by_distance", "geometry", out("tip", "geometry"), "distance", 0.01);
        node(g, "paint", "core.set_material", "geometry", out("clean", "geometry"), "material", in("material"));
        output(g, "geometry", PortType.GEOMETRY, out("paint", "geometry"));
        return g.build();
    }

    /** Torre em recuos: base, corpo menor e coroa piramidal. */
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
        Ref radius = math(g, "crownR", "multiply", out("mid", "width"), 0.72);
        sub(g, "crown", "spire", "radius", radius, "height", 14.0, "segments", 4, "yaw", 45.0);
        Ref crownUp = translate(g, "crownUp", out("crown", "geometry"), vec(g, "crownAt", 0.0, crownY, 0.0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("base", "geometry"), midUp, crownUp));
        return g.build();
    }

    /** Torre cilindrica: circulo varrido por uma linha vertical + janelas + frisos + telhado conico. */
    private static GraphAsset roundTower() {
        GraphBuilder g = asset("round_tower");
        g.input(num("radius", 8)).input(integer("floors", 18)).input(num("floor_h", 3.4)).input(integer("segments", 20))
                .input(num("chance", 0.5));
        Ref h = math(g, "h", "multiply", in("floors"), in("floor_h"));
        node(g, "ring", "core.create_circle", "radius", in("radius"), "segments", in("segments"));
        node(g, "count", "core.int_math", "operation", "add", "a", in("floors"), "b", 1);
        // perfil de cima para baixo: com o circulo anti-horario isso deixa as normais para fora
        node(g, "profile", "core.create_line", "start", vec(g, "top", 0.0, h, 0.0), "end", new Vec3(0, 0, 0), "count", out("count", "result"));
        node(g, "wall", "core.curve_to_mesh", "geometry", out("ring", "curve"), "profile", out("profile", "curve"));
        sub(g, "win", "windows", "geometry", out("wall", "mesh"), "chance", in("chance"), "ground", in("floor_h"));

        // frisos: um aro baixo (circulo varrido por um perfil de 0.5) no terreo e outro no topo
        node(g, "bandRing", "core.create_circle", "radius", math(g, "bandR", "add", in("radius"), 0.35), "segments", in("segments"));
        node(g, "bandProfile", "core.create_line", "start", new Vec3(0, 0.5, 0), "end", new Vec3(0, 0, 0));
        node(g, "band", "core.curve_to_mesh", "geometry", out("bandRing", "curve"), "profile", out("bandProfile", "curve"));
        Ref low = translate(g, "bandLow", out("band", "mesh"), vec(g, "bandLowAt", 0.0, in("floor_h"), 0.0));
        Ref high = translate(g, "bandHigh", out("band", "mesh"), vec(g, "bandHighAt", 0.0, math(g, "bandTop", "subtract", h, 0.5), 0.0));
        node(g, "bands", "core.join_geometry");
        g.connect(low.node(), low.port(), "bands", "geometry").connect(high.node(), high.port(), "bands", "geometry");
        node(g, "bandsPaint", "core.set_material", "geometry", out("bands", "geometry"), "material", "trim");

        sub(g, "cone", "spire", "radius", math(g, "coneR", "add", in("radius"), 0.5), "height", 9.0, "segments", in("segments"),
                "yaw", 0.0, "material", "roof");
        Ref coneUp = translate(g, "coneUp", out("cone", "geometry"), vec(g, "coneAt", 0.0, h, 0.0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("win", "geometry"), out("bandsPaint", "geometry"), coneUp));
        return g.build();
    }

    // ================= casas =================

    /**
     * Casa completa: paredes com janelas em todos os andares, telhado de 2 aguas (caixa cujos vertices de cima vao ao
     * eixo), chamine, porta com batente, varanda com colunas, caminho, quintal com cerca e arvores, e tom de cor proprio.
     */
    private static GraphAsset house() {
        GraphBuilder g = asset("house");
        g.input(integer("cols_x", 2)).input(integer("cols_z", 2)).input(num("chance", 0.6));
        sub(g, "body", "tower_body", "cols_x", in("cols_x"), "cols_z", in("cols_z"), "floors", 2, "cell", 3.0,
                "floor_h", 2.8, "chance", in("chance"), "ground_zone", 0.0);
        Ref w = out("body", "width"), d = out("body", "depth"), h = out("body", "height");
        Ref hd = math(g, "hd", "multiply", d, 0.5);

        // telhado de 2 aguas
        Ref roofW = math(g, "roofW", "add", w, 0.8);
        Ref roofH = math(g, "roofH", "multiply", roofW, 0.5);
        node(g, "roofBox", "core.create_box", "size", vec(g, "roofSize", roofW, roofH, math(g, "roofD", "add", d, 0.8)));
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

        // chamine
        Ref chimneyX = math(g, "chimneyX", "multiply", w, 0.22);
        Ref chimneyZ = math(g, "chimneyZ", "multiply", d, -0.2);
        Ref chimneyY = math(g, "chimneyY", "add", h, math(g, "chimneyRise", "multiply", roofH, 0.8));
        Ref chimney = part(g, "chimney", new Vec3(0.8, 2.8, 0.8), vec(g, "chimneyAt", chimneyX, chimneyY, chimneyZ), "brick");
        Ref chimneyCap = part(g, "chimneyCap", new Vec3(1.05, 0.18, 1.05),
                vec(g, "chimneyCapAt", chimneyX, math(g, "chimneyCapY", "add", chimneyY, 1.4), chimneyZ), "trim");

        // porta, batente, varanda com colunas, caminho
        Ref doorFront = math(g, "doorFront", "add", hd, 0.05);
        Ref door = part(g, "door", new Vec3(1.2, 2.2, 0.15), vec(g, "doorAt", 0.0, 1.1, doorFront), "door");
        Ref frame = part(g, "frame", new Vec3(1.7, 2.45, 0.1), vec(g, "frameAt", 0.0, 1.225, math(g, "frameZ", "add", hd, 0.02)), "trim");
        Ref porch = part(g, "porch", new Vec3(3.8, 0.25, 1.8), vec(g, "porchAt", 0.0, 0.125, math(g, "porchZ", "add", hd, 0.9)), "trim");
        Ref postZ = math(g, "postZ", "add", hd, 1.6);
        Ref postL = part(g, "postL", new Vec3(0.2, 2.6, 0.2), vec(g, "postLAt", -1.6, 1.55, postZ), "trim");
        Ref postR = part(g, "postR", new Vec3(0.2, 2.6, 0.2), vec(g, "postRAt", 1.6, 1.55, postZ), "trim");
        Ref porchRoof = part(g, "porchRoof", new Vec3(4.2, 0.18, 2.2), vec(g, "porchRoofAt", 0.0, 2.95, math(g, "porchRoofZ", "add", hd, 0.95)), "roof");
        Ref walk = part(g, "walk", new Vec3(1.4, 0.08, 1.8), vec(g, "walkAt", 0.0, 0.09, math(g, "walkZ", "add", hd, 2.65)), "sidewalk");

        // quintal: gramado com cerca (o anel do inset sobe como mureta)
        node(g, "yard", "core.create_grid", "size_x", math(g, "yardW", "add", w, 4.0), "size_z", math(g, "yardD", "add", d, 5.0));
        Ref yardUp = translate(g, "yardUp", out("yard", "mesh"), new Vec3(0, 0.05, 1.0));
        node(g, "yardFrame", "core.inset_faces", "geometry", yardUp, "amount", 0.18, "inner_attribute", "yard_inner");
        node(g, "fenceUp", "core.extrude", "geometry", out("yardFrame", "geometry"),
                "selection", not(g, "notYardInner", flag(g, "yardInner", "yard_inner")), "offset", new Vec3(0, 0.9, 0));
        Ref[] yp = position(g, "y");
        node(g, "fencePaint", "core.set_material", "geometry", out("fenceUp", "geometry"), "selection", cmp(g, "fenceHigh", "gt", yp[1], 0.3),
                "material", "fence");
        node(g, "lawnPaint", "core.set_material", "geometry", out("fencePaint", "geometry"), "selection", cmp(g, "lawnLow", "lt", yp[1], 0.3),
                "material", "grass");

        // arvores no fundo do quintal
        node(g, "backYard", "core.create_grid", "size_x", math(g, "backW", "add", w, 1.0), "size_z", 1.0);
        Ref backUp = translate(g, "backUp", out("backYard", "mesh"), vec(g, "backAt", 0.0, 0.05, math(g, "backZ", "multiply", hd, -1.0)));
        node(g, "backScatter", "core.distribute_points", "geometry", backUp, "mode", "count", "count", 2);
        sub(g, "shrub", "tree");
        node(g, "shrubSpin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "shrubs", "core.instance_on_points", "points", out("backScatter", "points"), "instance", out("shrub", "geometry"),
                "rotation", out("shrubSpin", "vector"), "scale", new Vec3(0.5, 0.5, 0.5));

        // tom de cor proprio da casa (id constante = o mesmo valor para todos os vertices)
        Ref built = join(g, "built", out("body", "geometry"), out("roof", "geometry"), chimney, chimneyCap, door, frame, porch, postL, postR,
                porchRoof, walk, out("lawnPaint", "geometry"), out("shrubs", "instances"));
        node(g, "toneRnd", "core.random_value", "id", 0);
        node(g, "tinted", "core.set_attribute", "geometry", built, "name", "tone", "domain", "point", "type", "float",
                "value", out("toneRnd", "float"));
        output(g, "geometry", PortType.GEOMETRY, out("tinted", "geometry"));
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
            Ref turned = rotate(g, "turn" + names[i], out("house" + names[i], "geometry"), new Vec3(0, yaw[i], 0));
            parts[i] = translate(g, "place" + names[i], turned, new Vec3(at[i][0], 0, at[i][1]));
        }
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", parts));
        return g.build();
    }

    /** Patio: duas lajes longas e duas curtas em volta de um gramado. */
    private static GraphAsset courtyard() {
        GraphBuilder g = asset("courtyard");
        sub(g, "north", "tower", "cols_x", 8, "cols_z", 2, "floors", 5);
        sub(g, "south", "tower", "cols_x", 8, "cols_z", 2, "floors", 6);
        sub(g, "east", "tower", "cols_x", 5, "cols_z", 2, "floors", 4);
        sub(g, "west", "tower", "cols_x", 5, "cols_z", 2, "floors", 4);
        Ref n = translate(g, "northAt", out("north", "geometry"), new Vec3(0, 0, -10));
        Ref s = translate(g, "southAt", rotate(g, "southTurn", out("south", "geometry"), new Vec3(0, 180, 0)), new Vec3(0, 0, 10));
        Ref e = translate(g, "eastAt", rotate(g, "eastTurn", out("east", "geometry"), new Vec3(0, 90, 0)), new Vec3(10, 0, 0));
        Ref wst = translate(g, "westAt", rotate(g, "westTurn", out("west", "geometry"), new Vec3(0, -90, 0)), new Vec3(-10, 0, 0));
        Ref lawn = part(g, "lawn", new Vec3(14, 0.2, 14), new Vec3(0, 0.1, 0), "grass");
        Ref pad = part(g, "pad", new Vec3(5, 0.25, 5), new Vec3(0, 0.12, 0), "sidewalk");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", n, s, e, wst, lawn, pad));
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
        node(g, "leaves", "core.set_material", "geometry", out("clean", "geometry"), "material", "leaves");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", trunk, out("leaves", "geometry")));
        return g.build();
    }

    private static GraphAsset lamp() {
        GraphBuilder g = asset("lamp");
        Ref pole = part(g, "pole", new Vec3(0.22, 6.5, 0.22), new Vec3(0, 3.25, 0), "metal");
        Ref arm = part(g, "arm", new Vec3(1.5, 0.15, 0.15), new Vec3(0.65, 6.4, 0), "metal");
        Ref head = part(g, "head", new Vec3(0.6, 0.22, 0.45), new Vec3(1.25, 6.28, 0), "lamp");
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", pole, arm, head));
        return g.build();
    }

    /** Carro apontado para +Z: carroceria, cabine de vidro e 4 rodas (instancias nos vertices de uma grade 2x2). */
    private static GraphAsset car() {
        GraphBuilder g = asset("car");
        Ref body = part(g, "body", new Vec3(1.9, 0.7, 4.2), new Vec3(0, 0.6, 0), "car_body");
        Ref cabin = part(g, "cabin", new Vec3(1.7, 0.6, 2.1), new Vec3(0, 1.25, -0.25), "car_glass");
        node(g, "axles", "core.create_grid", "size_x", 1.9, "size_z", 2.8, "vertices_x", 2, "vertices_z", 2);
        node(g, "wheelSpots", "core.mesh_to_points", "geometry", out("axles", "mesh"));
        Ref wheel = part(g, "wheel", new Vec3(0.3, 0.6, 0.7), new Vec3(0, 0.3, 0), "tire");
        node(g, "wheels", "core.instance_on_points", "points", out("wheelSpots", "points"), "instance", wheel);
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", body, cabin, out("wheels", "instances")));
        return g.build();
    }

    /** Parque: gramado, dois caminhos em cruz, lago, bancos e arvores espalhadas fora do lago e dos caminhos. */
    private static GraphAsset park() {
        GraphBuilder g = asset("park");
        g.input(num("size", 26)).input(num("density", 0.025));
        Ref grass = part(g, "grass", vec(g, "grassSize", in("size"), 0.4, in("size")), new Vec3(0, 0.2, 0), "grass");
        Ref pathX = part(g, "pathX", vec(g, "pathXSize", in("size"), 0.12, 2.2), new Vec3(0, 0.4, 0), "sidewalk");
        Ref pathZ = part(g, "pathZ", vec(g, "pathZSize", 2.2, 0.12, in("size")), new Vec3(0, 0.4, 0), "sidewalk");

        Ref pondR = math(g, "pondR", "multiply", in("size"), 0.17);
        node(g, "pondRing", "core.create_circle", "radius", pondR, "segments", 28);
        node(g, "pondFill", "core.curve_to_mesh", "geometry", out("pondRing", "curve"));
        Ref pondUp = translate(g, "pondUp", out("pondFill", "mesh"), new Vec3(0, 0.47, 0));
        node(g, "pond", "core.set_material", "geometry", pondUp, "material", "water");

        // bancos: quatro, nos vertices de uma grade em volta do lago
        Ref bench = part(g, "benchSeat", new Vec3(2.0, 0.15, 0.6), new Vec3(0, 0.95, 0), "trunk");
        Ref benchBack = part(g, "benchBack", new Vec3(2.0, 0.5, 0.1), new Vec3(0, 1.25, -0.3), "trunk");
        Ref benchLegs = part(g, "benchLegs", new Vec3(1.7, 0.5, 0.4), new Vec3(0, 0.65, 0), "metal");
        Ref benchSet = join(g, "benchSet", bench, benchBack, benchLegs);
        node(g, "benchGrid", "core.create_grid", "size_x", math(g, "benchSpan", "multiply", pondR, 2.9), "size_z", math(g, "benchSpan2", "multiply", pondR, 2.9),
                "vertices_x", 2, "vertices_z", 2);
        node(g, "benchPoints", "core.mesh_to_points", "geometry", out("benchGrid", "mesh"));
        node(g, "benches", "core.instance_on_points", "points", out("benchPoints", "points"), "instance", benchSet,
                "rotation", new Vec3(0, 0, 0));

        // arvores: pontos aleatorios, filtrados por Fields (fora do lago e dos caminhos)
        Ref inner = math(g, "inner", "subtract", in("size"), 6.0);
        node(g, "spread", "core.create_grid", "size_x", inner, "size_z", inner);
        Ref spreadUp = translate(g, "spreadUp", out("spread", "mesh"), new Vec3(0, 0.4, 0));
        node(g, "scatter", "core.distribute_points", "geometry", spreadUp, "density", in("density"));
        Ref[] p = position(g, "p");
        Ref r2 = fmath(g, "r2", "add", fmath(g, "px2", "multiply", p[0], p[0]), fmath(g, "pz2", "multiply", p[2], p[2]));
        Ref farFromPond = cmp(g, "farFromPond", "gt", r2, math(g, "pondClear2", "multiply", math(g, "pondClear", "add", pondR, 2.5),
                math(g, "pondClearB", "add", pondR, 2.5)));
        Ref offPathX = cmp(g, "offPathX", "gt", fmath(g, "absX", "abs", p[0], 0.0), 2.0);
        Ref offPathZ = cmp(g, "offPathZ", "gt", fmath(g, "absZ", "abs", p[2], 0.0), 2.0);
        Ref plantable = and(g, "plantable", farFromPond, and(g, "offPaths", offPathX, offPathZ));
        sub(g, "oneTree", "tree");
        node(g, "scale", "core.random_value", "min_vector", new Vec3(0.7, 0.7, 0.7), "max_vector", new Vec3(1.4, 1.4, 1.4));
        node(g, "spin", "core.random_value", "min_vector", new Vec3(0, 0, 0), "max_vector", new Vec3(0, 360, 0));
        node(g, "trees", "core.instance_on_points", "points", out("scatter", "points"), "instance", out("oneTree", "geometry"),
                "selection", plantable, "rotation", out("spin", "vector"), "scale", out("scale", "vector"));

        output(g, "geometry", PortType.GEOMETRY, join(g, "all", grass, pathX, pathZ, out("pond", "geometry"),
                out("benches", "instances"), out("trees", "instances")));
        return g.build();
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

    /**
     * Rua asfaltada: faixa + tracejado central + carros espalhados em duas pistas. Tracejado e carros so aparecem longe dos
     * cruzamentos (um Field sobre a posicao ao longo da rua).
     */
    private static GraphAsset road() {
        GraphBuilder g = asset("road");
        g.input(num("length", 100)).input(num("width", 10)).input(num("y", 0.12)).input(num("pitch", 40)).input(num("size", 200));
        sub(g, "ribbon", "street", "length", in("length"), "width", in("width"), "y", in("y"), "material", "road");

        // longe dos cruzamentos: distancia ao cruzamento mais proximo (modulo do passo) acima de meia rua + folga
        Ref[] p = position(g, "m");
        Ref shifted = fmath(g, "shifted", "add", p[2], math(g, "origin", "add", math(g, "halfSize", "multiply", in("size"), 0.5),
                math(g, "halfPitch", "multiply", in("pitch"), 0.5)));
        Ref wrapped = fmath(g, "wrapped", "subtract", fmath(g, "mod", "modulo", shifted, in("pitch")), math(g, "halfPitch2", "multiply", in("pitch"), 0.5));
        Ref clear = cmp(g, "clear", "gt", fmath(g, "away", "abs", wrapped, 0.0), math(g, "clearance", "add", math(g, "halfW", "multiply", in("width"), 0.5), 3.5));

        // tracejado: pontos de uma linha, uma caixa fina por ponto
        Ref halfLen = math(g, "halfLen", "multiply", in("length"), 0.5);
        node(g, "dashLine", "core.create_line", "start", vec(g, "dashA", 0.0, math(g, "dashY", "add", in("y"), 0.03), math(g, "dashNeg", "multiply", halfLen, -1.0)),
                "end", vec(g, "dashB", 0.0, out("dashY", "result"), halfLen),
                "count", out("dashCount", "result"));
        node(g, "dashCountF", "core.to_int", "value", math(g, "dashN", "divide", in("length"), 6.0), "mode", "round");
        node(g, "dashCount", "core.int_math", "operation", "max", "a", out("dashCountF", "result"), "b", 2);
        node(g, "dashPoints", "core.curve_to_points", "geometry", out("dashLine", "curve"));
        Ref dash = part(g, "dash", new Vec3(0.3, 0.03, 2.4), new Vec3(0, 0.015, 0), "marking");
        node(g, "dashes", "core.instance_on_points", "points", out("dashPoints", "points"), "instance", dash, "selection", clear);

        // carros: pontos aleatorios sobre a faixa, empurrados para uma de duas pistas
        node(g, "carSpots", "core.distribute_points", "geometry", out("ribbon", "geometry"), "density", 0.0035);
        node(g, "laneRnd", "core.random_value", "min_int", 0, "max_int", 1);
        Ref lane = out("laneRnd", "int");
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

        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("ribbon", "geometry"), out("dashes", "instances"), out("cars", "instances")));
        return g.build();
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
                sub(g, "ribbon" + i, "street", "length", length, "width", in("width"), "y", in("y"), "material", "sidewalk");
            }
            node(g, "choice" + i, "core.geometry_to_instance", "geometry", out("ribbon" + i, "geometry"));
            choices[i] = out("choice" + i, "instances");
        }
        Ref options = join(g, "options", choices);

        node(g, "count", "core.int_math", "operation", "add", "a", in("blocks"), "b", 1);
        node(g, "lines", "core.create_grid", "size_x", size, "size_z", 2.0, "vertices_x", out("count", "result"), "vertices_z", 2);
        node(g, "pts", "core.mesh_to_points", "geometry", out("lines", "mesh"));
        Ref[] p = position(g, "line");
        node(g, "which", "core.random_value", "min_int", 0, "max_int", variants - 1);
        node(g, "alongZ", "core.instance_on_points", "points", out("pts", "points"), "instance", options,
                "selection", cmp(g, "oneSide", "lt", p[2], 0.0), "pick_instance", true, "instance_index", out("which", "int"));
        Ref alongX = rotate(g, "alongX", out("alongZ", "instances"), new Vec3(0, 90, 0));
        output(g, "geometry", PortType.GEOMETRY, join(g, "all", out("alongZ", "instances"), alongX));
        return g.build();
    }

    private static GraphAsset sidewalkGrid() {
        return ringStreets("sidewalk_grid", "street", 1);
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

        // variantes de lote; cada quarteirao sorteia uma
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
        Ref podiumUp = translate(g, "podiumTopUp", out("podiumTop", "geometry"), new Vec3(0, 10.2, 0));
        Ref podium = join(g, "podium", out("podiumBase", "geometry"), podiumUp);

        Ref[] lots = {out("skyscraper", "geometry"), out("skyscraper2", "geometry"), out("midrise", "geometry"), out("slab", "geometry"),
                out("warehouse", "geometry"), out("cylinder", "geometry"), out("parkLot", "geometry"), out("housesA", "geometry"),
                out("housesB", "geometry"), out("yard", "geometry"), podium};
        Ref[] variants = new Ref[lots.length];
        for (int i = 0; i < lots.length; i++) {
            node(g, "lot" + i, "core.geometry_to_instance", "geometry", lots[i]);
            variants[i] = out("lot" + i, "instances");
        }
        Ref choices = join(g, "choices", variants);

        node(g, "count", "core.int_math", "operation", "add", "a", in("blocks"), "b", 1);
        node(g, "blockGrid", "core.create_grid", "size_x", size, "size_z", size, "vertices_x", out("count", "result"),
                "vertices_z", out("count", "result"));
        node(g, "blockPoints", "core.mesh_to_points", "geometry", out("blockGrid", "mesh"), "mode", "faces");
        // tom de cor por quarteirao: atributo do ponto -> da instancia -> dos vertices (ao materializar)
        node(g, "toneRnd", "core.random_value");
        node(g, "tonedPoints", "core.set_attribute", "geometry", out("blockPoints", "points"), "name", "tone",
                "domain", "point", "type", "float", "value", out("toneRnd", "float"));

        node(g, "pick", "core.random_value", "min_int", 0, "max_int", lots.length - 1);
        node(g, "quarter", "core.random_value", "min_int", 0, "max_int", 3);
        node(g, "yaw", "core.math", "operation", "multiply", "a", out("quarter", "int"), "b", 90.0);
        node(g, "turn", "core.combine_xyz", "y", out("yaw", "result"));
        node(g, "stretch", "core.random_value", "min_float", 0.85, "max_float", 1.25);
        node(g, "scale", "core.combine_xyz", "x", 1.0, "y", out("stretch", "float"), "z", 1.0);
        node(g, "lots", "core.instance_on_points", "points", out("tonedPoints", "geometry"), "instance", choices,
                "pick_instance", true, "instance_index", out("pick", "int"), "rotation", out("turn", "vector"),
                "scale", out("scale", "vector"));

        // postes nos quatro cantos de cada quarteirao
        sub(g, "streetLamp", "lamp");
        node(g, "cornerGrid", "core.create_grid", "size_x", math(g, "cornerSpan", "subtract", in("block_size"), 2.4),
                "size_z", out("cornerSpan", "result"), "vertices_x", 2, "vertices_z", 2);
        node(g, "cornerPoints", "core.mesh_to_points", "geometry", out("cornerGrid", "mesh"));
        node(g, "cornerLamps", "core.instance_on_points", "points", out("cornerPoints", "points"), "instance", out("streetLamp", "geometry"));
        node(g, "lampCluster", "core.geometry_to_instance", "geometry", out("cornerLamps", "instances"));
        node(g, "lamps", "core.instance_on_points", "points", out("blockPoints", "points"), "instance", out("lampCluster", "instances"));

        join(g, "world", out("ground", "geometry"), out("sidewalks", "geometry"), out("roads", "geometry"), out("lots", "instances"),
                out("lamps", "instances"));
        node(g, "flat", "core.realize_instances", "geometry", out("world", "geometry"));
        output(g, "geometry", PortType.GEOMETRY, out("flat", "geometry"));
        return g.build();
    }
}
