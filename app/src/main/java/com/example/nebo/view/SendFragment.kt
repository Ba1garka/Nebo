package com.example.nebo.view

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebo.R
import com.example.nebo.databinding.FragmentSendBinding
import com.example.nebo.model.SendDto
import com.example.nebo.viewmodel.SendViewModel


class SendFragment : Fragment() {
    private var binding: FragmentSendBinding? = null
    private val bind get() = binding!!
    private val viewModel: SendViewModel by viewModels()
    private lateinit var adapter: SendAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSendBinding.inflate(inflater, container, false)
        return bind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupRefresh()
        setupObservers()

        viewModel.loadReceivedSends()
        viewModel.loadReceivedSends()
    }

    private fun setupRecyclerView() {
        adapter = SendAdapter { send ->
            showDrawingDetails(send)
        }

        bind.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SendFragment.adapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
            )
        }
    }

    private fun setupObservers() {
        viewModel.showSendsResult.observe(viewLifecycleOwner) { result ->
            when {
                result.isSuccess -> {
                    bind.progressBar.visibility = View.GONE
                    result.getOrNull()?.let { sends ->
                        Log.d("SendFragment", "Received sends: ${sends.size}")
                        adapter.submitList(sends)
                        val latestSend = sends.firstOrNull()
                        val imageUrl2 = latestSend?.drawingPath?.substringBefore('?')
                            ?.replace("http://localhost", "http://10.0.2.2")
                        if (imageUrl2 != null) {
                            updateWidgetsWithImage(requireContext(), imageUrl2)
                            Log.e("SendFragment", "imageUrl2: " + imageUrl2)

                        }
                        bind.emptyPostsView.visibility = if (sends.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                result.isFailure -> {
                    Log.e("SendFragment", "Error loading sends", result.exceptionOrNull())
                    bind.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), result.exceptionOrNull()?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    bind.emptyPostsView.visibility = View.VISIBLE
                }
            }
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

    private fun setupRefresh() {
        bind.swipeRefresh.setOnRefreshListener {
            viewModel.loadReceivedSends()
            bind.swipeRefresh.isRefreshing = false
        }
    }

    private fun showDrawingDetails(send: SendDto) {
        // Реализуйте открытие деталей рисунка
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}