package il.example.dogtinder.ui.profile


import android.content.Context
import il.example.dogtinder.R
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import il.example.dogtinder.databinding.FragmentProfileBinding
import il.example.dogtinder.model.User
import il.example.dogtinder.repository.Firebase.ProfileRepFireBase
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared

class UsersSearchFragment : Fragment() {

    private var binding : FragmentProfileBinding by autoCleared()
    var userEntered =""
    var flag = "dog"
    var currentLoggedIn =""

    private val viewModel : ProfileViewModel by viewModels() {
        ProfileViewModel.ProfileViewModelFactory(ProfileRepFireBase())
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textSearch.imeOptions = EditorInfo.IME_ACTION_DONE

        arguments?.getString("current_user").let {
            if(it!=null){
                currentLoggedIn = it
                viewModel.getCurrentName(currentLoggedIn)
            }
        }

        //spinner section
        val spinnerValues = listOf("Dog Name", "Owner Name")

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerValues).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val spinner: Spinner = binding.spinner
        spinner.adapter = adapter

        val defaultSelectionPosition = spinnerValues.indexOf("Dog Name")
        spinner.setSelection(defaultSelectionPosition)


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Close the keyboard
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(spinner.windowToken, 0)

                val selectedItem = spinnerValues[position]
                if (selectedItem == "Owner Name") {
                    flag = "owner"
                }
                else {
                    flag="dog"
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        // End of spinner section



        binding.profilesRv.layoutManager = LinearLayoutManager(requireContext())

        binding.profilesRv.adapter = ProfilesAdapter(requireContext(),object : ProfilesAdapter.specificUser{
            override fun onUserClicked(user: User) {
                findNavController().navigate(R.id.action_usersSearchFragment_to_specificUserFragment, bundleOf("userId" to user.user_id,
                    "current_user" to currentLoggedIn))
            }
        })


        //Listening to anything that the user has entered
        binding.textSearch.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userEntered = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })


        //when clicking on the search button
        binding.searchBtn.setOnClickListener {
            // Close the keyboard
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.searchBtn.windowToken, 0)
            if(userEntered.isEmpty()){
                Toast.makeText(requireContext(),"Enter something",Toast.LENGTH_SHORT).show()
            }
            else {
                //getting the users and the allUsersFound below will get triggered
                viewModel.getAllUsers(userEntered,userEntered,flag)
            }
        }



        //that observe will get called a lot since of the snapshotlistener which listens to any changes in the firestore
        //for instance if someone searched for dog's name "sky" and someone just joined with that dog name then our list will be updated on live without the need to refresh
        viewModel.allUsersFound.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                }
                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    //setting the list inside our adapter as soon as any change was triggered in the firebase
                    (binding.profilesRv.adapter as ProfilesAdapter).setList(it.data!!)

                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                }
            }
        }


        //getting the name of the current used logged in, we have in the beginning only the id of that user
        viewModel.currentNameLogged.observe(viewLifecycleOwner){
            when(it){
                is Resource.Success -> {
                    currentLoggedIn = it.data!!
                }
            }
        }


        //closing the keyboard when touching anywhere on the screen
        binding.homeParent.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }

        // Set onTouchListener for the RecyclerView to close the keyboard
        binding.profilesRv.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
                return@setOnTouchListener true
            }
            return@setOnTouchListener false
        }




        //hide the keyboard when pressing the done in the last edittext
        binding.textSearch.setOnEditorActionListener { v, actionId, event ->
            if(actionId== EditorInfo.IME_ACTION_DONE){
                //hide the keyboard
                val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                true
            }
            else {
                false
            }
        }



    }
}