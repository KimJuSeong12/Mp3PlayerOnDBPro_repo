package com.example.mp3playerondbpro


import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mp3playerondbpro.MainActivity.Companion.REQUEST_CODE
import com.example.mp3playerondbpro.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        val REQUEST_CODE = 100
        val DB_NAME = "musicDB2"
        val VERSION = 1
    }

    //*********************************************************************************************
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    //데이타베이스 생성
    private val dbOpenHelper by lazy { DBOpenHelper(this, DB_NAME, VERSION) }
    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    var musicDataList: MutableList<MusicData>? = mutableListOf<MusicData>()
    lateinit var musicRecyclerAdapter: MusicRecyclerAdapter
    //*********************************************************************************************

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //외장메모리 읽기 승인
        var flag = ContextCompat.checkSelfPermission(this, permission[0])
        if (flag == PackageManager.PERMISSION_GRANTED) {
            startProcess()
        } else {
            //승인요청
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
        }

        setSupportActionBar(binding.toolbar)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        // 메뉴에서 서치객체 찾음
        val searchMenu = menu?.findItem(R.id.menu_search)
        val searchView = searchMenu?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    musicDataList?.clear()
                    dbOpenHelper.selectAllmusicTBL()?.let { musicDataList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()
                } else {
                    musicDataList?.clear()
                    dbOpenHelper.searchMusic(newText)?.let { musicDataList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()
                }
                return true
            }

        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // 좋아요 된것만 가져오기
            R.id.menu_like -> {
                val musicDataLikeList = dbOpenHelper.selectMusicLike()
                if (musicDataLikeList == null) {
                    Log.e("MainActivity", "musicDataLikeList.size = 0")
                    Toast.makeText(applicationContext, "좋아요리스트가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    musicDataList?.clear()
                    dbOpenHelper.selectMusicLike()?.let { musicDataList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()
                }
            }
            R.id.menu_main -> {
                val musicDataAllList = dbOpenHelper.selectAllmusicTBL()
                if (musicDataAllList!!.size <= 0 || musicDataAllList == null) {
                    Toast.makeText(applicationContext, "모든리스트가 없습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    musicDataList?.clear()
                    dbOpenHelper.selectAllmusicTBL()?.let { musicDataList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startProcess()
            } else {
                Toast.makeText(this, "권한승인을 해야만 앱을 사용할 수 있어요.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startProcess() {
        // 데이타베이스를 조회해서 음악파일이 있다면, 음원정보를 가져와서 데이타베이스 입력했음을 뜻함
        // 데이타베이스를 조회해서 음악파일이 없다면, 음원정보를 가져와서 데이타베이스 입력하지 않음을 뜻함.
        //1. 데이타베이스에서 음원파일을 가져온다.
        var musicDataDBList: MutableList<MusicData>? = mutableListOf<MusicData>()
        musicDataDBList = dbOpenHelper.selectAllmusicTBL()
        Log.e("MainActivity", "musicDataList.size = ${musicDataDBList?.size}")

        if (musicDataDBList == null || musicDataDBList!!.size <= 0) {
            //start 음원정보를 가져옴 **********************************************
            val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
            )
            val cursor = contentResolver.query(musicUri, projection, null, null, null)

            if (cursor!!.count <= 0) {
                Toast.makeText(this, "메모리에 음악파일에 없습니다. 다운받아주세요.", Toast.LENGTH_SHORT).show()
                finish()
            }
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val title = cursor.getString(1).replace("'", "")
                val artist = cursor.getString(2).replace("'", "")
                val albumId = cursor.getString(3)
                val duration = cursor.getInt(4)
                val musicData = MusicData(id, title, artist, albumId, duration, 0)
                musicDataList?.add(musicData)
            }
            Log.e("MainActivity", "2 musicDataList.size = ${musicDataList?.size}")
            //end 음원정보를 가져옴 **********************************************
            //start 음원정보를 테이블에 insert 해야됨.
            var size = musicDataList?.size
            if (size != null) {
                for (index in 0..size - 1) {
                    val musicData = musicDataList!!.get(index)
                    dbOpenHelper.insertMusicTBL(musicData)
                }
            }

        } else {
            musicDataList = musicDataDBList
        }
        //Adapter와 recyclerview 연결
        Log.e("MainActivity", "음원정보를 연결해서 정보를 가져옴")
        musicRecyclerAdapter = MusicRecyclerAdapter(this, musicDataList!!)
        binding.recyclerView.adapter = musicRecyclerAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }
}