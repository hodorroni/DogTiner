package il.example.dogtinder.model

data class Event(
    var date:String="", var city:String="", val userCreated:String="", val userName:String="", val eventId:String=""
    , val streetName:String="", var description:String="", val time:String="", var attenders: List<String> = emptyList())
