package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.LedgerDarkBg
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Pre-create WebView cache directories to prevent Chromium simple_file_enumerator opendir ENOENT errors
            val codeCacheDir = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            File(codeCacheDir, "js").mkdirs()
            File(codeCacheDir, "wasm").mkdirs()
        } catch (e: Exception) {
            // Non-fatal
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                DedunApp()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DedunApp() {
    val context = LocalContext.current
    val activity = context as? Activity

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            when {
                intentData?.clipData != null -> {
                    val count = intentData.clipData!!.itemCount
                    Array(count) { i -> intentData.clipData!!.getItemAt(i).uri }
                }
                intentData?.data != null -> {
                    arrayOf(intentData.data!!)
                }
                else -> null
            }
        } else {
            null
        }
        fileUploadCallback?.onReceiveValue(uris)
        fileUploadCallback = null
    }

    BackHandler(enabled = canGoBack) {
        webViewInstance?.let {
            if (it.canGoBack()) {
                it.goBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("dedun_main_container")
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dedun_webview"),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    setBackgroundColor(0xFF151411.toInt())
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }

                    if (activity != null) {
                        addJavascriptInterface(AndroidBridge(activity), "AndroidBridge")
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            fileUploadCallback?.onReceiveValue(null)
                            fileUploadCallback = filePathCallback

                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "application/json"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            return try {
                                fileChooserLauncher.launch(intent)
                                true
                            } catch (e: Exception) {
                                fileUploadCallback?.onReceiveValue(null)
                                fileUploadCallback = null
                                false
                            }
                        }

                        override fun onJsAlert(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            AlertDialog.Builder(ctx)
                                .setTitle("DEDUN")
                                .setMessage(message ?: "")
                                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                                .setOnCancelListener { result?.cancel() }
                                .show()
                            return true
                        }

                        override fun onJsConfirm(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            AlertDialog.Builder(ctx)
                                .setTitle("DEDUN")
                                .setMessage(message ?: "")
                                .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                                .setOnCancelListener { result?.cancel() }
                                .show()
                            return true
                        }

                        override fun onJsPrompt(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            defaultValue: String?,
                            result: JsPromptResult?
                        ): Boolean {
                            val input = EditText(ctx).apply {
                                setText(defaultValue ?: "")
                            }
                            AlertDialog.Builder(ctx)
                                .setTitle("DEDUN")
                                .setMessage(message ?: "")
                                .setView(input)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    result?.confirm(input.text.toString())
                                }
                                .setNegativeButton(android.R.string.cancel) { _, _ ->
                                    result?.cancel()
                                }
                                .setOnCancelListener { result?.cancel() }
                                .show()
                            return true
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("file://") || url.startsWith("data:")) {
                                return false
                            }
                            return try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }

                    loadUrl("file:///android_asset/index.html")
                    webViewInstance = this
                }
            },
            update = {
                webViewInstance = it
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerDarkBg),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "DEDUN: $name",
            color = com.example.ui.theme.GoldPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium
        )
    }
}
