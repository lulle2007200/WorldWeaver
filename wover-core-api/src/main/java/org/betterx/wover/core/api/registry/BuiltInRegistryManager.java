package org.betterx.wover.core.api.registry;

import org.betterx.wover.entrypoint.LibWoverCore;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class BuiltInRegistryManager {
    public static <V, T extends V> T register(Registry<V> registry, ResourceLocation resourceLocation, T object) {
        return Registry.register(registry, resourceLocation, object);
    }

    public static <V, T extends V> T register(Registry<V> registry, ResourceKey<V> resourceKey, T object) {
        return Registry.register(registry, resourceKey, object);
    }

    public static <V, T extends V> Holder.Reference<V> registerForHolder(
            Registry<V> registry,
            ResourceLocation resourceLocation,
            T object
    ) {
        return Registry.registerForHolder(registry, resourceLocation, object);
    }

    public static <T> Registry<T> createRegistry(
            ResourceKey<? extends Registry<T>> resourceKey,
            Function<Registry<T>, T> registryBootstrap
    ) {
        LibWoverCore.C.log.debug("Creating registry: " + resourceKey.location());
        return BuiltInRegistries.registerSimple(resourceKey, registryBootstrap::apply);
    }

    @Deprecated
    public static <T> Registry<T> createRegistry(
            ResourceKey<? extends Registry<T>> resourceKey,
            Lifecycle lifecycle,
            Function<Registry<T>, T> registryBootstrap
    ) {
        LibWoverCore.C.log.debug("Creating registry: " + resourceKey.location());
        return BuiltInRegistries.registerSimple(resourceKey, registryBootstrap::apply);
    }
}
