/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings.impl;

import com.google.gson.JsonObject;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockSetting extends Setting<Block> {
    
    public final Predicate<Block> filter;
    
    public BlockSetting(String name, String title, String description, Block defaultValue, Consumer<Block> onChanged, Consumer<Setting<Block>> onModuleActivated, IVisible visible, Predicate<Block> filter) {
        super(name, title, description, defaultValue, onChanged, onModuleActivated, visible);
        
        this.filter = filter;
    }
    
    @Override
    protected Block parseImpl(String str) {
        return parseId(Registries.BLOCK, str);
    }
    
    @Override
    protected boolean isValueValid(Block value) {
        return filter == null || filter.test(value);
    }
    
    @Override
    public Iterable<Identifier> getIdentifierSuggestions() {
        return Registries.BLOCK.getIds();
    }
    
    @Override
    protected JsonObject save(JsonObject jsonObject) {
        jsonObject.addProperty("value", Registries.BLOCK.getId(get()).toString());
        
        return jsonObject;
    }
    
    @Override
    protected Block load(JsonObject jsonObject) {
        value = Registries.BLOCK.get(Identifier.of(jsonObject.get("value").getAsString()));
        
        if (filter != null && !filter.test(value)) {
            for (Block block : Registries.BLOCK) {
                if (filter.test(block)) {
                    value = block;
                    break;
                }
            }
        }
        
        return get();
    }
    
    public static class Builder extends SettingBuilder<Builder, Block, BlockSetting> {
        
        private Predicate<Block> filter;
        
        public Builder() {
            super(null);
        }
        
        public Builder filter(Predicate<Block> filter) {
            this.filter = filter;
            return this;
        }
        
        @Override
        public BlockSetting build() {
            return new BlockSetting(name, title, description, defaultValue, onChanged, onModuleActivated, visible, filter);
        }
        
    }
    
}
