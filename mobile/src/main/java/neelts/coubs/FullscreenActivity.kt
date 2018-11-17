package neelts.coubs

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_fullscreen.*
import neelts.coubs.api.Timeline
import kotlin.concurrent.thread

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class FullscreenActivity : AppCompatActivity() {

	companion object {
		const val COUB = "http://coub.com/api/v2"
		const val WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE
	}

	private var audioUri: Uri? = null
	private var videoUri: Uri? = null

	private lateinit var audioPlayer: SimpleExoPlayer
	private lateinit var videoPlayer: SimpleExoPlayer

	override fun onCreate(savedInstanceState: Bundle?) {

		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_fullscreen)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.hide()

		audioPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
		audioPlayer.playWhenReady = true

		videoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext)
		video.player = videoPlayer
		video.useController = false
		videoPlayer.playWhenReady = true

		audioPlayer.addListener(AudioLoopListener(audioPlayer, videoPlayer))
		videoPlayer.addListener(VideoLoopListener(videoPlayer))

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

	private fun start() {
		if (videoUri != null && audioUri != null) {
			audioPlayer.prepare(buildMediaSource(audioUri))
			videoPlayer.prepare(buildMediaSource(videoUri))
		}
	}

	private fun getHot() {
		"$COUB/timeline/hot".httpGet().responseJson().third.fold(
			{ coubs -> getURI(Gson().fromJson(coubs.content, Timeline::class.java)) },
			{ error -> println(error.toString()) }
		)
	}

	private fun getURI(timeline: Timeline) {
		val coub = timeline.coubs[0]
		val html5 = coub.file_versions.html5
		var url = html5.video.high?.url
		if (url == null) url = html5.video.med?.url
		url?.httpGet()?.response()?.third?.fold(
			{ data -> patch(coub.id.toString(), data) },
			{ error -> println(error.toString()) }
		)
		url = html5.audio.high?.url
		if (url == null) url = html5.audio.med?.url
		url?.httpGet()?.response()?.third?.fold(
			{ data -> saveAudio(coub.id.toString(), data) },
			{ error -> println(error.toString()) }
		)
	}

	private fun patch(file: String, data: ByteArray) {
		println(file)
		data[0] = 0
		data[1] = 0
		val videoFile = createTempFile(file, ".mp4", applicationContext.externalCacheDir)
		videoFile.writeBytes(data)
		videoUri = Uri.fromFile(videoFile)
	}

	private fun saveAudio(file: String, data: ByteArray) {
		val audioFile = createTempFile(file, ".mp3", applicationContext.externalCacheDir)
		audioFile.writeBytes(data)
		audioUri = Uri.fromFile(audioFile)
	}

	private fun buildMediaSource(uri: Uri?): ExtractorMediaSource {
		return ExtractorMediaSource.Factory(
			DefaultDataSourceFactory(applicationContext, applicationContext.packageName)
		).createMediaSource(uri)
	}

	override fun onStart() {
		super.onStart()
		start()
	}

	override fun onStop() {
		super.onStop()
		audioPlayer.stop()
		videoPlayer.stop()
	}

	override fun onDestroy() {
		super.onDestroy()
		audioPlayer.release()
		videoPlayer.release()
	}
}

class VideoLoopListener(private val video: SimpleExoPlayer) : Player.EventListener {
	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		if (playbackState == Player.STATE_ENDED) video.seekTo(0)
	}
}

class AudioLoopListener(private val audio: SimpleExoPlayer, private val video: SimpleExoPlayer) : Player.EventListener {
	override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
		if (playbackState == Player.STATE_ENDED) {
			audio.seekTo(0)
			video.seekTo(0)
		}
	}
}