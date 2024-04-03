package il.example.dogtinder.model

import android.net.Uri
import il.example.dogtinder.R

data class User(val name:String="", val dogsName:String="", val phone:String="", val email:String="", val password:String="",
                var image:String="", val rating:Double=0.0, val user_id:String="", val comments:List<UserComments> = emptyList())
