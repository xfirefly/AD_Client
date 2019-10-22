package com.bluberry.adclient;

import android.util.Base64;

import com.bluberry.common.URLConfig;

import java.security.Key;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptLib {

    private static String Encrypt(String raw) throws Exception {
        Cipher c = getCipher(Cipher.ENCRYPT_MODE);

        byte[] encryptedVal = c.doFinal(raw.getBytes("UTF-8"));
        return Base64.encodeToString(encryptedVal, Base64.DEFAULT);
    }

    private static Cipher getCipher(int mode) throws Exception {
        //Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding", new SunJCE());
        // setup AES cipher in CBC mode with PKCS #5 padding
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");

        //a random Init. Vector. just for testing
        byte[] iv = ivs.getBytes("UTF-8");

        c.init(mode, generateKey(), new IvParameterSpec(iv));
        return c;
    }

    private static String Decrypt(String encrypted) throws Exception {

        byte[] decodedValue = Base64.decode(encrypted, Base64.DEFAULT);

        Cipher c = getCipher(Cipher.DECRYPT_MODE);
        byte[] decValue = c.doFinal(decodedValue);

        return new String(decValue);
    }

    //a random Init. Vector. just for testing
    static String ivs = "e675f725e675f725";

    private static Key generateKey() throws Exception {

        String passWords = URLConfig.prefix; // "config.ini";
        String salts = URLConfig.fireplayer; // "1920*1280";

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        char[] password = passWords.toCharArray();
        byte[] salt = salts.getBytes("UTF-8");

        KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        byte[] encoded = tmp.getEncoded();
        return new SecretKeySpec(encoded, "AES");

    }

    public static String decode(String encrypt) {
        String decrypt = null;
        try {
            decrypt = Decrypt(encrypt);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return decrypt;
    }
}