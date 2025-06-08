package com.example.nebo.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.nebo.databinding.FragmentCanvasBinding
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.nebo.MainActivity
import com.example.nebo.R
import com.example.nebo.viewmodel.CanvasViewModel
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class CanvasFragment : Fragment() {
    private var binding: FragmentCanvasBinding? = null
    private val bind get() = binding!! //становится не null после onCreateView
    private val viewModel: CanvasViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCanvasBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCanvas()
        setupToolbar()
        setupBrushControls()
        setupObservers()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCanvas() {
        //по умолчанию
        bind.drawingView.setBrushColor(Color.BLACK)
        bind.drawingView.setBrushSize(10f)
    }

    fun showColorPicker() {
        ColorPickerDialog.newBuilder()
            .setColor(Color.RED)  // Начальный цвет
            .setDialogType(ColorPickerDialog.TYPE_PRESETS)
            .setShowAlphaSlider(true)
            .create()
            .apply {
                setColorPickerDialogListener(object : ColorPickerDialogListener {
                    override fun onColorSelected(dialogId: Int, color: Int) {
                        bind.fabColorPicker.apply {
                            backgroundTintList = ColorStateList.valueOf(color)
                            imageTintList = null // Чтобы иконка сохраняла оригинальные цвета
                            setImageResource(R.drawable.ic_rainbow)
                        }
                        val alpha = Color.alpha(color)
                        bind.drawingView.setBrushColor(color)
                        bind.drawingView.setBrushAlpha(alpha)
                    }
                    override fun onDialogDismissed(dialogId: Int) {}
                })
            }
            .show(parentFragmentManager, "colorpicker")
    }


    private fun setupBrushControls() {

        //размер
        bind.brushSizeSeekBar.addOnChangeListener { slider, value, fromUser ->
            bind.drawingView.setBrushSize(value)
            bind.brushSizeText.text = "Size: ${value.toInt()}px"
        }

        //кончик
        bind.capStyleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnRoundCap -> bind.drawingView.setBrushStrokeCap(Paint.Cap.ROUND)
                    R.id.btnSquareCap -> bind.drawingView.setBrushStrokeCap(Paint.Cap.SQUARE)
                    R.id.btnButtCap -> bind.drawingView.setBrushStrokeCap(Paint.Cap.BUTT)
                }
            }
        }

        //текстура
        bind.textureGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val texture = when (checkedId) {
                    R.id.btnTexture1 -> createPatternBitmap(R.drawable.texture_1)
                    R.id.btnTexture2 -> createPatternBitmap(R.drawable.texture_2)
                    else -> null
                }
                bind.drawingView.setBrushTexture(texture)
            }
        }

        bind.fabColorPicker.setOnClickListener {
            showColorPicker()
        }

        bind.btnCloseParams.setOnClickListener {
            bind.brushParamsCard.visibility = View.GONE
        }

        bind.bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_brush -> {
                    toggleVisibility(bind.brushParamsCard)
                    true
                }
                R.id.action_eraser -> {
                    bind.drawingView.setBrushColor(Color.WHITE)
                    bind.drawingView.setBrushAlpha(255)
                    true
                }
                R.id.action_undo -> {
                    bind.drawingView.undo()
                    true
                }
                R.id.action_redo -> {
                    bind.drawingView.redo()
                    true
                }
                R.id.action_clear -> {
                    showClearConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun createPatternBitmap(drawableRes: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Canvas")
            .setMessage("Are you sure you want to clear the canvas?")
            .setPositiveButton("Clear") { _, _ ->
                bind.drawingView.clearCanvas()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupToolbar() {
        bind.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        bind.toolbar.setNavigationOnClickListener {
            (activity as? MainActivity)?.showBottomNavigation()
            requireActivity().onBackPressed()
        }
        bind.toolbar.apply {
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> {
                        saveDrawing()
                        true
                    }
                    R.id.download -> {
                        saveInGallery()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun saveInGallery() {
        bind.progressBar.visibility = View.VISIBLE
        val bitmap = createBitmapFromView(bind.drawingView)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "drawing_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
            }
            //  уведомляем
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            requireContext().sendBroadcast(intent)
            bind.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
        }
    }

    private fun saveDrawing() {
        bind.progressBar.visibility = View.VISIBLE
        val bitmap = createBitmapFromView(bind.drawingView)
        val file = saveBitmapToFile(bitmap)

        lifecycleScope.launch {
            try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val body = MultipartBody.Part.createFormData(
                    "file",
                    "drawing_${System.currentTimeMillis()}.jpg",
                    requestFile
                )

                viewModel.uploadDrawing(body)
                showT("Рисунок сохранен!")
            } catch (e: Exception) {
                bind.progressBar.visibility = View.GONE
                showT("Ошибка: ${e.message}")
            }
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val filesDir = requireContext().filesDir
        val file = File(filesDir, "temp_drawing.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    private fun setupObservers() {
        viewModel.uploadResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    bind.progressBar.visibility = View.GONE
                    showT("Рисунок сохранен")
                }
                result.isFailure -> {
                    bind.progressBar.visibility = View.GONE
                    showT(result.exceptionOrNull()?.message ?: "Ошибка сохранения")
                    Log.e("CanvasFragment", "Upload error", result.exceptionOrNull())
                }
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            bind.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            bind.toolbar.menu.findItem(R.id.action_save).isEnabled = !loading
        }
    }

     private fun showT(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
