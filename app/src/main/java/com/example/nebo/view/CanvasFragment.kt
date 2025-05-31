package com.example.nebo.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.nebo.databinding.FragmentCanvasBinding
import kotlinx.coroutines.launch
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.nebo.MainActivity
import com.example.nebo.R
import com.example.nebo.config.ApiService
import com.example.nebo.model.Drawing
import com.example.nebo.viewmodel.CanvasViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.*


class CanvasFragment : Fragment() {
    private var _binding: FragmentCanvasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CanvasViewModel by viewModels()

    private var currentColor = Color.BLACK
    private var currentBrushSize = 10f
    private val paths = mutableListOf<Path>()
    private val colors = mutableListOf<Int>()
    private val brushSizes = mutableListOf<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCanvasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCanvas()
        setupToolbar()
        setupObservers()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCanvas() {
        binding.drawingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val path = Path()
                    path.moveTo(event.x, event.y)
                    paths.add(path)
                    colors.add(currentColor)
                    brushSizes.add(currentBrushSize)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    paths.last().lineTo(event.x, event.y)
                    binding.drawingView.invalidate()
                    true
                }
                else -> false
            }
        }

        binding.drawingView.setCustomOnDrawListener { canvas ->
            paths.forEachIndexed { index, path ->
                val paint = Paint().apply {
                    color = colors[index]
                    strokeWidth = brushSizes[index]
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    isAntiAlias = true
                }
                canvas.drawPath(path, paint)
            }
        }

        // Кнопки инструментов
        binding.btnBlack.setOnClickListener { currentColor = Color.BLACK }
        binding.btnRed.setOnClickListener { currentColor = Color.RED }
        binding.btnBlue.setOnClickListener { currentColor = Color.BLUE }
        binding.btnEraser.setOnClickListener { currentColor = Color.WHITE }

        binding.btnSmallBrush.setOnClickListener { currentBrushSize = 5f }
        binding.btnMediumBrush.setOnClickListener { currentBrushSize = 10f }
        binding.btnLargeBrush.setOnClickListener { currentBrushSize = 20f }

        binding.btnClear.setOnClickListener {
            paths.clear()
            colors.clear()
            brushSizes.clear()
            binding.drawingView.invalidate()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.toolbar.menu.findItem(R.id.action_save).isEnabled = !isLoading
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    saveDrawing()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showMessage(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun saveDrawing() {
        binding.progressBar.visibility = View.VISIBLE
        val bitmap = createBitmapFromView(binding.drawingView)
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
                showMessage("Рисунок сохранен!")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showMessage("Ошибка: ${e.message}")
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
                    binding.progressBar.visibility = View.GONE
                    showMessage("Рисунок сохранен")
                }
                result.isFailure -> {
                    binding.progressBar.visibility = View.GONE
                    showMessage(result.exceptionOrNull()?.message ?: "Ошибка сохранения")
                    Log.e("CanvasFragment", "Upload error", result.exceptionOrNull())
                }
            }
        }
    }

}

