package com.example.imagewithcamera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;


public class MainActivity extends AppCompatActivity {
    Button button;
    public TextureView textureView;
    //check state orientation of output image
    /*private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }*/
    //для проверки задняя или передняя камера у нас вкл.
    private String cameraId = "0";
    //объект работающий с нашим девайсом.
    private CameraDevice cameraDevice;
    //Настроенный сеанс захвата для CameraDevice
    private CameraCaptureSession cameraCaptureSessions;
    //Неизменяемый пакет настроек и выходов, необходимых для захвата одного изображения с устройства камеры
    private CaptureRequest.Builder captureRequestBuilder;
    //размер экрана
    private Size imageDimension;
    // позволяет отправлять и обрабатывать Message и выполняемые объекты, связанные с потоком
    Handler mBackgroundHundler;

    HandlerThread mBackgroundThread;

    /*WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();*/



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    flipCamera();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void flipCamera() throws CameraAccessException {
        if(cameraDevice != null && cameraId.equals("0")){
            closeCamera();
            cameraId = "1";
            openCamera(cameraId);
        }
        else if(cameraDevice != null && cameraId.equals("1")){
            closeCamera();
            cameraId = "0";
            openCamera(cameraId);
        }
    }

    //проверяет разрешение./////////////////////////////////
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 101){
            if(grantResults[1] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(getApplicationContext(), "Sorry, camera permission is necessary", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //срабатывает, когда наш textureView становится доступен.
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            try {
                openCamera(cameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    //предназначен для получения обновлений о состоянии устройства камеры
    private final CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    //создаем предварительный просмотр камеры, ширина, высота устройства и что поменялось
    private void createCameraPreview() throws CameraAccessException {
        SurfaceTexture texture = textureView.getSurfaceTexture();
        texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
        Surface surface = new Surface(texture);
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        captureRequestBuilder.addTarget(surface);
        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                if(cameraDevice == null){
                    return;
                }
                cameraCaptureSessions = session;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                Toast.makeText(getApplicationContext(), "Configuration change", Toast.LENGTH_LONG).show();
            }
        }, null);
    }

    private void updatePreview() throws CameraAccessException {
        if(cameraDevice == null){
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHundler);
    }

//////
    private void openCamera(String idCamera) throws CameraAccessException {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            //PackageManager.PERMISSION_GRANTED == разрешение есть.
            //запрашиваем разрешение, если его нет с помощью requestPermissions
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 101);
            return;
        }
        //получаем доступ к камере через CameraManager.
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //берем определенную камеру (задняя 0, передняя 1)
        /*cameraId = manager.getCameraIdList()[0];*/
        //проверяем характеристики нашей камеры.
        CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
        //Конфигурации потока с несколькими разрешениями,
        // поддерживаемые этой логической камерой или устройством с датчиком сверхвысокого разрешения.
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        imageDimension = map.getOutputSizes(SurfaceTexture.class)[Integer.parseInt(cameraId)];
        //проверяем есть ли у нас доступ к камере
        //На вход метод требует Context и название разрешения.
        // Он вернет константу PackageManager.PERMISSION_GRANTED (если разрешение есть)
        // или PackageManager.PERMISSION_DENIED (если разрешения нет).


        manager.openCamera(cameraId, stateCallBack, null);
    }


    public void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable()){
            try {
                openCamera(cameraId);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        else{
            textureView.setSurfaceTextureListener(textureListener);
        }

    }

    @Override
    protected void onPause() {
        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onPause();
    }

    protected void stopBackgroundThread() throws InterruptedException {
        //Безопасно завершает работу петлителя потока обработчика
        mBackgroundThread.quitSafely();
        //
        mBackgroundThread.join();
        mBackgroundThread = null;
        mBackgroundHundler = null;
    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera background");
        mBackgroundThread.start();
        mBackgroundHundler = new Handler(mBackgroundThread.getLooper());
    }
}