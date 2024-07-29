package org.betterx.wover.biome.impl.modification;

import org.betterx.wover.biome.api.modification.BiomeModification;
import org.betterx.wover.biome.api.modification.BiomeModificationRegistry;
import org.betterx.wover.biome.api.modification.predicates.BiomePredicate;
import org.betterx.wover.common.generator.api.biomesource.ReloadableBiomeSource;
import org.betterx.wover.common.generator.api.chunkgenerator.RebuildableFeaturesPerStep;
import org.betterx.wover.core.api.registry.DatapackRegistryBuilder;
import org.betterx.wover.entrypoint.LibWoverBiome;
import org.betterx.wover.events.api.WorldLifecycle;
import org.betterx.wover.events.api.types.OnBootstrapRegistry;
import org.betterx.wover.events.impl.EventImpl;
import org.betterx.wover.state.api.WorldState;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;

import com.google.common.base.Stopwatch;
import static org.betterx.wover.events.impl.AbstractEvent.SYSTEM_PRIORITY;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.ApiStatus;

public class BiomeModificationRegistryImpl {
    public static final EventImpl<OnBootstrapRegistry<BiomeModification>> BOOTSTRAP_BIOME_MODIFICATION_REGISTRY
            = new EventImpl<>("BOOTSTRAP_BIOME_MODIFICATION_REGISTRY");

    private static boolean didInit = false;

    @ApiStatus.Internal
    public static void initialize() {
        if (didInit) return;
        didInit = true;

        DatapackRegistryBuilder.register(
                BiomeModificationRegistry.BIOME_MODIFICATION_REGISTRY,
                BiomeModification.CODEC,
                BiomeModificationRegistryImpl::onBootstrap
        );

        WorldLifecycle.MINECRAFT_SERVER_READY.subscribe(BiomeModificationRegistryImpl::whenReady, SYSTEM_PRIORITY);
    }

    private static void onBootstrap(BootstrapContext<BiomeModification> ctx) {
        BOOTSTRAP_BIOME_MODIFICATION_REGISTRY.emit(c -> c.bootstrap(ctx));
    }

    // based on Fabrics net.fabricmc.fabric.api.biome.v1.BiomeModifications
    // We need to reimplement this, as we want to drive the Modifications
    // from a Datapack backed Registry.
    // The current Fabric API implementation is not suitable for this.
    private static void whenReady(
            LevelStorageSource.LevelStorageAccess storageSource,
            PackRepository packRepository,
            WorldStem worldStem
    ) {
        final Stopwatch sw = Stopwatch.createStarted();

        final RegistryAccess registryAccess = WorldState.registryAccess();
        final Registry<BiomeModification> modifications = registryAccess
                .registry(BiomeModificationRegistry.BIOME_MODIFICATION_REGISTRY)
                .orElse(null);
        if (modifications == null) {
            LibWoverBiome.C.log.error("Biome Modification Registry is missing. Cannot apply Biome Modifications.");
            return;
        }
        final Registry<Biome> biomes = registryAccess.registryOrThrow(Registries.BIOME);

        final List<ResourceKey<Biome>> keys = biomes
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingInt(key -> biomes.getId(biomes.getOrThrow(key))))
                .toList();

        final BiomeTagModificationWorker biomeTagWorker = new BiomeTagModificationWorker();
        final List<BiomeModification> biomeModifications = modifications.stream().toList();

        int biomesChanged = 0;
        int biomesProcessed = 0;
        int modifiersApplied = 0;
        int tagsAdded = 0;

        for (ResourceKey<Biome> biomeKey : keys) {
            BiomePredicate.Context context = BiomePredicate.Context.of(registryAccess, biomeKey);
            if (context == null) {
                LibWoverBiome.C.log.warn("Failed to get biome context for {}", biomeKey.location());
                continue;
            }

            biomesProcessed++;
            GenerationSettingsWorker worker = null;
            MobSettingsWorker mobWorker = null;
            boolean didChangeBiome = false;
            for (BiomeModification modification : biomeModifications) {
                if (modification.predicate().test(context)) {
                    if (worker == null) {
                        worker = new GenerationSettingsWorker(registryAccess, context.biome);
                    }
                    if (mobWorker == null) {
                        mobWorker = new MobSettingsWorker(context.biome);
                    }

                    if (modification.biomeTags() != null) {
                        for (TagKey<Biome> tag : modification.biomeTags()) {
                            if (biomeTagWorker.addBiomeToTag(tag, context)) {
                                tagsAdded++;
                                didChangeBiome = true;
                            }
                        }
                    }

                    modification.apply(worker, mobWorker);
                    modifiersApplied++;
                }
            }

            if (worker != null) {
                //this call has an important side effect of re-freezing the features and carvers
                // make sure it is not bypassed due to lazy evaluation
                if (worker.finished()) {
                    didChangeBiome = true;
                }
            }

            if (mobWorker != null) {
                //this call has an important side effect of re-freezing the features and carvers
                // make sure it is not bypassed due to lazy evaluation
                if (mobWorker.finished()) {
                    didChangeBiome = true;
                }
            }

            if (didChangeBiome) biomesChanged++;
        }

        biomeTagWorker.finished();

        if (tagsAdded > 0) {
            //We need to reload all BiomeSources, as some tags have changed
            final Registry<LevelStem> dimensions = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            dimensions.forEach(stem -> {
                if (stem.generator().getBiomeSource() instanceof ReloadableBiomeSource reloadable) {
                    reloadable.reloadBiomes();
                }
            });
        }

        if (biomesProcessed > 0) {
            //We need to rebuild all feature maps, as we might have added feature that did not yet exist on any
            //of the valid biomes
            final Registry<LevelStem> dimensions = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
            dimensions.forEach(stem -> {
                if (stem.generator() instanceof RebuildableFeaturesPerStep<?> generator) {
                    generator.wover_rebuildFeaturesPerStep();
                }
            });

            LibWoverBiome.C.log.info(
                    "Applied {} biome modifications and added {} tags to {} of {} biomes in {}",
                    modifiersApplied,
                    tagsAdded,
                    biomesChanged,
                    biomesProcessed,
                    sw.stop()
            );
        }
    }

    public static ResourceKey<BiomeModification> createKey(
            ResourceLocation modificationID
    ) {
        return ResourceKey.create(
                BiomeModificationRegistry.BIOME_MODIFICATION_REGISTRY,
                modificationID
        );
    }
}
