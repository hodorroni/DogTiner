package il.example.dogtinder.ui.specificUser

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.ProfileRepository
import il.example.dogtinder.ui.profile.ProfileViewModel
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpecificUserViewModel(private val repository: ProfileRepository) : ViewModel() {


    private val _currentUser = MutableLiveData<Resource<User>>()
    val currentUser :LiveData<Resource<User>> = _currentUser

    var rotationScreenHandle = 0

    private val _currentLoggedIn = MutableLiveData<Resource<Boolean>>()
    val currentLoggedIn :LiveData<Resource<Boolean>> = _currentLoggedIn


    private val _deleteCommentStatus = MutableLiveData<Resource<String>>()
    val deleteCommentStatus : LiveData<Resource<String>> = _deleteCommentStatus

    fun getUser(user:String){
        viewModelScope.launch(Dispatchers.IO) {
            repository.specificUser(_currentUser,user)
        }
    }

    fun setComment(comment:String,userId: String,context: Context){
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("stamstam","WENT TO VIEWMODEL THEN TO THE REPOSITORY")
            repository.setComment(comment, userId, context)
        }
    }

    fun setRotationValue(){
        rotationScreenHandle++
    }


    fun currentUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentLoggedIn.postValue(repository.currentUserLoggedIn(userId))
        }
    }

    fun deleteComment(comment: String, userWatched: String, userWatches:String, date: String){
        //_deleteCommentStatus.postValue(Resource.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(comment,userWatched,userWatches,date,_deleteCommentStatus)
        }
    }



    //initializing the Repository instance in the Fragment associated with the viewModel

    class ProfileViewModelFactory(private val repository: ProfileRepository) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SpecificUserViewModel(repository) as T
        }
    }
}