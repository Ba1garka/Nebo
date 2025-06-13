package com.example.nebo

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.nebo.databinding.ActivityMainBinding
import com.example.nebo.view.IconWidget
import com.example.nebo.view.SendService
import com.example.nebo.viewmodel.AuthViewModel
import android.Manifest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        hideBottomNavigation()
        notificationPermission()

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment)
                    showBottomNavigation()

                }
                R.id.postsFragment -> {
                    navController.navigate(R.id.postsFragment)
                    showBottomNavigation()
                }
                R.id.canvasFragment -> {
                    navController.navigate(R.id.canvasFragment)
                    hideBottomNavigation()
                }
                R.id.sendFragment -> {
                    navController.navigate(R.id.sendFragment)
                    showBottomNavigation()
                }
                R.id.exit -> performLogout()
            }
            true
        }
    }

    private fun notificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    startService()
                }
                else -> {
                    requestPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        } else {
            // для версий ниже 13
            startService()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startService()
        } else {
            Toast.makeText(this, "Уведомления отключены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startService() {
        val intent = Intent(this, SendService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun performLogout() {
        val viewModel: AuthViewModel by viewModels()
        clearWidget(this)
        viewModel.logout()
        SendService.stop(this)
        viewModel.logoutResult.observe(this) { result ->
            when {
                result.isSuccess -> {
                    clearBackStack(navController)
                    navController.navigate(R.id.loginFragment)
                    hideBottomNavigation()
                }
                result.isFailure -> {
                    Toast.makeText(
                        this,
                        result.exceptionOrNull()?.message ?: "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun clearWidget(context: Context) {
        val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().remove("widget_image_url").apply()

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, IconWidget::class.java)
        )
        IconWidget.updateWidgets(context, appWidgetManager, widgetIds)
    }

    private fun clearBackStack(navController: NavController) {

        val startDest = navController.graph.startDestinationId

        // Очищаем до исходного фрагмента
        while (navController.currentDestination?.id != startDest) {
            if (!navController.popBackStack()) {
                break
            }
        }

        // если startDestination не первый в стеке
        while (navController.popBackStack()) {
            //
        }
    }

    fun showBottomNavigation() {
        binding.bottomNavigationView.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        binding.bottomNavigationView.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}