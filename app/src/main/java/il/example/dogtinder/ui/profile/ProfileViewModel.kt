package il.example.dogtinder.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.repository.DogsRepository
import il.example.dogtinder.repository.ProfileRepository
import il.example.dogtinder.ui.dog_feed.DogsViewModel
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: ProfileRepository) : ViewModel() {


    private val _allUsersFound = MutableLiveData<Resource<List<User>>>()
    val allUsersFound :LiveData<Resource<List<User>>> = _allUsersFound


    private val _currentNameLogged = MutableLiveData<Resource<String>>()
    val currentNameLogged = _currentNameLogged

    //the getCurrentUserName will postValue inside this function and we will observe the currentNameLogged
    fun getCurrentName(currentId:String){
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCurrentUserName(currentId,_currentNameLogged)
        }
    }


    fun getAllUsers(dogName:String,ownerName:String,flag:String){
        viewModelScope.launch{
            _allUsersFound.postValue(Resource.Loading())
            repository.getAllUsersLiveData(_allUsersFound,dogName,ownerName,flag)
        }
    }



    //initializing the Repository instance in the Fragment associated with the viewModel

    class ProfileViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}