package com.simats.Tmapp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.simats.Tmapp.api.ApiClient
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import org.json.JSONObject

/**
 * SocketService manages the global Socket.IO connection for the TeleHealth+ app.
 * It ensures a single instance and handles automatic reconnection and room joining.
 *
 * Payment events handled:
 *   payment_success  → broadcasts PAYMENT_SUCCESS_RECEIVED
 *   wallet_update    → broadcasts WALLET_UPDATE_RECEIVED
 *   refund_update    → broadcasts REFUND_UPDATE_RECEIVED
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

                // ── General notification handler ──────────────────────────
                mSocket?.on("new_notification") { args ->
                    if (args.isNotEmpty()) {
                        val payload = args[0] as? JSONObject
                        val title = payload?.optString("title", "New Notification") ?: "New Notification"
                        val description = payload?.optString("description", "") ?: ""
                        val displayMsg = if (description.isNotEmpty()) "$title: $description" else title

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                contextRef,
                                displayMsg,
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        broadcast("NEW_NOTIFICATION_RECEIVED")
                    }
                }

                // ── Payment success (patient receives this) ───────────────
                mSocket?.on("payment_success") { args ->
                    if (args.isNotEmpty()) {
                        val data = args[0] as? JSONObject
                        val amount = data?.optDouble("amount") ?: 0.0
                        val invoice = data?.optString("invoice_number") ?: ""
                        val apptId = data?.optInt("appointment_id") ?: -1

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                contextRef,
                                "Payment ₹$amount confirmed! Invoice: $invoice",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        broadcast("PAYMENT_SUCCESS_RECEIVED", mapOf("appointment_id" to apptId))
                        broadcast("NEW_NOTIFICATION_RECEIVED")
                    }
                }

                // ── Wallet update (doctor receives this) ──────────────────
                mSocket?.on("wallet_update") { args ->
                    if (args.isNotEmpty()) {
                        val data = args[0] as? JSONObject
                        val available = data?.optDouble("available_balance") ?: 0.0
                        val pending = data?.optDouble("pending_balance") ?: 0.0

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                contextRef,
                                "Wallet updated — Available: ₹$available | Pending: ₹$pending",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        broadcast("WALLET_UPDATE_RECEIVED")
                        broadcast("NEW_NOTIFICATION_RECEIVED")
                    }
                }

                // ── Refund update (patient/admin receives this) ───────────
                mSocket?.on("refund_update") { args ->
                    if (args.isNotEmpty()) {
                        val data = args[0] as? JSONObject
                        val refundAmt = data?.optDouble("refund_amount") ?: 0.0
                        val status = data?.optString("status") ?: "completed"

                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                contextRef,
                                "Refund ₹$refundAmt $status",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        broadcast("REFUND_UPDATE_RECEIVED")
                        broadcast("NEW_NOTIFICATION_RECEIVED")
                    }
                }

                // ── Appointment cancelled ─────────────────────────────────
                mSocket?.on("appointment_cancelled") { args ->
                    if (args.isNotEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                contextRef,
                                "An appointment has been cancelled",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }

                        broadcast("NEW_NOTIFICATION_RECEIVED")
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
            Log.d("SocketService", "Joined room: ${role}_$userId")
        }
    }

    private fun broadcast(action: String, extras: Map<String, Any> = emptyMap()) {
        val intent = Intent(action)
        extras.forEach { (key, value) ->
            when (value) {
                is Int -> intent.putExtra(key, value)
                is String -> intent.putExtra(key, value)
                is Double -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
            }
        }
        LocalBroadcastManager.getInstance(contextRef).sendBroadcast(intent)
    }

    fun getSocket(): Socket? = mSocket

    fun disconnect() {
        mSocket?.disconnect()
        mSocket = null
        sessionManager.setRoomConnected(false)
        Log.d("SocketService", "Socket disconnected manually")
    }

    fun emit(event: String, data: JSONObject) {
        mSocket?.emit(event, data)
    }
}