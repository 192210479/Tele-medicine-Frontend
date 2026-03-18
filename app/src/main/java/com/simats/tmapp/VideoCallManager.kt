package com.simats.tmapp

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.simats.tmapp.config.VideoConfig
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
    }

    fun initAgoraEngine(listener: VideoCallListener) {
        this.listener = listener
        try {
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = AgoraConfig.APP_ID
            config.mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    retryCount = 0
                    listener.onJoinChannelSuccess(channel, uid)
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    listener.onRemoteUserJoined(uid)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    listener.onRemoteUserOffline(uid)
                }

                override fun onError(err: Int) {
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
                }

                override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                    listener.onNetworkQuality(uid, txQuality, rxQuality)
                }

                override fun onRtcStats(stats: RtcStats) {
                    listener.onRtcStats(stats)
                }
            }
            mRtcEngine = RtcEngine.create(config)
            Log.d("AGORA", "Engine initialized")
            mRtcEngine?.enableVideo()
            
            // Resolution: 720p as requested
            val videoConfig = VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_1280x720,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
            )
            mRtcEngine?.setVideoEncoderConfiguration(videoConfig)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attemptReconnection() {
        if (retryCount < maxRetries) {
            retryCount++
            val token = lastToken
            val channel = channelName
            if (token != null && channel != null) {
                mRtcEngine?.joinChannel(token, channel, "", uid)
            }
        } else {
            listener?.onError(-100) // Custom error code for failed reconnection
        }
    }

    fun joinChannel(token: String, channel: String, userUid: Int) {
        this.channelName = channel
        this.uid = userUid
        this.lastToken = token
        
        mRtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        mRtcEngine?.enableVideo()
        mRtcEngine?.enableLocalVideo(true)
        
        Log.d("AGORA", "UID used: $userUid")
        Log.d("AGORA", "Channel name: $channel")
        Log.d("AGORA", "Token: $token")
        
        mRtcEngine?.joinChannel(token, channel, "", userUid)
    }

    fun setupLocalVideo(container: FrameLayout, userUid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(context)
        surfaceView.setZOrderMediaOverlay(true)
        container.removeAllViews()
        container.addView(surfaceView)
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, userUid))
    }

    fun startPreview() {
        Log.d("AGORA", "Preview started")
        mRtcEngine?.startPreview()
    }

    fun setupRemoteVideo(container: FrameLayout, remoteUid: Int) {
        val surfaceView = RtcEngine.CreateRendererView(context)
        container.removeAllViews()
        container.addView(surfaceView)
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
    }

    fun toggleMic(isMuted: Boolean) {
        mRtcEngine?.muteLocalAudioStream(isMuted)
    }

    fun toggleVideo(isPaused: Boolean) {
        mRtcEngine?.muteLocalVideoStream(isPaused)
    }

    fun switchCamera() {
        mRtcEngine?.switchCamera()
    }

    fun leaveChannel() {
        mRtcEngine?.stopPreview()
        mRtcEngine?.leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        channelName = null
        uid = 0
        lastToken = null
        retryCount = 0
    }

    fun getRtcEngine(): RtcEngine? = mRtcEngine
}
