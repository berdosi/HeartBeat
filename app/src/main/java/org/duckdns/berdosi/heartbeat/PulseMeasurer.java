package org.duckdns.berdosi.heartbeat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

class PulseMeasurer {
    private String cameraId;
    private final Activity activity;
    private CameraDevice cameraDevice;
    private CameraCaptureSession previewSession;
    private MeasureStore store;

    private final int measurementInterval = 50;
    private final int measurementLength = 15000; // ensure the number of data points is the power of two
    private final int clipLength = 5000;
    private final float dropHeight = 0.15f;


    private int ticksPassed = 0;

    private CaptureRequest.Builder previewCaptureRequestBuilder;

    PulseMeasurer(Activity _activity) {
        activity = _activity;
    }

    void start(Surface previewSurface) {

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = Objects.requireNonNull(cameraManager).getCameraIdList()[0];
        } catch (CameraAccessException | NullPointerException e) {
            Log.println(Log.ERROR, "camera", "No access to camera....");
        }

        try {

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.ERROR, "camera", "No permission to take photos");
            }
            Objects.requireNonNull(cameraManager).openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;

                    CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            previewSession = session;
                            try {

                                previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                previewCaptureRequestBuilder.addTarget(previewSurface); // this is previewSurface
                                previewCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);

                                HandlerThread thread = new HandlerThread("CameraPreview");
                                thread.start();

                                previewSession.setRepeatingRequest(previewCaptureRequestBuilder.build(), null, null);

                            } catch (CameraAccessException e) {
                                if (e.getMessage() != null) {
                                    Log.println(Log.ERROR, "camera", e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.println(Log.ERROR, "camera", "Session configuration failed");
                        }
                    };

                    try {
                        camera.createCaptureSession(Collections.singletonList(previewSurface), stateCallback, null); //1
                    } catch (CameraAccessException e) {
                        if (e.getMessage() != null) {
                            Log.println(Log.ERROR, "camera", e.getMessage());
                        }
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                }
            }, null);
        } catch (CameraAccessException | SecurityException e) {
            if (e.getMessage() != null) {
                Log.println(Log.ERROR, "camera", e.getMessage());
            }
        }
    }

    void measurePulse(TextureView textureView) {

        // 10 times a second, get the amount of red on the picture.
        // over the past 5 seconds, get the minimum, maximum, and the standardized values
        CountDownTimer timer;

        Log.i("measure", "started");
        store = new MeasureStore();

        timer = new CountDownTimer(measurementLength, measurementInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                // skip the first measurements, which are broken by exposure metering
                if (clipLength > (++ticksPassed * measurementInterval) ) return;

                Bitmap currentBitmap = textureView.getBitmap();
                int pixelCount = textureView.getWidth() * textureView.getHeight();
                int measurement = 0;
                int[] pixels = new int[pixelCount];
                currentBitmap.getPixels(pixels, 0, textureView.getWidth(), 0, 0, textureView.getWidth(), textureView.getHeight());

                // extract the red component
                // https://developer.android.com/reference/android/graphics/Color.html#decoding
                for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
                    measurement +=(pixels[pixelIndex] >> 16) & 0xff;
                }
                // max int is 2^31 (2147483647) , so width and height can be at most 2^11,
                // as 2^8 * 2^11 * 2^11 = 2^30, just below the limit

                store.add(measurement);
            }

            @Override
            public void onFinish() {
                ArrayList<Measurement<Float>> stdValues = store.getStdValues();
                StringBuilder returnValueSb = new StringBuilder();


                // look for "drops" of 0.15 - 0.75 in the value
                // a drop may take 2-3 ticks.
                int dropCount = 0;
                for (int stdValueIdx = 4; stdValueIdx < stdValues.size(); stdValueIdx++) {
                    if (((stdValues.get(stdValueIdx - 2).measurement - stdValues.get(stdValueIdx).measurement) > dropHeight) &&
                            !((stdValues.get(stdValueIdx - 3).measurement - stdValues.get(stdValueIdx -1).measurement) > dropHeight) &&
                            !((stdValues.get(stdValueIdx - 4).measurement - stdValues.get(stdValueIdx -2).measurement) > dropHeight)
                    )
                    {
                        dropCount++;
                    }
                }

                returnValueSb.append(activity.getString(R.string.detected_pulse));
                returnValueSb.append(activity.getString(R.string.separator));
                returnValueSb.append((float)dropCount / ((float)(measurementLength - clipLength) / 1000f / 60f));
                returnValueSb.append(activity.getString(R.string.row_separator));

                // building a double[] could help if Fourier transformation worked.
                // But, considering we need 2^n data points, it is more of a hassle.
                 double[] stdValuesDoubleArray = new double[(int) Math.pow(2, Math.ceil(Math.log(stdValues.size()) / Math.log(2)))];


                 int doubleArrayIndex = 0;
                for (int stdValueIdx = 0; stdValueIdx < stdValues.size(); stdValueIdx++) {
                    Measurement<Float> value = stdValues.get(stdValueIdx);
                    // stdValues.forEach((value) -> {
                    returnValueSb.append(value.timestamp.getTime());
                    returnValueSb.append(activity.getString(R.string.separator));
                    returnValueSb.append(value.measurement);
                    returnValueSb.append(activity.getString(R.string.row_separator));

                     stdValuesDoubleArray[doubleArrayIndex++] = (double) value.measurement;
                    // });
                }

                returnValueSb.append(activity.getString(R.string.fourier));
                returnValueSb.append(activity.getString(R.string.row_separator));


                 FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.UNITARY);
                 Complex[] transformationResult = transformer.transform(stdValuesDoubleArray, TransformType.FORWARD);

                 for (Complex complex : transformationResult) {
                     returnValueSb.append(complex.getReal());
                     returnValueSb.append(activity.getString(R.string.separator));
                     returnValueSb.append(complex.getImaginary());
                     returnValueSb.append(activity.getString(R.string.row_separator));
                 }

                ((EditText)activity.findViewById(R.id.editText)).setText(returnValueSb.toString());

            }
        };

        timer.start();
    }

    void stop() {
        try {
            cameraDevice.close();
        } catch (Exception e) {
            Log.println(Log.ERROR, "camera", "cannot close camera device" + e.getMessage());
        }
    }
}
