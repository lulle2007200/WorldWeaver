package org.betterx.wover.util;

import org.betterx.wover.core.api.ModCore;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;

/**
 * A set of resource locations that can contain wildcard locations. A wildcard location is a {@link ResourceLocation}
 * with a regular namespace and {@code *} as the path. For example, {@code minecraft:*} is a wildcard location for all
 * resources in the {@code minecraft} namespace.
 * <p>
 * This is a {@link HashSet} that overrides {@link #contains(Object)} to check for wildcard locations.
 * First, it checks if the location is contained as is in the set. If not, it checks if the namespace of the location
 * is contained in the set. If not, it returns false.
 */
public class ResourceLocationSet extends HashSet<ResourceLocation> {
    /**
     * Returns true if this set contains the specified element or a wildcard location for the element's namespace.
     *
     * @param o element whose presence in this set is to be tested
     * @return
     */
    @Override
    public boolean contains(Object o) {
        if (o instanceof ResourceLocation location) {
            return containsResource(location);
        }
        return super.contains(o);
    }

    private boolean containsResource(ResourceLocation o) {
        if (!super.contains(o)) {
            return super.stream().anyMatch(location -> {
                if (location.getPath().equals(WildcardResourceLocation.CATCH_ALL_PATH)) {
                    return location.getNamespace().equals(o.getNamespace());
                }
                return false;
            });
        }

        return true;
    }

    public static class WildcardResourceLocation {
        private WildcardResourceLocation() {
        }

        static final String CATCH_ALL_PATH = "__..-all-..__";

        private static String[] parse(String string, String c) {
            String[] parts = string.split(c, 2);
            if (parts.length == 2) return parts;
            return new String[]{ResourceLocation.DEFAULT_NAMESPACE, string};
        }

        public static ResourceLocation forAllFrom(ModCore mod) {
            return ResourceLocation.fromNamespaceAndPath(mod.namespace, CATCH_ALL_PATH);
        }

        public static ResourceLocation parse(String string) {
            final var decomposed = parse(string, ":");
            if (decomposed[1].equals("*")) {
                return ResourceLocation.fromNamespaceAndPath(decomposed[0], CATCH_ALL_PATH);
            } else {
                return ResourceLocation.fromNamespaceAndPath(decomposed[0], decomposed[1]);
            }
        }
    }
}
