package il.example.dogtinder.repository

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import il.example.dogtinder.model.Event
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.flow.Flow


interface DogsRepository {
    suspend fun addEvent(date:String, city:String, street:String,desc:String,time:String) : Resource<Void>

    fun getEventsLiveData(data : MutableLiveData<Resource<List<Event>>>)

    fun getEventsFlow() : Flow<Resource<List<Event>>>

    suspend fun updateAttenders(idEvent:String,increase:Boolean,decrease:Boolean) : Resource<Void>

    suspend fun getCurrentUserPhoto() : Resource<Uri>

    suspend fun eventUserExistence()

    suspend fun deleteEvent(userId:String,eventId:String)

    suspend fun currentLoggedIn():String

    suspend fun editEvent(event:Event,location:String,street:String,date:String,description:String,time:String) : Resource<Any>

    suspend fun outdatedEventDateDelete()

    //suspend fun getCurrentName() : Resource<String>
}