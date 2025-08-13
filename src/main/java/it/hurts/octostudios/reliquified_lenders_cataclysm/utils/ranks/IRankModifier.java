package it.hurts.octostudios.reliquified_lenders_cataclysm.utils.ranks;

import java.util.Arrays;

public interface IRankModifier {
    record RankModifierData(String modifier, int rank) {
    }

    RankModifierData getData();

    static <T extends Enum<T> & IRankModifier> String getModifierByRank(Class<T> enumClass, int rank) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(rankModifier -> rankModifier.getRank() == rank)
                .findFirst()
                .map(IRankModifier::getModifier).orElse(null);
    }

    default String getModifier() {
        return getData().modifier();
    }

    default int getRank() {
        return getData().rank();
    }
}
