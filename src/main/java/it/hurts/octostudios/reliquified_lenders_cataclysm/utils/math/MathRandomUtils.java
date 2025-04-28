package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math;

import net.minecraft.util.RandomSource;

public class MathRandomUtils {
    public static float randomized(RandomSource random, float value) {
        return value * random.nextFloat();
    }

    public static double randomized(RandomSource random, double value) {
        return value * random.nextDouble();
    }

    public static int randomized(RandomSource random, int value) {
        return value * random.nextInt();
    }
}
