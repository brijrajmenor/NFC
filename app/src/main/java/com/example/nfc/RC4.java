package com.example.nfc;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class RC4 {
    private byte[] S = new byte[256];
    private int x = 0;
    private int y = 0;

    /**
     * Initializes the RC4 cipher with a key.
     *
     * @param key The key to use for encryption and decryption.
     */
    public RC4(String key) {
        initialize(key.getBytes()); // Initialize state with provided key
    }

    /**
     * Initializes the RC4 state with a key.
     *
     * @param key The key to use for encryption and decryption.
     */
    private void initialize(byte[] key) {
        int keyLength = key.length;
        // Initialize S array
        for (int i = 0; i < 256; i++) {
            S[i] = (byte) i;
        }
        int j = 0;
        // Key Scheduling Algorithm (KSA)
        for (int i = 0; i < 256; i++) {
            j = (j + S[i] + key[i % keyLength]) & 0xFF;
            swap(i, j); // Swap elements
        }
    }

    private void swap(int i, int j) {
        byte temp = S[i];
        S[i] = S[j];
        S[j] = temp;
    }

    private byte keyStream() {
        // Pseudo-Random Generation Algorithm (PRGA)
        x = (x + 1) & 0xFF;
        y = (y + S[x]) & 0xFF;
        swap(x, y);
        return S[(S[x] + S[y]) & 0xFF];
    }

    public byte[] encrypt(byte[] data) {
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            output[i] = (byte) (data[i] ^ keyStream()); // XOR with keystream
        }
        return output;
    }

    public byte[] decrypt(byte[] data) {
        return encrypt(data); // RC4 is symmetric
    }

    public String encrypt(String data) {
        try {
            byte[] encryptedBytes = encrypt(data.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            Log.e("RC4", "Error encrypting string", e);
            return null;
        }
    }

    public String decrypt(String base64Data) {
        try {
            byte[] encryptedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            byte[] decryptedBytes = decrypt(encryptedBytes);
            return new String(decryptedBytes, "UTF-8"); // Convert to string, ensure valid output
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            Log.e("RC4", "Error decrypting string", e);
            return null;
        }
    }
}