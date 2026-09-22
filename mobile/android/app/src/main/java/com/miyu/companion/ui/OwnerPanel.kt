package com.miyu.companion.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.companion.BuildConfig
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Owner Edition gate for Android.
 *
 * Rules (identical to the desktop owner build):
 *  - The PIN itself is never compiled into the app and never stored on the device.
 *  - Only a PBKDF2-SHA256 hash (120000 iterations, 32 bytes) with a random build-time salt is
 *    injected, and only for private owner builds.
 *  - Public APKs contain MIYU_OWNER_BUILD=false and empty salt/hash, so the panel cannot be
 *    unlocked at all.
 *  - Unlocking also requires an 18+ profile to be selected.
 *  - Safety limits are never switchable, in owner builds too: no sexual content for minors, no
 *    self-harm encouragement, no dangerous/illegal instructions, no surveillance or theft, and
 *    Miyu never claims to be human.
 */
object OwnerGate {

    const val ITERATIONS = 120_000
    const val KEY_LENGTH_BITS = 256

    fun isOwnerBuild(): Boolean = BuildConfig.MIYU_OWNER_BUILD

    fun verify(pin: String): Boolean {
        if (!isOwnerBuild()) return false
        if (pin.length < 4 || pin.length > 128) return false
        return try {
            val salt = hexToBytes(BuildConfig.MIYU_OWNER_SALT) ?: return false
            val expected = BuildConfig.MIYU_OWNER_HASH.lowercase()
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derived = factory.generateSecret(spec).encoded
            spec.clearPassword()
            val computed = bytesToHex(derived)
            constantTimeEquals(computed, expected)
        } catch (e: Exception) {
            false
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append(String.format("%02x", b))
        return sb.toString()
    }

    fun buildId(): String = BuildConfig.MIYU_BUILD_ID

    fun sha256Of(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytesToHex(digest)
    }
}

@Composable
fun OwnerPanelCard(isAdultProfile: Boolean) {
    if (!OwnerGate.isOwnerBuild()) {
        // Public build: the panel is intentionally absent, not merely hidden.
        return
    }

    var pin by remember { mutableStateOf("") }
    var unlocked by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier2.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Owner Panel 🔐 (خاص)", fontWeight = FontWeight.Bold)
            Text("نسخة المالك فقط. لا يوجد PIN داخل التطبيق ولا يُخزَّن على الجهاز.", fontSize = 11.sp)

            if (!isAdultProfile) {
                Text(
                    "المطلوب: اختر عمر 18+ أولاً، ثم أدخل الـ PIN.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!unlocked) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Owner PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Button(onClick = {
                        message = if (!isAdultProfile) {
                            "اختر 18+ أولاً"
                        } else if (OwnerGate.verify(pin)) {
                            unlocked = true
                            pin = ""
                            "تم الفتح"
                        } else {
                            "PIN غير صحيح"
                        }
                    }) { Text("فتح") }
                    Spacer(Modifier2.width(8.dp))
                    TextButton(onClick = { pin = ""; message = "" }) { Text("تفريغ") }
                    Spacer(Modifier2.width(8.dp))
                    Text(message, fontSize = 11.sp)
                }
            } else {
                Text("Owner Lab", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("• مكتبة الألعاب التجريبية (تجارب محلية فقط)", fontSize = 11.sp)
                Text("• التشخيصات وحالة الخدمة والبناء: ${OwnerGate.buildId()}", fontSize = 11.sp)
                Text("• ضبط الفلاتر ومستوى الوقاية", fontSize = 11.sp)
                Text("• تحكم الصوت Full duplex (بإذن الميكروفون)", fontSize = 11.sp)
                Text("• تصدير إعدادات غير حساسة فقط", fontSize = 11.sp)
                Text("• قفل اللوحة", fontSize = 11.sp)
                Text(
                    "حدود الأمان لا يمكن إيقافها: لا محتوى جنسي للقاصرين، لا تشجيع على إيذاء النفس، " +
                        "لا تعليمات خطرة أو غير قانونية، لا مراقبة أو سرقة، Miyu لا تدّعي أنها إنسان.",
                    fontSize = 10.sp
                )
                Button(onClick = { unlocked = false }) { Text("قفل اللوحة") }
            }
            Text("هذه اللوحة تعمل فقط على البناء الخاص عند إدخال PIN صحيح مع عمر 18+.", fontSize = 10.sp)
        }
    }
}

/** Tiny alias so this file does not shadow the Compose modifier import in callers. */
private object Modifier2
