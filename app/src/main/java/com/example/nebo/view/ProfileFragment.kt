package com.example.nebo.view

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nebo.R
import com.example.nebo.databinding.FragmentProfileBinding
import com.example.nebo.viewmodel.AuthViewModel
import com.example.nebo.viewmodel.AvatarViewModel
import com.example.nebo.viewmodel.DrawingsViewModel
import com.example.nebo.viewmodel.SendViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class ProfileFragment : Fragment() {
    private var binding: FragmentProfileBinding? = null
    private val bind get() = binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val viewModelSend: SendViewModel by viewModels()
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
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                requireContext().sendBroadcast(intent)
                uploadPhotoFromUri(uri)
            }
        }
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

        context?.let { viewModelSend.loadReceivedSends(it) }

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
                    showE(result.exceptionOrNull()?.message ?: "Failed to load drawings")
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

                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val widgetIds = appWidgetManager.getAppWidgetIds(
                            context?.let { ComponentName(it, IconWidget::class.java) }
                        )
                        context?.let { IconWidget().onUpdate(it, appWidgetManager, widgetIds) }
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
                    showE(result.exceptionOrNull()?.message ?: "Failed to upload avatar")
                }
            }
        }

        viewModelSend.sendResult.observe(viewLifecycleOwner){ result ->
            when {
                result.isSuccess -> {
                    Toast.makeText(context, "Рисунок отправлен!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModelSend.widgetResult.observe(viewLifecycleOwner) { imageUrl ->
            imageUrl?.let { url ->
                updateWidgetsWithImage(requireContext(), url)
                Log.d("ProfileFragment", "Обновлён виджет : $url")
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DrawingsAdapter ({ imageUrl -> updateWidgetsWithImage(requireContext(), imageUrl) },
            { drawingId -> viewModelDrawing.delete(drawingId) },
            { drawingId -> showDialog(drawingId) }
        )
        bind.drawingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = this@ProfileFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL)
            )
        }
    }

    private fun showDialog(drawingId: Long) {
        val dialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Отправить рисунок")
            setMessage("Введите имя получателя")

            val input = EditText(requireContext()).apply {
                hint = "Имя пользователя"
                inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            }

            setView(input)
            setPositiveButton("Отправить") { _, _ ->
                val recipientName = input.text.toString()
                if (recipientName.isNotBlank()) {
                    viewModelSend.sendDrawing(drawingId, recipientName)
                } else {
                    Toast.makeText(context, "Введите имя получателя", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("Отмена", null)
        }.create()

        dialog.show()
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

    private fun showE(message: String) {
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
        startActivityForResult(intent, 10)
    }

    private fun dispatchTakePictureIntent() {
        lifecycleScope.launch(Dispatchers.IO) {
            val photoFile = createImageFile()

            try {
                takePictureLauncher.launch(photoFile)
            } catch (e: ActivityNotFoundException) {
                showE("No camera app available")
            } catch (e: SecurityException) {
                showE("Camera permission was revoked")
                cameraPermission()
            }
        }
    }

    private fun createImageFile(): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
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
                showE("Ошибка при обработке изображения: ${e.message}")
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