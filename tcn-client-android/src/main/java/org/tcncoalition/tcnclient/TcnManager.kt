package org.tcncoalition.tcnclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import org.tcncoalition.tcnclient.bluetooth.BluetoothStateListener
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothService
import org.tcncoalition.tcnclient.bluetooth.TcnBluetoothServiceCallback

abstract class TcnManager(
    private val context: Context
) : BluetoothStateListener,
    TcnBluetoothServiceCallback {

    protected var service: TcnBluetoothService? = null
    private var isBound = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@TcnManager.service =
                (service as TcnBluetoothService.LocalBinder).service.apply {
                    val notification = createNotification()
                    startForegroundNotificationIfNeeded(NOTIFICATION_ID, notification)
                    setBluetoothStateListener(this@TcnManager)
                    startTcnExchange(this@TcnManager)
                }
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    abstract fun foregroundNotification(): NotificationCompat.Builder

    private fun createNotification(): Notification {

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        val notificationChannelId = "happ_notification"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            context,
            notificationChannelId
        ) else Notification.Builder(context)


        return builder
            .setContentTitle("Happ se está ejecutando")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }

    fun startService() {
        context.bindService(
            Intent(context, TcnBluetoothService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun stopService() {
        if (isBound) {
            service?.stopTcnExchange()
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun changeOwnTcn() {
        if (isBound) {
            service?.changeOwnTcn()
        } else {
            startService()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1 // Don't use 0
    }
}