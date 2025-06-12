package com.app.qrcodegenerator;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

public class MainActivity extends AppCompatActivity {

    private EditText etInputText;
    private ImageView ivQRCode;
    private Button btnGenerateQR, btnSaveQR, btnShareQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etInputText = findViewById(R.id.etInputText);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnSaveQR = findViewById(R.id.btnSaveQR);
        btnShareQR = findViewById(R.id.btnShareQR); // Added button reference

        // Generate QR code
        btnGenerateQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = etInputText.getText().toString();

                if (!inputText.isEmpty()) {
                    generateQRCode(inputText);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter text", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Save QR code to gallery
        btnSaveQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) ivQRCode.getDrawable()).getBitmap();
                saveImageToGallery(bitmap);
            }
        });

        // Share QR code
        btnShareQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ((BitmapDrawable) ivQRCode.getDrawable()).getBitmap();
                shareImage(bitmap);
            }
        });
    }

    // Generate QR Code
    private void generateQRCode(String text) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();
            hints.put(EncodeHintType.MARGIN, "1");

            Bitmap bitmap = encodeAsBitmap(text, writer, BarcodeFormat.QR_CODE, 500, 500, hints);
            ivQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(MainActivity.this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    // Convert text to Bitmap QR code
    private Bitmap encodeAsBitmap(String contents, MultiFormatWriter writer, BarcodeFormat format, int width, int height, Hashtable<EncodeHintType, String> hints) throws WriterException {
        BitMatrix result = writer.encode(contents, format, width, height, hints);
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    // Save QR code to Gallery
    private void saveImageToGallery(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "qr_code_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/QR_Codes");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                Toast.makeText(MainActivity.this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to save QR Code", Toast.LENGTH_SHORT).show();
            }
        } else {
            String path = Environment.getExternalStorageDirectory().toString() + "/QR_Codes";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "qr_code_" + System.currentTimeMillis() + ".png");

            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                Toast.makeText(MainActivity.this, "QR Code saved to gallery!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Failed to save QR Code", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Share QR code via Intent
    private void shareImage(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "shared_qr_code.png");
            try (FileOutputStream stream = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }

            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            if (contentUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/png");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to share QR Code", Toast.LENGTH_SHORT).show();
        }
    }
}
