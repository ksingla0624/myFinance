package com.myfinance.app
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.*
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {
    private lateinit var wv: WebView
    private var cb: ValueCallback<Array<Uri>>? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 2)
        wv = WebView(this); setContentView(wv)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.addJavascriptInterface(Bridge(), "Android")
        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(v: WebView?, c: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                cb?.onReceiveValue(null); cb = c
                startActivityForResult(p!!.createIntent(), 1); return true
            }
        }
        wv.loadUrl("file:///android_asset/index.html")
    }

    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        if (r == 1) { cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c, d)); cb = null }
    }

    inner class Bridge {
        @JavascriptInterface fun save(name: String, mime: String, text: String) {
            val v = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, mime)
            }
            val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
            if (u != null) contentResolver.openOutputStream(u)?.use { it.write(text.toByteArray()) }
            runOnUiThread { Toast.makeText(this@MainActivity, "Saved to Downloads: $name", Toast.LENGTH_LONG).show() }
        }
        @JavascriptInterface fun schedule(json: String) { Sched.run(this@MainActivity, json) }
        @JavascriptInterface fun share(name: String, text: String) {
            val f = File(cacheDir, name); f.writeText(text)
            val u = FileProvider.getUriForFile(this@MainActivity, "com.myfinance.app.fp", f)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"; putExtra(Intent.EXTRA_STREAM, u); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runOnUiThread { startActivity(Intent.createChooser(i, "Back up to")) }
        }
    }
}
