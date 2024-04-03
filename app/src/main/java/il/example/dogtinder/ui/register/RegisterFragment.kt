package il.example.dogtinder.ui.register

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import il.example.dogtinder.R
import il.example.dogtinder.databinding.FragmentRegisterBinding
import il.example.dogtinder.repository.AuthRepository
import il.example.dogtinder.repository.Firebase.AuthRepFirebase
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared

class RegisterFragment:Fragment() {

    private var binding : FragmentRegisterBinding by autoCleared()

    private val viewModel : RegisterViewModel by viewModels(){
        RegisterViewModel.RegisterViewModelFactory(AuthRepFirebase())
    }
    private var imageTaken:Uri? = null

    val imageLauncher: ActivityResultLauncher<String>
    =registerForActivityResult(ActivityResultContracts.GetContent()){
        //it?.let { viewModel.setPicture(it) }
        it?.let {
            viewModel.setUri(it)
            val bitmap:Bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,it)
            Glide.with(this).asBitmap().load(it).transform(CircleCrop()).into(binding.imagePerson)
            imageTaken= it
            //binding.imagePicker.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerBtn.setOnClickListener {
            val email = binding.emailRegister.editText?.text.toString()
            var name = binding.nameRegister.editText?.text.toString()
            val nameContainsDigit = name.any { it.isDigit() }
            if(nameContainsDigit){
                Toast.makeText(requireContext(),"Your name can't contain numbers",Toast.LENGTH_SHORT).show()
                name=""
            }
            val dogName = binding.dogNameRegister.editText?.text.toString()
            val dogNameDigits = dogName.any{it.isDigit()}
            if(dogNameDigits){
                Toast.makeText(requireContext(),"Your dog's name can't contain numbers",Toast.LENGTH_SHORT).show()
            }
            val password = binding.passwordRegister.editText?.text.toString()
            val phoneNumber = binding.phoneRegister.editText?.text.toString()
            if(imageTaken==null){
                imageTaken = Uri.parse("android.resource://il.example.dogtinder/" + R.drawable.blank_user_photo)
            }
            imageTaken?.let {
                viewModel.createUser(name,dogName,phoneNumber,email,password,imageTaken.toString())
            }

        }

        viewModel.registerStatus.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading -> {
                    binding.ProgressBar.isVisible = true
                    binding.registerBtn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    findNavController().navigate(R.id.action_registerFragment_to_dogsFragment,  bundleOf("current_register" to it.data!!.user_id))
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    binding.registerBtn.isEnabled = true
                    Toast.makeText(requireContext(),it.message,Toast.LENGTH_SHORT).show()
                }
            }
        }

        //setting the image that was taken by the user
//        viewModel.photoUri.observe(viewLifecycleOwner){
//            Glide.with(this).load(it).transform(CircleCrop()).into(binding.imagePerson)
//        }

        binding.imagePicker.setOnClickListener {
            imageLauncher.launch("image/*")
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

        //hide the keyboard when pressing the done in the last edittext
        binding.passwordReg.setOnEditorActionListener { v, actionId, event ->
            if(actionId==EditorInfo.IME_ACTION_DONE){
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