package il.example.dogtinder.ui.specificUser

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import il.example.dogtinder.R
import il.example.dogtinder.databinding.FragmentSpecificUserBinding
import il.example.dogtinder.repository.Firebase.ProfileRepFireBase
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared

class SpecificUserFragment : Fragment() {

    private var binding : FragmentSpecificUserBinding by autoCleared()
    private val viewModel : SpecificUserViewModel by viewModels() {
        SpecificUserViewModel.ProfileViewModelFactory(ProfileRepFireBase())
    }

    var currentLoggedIn=""

    var userClicked = ""

    var flagDeleteClicked = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSpecificUserBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("current_user").let {
            if(it!=null){
                currentLoggedIn = it
            }
        }

        //the id of the user we clicked on
        arguments?.getString("userId").let {
            userClicked = it!!
            if(viewModel.rotationScreenHandle==0){
                viewModel.getUser(it!!)
                //incrementing the value to 1 so when rotating the screen it won't load again the user for no reason
                viewModel.setRotationValue()
                //Will return true if current user that's logged in clicked on his own profile picture if so then he won't have the button to add comments to himself
                viewModel.currentUser(it!!)
            }
        }

        binding.specificUserCommentsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.specificUserCommentsRv.adapter = SpecificUserAdapter(currentLoggedIn, object :SpecificUserAdapter.deleteComment{
            //Handle the click on the delete button for that comment
            override fun deleteComment(comment: String, userWrote: String, dateComment: String) {
                flagDeleteClicked = true
                viewModel.deleteComment(comment,userClicked,currentLoggedIn,dateComment)
            }
        })

        //checking if the current user that's logged in clicked on his own profile picture when he searched for people if so then dont show the add comments button
        viewModel.currentLoggedIn.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                }

                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    //won't be able to write comments for himself
                    if(it.data==true){
                        binding.userComment.isVisible = false
                    }
                    //the user that's logged in has clicked on different user profile so he will be able to write comments
                    else {

                    }
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                }
            }
        }

        //when clicking on the add comments button then we want to add the comments to this specific user profile
        binding.userComment.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_comment, null)
            val editTextComment = dialogView.findViewById<EditText>(R.id.editTextComment)

            val alertDialogBuilder = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Send") { dialog, which ->
                    val comment = editTextComment.text.toString()
                    if (comment.isNotBlank()) {
                        //updating the comment on the user's profile as soon as the user clicked on send
                        viewModel.setComment(comment,userClicked,requireContext())
                    } else {
                        Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }

            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }



        //for any change in the specific user such as comments or rating the user watching this user will be updated immediately
        viewModel.currentUser.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                }

                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    binding.profileDogName.text = it.data!!.dogsName
                    binding.profileName.text = it.data!!.name
                    binding.profilePhone.text = it.data!!.phone
                    Glide.with(requireContext()).load(it.data!!.image).into(binding.profilePicture)
                    //setting the adapter's list to be the one we got from here -> when someone else added a new comment or any change appeared then the adapter's list
                    //will be updated accordingly
                    (binding.specificUserCommentsRv.adapter as SpecificUserAdapter).setList(it.data!!.comments)

                }

                is Resource.Error -> {
                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                    binding.ProgressBar.isVisible = false
                }
            }
        }


        //observing the delete status of the comment
        //the flag meant to avoid when screen rotating and comment was deleted before to not show the message again even if we didnt delete anything when screen rotated
            viewModel.deleteCommentStatus.observe(viewLifecycleOwner){
                if(flagDeleteClicked){
                    when(it){
                        is Resource.Success -> {
                            Toast.makeText(requireContext(),it.data.toString(),Toast.LENGTH_SHORT).show()
                        }
                        is Resource.Error -> {
                            Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}