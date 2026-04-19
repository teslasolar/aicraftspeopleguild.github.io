package com.aicraftspeopleguild.acg.papers

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** Konomi · reference API browser.
 *
 *  The Konomi Standard is self-defining — the paper IS its own spec.
 *  The live bundle at guild/Enterprise/L4/api/konomi.json is served
 *  from the teslasolar SCADA Pages build and re-fetched on every view,
 *  so updates to any of the 8 industrial standards (ISA-95/88/18/101,
 *  OPC-UA, Sparkplug, Modbus, KPI) land in both the Pages browser and
 *  this phone tab without shipping a new APK.
 *
 *  We host the Pages browser in a WebView instead of re-rendering the
 *  bundle natively so Kotlin and JS stay in lockstep on one source.
 */
object MiniRegistry {
    fun isAvailable() = true

    private const val API_URL =
        "https://teslasolar.github.io/aicraftspeopleguild.github.io/" +
        "guild/Enterprise/L4/api/konomi/index.html"

    @Composable
    fun Render(modifier: Modifier, primary: Color) {
        var loading by remember { mutableStateOf(true) }
        var failed  by remember { mutableStateOf(false) }

        Column(modifier.fillMaxSize()) {
            Text(
                "◈ KONOMI · REFERENCE",
                color = primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
            )
            Text(
                "Live bundle · 8 base UDTs · 8 standards · 66 std UDTs · 5 crosswalks. " +
                "Loaded from the teslasolar Pages API — the same JSON the web mini fetches.",
                color = Color(0xFF8B95A0),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            )

            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFAF6F0))) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?,
                                    favicon: android.graphics.Bitmap?,
                                ) { loading = true; failed = false }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                }
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: android.webkit.WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        loading = false; failed = true
                                    }
                                }
                            }
                            loadUrl(API_URL)
                        }
                    },
                )
                if (loading) {
                    CircularProgressIndicator(
                        color = primary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (failed) {
                    Column(
                        Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "couldn't load the Konomi reference",
                            color = Color(0xFFB85C5C),
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "check device connectivity — the bundle lives on GitHub Pages",
                            color = Color(0xFF8B95A0),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
