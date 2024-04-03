package il.example.dogtinder.repository.Firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import il.example.dogtinder.model.User
import il.example.dogtinder.model.UserComments
import il.example.dogtinder.repository.ProfileRepository
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date

class ProfileRepFireBase : ProfileRepository {

    //reference for the user collection
    private val userRef = FirebaseFirestore.getInstance().collection("user")
    //reference for the pictures stored in the firestore
    private val firebaseStorage = FirebaseStorage.getInstance().reference

    //Authentication firebase reference
    private val firebaseAuth = FirebaseAuth.getInstance()


     override  fun pictureForSpecificUser(userId: String): Task<Uri> {
        val imageUri = firebaseStorage.child(userId)
        val uri = imageUri.downloadUrl
         return uri
    }



    //capitalize only the first letter for the dog or owner name and compare them both what the user has sent and in the firebase
    fun capitalizeFirstLetterKeepCase(str: String): String {
        return str.substring(0, 1).toLowerCase() + str.substring(1).toLowerCase()
    }

    override fun getAllUsersLiveData(
        data: MutableLiveData<Resource<List<User>>>,
        dogName: String,
        userName: String,
        flag:String
    ) {
        data.postValue(Resource.Loading())

        userRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                data.postValue(Resource.Error(e.localizedMessage))
                return@addSnapshotListener
            }

            val filteredUsers = mutableListOf<User>()
            val tasks = mutableListOf<Task<Uri>>() // List to hold download tasks for each user's image

            snapshot?.documents?.forEach { document ->
                val user = document.toObject(User::class.java)
                if(flag.equals("dog")){
                    if (user != null && (capitalizeFirstLetterKeepCase(user.dogsName) == capitalizeFirstLetterKeepCase(dogName))) {
                        //getting the picture for each user separately
                        val task = pictureForSpecificUser(document.id).addOnSuccessListener { uri ->
                            //updating the current user's uri
                            user.image = uri.toString()
                            filteredUsers.add(user)
                        }
                        tasks.add(task)
                    }
                }
                //user chose to search for owners by their names
                else {
                    if (user != null && (capitalizeFirstLetterKeepCase(user.name).contains(capitalizeFirstLetterKeepCase(userName)))) {
                        capitalizeFirstLetterKeepCase(user.name)
                        //getting the picture for each user separately
                        val task = pictureForSpecificUser(document.id).addOnSuccessListener { uri ->
                            //updating the current user's uri
                            user.image = uri.toString()
                            filteredUsers.add(user)
                        }
                        tasks.add(task)
                    }
                }

            }

            // Wait for all image download tasks to complete before posting the result
            Tasks.whenAllComplete(tasks).addOnCompleteListener { taskList ->
                if (taskList.isSuccessful) {
                    // All image download tasks completed successfully and also user's found with the name
                    if (filteredUsers.isNotEmpty()) {
                        Log.d("stamstamList","$filteredUsers")
                        data.postValue(Resource.Success(filteredUsers))
                    } else {
                        data.postValue(Resource.Error("No users found!"))
                    }
                } else {
                    // At least one image download task failed
                    data.postValue(Resource.Error("Failed to download some user images"))
                }
            }
        }
    }


    //     override fun getAllUsersLiveData(
//        data: MutableLiveData<Resource<List<User>>>,
//        dogName: String,
//        userName: String
//    ) {
//        data.postValue(Resource.Loading())
//        //listense to any changes in the user collection, if any change occured it will get update the list accordingly
//        userRef.addSnapshotListener{snapshot, e ->
//            if(e!=null){
//                data.postValue(Resource.Error(e.localizedMessage))
//            }
//
//            val filteredUsers = mutableListOf<User>()
//            snapshot?.documents?.forEach { document ->
//                val user = document.toObject(User::class.java)
//                if (user != null) {
//                    if (user.dogsName == dogName || user.name == userName) {
//                        // user.image = (pictureForSpecificUser(document.id)).toString()
//                        filteredUsers.add(user)
//                    }
//                }
//            }
//
//            if (filteredUsers.isNotEmpty()) {
//                data.postValue(Resource.Success(filteredUsers))
//            } else {
//                data.postValue(Resource.Error("No users found!"))
//            }
//        }
//    }

