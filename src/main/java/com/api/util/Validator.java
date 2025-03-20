package com.api.util;

public class Validator {
    public static boolean isEmpty(String ...args) {
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
