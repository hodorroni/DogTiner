package il.example.dogtinder.repository

import androidx.lifecycle.MutableLiveData
import il.example.dogtinder.model.Event
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.flow.Flow


interface CitiesEvent {

    fun getAllEventsByCity(city:String): Flow<Resource<List<Event>>>

    suspend fun updateAttenders(idEvent:String,increase:Boolean,decrease:Boolean) : Resource<Void>

    suspend fun deleteEvent(userId: String,eventId:String)

    suspend fun editEvent(event:Event,location:String,street:String,date:String,description:String,time:String) : Resource<Any>
}