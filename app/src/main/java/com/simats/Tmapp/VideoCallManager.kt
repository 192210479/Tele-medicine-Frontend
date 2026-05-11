package com.simats.Tmapp

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class VideoCallManager private constructor(private val context: Context) {

    private var mRtcEngine: RtcEngine? = null
    var channelName: String? = null
    var uid: Int = 0
    private var lastToken: String? = null
    private var retryCount = 0
    private val maxRetries = 3

    interface VideoCallListener {
        fun onRemoteUserJoined(uid: Int)
        fun onRemoteUserOffline(uid: Int)
        fun onJoinChannelSuccess(channel: String, uid: Int)
        fun onError(err: Int)
        fun onConnectionLost()
        fun onConnectionInterrupted()
        fun onConnectionStateChanged(state: Int, reason: Int)
        fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int)
        fun onRtcStats(stats: IRtcEngineEventHandler.RtcStats)
    }

    private var listener: VideoCallListener? = null

    companion object {
        @Volatile
        private var instance: VideoCallManager? = null

        fun getInstance(context: Context): VideoCallManager {
            return instance ?: synchronized(this) {
                instance ?: VideoCallManager(context.applicationContext).also { instance = it }
            }
        }

        // Call this only when the entire app session ends (e.g. logout), not on activity destroy
        fun destroyInstance() {
            synchronized(this) {
                instance?.destroyEngine()
                instance = null
            }
        }
    }

    fun initAgoraEngine(listener: VideoCallListener) {
        this.listener = listener

        // FIX: If engine already exists, just update listener and re-enable video
        // Do NOT recreate — prevents crash on repeated initAgoraEngine calls
        if (mRtcEngine != null) {
            Log.d("AGORA", "Engine already initialized, reusing")
            mRtcEngine?.enableVideo()
            return
        }

        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = AgoraConfig.APP_ID
            config.mEventHandler = object : IRtcEngineEventHandler() {

                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    retryCount = 0
                    this@VideoCallManager.uid = uid
                    listener.onJoinChannelSuccess(channel, uid)
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    listener.onRemoteUserJoined(uid)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    listener.onRemoteUserOffline(uid)
                }

                override fun onError(err: Int) {
                    Log.e("AGORA", "Engine error: $err")
                    listener.onError(err)
                }

                override fun onConnectionLost() {
                    listener.onConnectionLost()
                    attemptReconnection()
                }

                override fun onConnectionInterrupted() {
                    listener.onConnectionInterrupted()
                }

                override fun onConnectionStateChanged(state: Int, reason: Int) {
                    listener.onConnectionStateChanged(state, reason)
                    // FIX: Auto-reconnect on disconnected state with token expired reason
                    if (state == Constants.CONNECTION_STATE_DISCONNECTED && reason == Constants.CONNECTION_CHANGED_INTERRUPTED) {
                        attemptReconnection()
                    }
                }

                override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                    listener.onNetworkQuality(uid, txQuality, rxQuality)
                }

                override fun onRtcStats(stats: RtcStats) {
                    listener.onRtcStats(stats)
                }
            }

            mRtcEngine = RtcEngine.create(config)
            Log.d("AGORA", "Engine initialized fresh")

            mRtcEngine?.enableVideo()

            // 720p video config
            val videoConfig = VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
            mRtcEngine?.setVideoEncoderConfiguration(videoConfig)

        } catch (e: Exception) {
            Log.e("AGORA", "Failed to init engine: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun attemptReconnection() {
        if (retryCount < maxRetries) {
            retryCount++
            Log.d("AGORA", "Attempting reconnection #$retryCount")
            val token = lastToken
            val channel = channelName
            if (!token.isNullOrEmpty() && !channel.isNullOrEmpty()) {
                mRtcEngine?.joinChannel(token, channel, "", uid)
            }
        } else {
            Log.e("AGORA", "Max retries reached, giving up")
            listener?.onError(-100)
        }
    }

    fun joinChannel(token: String, channel: String, userUid: Int) {
        this.channelName = channel
        this.uid = userUid
        this.lastToken = token

        mRtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        mRtcEngine?.enableVideo()
        mRtcEngine?.enableLocalVideo(true)

        Log.d("AGORA", "Joining — UID: $userUid  Channel: $channel")

        mRtcEngine?.joinChannel(token, channel, "", userUid)
    }

    fun setupLocalVideo(container: FrameLayout, userUid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(context)
        surfaceView.setZOrderMediaOverlay(true)
        container.removeAllViews()
        container.addView(surfaceView)
        // FIX: Local video always uses uid = 0 in setupLocalVideo
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    fun startPreview() {
        Log.d("AGORA", "Starting preview")
        mRtcEngine?.startPreview()
    }

    fun setupRemoteVideo(container: FrameLayout, remoteUid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(context)
        container.removeAllViews()
        container.addView(surfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
    }

    fun setMicEnabled(isEnabled: Boolean) {
        mRtcEngine?.muteLocalAudioStream(!isEnabled)
    }

    fun setVideoEnabled(isEnabled: Boolean) {
        mRtcEngine?.muteLocalVideoStream(!isEnabled)
    }

    // Legacy names kept for backward compat
    fun toggleMic(isMuted: Boolean) {
        mRtcEngine?.muteLocalAudioStream(isMuted)
    }

    fun toggleVideo(isPaused: Boolean) {
        mRtcEngine?.muteLocalVideoStream(isPaused)
    }

    fun switchCamera() {
        mRtcEngine?.switchCamera()
    }

    /**
     * Call this when leaving a video call screen.
     * Does NOT destroy the engine — engine stays alive for the session.
     */
    fun leaveChannel() {
        mRtcEngine?.stopPreview()
        mRtcEngine?.leaveChannel()
        channelName = null
        uid = 0
        lastToken = null
        retryCount = 0
        listener = null
        Log.d("AGORA", "Channel left")
    }

    /**
     * Fully destroys the RtcEngine.
     * Call ONLY from destroyInstance() — never from an Activity's onDestroy.
     */
    private fun destroyEngine() {
        leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        Log.d("AGORA", "Engine destroyed")
    }

    fun getRtcEngine(): RtcEngine? = mRtcEngine
}