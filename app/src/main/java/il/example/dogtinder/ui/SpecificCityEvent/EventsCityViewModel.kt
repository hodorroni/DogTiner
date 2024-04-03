package il.example.dogtinder.ui.SpecificCityEvent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.Event
import il.example.dogtinder.repository.CitiesEvent
import il.example.dogtinder.repository.GeoCoder.GeoCoderInterface
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class EventsCityViewModel(private val repository:CitiesEvent, private val geoCoder: GeoCoderInterface) : ViewModel() {

    private val _citiesEvents = MutableLiveData<Resource<List<Event>>>()
    val citiesEvents : LiveData<Resource<List<Event>>> = _citiesEvents


    //latitude and logitude for each event based on the city name and the street
    private val _latLong = MutableLiveData<Resource<String>>()
    val latLong : LiveData<Resource<String>> = _latLong


    private val _editEvent = MutableLiveData<Resource<Any>>()
    val editEvent : LiveData<Resource<Any>> = _editEvent



    //getting the events for the specificcity
    fun getAllEventsForCity(city:String){
        viewModelScope.launch(Dispatchers.IO) {
            repository.getAllEventsByCity(city).flowOn(Dispatchers.IO).collect{eventsResource ->
                _citiesEvents.postValue(eventsResource)
            }
        }
    }



    //update the amount of people that attending to a specific event
    fun updateAttenders(idEvent:String,increase:Boolean, decrease:Boolean){
        viewModelScope.launch {
            repository.updateAttenders(idEvent,increase,decrease)
        }
    }



    //getting the lat and long of the event based on that city and street
    fun locationToLatLong(event:Event){
        viewModelScope.launch(Dispatchers.IO) {
            _latLong.postValue(geoCoder.locationToLatLong(event))
        }
    }


    //delete a specific event
    fun deleteEvent(userId:String,eventId:String){
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEvent(userId,eventId)
        }
    }


    fun editEvent( event: Event, location: String,street:String, date: String, description: String,time:String){
        viewModelScope.launch(Dispatchers.IO) {
            _editEvent.postValue(repository.editEvent(event,location,street,date,description,time))
        }
    }







    //initializing the Repository instance in the Fragment associated with the viewModel

    class EventsCitiesViewModelFactory(private val repository:CitiesEvent,private val geoCoder: GeoCoderInterface) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EventsCityViewModel(repository,geoCoder) as T
        }
    }
}