package com.company.chamberly

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.integrity.internal.c
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.io.File

class ChatActivity : ComponentActivity(){
    // TODO: add chat cache
    private lateinit var cacheFile : File   // cache file
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var groupChatId: String
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var messages = mutableListOf<Message>() // message list
    private val auth = Firebase.auth                // get current user
    private val database = Firebase.database        // realtime database
    private val firestore = Firebase.firestore      // firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE) // get shared preferences
        val currentUser = auth.currentUser // get current user
        val uid = sharedPreferences.getString("uid", "") ?: currentUser?.uid // get uid

        messageAdapter = MessageAdapter(uid!!) // create message adapter
        groupChatId = intent.getStringExtra("groupChatId") ?: "" // get group chat id
        recyclerView = findViewById(R.id.recyclerViewMessages)         // get recycler view
        recyclerView.adapter = messageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        messageAdapter.setOnMessageLongClickListener(object : MessageAdapter.OnMessageLongClickListener {
            override fun onMessageLongClick(message: Message) {
                showDialog(message)
            }
            override fun onSelfLongClick(message: Message) {
                showSelfDialog(message)
            }
        })
        recyclerView.adapter = messageAdapter


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
                        firestore.collection("GroupChatIds").document(groupChatId)
                            .update("locked" , false)
                            .addOnSuccessListener { firestore.collection("GroupChatIds").document(groupChatId)
                                .update("publishedPool", true)
                                .addOnSuccessListener {
                                    finish()
                                }
                            }
                    }

            }
            else{
                finish()
            }
        }
    }

    // Menu functions
    // copy message
    private fun copyMessage(message: Message) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Message", message.message_content)
        clipboard.setPrimaryClip(clip)
        Log.e("Holder", "Holder successfully copied: ${message.message_content}")

    }
    // report user
    private  fun reportUser(message: Message, reason: String){
        // Todo: get chamber info early


        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val UID = sharedPreferences.getString("uid", auth.currentUser?.uid)

        firestore.collection("GroupChatIds").document(groupChatId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    //Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val authorName = document.get("authorName") as? String
                    val report = hashMapOf(
                        "against" to message.uid,
                        "by" to UID,
                        "groupChatId" to groupChatId,
                        "realHost" to "",
                        "reason" to reason,
                        "reportDate" to FieldValue.serverTimestamp(),
                        "realHost" to authorName,
                        "ticketTaken" to false
                        //"Title" to ?
                    )
                    firestore.collection("Reports").add(report)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }






    }
    // block user
    private fun blockUser(message: Message) {
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val uid = message.uid

        firestore.collection("GroupChatIds").document(groupChatId!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val blockedUsers = documentSnapshot.get("blockedUsers") as? ArrayList<String>
                    if (blockedUsers != null) {
                        if (uid in blockedUsers) {
                            Toast.makeText(this, "User has been blocked", Toast.LENGTH_SHORT).show()
                            Log.e("BlockUser", "User has been blocked")
                        } else {
                            // Add user to blocked list
                            val updatedBlockedUsers = ArrayList<String>(blockedUsers)
                            updatedBlockedUsers.add(uid)

                            firestore.collection("GroupChatIds").document(groupChatId!!)
                                .update("blockedUsers", updatedBlockedUsers)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "User blocked", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error blocking user", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error retrieving blocked users", Toast.LENGTH_SHORT).show()
            }
    }



    override fun onDestroy() {
        onBackPressedCallback.remove()
        super.onDestroy()
    }

    private fun showSelfDialog(message: Message){
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_self_message_options)

        val  dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        dialogTitle.text = message.sender_name
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        dialogMessage.text = message.message_content

        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)

        // set dialog window's width and height
        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        copyButton.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDialog(message: Message) {
        val dialog = Dialog(this, R.style.Dialog)
        dialog.setContentView(R.layout.dialog_message_options)

        val dialogTitle = dialog.findViewById<TextView>(R.id.DialogTitle)
        dialogTitle.text = message.sender_name
        val dialogMessage = dialog.findViewById<TextView>(R.id.MessageContent)
        dialogMessage.text = message.message_content

        val copyButton = dialog.findViewById<Button>(R.id.buttonCopy)
        val reportButton = dialog.findViewById<Button>(R.id.buttonReport)
        val blockButton = dialog.findViewById<Button>(R.id.buttonBlock)

        // set dialog window's width and height
        val window = dialog.window
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window?.attributes = layoutParams

        // set copy button's click listener
        copyButton.setOnClickListener {
            copyMessage(message)
            dialog.dismiss()
        }

        // set report button's click listener
        reportButton.setOnClickListener {
            showReportDialog(message)
            dialog.dismiss()
        }

        // set block button's click listener
        blockButton.setOnClickListener {
            blockUser(message)
            dialog.dismiss()
        }

        // show Dialog
        dialog.show()
    }

    private fun showReportDialog(message: Message) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_report_options)

        val titleTextView = dialog.findViewById<TextView>(R.id.textReportTitle)
        titleTextView.text = "Reporting ${message.sender_name}"
        val harassmentButton = dialog.findViewById<Button>(R.id.buttonHarassment)
        val inappropriateBehaviorButton =
            dialog.findViewById<Button>(R.id.buttonInappropriateBehavior)
        val unsupportiveBehaviorButton =
            dialog.findViewById<Button>(R.id.buttonUnsupportiveBehavior)
        val spammingButton = dialog.findViewById<Button>(R.id.buttonSpamming)
        val annoyingButton = dialog.findViewById<Button>(R.id.buttonAnnoying)

        harassmentButton.setOnClickListener {
            reportUser(message, "harassment")
            dialog.dismiss()
        }
        inappropriateBehaviorButton.setOnClickListener {
            reportUser(message, "inappropriate behavior")
            dialog.dismiss()
        }
        unsupportiveBehaviorButton.setOnClickListener {
            reportUser(message, "unsupportive behavior")
            dialog.dismiss()
        }
        spammingButton.setOnClickListener {
            reportUser(message, "Spamming")
            dialog.dismiss()
        }
        annoyingButton.setOnClickListener {
            reportUser(message, "Annoying")
            dialog.dismiss()
        }

        // show Dialog
        dialog.show()
    }


}