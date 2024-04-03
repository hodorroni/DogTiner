package il.example.dogtinder.repository.Firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import il.example.dogtinder.R
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepFirebase : AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()

    private val fireStore =FirebaseFirestore.getInstance().collection("user")

    private val firebaseStorage= FirebaseStorage.getInstance().reference

    override suspend fun currentUser(): Resource<User> {
        return withContext(Dispatchers.IO) {
            try {
                val user = firebaseAuth.currentUser?.let { fireStore.document(it.uid).get().await().toObject(User::class.java) }
                if (user != null) {
                    return@withContext Resource.Success(user)
                } else {
                    return@withContext Resource.Error("")
                }
            } catch (e: Exception) {
                return@withContext Resource.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        return withContext(Dispatchers.IO){
            try{
                val result =firebaseAuth.signInWithEmailAndPassword(email,password).await()
                val user = fireStore.document(result.user?.uid!!).get().await().toObject(User::class.java)!!
                return@withContext Resource.Success(user)
            }
            catch (e:Exception){
                return@withContext Resource.Error(e.message?:"Unknown error occurred")
            }
        }
    }

    override suspend fun createUser(
        username: String,
        dogsName: String,
        phone: String,
        email: String,
        password: String,
        image: String
    ): Resource<User> {
        return withContext(Dispatchers.IO){
            try{
                val registerResult =firebaseAuth.createUserWithEmailAndPassword(email,password).await()
                val userId = registerResult.user?.uid!!
                val newUser = User(username,dogsName,phone,email, password, image,0.0,userId)
                fireStore.document(userId).set(newUser).await()

                //storing the photo to the storage
                //when logging in and using toObject firestore supports only strings, so the image has to by of type string and here im convering to Uri
                val parsedImage = Uri.parse(image)
                firebaseStorage.child(userId).putFile(parsedImage).await()
                return@withContext Resource.Success(newUser)
            }
            catch (e:Exception){
                return@withContext Resource.Error(e.message?:"Unknown error occurred")
            }
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }


    override suspend fun checkUserExist(userId: String): Boolean {
        val users = fireStore.get().await()
        var flag = false
        for(user in users){
            if(user.get("user_id").toString().equals(userId)){
                flag = true
                break
            }
        }
        return flag
    }
}