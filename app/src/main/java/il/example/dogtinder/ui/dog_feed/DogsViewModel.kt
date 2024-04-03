package il.example.dogtinder.ui.dog_feed

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.Event
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.repository.DogsRepository
import il.example.dogtinder.repository.GeoCoder.GeoCoderInterface
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class DogsViewModel(private val authRepository:AuthRepository, private val eventRepository: DogsRepository,
                    private val geoCoder: GeoCoderInterface
) : ViewModel() {



    //for the repository to make postValue
    //passing it to the firebase which will make the postValues to the fragment, we will observe on taskStatus
    //val _tasksStatus : MutableLiveData<Resource<List<Event>>> = MutableLiveData()

    //when not using the Flow we will use this instead
    //val taskStatus: LiveData<Resource<List<Event>>> = _tasksStatus


    //when using Flow instead of the above
    val taskStatus : LiveData<Resource<List<Event>>> = eventRepository.getEventsFlow().flowOn(Dispatchers.IO).asLiveData()


    //Adding taskStatus
    private val _addTaskStatus = MutableLiveData<Resource<Void>>()

    val addTaskStatus : LiveData<Resource<Void>> = _addTaskStatus



    //Getting the current user photo from the firebase storage
    private val _userImage = MutableLiveData<Resource<Uri>>()
    val userImage : LiveData<Resource<Uri>> = _userImage



    private val _userExist = MutableLiveData<Boolean>()
    val userExist :LiveData<Boolean> = _userExist


    //latitude and logitude for each event based on the city name and the street
    private val _latLong = MutableLiveData<Resource<String>>()
    val latLong : LiveData<Resource<String>> = _latLong


    //editing specific event that was created by an user
    private val _editEvent = MutableLiveData<Resource<Any>>()
    val editEvent : LiveData<Resource<Any>> = _editEvent


//    val latLongTransformation = MutableLiveData<Event>()
//    val transformationLatLong =latLongTransformation.switchMap {concatedLatLong ->
//        liveData(viewModelScope.coroutineContext + Dispatchers.IO){
//            emit(Resource.Loading())
//            val result = geoCoder.locationToLatLong(concatedLatLong)
//            if(result is Resource.Success){
//                emit(Resource.Success(result.data))
//            }
//            else {
//                emit(Resource.Error(result.message.toString()))
//            }
//        }
//    }
//
//    //function for the transformation
//    fun setLatLong(event: Event){
//        if(latLongTransformation.value!=event){
//            latLongTransformation.value = event
//        }
//    }




    init {
        //Deleting events that were previously created by users which has deleted if there is any user that got deleted
        viewModelScope.launch(Dispatchers.IO) {
            //get the image for the user
            getUserImage()

            //delete all the events that the current date is bigger than the date they were set to
            eventRepository.outdatedEventDateDelete()



            //checking if user exist in the database maybe admin deleted him, if not exist then delete all the events this user created
            eventRepository.eventUserExistence()

            //passing to the repository the MutableLiveData for all the lists
            //in that way every change in the DB the snapshot will get called in the rep and update the list


            //when not using flow we will use this and we would comment out the taskStatus above that uses flow @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
            //eventRepository.getEventsLiveData(_tasksStatus)


        }
    }


    fun eventUserExist() {
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.eventUserExistence()
        }
    }


    fun getUserImage(){
        viewModelScope.launch {
            val uri = eventRepository.getCurrentUserPhoto()
            _userImage.postValue(uri)
        }
    }


    fun addEvent(date:String, city:String,street:String,desc:String,time:String){
            viewModelScope.launch {
                _addTaskStatus.postValue(Resource.Loading())
                eventRepository.addEvent(date,city,street,desc,time)
            }
    }

    fun updateAttenders(idEvent:String,increase:Boolean, decrease:Boolean){
            viewModelScope.launch {
                val updated = eventRepository.updateAttenders(idEvent,increase,decrease)
            }
    }


    fun logout(){
        authRepository.logout()
    }

    //check if the user exist in the database and wasn't removed by the admins. if so redirect the user to the log in screen (logging him out)
    fun checkUserExist(userId:String){
        viewModelScope.launch(Dispatchers.IO) {
            _userExist.postValue(authRepository.checkUserExist(userId))
        }
    }



    //getting the lat and long of the event based on that city and street
    fun locationToLatLong(event:Event){
        viewModelScope.launch(Dispatchers.IO) {
            val result = geoCoder.locationToLatLong(event)
            _latLong.postValue(result)
        }
    }

    //delete a specific event
    fun deleteEvent(userId:String,eventId:String){
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.deleteEvent(userId,eventId)
        }
    }


    fun editEvent( event: Event, location: String,street:String, date: String, description: String,time:String){
        viewModelScope.launch(Dispatchers.IO) {
            _editEvent.postValue(eventRepository.editEvent(event,location,street,date,description,time))
        }
    }



    //initializing the Repository instance in the Fragment associated with the viewModel

    class DogsViewModelFactory(private val authRepository:AuthRepository,private val repository: DogsRepository,
                               private val geoCoder: GeoCoderInterface
    ) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DogsViewModel(authRepository,repository,geoCoder) as T
        }
    }

}