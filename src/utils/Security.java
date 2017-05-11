/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.security.SecureRandom;

/**
 *
 * @author miguel
 */
public class Security {

    private static SecureRandom random = new SecureRandom();

    public static synchronized String generateToken() {
        long longToken = Math.abs(random.nextLong());
        String random = Long.toString(longToken, 16);
        return random;
    }
}
