package com.simats.tmapp

import android.content.Context
import io.socket.client.Socket

/**
 * SocketManager provides access to the global Socket.IO connection.
 * It uses the underlying SocketService for connection management.
 */
object SocketManager {
    fun getInstance(context: Context): SocketService {
        return SocketService.getInstance(context)
    }

    fun getSocket(context: Context): Socket? {
        return SocketService.getInstance(context).getSocket()
    }

    fun connect(context: Context) {
        SocketService.getInstance(context).connect()
    }

    fun disconnect(context: Context) {
        SocketService.getInstance(context).disconnect()
    }
}
