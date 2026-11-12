package org.jellyfin.androidtv.ui.playback.overlay;

import static java.lang.Math.round;

import android.content.Context;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter;
import androidx.leanback.widget.Action;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowView;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SeekBar;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.UserPreferences;
import org.jellyfin.androidtv.preference.constant.ClockBehavior;
import org.jellyfin.androidtv.ui.playback.PlaybackController;
import org.jellyfin.androidtv.ui.playback.overlay.action.AndroidAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChannelBarChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ChapterAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ClosedCaptionsAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.CustomAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.FastForwardAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.GuideAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlayPauseAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PlaybackSpeedAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.PreviousLiveTvChannelAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RecordAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.RewindAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectAudioAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SelectQualityAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipNextAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.SkipPreviousAction;
import org.jellyfin.androidtv.ui.playback.overlay.action.ZoomAction;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.koin.java.KoinJavaComponent;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CustomPlaybackTransportControlGlue extends PlaybackTransportControlGlue<VideoPlayerAdapter> {

    // Normal playback actions
    private PlayPauseAction playPauseAction;
    private RewindAction rewindAction;
    private FastForwardAction fastForwardAction;
    private SkipPreviousAction skipPreviousAction;
    private SkipNextAction skipNextAction;
    private SelectAudioAction selectAudioAction;
    private ClosedCaptionsAction closedCaptionsAction;
    private SelectQualityAction selectQualityAction;
    private PlaybackSpeedAction playbackSpeedAction;
    private ZoomAction zoomAction;
    private ChapterAction chapterAction;

    // TV actions
    private PreviousLiveTvChannelAction previousLiveTvChannelAction;
    private ChannelBarChannelAction channelBarChannelAction;
    private GuideAction guideAction;
    private RecordAction recordAction;

    private final PlaybackController playbackController;
    private ArrayObjectAdapter primaryActionsAdapter;
    private ArrayObjectAdapter secondaryActionsAdapter;

    // Injected views
    private TextView mEndsText = null;

    // Focus groups of the playback controls
    private LinearLayout mButtonRef = null;
    private LinearLayout mSecondaryButtonRef = null;
    private SeekBar mProgressBar = null;

    // The action that was activated last, used to restore the selection when the controls appear
    private Action mLastActivatedAction = null;

    // The action that was focused last, used to restore the position in the controls
    private Action mLastFocusedAction = null;

    private final Handler mHandler = new Handler();
    private Runnable mRefreshEndTime;
    private Runnable mRefreshViewVisibility;

    CustomPlaybackTransportControlGlue(Context context, VideoPlayerAdapter playerAdapter, PlaybackController playbackController) {
        super(context, playerAdapter);
        this.playbackController = playbackController;

        mRefreshEndTime = () -> {
            setEndTime();
            if (!isPlaying()) {
                mHandler.postDelayed(mRefreshEndTime, 30000);
            }
        };

        mRefreshViewVisibility = () -> {
            if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
                mEndsText.setVisibility(mButtonRef.getVisibility());
            else
                mHandler.postDelayed(mRefreshViewVisibility, 100);
        };

        initActions(context);
    }

    @Override
    protected void onDetachedFromHost() {
        mHandler.removeCallbacks(mRefreshEndTime);
        mHandler.removeCallbacks(mRefreshViewVisibility);

        closedCaptionsAction.removePopup();
        playbackSpeedAction.dismissPopup();
        selectAudioAction.dismissPopup();
        selectQualityAction.dismissPopup();
        zoomAction.dismissPopup();

        super.onDetachedFromHost();
    }

    @Override
    protected PlaybackRowPresenter onCreateRowPresenter() {
        final AbstractDetailsDescriptionPresenter detailsPresenter = new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(ViewHolder vh, Object item) {

            }
        };
        PlaybackTransportRowPresenter rowPresenter = new PlaybackTransportRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                RowPresenter.ViewHolder vh = super.createRowViewHolder(parent);

                // Keep track of the focus groups of the transport row
                LinearLayout rowView = (LinearLayout) vh.view;
                PlaybackTransportRowView bar = (PlaybackTransportRowView) rowView.getChildAt(1);
                mButtonRef = (LinearLayout) ((ViewGroup) bar.getChildAt(0)).getChildAt(0);
                mProgressBar = (SeekBar) bar.getChildAt(1);
                ViewGroup secondaryDock = (ViewGroup) bar.getChildAt(2)
                        .findViewById(androidx.leanback.R.id.secondary_controls_dock);
                mSecondaryButtonRef = (LinearLayout) secondaryDock.getChildAt(0);

                ClockBehavior showClock = KoinJavaComponent.<UserPreferences>get(UserPreferences.class).get(UserPreferences.Companion.getClockBehavior());

                if (showClock == ClockBehavior.ALWAYS || showClock == ClockBehavior.IN_VIDEO) {
                    Context context = parent.getContext();
                    mEndsText = new TextView(context);
                    mEndsText.setTextAppearance(context, androidx.leanback.R.style.Widget_Leanback_PlaybackControlsTimeStyle);
                    setEndTime();

                    FrameLayout v = (FrameLayout) bar.getChildAt(0);

                    bar.removeViewAt(0);
                    RelativeLayout rl = new RelativeLayout(context);
                    RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    rl.addView(v);

                    RelativeLayout.LayoutParams rlp2 = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
                    rlp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    rlp2.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    rl.addView(mEndsText, rlp2);
                    bar.addView(rl, 0, rlp);
                }

                return vh;
            }

            @Override
            protected void onProgressBarClicked(PlaybackTransportRowPresenter.ViewHolder vh) {
                // Clicking the progress bar must not change the playback state
            }

            @Override
            public void onReappear(RowPresenter.ViewHolder rowViewHolder) {
                // Don't move the focus back to the progress bar, the app manages the selection
            }

            @Override
            protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
                super.onBindRowViewHolder(vh, item);
                vh.setOnKeyListener(CustomPlaybackTransportControlGlue.this);
            }

            @Override
            protected void onUnbindRowViewHolder(RowPresenter.ViewHolder vh) {
                super.onUnbindRowViewHolder(vh);
                vh.setOnKeyListener(null);
            }
        };
        rowPresenter.setDescriptionPresenter(detailsPresenter);
        return rowPresenter;
    }

    private void initActions(Context context) {
        playPauseAction = new PlayPauseAction(context);
        rewindAction = new RewindAction(context);
        fastForwardAction = new FastForwardAction(context);
        skipPreviousAction = new SkipPreviousAction(context);
        skipNextAction = new SkipNextAction(context);
        selectAudioAction = new SelectAudioAction(context, this);
        selectAudioAction.setLabels(new String[]{context.getString(R.string.lbl_audio_track)});
        closedCaptionsAction = new ClosedCaptionsAction(context, this);
        closedCaptionsAction.setLabels(new String[]{context.getString(R.string.lbl_subtitle_track)});
        selectQualityAction = new SelectQualityAction(context, this, KoinJavaComponent.get(UserPreferences.class));
        selectQualityAction.setLabels(new String[]{context.getString(R.string.lbl_quality_profile)});
        playbackSpeedAction = new PlaybackSpeedAction(context, this, playbackController);
        playbackSpeedAction.setLabels(new String[]{context.getString(R.string.lbl_playback_speed)});
        zoomAction = new ZoomAction(context, this);
        zoomAction.setLabels(new String[]{context.getString(R.string.lbl_zoom)});
        chapterAction = new ChapterAction(context, this);
        chapterAction.setLabels(new String[]{context.getString(R.string.lbl_chapters)});

        previousLiveTvChannelAction = new PreviousLiveTvChannelAction(context, this);
        previousLiveTvChannelAction.setLabels(new String[]{context.getString(R.string.lbl_prev_item)});
        channelBarChannelAction = new ChannelBarChannelAction(context, this);
        channelBarChannelAction.setLabels(new String[]{context.getString(R.string.lbl_other_channels)});
        guideAction = new GuideAction(context, this);
        guideAction.setLabels(new String[]{context.getString(R.string.lbl_live_tv_guide)});
        recordAction = new RecordAction(context, this);
        recordAction.setLabels(new String[]{
                context.getString(R.string.lbl_record),
                context.getString(R.string.lbl_cancel_recording)
        });
    }

    @Override
    protected void onCreatePrimaryActions(ArrayObjectAdapter primaryActionsAdapter) {
        this.primaryActionsAdapter = primaryActionsAdapter;
    }

    @Override
    protected void onCreateSecondaryActions(ArrayObjectAdapter secondaryActionsAdapter) {
        this.secondaryActionsAdapter = secondaryActionsAdapter;
    }

    void addMediaActions() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();

        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        // Primary Items (shown above the progress bar)
        if (!playerAdapter.isLiveTv()) {
            primaryActionsAdapter.add(playbackSpeedAction);
        }

        primaryActionsAdapter.add(playPauseAction);

        if (playerAdapter.hasSubs()) {
            primaryActionsAdapter.add(closedCaptionsAction);
        }

        if (playerAdapter.hasNextItem()) {
            primaryActionsAdapter.add(skipNextAction);
        }

        if (playerAdapter.hasMultiAudio()) {
            primaryActionsAdapter.add(selectAudioAction);
        }

        if (playerAdapter.isLiveTv()) {
            primaryActionsAdapter.add(channelBarChannelAction);
            primaryActionsAdapter.add(guideAction);
        }

        // Secondary Items (shown below the progress bar)
        secondaryActionsAdapter.add(zoomAction);

        if (!playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(selectQualityAction);
        }

        if (playerAdapter.canSeek()) {
            secondaryActionsAdapter.add(rewindAction);
            secondaryActionsAdapter.add(fastForwardAction);
        }

        if (playerAdapter.hasPreviousItem()) {
            secondaryActionsAdapter.add(skipPreviousAction);
        }

        if (playerAdapter.hasNextItem()) {
            secondaryActionsAdapter.add(skipNextAction);
        }

        if (playerAdapter.hasChapters()) {
            secondaryActionsAdapter.add(chapterAction);
        }

        if (playerAdapter.isLiveTv()) {
            secondaryActionsAdapter.add(previousLiveTvChannelAction);
            if (playerAdapter.canRecordLiveTv()) {
                secondaryActionsAdapter.add(recordAction);
                recordingStateChanged();
            }
        }
    }

    @Override
    public void onActionClicked(Action action) {
        // Remember the action to restore the selection when the controls appear again
        mLastActivatedAction = action;

        if (action instanceof AndroidAction) {
            ((AndroidAction) action).onActionClicked(getPlayerAdapter());
        }
        notifyActionChanged(action);
    }

    /**
     * Select the action that was activated last, the action that was focused last, the playback
     * speed action or the play/pause action. Called when the playback controls become visible.
     */
    public void focusPreferredAction() {
        Action action = mLastActivatedAction;
        if (action == null || findActionView(action) == null) action = mLastFocusedAction;
        if (action == null || findActionView(action) == null) action = playbackSpeedAction;
        if (findActionView(action) == null) action = playPauseAction;

        focusAction(action);
    }

    /**
     * Move the focus between the actions and the progress bar.
     *
     * @return true if the key was handled
     */
    public boolean handleNavigationKey(int keyCode) {
        View focused = getFocusedControl();
        if (focused == null) return false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT: {
                // The progress bar uses left and right to skip forward or backward. The Leanback
                // seek ui is not used because it pauses the playback while seeking.
                if (focused == mProgressBar) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        playbackController.fastForward();
                    } else {
                        playbackController.rewind();
                    }
                    return true;
                }

                int step = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1;
                ViewGroup row = getControlRow(focused);
                if (row == null) return false;
                return focusSibling(row, focused, step);
            }

            case KeyEvent.KEYCODE_DPAD_UP:
                if (isInRow(focused, mSecondaryButtonRef)) return focusProgressBar();
                if (focused == mProgressBar) return focusControlRow(mButtonRef);
                return false;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (isInRow(focused, mButtonRef)) return focusProgressBar();
                if (focused == mProgressBar) return focusControlRow(mSecondaryButtonRef);
                return false;
        }

        return false;
    }

    private void focusAction(Action action) {
        View view = findActionView(action);
        if (view == null) return;

        view.requestFocus();
        mLastFocusedAction = action;
    }

    private Action findActionForView(View view) {
        if (view == null) return null;

        if (primaryActionsAdapter != null && mButtonRef != null) {
            int index = mButtonRef.indexOfChild(view);
            if (index >= 0 && index < primaryActionsAdapter.size()) {
                return (Action) primaryActionsAdapter.get(index);
            }
        }

        if (secondaryActionsAdapter != null && mSecondaryButtonRef != null) {
            int index = mSecondaryButtonRef.indexOfChild(view);
            if (index >= 0 && index < secondaryActionsAdapter.size()) {
                return (Action) secondaryActionsAdapter.get(index);
            }
        }

        return null;
    }

    private View findActionView(Action action) {
        if (primaryActionsAdapter == null || secondaryActionsAdapter == null) return null;

        int index = primaryActionsAdapter.indexOf(action);
        if (index >= 0 && mButtonRef != null && index < mButtonRef.getChildCount()) {
            return mButtonRef.getChildAt(index);
        }

        index = secondaryActionsAdapter.indexOf(action);
        if (index >= 0 && mSecondaryButtonRef != null && index < mSecondaryButtonRef.getChildCount()) {
            return mSecondaryButtonRef.getChildAt(index);
        }

        return null;
    }

    private View getFocusedControl() {
        if (mProgressBar != null && mProgressBar.isFocused()) return mProgressBar;
        if (mButtonRef != null) {
            View focused = mButtonRef.getFocusedChild();
            if (focused != null) return focused;
        }
        if (mSecondaryButtonRef != null) {
            View focused = mSecondaryButtonRef.getFocusedChild();
            if (focused != null) return focused;
        }
        return null;
    }

    private ViewGroup getControlRow(View focused) {
        if (isInRow(focused, mButtonRef)) return mButtonRef;
        if (isInRow(focused, mSecondaryButtonRef)) return mSecondaryButtonRef;
        return null;
    }

    private boolean isInRow(View focused, ViewGroup row) {
        return row != null && row.indexOfChild(focused) >= 0;
    }

    private boolean focusSibling(ViewGroup row, View focused, int step) {
        int target = row.indexOfChild(focused) + step;
        // The edge of the row is reached, the selection stays where it is
        if (target < 0 || target >= row.getChildCount()) return true;

        View child = row.getChildAt(target);
        if (child == null) return true;

        child.requestFocus();
        mLastFocusedAction = findActionForView(child);
        return true;
    }

    private boolean focusProgressBar() {
        if (mProgressBar == null) return false;
        mProgressBar.requestFocus();
        return true;
    }

    private boolean focusControlRow(ViewGroup row) {
        if (row == null || row.getChildCount() == 0) return false;

        // Restore the action that was focused last in this row
        if (mLastFocusedAction != null) {
            View view = findActionView(mLastFocusedAction);
            if (view != null && row.indexOfChild(view) >= 0) {
                view.requestFocus();
                return true;
            }
        }

        View child = row.getChildAt(0);
        child.requestFocus();
        mLastFocusedAction = findActionForView(child);
        return true;
    }

    public void onCustomActionClicked(Action action, View view) {
        // Handle custom action clicks which require a popup menu
        if (action instanceof CustomAction) {
            ((CustomAction) action).handleClickAction(playbackController, getPlayerAdapter(), getContext(), view);
        }

        if (action == playbackSpeedAction) {
            // Post a callback to calculate the new time, since Exoplayer updates this in an async fashion.
            // This is a hack, we should instead have onPlaybackParametersChanged call out to this
            // class to notify rather than poll. But communication is unidirectional at the moment:
            mHandler.postDelayed(mRefreshEndTime, 5000);  // 5 seconds
        }
    }

    private void setEndTime() {
        if (mEndsText == null || getPlayerAdapter().getDuration() < 1)
            return;
        long msLeft = getPlayerAdapter().getDuration() - getPlayerAdapter().getCurrentPosition();
        long realTimeLeft = round(msLeft / playbackController.getPlaybackSpeed());

        LocalDateTime endTime = LocalDateTime.now().plus(realTimeLeft, ChronoUnit.MILLIS);
        mEndsText.setText(getContext().getString(R.string.lbl_playback_control_ends, DateTimeExtensionsKt.getTimeFormatter(getContext()).format(endTime)));
    }

    private void notifyActionChanged(Action action) {
        ArrayObjectAdapter adapter = primaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
            return;
        }
        adapter = secondaryActionsAdapter;
        if (adapter.indexOf(action) >= 0) {
            adapter.notifyArrayItemRangeChanged(adapter.indexOf(action), 1);
        }
    }

    void setInitialPlaybackDrawable() {
        playPauseAction.setIndex(PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE);
        notifyActionChanged(playPauseAction);
    }

    void invalidatePlaybackControls() {
        if (primaryActionsAdapter.size() > 0)
            primaryActionsAdapter.clear();
        if (secondaryActionsAdapter.size() > 0)
            secondaryActionsAdapter.clear();
        addMediaActions();
    }

    void recordingStateChanged() {
        if (getPlayerAdapter().isRecording()) {
            recordAction.setIndex(RecordAction.INDEX_RECORDING);
        } else {
            recordAction.setIndex(RecordAction.INDEX_INACTIVE);
        }
        notifyActionChanged(recordAction);
    }

    void updatePlayState() {
        playPauseAction.setIndex(isPlaying() ? PlaybackControlsRow.PlayPauseAction.INDEX_PAUSE : PlaybackControlsRow.PlayPauseAction.INDEX_PLAY);
        notifyActionChanged(playPauseAction);
        setEndTime();
        if (!isPlaying()) {
            mHandler.removeCallbacks(mRefreshEndTime);
            mHandler.postDelayed(mRefreshEndTime, 30000);
        } else {
            mHandler.removeCallbacks(mRefreshEndTime);
        }

    }

    public void setInjectedViewsVisibility() {
        if (mButtonRef != null && mButtonRef.getVisibility() != mEndsText.getVisibility())
            mEndsText.setVisibility(mButtonRef.getVisibility());
        mHandler.removeCallbacks(mRefreshViewVisibility);
        mHandler.postDelayed(mRefreshViewVisibility, 100);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            // Let the playback glue handle the key
            if (super.onKey(v, keyCode, event)) return true;
            return false;
        }

        VideoPlayerAdapter playerAdapter = getPlayerAdapter();

        if (playerAdapter.hasSubs() && keyCode == KeyEvent.KEYCODE_CAPTIONS) {
            closedCaptionsAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        if (playerAdapter.hasMultiAudio() && keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK) {
            selectAudioAction.handleClickAction(playbackController, getPlayerAdapter(), getContext(), v);
        }
        return super.onKey(v, keyCode, event);
    }
}
