package neelts.coubs

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.transition.Fade
import android.transition.TransitionManager
import android.view.*
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
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
		const val COUBS = "http://coub.com/coubs"
		const val WRITE = Manifest.permission.WRITE_EXTERNAL_STORAGE
		const val AUDIO = "mp3"
		const val VIDEO = "mp4"
	}

	enum class TimelineType(val type: String, val text: String) {
		HOT_DAILY("timeline/hot", "Hot Daily"),
		HOT_WEEKLY("timeline/hot/weekly", "Hot Weekly"),
		HOT_MONTHLY("timeline/hot/monthly", "Hot Monthly")
	}

	private val timelines = arrayOf(
		TimelineType.HOT_MONTHLY,
		TimelineType.HOT_WEEKLY,
		TimelineType.HOT_DAILY
	)

	private var timeline: TimelineType = TimelineType.HOT_DAILY

	private var timelineIndex: Int = timelines.indexOf(timeline)
	private var index: Int = 0
	private var page: Int = 1

	private var coubs: Array<Coub> = emptyArray()

	private var id: Int = 0
	private var link: String? = null
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
		thread { getCoubs() }.join()
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
			thread { increment() }
		}
	}

	private fun increment() {
		"$COUBS/$link/increment_views".httpPost().responseString().third.fold(
			{ response -> println("views: $response") },
			{ error -> println(error) }
		)
	}

	private fun getCoubs(media: Boolean = true, extra: String = "") {
		"$COUB/${timeline.type}?page=$page$extra".httpGet().responseJson().third.fold(
			{ response -> run {
				val timeline = Gson().fromJson(response.content, Timeline::class.java)
				if (timeline.coubs.isNotEmpty()) {
					id = timeline.next
					coubs = coubs.plus(timeline.coubs)
				}
				if (media) getMedia()
			} },
			{ error -> println(error.toString()) }
		)
	}

	private fun getMedia() {

		if (index < coubs.size) {

			val coub = coubs[index]
			val html5 = coub.file_versions.html5
			val link = coub.permalink

			val videoFile = getFile(link, VIDEO)
			if (videoFile.exists()) {
				videoSource = buildMediaSource(Uri.fromFile(videoFile))
			} else {
				var url = html5.video.high?.url
				if (url == null) url = html5.video.med?.url
				url?.httpGet()?.response()?.third?.fold(
					{ data -> saveVideo(link, data) },
					{ error -> println(error.toString()) }
				)
			}

			val audioFile = getFile(link, AUDIO)
			if (audioFile.exists()) {
				audioSource = buildMediaSource(Uri.fromFile(audioFile))
			} else {
				var url = html5.audio.high?.url
				if (url == null) url = html5.audio.med?.url
				url?.httpGet()?.response()?.third?.fold(
					{ data -> saveAudio(link, data) },
					{ error -> println(error.toString()) }
				)
			}

			this.link = link
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
			KeyEvent.KEYCODE_DPAD_UP -> up()
			KeyEvent.KEYCODE_DPAD_DOWN -> down()
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
		hideNotice()
		if (index < coubs.size) {
			getNext()
		} else {
			page++
			thread { getCoubs(false, "&anchor=$id") }.join()
			getNext()
		}
		return true
	}

	private fun up(): Boolean {
		if (timelineIndex > 0) {
			timelineIndex--
			getTimeline()
		}
		return true
	}

	private fun down(): Boolean {
		if (timelineIndex < timelines.size - 1) {
			timelineIndex++
			getTimeline()
		}
		return true
	}

	private fun getTimeline() {
		timeline = timelines[timelineIndex]
		showNotice()
		caption.text = timeline.text
		reset()
		thread { getCoubs() }.join()
		getNext()
	}

	private fun reset() {
		if (coubs.isNotEmpty()) coubs = emptyArray()
	}

	private fun showNotice() {
		if (notice.visibility == View.INVISIBLE) {
			TransitionManager.beginDelayedTransition(notice, Fade(Fade.MODE_IN))
			notice.visibility = View.VISIBLE
		}
	}

	private fun hideNotice() {
		if (notice.visibility == View.VISIBLE) {
			TransitionManager.beginDelayedTransition(notice, Fade())
			notice.visibility = View.INVISIBLE
		}
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