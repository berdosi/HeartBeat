package org.duckdns.berdosi.heartbeat;

import android.Manifest;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.TextureView;
import android.view.Surface;

public class MainActivity extends AppCompatActivity {
    private final CameraService cameraService = new CameraService(this);

    private Canvas chartCanvas;

    private OutputAnalyzer analyzer = new OutputAnalyzer(this, chartCanvas);

    @Override
    protected void onResume() {
        super.onResume();

        chartCanvas = ((TextureView) findViewById(R.id.textureView)).lockCanvas();

        analyzer  = new OutputAnalyzer(this, chartCanvas);


        TextureView textureView = findViewById(R.id.textureView2);

        SurfaceTexture previewSurfaceTexture = textureView.getSurfaceTexture();
        if (previewSurfaceTexture != null) {
            // todo this first appears when we close the application and switch back - TextureView isn't quite ready at the first onResume.
            Surface previewSurface = new Surface(previewSurfaceTexture);

            cameraService.start(previewSurface);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraService.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }
}
