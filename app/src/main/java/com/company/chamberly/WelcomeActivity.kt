package com.company.chamberly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        // Check if the user is logged in
        if (currentUser != null) {
            userExist(currentUser.uid) { exists ->
                if (exists) {
                    // The user exists
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // The user does not exist
                    // Handle the case when the user does not exist
                    // For example, show a login screen or redirect to sign up page
                }
            }
        } else {
            // The user is not logged in
            // Handle the case when the user is not logged in
            // For example, show a login screen or redirect to sign up page
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


    //TODO: Check if user exist in database
    private fun userExist(uid: String, callback: (Boolean) -> Unit) {
        // Check if UID is exist in Display_Names collection
        val displayNameRef = database.collection("Display_Names").whereEqualTo("UID", uid)
        displayNameRef.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Check if UID is exist in Accounts collection
                    val accountRef = database.collection("Accounts").whereEqualTo("UID", uid)
                    accountRef.get()
                        .addOnSuccessListener { querySnapshot ->
                            callback(!querySnapshot.isEmpty)
                        }
                        .addOnFailureListener { exception ->
                            callback(false)
                        }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                callback(false)
            }
    }

    private fun Check() {
        val user = Firebase.auth.currentUser
        val editText = findViewById<EditText>(R.id.display_name)

        if (user != null) {
            val displayName = editText.text.toString()

            // Check if displayName is already used
            val displayNameRef = database.collection("Display_Names").whereEqualTo("displayName", displayName)
            displayNameRef.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // displayName is available, proceed with storing data
                        val displayNameData = mapOf(
                            "Display_Name" to displayName,
                            "Email" to "${user.uid}@chamberly.net",
                            "UID" to user.uid
                        )
                        database.collection("Display_Names").document(displayName)
                            .set(displayNameData)
                            .addOnSuccessListener {
                                // Add a new document with a generated ID into Account collection
                                Toast.makeText(this, "Welcome to Chamberly!", Toast.LENGTH_SHORT).show()
                                val account = mapOf(
                                    "UID" to user.uid,
                                    "Display_Name" to displayName,
                                    "Email" to "${user.uid}@chamberly.net",
                                    "platform" to "android",
                                    "timestamp" to FieldValue.serverTimestamp()
                                )
                                database.collection("Accounts").document(user.uid.toString())
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
