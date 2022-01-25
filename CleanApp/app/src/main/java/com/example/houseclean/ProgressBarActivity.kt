package com.example.houseclean

import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.houseclean.databinding.ActivityMainBinding
import com.example.houseclean.databinding.ActivityProgressBarBinding

class ProgressBarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBarBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun show() {

    }

    fun dismiss() {

    }
}