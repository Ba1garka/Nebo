package com.example.nebo.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide

import com.example.nebo.R
import com.example.nebo.databinding.FragmentProfileBinding
import com.example.nebo.model.DrawingResponse
import com.example.nebo.viewmodel.AvatarViewModel
import com.example.nebo.viewmodel.DrawingsViewModel
import com.example.nebo.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding
    private val viewModel: UserViewModel by viewModels()
    private val viewModelDrawing: DrawingsViewModel by viewModels()
    private lateinit var adapter: DrawingsAdapter
    private val avatarViewModel: AvatarViewModel by viewModels()

    private var currentPhotoUri: Uri? = null

    // Лаунчер для камеры
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            showError("Camera permission is required to take photos")
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                uploadPhotoFromUri(uri)
            }
        }
    }

    // Лаунчер для галереи
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { uploadPhotoFromUri(it) }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadUserData()

        viewModel.userData.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val user = result.getOrNull()!!
                    with(binding) {
                        nameTextView.text = user.name
                        emailTextView.text = user.email
                        birthDateTextView.text = user.birthDate
                        drawingsCountTextView.text = "Drawings: ${user.drawingsCount}"

                        if (!user.avatarUrl.isNullOrEmpty()) {
                            val avatar = user.avatarUrl.substringBefore('?').replace("http://localhost", "http://10.0.2.2")
                            Glide.with(this@ProfileFragment)
                                .load(avatar)
                                .circleCrop()
                                .into(avatarImageView)
                        }
                    }
                }
                result.isFailure -> {
                    Toast.makeText(
                        context,
                        result.exceptionOrNull()?.message ?: "Failed to load profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        avatarViewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val avatarUrl = result.getOrNull()!!
                    // Обновляем аватарку
                    val avatar = avatarUrl.substringBefore('?')?.replace("http://localhost", "http://10.0.2.2")

                    Glide.with(this)
                        .load(avatar)
                        .circleCrop()
                        .into(binding.avatarImageView)

                    // Обновляем данные пользователя
                    viewModel.loadUserData()
                }
                result.isFailure -> {
                    showError(result.exceptionOrNull()?.message ?: "Failed to upload avatar")
                }
            }
        }

        setupRecyclerView()
        setupObservers()
        viewModelDrawing.loadUserDrawings()

        binding.avatarImageView.setOnClickListener{
            showImagePickerDialog()
        }
    }

    private fun setupObservers() {
        viewModelDrawing.drawingsResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val drawings = result.getOrNull()!!
                    if (drawings.isEmpty()) {
                        //showEmptyState()
                        Toast.makeText(requireContext(),"У вас пока нет рисунков",Toast.LENGTH_LONG).show()
                    } else {
                        updateDrawingsList(drawings)
                    }
                }
                result.isFailure -> {
                    showError(result.exceptionOrNull()?.message ?: "Failed to load drawings")
                }
            }
        }

        viewModelDrawing.isLoading.observe(viewLifecycleOwner) { isLoading ->
            //binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        adapter = DrawingsAdapter()
        binding.drawingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = this@ProfileFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
            )
        }
    }

    private fun updateDrawingsList(drawings: List<DrawingResponse>) {
        // Обновление RecyclerView или другого UI
        adapter.submitList(drawings)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("DrawingsFragment", message)
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showRationaleDialog(
                    "Camera permission is needed to take photos",
                    Manifest.permission.CAMERA
                )
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showRationaleDialog(message: String, permission: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                cameraPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> launchGalleryPicker()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchGalleryPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = createImageFile()
        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )

        try {
            takePictureLauncher.launch(currentPhotoUri)
        } catch (e: ActivityNotFoundException) {
            showError("No camera app available")
        } catch (e: SecurityException) {
            showError("Camera permission was revoked")
            // Запросить разрешение снова
            checkCameraPermissionAndLaunch()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().externalCacheDir ?: requireContext().cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                uploadPhotoFromUri(uri)
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_GALLERY = 1001
    }

    private fun uploadPhotoFromUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = uri.toFile(requireContext())
                avatarViewModel.uploadAvatar(file)
            } catch (e: Exception) {
                showError("Ошибка при обработке изображения: ${e.message}")
            }
        }
    }

}

// Расширение для преобразования Uri в File
fun Uri.toFile(context: Context): File {
    val file = File.createTempFile(
        "temp_img_",
        ".jpg",
        context.cacheDir
    )

    context.contentResolver.openInputStream(this)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return file
}