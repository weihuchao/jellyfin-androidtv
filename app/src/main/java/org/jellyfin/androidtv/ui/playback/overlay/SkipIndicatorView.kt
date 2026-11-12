package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.util.TimeUtils
import kotlin.math.abs

/**
 * Time the hint stays visible after the last fast forward or rewind request.
 */
private const val AUTO_HIDE_DELAY = 1000L

private const val MILLIS_PER_SECOND = 1000L

private data class SkipIndicatorState(
	val forward: Boolean,
	val offsetMs: Long,
)

@Composable
private fun SkipIndicatorComposable(
	visible: Boolean,
	forward: Boolean,
	text: String,
) {
	Box(
		contentAlignment = Alignment.BottomEnd,
		modifier = Modifier
			.padding(48.dp)
	) {
		AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
			Row(
				modifier = Modifier
					.clip(RoundedCornerShape(6.dp))
					.background(colorResource(R.color.popup_menu_background).copy(alpha = 0.6f))
					.padding(10.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					imageVector = ImageVector.vectorResource(
						if (forward) R.drawable.ic_fast_forward else R.drawable.ic_rewind
					),
					contentDescription = null,
				)

				Text(
					text = text,
					color = colorResource(R.color.button_default_normal_text),
					fontSize = 18.sp,
				)
			}
		}
	}
}

/**
 * Shows a hint while the user is fast forwarding or rewinding, so the pending jump is
 * visible even though the player only moves once the key is released.
 */
class SkipIndicatorView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
	private val _state = MutableStateFlow(SkipIndicatorState(forward = true, offsetMs = 0L))
	private val _visible = MutableStateFlow(false)
	private val handler = Handler(Looper.getMainLooper())
	private val hideRunnable = Runnable { _visible.value = false }

	/**
	 * Shows the hint for a fast forward or rewind request.
	 *
	 * @param offsetMs The accumulated offset of the current gesture
	 * @param forward True when fast forwarding, false when rewinding
	 */
	fun show(offsetMs: Long, forward: Boolean) {
		_state.value = SkipIndicatorState(forward = forward, offsetMs = offsetMs)
		_visible.value = true

		handler.removeCallbacks(hideRunnable)
		handler.postDelayed(hideRunnable, AUTO_HIDE_DELAY)
	}

	@Composable
	override fun Content() {
		val state by _state.collectAsState()
		val visible by _visible.collectAsState()
		val context = LocalContext.current

		val label = stringResource(if (state.forward) R.string.fast_forward else R.string.rewind)
		val duration = TimeUtils.formatSeconds(context, (abs(state.offsetMs) / MILLIS_PER_SECOND).toInt())

		SkipIndicatorComposable(visible, state.forward, "$label $duration")
	}
}
