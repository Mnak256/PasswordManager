package com.blender.mainak.passwordmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    DatabaseHandler databaseHandler;
    RecyclerViewAdapter recyclerViewAdapter;
    java.util.List<Record> records;

    String temp;
    byte[]iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Toast.makeText(this, getFilesDir().toString(), Toast.LENGTH_LONG).show();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                showInputDialog();
            }
        });

        databaseHandler = new DatabaseHandler(this, 5);

        refreshListView();
    }

    @Override
    protected void onDestroy() {
        databaseHandler.close();
        super.onDestroy();
    }

    void refreshListView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        records = databaseHandler.getAllRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        recyclerViewAdapter = new RecyclerViewAdapter(records);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerViewAdapter);
    }

    void auth(final Record record) {
        final String MY_KEY = "mykey";
        Cipher cipher;
        KeyStore keyStore;
        KeyGenerator keyGenerator;
        FingerprintManager fingerprintManager;
        SecretKey encKey;

        getApplicationContext().checkSelfPermission(Context.FINGERPRINT_SERVICE);
        fingerprintManager = getApplicationContext().getSystemService(FingerprintManager.class);

        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyStore.load(null);
            keyGenerator.init(new KeyGenParameterSpec.Builder(MY_KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_CBC).setUserAuthenticationRequired(true).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).build());
            keyGenerator.generateKey();

            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//            keyStore.load(null);
            final SecretKey key = (SecretKey) keyStore.getKey(MY_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            fingerprintManager.authenticate(new FingerprintManager.CryptoObject(cipher), new CancellationSignal(), 0, new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    try {
                        Toast.makeText(getApplicationContext(), "authentic", Toast.LENGTH_SHORT).show();

                        File keyStoreTest = new File(getApplicationContext().getFilesDir(), "MyKeyStore");
//                        Log.i("MainActivity", "keyStore file exists:" + keyStoreTest.exists());

                        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

                        if (!keyStoreTest.exists()) {
                            KeyStore newKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                            char[] keyStorePassword = "123abc".toCharArray();
                            newKeyStore.load(null, keyStorePassword);

                            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES);
                            keyGenerator.init(256);
                            SecretKey secretKey = keyGenerator.generateKey();

                            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
                            KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyStorePassword);
                            newKeyStore.setEntry("myKeyAlias", secretKeyEntry, protectionParameter);

                            FileOutputStream keyStoreFileOutputStream = openFileOutput("MyKeyStore", Context.MODE_PRIVATE);
                            newKeyStore.store(keyStoreFileOutputStream, keyStorePassword);
                            keyStoreFileOutputStream.close();
                        }

                        FileInputStream stream = openFileInput("MyKeyStore");
                        keyStore.load(stream, "123abc".toCharArray());
                        stream.close();

                        Key key = keyStore.getKey("myKeyAlias", "123abc".toCharArray());

                        SecureRandom rnd = new SecureRandom();
                        IvParameterSpec iv = new IvParameterSpec(rnd.generateSeed(16));

                        byte[]cypherText = encrypt(record.password.getBytes(), key, iv);
//                        byte[]decryptedText = decrypt(cypherText, key, iv);

                        Log.i("MainActivity", "plain text:" + record.password);
                        Log.i("MainActivity", "cypher text:" + new String(cypherText));
//                        Log.i("MainActivity", "plain text:" + new String(decryptedText));

                        addRecord(new Record(record.domain, record.username, new String(cypherText)));

                    } catch (Exception e) {
                        Log.i("MainActivity", "err:" + e.toString());
                        e.printStackTrace();
                    }

                    //reInit();
//                    super.onAuthenticationSucceeded(result);
                }
                @Override
                public void onAuthenticationFailed() {
                    Toast.makeText(getApplicationContext(), "failed :(", Toast.LENGTH_SHORT).show();
                    super.onAuthenticationFailed();
                }
            }, null);
        } catch (Exception e) {
            //throw new RuntimeException("Failed to get Cipher", e);
            e.printStackTrace();
        }



        Toast.makeText(getApplicationContext(), "Authenticate Fingerprint to View", Toast.LENGTH_SHORT).show();
    }

    public byte[] encrypt(byte[] plainText, Key key, IvParameterSpec iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(plainText);
    }

    public byte[] decrypt(byte[] cipherText, Key key, IvParameterSpec iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(cipherText);
    }

    void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add New Note");
        View view = getLayoutInflater().inflate(R.layout.input_alert_dialog, null);
        builder.setView(view);

        final EditText editTextDomain = view.findViewById(R.id.editText_domain);
        final EditText editTextUsername = view.findViewById(R.id.editText_username);
        final EditText editTextPassword = view.findViewById(R.id.editText_password);

        /*
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.toggleSoftInputFromWindow(editTextTitle.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED, 0);
        }
        editTextTitle.requestFocus();
        */

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String domain = editTextDomain.getText().toString();
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();
                if (domain.length() == 0 || username.length() == 0 || password.length() == 0) {
                    Toast.makeText(MainActivity.this, "Inputs Cannot be Empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                auth(new Record(domain, username, password));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void addRecord(Record record) {
        recyclerViewAdapter.addRecord(record);
        recyclerViewAdapter.notifyDataSetChanged();
        databaseHandler.addRecord(record);
    }
}
