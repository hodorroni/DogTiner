package il.example.dogtinder.repository.Firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import il.example.dogtinder.model.Event
import il.example.dogtinder.repository.CitiesEvent
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CitiesEventFireBase : CitiesEvent {

    private val eventRef = FirebaseFirestore.getInstance().collection("Events")
    private val userRef = FirebaseFirestore.getInstance().collection("user")
    private val firebaseAuth = FirebaseAuth.getInstance()



    //capitalize only the first letter for the dog or owner name and compare them both what the user has sent and in the firebase
    fun capitalizeFirstLetterKeepCase(str: String): String {
        return str.toLowerCase().trim()
    }

    override fun getAllEventsByCity(city: String): Flow<Resource<List<Event>>> = callbackFlow {
        trySend(Resource.Loading())
        val snapShotListener = eventRef.addSnapshotListener { value, error ->
            val listEvents: MutableList<Event> = mutableListOf()

            if (value != null) {
                val eventsObjects = value.toObjects(Event::class.java)
                for (event in eventsObjects) {
                    if (capitalizeFirstLetterKeepCase(event.city) == capitalizeFirstLetterKeepCase(city)) {
                        listEvents.add(event)
                    }
                }
                if (listEvents.isNotEmpty()) {
                    trySend(Resource.Success(listEvents.toList()))
                } else {
                    trySend(Resource.Error("No events found in $city"))
                }
            } else {
                trySend(Resource.Error("Failed to retrieve events"))
            }
        }

        // Cancel the snapshot listener when the flow is closed
        awaitClose { snapShotListener.remove() }
    }




    //When clicking to attend on a specific event
    override suspend fun updateAttenders(idEvent: String,increase:Boolean,decrease: Boolean) : Resource<Void> {
        return withContext(Dispatchers.IO){
            try {
                //current user logged in
                val currentUser = firebaseAuth.currentUser!!.uid
                //the current document we are looking to update the fields
                val currentDocument = eventRef.document(idEvent).get().await()
                //the id of the user who created the document
                val createdUserEvent = currentDocument.getString("userCreated")

                //the current user cant attend for his own event
                if(currentUser==createdUserEvent){
                    Resource.Error("Can't attend to your own event",null)
                }
                //of someone else tries to attend to any other event that was created
                else if(increase) {
                    //getting the current user from the specific document we need
                    val currentAttenders = (currentDocument.get("attenders") as List<String>).toMutableList()
                    //val newAttenders = currentAttenders + 1
                    //val updated = hashMapOf<String,Any>("attenders" to newAttenders)

                    //getting the name of the current user which logged in and attempted to attend for this event
                    val nameOfCurrentUser = userRef.document(currentUser).get().await().getString("name")

                    //if the name appears to be in the in the list of attenders then we won't add this user to the list
                    if(nameOfCurrentUser !in currentAttenders){
                        currentAttenders.add(nameOfCurrentUser.toString())
                    }
                    val updated = hashMapOf<String,Any>("attenders" to currentAttenders)
                    val setDocument = eventRef.document(idEvent).update(updated).await()
                    //val updatedDocument = eventRef.document(idEvent).get().await()
                    Resource.Success(setDocument)
                }

                //when we want to remove someone
                else {
                    //getting the current user from the specific document we need
                    val currentAttenders = (currentDocument.get("attenders") as List<String>).toMutableList()

                    //getting the name of the current user which logged in and attempted to attend for this event
                    val nameOfCurrentUser = userRef.document(currentUser).get().await().getString("name")

                    if(nameOfCurrentUser in currentAttenders){
                        currentAttenders.remove(nameOfCurrentUser.toString())
                    }
                    val updated = hashMapOf<String,Any>("attenders" to currentAttenders)
                    val setDocument = eventRef.document(idEvent).update(updated).await()
                    Resource.Success(setDocument)
                }
            }
            catch (e:Exception){
                Resource.Error(e.message?:"Unknown error occurred")
            }
        }
    }


    //The ability to delete an event by the user that created it
    override suspend fun deleteEvent(userId: String,eventId:String) {
        val currentId = firebaseAuth.currentUser!!.uid
        if(currentId==userId){
            val documentsEvents = eventRef.get().await()
            for (document in documentsEvents){
                if(eventId.equals(document.get("eventId").toString())){
                    eventRef.document(eventId).delete().await()
                    break
                }
            }
        }
    }


    override suspend fun editEvent(
        event: Event,
        location: String,
        street:String,
        date: String,
        description: String,
        time:String
    ): Resource<Any> {
        val eventDb = eventRef.document(event.eventId).get().await()
        if(eventDb!=null){
            var oldLocation=location
            var oldDate=date
            var oldDescription=description
            var oldTime=time
            var oldStreet=street
            if(location.isNullOrEmpty()) oldLocation = event.city
            if(street.isNullOrEmpty()) oldStreet = event.streetName
            if(date.isNullOrEmpty()) oldDate = event.date
            if(description.isNullOrEmpty()) oldDescription = event.description
            if(time.isNullOrEmpty()) oldTime = event.time
            val eventData = hashMapOf(
                "city" to oldLocation,
                "date" to oldDate,
                "description" to oldDescription,
                "time" to oldTime,
                "streetName" to oldStreet
            ).toMutableMap<String,Any>()

            eventRef.document(event.eventId).update(eventData).await()
            return Resource.Success("Success")
        }
        else {
            return Resource.Error("Event wasn't found")
        }
    }
}