package com.example.houseclean

import android.app.NotificationManager
import androidx.core.app.NotificationCompat

object InboxStyleMockData {

    const val mContentTitle = "title"
    const val mContentText = "text"
    const val mNumberOfNewEmails = 5
    const val mPriority = NotificationCompat.PRIORITY_DEFAULT

    const val mBigConstentTitle = "5 new emails from Jane"
    const val mSummaryText = "New emails"

    fun mIndividualEmailSumary(): ArrayList<String>{
        val list = ArrayList<String>()

        list.add("Jane text1")
        list.add("Jane text2")
        list.add("Jane text3")
        list.add("Jane text4")
        list.add("Jane text5")

        return list
    }

    fun mParticipants(): ArrayList<String>{
        val list = ArrayList<String>()

        list.add("Jane 1")
        list.add("Jane 2")
        list.add("Jane 3")
        list.add("Jane 4")
        list.add("Jane 5")

        return list
    }

    const val mChannelId = "channel_email_1"

    const val mChannelName = "Sample Email"

    const val mChannelDescription = "Sample Email Notification"
    const val mChannelImportance = NotificationManager.IMPORTANCE_DEFAULT
    const val mChannelEnableVibrate = true
    const val mChannelLockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE



}