package com.github.kidplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.CueGroup;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.model.GSYVideoModel;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoViewBridge;
import com.github.kidplayer.comm.App;
import com.github.kidplayer.comm.SSLSocketClient;
import com.github.kidplayer.data.VFile;
import com.github.kidplayer.exo.MyExo2ListPlayerView;
import com.github.kidplayer.exo.MyExo2MediaPlayer;
import com.github.kidplayer.exo.MyExo2PlayerManager;
import com.github.kidplayer.exo.MyExo2VideoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class GsyTvVideoView extends MyExo2ListPlayerView implements Player.Listener {
    private boolean releaseCompleted;

    public GsyTvVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        PlayerFactory.setPlayManager(Exo2PlayerManager.class);
        PlayerFactory.setPlayManager(IjkPlayerManager.class);
        //PlayerFactory.setPlayManager(GSYExoPlayerManager.class);

        //  CacheFactory.setCacheManager(ProxyCacheManager.class);
        // CacheFactory.setCacheManager(ExoPlayerCacheManager.class);
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);
        this.mDismissControlTime = 500;


    }

    private SubtitleView mSubtitleView;

    @Override
    protected void init(Context context) {
        super.init(context);
        mSubtitleView = findViewById(R.id.sub_title_view);
        mSubtitleView.setUserDefaultStyle();
        mSubtitleView.setUserDefaultTextSize();


        View maskConView = findViewById(R.id.maskCon);
        View maskView = findViewById(R.id.mask);


        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;

        PlayerController.getInstance().setMaskViews(maskConView,maskView,mScreenHeight,mScreenWidth);


        //mSubtitleView.setStyle(new CaptionStyleCompat(Color.RED, Color.TRANSPARENT, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, CaptionStyleCompat.EDGE_TYPE_NONE, null));
        // mSubtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override

            public void run() {

                try {
                    checkAndSkipFoot();
                }catch (Throwable e){
                    e.printStackTrace();
                }

                handler.postDelayed(this, 15*1000);

            }

        };
        handler.post(runnable);


    }

    public void checkAndSkipFoot(){

        if(PlayerController.getInstance().getCurItem()==null)return;
        int typeId = PlayerController.getInstance().getCurItem().getFolder().getTypeId();
        if(typeId >= 500 && typeId <600  ){
            long t=GsyTvVideoView.this.getDuration() - GsyTvVideoView.this.getCurrentPosition();
            if( t<60000)
                PlayerController.getInstance().next();
            else if(GsyTvVideoView.this.getCurrentPosition()<30000){
                PlayerController.getInstance().seekTo(31000);
            }
        }
    }
    @Override
    protected void startDismissControlViewTimer() {
        super.startDismissControlViewTimer();
        mPostDismiss = false;
    }

    @Override
    protected void changeUiToPlayingShow() {
        super.changeUiToPlayingShow();
        setViewShowState(mBottomProgressBar, GONE);
    }

    @Override
    protected void hideAllWidget() {
        super.hideAllWidget();
        setViewShowState(mBottomProgressBar, GONE);
    }

    private static boolean isAcronym(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLowerCase(c)) {
                return false;
            }
        }
        return true;
    }

    int cueCount = 0;

    @Override
    public void onCues(CueGroup cueGroup) {
        if (mSubtitleView != null) {
            mSubtitleView.setCues(cueGroup.cues);
            for (Cue cue : cueGroup.cues) {
                if (cue.text != null &&
                        !"".equals(cue.text.toString().trim()) &&
                        isAcronym(cue.text.toString())) {
                    cueCount++;
                } else cueCount = 0;
            }
            if (cueCount > 3)
                mSubtitleView.setVisibility(GONE);
            else mSubtitleView.setVisibility(VISIBLE);

        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_layout_subtitle;
    }

    @Override
    public void onPrepared() {
        super.onPrepared();
        if (getGSYVideoManager().getPlayer().getMediaPlayer() instanceof MyExo2MediaPlayer) {
            ((MyExo2MediaPlayer) (getGSYVideoManager().getPlayer().getMediaPlayer())).addCutesListener(this);
            mSubtitleView.setVisibility(GONE);
        } else {
            mSubtitleView.setVisibility(GONE);

        }
    }


    @Override
    public void onAutoCompletion() {


       // PlayerController.getInstance().incPlayCount();
        //PlayerController.getInstance().setCurIndex(PlayerController.getInstance().getCurIndex() + 1);

      //  if (getGSYVideoManager() instanceof MyExo2VideoManager) {
      //      mPlayPosition++;
      //  } else {
          //  playNext();
            PlayerController.getInstance().next();
      //  }
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        if (!releaseCompleted) PlayerController.getInstance().next();
         releaseCompleted = false;
    }

    @Override
    public void onError(int what, int extra) {
        //super.onError(what, extra);
        Toast.makeText(App.getInstance().getApplicationContext(), "播放出错", Toast.LENGTH_SHORT).show();

        boolean retry=false;
        if(PlayerController.getInstance().getCurItem().getFolder()!=null
                && PlayerController.getInstance().getCurItem().getdLink()!=null
                && PlayerController.getInstance().getCurItem().getFolder().getTypeId()>=500
                &&PlayerController.getInstance().getCurItem().getdLink().indexOf("new.iskcd.com")==-1

        ){
            PlayerController.getInstance().getCurItem().setdLink(
                    PlayerController.getInstance().getCurItem().getdLink()
                            .replaceAll("(http[s]://).*?/","$1new.iskcd.com/")
            );
            if(PlayerController.getInstance().getCurItem().getdLink().indexOf("news.iskcd.com")>-1){
                retry=true;
            }
        }

        if(retry){
            setVideoURI(null,PlayerController.getInstance().getCurItem(),0);

        }else{
            // if (mPlayPosition < mUriList.size() - 1) {
            //    playNext();
            // } else if(mUriList.size()==1)
            PlayerController.getInstance().markInvalid();
            PlayerController.getInstance().next();
            //   else PlayerController.getInstance().playNextFolder();
        }


    }

    @Override
    public GSYVideoViewBridge getGSYVideoManager() {
        if (PlayerFactory.getPlayManager() instanceof MyExo2PlayerManager) {
            MyExo2VideoManager.instance().initContext(getContext().getApplicationContext());
            return MyExo2VideoManager.instance();
        } else {
            GSYVideoManager.instance().initContext(getContext().getApplicationContext());
            return GSYVideoManager.instance();
        }

    }
    //Handler mHandler = new Handler();

    public void setVideoURI(Uri videoUrl, VFile res, int fi) {

        //releaseVideos();
        mPauseBeforePrepared = false;
        getGSYVideoManager().pause();
        if (res.getFolder().getTypeId() >= 500 && res.getFolder().getTypeId() < 600) {
            PlayerFactory.setPlayManager(IjkPlayerManager.class);
            // GSYVideoManager.onPause();
            //  GSYExoVideoManager.onPause();
        } else {
            PlayerFactory.setPlayManager(MyExo2PlayerManager.class);
            //  GSYVideoManager.onPause();

        }

         setUpUrls(res, fi);

        releaseCompleted = true;
        startPlayLogic();
        releaseCompleted = false;






    }

    private GSYVideoModel createModel(VFile res) {
        return new GSYVideoModel(
                res.getFolder().getTypeId() >= 500 && res.getFolder().getTypeId() < 600 ?
                        SSLSocketClient.ServerManager.getServerHttpAddress() + "/api/vFileUrl.m3u8?id=" + res.getId() :
                        res.getdLink() != null ? App.getUrl(res.getdLink()) :

                                (SSLSocketClient.ServerManager.getServerHttpAddress() + "/api/vFileUrl.mp4?id=" + res.getId())

                , (res.getName()!=null?res.getName():res.getFolder().getName()) + "(" +PlayerController.getInstance().getCurIndex()+ ")");
    }

    private void setUpUrls(VFile res, int fi) {
        Map header = new HashMap<>();

        header.put("allowCrossProtocolRedirects", "true");
        setMapHeadData(header);

        VFile[] files = res.getFolder().getFiles().toArray(new VFile[]{});
        int curIndex = fi;


        List<GSYVideoModel> urls = new ArrayList<>();

        if (res.getFolder().getTypeId() == 400)
            for (int i = curIndex; i < files.length; i++) {
                urls.add(

                        createModel(files[i]));
            }
        else {
            urls.add(createModel(res));
        }

        setMapHeadData(header);

        setUp(urls, 0, null, header);
    }

    @Override
    protected void startButtonLogic() {
        if (mVideoAllCallBack != null && (mCurrentState == CURRENT_STATE_NORMAL
                || mCurrentState == CURRENT_STATE_AUTO_COMPLETE)) {
            Debuger.printfLog("onClickStartIcon");
            mVideoAllCallBack.onClickStartIcon(mOriginUrl, mTitle, this);
        } else if (mVideoAllCallBack != null) {
            Debuger.printfLog("onClickStartError");
            mVideoAllCallBack.onClickStartError(mOriginUrl, mTitle, this);
        }
        startPlayLogic();
    }


    @Override
    public void startPlayLogic() {
        super.startPlayLogic();
        /*
        if (mVideoAllCallBack != null) {
            Debuger.printfLog("onClickStartThumb");
            mVideoAllCallBack.onClickStartThumb(mOriginUrl, mTitle, this);
        }
        // boolean hasPrepare = getGSYVideoManager().listener()
        // if(!hasPrepare)
        super.prepareVideo();
        // else
        //  prepareDatasources();

        // mHadPrepared=true;
        getGSYVideoManager().start();
        ;


        startDismissControlViewTimer();*/
    }

    private boolean playing;

    public boolean isPlaying() {

        updateHandler.post(() -> {
            playing = this.getCurrentState() == CURRENT_STATE_PLAYING;
        });
        return playing;

    }

    private long seekTime;

    @Override
    public void seekTo(long time) {
        seekTime = time;
        updateHandler.postDelayed(() -> {
            super.seekTo(seekTime);
        }, 0);

    }

    private Handler updateHandler = new Handler(Looper.getMainLooper());
    private long currentPosition;

    private long duration;

    public long getDuration() {
        updateHandler.post(() -> {
            duration = super.getDuration();
        });
        return duration;
    }

    public long getCurrentPosition() {

        updateHandler.post(() -> {
            currentPosition = super.getCurrentPositionWhenPlaying();
        });
        return currentPosition;
    }

    public void onPause() {
        updateHandler.post(() -> {
            super.onVideoPause();

            // super.pauseLogic(null, true);
        });
    }

    public void start() {
        onResume();
    }

    public void onResume() {
        updateHandler.post(() -> {
            onVideoResume();
            getGSYVideoManager().start();
        });
    }

    public void release() {
        //super.release();
        super.releaseVideos();
    }

    public boolean onKeyDown(int keyCode,KeyEvent event) {

        switch (keyCode){
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(this.getCurrentState() == CURRENT_STATE_PLAYING){
                    onPause();
                }else{
                    onVideoResume();
                }
                break;

            default:
                super.onKeyDown(keyCode,event);
        }

        return false;
    }
}
