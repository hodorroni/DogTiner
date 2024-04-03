package il.example.dogtinder.ui.login

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import il.example.dogtinder.R
import il.example.dogtinder.databinding.FragmentLoginBinding
import il.example.dogtinder.repository.Firebase.AuthRepFirebase
import il.example.dogtinder.utils.Resource
import il.example.dogtinder.utils.autoCleared

class LoginFragment:Fragment() {

    private var binding : FragmentLoginBinding by autoCleared()

    private val viewModel : LoginViewModel by viewModels() {
        LoginViewModel.LoginViewModelFactory(AuthRepFirebase(), requireActivity().application)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.loginBtn.setOnClickListener {
            val email_text = binding.loginEmail.editText?.text.toString()
            val password_text = binding.passwordLogin.editText?.text.toString()
            viewModel.login(email_text,password_text)
        }

        //user pressed on the login button
        viewModel.loginStatus.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading ->{
                    binding.ProgressBar.isVisible = true
                    binding.loginBtn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    Toast.makeText(requireContext(),"Logged In",Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_dogsFragment, bundleOf("current_login" to it.data!!.user_id))
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    binding.loginBtn.isEnabled = true
                    Toast.makeText(requireContext(),it.message.toString(),Toast.LENGTH_SHORT).show()
                }
            }
        }

        //current user logged in automatically or with credentials
        viewModel.currentUser.observe(viewLifecycleOwner){
            when(it){
                is Resource.Loading ->{
                    binding.ProgressBar.isVisible = true
                    binding.loginBtn.isEnabled = false
                }
                is Resource.Success -> {
                    binding.ProgressBar.isVisible = false
                    findNavController().navigate(R.id.action_loginFragment_to_dogsFragment, bundleOf("current_login" to it.data!!.user_id))
                }
                is Resource.Error -> {
                    binding.ProgressBar.isVisible = false
                    binding.loginBtn.isEnabled = true
                    if(!it.message.equals("")){
                        Toast.makeText(requireContext(),it.message.toString(),Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.registerBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
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
        binding.passwordLoginDone.setOnEditorActionListener { v, actionId, event ->
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