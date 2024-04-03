package il.example.dogtinder.ui.attenders

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import il.example.dogtinder.R
import il.example.dogtinder.databinding.AttendersLayoutBinding
import il.example.dogtinder.databinding.FragmentAttendersBinding
import il.example.dogtinder.repository.Firebase.AuthRepFirebase
import il.example.dogtinder.repository.Firebase.EventRepFirebase
import il.example.dogtinder.ui.dog_feed.DogsViewModel
import il.example.dogtinder.utils.autoCleared

class Attenders:Fragment() {

    private var binding: FragmentAttendersBinding by autoCleared()
    var currentUser=""
    var eventId=""

    private val viewModel : AttendersViewModel by viewModels() {
        AttendersViewModel.AttendersViewModelFactory(EventRepFirebase())
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAttendersBinding.inflate(inflater,container,false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listAttenders = arguments?.get("list_attenders") as List<String>
        listAttenders.let {
            if(it.isEmpty()){
                binding.attendersTitle.text = "No people attend to this event"
            }
            binding.attendersRv.layoutManager = LinearLayoutManager(requireContext())
            binding.attendersRv.adapter=AttendersAdapter(it)
        }


        //the id of whom created the event
        val userCreated = arguments?.getString("user_created")
        eventId = arguments?.getString("event_id").toString()



        //enable the delete button if the current logged in user is the one that entered into that event
        viewModel.currentUser.observe(viewLifecycleOwner){
            currentUser=it
            //if the current used that logged in is the one that created the event then show the delete button for that user since he's the only one that can delete it
            if(currentUser.equals(userCreated)){
                binding.deleteEventBtn.isVisible = true
            }
        }

        //delete the event
        binding.deleteEventBtn.setOnClickListener {
            if (userCreated != null) {
                showDeleteConfirmationDialog(requireContext()){
                    viewModel.deleteEvent(userCreated,eventId)
                    Toast.makeText(requireContext(),"Successfully deleted the event",Toast.LENGTH_SHORT).show()
                }
            }
        }

        //when finish deleting the event then move to the previous page.
        viewModel.deletionStatus.observe(viewLifecycleOwner){
            if(it){
                findNavController().navigate(R.id.action_attenders_to_dogsFragment)
            }
        }

        //go back to the previous page
//        binding.attendBackBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_attenders_to_dogsFragment)
//        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.attenders_menu,menu)
        val back_icon =menu.findItem(R.id.back_attenders_btn)
        val colorControlNormal = ContextCompat.getColor(requireContext(), R.color.white)
        back_icon.icon?.let { DrawableCompat.setTint(it, colorControlNormal) }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.back_attenders_btn){
            findNavController().navigate(R.id.action_attenders_to_dogsFragment, bundleOf("current_user" to currentUser))
        }
        return super.onOptionsItemSelected(item)
    }

    fun showDeleteConfirmationDialog(context: Context, onDeleteClicked: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.apply {
            setTitle("Delete Confirmation")
            setMessage("Are you sure you want to delete?")
            setPositiveButton("Delete") { dialog, which ->
                // Call the onDeleteClicked lambda when the "Delete" button is clicked
                onDeleteClicked()
            }
            setNegativeButton("Back") { dialog, which ->
                // Dismiss the dialog if "Back" button is clicked
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }


}