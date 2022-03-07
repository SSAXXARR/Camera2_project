package com.example.imagewithcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

import com.chibde.visualizer.LineBarVisualizer;
import com.chibde.visualizer.LineVisualizer;

public class MRecorder extends AppCompatActivity {
    public static final int AUDIO_PERMISSION_REQUEST_CODE = 102;

    public static final String[] WRITE_EXTERNAL_STORAGE_PERMS = {
            Manifest.permission.RECORD_AUDIO
    };

    protected MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spectr_sound);
        initialize();
        LineBarVisualizer lineBarVisualizer = findViewById(R.id.visualizer);
        mediaPlayer = MediaPlayer.create(this, R.raw.sample_src_main_res_raw_red_e);

// set custom color to the line.
        lineBarVisualizer.setColor(ContextCompat.getColor(this, R.color.av_green));

// define custom number of bars you want in the visualizer between (10 - 256).
        lineBarVisualizer.setDensity(70);

// Set you media player to the visualizer.
        lineBarVisualizer.setPlayer(mediaPlayer.getAudioSessionId());
    }

    private void initialize() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(WRITE_EXTERNAL_STORAGE_PERMS, AUDIO_PERMISSION_REQUEST_CODE);
        } else {
            setPlayer();
        }
    }


    private void setPlayer(){
        mediaPlayer = MediaPlayer.create(this, R.raw.sample_src_main_res_raw_red_e);
        mediaPlayer.setLooping(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
    }



    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setPlayer();
            } else {
                this.finish();
            }
        }
    }

}

        /*LineBarVisualizer lineBarVisualizer = findViewById(R.id.visualizer);
        //то что он должен проиграть. my music
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sample_src_main_res_raw_red_e);

// set custom color to the line.
        lineBarVisualizer.setColor(ContextCompat.getColor(this, R.color.av_red));

// define custom number of bars you want in the visualizer between (10 - 256).
        lineBarVisualizer.setDensity(100);

// Set you media player to the visualizer.
        lineBarVisualizer.setPlayer(mediaPlayer.getAudioSessionId()); //<---Error
    }*/


