package il.example.dogtinder.ui.register

import android.content.Context
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.utils.Resource
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository:AuthRepository) : ViewModel() {


    private val _registerStatus =  MutableLiveData<Resource<User>>()

    val registerStatus : LiveData<Resource<User>> = _registerStatus

    private val _photoUri = MutableLiveData<Uri>()
    val photoUri : LiveData<Uri> get() = _photoUri


    fun createUser(name:String,dogsName:String,phone:String,email:String,password:String,image:String){
        _registerStatus.postValue(Resource.Loading())
        if(name.isEmpty() || dogsName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty()){
            _registerStatus.postValue(Resource.Error("You have to fill the fields to register"))
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            _registerStatus.postValue(Resource.Error("Not a valid email "))
        }
        else {
            viewModelScope.launch {
                val registerResult = repository.createUser(name,dogsName,phone, email,password ,image)
                _registerStatus.postValue(registerResult)
            }
        }

    }

    fun setUri(uri:Uri){
        _photoUri.value = uri
    }


    //initializing the Repository instance in the Fragment associated with the viewModel

    class RegisterViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.NewInstanceFactory(){
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegisterViewModel(repository) as T
        }
    }


}