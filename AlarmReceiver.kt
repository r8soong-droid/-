package com.egen.bosstimer
import android.app.*
import android.content.*
import android.os.Build

class AlarmReceiver: BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val name=intent.getStringExtra("name") ?: "보스"
        val channelId="boss_alert"
        val nm=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=26){
            nm.createNotificationChannel(NotificationChannel(channelId,"보스 출현 알림",NotificationManager.IMPORTANCE_HIGH))
        }
        val n=Notification.Builder(context,channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("보스 출현 5분 전")
            .setContentText("$name 출현까지 5분 남았습니다.")
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
        nm.notify((System.currentTimeMillis()%100000).toInt(),n)
    }
}