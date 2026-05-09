package aqmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import aqmusic.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requiredPermissions by lazy {
        mutableListOf<String>().apply {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.map { it.key }
        if (denied.isEmpty()) {
            ensureOverlayPermission()
        } else {
            showPermissionDialog(denied)
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            continueStartup()
        } else {
            showOverlayDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupWebView()
        binding.retryButton.setOnClickListener {
            requestPermissions()
        }
        requestPermissions()
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.loadingProgress.progress = newProgress
                if (newProgress >= 100) {
                    binding.loadingContainer.alpha = 0f
                    binding.loadingContainer.visibility = android.view.View.GONE
                }
            }
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                binding.loadingContainer.visibility = android.view.View.VISIBLE
                binding.loadingContainer.alpha = 1f
                binding.loadingTitle.text = getString(R.string.loading_page)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                binding.loadingContainer.visibility = android.view.View.GONE
            }
        }
    }

    private fun requestPermissions() {
        binding.statusMessage.text = getString(R.string.checking_permissions)
        if (hasAllPermissions()) {
            ensureOverlayPermission()
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun ensureOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            continueStartup()
        } else {
            showOverlayDialog()
        }
    }

    private fun continueStartup() {
        binding.statusMessage.text = getString(R.string.loading_app)
        binding.loadingContainer.visibility = android.view.View.VISIBLE
        binding.webView.loadUrl("https://app.aqmusic.qzz.io")
    }

    private fun showPermissionDialog(deniedPermissions: List<String>) {
        val message = buildString {
            append(getString(R.string.permission_request_message))
            append("\n\n")
            deniedPermissions.forEach { permission ->
                append(getString(permissionNameResource(permission)))
                append("\n")
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required)
            .setMessage(message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.close_app) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.overlay_permission_title)
            .setMessage(R.string.overlay_permission_message)
            .setPositiveButton(R.string.enable_now) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton(R.string.close_app) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun permissionNameResource(permission: String): Int {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> R.string.audio_permission
            Manifest.permission.POST_NOTIFICATIONS -> R.string.notification_permission
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.files_permission
            else -> R.string.permission_required
        }
    }
}
