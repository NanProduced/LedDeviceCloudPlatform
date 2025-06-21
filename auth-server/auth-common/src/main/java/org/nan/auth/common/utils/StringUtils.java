package org.nan.auth.common.utils;

public class StringUtils {

    public static String[] splitByHash(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        int hashIndex = input.indexOf('#');

        if (hashIndex == -1) {
            return null;
        } else {
            String part1 = input.substring(0, hashIndex);
            String part2 = input.substring(hashIndex + 1);
            return new String[]{part1, part2};
        }
    }
}
