package com.company.chamberly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.company.chamberly.ui.theme.ChamberlyTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val CreateButton = findViewById<Button>(R.id.create_button)
        CreateButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, CreateActivity::class.java)
            startActivity(intent)
        }
        val SearchButton = findViewById<Button>(R.id.search_button)
        SearchButton.setOnClickListener {
            val intent = intent
            intent.setClass(this, SearchActivity::class.java)
            startActivity(intent)
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                auth.signOut()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }
}
