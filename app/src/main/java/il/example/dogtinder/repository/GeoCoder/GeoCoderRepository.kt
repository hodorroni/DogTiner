package il.example.dogtinder.repository.GeoCoder

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import il.example.dogtinder.model.Event
import il.example.dogtinder.utils.Resource
import java.util.Locale

class GeoCoderRepository(context:Context) : GeoCoderInterface {

    private val geocoder = Geocoder(context, Locale.ENGLISH)


    override fun locationToLatLong(event:Event) : Resource<String>{
        try{
            var lat_long =""
            val city = event.city
            val street = event.streetName
            val result : MutableList<Address> ? =geocoder.getFromLocationName("$city, $street",1)
            if(!result.isNullOrEmpty()){
                lat_long = result.get(0).longitude.toString()+","+result.get(0).latitude.toString()
                return Resource.Success(lat_long)
            }
            return Resource.Error("Couldn't find the coordinates for that location")
        }

        catch (e:Exception){
            return Resource.Error("Network call failed")
        }

    }


}