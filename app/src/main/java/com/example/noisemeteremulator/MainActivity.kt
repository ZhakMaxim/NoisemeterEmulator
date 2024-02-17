package com.example.noisemeteremulator
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    private var audioRecord: AudioRecord? = null
    private val minBufferSize = AudioRecord.getMinBufferSize(
        44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT  //3584
    )
    private val buffer = ShortArray(minBufferSize)
    private var isMeasuring = false
    private val handler = Handler(Looper.getMainLooper())
    var noiseLevelArr: MutableList<Int> = ArrayList()

    @SuppressLint("SetTextI18n")
    private val updateNoiseLevel = object : Runnable {
        override fun run() {
            if (isMeasuring) {
                val noiseLevel = calculateNoiseLevel()
                val tv = findViewById<TextView>(R.id.NoiseLvlTextView)
                tv.text = "$noiseLevel дБ"
                noiseLevelArr += noiseLevel
                handler.postDelayed(this, 400)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var noiseLvlTV = findViewById<TextView>(R.id.NoiseLvlTextView)
        var tgl = findViewById<ToggleButton>(R.id.OnOffButton)
        var AvgNoiseLvlTV = findViewById<TextView>(R.id.MaxNoiseLvlTextView)
        var minNoiseLvlTV = findViewById<TextView>(R.id.MinNoiseLvlTextView)
        var maxNoiseLvlTV = findViewById<TextView>(R.id.MaxNoiseLvlTextView)
        var tv3 = findViewById<TextView>(R.id.textView3)
        var tv = findViewById<TextView>(R.id.textView)
        var tv4 = findViewById<TextView>(R.id.textView4)
        var tv6 = findViewById<TextView>(R.id.textView6)
        tv3.visibility = View.INVISIBLE
        tv.visibility = View.INVISIBLE
        tv4.visibility = View.INVISIBLE
        tv6.visibility = View.INVISIBLE
        minNoiseLvlTV.visibility = View.INVISIBLE
        maxNoiseLvlTV.visibility = View.INVISIBLE

        tgl.setOnCheckedChangeListener{_, isChecked ->
             if(isChecked){
                 AvgNoiseLvlTV.text = ""
                 tv.visibility = View.VISIBLE
                 tv3.visibility = View.INVISIBLE
                 minNoiseLvlTV.visibility = View.INVISIBLE
                 maxNoiseLvlTV.visibility = View.INVISIBLE
                 tv4.visibility = View.INVISIBLE
                 tv6.visibility = View.INVISIBLE
                 val permission = ContextCompat.checkSelfPermission(this,
                     Manifest.permission.READ_PHONE_STATE)

                 if (permission != PackageManager.PERMISSION_GRANTED) {
                     ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
                 }

                 audioRecord = AudioRecord(
                     MediaRecorder.AudioSource.MIC, 44100,
                     AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize
                 )
                 if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                     audioRecord?.startRecording()
                     isMeasuring = true
                     handler.post(updateNoiseLevel)
                 } else {
                     Toast.makeText(this, "Audio initialization failed", Toast.LENGTH_SHORT).show()
                 }
             }
             else {
                 isMeasuring = false
                 noiseLvlTV.text = "Средний уровень шума"
                 tv.visibility = View.INVISIBLE
                 tv3.visibility = View.VISIBLE
                 minNoiseLvlTV.visibility = View.VISIBLE
                 maxNoiseLvlTV.visibility = View.VISIBLE
                 tv4.visibility = View.VISIBLE
                 tv6.visibility = View.VISIBLE
                 noiseLvlTV.text ="${noiseLevelArr.sum()/noiseLevelArr.size} дБ"
                 val minNoiseLvl = noiseLevelArr.min()
                 val maxNoiseLvl = noiseLevelArr.max()
                 minNoiseLvlTV.text = minNoiseLvl.toString() + "дБ"
                 maxNoiseLvlTV.text = maxNoiseLvl.toString() + "дБ"
                 noiseLevelArr.clear()
             }
        }
    }

    private fun calculateNoiseLevel(): Int {
        audioRecord?.read(buffer, 0, buffer.size)
        var sum = 0.0
        val bufferSize = buffer.size

        for (i in 0 until bufferSize) {
            sum += buffer[i] * buffer[i].toDouble()
        }

        val rms = Math.sqrt(sum / bufferSize)
        val referencePressure = 2.0e-5 // Стандартное акустическое давление (0 дБ)
        val pressure = rms / referencePressure
        val db = 6.2 * Math.log10(pressure)

        return db.roundToInt()
    }
}