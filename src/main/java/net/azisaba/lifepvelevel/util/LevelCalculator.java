package net.azisaba.lifepvelevel.util;

public class LevelCalculator {
    private static final double BASE = 50;
    private static final double GROWTH = 200;
    private static final double REVERSE_PQ_PREFIX = -(BASE - 0.5 * GROWTH) / GROWTH;
    private static final double REVERSE_CONST = REVERSE_PQ_PREFIX * REVERSE_PQ_PREFIX;
    private static final double GROWTH_DIVIDES_2 = 2 / GROWTH;
    private static final double HALF_GROWTH = GROWTH / 2;

    public static long toLevel(long exp) {
        if (exp < 0) {
            return 0;
        }
        return (long) (REVERSE_PQ_PREFIX + Math.sqrt(REVERSE_CONST + GROWTH_DIVIDES_2 * exp));
    }

    public static long toExp(double level) {
        return toExp0(level + 1);
    }

    private static long toExp0(double level) {
        double lv = Math.floor(level);
        long x0 = getTotalExpToFullLevel(lv);
        if (lv == level) return x0;
        return (long) ((getTotalExpToFullLevel(level + 1) - x0) * (level % 1) + x0);
    }

    private static long getTotalExpToFullLevel(double level) {
        return (long) ((HALF_GROWTH * (level - 2) + BASE) * (level - 1));
    }
}
