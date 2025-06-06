package com.example.nebo.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nebo.databinding.FragmentProfileBinding
import com.example.nebo.viewmodel.AuthViewModel
import com.example.nebo.viewmodel.AvatarViewModel
import com.example.nebo.viewmodel.DrawingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null
    private val bind get() = binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val viewModelDrawing: DrawingsViewModel by viewModels()
    private lateinit var adapter: DrawingsAdapter
    private val avatarViewModel: AvatarViewModel by viewModels()
    private var currentPhotoUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { success ->
        if (success) {
            dispatchTakePictureIntent()
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

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadPhotoFromUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadUserData()
        setupRecyclerView()
        setupObservers()
        viewModelDrawing.loadUserDrawings()

        bind.updatePhotoButton.setOnClickListener{
            showImagePickerDialog()
        }

    }

    private fun setupObservers() {
        viewModelDrawing.drawingsResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val drawings = result.getOrNull()!!
                    if (drawings.isEmpty()) {
                        Toast.makeText(requireContext(),"У вас пока нет рисунков",Toast.LENGTH_LONG).show()
                    } else {
                        adapter.submitList(drawings)
                    }
                }
                result.isFailure -> {
                    showError(result.exceptionOrNull()?.message ?: "Failed to load drawings")
                }
            }
        }

        viewModel.userDataResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    val user = result.getOrNull()!!
                    with(bind) {
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
                    val avatar = avatarUrl.substringBefore('?').replace("http://localhost", "http://10.0.2.2")

                    Glide.with(this)
                        .load(avatar)
                        .circleCrop()
                        .into(bind.avatarImageView)

                    viewModel.loadUserData()
                }
                result.isFailure -> {
                    showError(result.exceptionOrNull()?.message ?: "Failed to upload avatar")
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DrawingsAdapter { imageUrl ->
            updateWidgetsWithImage(requireContext(), imageUrl)
        }
        bind.drawingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = this@ProfileFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
            )
        }
    }

    private fun updateWidgetsWithImage(context: Context, imageUrl: String) {
        context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE).edit()
            .putString("widget_image_url", imageUrl)
            .apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, IconWidget::class.java)
        )
        IconWidget.updateWidgets(context, appWidgetManager, widgetIds)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("DrawingsFragment", message)
    }

    private fun cameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showRationaleDialog()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage("Camera permission is needed to take photos")
            .setPositiveButton("OK") { _, _ ->
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                    0 -> cameraPermission()
                    1 -> launchGalleryPicker()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchGalleryPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { //запрашиваем выбор фото
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, 1001)
    }
//    private fun launchGalleryPicker() {
//        galleryLauncher.launch("image/*")
//    }

    private fun dispatchTakePictureIntent() {
        lifecycleScope.launch(Dispatchers.IO) {
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
                cameraPermission()
            }
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
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                uploadPhotoFromUri(uri)
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}

fun Uri.toFile(context: Context): File {
    val file = File.createTempFile(
        "temp_img_",
        ".jpg",
        context.cacheDir
    )

    context.contentResolver.openInputStream(this)?.use { input -> //перенос данных из юри в файл
        file.outputStream().use { output -> //use гарантируют, что оба потока будут закрыты автоматически
            input.copyTo(output)
        }
    }

    return file
}