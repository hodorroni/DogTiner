package il.example.dogtinder.ui.login

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository,application: Application) : AndroidViewModel(application) {


    private val _currentUser = MutableLiveData<Resource<User>>()
    val currentUser : LiveData<Resource<User>> = _currentUser


    private val _loginStatus = MutableLiveData<Resource<User>>()
    val loginStatus : LiveData<Resource<User>> = _loginStatus

    //if the app remembers us as we are logged in then get the current user from the DB
    //and send a Toast to the user with Welcome Back.
    init {
        viewModelScope.launch {
            _currentUser.postValue(Resource.Loading())
            val currentResult = repository.currentUser()
            //if the user isn't null from the firebase then it "remembers" us and we can welcome back the user
            if(currentResult is Resource.Success){
                Toast.makeText(application.applicationContext,"Welcome Back",Toast.LENGTH_SHORT).show()
            }
            _currentUser.postValue(currentResult)
        }
    }



    fun login(email: String, password: String){
        if(email.isEmpty() || password.isEmpty()){
            _loginStatus.postValue(Resource.Error("Fill the required fields"))
        }
        else {
            _loginStatus.postValue(Resource.Loading())
            viewModelScope.launch {
                val loginResult = repository.login(email,password)
                _loginStatus.postValue(loginResult)
            }
        }

    }


    //initializing the Repository instance in the Fragment associated with the viewModel

    class LoginViewModelFactory(private val repository: AuthRepository, private val application: Application) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(repository,application) as T
        }
    }





}