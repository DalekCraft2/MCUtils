package org.enginehub.util.minecraft.dumper;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;

import java.util.Iterator;

import static org.enginehub.util.minecraft.util.GameSetupUtils.setupGame;

@AutoService(Dumper.class)
public class EntityTypeCategoriesDumper extends RegistryClassDumper {

    // worldedit doesn't actually do entity type categories yet
    public static void main(String[] args) {
        setupGame();
        new EntityTypeCategoriesDumper().run();
    }

    public EntityTypeCategoriesDumper() {
        super(ClassName.get("com.sk89q.worldedit.world.entity", "EntityTypeCategory"), false);
    }

    @Override
    protected Iterator<String> getIds() {
        return EntityTypeTags.getAllTags().getAvailableTags().stream().map(ResourceLocation::getPath).iterator();
    }
}
