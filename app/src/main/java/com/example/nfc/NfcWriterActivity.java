package com.example.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;

public class NfcWriterActivity extends Activity {

    private NfcAdapter nfcAdapter;
    private String messageToWrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_writer);

        messageToWrite = getIntent().getStringExtra("message");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC not supported on this device.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            // Write the message to the NFC tag
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeToNfcTag(tag, messageToWrite);
        }
    }

    private void writeToNfcTag(Tag tag, String message) {
        NdefRecord record = NdefRecord.createTextRecord("en", message);
        NdefMessage ndefMessage = new NdefMessage(record);

        try {
            Ndef ndef = Ndef.get(tag);
            ndef.connect();
            if (!ndef.isWritable()) {
                Toast.makeText(this, "NFC Tag is read-only", Toast.LENGTH_SHORT).show();
                return;
            }
            ndef.writeNdefMessage(ndefMessage);
            Toast.makeText(this, "Data written to NFC tag", Toast.LENGTH_SHORT).show();
            ndef.close();
        } catch (IOException | FormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to write to NFC tag", Toast.LENGTH_SHORT).show();
        }
    }
}
