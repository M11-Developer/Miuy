package com.miyu.companion.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OverlayPermission {
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    /**
     * IMPORTANT SECURITY NOTE:
     * - We NEVER use overlay to read other apps' content
     * - We NEVER capture keystrokes or passwords
     * - We NEVER do automatic clicks in other apps
     * - Overlay is ONLY for showing Miyu character bubble
     * - User must explicitly grant permission
     * - Foreground notification is always shown when overlay is active
     */
    fun getSecurityDisclosure(): String {
        return """
        Miyu Floating Companion يستخدم إذن الظهور فوق التطبيقات الأخرى لعرض شخصية Miyu فقط.

        ما لا يفعله Miyu:
        • لا يقرأ محتوى التطبيقات الأخرى
        • لا يسجل ضغطات المفاتيح أو كلمات المرور
        • لا يراقب الشاشة
        • لا ينقر تلقائياً داخل التطبيقات
        • لا يعمل بشكل مخفي

        ما يفعله:
        • يعرض فقاعة Miyu العائمة
        • يمكن سحبها وتغيير حجمها
        • يفتح لوحة تحكم عند النقر
        • يمكن إخفاؤها أو إيقافها في أي وقت

        يمكنك إيقاف الخدمة من الإشعار الدائم.
        """.trimIndent()
    }
}
