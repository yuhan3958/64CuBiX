package me.cubix.gfx.model;

import me.cubix.world.block.BlockId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockModels {
    private static final int FACE_COUNT = 6;

    private final Map<Short, BlockModel> models;
    private final List<String> texturePaths;

    public record BlockModel(int[] faceLayers) {
        public int layerForFace(int face) {
            return faceLayers[face];
        }
    }

    private record LayerDef(String name, String texture) {}

    private BlockModels(Map<Short, BlockModel> models, List<String> texturePaths) {
        this.models = models;
        this.texturePaths = texturePaths;
    }

    public static BlockModels load() {
        LinkedHashMap<String, Integer> textureLayers = new LinkedHashMap<>();
        Map<Short, BlockModel> models = new HashMap<>();

        loadBlock(models, textureLayers, BlockId.STONE, "stone");
        loadBlock(models, textureLayers, BlockId.DIRT, "dirt");
        loadBlock(models, textureLayers, BlockId.GRASS, "grass");
        loadBlock(models, textureLayers, BlockId.WATER, "water");

        return new BlockModels(models, new ArrayList<>(textureLayers.keySet()));
    }

    public BlockModel modelFor(short id) {
        return models.get(id);
    }

    public List<String> texturePaths() {
        return texturePaths;
    }

    private static void loadBlock(Map<Short, BlockModel> models, LinkedHashMap<String, Integer> textureLayers, short id, String name) {
        String json = readResource("/models/block/" + name + "/model.json");
        List<LayerDef> layers = parseLayers(json);
        if (layers.isEmpty()) throw new RuntimeException("No layers in model: " + name);

        Map<String, Integer> localLayers = new HashMap<>();
        for (LayerDef layer : layers) {
            int layerIndex = textureLayers.computeIfAbsent(layer.texture(), ignored -> textureLayers.size());
            localLayers.put(layer.name(), layerIndex);
        }

        int defaultLayer = localLayers.getOrDefault("all", textureLayers.get(layers.get(0).texture()));
        int[] faces = new int[] { defaultLayer, defaultLayer, defaultLayer, defaultLayer, defaultLayer, defaultLayer };
        applyFace(json, localLayers, faces, "all", -1);
        applyFace(json, localLayers, faces, "side", -2);
        applyFace(json, localLayers, faces, "pos_x", ChunkBuilder.POS_X);
        applyFace(json, localLayers, faces, "neg_x", ChunkBuilder.NEG_X);
        applyFace(json, localLayers, faces, "top", ChunkBuilder.POS_Y);
        applyFace(json, localLayers, faces, "bottom", ChunkBuilder.NEG_Y);
        applyFace(json, localLayers, faces, "pos_z", ChunkBuilder.POS_Z);
        applyFace(json, localLayers, faces, "neg_z", ChunkBuilder.NEG_Z);

        models.put(id, new BlockModel(faces));
    }

    private static void applyFace(String json, Map<String, Integer> localLayers, int[] faces, String faceName, int face) {
        String layerName = parseFaceLayer(json, faceName);
        if (layerName == null) return;

        Integer layer = localLayers.get(layerName);
        if (layer == null) throw new RuntimeException("Unknown model layer: " + layerName);

        if (face == -1) {
            for (int i = 0; i < FACE_COUNT; i++) faces[i] = layer;
        } else if (face == -2) {
            faces[ChunkBuilder.POS_X] = layer;
            faces[ChunkBuilder.NEG_X] = layer;
            faces[ChunkBuilder.POS_Z] = layer;
            faces[ChunkBuilder.NEG_Z] = layer;
        } else {
            faces[face] = layer;
        }
    }

    private static List<LayerDef> parseLayers(String json) {
        List<LayerDef> layers = new ArrayList<>();
        Pattern objectPattern = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"texture\"\\s*:\\s*\"([^\"]+)\"\\s*}");
        Matcher matcher = objectPattern.matcher(json);
        while (matcher.find()) {
            layers.add(new LayerDef(matcher.group(1), matcher.group(2)));
        }
        return layers;
    }

    private static String parseFaceLayer(String json, String faceName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(faceName) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String readResource(String path) {
        try (InputStream in = BlockModels.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing block model: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
