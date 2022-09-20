package net.azisaba.lifepvelevel.util;

public class LevelCalculator {
    public static long toLevel(long exp) {
        return (long) (Math.sqrt(exp / 10.0));
    }

    public static long toExp(long level) {
        return level * level * 10;
    }
}
