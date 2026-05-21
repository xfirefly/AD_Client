package com.bluberry.adclient;

import android.util.Base64;
import android.util.Log;

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

    //a random Init. Vector. just for testing
    static String ivs = "e675f725e675f725";

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

    // Cache the derived key — password and salt are compile-time constants so the
    // result is always identical. PBKDF2WithHmacSHA1 with 65536 iterations is
    // intentionally slow (~300-2000 ms on low-end hardware); computing it once and
    // reusing eliminates the per-program startup cost.
    private static volatile Key cachedKey = null;

    // SharedPreferences key used to persist the derived AES key across cold boots.
    // Value is the Base64-encoded raw 16-byte AES key.
    private static final String PREF_KEY_CACHE = "aes_derived_key";

    private static Key generateKey() throws Exception {
        // 1. In-process memory cache (fastest path — same process lifetime)
        if (cachedKey != null) {
            Log.d("ADR_TIMING", "[CryptLib] generateKey: memory cache hit");
            return cachedKey;
        }

        synchronized (CryptLib.class) {
            if (cachedKey != null) return cachedKey;

            // 2. Persistent cache in SharedPreferences (survives cold reboot)
            String stored = App.getString(PREF_KEY_CACHE);
            if (stored != null && !stored.equals("0") && !stored.isEmpty()) {
                byte[] keyBytes = Base64.decode(stored, Base64.DEFAULT);
                cachedKey = new SecretKeySpec(keyBytes, "AES");
                Log.w("ADR_TIMING", "[CryptLib] generateKey: loaded from SharedPreferences, skipping PBKDF2");
                return cachedKey;
            }

            // 3. First ever run — compute PBKDF2 and persist the result.
            //    Prefer the native BoringSSL provider (AndroidOpenSSL / Conscrypt) over
            //    the default pure-Java BouncyCastle. The provider name changed across
            //    Android versions, so try both before falling back to the default.
            Log.w("ADR_TIMING", "[CryptLib] generateKey: computing PBKDF2 (first run)...");
            long t0 = System.currentTimeMillis();

            SecretKeyFactory factory = nativePbkdf2Factory();
            KeySpec spec = new PBEKeySpec(
                    URLConfig.prefix.toCharArray(),
                    URLConfig.fireplayer.getBytes("UTF-8"),
                    65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            cachedKey = new SecretKeySpec(tmp.getEncoded(), "AES");
            Log.w("ADR_TIMING", "[CryptLib] PBKDF2 took " + (System.currentTimeMillis() - t0)
                    + " ms via provider=" + factory.getProvider().getName() + " (will not repeat)");

            // Persist so future cold boots skip PBKDF2 entirely
            String encoded = Base64.encodeToString(cachedKey.getEncoded(), Base64.NO_WRAP);
            App.saveString(PREF_KEY_CACHE, encoded);
            Log.w("ADR_TIMING", "[CryptLib] AES key persisted to SharedPreferences");
        }
        return cachedKey;
    }

    /**
     * Returns a SecretKeyFactory for PBKDF2WithHmacSHA1 using the fastest available provider.
     *
     * Priority:
     *  1. "AndroidOpenSSL" — native BoringSSL, default on Android 6-9
     *  2. "Conscrypt"      — native BoringSSL, default on Android 10+
     *  3. Default provider — pure-Java BouncyCastle (fallback)
     */
    private static SecretKeyFactory nativePbkdf2Factory() {
        final String algo = "PBKDF2WithHmacSHA1";
        for (String provider : new String[]{"AndroidOpenSSL", "Conscrypt"}) {
            try {
                SecretKeyFactory f = SecretKeyFactory.getInstance(algo, provider);
                Log.d("ADR_TIMING", "[CryptLib] using native provider: " + provider);
                return f;
            } catch (Exception ignored) {
                // provider not available on this Android version — try next
            }
        }
        // Fallback: let the JVM pick (typically BouncyCastle)
        try {
            Log.w("ADR_TIMING", "[CryptLib] native provider unavailable, falling back to default (BouncyCastle)");
            return SecretKeyFactory.getInstance(algo);
        } catch (Exception e) {
            throw new RuntimeException("No PBKDF2WithHmacSHA1 provider available", e);
        }
    }



    public static String decode(String encrypt) {
        String decrypt = null;
        try {
            long t0 = System.currentTimeMillis();
            decrypt = Decrypt(encrypt);
            Log.d("ADR_TIMING", "[CryptLib] decode() took " + (System.currentTimeMillis() - t0) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decrypt;
    }
}