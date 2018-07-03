package de.waldau_webdesign.app.projektmobileanwendungen;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import devliving.online.mvbarcodereader.MVBarcodeScanner;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE = 9001;
    private static final String TAG = "BarcodeMain";

    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference ref_sensors = database.getReference("projekt/sensors");

    @BindView(R.id.ivQrCode)
    ImageView ivQrCode;
    @BindView(R.id.status_message)
    TextView statusMessageView;
    @BindView(R.id.fab)
    FloatingActionButton fab;

    private MaterialDialog.Builder dialogBuilder;
    private MaterialDialog dialog;

    final static HashMap<Integer, String> TYPE_MAP;
    final static String[] barcodeTypeItems;
    private MVBarcodeScanner.ScanningMode mMode = MVBarcodeScanner.ScanningMode.SINGLE_AUTO;
    @MVBarcodeScanner.BarCodeFormat
    int[] mFormats = null;
    private Barcode mBarcode;
    private List<Barcode> mBarcodes;

    private String statusMessage = "";

    static {
        TYPE_MAP = new HashMap<>();

        TYPE_MAP.put(Barcode.ALL_FORMATS, "All Formats");
        TYPE_MAP.put(Barcode.QR_CODE, "QR Code");

        List<String> items = new ArrayList<>(TYPE_MAP.values());
        Collections.sort(items);
        String[] tempArray = new String[items.size()];
        tempArray = items.toArray(tempArray);
        barcodeTypeItems = tempArray;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        dialogBuilder = new MaterialDialog.Builder(this)
                .title(R.string.please_wait)
                .content(R.string.please_wait)
                .progress(true, 0);


        ivQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatusMessageView("");
                openBarcodeScanner();
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                updateStatusMessageView("");
                openBarcodeScanner();

            }
        });
    }

    /**
     * Hilfsmethode zum Erzeugen einer neuen zufaelligen ID in der Firebase Datenbank
     *
     * @param name Name des Sensors
     * @param desc Beschreibung des Sensors
     */
    private void createNewSensor(String name, String desc) {

        String qr_code_id = ref_sensors.push().getKey();

        ref_sensors.child(qr_code_id).child("name").setValue(name);
        ref_sensors.child(qr_code_id).child("description").setValue(desc);
        ref_sensors.child(qr_code_id).child("statusMessage").setValue("0");


    }

    /**
     * Oeffnet den Barcodescanner
     */
    private void openBarcodeScanner() {

        new MVBarcodeScanner.Builder()
                .setScanningMode(mMode)
                .setFormats(mFormats)
                .build()
                .launchScanner(this, REQ_CODE);
    }

    /**
     * Updated die QR-Code-Infos
     */
    private void updateQrCodeInfo() {

        if (mBarcodes == null) {
            Log.d("QR-Code", "no QR Code scanned");
            updateStatusMessageView("");
            return;
        }

        if (mBarcodes.size() >= 1) {

            Log.d("QR-Code", "QR Code scanned: " + mBarcodes.size());

            for (Barcode barcode : mBarcodes) {
                String qrCode = barcode.rawValue;
                Log.d("QR-Code", "QR-Code: " + qrCode);
            }

            updateFirebaseDatabase(mBarcodes, 1);

        }


    }

    /**
     * Setzt die Status Message
     *
     * @param status der aktuelle Status
     */
    private void updateStatusMessageView(String status) {
        statusMessageView.setText(status);
    }

    /**
     * Updated die Firebasedatenbank
     *
     * @param qrcodes Liste von qrcodes
     * @param value   Wert / Status der gesetzt werden soll
     */
    private void updateFirebaseDatabase(List<Barcode> qrcodes, int value) {


        openWaitDialog();
        final StringBuilder sb = new StringBuilder();

        Map<String, Object> statusUpdates = new HashMap<>();
        for (Barcode qrcode : qrcodes) {
            statusUpdates.put(qrcode.rawValue + "/status", value);
            sb.append(qrcode.rawValue);
        }

        statusMessage = "";

        ref_sensors.updateChildren(statusUpdates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if (databaseError != null) {
                    statusMessage = "Ein Fehler ist aufgetreten.";
                    Log.d(TAG, "Data could not be saved ");
                } else {
                    statusMessage = "Erfolgreich";
                    Log.d(TAG, "Data saved successfully.");
                }

                updateStatusMessageView(statusMessage);
                showSnackbar(statusMessage + " | QR-CODE: " + sb.toString());
            }
        });


        closeWaitDialog();
    }


    /**
     * Zeigt eine Snackbar an
     *
     * @param text Text der angezeigt werden soll
     */
    private void showSnackbar(String text) {
        Snackbar.make(findViewById(R.id.coordinatorLayout), text, Snackbar.LENGTH_LONG).setAction("Action", null).show();
    }

    /**
     * Oeffnet den Wait Dialog
     */
    private void openWaitDialog() {
        dialog = dialogBuilder.show();
    }

    /**
     * Schliesst den Wait Dialog
     */
    private void closeWaitDialog() {
        dialog.dismiss();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && data != null
                    && data.getExtras() != null) {
                Log.d("BARCODE-SCANNER", "onActivityResult inside block called");
                if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObject)) {
                    mBarcode = data.getParcelableExtra(MVBarcodeScanner.BarcodeObject);

                    mBarcodes = new ArrayList<>();
                    mBarcodes.add(mBarcode);
                } else if (data.getExtras().containsKey(MVBarcodeScanner.BarcodeObjects)) {
                    mBarcodes = data.getParcelableArrayListExtra(MVBarcodeScanner.BarcodeObjects);
                }
            } else {
                mBarcode = null;
                mBarcodes = null;
            }
            updateQrCodeInfo();
        }
    }


}