//    override suspend fun specificUser(user:User): Resource<User> {
//        return withContext(Dispatchers.IO) {
//            try {
//                val querySnapshot = userRef.get().await() // waiting for the result from the firebase
//                for (document in querySnapshot) {
//                    val user = document.toObject(User::class.java)
//                    if (user != null && (user.dogsName == dogName || user.name == userName)) {
//                        return@withContext Resource.Success(user)
//                    }
//                }
//                Resource.Error("User not found")
//            } catch (e: Exception) {
//                Resource.Error(e.message ?: "Unknown error occurred")
//            }
//        }
//    }

    override fun specificUser(data:MutableLiveData<Resource<User>>,user: String) {
        data.postValue(Resource.Loading())


        //listening to specific user changes -> meaning when we clicked on any user to see more details about them
        //then any changes in the comments or rating or anything else will be updated on live when someone else is watching him
        val userIdListener = userRef.document("/"+user)
        userIdListener.addSnapshotListener { snapshot, e ->
            if (e != null) {
                data.postValue(Resource.Error(e.localizedMessage))
                return@addSnapshotListener
            }
            if(snapshot!=null && snapshot.exists()){
                val tasks = mutableListOf<Task<Uri>>() // List to hold download tasks for each user's image
                //getting the updated user if any update occured meanwhile someone observing this user
                val updatedUser = snapshot.toObject(User::class.java)
                if(updatedUser!=null){
                    //Downloading the image for this user from the firestore storage
                    val task = pictureForSpecificUser(user).addOnSuccessListener { uri ->
                        //updating the current user's uri
                        updatedUser.image = uri.toString()
                    }
                    tasks.add(task)
                    // Wait for all image download tasks to complete before posting the result
                    Tasks.whenAllComplete(tasks).addOnCompleteListener { taskList ->
                        if (taskList.isSuccessful) {
                            // All image download tasks completed successfully
                            data.postValue(Resource.Success(updatedUser))
                        } else {
                            // At least one image download task failed
                            data.postValue(Resource.Error("Failed to download user's image"))
                        }
                    }
                }
                else {
                    data.postValue(Resource.Error("Couldn't find related user"))
                }

            }
            else {
                data.postValue(Resource.Error("Failed to show info about that user"))
            }
        }
    }

    override suspend fun setComment(comment: String, userId: String,context: Context) {
        try {
            val currentId = firebaseAuth.currentUser?.uid!!
            if (currentId == userId) {
                // You cant comment on yourself
                //Toast.makeText(context, "You can't comment on yourself", Toast.LENGTH_SHORT).show()
                return
            }

            //getting the name of the one that's writing the comments
            val currentLogged = userRef.document("/"+currentId).get().await()
            var currentNameLogged =""
            if(currentLogged.exists()){
                currentNameLogged = currentLogged.get("name").toString()
            }



            val userIdListener = userRef.document("/"+userId)
            val userDoc = userIdListener.get().await()
            if (userDoc.exists()) {
                val newUserComment = UserComments(currentNameLogged, formatDate(Date()), comment)
                userIdListener.update("comments", FieldValue.arrayUnion(newUserComment.toMap()))
                // Successfully added the comment
                //Toast.makeText(context, "Comment was successfully stored", Toast.LENGTH_SHORT).show()

            } else {
                // User doesn't exist
                //Toast.makeText(context, "User does not exist", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            //Toast.makeText(context, "Unknown error", Toast.LENGTH_SHORT).show()
        }
    }


    override suspend fun currentUserLoggedIn(userId: String) : Resource<Boolean>{
        try{
            val currentId = firebaseAuth.currentUser?.uid!!
            if(currentId == userId){
                return Resource.Success(true)
            }
            return Resource.Success(false)
        }
        catch (e:Exception){
            return Resource.Error(e.localizedMessage)
        }
    }

    private fun formatDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy")
        return dateFormat.format(date)
    }



    //delete comment by user who wrote it
    override suspend fun deleteComment(
        comment: String,
        userWatched: String,
        userWatches:String,
        date: String,
        data:MutableLiveData<Resource<String>>
    ){
        try{
            //get the document of the user we are currently watching
            val userProfileLooked = userRef.document(userWatched).get().await()
            //get the list of the comments for the user we are looking
            val commentsList = userProfileLooked.get("comments") as ArrayList<HashMap<String, Any>>
            //get the name of the user that is watching the currently user profile
            var userNameWatches=""
            userRef.get().addOnSuccessListener {
                for(document in it.documents){
                    val user = document.toObject<User>()
                    if (user != null) {
                        if(user.name.trim().equals(userWatches.trim())){
                            userNameWatches = user.name
                            break
                        }
                    }
                }
                //iterating over the items in the list of comments
                for (item in commentsList){
                    //that the comment we want to delete
                    if(item["comment"] == comment && item["date"]==date && item["userWrote"]==userNameWatches){
                        Log.d("stamstam","got here")
                        commentsList.remove(item)
                        break
                    }
                }
                userRef.document(userWatched).update("comments",commentsList)
                data.postValue(Resource.Success("Comment Deleted Successfully"))
            }
        }

        catch (e:Exception){
            data.postValue(Resource.Error("An error occurred"))
        }
    }

    override suspend fun getCurrentUserName(currentLoggedIn: String,data:MutableLiveData<Resource<String>>) {
        userRef.get().addOnSuccessListener {
            var userNameWatches=""
            for(document in it.documents){
                val user = document.toObject<User>()
                if (user != null) {
                    if(user.user_id.trim().equals(currentLoggedIn.trim())){
                        userNameWatches = user.name
                        data.postValue(Resource.Success(userNameWatches))
                    }
                }
            }
        }
    }
}