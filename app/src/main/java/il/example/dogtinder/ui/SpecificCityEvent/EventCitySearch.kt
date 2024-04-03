package il.example.dogtinder.ui.SpecificCityEvent

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import il.example.dogtinder.R
import il.example.dogtinder.databinding.FragmentSpecificCityBinding
import il.example.dogtinder.model.Event
import il.example.dogtinder.repository.Firebase.CitiesEventFireBase
import il.example.dogtinder.repository.GeoCoder.GeoCoderRepository
import il.example.dogtinder.ui.dog_feed.EventsAdapter
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EventCitySearch: Fragment() {

    private var binding : FragmentSpecificCityBinding by autoCleared()

    private var city=""

    private var currentLoggedIn = ""
    private var flagListWasntEmpty = false
    private var flagClickedShowMap = false
    private var flagEditEvent=false

    private val viewModel : EventsCityViewModel by viewModels() {
        EventsCityViewModel.EventsCitiesViewModelFactory(CitiesEventFireBase(),GeoCoderRepository(requireContext()))

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpecificCityBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLoggedIn = arguments?.getString("current_logged").toString()

        binding.eventsCityRv.layoutManager = LinearLayoutManager(requireContext())

        binding.eventsCityRv.adapter = EventsAdapter(currentLoggedIn,object :EventsAdapter.EventListener{
            override fun onEventClicked(
                event: Event,
                toIncrease: Boolean,
                toDecrease: Boolean,
                toShowAmount: Boolean,
                toShowInMaps:Boolean,
                toDeleteEvent:Boolean,
                toEditEvent:Boolean
            ) {
                //if we clicked on the plus button then we need to add the current use to the list of attending users since he wants to come to this event
                if(toIncrease){
                    viewModel.updateAttenders(event.eventId,toIncrease,toDecrease)
                }
                //if we clicked on the attenders amount and not to increase the amount or decrease
                else if (toShowAmount) {
                    findNavController().navigate(
                        R.id.action_eventCitySearch_to_attenders, bundleOf("list_attenders" to event.attenders, "user_created" to event.userCreated,
                        "event_id" to event.eventId)
                    )
                }
                else if(toDecrease) {
                    viewModel.updateAttenders(event.eventId, toIncrease, toDecrease)
                }
                //clicked to see in maps
                else if(toShowInMaps) {
                    viewModel.locationToLatLong(event)
                    //to avoid when rotating the screen and clicking once on show on map it will open the maps again even if not clicked on the button
                    flagClickedShowMap = true
                }
                //the user clicked to delete the event
                else if(toDeleteEvent) {
                    viewModel.deleteEvent(currentLoggedIn,event.eventId)
                }
                //User chose to edit a specific event
                else {
                    flagEditEvent = true
                    val dialogView =
                        LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_event, null)
                    val cityNameFinder = dialogView.findViewById<EditText>(R.id.city_name)
                    val streetNameFinder = dialogView.findViewById<EditText>(R.id.street_name)
                    val descriptionFinder = dialogView.findViewById<EditText>(R.id.description_name)
                    descriptionFinder.imeOptions = EditorInfo.IME_ACTION_DONE
                    val btn_date_picker = dialogView.findViewById<Button>(R.id.btn_date_picker)
                    val btn_time_picker = dialogView.findViewById<Button>(R.id.btn_time_picker)
                    var chosen_time = ""
                    var datePicked = ""

                    var city = ""
                    var street = ""
                    var description = ""
                    descriptionFinder.addTextChangedListener(object: TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                            description=s.toString()
                        }

                        override fun afterTextChanged(s: Editable?) {
                        }
                    })
                    cityNameFinder.addTextChangedListener(object: TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            city=s.toString()
                        }

                        override fun afterTextChanged(s: Editable?) {
                        }
                    })
                    streetNameFinder.addTextChangedListener(object: TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                            street = s.toString()
                        }

                        override fun afterTextChanged(s: Editable?) {
                        }
                    })

                    val text_date_picked = dialogView.findViewById<TextView>(R.id.text_date_picked)
                    val text_time_picked = dialogView.findViewById<TextView>(R.id.text_time_picked)

                    val btn_edit_event = dialogView.findViewById<Button>(R.id.btn_edit_event)
                    val btn_cancel_event = dialogView.findViewById<Button>(R.id.btn_cancel_event)

                    val rootDialog = dialogView.findViewById<LinearLayout>(R.id.dialog_add_event_root)
                    //when touching any where on the screen close the keyboard
                    rootDialog.setOnTouchListener { _, event ->
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(rootDialog.windowToken, 0)
                            return@setOnTouchListener true
                        }
                        return@setOnTouchListener false
                    }


                    //close the keyboard if pressed done when in the description text
                    descriptionFinder.setOnEditorActionListener { v, actionId, event ->
                        if(actionId==EditorInfo.IME_ACTION_DONE){
                            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                            true
                        }
                        else {
                            false
                        }
                    }


                    //a window to choose the time
                    btn_time_picker.setOnClickListener {
                        // Close the keyboard
                        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(btn_time_picker.windowToken, 0)

                        val calendar = Calendar.getInstance()
                        val hour = calendar.get(Calendar.HOUR_OF_DAY)
                        val minute = calendar.get(Calendar.MINUTE)
                        val timePickerDialog = TimePickerDialog(
                            requireContext(),
                            { _, selectedHour, selectedMinute ->

                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = System.currentTimeMillis()
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)

                                    // Get the specified time
                                    val specifiedTime = timeInMillis

                                    // Compare with the current time
                                    val currentTime = Calendar.getInstance().timeInMillis

                                    // If the specified time has already passed for the current day
                                    if (specifiedTime <= currentTime) {
                                        // Increment the day by one
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }

                                    // Format the hour and minute into a readable string
                                    chosen_time = String.format("%02d:%02d", selectedHour, selectedMinute)
                                    text_time_picked.text = chosen_time
                                }
                            },
                            hour,
                            minute,
                            false
                        )
                        // Show time picker dialog
                        timePickerDialog.show()
                    }


                    //Calendar instance to choose the date of the event
                    btn_date_picker.setOnClickListener {
                        // Close the keyboard
                        val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(btn_time_picker.windowToken, 0)

                        val calendar = Calendar.getInstance()
                        val datePickerDialog = DatePickerDialog(
                            requireContext(),
                            { _, year, monthOfYear, dayOfMonth ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(Calendar.YEAR, year)
                                selectedCalendar.set(Calendar.MONTH, monthOfYear)
                                selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                val formattedDate = dateFormat.format(selectedCalendar.time)
                                datePicked = formattedDate
                                text_date_picked.text = datePicked
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )

                        // Set the minimum date to today's date
                        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
                        datePickerDialog.show()
                    }

                    val alertDialogBuilder = AlertDialog.Builder(requireContext())
                        .setView(dialogView)


                    val alertDialog = alertDialogBuilder.create()
                    alertDialog.show()

                    //Add the event to the firebase
                    btn_edit_event.setOnClickListener {
                        val containsNumbers = city.any { it.isDigit() }
                        val streetOnlyNumbers = street.all { it.isDigit() }
                        if(containsNumbers && !city.isEmpty()){
                            Toast.makeText(requireContext(),"City field can't contain numbers",Toast.LENGTH_SHORT).show()
                        }
                        else if(streetOnlyNumbers && !street.isEmpty()){
                            Toast.makeText(requireContext(),"Street can't contain only numbers",Toast.LENGTH_SHORT).show()
                        }

                        else if(city.isEmpty() && street.isEmpty() && chosen_time.isEmpty() && datePicked.isEmpty() && description.isEmpty()){
                            Toast.makeText(requireContext(),"Please fill any field",Toast.LENGTH_SHORT).show()
                        }
                        else {
                            //this add event line of code will trigger the snapshot in our firebase which will post to the taskStatus under
                            viewModel.editEvent(event, city,street,datePicked,description,chosen_time)
                            alertDialog.dismiss()
                        }
                    }

                    //dont do anything just close the dialog
                    btn_cancel_event.setOnClickListener {
                        alertDialog.dismiss()
                    }

                }
            }
        })

        //getting the city name from the DogsFragment, to show all the events for this city
        city = arguments?.getString("city").toString()
        city?.let {
            viewModel.getAllEventsForCity(it)
        }




        //loading all the events for the specific city
        viewModel.citiesEvents.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                }

                is Resource.Success -> {
                    binding.titlePageEventCities.text = "Events in $city"
                    binding.ProgressBar.isVisible = false
                    //update the recycler's view list as of the current list of event
                    (binding.eventsCityRv.adapter as EventsAdapter).setList(it.data!!)
                    flagListWasntEmpty = true
                }

                //
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    //list was empty -> no events found in that city show the toast message indicating that no events found
                    if(!flagListWasntEmpty){
                        Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_eventCitySearch_to_dogsFragment,
                            bundleOf("current_user" to currentLoggedIn)
                        )
                    }
                    //list wasnt empty and a user deleted the event and the list got empty. then dont show the toast message saying couldn't find events in that city
                    else {
                        findNavController().navigate(R.id.action_eventCitySearch_to_dogsFragment,
                            bundleOf("current_user" to currentLoggedIn))
                    }

                }
            }
        }


        //getting the lat long from the city and the street corresponds to it and observing it in here to avoid screen rotations opening google maps again
        viewModel.latLong.observe(viewLifecycleOwner){
            //to avoid when rotating the screen to execute this code once again.
            if(flagClickedShowMap){
                when(it){
                    //sent an intent to open google maps with the following coordinates
                    //will save the concatedLatLong and will execute the intent inside the show in maps button clicked
                    is Resource.Success -> {
                        val latLongString = it.data // Assuming this string is in the format "longitude,latitude"
                        val latLongArray = latLongString!!.split(",")
                        val longitude = latLongArray[0]
                        val latitude = latLongArray[1]
                        val mapIntentUri = Uri.parse("geo:0,0?q=$latitude,$longitude(label)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps") // Specify the package name of Google Maps app to ensure it's used
                        if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                            startActivity(mapIntent)
                        } else {
                            // Google Maps app is not installed, handle accordingly (open web browser, show error message, etc.)
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps")
                            )
                            startActivity(intent)
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }


        //observing when the user chose to edit a specific event -> the one he created other's won't have this option
        viewModel.editEvent.observe(viewLifecycleOwner){
            if(flagEditEvent){
                when(it){
                    is Resource.Success -> {
                        Toast.makeText(requireContext(),"Successfully edited",Toast.LENGTH_SHORT).show()
                    }

                    is Resource.Error -> {
                        Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}