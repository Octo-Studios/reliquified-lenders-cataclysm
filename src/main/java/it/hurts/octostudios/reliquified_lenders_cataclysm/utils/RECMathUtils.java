package it.hurts.octostudios.reliquified_lenders_cataclysm.utils;

import it.hurts.sskirillss.relics.utils.MathUtils;

public class RECMathUtils {
    public static int roundInt(double value) {
        return (int) MathUtils.round(value, 0);
    }

    public static double roundOneDigit(double value) {
        return MathUtils.round(value, 1);
    }

    public static double roundDamage(double value) {
        return MathUtils.round(value / 2, 1);
    }

    public static double roundPercents(double value) {
        return MathUtils.round(value * 100, 0);
    }
}
