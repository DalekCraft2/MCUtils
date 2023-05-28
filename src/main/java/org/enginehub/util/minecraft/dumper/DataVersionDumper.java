package org.enginehub.util.minecraft.dumper;

import com.google.auto.service.AutoService;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.enginehub.util.minecraft.util.GameSetupUtils.getServerRegistries;
import static org.enginehub.util.minecraft.util.GameSetupUtils.setupGame;

public class DataVersionDumper implements Dumper {

    public static void main(String[] args) {
        setupGame();
        new Default().run();
    }

    @AutoService(Dumper.class)
    public static class Default implements Dumper {
        @Override
        public void run() {
            File file = new File("output/" + SharedConstants.getCurrentVersion().getDataVersion().getVersion() + ".json");
            new DataVersionDumper(file).run();
        }
    }

    private final File file;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public DataVersionDumper(File file) {
        this.file = file;
    }

    private <T> Map<String, List<String>> getTags(Registry<T> registry) {
        Map<String, List<String>> tagCollector = new TreeMap<>();

        registry.getTags().forEach(tagPair ->
            tagCollector.put(tagPair.getFirst().location().toString(), tagPair.getSecond().stream()
                .map(entry -> checkNotNull(registry.getKey(entry.value())))
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.toList()))
        );

        return tagCollector;
    }

    @SuppressWarnings("rawtypes")
    private String getTypeName(Class<? extends Property> clazz) {
        if (clazz == EnumProperty.class) {
            return "enum";
        } else if (clazz == IntegerProperty.class) {
            return "int";
        } else if (clazz == BooleanProperty.class) {
            return "bool";
        } else if (clazz == DirectionProperty.class) {
            return "direction";
        } else {
            throw new RuntimeException("Unknown property!");
        }
    }

    @Override
    public void run() {
        // Blocks
        Map<String, Map<String, Object>> blocks = new TreeMap<>();
        for (ResourceLocation blockId : getServerRegistries().registryOrThrow(Registries.BLOCK).keySet()) {
            Map<String, Object> bl = new TreeMap<>();
            Block block = getServerRegistries().registryOrThrow(Registries.BLOCK).get(blockId);
            Map<String, Object> properties = new TreeMap<>();
            for (Property<?> prop : block.defaultBlockState().getProperties()) {
                Map<String, Object> propertyValues = new TreeMap<>();
                propertyValues.put("values", prop.getPossibleValues().stream().map(s -> s.toString().toLowerCase()).collect(Collectors.toList()));
                propertyValues.put("type", getTypeName(prop.getClass()));
                properties.put(prop.getName(), propertyValues);
            }
            bl.put("properties", properties);
            StringBuilder defaultState = new StringBuilder();
            defaultState.append(blockId.toString());
            if (!block.defaultBlockState().getValues().isEmpty()) {
                List<String> bits = new ArrayList<>();
                block.defaultBlockState().getValues().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getName()))
                    .forEach(e ->
                        bits.add(e.getKey().getName() + "=" + e.getValue().toString().toLowerCase())
                    );
                defaultState.append("[").append(String.join(",", bits)).append("]");
            }
            bl.put("defaultstate", defaultState.toString());
            blocks.put(blockId.toString(), bl);
        }

        // Items
        List<String> items = getServerRegistries().registryOrThrow(Registries.ITEM).keySet().stream().sorted().map(ResourceLocation::toString).collect(Collectors.toList());

        // Entities
        List<String> entities = getServerRegistries().registryOrThrow(Registries.ENTITY_TYPE).keySet().stream().sorted().map(ResourceLocation::toString).collect(Collectors.toList());

        // Biomes
        List<String> biomes = getServerRegistries().registryOrThrow(Registries.BIOME).keySet().stream().sorted().map(ResourceLocation::toString).collect(Collectors.toList());

        // BlockTags
        Map<String, List<String>> blockTags = getTags(getServerRegistries().registryOrThrow(Registries.BLOCK));

        // ItemTags
        Map<String, List<String>> itemTags = getTags(getServerRegistries().registryOrThrow(Registries.ITEM));

        // EntityTags
        Map<String, List<String>> entityTags = getTags(getServerRegistries().registryOrThrow(Registries.ENTITY_TYPE));

        Map<String, Object> output = new TreeMap<>();
        output.put("blocks", blocks);
        output.put("items", items);
        output.put("entities", entities);
        output.put("biomes", biomes);
        output.put("blocktags", blockTags);
        output.put("itemtags", itemTags);
        output.put("entitytags", entityTags);

        try {
            Files.asCharSink(file, StandardCharsets.UTF_8).write(gson.toJson(output));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

