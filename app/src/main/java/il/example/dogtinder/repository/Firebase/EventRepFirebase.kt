package il.example.dogtinder.repository.Firebase

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import il.example.dogtinder.model.Event
import il.example.dogtinder.repository.DogsRepository
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class EventRepFirebase : DogsRepository {
    private val eventRef = FirebaseFirestore.getInstance().collection("Events")
    private val userRef = FirebaseFirestore.getInstance().collection("user")
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val firebaseStorage = FirebaseStorage.getInstance().reference


    override suspend fun addEvent(
        date: String,
        city: String,
        street: String,
        desc: String,
        time: String
    ): Resource<Void> =
        withContext(Dispatchers.IO) {
            try {
                val currentId = firebaseAuth.currentUser?.uid!!
                //the document in firestore with the currentid
                val documentCurrentUser = userRef.document(currentId).get().await()
                //getting the value of the "name" field inside that document
                val nameCurrent = documentCurrentUser.getString("name")!!
                //getting the id of the current document that was created to store it in my Event data class
                val eventId = eventRef.document().id
                val event = Event(date, city, currentId, nameCurrent, eventId, street, desc, time)
                //val setDocument = eventRef.document(city).set(event).await()
                val setDocument = eventRef.document(eventId).set(event).await()
                Resource(setDocument)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Coudln't add event : ${e.message}")
            }
        }

    override suspend fun getCurrentUserPhoto(): Resource<Uri> {
        return withContext(Dispatchers.IO) {
            try {
                val currentId = firebaseAuth.currentUser?.uid!!
                val imageUri = firebaseStorage.child(currentId)
                val uri = imageUri.downloadUrl.await()
                return@withContext Resource.Success(uri)
            } catch (e: Exception) {
                return@withContext Resource.Error(e.message ?: "Unknown message")
            }
        }
    }

    //The ability to delete an event by the user that created it
    override suspend fun deleteEvent(userId: String, eventId: String) {
        val currentId = firebaseAuth.currentUser!!.uid
        if (currentId == userId) {
            val documentsEvents = eventRef.get().await()
            for (document in documentsEvents) {
                if (eventId.equals(document.get("eventId").toString())) {
                    eventRef.document(eventId).delete().await()
                    break
                }
            }
        }
    }

    //get the current logged in user id
    override suspend fun currentLoggedIn(): String {
        return firebaseAuth.currentUser!!.uid
    }

    //for every change in our list the snapshotlistener will be triggered and it will postvalue to the variable passed to the function
    override fun getEventsLiveData(data: MutableLiveData<Resource<List<Event>>>) {
        data.postValue(Resource.Loading())

        eventRef.orderBy("city").addSnapshotListener { snapshot, e ->
            if (e != null) {
                data.postValue(Resource.Error(e.localizedMessage))
            }
            if (snapshot != null && !snapshot.isEmpty) {
                //posting the data from here (the data variable) that was passed(we can observe from the fragment)
                data.postValue(Resource.Success(snapshot.toObjects(Event::class.java)))
            } else {
                data.postValue(Resource.Error("No data found!"))
            }
        }
    }


    //for every change in our list the snapshotlistener will be triggered and it will send the response which is of type  Resource<(Mutable)List<Event!>>
    //inside our viewmodel we have an livedata which gets this flow and converts it to livedata, then in our fragment we will observe this livedata
    //for any change in our database the snapshot will be triggered and it will send the response to our livedata in the viewmodel
    override fun getEventsFlow(): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading())
        val snapShotListener = eventRef.addSnapshotListener { value, error ->
            val response = if (value != null) {
                val events = value.toObjects(Event::class.java)
                Resource.Success(events)
            } else {
                Resource.Error(error?.message ?: error.toString())
            }
            //sending the response everytime the SnapShoListener will get triggered
            trySend(response)
        }

        //will keep the channel open to get updates
        //when there will be no listeneres anymore or DB will get closed it will be closed by itself
        awaitClose {
            snapShotListener.remove()
        }
    }


    //    override fun getEventsLiveData(data: MutableLiveData<Resource<List<Event>>>) {
