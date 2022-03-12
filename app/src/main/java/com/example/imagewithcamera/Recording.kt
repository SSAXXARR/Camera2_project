package com.example.imagewithcamera

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.spectr_sound.*
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

const val REQEST_CODE = 200
class Recording : AppCompatActivity(), Timer.OnTimerTickListener {
    private var permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spectr_sound)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0])== PackageManager.PERMISSION_GRANTED

        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQEST_CODE)
        }

        timer = Timer(this)
        btnRecord.setOnClickListener{
            when{
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
        }
    }

    private fun pauseRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.pause()
        }
        isPaused= true
        timer.pause()
    }
    private fun resumeRecorder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder.resume()
        }
        isPaused= false
        timer.start()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQEST_CODE){
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQEST_CODE)
            return
        }

        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())

        fileName = "media_recorder_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            }catch (ex: Exception){
                ex.stackTrace
            }
            start()
            //waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
        }
        isRecording = true
        isPaused = false

        timer.start()
    }
    private fun stopRecorder(){
        timer.stop()
    }

    override fun onTimerTick(duration: String) {
        tvTimer.text = duration
        Toast.makeText(this, (resources.displayMetrics.widthPixels).toString(), Toast.LENGTH_SHORT).show()
        waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}