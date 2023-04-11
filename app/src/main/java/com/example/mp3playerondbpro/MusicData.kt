package com.example.mp3playerondbpro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
class MusicData(
    var id: String,
    var title: String?,
    var artist: String?,
    var albumId: String?,
    var duration: Int?,
    var likes: Int?
) :
    Parcelable {
    companion object : Parceler<MusicData> {
        override fun create(parcel: Parcel): MusicData {
            return MusicData(parcel)
        }

        override fun MusicData.write(parcel: Parcel, flags: Int) {
            parcel.writeString(id)
            parcel.writeString(title)
            parcel.writeString(artist)
            parcel.writeString(albumId)
            parcel.writeInt(duration!!)
            parcel.writeInt(likes!!)
        }

    } // end of companion object

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readInt(),
    )

    // 음악 id를 통해서 음악파일 Uri를 가져오는 함수
    fun getMusicUri(): Uri =
        Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.id)

    // 음악앨범을 가져오는 Uri 함수
    fun getAlbumUri(): Uri = Uri.parse("content://media/external/audio/albumart/${this.albumId}")

    // 음악앨범을 Uri를 통해서 Bitmap으로 가져오는 함수
    fun getAlbumBitmap(context: Context, albumSize: Int): Bitmap? {
        val contentResolver = context.contentResolver
        val albumUri = getAlbumUri()
        val options = BitmapFactory.Options()
        var bitmap: Bitmap? = null
        var parcelFileDescriptor: ParcelFileDescriptor? = null

        try {
            if (albumUri != null) {
                parcelFileDescriptor = contentResolver.openFileDescriptor(albumUri, "r")
                bitmap = BitmapFactory.decodeFileDescriptor(
                    parcelFileDescriptor?.fileDescriptor,
                    null,
                    options
                )

                if (bitmap != null) {
                    val tempBitmap = Bitmap.createScaledBitmap(bitmap, albumSize, albumSize, true)
                    bitmap.recycle()
                    bitmap = tempBitmap
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("MusicData", e.toString())
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close()
                }
            } catch (e: java.lang.Exception) {
                Log.e("MusicData", e.toString())
            }
        }
        return bitmap
    }

}