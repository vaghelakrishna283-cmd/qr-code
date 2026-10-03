package com.krishna.upiqr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("upi_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                UpiQrScreen(
                    initialUpiId = prefs.getString("upi_id", "") ?: "",
                    onSaveUpiId = { prefs.edit().putString("upi_id", it.trim()).apply() },
                    onShare = { shareBitmap(it) }
                )
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        val file = File(cacheDir, "upi_payment_qr.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share QR"))
    }
}

@Composable
fun UpiQrScreen(initialUpiId: String, onSaveUpiId: (String) -> Unit, onShare: (Bitmap) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf(initialUpiId) }
    var payeeName by remember { mutableStateOf("UPI Payment") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val validAmount = amount != null && amount > 0.0
    val validUpi = Regex("^[A-Za-z0-9.\\-_]{2,256}@[A-Za-z0-9.\\-_]{2,64}$").matches(upiId.trim())

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Text("UPI Payment", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Generate a payment QR", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { if (it.matches(Regex("^\\d{0,9}(\\.\\d{0,2})?$"))) amountText = it; error = "" },
            label = { Text("Amount (₹)") }, placeholder = { Text("500") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = upiId, onValueChange = { upiId = it; error = "" },
            label = { Text("UPI ID") }, placeholder = { Text("yourname@upi") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = payeeName, onValueChange = { payeeName = it },
            label = { Text("Payee name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                when {
                    !validAmount -> error = "Please enter a valid amount."
                    !validUpi -> error = "Please enter a valid UPI ID, e.g. name@upi."
                    else -> {
                        onSaveUpiId(upiId)
                        qrBitmap = generateQr(buildUpiUri(upiId.trim(), payeeName.trim().ifEmpty { "UPI Payment" }, amount!!), 900)
                        error = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(12.dp)
        ) { Text("GENERATE QR", fontWeight = FontWeight.Bold) }

        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))

        qrBitmap?.let { bitmap ->
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "UPI Payment QR", modifier = Modifier.size(290.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("₹${String.format(Locale.US, "%.2f", amount ?: 0.0)}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(upiId, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { onShare(bitmap) }, modifier = Modifier.fillMaxWidth()) { Text("SHARE QR") }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("The QR uses the standard UPI payment URI and can be scanned by compatible UPI apps.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun buildUpiUri(upiId: String, payeeName: String, amount: Double): String {
    fun enc(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    return "upi://pay?pa=${enc(upiId)}&pn=${enc(payeeName)}&am=${String.format(Locale.US, "%.2f", amount)}&cu=INR"
}

fun generateQr(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8")
    val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size)
    for (y in 0 until size) for (x in 0 until size) pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
    return Bitmap.createBitmap(pixels, 0, size, size, Bitmap.Config.ARGB_8888)
}
