package org.betterx.wover.surface.api.noise;

import org.betterx.wover.core.api.registry.DatapackRegistryBuilder;
import org.betterx.wover.entrypoint.LibWoverSurface;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry for custom Numeric Providers. Numeric Providers can generate number sequences
 * based on a set of parameters. This is a builting registry, that can provide a list of
 * default Numeric Providers. It is not loaded from datapacks.
 * <p>
 * You can however add your own Numeric Providers to this registry while your mod is initialized using
 * the {@link #register(ResourceLocation, MapCodec)} method.
 */
public class NumericProviderRegistry {
    /**
     * The Key for the Registry. ({@code wover/numeric_provider})
     */
    public static final ResourceKey<Registry<MapCodec<? extends NumericProvider>>> NUMERIC_PROVIDER_REGISTRY = DatapackRegistryBuilder.createRegistryKey(
            LibWoverSurface.C.id("wover/numeric_provider")
    );

    /**
     * The actual Registry for the Numeric Providers.
     */
    public static final Registry<MapCodec<? extends NumericProvider>> NUMERIC_PROVIDER = new MappedRegistry<>(
            NUMERIC_PROVIDER_REGISTRY,
            Lifecycle.stable()
    );

    /**
     * Creates a ResourceKey for the Numeric Provider Registry.
     *
     * @param location The location of the Numeric Provider.
     * @return The ResourceKey for the Numeric Provider.
     */
    public static ResourceKey<MapCodec<? extends NumericProvider>> createKey(ResourceLocation location) {
        return ResourceKey.create(NUMERIC_PROVIDER_REGISTRY, location);
    }

    /**
     * Registers a new Numeric Provider.
     *
     * @param key   The ResourceKey for the Numeric Provider.
     * @param codec The Codec for the Numeric Provider.
     * @return The same ResourceKey that was passed in.
     */
    public static ResourceKey<MapCodec<? extends NumericProvider>> register(
            ResourceKey<MapCodec<? extends NumericProvider>> key,
            MapCodec<? extends NumericProvider> codec
    ) {
        Registry.register(NUMERIC_PROVIDER, key, codec);
        return key;
    }

    /**
     * Registers a new Numeric Provider.
     *
     * @param location The location of the Numeric Provider.
     * @param codec    The Codec for the Numeric Provider.
     * @return The newly created ResourceKey for the Numeric Provider.
     */
    public static ResourceKey<MapCodec<? extends NumericProvider>> register(
            ResourceLocation location,
            MapCodec<? extends NumericProvider> codec
    ) {
        return register(createKey(location), codec);
    }
}
