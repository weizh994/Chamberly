package com.company.chamberly

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateActivity : ComponentActivity() {
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private val auth = Firebase.auth
    private val database = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        val currentUser = auth.currentUser

        val editText = findViewById<EditText>(R.id.chamber_title)
        val createButton = findViewById<Button>(R.id.create_button)


        // Limit the length of the title
        val maxLength = 50
        val filterArray = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
        editText.filters = filterArray

        createButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val authorUID = sharedPreferences.getString("uid", currentUser?.uid)
            val authorName = sharedPreferences.getString("displayName", "NONE")
            val title = editText.text.toString()
            if (title.isEmpty()){
                editText.error = "Please enter a title"
            } else {
                //Toast.makeText(this, authorName, Toast.LENGTH_SHORT).show()
                val chamber = Chamber(
                    authorName = authorName ?: "",
                    authorUID = authorUID ?: "",
                    groupTitle = title
                )

                val collectionRef = database.collection("GroupChatIds")
                val documentRef = collectionRef.document() // generate a random document ID
                chamber.groupChatId = documentRef.id // set the document ID to the random ID

                documentRef.set(chamber)
                    .addOnSuccessListener {
                        // Save additional data to Realtime Database
                        val realtimeDb = FirebaseDatabase.getInstance()
                        val chamberDataRef = realtimeDb.getReference(chamber.groupChatId)

                        // Set "Host" data
                        chamberDataRef.child("Host").setValue(chamber.authorUID)

                        // Set empty "messages" child key
                        chamberDataRef.child("messages").push().setValue("")

                        // Set "timestamp" data
                        val timestamp = System.currentTimeMillis() / 1000 // Convert to seconds
                        chamberDataRef.child("timestamp").setValue(timestamp)

                        // Set "Title" data
                        chamberDataRef.child("Title").setValue(chamber.groupTitle)

                        // Set "Users" data
                        val usersRef = chamberDataRef.child("Users")
                        val membersRef = usersRef.child("members")
                        val hostRef = membersRef.child(chamber.authorUID)
                        hostRef.setValue(authorName).addOnSuccessListener {
                            val intent = Intent(this@CreateActivity, ChatActivity::class.java)
                            //TODO : pass chamber object to ChatActivity
                            //intent.putExtra("chamber", chamber)
                            intent.putExtra("groupChatId", chamber.groupChatId)
                            intent.putExtra("groupTitle", chamber.groupTitle)
                            intent.putExtra("authorName",chamber.authorName)
                            intent.putExtra("authorUID",chamber.authorUID)
                            startActivity(intent)
                            finish()
                        }



                        //Toast.makeText(this, "Chamber created: $chamber", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error creating chamber: $e", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@CreateActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }
    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }

}
