package com.github.kidplayer.exo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.exo2.ExoSourceManager;
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import tv.danmaku.ijk.media.exo2.demo.EventLogger;

/**
 * 自定义exo player，实现不同于库的exo 无缝切换效果
 */
public class MyExo2MediaPlayer extends IjkExo2MediaPlayer {

    private static final String TAG = "GSYExo2MediaPlayer";

    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;

    private final Timeline.Window window = new Timeline.Window();

    public static final int POSITION_DISCONTINUITY = 899;

    private int playIndex = 0;

    public MyExo2MediaPlayer(Context context) {
        super(context);
        this.mHeaders = new HashMap<>();
        mHeaders.put("allowCrossProtocolRedirects", "true");

        this.mExoHelper = ExoSourceManager.newInstance(context, this.mHeaders);
    }
    @Override
    public void onCues(List<Cue> cues) {
        super.onCues(cues);
        /// 这里
    }
    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        notifyOnInfo(POSITION_DISCONTINUITY, reason);
    }

    public void setDataSource(List<String> uris, Map<String, String> headers, int index, boolean cache) {
        mHeaders = headers;
        if (uris == null) {
            return;
        }
        ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource();
        for (String uri : uris) {
            MediaSource mediaSource = mExoHelper.getMediaSource(uri, isPreview, cache, false, mCacheDir, getOverrideExtension());
            concatenatedSource.addMediaSource(mediaSource);
        }
        playIndex = index;
        mMediaSource = concatenatedSource;
    }


    /**
     * 上一集
     */
    public void previous() {
        if (mInternalPlayer == null) {
            return;
        }
        Timeline timeline = mInternalPlayer.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return;
        }
        int windowIndex = mInternalPlayer.getCurrentMediaItemIndex();
        timeline.getWindow(windowIndex, window);
        int previousWindowIndex = mInternalPlayer.getPreviousMediaItemIndex();
        if (previousWindowIndex != C.INDEX_UNSET
            && (mInternalPlayer.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
            || (window.isDynamic && !window.isSeekable))) {
            mInternalPlayer.seekTo(previousWindowIndex, C.TIME_UNSET);
        } else {
            mInternalPlayer.seekTo(0);
        }
    }

    @Override
    protected void prepareAsyncInternal() {
        new Handler(Looper.getMainLooper()).post(
            new Runnable() {
                @Override
                public void run() {
                    if (mTrackSelector == null) {
                        mTrackSelector = new DefaultTrackSelector(mAppContext);
                    }
                    mEventLogger = new EventLogger(mTrackSelector);
                    boolean preferExtensionDecoders = true;
                    boolean useExtensionRenderers = true;//是否开启扩展
                    @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = useExtensionRenderers
                        ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
                    if (mRendererFactory == null) {
                        mRendererFactory = new DefaultRenderersFactory(mAppContext);
                        mRendererFactory.setExtensionRendererMode(extensionRendererMode);
                    }
                    if (mLoadControl == null) {
                        mLoadControl = new DefaultLoadControl();
                    }
                    mInternalPlayer = new ExoPlayer.Builder(mAppContext, mRendererFactory)
                        .setLooper(Looper.getMainLooper())
                        .setTrackSelector(mTrackSelector)
                        .setLoadControl(mLoadControl).build();

                    mInternalPlayer.addListener(MyExo2MediaPlayer.this);
                    mInternalPlayer.addAnalyticsListener(MyExo2MediaPlayer.this);
                    mInternalPlayer.addListener(mEventLogger);
                    if (mSpeedPlaybackParameters != null) {
                        mInternalPlayer.setPlaybackParameters(mSpeedPlaybackParameters);
                    }
                    if (mSurface != null)
                        mInternalPlayer.setVideoSurface(mSurface);
                    ///fix start index
                    if (playIndex > 0) {
                        mInternalPlayer.seekTo(playIndex, C.INDEX_UNSET);
                    }
                    mInternalPlayer.setMediaSource(mMediaSource, false);
                    mInternalPlayer.prepare();
                    mInternalPlayer.setPlayWhenReady(false);
                }
            }
        );
    }

    /**
     * 下一集
     */
    public boolean next() {
        if (mInternalPlayer == null) {
            return false;
        }
        Timeline timeline = mInternalPlayer.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return false;
        }
        int windowIndex = mInternalPlayer.getCurrentMediaItemIndex();
        int nextWindowIndex = mInternalPlayer.getNextMediaItemIndex();
        if (nextWindowIndex != C.INDEX_UNSET) {
            mInternalPlayer.seekTo(nextWindowIndex, C.TIME_UNSET);
            return true;
        } else if (timeline.getWindow(windowIndex, window).isDynamic) {
            mInternalPlayer.seekTo(windowIndex, C.TIME_UNSET);
            return true;
        }
        return false;
    }

    public int getCurrentWindowIndex() {
        if (mInternalPlayer == null) {
            return 0;
        }
        return mInternalPlayer.getCurrentMediaItemIndex();
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        super.onMediaItemTransition(mediaItem, reason);

      //  MyExo2VideoManager.instance().listener().onAutoCompletion();

    }

    public void addCutesListener(Player.Listener lis) {
        if( this.mInternalPlayer!=null)
        this.mInternalPlayer.addListener(lis);
    }
}
