package com.example.mp3playerondbpro

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.SeekBar
import com.example.mp3playerondbpro.databinding.ActivityPlayBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlayActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityPlayBinding
    val ALBUM_IMAGE_SIZE = 90
    var mediaPlayer: MediaPlayer? = null
    lateinit var musicData: MusicData
    private var playList: MutableList<Parcelable>? = null
    private var currentposition: Int = 0
    var mp3playerJob: Job? = null
    var pauseFlag = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //전달해온 intent 값을 가져옴.
//        musicData = intent.getSerializableExtra("musicData") as MusicData
        playList = intent.getParcelableArrayListExtra("parcelableList")
        currentposition = intent.getIntExtra("position", 0)
        musicData = playList?.get(currentposition) as MusicData

        //화면에 바인딩 진행
        binding.albumTitle.text = musicData.title
        binding.albumArtist.text = musicData.artist
        binding.totalDuration.text = SimpleDateFormat("mm:ss").format(musicData.duration)
        binding.playDuration.text = "00:00"
        val bitmap = musicData.getAlbumBitmap(this, ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.albumImage.setImageBitmap(bitmap)
        } else {
            binding.albumImage.setImageResource(R.drawable.music_24)
        }
        //음악파일객체 가져옴
        mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
        //이벤트처리(일시정지, 실행, 돌아가기, 정지, 시크바 조절)
        binding.listButton.setOnClickListener(this)
        binding.playButton.setOnClickListener(this)
        binding.stopButton.setOnClickListener(this)
        binding.nextButton.setOnClickListener(this)
        binding.backButton.setOnClickListener(this)
        binding.seekBar.max = mediaPlayer!!.duration
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.listButton -> {
                mp3playerJob?.cancel()
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                finish()
            }
            R.id.playButton -> {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer?.pause()
                    binding.playButton.setImageResource(R.drawable.play_24)
                    pauseFlag = true
                } else {
                    mediaPlayer?.start()
                    binding.playButton.setImageResource(R.drawable.pause)
                    pauseFlag = false
                    getCoroutine()
                }
            }
            R.id.stopButton -> {
                mediaPlayer?.stop()
                mp3playerJob?.cancel()
                mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
                binding.seekBar.progress = 0
                binding.playDuration.text = "00:00"
                binding.seekBar.max = mediaPlayer!!.duration
                binding.totalDuration.text = SimpleDateFormat("mm:ss").format(musicData.duration)
                binding.playButton.setImageResource(R.drawable.play_24)
            }

            R.id.nextButton -> {
                if (currentposition < playList!!.size - 1) {
                    currentposition++
                } else {
                    currentposition = 0
                }
                getMango()
                mediaPlayer?.start()
                getCoroutine()
            }

            R.id.backButton -> {
                if (currentposition > 0) {
                    currentposition--
                } else {
                    currentposition = playList!!.size - 1
                }
                getMango()
                mediaPlayer?.start()
                getCoroutine()
            }
        }
    }

    private fun getCoroutine() {
        val backgroundScope = CoroutineScope(Dispatchers.Default + Job())
        mp3playerJob = backgroundScope.launch {
            while (mediaPlayer!!.isPlaying) {
                var currentPosition = mediaPlayer?.currentPosition!!
                //코루틴속에서 화면의 값을 변동시키고자 할대 runOnUiThread
                runOnUiThread {
                    binding.seekBar.progress = currentPosition
                    binding.playDuration.text =
                        SimpleDateFormat("mm:ss").format(mediaPlayer?.currentPosition)
                }
                try {
                    delay(1000)
                } catch (e: java.lang.Exception) {
                    Log.e("PlayActivity", "delay 오류발생 ${e.printStackTrace()}")
                }
            }//end of while
            if (pauseFlag == false) {
                runOnUiThread {
                    binding.seekBar.progress = 0
                    binding.playDuration.text = "00:00"
                    binding.playButton.setImageResource(R.drawable.play_24)
                }
            }
        }//end of mp3PlayerJob
    }

    override fun onBackPressed() {
        mediaPlayer?.stop()
        mp3playerJob?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        finish()
    }

    fun getMango() {
        mediaPlayer?.stop()
        musicData = playList!!.get(currentposition) as MusicData
        mediaPlayer = MediaPlayer.create(this, musicData.getMusicUri())
        binding.seekBar.progress = 0
        binding.playDuration.text = "00:00"
        binding.seekBar.max = mediaPlayer?.duration ?: 0
        binding.totalDuration.text =
            SimpleDateFormat("mm:ss").format(musicData.duration)
        binding.albumTitle.text = musicData.title
        binding.albumArtist.text = musicData.artist
        binding.playButton.setImageResource(R.drawable.pause)
        val bitmap = musicData.getAlbumBitmap(this, ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.albumImage.setImageBitmap(bitmap)
        } else {
            binding.albumImage.setImageResource(R.drawable.music_24)
        }
        mediaPlayer?.start()
    }
}