package com.example.myhome

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SangokFcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SangokFCM"
        const val CHANNEL_ID = "sangok_push_channel"
        const val CHANNEL_NAME = "산곡운전학원 알림"
    }

    /**
     * 새 FCM 토큰이 발급될 때 호출됩니다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새 FCM 토큰 발급: $token")
        saveFcmToken(token)
    }

    /**
     * 앱이 포그라운드 상태일 때 메시지를 수신합니다.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "메시지 수신 from: ${remoteMessage.from}")

        // notification payload 처리
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "산곡 운전면허학원"
            val body = notification.body ?: ""
            sendNotification(title, body)
        }

        // data payload 처리 (포그라운드에서만 수동 처리 필요)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "산곡 운전면허학원"
            val body = remoteMessage.data["body"] ?: ""
            val url = remoteMessage.data["url"] ?: ""
            sendNotification(title, body, url)
        }
    }

    /**
     * 실제 알림 빌드 및 표시
     */
    private fun sendNotification(title: String, body: String, url: String = "") {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (url.isNotEmpty()) {
                putExtra("push_url", url)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상은 채널 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "산곡 자동차운전전문학원 공지 및 이벤트 알림"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun saveFcmToken(token: String) {
        val prefs = getSharedPreferences("sangok_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }
}
