package il.example.dogtinder.ui.dog_feed

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import il.example.dogtinder.R
import il.example.dogtinder.databinding.FragmentDogsBinding
import il.example.dogtinder.model.Event
import il.example.dogtinder.repository.Firebase.AuthRepFirebase
import il.example.dogtinder.repository.Firebase.EventRepFirebase
import il.example.dogtinder.repository.GeoCoder.GeoCoderRepository
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DogsFragment:Fragment() {

    private var binding : FragmentDogsBinding by autoCleared()

    private val viewModel : DogsViewModel by viewModels() {
        DogsViewModel.DogsViewModelFactory(AuthRepFirebase(),EventRepFirebase(),
            GeoCoderRepository(requireContext()))
    }

    private var currentLoggedIn = ""

    private var job: Job? = null

    private var flagClickedShowMap = false

    private var flagEditEvent = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDogsBinding.inflate(inflater,container,false)
        setHasOptionsMenu(true)
        return binding.root
    }



    //Job to run on the back thread to check if any user deleted himself if so, delete this event and update the UI accordingly
    private fun startPeriodicTask() {
        if(job==null || job?.isActive!=true){
            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    // checking if any user deleted himself which made an event. if so update the UI every x time and delete those events for the rest of the users on live
                    viewModel.eventUserExist()

                    //checking if the user isnt deleted by admins. if so log him out from the app -> redirect to the log in page.
                    viewModel.checkUserExist(currentLoggedIn)

                    // Delay for 10 seconds
                    delay(10_000L)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel the coroutine job when the view is destroyed to avoid leaks
        job?.cancel()
    }

    override fun onResume() {
        super.onResume()
        flagEditEvent = false
        flagClickedShowMap = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //getting the current user that logged in or from the login page (it can remember us automatically too) or from the register pae
         arguments?.getString("current_login").let {
             if(it!=null){
                 currentLoggedIn =it
                 startPeriodicTask()
             }
         }
        if(currentLoggedIn==""){
            //getting the current user from the register page if we just registered and we came here not from the login page
             arguments?.getString("current_register").let {
                if(it!=null){
                    currentLoggedIn = it
                }
            }
            //We got here by clicking the back button in the attenders list or from the SpecificCity search when no events found  for that city
            //or when the user deleted all the events he created possible for that city
            if(currentLoggedIn==""){
                currentLoggedIn = arguments?.getString("current_user").toString()
            }
            startPeriodicTask()
        }


        binding.eventsRv.layoutManager = LinearLayoutManager(requireContext())


        binding.eventsRv.adapter = EventsAdapter(currentLoggedIn,object : EventsAdapter.EventListener {
            //Attending people to the event to update
            override fun onEventClicked(
                event: Event,
                toIncrease: Boolean,
                toDecrease: Boolean,
                toShowAmount: Boolean,
                toShowInMaps:Boolean,
                toDeleteEvent:Boolean,
                toEditEvent:Boolean
            ) {
                if (toIncrease) {
                    viewModel.updateAttenders(event.eventId, toIncrease, toDecrease)
                }
                //if we clicked on the attenders amount and not to increase the amount or decrease
                else if (toShowAmount) {
                    //avoid to open maps again if was clicked once and returned into the page
                    flagClickedShowMap = false

                    findNavController().navigate(
                        R.id.action_dogsFragment_to_attenders, bundleOf(
                            "list_attenders" to event.attenders,
                            "user_created" to event.userCreated,
                            "event_id" to event.eventId
                        )
                    )
                } else if(toDecrease) {
                    viewModel.updateAttenders(event.eventId, toIncrease, toDecrease)
                }
                //clicked to see in maps, get the lat and long based on the city and street name
                else if(toShowInMaps) {

                    //a flag to avoid screen rotations which open the maps once again if we clicked previously
                    //using it when observing the latlong values
                    flagClickedShowMap = true
                    viewModel.locationToLatLong(event)
                }
                //the user clicked to delete the event
                else if(toDeleteEvent) {
                    viewModel.deleteEvent(currentLoggedIn,event.eventId)
                }

                //User chose to edit a specific event
                else {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_event, null)
                    val cityNameFinder = dialogView.findViewById<EditText>(R.id.city_name)
                    cityNameFinder.imeOptions = EditorInfo.IME_ACTION_DONE
                    val streetNameFinder = dialogView.findViewById<EditText>(R.id.street_name)
                    streetNameFinder.imeOptions = EditorInfo.IME_ACTION_DONE
                    val descriptionFinder = dialogView.findViewById<EditText>(R.id.description_name)
                    descriptionFinder.imeOptions = EditorInfo.IME_ACTION_DONE
                    val btn_date_picker = dialogView.findViewById<Button>(R.id.btn_date_picker)
                    val btn_time_picker = dialogView.findViewById<Button>(R.id.btn_time_picker)
                    var chosen_time = ""
                    var datePicked = ""

                    var city = ""
                    var street = ""
                    var description = ""
                    descriptionFinder.addTextChangedListener(object: TextWatcher{
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
                    cityNameFinder.addTextChangedListener(object: TextWatcher{
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
                    streetNameFinder.addTextChangedListener(object: TextWatcher{
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

                    //close the keyboard if pressed done when in the description text
                    cityNameFinder.setOnEditorActionListener { v, actionId, event ->
                        if(actionId==EditorInfo.IME_ACTION_DONE){
                            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                            true
                        }
                        else {
                            false
                        }
                    }

                    //close the keyboard if pressed done when in the description text
                    streetNameFinder.setOnEditorActionListener { v, actionId, event ->
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
                        flagEditEvent = true
                        if(containsNumbers && !city.isEmpty()){
                            Toast.makeText(requireContext(),"City can't contain numbers",Toast.LENGTH_SHORT).show()
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


//        binding.searchUsers.setOnClickListener {
//            //avoid to open maps again if was clicked once and returned into the page
//            flagClickedShowMap = false
//            findNavController().navigate(R.id.action_dogsFragment_to_usersSearchFragment,
//                bundleOf("current_user" to currentLoggedIn)
//            )
//        }

        //searching events by a specific city name
//        binding.cityBtnSearch.setOnClickListener {
//            val dialogView =
//                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_events_city, null)
//            val editTextComment = dialogView.findViewById<EditText>(R.id.editTextComment)
//
//            val alertDialogBuilder = AlertDialog.Builder(requireContext())
//                .setView(dialogView)
//                .setPositiveButton("Search") { dialog, which ->
//                    val city = editTextComment.text.toString()
//                    if (city.isNotBlank()) {
//                        //avoid to open maps again if was clicked once and returned into the page
//                        flagClickedShowMap = false
//                        //Moving to show the events for the specific city
//                        findNavController().navigate(
//                            R.id.action_dogsFragment_to_eventCitySearch,
//                            bundleOf("city" to city, "current_logged" to currentLoggedIn)
//                        )
//                    } else {
//                        Toast.makeText(requireContext(), "City field is empty", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//                }
//                .setNegativeButton("Cancel") { dialog, which ->
//                    dialog.dismiss()
//                }
//
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
//        }



        //Logout button
//        binding.logoutBtnDogs.setOnClickListener {
//            viewModel.logout()
//            //to avoid the toast error message in the login fragment
//            findNavController().navigate(R.id.action_dogsFragment_to_loginFragment)
//        }


        //adding a new event to the database
//        binding.btnAddEvent.setOnClickListener {
//
//            val dialogView =
//                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
//            val cityNameFinder = dialogView.findViewById<EditText>(R.id.city_name)
//            val streetNameFinder = dialogView.findViewById<EditText>(R.id.street_name)
//            val descriptionFinder = dialogView.findViewById<EditText>(R.id.description_name)
//            val btn_date_picker = dialogView.findViewById<Button>(R.id.btn_date_picker)
//            val btn_time_picker = dialogView.findViewById<Button>(R.id.btn_time_picker)
//            var chosen_time = ""
//            var datePicked = ""
//
//            val text_date_picked = dialogView.findViewById<TextView>(R.id.text_date_picked)
//            val text_time_picked = dialogView.findViewById<TextView>(R.id.text_time_picked)
//
//            val btn_add_event = dialogView.findViewById<Button>(R.id.btn_Add_event)
//            val btn_cancel_event = dialogView.findViewById<Button>(R.id.btn_cancel_event)
//
//
//            //a window to choose the time
//            btn_time_picker.setOnClickListener {
//                val calendar = Calendar.getInstance()
//                val hour = calendar.get(Calendar.HOUR_OF_DAY)
//                val minute = calendar.get(Calendar.MINUTE)
//                val timePickerDialog = TimePickerDialog(
//                    requireContext(),
//                    { _, selectedHour, selectedMinute ->
//
//                        val calendar = Calendar.getInstance().apply {
//                            timeInMillis = System.currentTimeMillis()
//                            set(Calendar.HOUR_OF_DAY, selectedHour)
//                            set(Calendar.MINUTE, selectedMinute)
//                            set(Calendar.SECOND, 0)
//
//                            // Get the specified time
//                            val specifiedTime = timeInMillis
//
//                            // Compare with the current time
//                            val currentTime = Calendar.getInstance().timeInMillis
//
//                            // If the specified time has already passed for the current day
//                            if (specifiedTime <= currentTime) {
//                                // Increment the day by one
//                                add(Calendar.DAY_OF_MONTH, 1)
//                            }
//
//                            // Format the hour and minute into a readable string
//                            chosen_time = String.format("%02d:%02d", selectedHour, selectedMinute)
//                            text_time_picked.text = chosen_time
//                        }
//                    },
//                    hour,
//                    minute,
//                    false
//                )
//                // Show time picker dialog
//                timePickerDialog.show()
//            }
//
//
//            //Calendar instance to choose the date of the event
//            btn_date_picker.setOnClickListener {
//                val calendar = Calendar.getInstance()
//                val datePickerDialog = DatePickerDialog(
//                    requireContext(),
//                    { _, year, monthOfYear, dayOfMonth ->
//                        val selectedCalendar = Calendar.getInstance()
//                        selectedCalendar.set(Calendar.YEAR, year)
//                        selectedCalendar.set(Calendar.MONTH, monthOfYear)
//                        selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
//
//                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
//                        val formattedDate = dateFormat.format(selectedCalendar.time)
//                        datePicked = formattedDate
//                        text_date_picked.text = datePicked
//                    },
//                    calendar.get(Calendar.YEAR),
//                    calendar.get(Calendar.MONTH),
//                    calendar.get(Calendar.DAY_OF_MONTH)
//                )
//
//                // Set the minimum date to today's date
//                datePickerDialog.datePicker.minDate = System.currentTimeMillis()
//                datePickerDialog.show()
//            }
//
//
//
//
//
//            val alertDialogBuilder = AlertDialog.Builder(requireContext())
//                .setView(dialogView)
//
//
//            val alertDialog = alertDialogBuilder.create()
//            alertDialog.show()
//
//            //Add the event to the firebase
//                btn_add_event.setOnClickListener {
//                    val city = cityNameFinder.text.toString()
//                    val street = streetNameFinder.text.toString()
//                    val description = descriptionFinder.text.toString()
//                    if(city.isEmpty() || street.isEmpty() || chosen_time.isEmpty() || datePicked.isEmpty() || description.isEmpty()){
//                        Toast.makeText(requireContext(),"Please fill all the fields",Toast.LENGTH_SHORT).show()
//                    }
//                    else {
//                        //this add event line of code will trigger the snapshot in our firebase which will post to the taskStatus under
//                        viewModel.addEvent(datePicked,city,street,description,chosen_time)
//                        alertDialog.dismiss()
//                    }
//                }
//
//            //dont do anything just close the dialog
//                    btn_cancel_event.setOnClickListener {
//                        alertDialog.dismiss()
//                    }
//        }


        //this will get triggered a lot, since of every change in our list this will be observed from our EventRepFirebase which posts values to the mutableData
        viewModel.taskStatus.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading ->{
                    binding.ProgressBar.isVisible = true
                    //binding.btnAddEvent.isEnabled = false
                }
                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    //binding.btnAddEvent.isEnabled = true
                    //will trigger the bottom observe code

                    //for every change in our List this line will be called a lot
//                    binding.newCityAdded.text = it.data?.get(0)?.city.toString()
//                    binding.nameUserAdded.text = it.data?.get(0)?.userName.toString()

                    (binding.eventsRv.adapter as EventsAdapter).setList(it.data!!)
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    //binding.btnAddEvent.isEnabled = true
                    Toast.makeText(requireContext(),it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        //Adding event to the list of events
        //the above taskStatus will get called since the addSnapshotListener that attached to the list and listens to any changes for the list
        viewModel.addTaskStatus.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading ->{
                    binding.ProgressBar.isVisible = true
                }
                is Resource.Success ->{
                    binding.ProgressBar.isVisible = false
                    Toast.makeText(requireContext(),"Event Added",Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                }
            }
        }




        //Getting the image of the user from the firestore
        viewModel.userImage.observe(viewLifecycleOwner) { resource ->
            when (resource) {

                is Resource.Success -> {
                    val uri = resource.data // Extract URI from Resource
                    binding.ProgressBar.isVisible = false
                    Glide.with(this).load(uri).transform(CircleCrop()).into(binding.imagePersonDogs)
                }
                is Resource.Error -> {
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                    binding.ProgressBar.isVisible = false
                }
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                }
            }
        }



        //if user got deleted by the admins redirect him to the log in page -> log him out
        viewModel.userExist.observe(viewLifecycleOwner){
            //the user isnt found anymore in the database
            if(!it){
                findNavController().navigate(R.id.action_dogsFragment_to_loginFragment)
            }
        }



        //getting the lat long from the city and the street corresponds to it and observing it in here to avoid screen rotations opening google maps again
        viewModel.latLong.observe(viewLifecycleOwner){
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.spinner_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.item_search_users){
            //avoid to open maps again if was clicked once and returned into the page
            flagClickedShowMap = false
            findNavController().navigate(R.id.action_dogsFragment_to_usersSearchFragment,
                bundleOf("current_user" to currentLoggedIn)
            )
        }
        else if(item.itemId == R.id.item_add_event){
            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_event, null)
            val cityNameFinder = dialogView.findViewById<EditText>(R.id.city_name)
            cityNameFinder.imeOptions = EditorInfo.IME_ACTION_DONE
            val streetNameFinder = dialogView.findViewById<EditText>(R.id.street_name)
            streetNameFinder.imeOptions = EditorInfo.IME_ACTION_DONE
            val descriptionFinder = dialogView.findViewById<EditText>(R.id.description_name)
            descriptionFinder.imeOptions = EditorInfo.IME_ACTION_DONE
            val btn_date_picker = dialogView.findViewById<Button>(R.id.btn_date_picker)
            val btn_time_picker = dialogView.findViewById<Button>(R.id.btn_time_picker)
            var chosen_time = ""
            var datePicked = ""


            var city = ""
            var street = ""
            var description = ""
            descriptionFinder.addTextChangedListener(object: TextWatcher{
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
            cityNameFinder.addTextChangedListener(object: TextWatcher{
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
            streetNameFinder.addTextChangedListener(object: TextWatcher{
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

            val btn_add_event = dialogView.findViewById<Button>(R.id.btn_Add_event)
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

            //close the keyboard if pressed done when in the description text
            cityNameFinder.setOnEditorActionListener { v, actionId, event ->
                if(actionId==EditorInfo.IME_ACTION_DONE){
                    val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    true
                }
                else {
                    false
                }
            }

            //close the keyboard if pressed done when in the description text
            streetNameFinder.setOnEditorActionListener { v, actionId, event ->
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
            btn_add_event.setOnClickListener {
                val containsNumbers = city.any { it.isDigit() }
                val streetOnlyNumbers = street.all { it.isDigit() }
                if(containsNumbers && !city.isEmpty()){
                    Toast.makeText(requireContext(),"City can't contain numbers",Toast.LENGTH_SHORT).show()
                }
                else if(streetOnlyNumbers && !street.isEmpty()){
                    Toast.makeText(requireContext(),"Street can't contain only numbers",Toast.LENGTH_SHORT).show()
                }

                else if(city.isEmpty() || street.isEmpty() || chosen_time.isEmpty() || datePicked.isEmpty() || description.isEmpty()){
                    Toast.makeText(requireContext(),"Please fill all the fields",Toast.LENGTH_SHORT).show()
                }
                else {
                    //this add event line of code will trigger the snapshot in our firebase which will post to the taskStatus under
                    viewModel.addEvent(datePicked,city,street,description,chosen_time)
                    alertDialog.dismiss()
                }
            }

            //dont do anything just close the dialog
            btn_cancel_event.setOnClickListener {
                alertDialog.dismiss()
            }
        }
        else if(item.itemId == R.id.item_logout){
            viewModel.logout()
            //to avoid the toast error message in the login fragment
            findNavController().navigate(R.id.action_dogsFragment_to_loginFragment)
        }
        else if(item.itemId == R.id.item_search_city_event){
            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_events_city, null)
            val editTextComment = dialogView.findViewById<EditText>(R.id.editTextComment)
            editTextComment.setOnEditorActionListener { v, actionId, event ->
                if(actionId==EditorInfo.IME_ACTION_DONE){
                    val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    true
                }
                else {
                    false
                }
            }

            val alertDialogBuilder = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Search") { dialog, which ->
                    val city = editTextComment.text.toString()
                    if (city.isNotBlank()) {
                        //avoid to open maps again if was clicked once and returned into the page
                        flagClickedShowMap = false
                        //Moving to show the events for the specific city
                        findNavController().navigate(
                            R.id.action_dogsFragment_to_eventCitySearch,
                            bundleOf("city" to city, "current_logged" to currentLoggedIn)
                        )
                    } else {
                        Toast.makeText(requireContext(), "City field is empty", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        return super.onOptionsItemSelected(item)
    }
}