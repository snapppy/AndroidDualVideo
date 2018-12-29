package com.example.warnerfamily.dualcameracapture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.media.MediaRecorder.VideoSource.CAMERA;

@SuppressWarnings("ALL")
public class MainActivity extends Activity {

    private Camera mBackCamera;
    private Camera mFrontCamera;
    private BackCameraPreview mBackCamPreview;
    private FrontCameraPreview mFrontCamPreview;
    private Button recordBtn;
    boolean recording = false;
    File file;
    boolean mInitSuccesful = false;

    SurfaceTexture sft;
    Surface sf;

    SurfaceTexture sft2;
    Surface sf2;

    MediaRecorder mediaRecorder1 = new MediaRecorder();
    MediaRecorder mediaRecorder2 = new MediaRecorder();

    public static String TAG = "DualCamActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_cam);
        recordBtn = findViewById(R.id.button);
        recordBtn.setOnClickListener(recordVideoListener);

        Log.i(TAG, "Number of cameras: " + Camera.getNumberOfCameras());

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        CAMERA);
        } else {
            Log.i(TAG, "Already have the permissions");

            mBackCamera = getCameraInstance(0);

            mBackCamPreview = new BackCameraPreview(this, mBackCamera);
            FrameLayout backPreview = (FrameLayout) findViewById(R.id.back_camera_preview);
            backPreview.addView(mBackCamPreview);

            mFrontCamera = getCameraInstance(1);
            mFrontCamPreview = new FrontCameraPreview(this, mFrontCamera);
            FrameLayout frontPreview = (FrameLayout) findViewById(R.id.front_camera_preview);
            frontPreview.addView(mFrontCamPreview);
            Log.i(TAG, "about to call prepare to record video...");
            prepareToRecordVideo();
        }

    }

    private View.OnClickListener recordVideoListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (recording) {
                //stop recording
                mediaRecorder1.stop();
                mediaRecorder1.reset();

                mediaRecorder2.stop();
                mediaRecorder2.reset();

                recording = false;
                recordBtn.setText("Start Recording");
                Log.i(TAG, "Stop recording ");
            } else {
                //start recording
                try {
                    mediaRecorder1.prepare();
                    mediaRecorder2.prepare();
                } catch (IOException e) {
                    Log.i(TAG, "Prepare failed" );
                }
                Log.i(TAG, "Start recording cam 1" );
                mediaRecorder1.start();

                Log.i(TAG, "Start recording cam 2" );
                mediaRecorder2.start();

                recording = true;
                recordBtn.setText("Stop Recording");
                Log.i(TAG, "Start recording " );
            }
        }
    };

    public void prepareToRecordVideo() {

        // start the recording stuff
        Log.i(TAG, "PREPARE TO RECORD VIDEO");
        mBackCamera.unlock();
        mFrontCamera.unlock();

        mediaRecorder1.setCamera(mBackCamera);
        mediaRecorder2.setCamera(mFrontCamera);

        /* Camera one */
        mediaRecorder1.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder1.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder1.setProfile(CamcorderProfile.get(0, CamcorderProfile.QUALITY_1080P));
        mediaRecorder1.setAudioSamplingRate(8000);
        mediaRecorder1.setOutputFile(this.initFile("CAMERA_1_").toString());

        //https://stackoverflow.com/questions/45221237/android-taking-video-error-22
        sft = new SurfaceTexture(0);
        sf = new Surface(sft);
        mediaRecorder1.setPreviewDisplay(sf);

        mediaRecorder1.setOnErrorListener(new MediaRecorder.OnErrorListener() {

            @Override
            public void onError(MediaRecorder mediaRecorder, int i, int i1) {
                Log.i(TAG, "RECORDING FAILED ERROR CODE: " + i + " AND EXTRA CODE: " + i1);
            }
        });

        /* Only one camera can access the mic at any given time.
         * In order to get around this, I saved the profile and extracted just the information I needed
         * while leaving any auditory information blank. This by default will keep the mic disabled.
         * Using the profile directly on the other hand, like on mediaRecorder1, will use the mic
         * with the settings saved in the cameraprofile.
         */
        CamcorderProfile cp = CamcorderProfile.get(1, CamcorderProfile.QUALITY_1080P);

        /* Camera two */
        mediaRecorder2.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder2.setOutputFormat(cp.fileFormat);
        mediaRecorder2.setVideoEncoder(cp.videoCodec);
        mediaRecorder2.setVideoEncodingBitRate(cp.videoBitRate);
        mediaRecorder2.setVideoFrameRate(cp.videoFrameRate);
        mediaRecorder2.setVideoSize(cp.videoFrameWidth, cp.videoFrameHeight);
        mediaRecorder2.setOutputFile(this.initFile("CAMERA_2_").toString());

        sft2 = new SurfaceTexture(0);
        sf2 = new Surface(sft2);
        mediaRecorder2.setPreviewDisplay(sf2);

        mInitSuccesful = true;
        Log.i(TAG, "RECORD INIT SUCCESSFUL " + mInitSuccesful);
    }

    private File initFile(String cameraNamePrefix) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "DualCameraCapture");


        if (!dir.exists() && !dir.mkdirs()) {
            Log.wtf(TAG,
                    "Failed to create storage directory: "
                            + dir.getAbsolutePath());
            Toast.makeText(this, "not record", Toast.LENGTH_SHORT);
            file = null;
        } else {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile;
                mediaFile = new File(dir.getPath() + File.separator +
                        cameraNamePrefix + timeStamp + ".mp4");
            Log.i(TAG, mediaFile.getAbsolutePath());
            return mediaFile;
        }


        return file;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    mBackCamera = getCameraInstance(0);
                    mFrontCamera = getCameraInstance(1);
                    Log.i(TAG, "about to call prepareToRecordVideo...");
                    prepareToRecordVideo();
                }
                return;
            }
        }
    }

    public static Camera getCameraInstance(int cameraId){
        Camera camera = null;
        try {
            camera = Camera.open(cameraId);
        }
        catch (Exception e){
            Log.e(TAG,"Camera " + cameraId + " is not available " + e.toString() );
        }
        return camera;

    }
}