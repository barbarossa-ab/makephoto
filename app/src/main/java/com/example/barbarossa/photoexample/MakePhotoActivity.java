package com.example.barbarossa.photoexample;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MakePhotoActivity extends Activity {
    public final static String DEBUG_TAG = "MakePhotoActivity";

    private int             mCameraId = -1;
    private Camera          mCamera;
    private CameraPreview   mPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_pick);

        // do we have a camera?
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG)
                    .show();
        } else {
            mCameraId = findFrontFacingCamera();
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                Log.d(DEBUG_TAG, "Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (mCameraId < 0) {
            Toast.makeText(this, "No front facing camera found.",
                    Toast.LENGTH_LONG).show();
        } else {
            if(safeCameraOpen(mCameraId)) {
                // Create our Preview view and set it as the content of our activity.
                mPreview = new CameraPreview(this, mCamera);
                FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
                preview.addView(mPreview);

            } else {
                Toast.makeText(this, "Could not open camera",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

    // I think Android Documentation recommends doing this in a separate
    // task to avoid blocking main UI
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        try {
            releaseCamera();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "failed to open Camera");
            e.printStackTrace();
        }
        return qOpened;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
            preview.removeView(mPreview);
            mCamera.release();
            mCamera = null;
        }
    }


    private class PhotoHandler implements Camera.PictureCallback {

        private final Context context;

        public PhotoHandler(Context context) {
            this.context = context;
        }

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFileDir = getDir();

            Log.e(DEBUG_TAG, "Inside onPictureTaken");

            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

                Log.d(MakePhotoActivity.DEBUG_TAG, "Can't create directory to save image.");
                Toast.makeText(context, "Can't create directory to save image.",
                        Toast.LENGTH_LONG).show();

                pictureFileDir = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String photoFile = "Picture_" + date + ".jpg";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;

            File pictureFile = new File(filename);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast.makeText(context, "New Image saved:" + photoFile,
                        Toast.LENGTH_LONG).show();

            } catch (Exception error) {
                Log.d(MakePhotoActivity.DEBUG_TAG, "File" + filename + "not saved: "
                        + error.getMessage());
                Toast.makeText(context, "Image could not be saved.",
                        Toast.LENGTH_LONG).show();
            }

            MakePhotoActivity.this.finish();
        }

        private File getDir() {
            File sdDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(sdDir, "CameraAPIDemo");
        }
    }


    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
//            releaseCamera();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

                mCamera.takePicture(null, null,
                        new PhotoHandler(getApplicationContext()));

            } catch (Exception e){
                Log.d(DEBUG_TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }

}