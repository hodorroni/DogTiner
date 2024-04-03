package il.example.dogtinder.repository.GeoCoder

import il.example.dogtinder.model.Event
import il.example.dogtinder.utils.Resource

interface GeoCoderInterface {
    fun locationToLatLong(event: Event) : Resource<String>
}