package neelts.coubs

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_fullscreen.*
import neelts.coubs.api.Coub
import neelts.coubs.api.Timeline
import java.io.File
import kotlin.concurrent.thread

class FullscreenActivity : Activity() {

	companion object {
		const val COUB = "http://coub.com/api/v2"
		const val WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE
		const val AUDIO = "mp3"
		const val VIDEO = "mp4"
	}

	private var index: Int = 0
	private var page: Int = 1

	private var coubs: Array<Coub> = emptyArray()

	private var audioSource: ExtractorMediaSource? = null
	private var videoSource: ExtractorMediaSource? = null

	private lateinit var audioPlayer: SimpleExoPlayer
	private lateinit var videoPlayer: SimpleExoPlayer

	override fun onCreate(savedInstanceState: Bundle?) {

		super.onCreate(savedInstanceState)

		requestWindowFeature(Window.FEATURE_NO_TITLE)
		window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
		setContentView(R.layout.activity_fullscreen)

		audioPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
		audioPlayer.playWhenReady = true

		videoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
		video.player = videoPlayer
		video.useController = false
		videoPlayer.playWhenReady = true

		audioPlayer.addListener(object : Player.EventListener {
			override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
				if (playbackState == Player.STATE_ENDED) {
					audioPlayer.seekTo(0)
					videoPlayer.seekTo(0)
				}
			}
		})

		videoPlayer.addListener(object : Player.EventListener {
			override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
				if (playbackState == Player.STATE_ENDED) videoPlayer.seekTo(0)
			}
		})

		if (ContextCompat.checkSelfPermission(this, WRITE) != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE)) {
			} else ActivityCompat.requestPermissions(this, arrayOf(WRITE), 1)
		} else init()
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) init()
	}

	private fun init() {
		thread { getHot() }.join()
		start()
	}

	private fun getNext() {
		audioSource = null
		videoSource = null
		stop()
		thread { getMedia() }.join()
		start()
	}

	private fun start() {
		if (videoSource != null && audioSource != null) {
			audioPlayer.prepare(audioSource)
			videoPlayer.prepare(videoSource)
		}
	}

	private fun getHot(media: Boolean = true) {
		"$COUB/timeline/hot?page=$page".httpGet().responseJson().third.fold(
			{ response -> run {
				val timeline = Gson().fromJson(response.content, Timeline::class.java)
				coubs = coubs.plus(timeline.coubs)
				if (media) getMedia()
			} },
			{ error -> println(error.toString()) }
		)
	}

	private fun getMedia() {

		if (index < coubs.size) {

			val coub = coubs[index]
			val html5 = coub.file_versions.html5
			val file = coub.id.toString()

			val videoFile = getFile(file, VIDEO)
			if (videoFile.exists()) {
				videoSource = buildMediaSource(Uri.fromFile(videoFile))
			} else {
				var url = html5.video.high?.url
				if (url == null) url = html5.video.med?.url
				url?.httpGet()?.response()?.third?.fold(
					{ data -> saveVideo(file, data) },
					{ error -> println(error.toString()) }
				)
			}

			val audioFile = getFile(file, AUDIO)
			if (audioFile.exists()) {
				audioSource = buildMediaSource(Uri.fromFile(audioFile))
			} else {
				var url = html5.audio.high?.url
				if (url == null) url = html5.audio.med?.url
				url?.httpGet()?.response()?.third?.fold(
					{ data -> saveAudio(file, data) },
					{ error -> println(error.toString()) }
				)
			}
		}
	}

	private fun saveVideo(file: String, data: ByteArray) {
		data[0] = 0
		data[1] = 0
		val videoFile = getFile(file, VIDEO)
		videoFile.writeBytes(data)
		videoSource = buildMediaSource(Uri.fromFile(videoFile))
	}

	private fun saveAudio(file: String, data: ByteArray) {
		val audioFile = getFile(file, AUDIO)
		audioFile.writeBytes(data)
		audioSource = buildMediaSource(Uri.fromFile(audioFile))
	}

	private fun buildMediaSource(uri: Uri?): ExtractorMediaSource {
		return ExtractorMediaSource.Factory(
			DefaultDataSourceFactory(applicationContext, applicationContext.packageName)
		).createMediaSource(uri)
	}

	private fun getFile(file:String, ext:String):File {
		return File(applicationContext.externalCacheDir, "$file.$ext")
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		return when (keyCode) {
			KeyEvent.KEYCODE_DPAD_LEFT -> left()
			KeyEvent.KEYCODE_DPAD_RIGHT -> right()
			else -> super.onKeyDown(keyCode, event)
		}
	}

	override fun onTouchEvent(event: MotionEvent?): Boolean {
		return when (event?.action) {
			MotionEvent.ACTION_DOWN -> if (event.x >= video.width * .5) right() else left()
			else -> super.onTouchEvent(event)
		}
	}

	private fun left(): Boolean {
		if (index > 0) index--
		getNext()
		return true
	}

	private fun right(): Boolean {
		index++
		if (index < coubs.size) {
			getNext()
		} else {
			page++
			thread { getHot(false) }.join()
			getNext()
		}
		return true
	}

	override fun onStart() {
		super.onStart()
		start()
	}

	override fun onStop() {
		stop()
		super.onStop()
	}

	private fun stop() {
		audioPlayer.stop()
		videoPlayer.stop()
	}

	override fun onDestroy() {
		audioPlayer.release()
		videoPlayer.release()
		super.onDestroy()
	}
}