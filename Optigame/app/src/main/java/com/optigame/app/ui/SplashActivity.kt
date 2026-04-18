package com.optigame.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.optigame.app.R
import com.optigame.app.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_gear)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        binding.ivGear.startAnimation(rotateAnim)
        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        binding.tvTagline.startAnimation(fadeIn)

        lifecycleScope.launch {
            delay(2500)
            startActivity(Intent(this@SplashActivity, SetupActivity::class.java))
            finish()
        }
    }
}
