package com.api.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PBKDF2Encryption {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String encryptPassword(String password, String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),
                Base64.getDecoder().decode(salt),
                ITERATIONS,
                KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] hash = factory.generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String inputPassword, String storedSalt, String storedHashedPassword)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        String hashedInputPassword = encryptPassword(inputPassword, storedSalt);
        byte[] inputHashBytes = Base64.getDecoder().decode(hashedInputPassword);
        byte[] storedHashBytes = Base64.getDecoder().decode(storedHashedPassword);

        return MessageDigest.isEqual(inputHashBytes, storedHashBytes);
    }

//    public static void main(String[] args) {
//        try {
//            String password = "12345678";
////            String salt = generateSalt();
////            String encryptedPassword = encryptPassword(password, salt);
//            String salt = "S8gXTptQOcnsOCU3S7XzDg==";
//            String encryptedPassword = "tgB3S+ikpEFf0+d8wOMuTvVJR/GxX3t+K6ooxm7pgA8=";
//
//            System.out.println("Password: " + password);
//            System.out.println("Salt: " + salt);
//            System.out.println("Encrypted Password: " + encryptedPassword);
//
//            boolean isMatch = verifyPassword("12345678", salt, encryptedPassword);
//            System.out.println("Correct Password Match: " + isMatch);
//
//            boolean isWrongMatch = verifyPassword("wrongPassword", salt, encryptedPassword);
//            System.out.println("Wrong Password Match: " + isWrongMatch);
//
//        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//    }
}
