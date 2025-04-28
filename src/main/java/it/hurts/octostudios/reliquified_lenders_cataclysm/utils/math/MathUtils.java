package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.math;

public class MathUtils {
    public static int roundInt(double value) {
        return (int) it.hurts.sskirillss.relics.utils.MathUtils.round(value, 0);
    }

    public static double roundOneDigit(double value) {
        return it.hurts.sskirillss.relics.utils.MathUtils.round(value, 1);
    }

    public static double roundHP(double value) {
        return it.hurts.sskirillss.relics.utils.MathUtils.round(value / 2, 1);
    }

    public static double roundPercents(double value) {
        return it.hurts.sskirillss.relics.utils.MathUtils.round(value * 100, 0);
    }

    public static int roundThousands(double value) {
        return (int) it.hurts.sskirillss.relics.utils.MathUtils.round(value * 1000, 0);
    }
}
