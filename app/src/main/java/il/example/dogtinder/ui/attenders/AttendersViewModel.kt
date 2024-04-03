package il.example.dogtinder.ui.attenders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.repository.DogsRepository
import il.example.dogtinder.ui.dog_feed.DogsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttendersViewModel(private val eventRepository: DogsRepository) : ViewModel() {

    private val _currentUser = MutableLiveData<String>()
    val currentUser :LiveData<String> = _currentUser


    private val _deletionStatus = MutableLiveData<Boolean>(false)
    val deletionStatus : LiveData<Boolean> = _deletionStatus

    //get the current used logged in to the application
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val user = eventRepository.currentLoggedIn()
            _currentUser.postValue(user)
        }
    }

    //delete a specific event
    fun deleteEvent(userId:String,eventId:String){
        viewModelScope.launch(Dispatchers.IO) {
            eventRepository.deleteEvent(userId,eventId)
            _deletionStatus.postValue(true)
        }
    }






    //initializing the Repository instance in the Fragment associated with the viewModel

    class AttendersViewModelFactory( private val repository: DogsRepository) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AttendersViewModel(repository) as T
        }
    }


}