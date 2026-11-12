package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListPopupWindow
import android.widget.ListView
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.VideoSpeedController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter
import java.util.Locale

class PlaybackSpeedAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val speedController = VideoSpeedController(playbackController)
	private val speeds = VideoSpeedController.SpeedSteps.entries.toTypedArray()
	private var popup: ListPopupWindow? = null

	init {
		initializeWithIcon(R.drawable.ic_playback_speed)
	}


	private companion object {
		// Enough room for the radio button and the speed label
		const val MENU_WIDTH_DP = 120
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()

		val selectedIndex = speeds.indexOf(speedController.currentSpeed).coerceAtLeast(0)
		val labels = speeds.map { String.format(Locale.US, "%.2fx", it.speed) }

		val listPopup = ListPopupWindow(context)
		listPopup.anchorView = view
		listPopup.setDropDownGravity(Gravity.END)
		// The radio button of the single choice layout is wide, give the popup enough room for the label
		listPopup.setContentWidth((MENU_WIDTH_DP * context.resources.displayMetrics.density).toInt())
		// Make the popup focusable, otherwise the remote control events stay on the playback screen
		listPopup.setModal(true)
		listPopup.setAdapter(SpeedAdapter(context, labels, selectedIndex))
		listPopup.setOnItemClickListener { _, _, position, _ ->
			speedController.currentSpeed = speeds[position]
			listPopup.dismiss()
		}
		listPopup.setOnDismissListener {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			popup = null
		}

		popup = listPopup
		listPopup.show()

		// Open the menu on the currently selected speed: check it, scroll it into view and focus it
		listPopup.listView?.apply {
			choiceMode = ListView.CHOICE_MODE_SINGLE
			setItemChecked(selectedIndex, true)
			setSelection(selectedIndex)
		}
		listPopup.setSelection(selectedIndex)
		view.post { listPopup.setSelection(selectedIndex) }
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}


	private class SpeedAdapter(
		context: Context,
		labels: List<String>,
		private val selectedIndex: Int,
	) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, labels) {
		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			val view = super.getView(position, convertView, parent)
			if (view is CheckedTextView) {
				view.isChecked = position == selectedIndex
			}
			return view
		}
	}
