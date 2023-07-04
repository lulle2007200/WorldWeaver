package org.betterx.wover.surface.impl.numeric;

import org.betterx.wover.math.api.MathHelper;
import org.betterx.wover.surface.api.numeric.NumericProvider;
import org.betterx.wover.surface.mixin.SurfaceRulesContextAccessor;

import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.Objects;

public final class RandomIntProvider implements NumericProvider {
    public static final Codec<RandomIntProvider> CODEC = Codec
            .INT.fieldOf("range")
                .xmap(RandomIntProvider::new, obj -> obj.range)
                .codec();
    public final int range;
    private final RandomSource random;


    public RandomIntProvider(int range) {
        this.range = range;
        random = new XoroshiroRandomSource(MathHelper.getSeed(range));
    }

    @Override
    public int getNumber(SurfaceRulesContextAccessor context) {
        return random.nextInt(range);
    }

    @Override
    public Codec<? extends NumericProvider> pcodec() {
        return CODEC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RandomIntProvider) obj;
        return this.range == that.range;
    }

    @Override
    public int hashCode() {
        return Objects.hash(range);
    }

    @Override
    public String toString() {
        return "RandomIntProvider[" +
                "range=" + range + ']';
    }
}
