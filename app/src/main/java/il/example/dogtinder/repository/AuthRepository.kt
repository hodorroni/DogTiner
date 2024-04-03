package il.example.dogtinder.repository

import android.content.Context
import android.net.Uri
import il.example.dogtinder.model.User
import il.example.dogtinder.utils.Resource


interface AuthRepository {

    suspend fun currentUser() : Resource<User>
    suspend fun login(email:String,password:String) : Resource<User>
    suspend fun createUser(username:String,dogsName:String,phone:String,email:String, password:String,image:String) : Resource<User>

    suspend fun checkUserExist(userId:String) : Boolean


    //suspend fun getCurrentPicture() : Resource<String>

    fun logout()
}