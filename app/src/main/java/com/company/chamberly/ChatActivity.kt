package com.company.chamberly

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.io.File

class ChatActivity : ComponentActivity() {
    // TODO: add chat cache
    private val auth = Firebase.auth        // get current user
    private val database = Firebase.database// realtime database
    private val firestore = Firebase.firestore// firestore
    private lateinit var cacheFile : File   // cache file
    var messages = mutableListOf<Message>() // message list
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val groupChatId = intent.getStringExtra("groupChatId") // get group chat id
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMessages) // get recycler view
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE) // get shared preferences
        val currentUser = auth.currentUser // get current user
        val uid = sharedPreferences.getString("uid", "") ?: currentUser?.uid // get uid
        val messageAdapter = MessageAdapter(uid!!) // get message adapter
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        //load cache file
        if(groupChatId!=null){
            cacheFile = File(this.cacheDir, groupChatId)
            if(cacheFile.exists()){
                //load data from the file
                //the content of the file is a JSON string
                val content = this.openFileInput(groupChatId).bufferedReader().use { it.readText() }
                // Convert the content to a list of Message and update the UI
                val gson = Gson()
                val type = object : TypeToken<List<Message>>() {}.type
                messages= Gson().fromJson(content, type)
                messageAdapter.notifyDataSetChanged()    // update the UI
            }
        }
        val messagesRef = database.getReference(groupChatId!!).child("messages")
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear() //clear the list

                for (childSnapshot in snapshot.children) {
                    if (childSnapshot.exists()) {
                        val messageValue = childSnapshot.getValue()
                        if (messageValue != null && messageValue is Map<*, *>) {
                            val uid = messageValue["uid"] as? String
                            val messageContent = messageValue["message_content"] as? String
                            val messageType = messageValue["message_type"] as? String
                            val senderName = messageValue["sender_name"] as? String

                            if (uid != null && messageContent != null && messageType != null && senderName != null) {
                                val message = Message(uid, messageContent, messageType, senderName)
                                messages.add(message)
                            }
                        }
                    }
                }

                //save the data to the cache file with groupChatId as the file name
                val content = Gson().toJson(messages)
                this@ChatActivity.openFileOutput(groupChatId, Context.MODE_PRIVATE).use {
                    it.write(content.toByteArray())
                }
                messageAdapter.setMessages(messages) // using new messages
                recyclerView.scrollToPosition(messages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Error fetching messages: ${error.message}")
            }
        })

        val sendButton = findViewById<Button>(R.id.buttonSend)
        sendButton.setOnClickListener {
            val editText = findViewById<EditText>(R.id.editTextMessage)
            val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
            val uid = sharedPreferences.getString("uid", currentUser?.uid)
            val senderName = sharedPreferences.getString("displayName", "NONE")
            val message = Message(uid!!, editText.text.toString(), "text", senderName!!)

            val chatRef = database.getReference(groupChatId!!).child("messages").push().setValue(message)
                .addOnSuccessListener {
                    editText.setText("")
                    messages.add(message)
                    // TODO add into cache
                    recyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error sending message: $e", Toast.LENGTH_SHORT).show()
                }
        }



        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitChat(groupChatId)
            }
        }
        this@ChatActivity.onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }
    fun exitChat(groupChatId: String){
        lateinit var authorUID : String
        firestore.collection("GroupChatIds").document(groupChatId).get().addOnSuccessListener {
            authorUID = it.getString("authorUID")!!
            // TODO:  use cache uid instead of auth.currentUser!!.uid
            if(authorUID != auth.currentUser!!.uid){
                // delete the group chat id from the user's list
                database.reference.child(groupChatId).child("Users").child("members").child(auth.currentUser!!.uid).removeValue()
                    .addOnSuccessListener {
                        firestore.collection("GroupChatIds").document(groupChatId).update("locked" , false)
                            .addOnSuccessListener { finish() }
                    }

            }
            else{
                finish()
            }
        }
    }

    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }

}