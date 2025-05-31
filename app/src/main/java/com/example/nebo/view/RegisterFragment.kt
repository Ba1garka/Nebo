package com.example.nebo.view

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.nebo.R
import com.example.nebo.databinding.FragmentRegisterBinding
import com.example.nebo.viewmodel.AuthViewModel
import java.time.LocalDate
import java.util.Calendar

class RegisterFragment : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private var selectedDate: LocalDate? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.birthDateEditText.setOnClickListener {
            showDatePicker()
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val name = binding.nameEditText.text.toString()

            if (email.isBlank() || password.isBlank() || name.isBlank() || selectedDate == null) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register(email, password, name, selectedDate!!)
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                result.isFailure -> {
                    Toast.makeText(context,
                        result.exceptionOrNull()?.message ?: "Registration failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("REGISTER", result.exceptionOrNull()?.message ?: "Registration failed")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                binding.birthDateEditText.setText(selectedDate!!.toString())
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }
}