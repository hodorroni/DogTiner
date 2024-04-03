package il.example.dogtinder.model

import java.util.Date

data class UserComments(val userWrote:String="", val date: String="", val comment:String=""){
    fun toMap(): Map<String, Any> {
        return mapOf(
            "userWrote" to userWrote,
            "date" to date,
            "comment" to comment
        )
    }
}
