package com.ouo.mask.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/***********************************************************
 * Luhn 算法工具
 *
 * Author:   ouo
 * Date:     2024/11/28
 ***********************************************************/
@Slf4j
public class LuhnUtil {

    private static final int DEC_RADIX = 10;

    /**
     * Validate bank card number using Luhn algorithm
     * see {@link org.hibernate.validator.internal.constraintvalidators.hv.LuhnCheckValidator}
     *
     * @param bankCardNo
     * @return
     */
    public static boolean isValid(final String bankCardNo) {
        if (null == bankCardNo || 1 >= bankCardNo.length()) return false;
        return isCheckDigitValid(bankCardNo.substring(0, bankCardNo.length() - 1), bankCardNo.charAt(bankCardNo.length() - 1));
    }

    /**
     * Validate check digit using Luhn algorithm
     *
     * @param value      The digits over which to calculate the checksum
     * @param checkDigit the check digit
     * @return {@code true} if the luhn check result matches the check digit, {@code false} otherwise
     */
    public static boolean isCheckDigitValid(final CharSequence value, char checkDigit) {
        if (value == null) {
            return false;
        }
        List<Integer> digits;
        try {
            digits = extractDigits(value.toString());
        } catch (NumberFormatException e) {
            return false;
        }
        int modResult = ModUtil.calculateLuhnMod10Check(digits);

        if (!Character.isDigit(checkDigit)) {
            return false;
        }

        int checkValue = extractDigit(checkDigit);
        return checkValue == modResult;
    }

    /**
     * Returns the numeric {@code int} value of a {@code char}
     *
     * @param value the input {@code char} to be parsed
     * @return the numeric {@code int} value represented by the character.
     * @throws NumberFormatException in case character is not a digit
     */
    private static int extractDigit(char value) throws NumberFormatException {
        if (Character.isDigit(value)) {
            return Character.digit(value, DEC_RADIX);
        } else {
            throw new NumberFormatException(value + " is not a digit.");
        }
    }

    /**
     * Parses the {@link String} value as a {@link List} of {@link Integer} objects
     *
     * @param value the input string to be parsed
     * @return List of {@code Integer} objects.
     * @throws NumberFormatException in case any of the characters is not a digit
     */
    private static List<Integer> extractDigits(final String value) throws NumberFormatException {
        List<Integer> digits = new ArrayList<>(value.length());
        char[] chars = value.toCharArray();
        for (char c : chars) {
            digits.add(extractDigit(c));
        }
        return digits;
    }
}
