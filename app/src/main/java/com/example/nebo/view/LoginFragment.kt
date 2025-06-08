package com.example.nebo.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.nebo.MainActivity
import com.example.nebo.R
import com.example.nebo.databinding.FragmentLoginBinding
import com.example.nebo.viewmodel.AuthViewModel

class LoginFragment : Fragment() {
    private var binding: FragmentLoginBinding? = null
    private val bind get()= binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind.loginButton.setOnClickListener {
            val email = bind.emailEditText.text.toString()
            val password = bind.passwordEditText.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        bind.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    (activity as? MainActivity)?.showBottomNavigation()
                    findNavController().navigate(R.id.action_loginFragment_to_profileFragment)
                }
                result.isFailure -> {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Login failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}