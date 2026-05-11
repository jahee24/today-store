package com.today_store.frontend

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.File

class MainActivity : FlutterActivity() {
    private val channelName = "today_store/share"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "shareImageToInstagram" -> {
                        val filePath = call.argument<String>("filePath")
                        if (filePath.isNullOrBlank()) {
                            result.error("INVALID_PATH", "filePath is required", null)
                            return@setMethodCallHandler
                        }
                        result.success(shareImageToInstagram(filePath))
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun shareImageToInstagram(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists()) return false

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                setPackage("com.instagram.android")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val resolved = intent.resolveActivity(packageManager) ?: return false
            grantUriPermission(
                resolved.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
