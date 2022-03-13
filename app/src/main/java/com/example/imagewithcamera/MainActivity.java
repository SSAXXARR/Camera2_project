package com.example.imagewithcamera;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.content.pm.PathPermission;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.LineBarVisualizer;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity implements Timer.OnTimerTickListener{
    ImageButton button;
    public TextureView textureView;
    private ArrayList<String> permissionsArrayList = new ArrayList<String>();
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 202;
    //для проверки задняя или передняя камера у нас вкл.
    private String cameraId = "0";
    //объект работающий с нашим девайсом.
    private CameraDevice cameraDevice;
    //Настроенный сеанс захвата для CameraDevice
    private CameraCaptureSession cameraCaptureSessions;
    //Неизменяемый пакет настроек и выходов, необходимых для захвата одного изображения с устройства камеры
    private CaptureRequest.Builder captureRequestBuilder;
    //размер экрана
    public Size[] imageDimension;
    // позволяет отправлять и обрабатывать Message и выполняемые объекты, связанные с потоком
    Handler mBackgroundHundler;
    HandlerThread mBackgroundThread;

    //Spectrum
    WaveformView waveform;
    String dirPath = "";
    String fileName = "";
    private MediaRecorder recorder;
    private boolean isRecording = false;
    private boolean isPaused = false;
    Timer timer;
    String file;
    TextView tvTimer;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        button = findViewById(R.id.button);
        tvTimer = findViewById(R.id.tvTimer);
        //таймер нужен для корректной работы отображения спектра.
        timer = new Timer(this);
        //startRecording();

        waveform = findViewById(R.id.waveformView);

        button.setOnClickListener(view -> {
            try {
                flipCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        });

    }
    //запуск микрофона и записи в файл, так же запуск таймера
    public void startRecording() {
        requestPerms();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
                recorder = new MediaRecorder();

                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                file = getRecordingFilePath();
                recorder.setOutputFile(file);

                try {
                    recorder.prepare();
                }catch (IOException e) {
                    e.printStackTrace();
                }
                recorder.start();
                timer.start();
        }
        isRecording = true;
        isPaused = false;

    }
    private void stopRecorder(){
        timer.stop();

    }
    //имплементированный метор таймера.
    // через него мы подбираем размер отображаемой амплитуды спектра.
    @Override
    public void onTimerTick(@NotNull String duration) {
        waveform.addAmplitude(recorder.getMaxAmplitude());
    }
    void pauseRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.pause();
        }
        isPaused= true;
        timer.pause();
    }
    void resumeRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.resume();
        }
        isPaused= false;
        timer.start();
    }
    //метод который записывает в файл нашу запись
    //и делает уникальное имя из даты и времени
    private String getRecordingFilePath(){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.DD_hh.mm.ss");
        File file = new File(musicDirectory, "textRecordingFile" + simpleDateFormat + "date"+ ".mp3");
        return file.getPath();
    }

    //метод, который меняет камеры
    private void flipCamera() throws CameraAccessException {
        if (cameraDevice != null && cameraId.equals("0")) {
            closeCamera();
            cameraId = "1";
            openCamera(cameraId);
        } else if (cameraDevice != null && cameraId.equals("1")) {
            closeCamera();
            cameraId = "0";
            openCamera(cameraId);
        }
    }

    //проверяет разрешение на использование камеры.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            //для камеры
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Sorry, camera permission is necessary", Toast.LENGTH_SHORT).show();
                finish();
            }
            //для микрофона
            else if(grantResults[1] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(), "Sorry, camera permission is necessary", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    //запрашивает все разрешения разом.
    public void requestPerms() {
        String[] permission = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, permission, 101);
        }
    }


    //срабатывает, когда наш textureView становится доступен и открывает камеру
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
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels / 2;
        Size sizeFinish = getOptimalSize(imageDimension, screenWidth, screenHeight);
        int cameraSizeW = sizeFinish.getWidth();
        int cameraSizeH = sizeFinish.getHeight();
        texture.setDefaultBufferSize(cameraSizeW, cameraSizeH);
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
                if (cameraDevice == null) {
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

    //обновление
    private void updatePreview() throws CameraAccessException {
        if (cameraDevice == null) {
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO);

        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHundler);
    }

    //ищет самый лучший размер камеры для размера превью (в нашем случает 50% экрана)
    public Size getOptimalSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;
        Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    //метод, который открывает камеру
    private void openCamera(String idCamera) throws CameraAccessException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //метод, который проверяет все разрешения разом.
            requestPerms();
            return;
        }
        //получаем доступ к камере через CameraManager.
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        //проверяем характеристики нашей камеры.
        CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraId);
        //Конфигурации потока с несколькими разрешениями,
        // поддерживаемые этой логической камерой или устройством с датчиком сверхвысокого разрешения.
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        //получаем размер с камеры какой идет на вывод
        imageDimension = map.getOutputSizes(SurfaceTexture.class);
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
        //запуск таймера.
        startRecording();

    }

    @Override
    protected void onPause() {
        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pauseRecorder();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecorder();
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
