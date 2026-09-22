package com.miyu.companion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.companion.data.*
import com.miyu.companion.service.FloatingCompanionService
import com.miyu.companion.ui.*
import com.miyu.companion.util.OverlayPermission
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check again after returning from settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = intent?.let {
            if (it.action == Intent.ACTION_SEND && it.type == "text/plain") {
                it.getStringExtra(Intent.EXTRA_TEXT)
            } else null
        }

        val showControls = intent?.getBooleanExtra("show_controls", false) ?: false

        setContent {
            MaterialTheme {
                MiyuApp(
                    sharedText = sharedText,
                    showControlsInitially = showControls,
                    onRequestOverlay = { requestOverlayPermission() },
                    onStartFloating = { startFloatingService() },
                    onStopFloating = { stopFloatingService() },
                    onOpenApp = { packageName -> openApp(packageName) }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (!OverlayPermission.hasPermission(this)) {
            val intent = OverlayPermission.requestPermissionIntent(this)
            overlayPermissionLauncher.launch(intent)
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingCompanionService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingCompanionService::class.java).apply {
            action = FloatingCompanionService.ACTION_STOP
        }
        startService(intent)
    }

    private fun openApp(packageName: String) {
        try {
            when (packageName.lowercase()) {
                "youtube" -> {
                    val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
                    startActivity(intent)
                }
                "chrome", "browser" -> {
                    val intent = packageManager.getLaunchIntentForPackage("com.android.chrome")
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
                    startActivity(intent)
                }
                "focus" -> {
                    // Open our own focus room
                }
                else -> {
                    // Try to launch by package, else search
                    val launch = packageManager.getLaunchIntentForPackage(packageName)
                    if (launch != null) startActivity(launch)
                    else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$packageName")))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiyuApp(
    sharedText: String?,
    showControlsInitially: Boolean,
    onRequestOverlay: () -> Unit,
    onStartFloating: () -> Unit,
    onStopFloating: () -> Unit,
    onOpenApp: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesRepository(context) }
    val scope = rememberCoroutineScope()

    var currentToy by remember { mutableStateOf<ToyType?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(showControlsInitially) }
    var selectedAge by remember { mutableStateOf<Int?>(null) }
    var selectedLanguage by remember { mutableStateOf("ar") }
    var respect by remember { mutableStateOf(100) }

    val miyuState by prefs.miyuState.collectAsState(initial = MiyuState())
    val floatingPos by prefs.floatingPosition.collectAsState(initial = FloatingPosition())

    LaunchedEffect(miyuState) {
        selectedAge = miyuState.age
        selectedLanguage = miyuState.language
        respect = miyuState.respectMeter
        currentToy = miyuState.toy?.let { key -> ToyType.values().find { it.key == key } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Miyu — a little company ✨", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Safety banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("🔒 Safe Play Mode", fontWeight = FontWeight.Bold)
                    Text(
                        "Miyu لا تقرأ التطبيقات الأخرى ولا تسجل كلمات المرور. الفقاعة العائمة تظهر شخصية Miyu فقط.",
                        fontSize = 12.sp
                    )
                }
            }

            // Overlay permission section
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Floating Companion", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "أظهري Miyu كفقاعة عائمة فوق التطبيقات الأخرى",
                        fontSize = 14.sp
                    )

                    val hasPermission = OverlayPermission.hasPermission(context)
                    if (!hasPermission) {
                        Button(onClick = {
                            showPermissionDialog = true
                        }) {
                            Text("طلب إذن الظهور فوق التطبيقات")
                        }
                        Text(
                            "يجب منح الإذن من إعدادات النظام. Miyu لن تفعّل الفقاعة تلقائياً بدون موافقتك.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onStartFloating) {
                                Text("إظهار الفقاعة")
                            }
                            OutlinedButton(onClick = onStopFloating) {
                                Text("إيقاف")
                            }
                        }

                        // Controls for bubble
                        if (showControls || true) {
                            Divider(Modifier.padding(vertical = 8.dp))
                            Text("تحكم الفقاعة", fontWeight = FontWeight.Bold)

                            // Size slider
                            var size by remember(floatingPos.size) { mutableStateOf(floatingPos.size) }
                            Text("الحجم: ${(size * 100).toInt()}%")
                            Slider(
                                value = size,
                                onValueChange = { size = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        prefs.savePosition(floatingPos.copy(size = size))
                                    }
                                },
                                valueRange = 0.5f..1.5f
                            )

                            // Alpha slider
                            var alpha by remember(floatingPos.alpha) { mutableStateOf(floatingPos.alpha) }
                            Text("الشفافية: ${(alpha * 100).toInt()}%")
                            Slider(
                                value = alpha,
                                onValueChange = { alpha = it },
                                onValueChangeFinished = {
                                    scope.launch {
                                        prefs.savePosition(floatingPos.copy(alpha = alpha))
                                    }
                                },
                                valueRange = 0.3f..1f
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = floatingPos.snapToEdge,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            prefs.savePosition(floatingPos.copy(snapToEdge = checked))
                                        }
                                    }
                                )
                                Text("تثبيت على الحافة (Snap to edges)")

                                Spacer(Modifier.width(8.dp))

                                Checkbox(
                                    checked = floatingPos.isMini,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            prefs.savePosition(floatingPos.copy(isMini = checked))
                                        }
                                    }
                                )
                                Text("وضع مصغر")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = floatingPos.clickThrough,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            prefs.savePosition(floatingPos.copy(clickThrough = checked))
                                        }
                                    }
                                )
                                Text("Click-through Mode (تمرير النقر)")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = floatingPos.motionPaused,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            prefs.savePosition(floatingPos.copy(motionPaused = checked))
                                        }
                                    }
                                )
                                Text("إيقاف الحركة (Stop motion)")
                            }
                        }
                    }
                }
            }

            // Play Lab
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Miyu Play Lab 🎮", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("قولي: امسكي عربية لعبة أو Hold a toy car", fontSize = 13.sp)

                    var input by remember { mutableStateOf(sharedText ?: "") }
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("ماذا تمسك Miyu؟") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        val toy = ToyType.fromInput(input)
                        if (toy != null) {
                            currentToy = toy
                            scope.launch {
                                prefs.setCurrentToy(toy.key)
                            }
                        }
                    }) {
                        Text("اعرض اللعبة")
                    }

                    if (currentToy != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(currentToy!!.emoji, fontSize = 32.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(currentToy!!.labelAr, fontWeight = FontWeight.Bold)
                                    Text(currentToy!!.labelEn, fontSize = 12.sp)
                                    Text("Miyu ماسكة ${currentToy!!.labelAr} — حركة مرحة!", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Quick chips
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        ToyType.values().take(5).forEach { toy ->
                            FilterChip(
                                selected = currentToy == toy,
                                onClick = {
                                    currentToy = toy
                                    scope.launch { prefs.setCurrentToy(toy.key) }
                                },
                                label = { Text(toy.emoji) }
                            )
                        }
                    }
                }
            }

            // Age profile
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("العمر واللغة", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 8, 12, 16, 18).forEach { age ->
                            FilterChip(
                                selected = selectedAge == age,
                                onClick = {
                                    selectedAge = age
                                    scope.launch {
                                        prefs.saveAgeProfile(age, selectedLanguage, true)
                                    }
                                },
                                label = { Text("$age") }
                            )
                        }
                    }
                    Row {
                        FilterChip(
                            selected = selectedLanguage == "ar",
                            onClick = {
                                selectedLanguage = "ar"
                                scope.launch {
                                    selectedAge?.let { prefs.saveAgeProfile(it, "ar", true) }
                                }
                            },
                            label = { Text("العربية") }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = selectedLanguage == "en",
                            onClick = {
                                selectedLanguage = "en"
                                scope.launch {
                                    selectedAge?.let { prefs.saveAgeProfile(it, "en", true) }
                                }
                            },
                            label = { Text("English") }
                        )
                    }
                    Text("Kindness meter: $respect% · الكلام اللطيف يجعل اللعب أجمل", fontSize = 11.sp)
                    LinearProgressIndicator(progress = { respect / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Owner Edition: only rendered in private owner builds (public APKs have
            // BuildConfig.MIYU_OWNER_BUILD == false, so this composable returns immediately).
            OwnerPanelCard(isAdultProfile = (selectedAge ?: 0) >= 18)

            // App launcher (safe intents only)
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("فتح التطبيقات (بإذنك)", fontWeight = FontWeight.Bold)
                    Text("Miyu تفتح التطبيقات باستخدام Intent فقط، لا تتحكم بمحتواها", fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenApp("youtube") }) { Text("YouTube") }
                        Button(onClick = { onOpenApp("chrome") }) { Text("Chrome") }
                        OutlinedButton(onClick = { onOpenApp("focus") }) { Text("Focus") }
                    }
                }
            }

            // Support card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ادعمي Miyu 💖", fontWeight = FontWeight.Bold)
                    Text("Vodafone Cash", fontSize = 12.sp)
                    Text("01027653109", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("النسخ لا يبدأ تحويلاً تلقائياً. تأكدي من الرقم.", fontSize = 10.sp)
                }
            }

            // Info about iOS limits
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("ملاحظة عن iOS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "iOS لا يسمح بفقاعة حرة فوق كل التطبيقات مثل Android. بدائل iOS: Picture-in-Picture, Widgets, Live Activities, Dynamic Island.",
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("إذن الظهور فوق التطبيقات") },
            text = { Text(OverlayPermission.getSecurityDisclosure()) },
            confirmButton = {
                Button(onClick = {
                    showPermissionDialog = false
                    onRequestOverlay()
                }) {
                    Text("فتح الإعدادات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
