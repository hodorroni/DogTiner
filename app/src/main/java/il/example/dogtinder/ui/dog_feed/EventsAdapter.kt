package il.example.dogtinder.ui.dog_feed

import android.app.ActionBar.LayoutParams
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import il.example.dogtinder.databinding.EventLayoutBinding
import il.example.dogtinder.model.Event
import il.example.dogtinder.utils.Resource

class EventsAdapter(private val currentUserLogged:String,private val callback:EventListener) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    private val events =ArrayList<Event>()


    fun setList(events:Collection<Event>){
        this.events.clear()
        this.events.addAll(events)
        notifyDataSetChanged()
    }


    interface EventListener{
        fun onEventClicked(event:Event,toIncrease:Boolean,toDecrease:Boolean,toShowAmount:Boolean,toShowInMaps:Boolean,
                           toDeleteEvent:Boolean,toEditEvent:Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        return EventViewHolder(EventLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int {
        return events.size
    }

    inner class EventViewHolder(private val binding:EventLayoutBinding) :RecyclerView.ViewHolder(binding.root){

        init {
            //when clicking on the +1 icon then pass the current event that was "clicked"
            binding.attendersPlusOne.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],true,false,false,false,false,false)
            }

            //when clicking to show the amount of people to navigate to different screen
            binding.totalAttenders.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],false,false,true,false,false,false)
            }

            //when clicking on -1 icon to decrease the amount of people
            binding.attendersMinusOne.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],false,true,false,false,false,false)
            }

            //when clicking on the show on maps
            binding.btnShowMaps.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],false,false,false,true,false,false)
            }

            //when clicking to delete an event that was created by a specific user
            binding.deleteBtnEvent.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],false,false,false,false,true,false)
            }
            binding.editBtnEvent.setOnClickListener {
                callback.onEventClicked(events[adapterPosition],false,false,false,false,false,true)
            }
        }


        fun bind(event:Event){
            // Create SpannableString for each TextView
            val locationSpannable = SpannableString("Location: ${event.city}, ${event.streetName}")
            val dateSpannable = SpannableString("Date: ${event.date}")
            val userCreatedSpannable = SpannableString("User Created: ${event.userName}")
            val attendersSpannable = SpannableString("Attenders: ${event.attenders.size}")
            val descriptionSpannable = SpannableString("Description: ${event.description}")

            // Set text color for each part of the SpannableString
            locationSpannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            locationSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 9, locationSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            dateSpannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            dateSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 5, dateSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            userCreatedSpannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, 13, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            userCreatedSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 13, userCreatedSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            attendersSpannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, 10, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            attendersSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 10, attendersSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            descriptionSpannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, 12, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            descriptionSpannable.setSpan(ForegroundColorSpan(Color.WHITE), 12, descriptionSpannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Set SpannableString to TextViews
            binding.cityEvent.text = locationSpannable
            binding.dateEvent.text = dateSpannable
            binding.usernameEvent.text = userCreatedSpannable
            binding.totalAttenders.text = attendersSpannable
            binding.descEvent.text = descriptionSpannable
            //check if the current user logged in is the one that created this event if so show the delete button and the edit button
            binding.deleteBtnEvent.isVisible = currentUserLogged.equals(event.userCreated)
            binding.editBtnEvent.isVisible = currentUserLogged.equals(event.userCreated)

            //if the current user is the one that created the event dont show the plus and minus one buttons cause he can't attend to his own event
            binding.attendersPlusOne.isVisible = !currentUserLogged.equals(event.userCreated)
            binding.attendersMinusOne.isVisible = !currentUserLogged.equals(event.userCreated)
        }
    }

}