package com.simats.tmapp

import android.content.Context
import android.util.Log
import com.simats.tmapp.api.ApiClient
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import org.json.JSONObject

/**
 * SocketService manages the global Socket.IO connection for the TeleHealth+ app.
 * It ensures a single instance and handles automatic reconnection and room joining.
 */
class SocketService private constructor(context: Context) {
    private val sessionManager = SessionManager.getInstance(context)
    private var mSocket: Socket? = null
    private val contextRef = context.applicationContext

    companion object {
        @Volatile
        private var instance: SocketService? = null

        fun getInstance(context: Context): SocketService {
            return instance ?: synchronized(this) {
                instance ?: SocketService(context.applicationContext).also { instance = it }
            }
        }

        val socket: Socket?
            get() = instance?.getSocket()
    }

    fun connect() {
        val userId = sessionManager.getUserId()
        if (userId == -1) {
            Log.d("SocketService", "No user ID found, skipping connection.")
            return
        }

        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.transports = arrayOf("websocket")
                opts.reconnection = true
                opts.reconnectionAttempts = Int.MAX_VALUE
                opts.reconnectionDelay = 2000

                mSocket = IO.socket(ApiClient.BASE_URL, opts)

                mSocket?.on(Socket.EVENT_CONNECT) {
                    Log.d("SocketService", "Connected to socket server")
                    emitConnectUser()
                }

                // Reliability improvement using manager level reconnect
                // In Socket.IO 2.x, reconnect events are handled by the Manager
                mSocket?.io()?.on(Manager.EVENT_RECONNECT) {
                    Log.d("SocketService", "Reconnected to socket server (Manager)")
                    emitConnectUser()
                }

                mSocket?.on(Socket.EVENT_DISCONNECT) {
                    Log.d("SocketService", "Disconnected from socket server")
                    sessionManager.setRoomConnected(false)
                }

                mSocket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e("SocketService", "Socket connection error: ${args.getOrNull(0)}")
                }

                mSocket?.on("notification") { args ->
                    if (args.isNotEmpty()) {
                        val payload = args[0] as? JSONObject
                        val message = payload?.optString("message", "New Notification") ?: "New Notification"
                        // Show toast on Main thread
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(contextRef, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                        // Broadcast to UI to update badge counts
                        val intent = android.content.Intent("NEW_NOTIFICATION_RECEIVED")
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(contextRef).sendBroadcast(intent)
                    }
                }

                mSocket?.on("appointment_cancelled") { args ->
                    if (args.isNotEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(contextRef, "An appointment has been cancelled", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("SocketService", "Socket initialization error", e)
            }
        }

        if (mSocket?.connected() == false) {
            mSocket?.connect()
            Log.d("SocketService", "Connecting socket...")
        }
    }

    private fun emitConnectUser() {
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole().lowercase()
        
        if (userId != -1) {
            val joinData = JSONObject()
            joinData.put("user_id", userId)
            joinData.put("role", role)
            mSocket?.emit("connect_user", joinData)

            val roomData = JSONObject()
            roomData.put("room", "${role}_$userId")
            mSocket?.emit("join_room", roomData)

            sessionManager.setRoomConnected(true)
            Log.d("SocketService", "Emitted connect_user and join for ${role}_$userId")
        }
    }

    fun disconnect() {
        mSocket?.disconnect()
        sessionManager.setRoomConnected(false)
    }

    fun getSocket(): Socket? = mSocket
}
