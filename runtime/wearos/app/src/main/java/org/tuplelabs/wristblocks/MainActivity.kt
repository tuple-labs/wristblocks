package org.tuplelabs.wristblocks

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.delay
import org.json.JSONObject

/** One block from app.json; fields that a block type doesn't use are just defaults. */
data class Block(val type: String, val text: String, val label: String, val goal: Int, val step: Int, val minutes: Int, val gesture: Boolean)

data class Spec(val name: String, val color: Color, val blocks: List<Block>) {
    companion object {
        fun load(c: Context): Spec {
            val o = JSONObject(c.assets.open("app.json").bufferedReader().readText())
            val arr = o.getJSONArray("blocks")
            val blocks = (0 until arr.length()).map {
                val b = arr.getJSONObject(it)
                Block(
                    b.getString("type"), b.optString("text"), b.optString("label"),
                    b.optInt("goal"), b.optInt("step", 1), b.optInt("minutes", 25), b.optBoolean("gesture"),
                )
            }
            val hex = o.optString("color", "#F2C46D").removePrefix("#").toLong(16)
            return Spec(o.getString("name"), Color(0xFF000000 or hex), blocks)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spec = Spec.load(this)
        setContent {
            MaterialTheme(colorScheme = ColorScheme(primary = spec.color, onPrimary = Color(0xFF111111))) {
                AppScaffold {
                    val list = rememberTransformingLazyColumnState()
                    ScreenScaffold(scrollState = list) { padding ->
                        TransformingLazyColumn(state = list, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                            spec.blocks.forEachIndexed { i, b -> item { BlockView(i, b) } }
                        }
                    }
                }
            }
        }
    }
}

fun Context.buzz(strong: Boolean) {
    val v = (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    v.vibrate(
        if (strong) VibrationEffect.createWaveform(longArrayOf(0, 90, 70, 90, 70, 250), -1)
        else VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK),
    )
}

@Composable
fun BlockView(index: Int, b: Block) {
    when (b.type) {
        "title" -> Text(b.text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        "counter" -> CounterBlock("counter.$index", b)
        "timer" -> TimerBlock(b)
        else -> Text(b.text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))
    }
}

/** The button that double pinch (Wear OS 7, Pixel Watch 3+) presses; a normal button everywhere else. */
@Composable
fun PrimaryButton(label: String, gesture: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val config = rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val mod = if (gesture) Modifier.oneHandedGesture(gestureConfiguration = config, interactionSource = interaction,
        onGestureLabel = label, onGesture = onClick) else Modifier
    Button(onClick = onClick, interactionSource = interaction, modifier = mod) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ResetButton(onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.filledTonalButtonColors()) { Text("↺") }
}

@Composable
fun CounterBlock(key: String, b: Block) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("wristblocks", Context.MODE_PRIVATE) }
    var count by remember { mutableIntStateOf(prefs.getInt(key, 0)) }
    fun set(v: Int) { count = v; prefs.edit().putInt(key, v).apply() }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(b.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        if (b.goal > 0) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
                CircularProgressIndicator(progress = { (count % b.goal) / b.goal.toFloat() },
                    modifier = Modifier.size(72.dp), strokeWidth = 5.dp)
                Text("${count % b.goal}", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            }
            if (count >= b.goal) Text("Rounds: ${count / b.goal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("$count", fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("+${b.step}", b.gesture) {
                set(count + b.step)
                ctx.buzz(strong = b.goal > 0 && count % b.goal < b.step)
            }
            ResetButton { set(0) }
        }
    }
}

@Composable
fun TimerBlock(b: Block) {
    val ctx = LocalContext.current
    val total = b.minutes * 60_000L
    var endAt by remember { mutableLongStateOf(0L) }
    var pausedLeft by remember { mutableStateOf<Long?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endAt) {
        while (endAt > 0) {
            now = System.currentTimeMillis()
            if (now >= endAt) { endAt = 0; pausedLeft = null; ctx.buzz(strong = true) }
            delay(250)
        }
    }
    val left = if (endAt > 0) (endAt - now).coerceAtLeast(0) else pausedLeft ?: total
    val s = ((left + 999) / 1000).toInt()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(b.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text("%d:%02d".format(s / 60, s % 60), fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton(if (endAt > 0) "Pause" else if (pausedLeft != null) "Resume" else "Start", b.gesture) {
                val t = System.currentTimeMillis()
                if (endAt > 0) { pausedLeft = (endAt - t).coerceAtLeast(0); endAt = 0 }
                else { endAt = t + (pausedLeft ?: total); pausedLeft = null; now = t }
                ctx.buzz(strong = false)
            }
            ResetButton { endAt = 0; pausedLeft = null }
        }
    }
}
