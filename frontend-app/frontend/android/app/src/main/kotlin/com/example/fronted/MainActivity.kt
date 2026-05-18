package com.today_store.frontend

import android.content.Intent
import android.content.pm.PackageManager
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
                    "isAppInstalled" -> {
                        val app = call.argument<String>("app")?.trim().orEmpty()
                        if (app.isEmpty()) {
                            result.error("INVALID_APP", "app is required", null)
                            return@setMethodCallHandler
                        }
                        result.success(isAppInstalled(app))
                    }
                    "openApp" -> {
                        val app = call.argument<String>("app")?.trim().orEmpty()
                        if (app.isEmpty()) {
                            result.error("INVALID_APP", "app is required", null)
                            return@setMethodCallHandler
                        }
                        result.success(openApp(app))
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

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isAppInstalled(app: String): Boolean {
        return when (app) {
            "instagram" -> isPackageInstalled("com.instagram.android")
            "daangn" ->
                isPackageInstalled("com.towneers.www") ||
                    isPackageInstalled("com.towneers.hello")
            "naver" -> isPackageInstalled("com.naver.smartplace")
            else -> false
        }
    }

    private fun openApp(app: String): Boolean {
        return when (app) {
            "naver" -> {
                val intent =
                    packageManager.getLaunchIntentForPackage("com.naver.smartplace")
                        ?: return false
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            else -> false
        }
    }
}
