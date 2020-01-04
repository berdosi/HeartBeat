package org.duckdns.berdosi.heartbeat;

import android.Manifest;
import android.graphics.SurfaceTexture;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.TextureView;
import android.view.Surface;

public class MainActivity extends AppCompatActivity {
    private final CameraService cameraService = new CameraService(this);

    private OutputAnalyzer analyzer;

    @Override
    protected void onResume() {
        super.onResume();

        analyzer  = new OutputAnalyzer(this, findViewById(R.id.graphTextureView));

        TextureView cameraTextureView = findViewById(R.id.textureView2);

        SurfaceTexture previewSurfaceTexture = cameraTextureView.getSurfaceTexture();
        if (previewSurfaceTexture != null) {
            // this first appears when we close the application and switch back - TextureView isn't quite ready at the first onResume.
            Surface previewSurface = new Surface(previewSurfaceTexture);

            cameraService.start(previewSurface);
            analyzer.measurePulse(cameraTextureView, cameraService);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraService.stop();
        if (analyzer != null ) analyzer.stop();
        analyzer  = new OutputAnalyzer(this, findViewById(R.id.graphTextureView));
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
