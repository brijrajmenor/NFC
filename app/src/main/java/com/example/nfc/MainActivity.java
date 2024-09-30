package com.example.nfc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {

    // Shared Preferences key
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_PASSWORD = "password"; // Key for stored password

    private NfcAdapter nfcAdapter;
    private Tag tag;
    private Button submitButton;
    private TextView welcomeText, totalPointsText, pointDayText, pointDisplayText, username_display_text;
    private EditText pointsInput, usernameInput, passwordInput;
    private ImageView logoImage, logoutButton;
    private Button resetButton;

    // Encrypts data using RC4
    private String rc4Encrypt(String data) {
        RC4 rc4 = new RC4("12345678"); // Key for RC4 encryption
        return rc4.encrypt(data);  // Encrypt and return the data
    }

    // Decrypts data using RC4
    private String rc4Decrypt(String encryptedData) {
        RC4 rc4 = new RC4("12345678"); // Key for RC4 decryption (same key as used for encryption)
        try {
            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = rc4.decrypt(encryptedBytes);
            return new String(decryptedBytes, "UTF-8"); // Convert to string, ensure valid output
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            Log.e("NFC", "Error decrypting string", e);
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if user is logged in
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);

        setContentView(R.layout.activity_main);

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Link UI components with the XML layout
        pointsInput = findViewById(R.id.points);
        submitButton = findViewById(R.id.submit);
        welcomeText = findViewById(R.id.welcome);
        pointDayText = findViewById(R.id.point_day);
        pointDisplayText = findViewById(R.id.point_display);
        logoImage = findViewById(R.id.imageView);
        username_display_text = findViewById(R.id.username_display);
        usernameInput = findViewById(R.id.username);
        logoutButton = findViewById(R.id.logout);
        resetButton = findViewById(R.id.reset);
        passwordInput = findViewById(R.id.password);

        resetButton.setOnClickListener(v -> {
            // Clear EditText fields
            pointsInput.setText("");
            username_display_text.setText("");

            // Reset displayed points
            pointDisplayText.setText("Total Points: 0");
            usernameInput.setVisibility(View.VISIBLE);
            // Optionally, clear username display
            TextView usernameDisplay = findViewById(R.id.username_display);
            usernameDisplay.setText("");
            usernameInput.setText("");
            passwordInput.setText("");
        });

        // Set up the NFC write button
        submitButton.setOnClickListener(v -> {
            if (passwordInput.getText().toString().equals("abc")) {
                submitPoints();
            } else {
                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
            }
        });

        logoutButton.setOnClickListener(v -> logout());

        // Check if NFC is available and enabled
        if (nfcAdapter == null) {
            // NFC is not supported on this device
            Toast.makeText(this, "NFC is not supported on this device.", Toast.LENGTH_LONG).show();
        } else {
            // NFC is available, check if it's enabled
            if (!nfcAdapter.isEnabled()) {
                // NFC is disabled, show a dialog to redirect to settings
                showNfcSettingsDialog();
            } else {
                // NFC is enabled, continue with the app logic
                Toast.makeText(this, "NFC is enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean validatePassword(String storedPassword) {
        String enteredPassword = passwordInput.getText().toString();
        return enteredPassword.equals(storedPassword);
    }

    // Method to set a new password (you can call this from a settings screen)
    public void setPassword(String newPassword) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PASSWORD, newPassword);
        editor.apply();
        Toast.makeText(this, "Password Updated", Toast.LENGTH_SHORT).show();
    }

    private void submitPoints() {
        writeNFC();
    }

    public void logout() {
        // Use the same SharedPreferences key
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();

        // Debugging: Check if the logout is triggered
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();  // Close the MainActivity
    }

    // Show a dialog to enable NFC in settings
    private void showNfcSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NFC Disabled")
                .setMessage("NFC is turned off. Do you want to go to settings and turn it on?")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Open NFC settings
                    Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "You need to turn on NFC to proceed.", Toast.LENGTH_SHORT).show();
                })
                .create()
                .show();
    }

    // Read NFC tag data
    public void readTagData() {
        if (tag != null) {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                try {
                    ndef.connect();
                    NdefMessage ndefMessage = ndef.getNdefMessage();
                    if (ndefMessage != null) {
                        NdefRecord[] records = ndefMessage.getRecords();
                        if (records.length > 0) {
                            NdefRecord record = records[0];
                            byte[] payload = record.getPayload();
                            if (payload != null && payload.length > 1) {  // Adjust for the language byte
                                // Skip the first byte (language code) and convert the rest to a Base64 string
                                String base64Data = new String(payload, "UTF-8");

                                Log.d("NFC", "Base64 Encrypted Data from Tag: " + base64Data);

                                // Decrypt the Base64 string
                                String decryptedData = rc4Decrypt(base64Data);  // Use RC4 decryption
                                Log.d("NFC", "Decrypted Data: " + decryptedData);

                                // Validate decrypted data format
                                if (decryptedData.contains(",")) {
                                    String[] decryptedDataParts = decryptedData.split(",");
                                    if (decryptedDataParts.length > 1) {
                                        int decryptedPoints = Integer.parseInt(decryptedDataParts[0]);
                                        String decryptedUsername = decryptedDataParts[1];

                                        // Update the UI with decrypted data
                                        runOnUiThread(() -> {
                                            pointDisplayText.setText("Total Points: " + decryptedPoints);
                                            username_display_text.setText("Welcome " + decryptedUsername);
                                            usernameInput.setVisibility(View.GONE);  // Hide username input field
                                        });
                                    }
                                } else {
                                    Log.e("NFC", "Invalid decrypted data format");
                                }
                            }
                        }
                    }
                } catch (IOException | FormatException e) {
                    Log.e("NFC", "Error reading from NFC tag", e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        if (ndef != null && ndef.isConnected()) {
                            ndef.close();
                        }
                    } catch (IOException e) {
                        Log.e("NFC", "Error closing NFC connection", e);
                    }
                }
            }
        }
    }

    // Write to NFC tag
    public void writeNFC() {
        if (tag != null) {
            String text = pointsInput.getText().toString();
            if (!text.isEmpty()) {
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    try {
                        ndef.connect();

                        // Fetch existing data
                        NdefMessage ndefMessage = ndef.getNdefMessage();
                        int existingPoints = 0;
                        String existingUsername = "";

                        if (ndefMessage != null) {
                            NdefRecord[] records = ndefMessage.getRecords();
                            if (records.length > 0) {
                                NdefRecord record = records[0];
                                byte[] payload = record.getPayload();
                                if (payload != null && payload.length > 0) {
                                    String decryptedData = rc4Decrypt(new String(payload, "UTF-8"));
                                    String[] dataParts = decryptedData.split(",");
                                    if (dataParts.length == 2) {
                                        existingPoints = Integer.parseInt(dataParts[0]);
                                        existingUsername = dataParts[1];
                                    }
                                }
                            }
                        }

                        int newPoints = Integer.parseInt(text);
                        String username = usernameInput.getText().toString();

                        // Handle cases where username is not provided
                        if (username.isEmpty()) {
                            username = existingUsername;  // Keep existing username if not provided
                        }

                        // Update and encrypt the data
                        int updatedPoints = existingPoints + newPoints;
                        String dataToEncrypt = updatedPoints + "," + username;
                        Log.d("NFC", "Data to Encrypt: " + dataToEncrypt);
                        String encryptedData = rc4Encrypt(dataToEncrypt);
                        Log.e("NFC", encryptedData);

                        // Create a new NdefRecord without extra metadata
                        NdefRecord record = NdefRecord.createExternal("myapp", "mydata", encryptedData.getBytes("UTF-8"));

                        // Write to the NFC tag
                        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
                        ndef.writeNdefMessage(message);

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Written to NFC tag", Toast.LENGTH_SHORT).show();
                            pointDisplayText.setText("Total Points: " + updatedPoints);
                        });

                    } catch (Exception e) {
                        Log.e("NFC", "Error writing to NFC tag", e);
                    } finally {
                        try {
                            if (ndef != null && ndef.isConnected()) {
                                ndef.close();
                            }
                        } catch (IOException e) {
                            Log.e("NFC", "Error closing NFC connection", e);
                        }
                    }
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(this, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    MainActivity.this.tag = tag;
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "NFC tag detected", Toast.LENGTH_SHORT).show());
                    readTagData();
                }
            }, NfcAdapter.FLAG_READER_NFC_A, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }
}