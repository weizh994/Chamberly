package com.company.chamberly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
// TODO: Add cache file for sign in
class WelcomeActivity : AppCompatActivity() {
    private val auth = Firebase.auth
    private val database = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
            val intent = intent
            intent.setClass(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            auth.signInAnonymously()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        Toast.makeText(this, "Welcome!${user?.uid}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        val addButton = findViewById<Button>(R.id.continue_button)
        addButton.setOnClickListener {
            Check()
        }
    }
    private fun Check() {
        val user = Firebase.auth.currentUser
        val editText = findViewById<EditText>(R.id.display_name)

        if (user != null) {
            val displayName = editText.text.toString()

            // Check if displayName is already used
            val displayNameRef = database.collection("Display_Name").whereEqualTo("displayName", displayName)
            displayNameRef.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // displayName is available, proceed with storing data
                        val displayNameData = DisplayName(
                            displayName = displayName,
                            email = "${user.uid}@chamberly.net",
                            uid = user.uid
                        )
                        database.collection("Display_Name").document(displayName)
                            .set(displayNameData)
                            .addOnSuccessListener {
                                // Add a new document with a generated ID into Account collection
                                Toast.makeText(this, "Welcome to Chamberly!", Toast.LENGTH_SHORT).show()
                                val account = Account(
                                    displayName = displayName,
                                    email = "${user.uid}@chamberly.net",
                                    uid = user.uid
                                )
                                database.collection("Account").document(user.uid.toString())
                                    .set(account)
                                    .addOnSuccessListener {
                                        Log.d("TAG", "DocumentSnapshot successfully written!")

                                        // Save displayName to SharedPreferences
                                        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
                                        val editor = sharedPreferences.edit()
                                        editor.putString("uid", user.uid)
                                        editor.putString("displayName", displayName)
                                        editor.apply()

                                        // Go to MainActivity
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                        finish() // Optional: Finish WelcomeActivity to prevent going back
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("TAG", "Error writing document", e)
                                        Toast.makeText(this, "Error writing document", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error storing displayName", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // displayName is already used
                        Toast.makeText(this, "This name has been used!", Toast.LENGTH_SHORT).show()
                        //editText.error = "This name has been used!"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking displayName", Toast.LENGTH_SHORT).show()
                }
        }
    }


}
