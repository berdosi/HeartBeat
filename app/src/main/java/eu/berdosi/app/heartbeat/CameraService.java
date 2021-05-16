package eu.berdosi.app.heartbeat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Collections;
import java.util.Objects;

class CameraService {
    private String cameraId;
    private final Activity activity;
    private final Handler handler;
    private CameraDevice cameraDevice;
    private CameraCaptureSession previewSession;

    private CaptureRequest.Builder previewCaptureRequestBuilder;

    CameraService(Activity _activity, Handler _handler) {
        activity = _activity;
        handler = _handler;
    }

    void start(Surface previewSurface) {

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = Objects.requireNonNull(cameraManager).getCameraIdList()[0];
        } catch (CameraAccessException | NullPointerException | ArrayIndexOutOfBoundsException e) {
            Log.e("camera", "No access to camera", e);
            handler.sendMessage(Message.obtain(
                handler,
                MainActivity.MESSAGE_CAMERA_NOT_AVAILABLE,
                "No access to camera...."));
        }

        try {

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.println(Log.ERROR, "camera", "No permission to take photos");
                handler.sendMessage(Message.obtain(
                    handler,
                    MainActivity.MESSAGE_CAMERA_NOT_AVAILABLE,
                    "No permission to take photos"));
                return;
            }

            // message has been sent to MainActivity, this method can return.
            if (cameraId == null) {
                return;
            };

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
                        // deprecated in API 30, but changing it would bump minSdkVersion to 28.
                        //noinspection deprecation
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
                handler.sendMessage(Message.obtain(
                    handler,
                    MainActivity.MESSAGE_CAMERA_NOT_AVAILABLE,
                    e.getMessage()));
            }
        }
    }

    void stop() {
        try {
            cameraDevice.close();
        } catch (Exception e) {
            Log.println(Log.ERROR, "camera", "cannot close camera device" + e.getMessage());
        }
    }
}
