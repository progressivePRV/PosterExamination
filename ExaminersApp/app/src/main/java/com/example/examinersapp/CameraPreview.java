package com.example.examinersapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraPreview extends AppCompatActivity {

    private static final String TAG = "okay";
    private int REQUEST_CODE_PERMISSIONS = 1111;
    private final String[] REQUIRED_PERMISSIONS = new String[]
            {"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};


    CountDownTimer cdt = null;
    TextView timer_tv;

    // for cameraX
    PreviewView previewView;

    // for qr code detector
    BarcodeScannerOptions options =
            new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build();

    BarcodeScanner scanner = BarcodeScanning.getClient(options);

    String qr_code_detector_result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        Log.d(TAG, "onCreate: called");

        //checking for permission
        if(AllPermissionsGranted()){
            Log.d(TAG, "onCreate: all permission granted");
            // use camera
            StartCamera();
            //Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
        }else{
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }

        //getting views
        previewView = findViewById(R.id.previewView);
        timer_tv = findViewById(R.id.timer_tv_in_camera_preview);

        //StartHandler();
        StartCountDownTimer();

    }

    @Override
    protected void onStop() {
        cdt.cancel();
        Intent i = new Intent();
        setResult(RESULT_CANCELED,i);
        super.onStop();
    }

    private void StartCountDownTimer() {
        Log.d(TAG, "StartCountDownTimer: called");
        cdt =  new CountDownTimer(30000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: ");
                timer_tv.setText(millisUntilFinished / 1000+" s");
            }

            @Override
            public void onFinish() {
                finish();
            }
        };
        cdt.start();
    }


    private void StartCamera() {

        Log.d(TAG, "StartCamera: called");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                BindPreview(cameraProvider);

            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void BindPreview(ProcessCameraProvider cameraProvider) {
        Log.d(TAG, "BindPreview: called");

        // this is for getting live image stream and to show it
        Preview preview =  new Preview.Builder().build();

        CameraSelector cameraSelector =  new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        // this is for analysing the image
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(680, 400))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();


        // this is for image analsys
        imageAnalysis.setAnalyzer( ContextCompat.getMainExecutor(this),new ImageAnalyser());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this,cameraSelector,imageAnalysis,preview);
    }

    class ImageAnalyser implements ImageAnalysis.Analyzer{

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            @SuppressLint("UnsafeExperimentalUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                // Pass image to an ML Kit Vision API
                // ...
                GiveQRCodeResult(image,imageProxy);
                Log.d(TAG, "analyze: am i getting resulr in analyzer result=>"+qr_code_detector_result);
                if(!qr_code_detector_result.isEmpty()){
                    // app got the qr code for what it was looking for
//                    int l = "com.example.examinersapp".length();  // lenght of starting text
//                    String text = qr_code_detector_result.substring(l);
                    Intent i = new Intent();
                    i.putExtra("result",qr_code_detector_result);
                    setResult(RESULT_OK,i);
                    finish();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){

            for (int i : grantResults){
                Log.d(TAG, "onRequestPermissionsResult: grantResult "+i);
                if(i != 0){
                    Log.d(TAG, "onRequestPermissionsResult: permission not granted");
                    Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            Log.d(TAG, "onRequestPermissionsResult: permission granted");
            //use camera
            StartCamera();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private boolean AllPermissionsGranted(){
        for (String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this,permission)
            != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }


    ////// for qr code dector
    void GiveQRCodeResult(InputImage image, ImageProxy imageProxy){
        Task<List<Barcode>> result = scanner.process(image)
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Barcode>> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "onComplete: qr code scan successful");
                            // get output from barcode
                            for (Barcode barcode: task.getResult()) {
                                //Rect bounds = barcode.getBoundingBox();
                                //Point[] corners = barcode.getCornerPoints();

                                String rawValue = barcode.getRawValue();
                                Log.d(TAG, "onComplete: in oncomplete listner raw value"+rawValue);
                                qr_code_detector_result = rawValue;
                                int valueType = barcode.getValueType();
                                Log.d(TAG, "onComplete: value type is=>"+valueType);

                            }
                        }else{
                            Log.d(TAG, "onComplete: qr code scanner error=>"+task.getException().getMessage());
                        }
                        imageProxy.close();
                    }
                });
    }

}