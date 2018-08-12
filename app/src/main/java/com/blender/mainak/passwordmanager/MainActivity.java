package com.blender.mainak.passwordmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.security.keystore.KeyProperties;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;

public class MainActivity extends AppCompatActivity {

    DatabaseHandler databaseHandler;
    RecyclerViewAdapter recyclerViewAdapter;
    List<Record> records;
    FingerprintAuth fingerprintAuth;
    byte[] iv = { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8 };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();
            }
        });

        databaseHandler = new DatabaseHandler(this, 7);

        refreshListView();

        fingerprintAuth = new FingerprintAuth(this);
    }

    public void cardOnClick(View cardView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setTitle("Add New Note");
        View view = getLayoutInflater().inflate(R.layout.record_view, null);

        TextView editTextDomainCard = cardView.findViewById(R.id.textView_domain);
        TextView editTextUsernameCard = cardView.findViewById(R.id.textView_username);
        TextView editTextPasswordCard = cardView.findViewById(R.id.textView_password);

        TextView editTextDomainAlertDialog = view.findViewById(R.id.textView_domain);
        TextView editTextUsernameAlertDialog = view.findViewById(R.id.textView_username);
        final TextView editTextPasswordAlertDialog = view.findViewById(R.id.textView_password);

        editTextDomainAlertDialog.setText(editTextDomainCard.getText());
        editTextUsernameAlertDialog.setText(editTextUsernameCard.getText());
        final String base64Password = editTextPasswordCard.getText().toString();
        editTextPasswordAlertDialog.setText(base64Password);

        builder.setView(view);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        /*builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });*/
        builder.show();

        fingerprintAuth.setOnAuthSuccessCallback(new OnAuthSuccess() {
            @Override
            public void run() {
                Log.i("MainActivity", "base64 password:" + base64Password);
                byte[] password = Base64.decode(base64Password, Base64.DEFAULT);
                Log.i("MainActivity", "byte array:" + new String(password));
                String base64Cypher = Base64.encodeToString(password, Base64.DEFAULT);
                Log.i("MainActivity", "base64 password:" + base64Cypher);
                try {
                    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    FileInputStream stream = openFileInput("MyKeyStore");
                    keyStore.load(stream, "123abc".toCharArray());
                    stream.close();
                    Key key = keyStore.getKey("myKeyAlias", "123abc".toCharArray());
                    byte[] planeText = decrypt(password, key, new IvParameterSpec(MainActivity.this.iv));
                    Log.i("MainActivity", "decrypted test:" + new String(planeText));

                    editTextPasswordAlertDialog.setText(new String(planeText));
                } catch (Exception e) {
                    Log.e("MainActivity", e.toString());
                }
            }
        });
        fingerprintAuth.auth();
        Toast.makeText(this, "Authenticate Fingerprint to View", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (databaseHandler != null) {
            databaseHandler.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final SearchView searchView = (SearchView)menu.findItem(R.id.menu_item_search).getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
//                search(query);
                Log.i("MainActivity", "searched:" + query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().length() == 0) {
                    search(".");
                }
                search(newText);
//                Log.i("MainActivity", "search typed:" + newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_search:
                Log.i("MainActivity", "search selected");
                break;
        }
        return true;
    }

    private void search(String searchQuery) {
        List<Record> searchList = databaseHandler.search(searchQuery);
        recyclerViewAdapter.clear();
        recyclerViewAdapter.addRecord(searchList);
    }

    private void refreshListView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        records = databaseHandler.getAllRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        recyclerViewAdapter = new RecyclerViewAdapter(records);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerViewAdapter);

        //implementing swipe to delete:
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                recyclerViewAdapter.removeRecord(viewHolder.getAdapterPosition(), databaseHandler);
            }

            @Override
            public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(0, LEFT | RIGHT);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void auth(final Record record) {
        fingerprintAuth.setOnAuthSuccessCallback(new OnAuthSuccess() {
            @Override
            public void run() {
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

                    //TODO: use Randomized IV for each encryption.
//                    SecureRandom rnd = new SecureRandom();
                    IvParameterSpec iv = new IvParameterSpec(MainActivity.this.iv); //using deterministic IV for now.

                    byte[]cypherText = encrypt(record.password.getBytes(), key, iv);
//                        byte[]decryptedText = decrypt(cypherText, key, iv);

                    Log.i("MainActivity", "plain text:" + record.password);
                    Log.i("MainActivity", "cypher text:" + new String(cypherText));
                    Log.i("MainActivity", "decrypted text:" + new String(decrypt(cypherText, key, iv)));

                    String base64Cypher = Base64.encodeToString(cypherText, Base64.DEFAULT);

                    addRecord(new Record(record.domain, record.username, base64Cypher));

                } catch (Exception e) {
                    Log.i("MainActivity", "err:" + e.toString());
                    e.printStackTrace();
                }
            }
        });
        fingerprintAuth.auth();

        Toast.makeText(getApplicationContext(), "Authenticate Fingerprint to View", Toast.LENGTH_SHORT).show();
    }

    private byte[] encrypt(byte[] plainText, Key key, IvParameterSpec iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(plainText);
    }

    private byte[] decrypt(byte[] cipherText, Key key, IvParameterSpec iv) throws Exception
    {
        Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(cipherText);
    }

    private void showInputDialog() {
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

    private void addRecord(Record record) {
        recyclerViewAdapter.addRecord(record);
        databaseHandler.addRecord(record);
    }
}
