package com.example.houseclean

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class NotificationUtil {

    fun createInboxStyleNotificationChannel(context: Context): String{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            val channelId: String = InboxStyleMockData.mChannelId
            val channelName: CharSequence = InboxStyleMockData.mChannelName
            val channelDescription: String = InboxStyleMockData.mChannelDescription
            val channelImportance: Int = InboxStyleMockData.mChannelImportance
            val channelEnableVibrate: Boolean = InboxStyleMockData.mChannelEnableVibrate
            val channelLockscreenVisibility: Int = InboxStyleMockData.mChannelLockscreenVisibility

            val notificationChannel = NotificationChannel(channelId,channelName,channelImportance)
            notificationChannel.description = channelDescription
            notificationChannel.enableVibration(channelEnableVibrate)
            notificationChannel.lockscreenVisibility = channelLockscreenVisibility

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)

            return channelId
        }else{
            return ""
        }
     }
}