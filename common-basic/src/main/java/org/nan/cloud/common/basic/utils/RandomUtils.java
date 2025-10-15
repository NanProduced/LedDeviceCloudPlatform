package org.nan.cloud.common.basic.utils;

import java.util.Random;

public class RandomUtils {

    private static final Random RANDOM = new Random();


    public static int random5Digits() {
        return RANDOM.nextInt(90000) + 10000;
    }
}
