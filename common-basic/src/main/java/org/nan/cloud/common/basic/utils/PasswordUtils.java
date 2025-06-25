package org.nan.cloud.common.basic.utils;

import org.nan.cloud.common.basic.exception.ExceptionEnum;

import java.security.SecureRandom;

public class PasswordUtils {

    // 定义密码字符集（包含大写字母、小写字母、数字和特殊字符）
    private static final String UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_CASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "@#$%&";

    // 合并所有字符集
    private static final String ALL_CHARACTERS = UPPER_CASE + LOWER_CASE + DIGITS + SPECIAL_CHARACTERS;

    private static final SecureRandom random = new SecureRandom();

    /**
     * 生成一个随机的x位密码
     * @return 随机生成的x位密码
     */
    public static String generatePassword(int digit) {
        ExceptionEnum.OPERATION_NOT_SUPPORTED.throwIf(digit <= 8,"digit must greater than 8");
        StringBuilder password = new StringBuilder(digit);

        // 确保每类字符至少出现一次
        password.append(randomFrom(UPPER_CASE));
        password.append(randomFrom(LOWER_CASE));
        password.append(randomFrom(DIGITS));
        password.append(randomFrom(SPECIAL_CHARACTERS));

        // 补充剩余的8个字符，确保总长度为x
        for (int i = 4; i < digit; i++) {
            password.append(randomFrom(ALL_CHARACTERS));
        }

        // 打乱顺序，增加密码的复杂度
        return shuffle(password.toString());
    }

    /**
     * 从指定的字符集中随机选择一个字符
     * @param characters 字符集
     * @return 随机字符
     */
    private static char randomFrom(String characters) {
        int index = random.nextInt(characters.length());
        return characters.charAt(index);
    }

    /**
     * 打乱字符串的顺序
     * @param str 需要打乱顺序的字符串
     * @return 打乱顺序后的字符串
     */
    private static String shuffle(String str) {
        char[] array = str.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

}
