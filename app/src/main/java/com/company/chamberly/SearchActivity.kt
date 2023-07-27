package com.company.chamberly

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.yalantis.library.Koloda
import com.yalantis.library.KolodaListener


// Firebase lock system: block other users from joining a chamber

class SearchActivity : ComponentActivity() ,KolodaListener{
    private val auth = Firebase.auth
    private val currentUser = auth.currentUser
    private lateinit var koloda: Koloda
    private lateinit var adapter: ChamberAdapter
    private val database = Firebase.database// realtime database
    private val firestore = Firebase.firestore
    private var isFirstTimeEmpty = true
    private var lastTimestamp: Any? = null
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        koloda = findViewById(R.id.koloda)
        koloda.kolodaListener = this

        // set adapter
        adapter = ChamberAdapter(this)
        koloda.adapter =  adapter


        // fetch now data from firestore onCreate
        //fetchChambers()

        val dislikeButton: ImageButton = findViewById(R.id.dislike)
        val likeButton: ImageButton = findViewById(R.id.like)


        dislikeButton.setOnClickListener {
            koloda.onClickLeft()
        }

        likeButton.setOnClickListener {
            koloda.onClickRight()
        }

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                releaseCards()
                val intent = Intent(this@SearchActivity, MainActivity::class.java)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    // TODO: Check why is "locked" but not "isLocked"

    // Check from real-time database
    private fun isVacant(chamber: Chamber, callback: (Boolean) -> Unit) {
        val uid = getSharedPreferences("cache", Context.MODE_PRIVATE).getString("uid", currentUser?.uid)
        database.reference.child(chamber.groupChatId).child("Users").child("members").get()
            .addOnSuccessListener { dataSnapshot ->
                val size = dataSnapshot.childrenCount
                for (snapshot in dataSnapshot.children) {
                    //Check if Not currentUser himself
                    if(snapshot.key == uid){
                        callback(false)
                        return@addOnSuccessListener
                    }
                }

                //check size
                val result = size <= 1
                callback(result)
            }
            .addOnFailureListener { exception ->
                Log.e("firebase", "Error getting data", exception)
                callback(false)
            }
    }

    private fun fetchChambers() {
        //TODO: load lastTimestamp from cache
        val query: Query = if (lastTimestamp == null) {
            firestore.collection("GroupChatIds")
                .whereEqualTo("locked", false)
                .whereEqualTo("publishedPool", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(4)
        } else {
            // fetch next 4 chambers
            Log.e(TAG, "New FetchChambers: last document is at ${lastTimestamp}")
            firestore.collection("GroupChatIds")
                .whereEqualTo("locked", false)
                .whereEqualTo("publishedPool", true)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .startAfter(lastTimestamp)
                .limit(4)
        }

        fetchChambersRecursively(query)
    }

    private fun fetchChambersRecursively(query: Query) {
        query.get()
            .addOnSuccessListener { querySnapshot ->
                Log.e(TAG, "fetchChambers: ${querySnapshot.documents.size}")
                for (documentSnapshot in querySnapshot) {
                    val chamber = documentSnapshot.toObject(Chamber::class.java)
                    // set publishedpool as false to locked this chamber
                    firestore.collection("GroupChatIds").document(chamber.groupChatId)
                        .update("publishedPool", false)
                    isVacant(chamber) { isVacant ->
                        if (isVacant) {
                            adapter.setData(chamber)
                        }
                        else{
                            // reset publishedPool as true again
                            firestore.collection("GroupChatIds").document(chamber.groupChatId)
                                .update("publishedPool", true)
                        }
                    }
                }
                val lastDocument = querySnapshot.documents.lastOrNull()
                //TODO: save into cache
                lastTimestamp = lastDocument?.get("timestamp")
            }
            .addOnFailureListener { exception ->
                Log.e("SearchActivity", "Error fetching chambers: $exception")
            }
    }

    // override koloda listener
    override fun onCardSwipedLeft(position: Int) {
        val chamber = (adapter as ChamberAdapter).getItem(position+1)
        Log.e("SearchActivity", "Card swiped left : ${chamber.groupTitle}")


        firestore.collection("GroupChatIds").document(chamber.groupChatId)
            .update("publishedPool", true)

        // Call the super implementation if needed
        super.onCardSwipedLeft(position)
    }
    override fun onCardSwipedRight(position: Int) {

        val chamber = (adapter as ChamberAdapter).getItem(position+1)
        isVacant(chamber) { isVacant ->
            if (isVacant) {
                // add user to Chat
                joinChat(chamber)
            }
        }
        // Call the super implementation if needed
        super.onCardSwipedRight(position)
    }

    override fun onClickLeft(position: Int)  {
        Log.e("SearchActivity", "Card swiped left at position: $position")
        val chamber = (adapter as ChamberAdapter).getItem(position+1)
        Log.e("SearchActivity", "Card swiped left : ${chamber.groupTitle}")

        firestore.collection("GroupChatIds").document(chamber.groupChatId)
            .update("publishedPool", true)
        // Call the super implementation if needed
        super.onClickLeft(position)
    }

    override fun onClickRight(position: Int)  {
        //Log.e("SearchActivity", "Card swiped right at position: $position")
        // TODO: check why position starts from -1
        val chamber = (adapter as ChamberAdapter).getItem(position+1)
        isVacant(chamber) { isVacant ->
            if (isVacant) {
                // add user to Chat
                joinChat(chamber)
            }
        }
        // Call the super implementation if needed
        super.onClickRight(position)
    }
    override fun onEmptyDeck() {
        if (isFirstTimeEmpty) {
            isFirstTimeEmpty = false
            Log.e("onEmptyDeck", "Initial")
        } else {
            Log.e("onEmptyDeck", "Now it's empty")
            //TODO: set publishedPool as true again
            releaseCards()
            fetchChambers()
        }

        // Call the super implementation if needed
        super.onEmptyDeck()
    }

    // When exit the activity, set publishedPool as true again
    fun releaseCards(){
        for(chamber in adapter.dataList){
            firestore.collection("GroupChatIds").document(chamber.groupChatId)
                .update("publishedPool", true)
        }
    }

    fun joinChat(chamber: Chamber){
        val sharedPreferences = getSharedPreferences("cache", Context.MODE_PRIVATE)
        val authorUID = sharedPreferences.getString("uid", currentUser?.uid)
        val authorName = sharedPreferences.getString("displayName", "Anonymous")
        val chamberDataRef = database.reference.child(chamber.groupChatId)
        val message = Message(authorUID!!, "${authorName} user joined and chamber auto-locked", "text", authorName!!)
        chamberDataRef.child("messages").push().setValue(message)
            .addOnSuccessListener {
                chamberDataRef.child("Users").child("members").child(authorUID!!).setValue(authorName)
                    .addOnSuccessListener {
                        firestore.collection("GroupChatIds").document(chamber.groupChatId).update("locked" , true)
                            .addOnSuccessListener{
                                val intent = Intent(this@SearchActivity, ChatActivity::class.java)
                                //TODO : pass chamber object to ChatActivity
                                //intent.putExtra("chamber", chamber)
                                intent.putExtra("groupChatId", chamber.groupChatId)
                                intent.putExtra("groupTitle", chamber.groupTitle)
                                intent.putExtra("authorName",chamber.authorName)
                                intent.putExtra("authorUID",chamber.authorUID)
                                startActivity(intent)
                                finish()
                            }
                    }
            }
    }

    override fun onDestroy() {
        releaseCards()
        onBackPressedCallback.remove()
        super.onDestroy()
    }

}