//        eventRef.orderBy("city").addSnapshotListener { snapshot, e ->
//            if (e != null) {
//                data.postValue(Resource.Error(e.localizedMessage))
//                return@addSnapshotListener
//            }
//
//            if (snapshot != null && !snapshot.isEmpty) {
//                val events = mutableListOf<Event>()
//                for (doc in snapshot.documents) {
//                    val eventId = doc.id
//                    val date = doc.getString("date") ?: ""
//                    val city = doc.getString("city") ?: ""
//                    val userCreated = doc.getString("userCreated") ?: ""
//                    val userName = doc.getString("userName") ?: ""
//                    val attendersList = doc.get("attenders") as? List<String> ?: emptyList()
//
//                    val event = Event(date, city, userCreated, userName, eventId, attendersList)
//                    events.add(event)
//                }
//                data.postValue(Resource.Success(events))
//            } else {
//                data.postValue(Resource.Error("No data found!"))
//            }
//        }
//    }

    //When clicking to attend on a specific event
    override suspend fun updateAttenders(
        idEvent: String,
        increase: Boolean,
        decrease: Boolean
    ): Resource<Void> {
        return withContext(Dispatchers.IO) {
            try {
                //current user logged in
                val currentUser = firebaseAuth.currentUser!!.uid
                //the current document we are looking to update the fields
                val currentDocument = eventRef.document(idEvent).get().await()
                //the id of the user who created the document
                val createdUserEvent = currentDocument.getString("userCreated")

                //the current user cant attend for his own event
                if (currentUser == createdUserEvent) {
                    Resource.Error("Can't attend to your own event", null)
                }
                //of someone else tries to attend to any other event that was created
                else if (increase) {
                    //getting the current user from the specific document we need
                    val currentAttenders =
                        (currentDocument.get("attenders") as List<String>).toMutableList()
                    //val newAttenders = currentAttenders + 1
                    //val updated = hashMapOf<String,Any>("attenders" to newAttenders)

                    //getting the name of the current user which logged in and attempted to attend for this event
                    val nameOfCurrentUser =
                        userRef.document(currentUser).get().await().getString("name")

                    //if the name appears to be in the in the list of attenders then we won't add this user to the list
                    if (nameOfCurrentUser !in currentAttenders) {
                        currentAttenders.add(nameOfCurrentUser.toString())
                    }
                    val updated = hashMapOf<String, Any>("attenders" to currentAttenders)
                    val setDocument = eventRef.document(idEvent).update(updated).await()
                    //val updatedDocument = eventRef.document(idEvent).get().await()
                    Resource.Success(setDocument)
                }

                //when we want to remove someone
                else {
                    //getting the current user from the specific document we need
                    val currentAttenders =
                        (currentDocument.get("attenders") as List<String>).toMutableList()

                    //getting the name of the current user which logged in and attempted to attend for this event
                    val nameOfCurrentUser =
                        userRef.document(currentUser).get().await().getString("name")

                    if (nameOfCurrentUser in currentAttenders) {
                        currentAttenders.remove(nameOfCurrentUser.toString())
                    }
                    val updated = hashMapOf<String, Any>("attenders" to currentAttenders)
                    val setDocument = eventRef.document(idEvent).update(updated).await()
                    Resource.Success(setDocument)
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Unknown error occurred")
            }
        }
    }


    //if the user got deleted then remove the event that he previously created
    //in the init of the viewModel i will call it
    override suspend fun eventUserExistence() {
        try {
            var foundEvent = false
            val documentsUser = userRef.get().await()
            val documentsEvents = eventRef.get().await()
            for (event in documentsEvents) {
                val eventId = event.id
                val userCreatedEvent = event.get("userCreated").toString()
                //running over all the users in the database, if the user that created the event wasnt found in the users list that means this user got deleted
                //so we need to delete all the events he has made
                for (user in documentsUser) {
                    val userId = user.get("user_id").toString()
                    if (userCreatedEvent.equals(userId)) {
                        foundEvent = true
                        break
                    }
                }
                //if the event wasnt found that means that the user was deleted therefore we need to delete that event
                if (!foundEvent) {
                    eventRef.document(eventId).delete().await()
                }
                foundEvent = false
            }

        } catch (e: Exception) {

        }
    }


    //edit specific event

    override suspend fun editEvent(
        event: Event,
        location: String,
        street: String,
        date: String,
        description: String,
        time: String
    ): Resource<Any> {
        val eventDb = eventRef.document(event.eventId).get().await()
        if (eventDb != null) {
            var oldLocation = location
            var oldDate = date
            var oldDescription = description
            var oldTime = time
            var oldStreet = street
            if (location.isEmpty()) oldLocation = event.city
            if (street.isEmpty()) oldStreet = event.streetName
            if (date.isEmpty()) oldDate = event.date
            if (description.isEmpty()) oldDescription = event.description
            if (time.isEmpty()) oldTime = event.time

            val eventData = hashMapOf<String, Any>(
                "city" to oldLocation,
                "date" to oldDate,
                "description" to oldDescription,
                "time" to oldTime,
                "streetName" to oldStreet
            )
            eventRef.document(event.eventId).update(eventData).await()
            return Resource.Success("Success")
        } else {
            return Resource.Error("Event wasn't found")
        }
    }


    override suspend fun outdatedEventDateDelete() {
        val documentEvents = eventRef.get().await()
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy")
        val formattedDate = dateFormat.format(currentDate)
        val (currentDay, currentMonth, currentYear) = formattedDate.split("-")
        for (document in documentEvents) {
            val dbDateString = document.getString("date")
            dbDateString?.let {
                val dbDate = dateFormat.parse(dbDateString)
                val (dbDay, dbMonth, dbYear) = dbDateString.split("-")

                // Convert strings to integers for comparison
                val current = currentYear.toInt() * 10000 + currentMonth.toInt() * 100 + currentDay.toInt()
                val db = dbYear.toInt() * 10000 + dbMonth.toInt() * 100 + dbDay.toInt()
                //today's date is after the date in the database then just remove this event
                if (current> db) {
                    eventRef.document(document.id).delete().await()
                }
            }
        }
    }
}