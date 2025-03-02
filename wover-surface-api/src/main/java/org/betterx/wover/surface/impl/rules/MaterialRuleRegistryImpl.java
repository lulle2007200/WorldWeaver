package org.betterx.wover.surface.impl.rules;

import org.betterx.wover.entrypoint.LibWoverSurface;
import org.betterx.wover.legacy.api.LegacyHelper;
import org.betterx.wover.surface.api.rules.MaterialRuleManager;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.SurfaceRules;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class MaterialRuleRegistryImpl {
    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> SWITCH_RULE
            = MaterialRuleManager.createKey(LibWoverSurface.C.id("switch_rule"));

    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> register(
            ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> key,
            MapCodec<? extends SurfaceRules.RuleSource> rule
    ) {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, key, rule);
        return key;
    }

    @NotNull
    public static ResourceKey<MapCodec<? extends SurfaceRules.RuleSource>> createKey(ResourceLocation location) {
        return ResourceKey.create(
                BuiltInRegistries.MATERIAL_RULE.key(),
                location
        );
    }

    @ApiStatus.Internal
    public static void bootstrap() {
        register(SWITCH_RULE, SwitchRuleSource.CODEC);

        if (LegacyHelper.isLegacyEnabled()) {
            Registry.register(
                    BuiltInRegistries.MATERIAL_RULE,
                    "bclib_switch_rule",
                    LegacyHelper.wrap(SwitchRuleSource.CODEC)
            );
        }
    }
}
