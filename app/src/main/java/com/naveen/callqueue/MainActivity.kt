package com.naveen.callqueue

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.naveen.callqueue.service.AutoDialService
import com.naveen.callqueue.telecom.SimAccountHelper
import com.naveen.callqueue.telecom.SimOption
import com.naveen.callqueue.ui.DialingScreen
import com.naveen.callqueue.ui.PasteNumbersScreen
import com.naveen.callqueue.ui.PermissionsScreen
import com.naveen.callqueue.ui.ReviewListScreen
import com.naveen.callqueue.ui.SettingsScreen
import com.naveen.callqueue.ui.theme.CallQueueTheme
import kotlinx.coroutines.launch

private val REQUIRED_PERMISSIONS: Array<String> = buildList {
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.READ_PHONE_NUMBERS)
    add(Manifest.permission.READ_CALL_LOG)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()

class MainActivity : ComponentActivity() {

    private val viewModel: AutoDialViewModel by viewModels()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? AutoDialService.LocalBinder)?.getService()
            viewModel.bindService(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            viewModel.bindService(null)
        }
    }

    private var bound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CallQueueTheme {
                var permissionsGranted by remember { mutableStateOf(hasAllPermissions()) }
                var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations()) }

                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    permissionsGranted = results.values.all { it }
                    if (permissionsGranted) startAndBindService()
                }

                // Re-check both after the user comes back from the runtime-permission dialog or
                // the battery-optimization settings screen, since neither reports back directly.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            permissionsGranted = hasAllPermissions()
                            batteryExempt = isIgnoringBatteryOptimizations()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(Unit) {
                    if (permissionsGranted) startAndBindService()
                }

                AppNavHost(
                    viewModel = viewModel,
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = { permissionLauncher.launch(REQUIRED_PERMISSIONS) },
                    batteryExempt = batteryExempt,
                    onRequestBatteryExemption = { requestBatteryExemption() }
                )
            }
        }
    }

    override fun onDestroy() {
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    private fun hasAllPermissions(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun startAndBindService() {
        // Plain bind, not startForegroundService: the service only promotes itself to a
        // foreground service (with its notification) once a dial session actually begins.
        val intent = Intent(this, AutoDialService::class.java)
        bound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}

@Composable
private fun AppNavHost(
    viewModel: AutoDialViewModel,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    batteryExempt: Boolean,
    onRequestBatteryExemption: () -> Unit
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val service by viewModel.service.collectAsState()
    val currentService = service
    val sessionState = if (currentService != null) currentService.state.collectAsState().value else null

    val setupComplete = permissionsGranted && batteryExempt

    LaunchedEffect(setupComplete) {
        if (setupComplete && navController.currentDestination?.route == "permissions") {
            navController.navigate("paste") { popUpTo("permissions") { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = if (setupComplete) "paste" else "permissions") {

        composable("permissions") {
            PermissionsScreen(
                permissionsGranted = permissionsGranted,
                onGrantClick = onRequestPermissions,
                batteryExempt = batteryExempt,
                onRequestBatteryExemption = onRequestBatteryExemption
            )
        }

        composable("paste") {
            PasteNumbersScreen(
                initialText = viewModel.pasteText.value,
                onContinue = { text, parsed ->
                    viewModel.pasteText.value = text
                    viewModel.loadIntoReview(parsed)
                    navController.navigate("review")
                },
                onOpenSettings = { navController.navigate("settings") }
            )
        }

        composable("review") {
            ReviewListScreen(
                entries = viewModel.reviewEntries,
                listName = viewModel.listName.value,
                onListNameChange = { viewModel.listName.value = it },
                onRemove = viewModel::removeReviewEntry,
                onRename = viewModel::renameReviewEntry,
                onBack = { navController.popBackStack() },
                onStart = {
                    scope.launch {
                        val simHandle = viewModel.prefs.getSimAccountHandle()
                        val sims = SimAccountHelper.listSims(context)
                        if (simHandle == null && sims.size > 1) {
                            Toast.makeText(context, "Pick a default SIM in Settings first", Toast.LENGTH_LONG).show()
                            navController.navigate("settings")
                        } else {
                            viewModel.startSession()
                            navController.navigate("dialing") {
                                popUpTo("paste")
                            }
                        }
                    }
                }
            )
        }

        composable("dialing") {
            if (sessionState != null) {
                DialingScreen(
                    state = sessionState,
                    onHold = { service?.hold() },
                    onResume = { service?.resumeSession() },
                    onSkip = { service?.skip() },
                    onLater = { service?.later() },
                    onTag = { service?.submitOutcome(it) },
                    onRedialNow = { service?.redialNow() },
                    onEndSession = {
                        service?.stopSession()
                        navController.navigate("paste") { popUpTo("paste") { inclusive = true } }
                    }
                )
            }
        }

        composable("settings") {
            var sims by remember { mutableStateOf<List<SimOption>>(emptyList()) }
            LaunchedEffect(Unit) { sims = SimAccountHelper.listSims(context) }

            val retryMax by viewModel.prefs.retryMax.collectAsState(initial = 2)
            val outcomeTaggingEnabled by viewModel.prefs.outcomeTaggingEnabled.collectAsState(initial = true)
            val gapSeconds by viewModel.prefs.gapSeconds.collectAsState(initial = 4)
            val selectedSimId by viewModel.prefs.simId.collectAsState(initial = null)

            SettingsScreen(
                sims = sims,
                selectedSimId = selectedSimId,
                onSelectSim = { sim -> scope.launch { viewModel.prefs.setSimAccount(sim.handle, sim.label) } },
                retryMax = retryMax,
                onRetryMaxChange = { scope.launch { viewModel.prefs.setRetryMax(it) } },
                outcomeTaggingEnabled = outcomeTaggingEnabled,
                onOutcomeTaggingChange = { scope.launch { viewModel.prefs.setOutcomeTagging(it) } },
                gapSeconds = gapSeconds,
                onGapChange = { scope.launch { viewModel.prefs.setGapSeconds(it) } },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
