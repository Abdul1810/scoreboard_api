package com.api.util;

public class Utils {
    public static boolean validateStringWithOptions(String value, String... options) {
        for (String option : options) {
            if (option.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean validatePositiveObjIntegers(Object... numbers) {
        for (Object number : numbers) {
            if (!validatePositiveObjInteger(number)) {
                return false;
            }
        }
        return true;
    }

    public static boolean validatePositiveIntegers(String... numbers) {
        for (String number : numbers) {
            if (!validatePositiveInteger(number)) {
                return false;
            }
        }
        return true;
    }

    public static boolean withinMaxIntegers(int max, String... numbers) {
        for (String number : numbers) {
            if (Integer.parseInt(number) > max) {
                return false;
            }
        }
        return true;
    }

    private static boolean validatePositiveInteger(String number) {
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Integer.parseInt(number) < 0) {
            return false;
        }
        return true;
    }

    private static boolean validatePositiveObjInteger(Object number) {
        try {
            Integer.parseInt(number.toString());
        } catch (NumberFormatException e) {
            return false;
        }
        if (Integer.parseInt(number.toString()) < 0) {
            return false;
        }
        return true;
    }
}
