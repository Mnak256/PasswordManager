package com.blender.mainak.passwordmanager;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Toast;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

class FingerprintAuth {

    private Context context;
    private FingerprintManager fingerprintManager;
    private Cipher cipher;

    private OnAuthSuccess onAuthSuccessCallback;

    FingerprintAuth(final Context context) {
        final String MY_KEY = "mykey";
        this.context = context;
        context.checkSelfPermission(Context.FINGERPRINT_SERVICE);
        this.fingerprintManager = context.getSystemService(FingerprintManager.class);
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyStore.load(null);
            keyGenerator.init(new KeyGenParameterSpec.Builder(MY_KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_CBC).setUserAuthenticationRequired(true).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).build());
            keyGenerator.generateKey();

            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            SecretKey key = (SecretKey) keyStore.getKey(MY_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
        } catch (Exception e) {
            Log.e("FingerprintAuth", e.toString());
        }
    }

    void setOnAuthSuccessCallback(OnAuthSuccess onAuthSuccess) {
        this.onAuthSuccessCallback = onAuthSuccess;
    }

    void auth() {
        fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), new CancellationSignal(), 0, new FingerprintManager.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                onAuthSuccessCallback.run();
            }
            @Override
            public void onAuthenticationFailed() {
                Toast.makeText(context, "failed :(", Toast.LENGTH_SHORT).show();
                super.onAuthenticationFailed();
            }
        }, null);
    }
}
