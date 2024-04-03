package il.example.dogtinder.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import il.example.dogtinder.model.Event
import il.example.dogtinder.model.User
import il.example.dogtinder.utils.Resource

interface ProfileRepository {
    fun getAllUsersLiveData(data: MutableLiveData<Resource<List<User>>>,dogName:String="",userName:String="",flag:String)
    fun specificUser(data:MutableLiveData<Resource<User>>,user: String)
    fun pictureForSpecificUser(userId:String) : Task<Uri>
    suspend fun setComment(comment:String,userId: String,context: Context)
    suspend fun currentUserLoggedIn(userId: String) : Resource<Boolean>

    suspend fun deleteComment(comment: String, userWatched: String, userWatches:String,date: String,data:MutableLiveData<Resource<String>>)

    suspend fun getCurrentUserName(currentLoggedIn:String,data:MutableLiveData<Resource<String>>)

}