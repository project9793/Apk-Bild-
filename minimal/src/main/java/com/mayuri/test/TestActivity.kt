package com.mayuri.test

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class TestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "Mayuri test app\n\nAgar ye khul gaya to phone me APK install ho sakta hai."
                textSize = 18f
                setPadding(48, 96, 48, 48)
            }
        )
    }
}
