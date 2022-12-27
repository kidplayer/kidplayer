package com.github.kidplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.annotation.JSONField;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.Where;
import com.github.kidplayer.comm.App;
import com.github.kidplayer.data.CatType;
import com.github.kidplayer.data.Folder;
import com.github.kidplayer.data.VFile;
import com.github.kidplayer.view.adapter.FolderCatsListRecycleViewAdapter;
import com.github.kidplayer.view.adapter.FolderListAdapter;
import com.github.kidplayer.view.adapter.FolderNumListRecycleViewAdapter;
import com.github.kidplayer.view.adapter.QtabListRecycleViewAdapter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public final class PlayerController {

    final static int MODE_RANDOM = 1;
    final static int MODE_SEQ = 0;
    final static int MODE_LOOP = 2;
    final static int MODE_KEYWORD = 3;

    private static PlayerController instance;
    private VFile curItem;

    private int mode;

    private GsyTvVideoView videoView;
    private Uri videoUrl;
    private View girdView;
    private int curIndex;
    private String curCat;

    private FolderCatsListRecycleViewAdapter catsAdaper;
    private FolderListAdapter foldersAdapter;
    private FolderNumListRecycleViewAdapter numAdapter;
    //private List<String> cats;
    private int curFocusFolderIndex;
    private int curCatId;

    @JSONField(serialize = false)
    private Map<String, Integer> allTypeMap;

    //@JSONField(serialize = false)
    private LinkedHashMap<Integer, String> typeIdMap;

    @JSONField(serialize = false)
    private Folder curFolder;

    @JSONField(serialize = false)
    private VFile[] numFiles;
    private List<Folder> curCatList;
    private RecyclerView qTabRecyclerView;
    private QtabListRecycleViewAdapter qAdapter;
    private RecyclerView foldersRecyclerView;
    private Timer timerCat =new Timer();
    private Timer timer2 = new Timer();
    private List<CatType> cats;

    private View maskView;
    private View MaskViewCon;
    private int screenWith;
    private int screenHeight;
    private int maskX;
    private int maskY;
    private int maskW;
    private int maskH;

    public int getMaskX() {
        return maskX;
    }

    public int getMaskY() {
        return maskY;
    }

    public int getMaskW() {
        return maskW;
    }

    public int getMaskH() {
        return maskH;
    }

    public boolean isMaskActive(){
        return MaskViewCon !=null && MaskViewCon.getVisibility()==View.VISIBLE;
    }
    public void setMaskXyWH(int sh,int x, int y, int w, int h,float alpha){



        this.maskX=x;
        this.maskY=y;
        this.maskW=w;
        this.maskH=h;

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(sh<=0){
                    MaskViewCon.setVisibility(View.GONE);
                    maskView.setVisibility(View.GONE);
                    return;
                }else{
                    MaskViewCon.setVisibility(View.VISIBLE);
                    maskView.setVisibility(View.VISIBLE);
                }

                maskView.setX(x);
                maskView.setY(y);
                maskView.setAlpha(alpha);
                ViewGroup.LayoutParams lp=  maskView.getLayoutParams();
                lp.width=w;
                lp.height=h;
                maskView.setLayoutParams(lp);
            }
        });

    }

    public int getScreenWith() {
        return screenWith;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    private PlayerController() {
    }

    public  void setMaskViews(View maskConView, View maskView,int h,int w) {
        this.maskView=maskView;
        this.MaskViewCon=maskConView;
        this.screenWith=w;
        this.screenHeight=h;

    }


    public VFile getCurItem(){
        return this.curItem;
    }
    @JSONField(serialize = false)
    public List<Folder> getCurCatList() {
        return this.curCatList;
    }

    public void setCurCatList(List<Folder> curCatList) {
        this.curCatList = curCatList;
    }

    public List<Folder> loadCatFolderList(int typeId){
        List<Folder> ret = null;
        try {
            Where<Folder, ?> where = App.getHelper().getDao(Folder.class).queryBuilder().where();

            if(typeId>0)
            ret= where.eq("typeId",typeId).and().eq("status",0).queryBuilder().orderBy("orderSeq",false).query();
            //local
            else if(typeId==0)  ret= where.eq("isFav",1).queryBuilder().orderBy("orderSeq",false).query();


        } catch (SQLException throwables) {
            throwables.printStackTrace();
            ret = new ArrayList<>();
        }
        return ret;
    }

    public synchronized void refreshCats() {

        try {
            CatType fav = new CatType();
            fav.setTypeId(0);
            fav.setName("Fav");
            fav.setStatus("A");
            fav.setOrderSeq(0);
            App.getCatTypeDao().createOrUpdate(fav);

            this.cats = App.getCatTypeDao().queryBuilder().orderBy("orderSeq",true).where().eq("status","A").query();

            allTypeMap = new LinkedHashMap<>();
            typeIdMap = new LinkedHashMap<>();
            for(CatType cat:cats){
                allTypeMap.put(cat.getName(),cat.getTypeId());
                typeIdMap.put(cat.getTypeId(),cat.getName());
            }

        }catch (Throwable e){
            e.printStackTrace();
        }


        if (catsAdaper != null) {
            catsAdaper.notifyDataSetChanged();
        }

    }

    public void setRVAdapts(FolderCatsListRecycleViewAdapter catsAdaper, FolderListAdapter foldersAdapter, FolderNumListRecycleViewAdapter numAdapter, QtabListRecycleViewAdapter qAdapter) {
        this.catsAdaper = catsAdaper;
        this.foldersAdapter = foldersAdapter;
        this.numAdapter = numAdapter;
        this.qAdapter=qAdapter;
    }
    @JSONField(serialize = false)
    public List<CatType> getCats() {


        return this.cats;

    }


    private  static Context context;
    public static void setContext(Context context){
        PlayerController.context = context;
    }
    public static PlayerController getInstance() {
        if (instance == null) instance = new PlayerController();
        return instance;
    }

    public long getDuration() {

        return videoView == null ? 0 : videoView.getDuration();
    }

    public long getCurrentPosition() {
        return videoView == null ? 0 : videoView.getCurrentPosition();

    }

    public boolean isPlaying() {
        return videoView == null ? false : videoView.isPlaying();

    }

    public void seekTo(int pos) {
        if (videoView != null) videoView.seekTo(pos);

    }

    public void pause() {
        if (videoView != null) videoView.onPause();
    }

    public void start() {
        if (videoView != null) videoView.start();

    }

    public void prepare() {
    }


    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }


    public PlayerController play(VFile res) {

            this.curItem = res;

        Iterator<VFile> it = res.getFolder().getFiles().iterator();
            int i = 0;
            while (it.hasNext()) {
                if (it.next().getId() == res.getId()) {
                    break;
                }
                i++;
            }

        PlayerController.getInstance().setCurIndex(i);

        final int fi = i;
        new Thread(new Runnable() {
            @Override
            public void run() {

                String title = "";
              //  videoUrl = App.getUri(res);
                //videoUrl =Uri.parse("https://prod.vodvideo.cbsnews.com/cbsnews/vr/hls/2022/07/15/2052222019587/1127348_hls/master.m3u8");
                //https:/d2e1asnsl7br7b.cloudfront.net/7782e205e72f43aeb4a48ec97f66ebbe/index.m3u8
               // title = res.getName();

                Handler handler = new Handler(Looper.getMainLooper());
                String finalTitle = title;



                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        synchronized (videoView) {

                            //videoView.pause();
                            videoView.setVideoURI(videoUrl,res,fi);
                            //videoView.resume();

                        }
                    }
                });
            }
        }).start();

        return this;
    }

    private void storePosition(VFile res) {
        SharedPreferences sp = App.getInstance().getApplicationContext().getSharedPreferences("SP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("id", res.getId());
        editor.apply();
        sp.edit().commit();
    }


    public void prev() {
    }

    public VFile playNextFolder() {

        if (curItem == null) {
            return null;
        }
        try {
            Folder folder = null;

            List<Folder> catFolerList = this.getCurCatList();
            int nextPos = this.curFocusFolderIndex + 1;
            if (catFolerList.size() > 0 && catFolerList.size() > nextPos && nextPos >= 0) {
                folder = this.getCurCatList().get(nextPos);
                this.play(folder, nextPos, 0);
                if(folder.getFiles().size()>0)
                     return folder.getFiles().iterator().next();
                folder =null;
            }
            if (folder == null) {
                Dao<Folder, ?> folderDao = App.getHelper().getDao(Folder.class);

                int typeId = curItem.getFolder().getTypeId();
                folder = folderDao.queryBuilder()
                        .where().eq("typeId", typeId)
                        .and().lt("orderSeq", curItem.getOrderSeq())
                        .and().eq("status",0)
                        .queryBuilder()
                        .orderBy("orderSeq", false)
                        .orderBy("id", false)
                        .queryForFirst();
                if (folder == null) {

                    folder = folderDao.queryBuilder()
                            .where().eq("typeId", typeId)
                            .and().eq("status",0)
                            .queryBuilder()
                            .orderBy("orderSeq", false)
                            .orderBy("id", false)
                            .queryForFirst();
                }
            }

            if (folder != null) {
                Iterator<VFile> it = folder.getFiles().iterator();
                if(it!=null) {
                    try {
                        curItem = it.next();
                    }catch (Throwable e){
                        e.printStackTrace();
                    }
                    play(curItem);
                    return curItem;
                }

            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return null;

    }

    public void incPlayCount(){
        if( this.curItem!=null && this.curItem.getId()>0){
            this.curItem.setPlayCnt(this.curItem.getPlayCnt()+1);
            try {

                Dao<VFile, Integer> vFileDao = App.getHelper().getDao(VFile.class);

                vFileDao.createOrUpdate(this.curItem);

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
       ;
    }

    public void markInvalid(){
        if(curItem!=null && curItem.getFolder()!=null && curItem.getFolder().getTypeId()>=500){

            try {

                Dao<Folder, Integer> folderDao = App.getHelper().getDao(Folder.class);
                curItem.getFolder().setStatus(1);
                folderDao.update(curItem.getFolder());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
    }
    public void next() {

        if (mode == MODE_LOOP && curItem != null) {
            play(curItem);
            return;
        }

        try {
            Dao<VFile, Integer> vfDao = App.getHelper().getDao(VFile.class);

            VFile vf = curItem;


            int vfId = vf.getId();

            vf = vfDao.queryForId(vfId);

            VFile nextVf = null;

            if (vf != null) {
                if(vf.getFolder()!=null) {
                    Iterator<VFile> fileListIterator = vf.getFolder().getFiles().iterator();
                    while (fileListIterator.hasNext()) {
                        VFile file = fileListIterator.next();
                        if (file.getId() == vfId) {
                            if (fileListIterator.hasNext()) {
                                nextVf = fileListIterator.next();
                            }
                            break;
                        }
                    }
                }

                if (nextVf != null) {
                    play(nextVf);
                    return;
                }
            }

            playNextFolder();


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    public void playByVFileId(int id) {


        VFile item = null;
        try {
            item = App.getHelper().getDao(VFile.class).queryBuilder().where().eq("id", id).queryForFirst();
            playVFile(item);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void init(long folderId) {

        try {

            if (folderId > 0) {
                Folder folder = null;

                Dao<Folder, Integer> dao = App.getHelper().getDao(Folder.class);
                folder = dao.queryForId((int) folderId);
                if (folder != null) this.curItem = folder.getFiles().iterator().next();


            }
            if (this.curItem == null) {

                Dao<VFile, Integer> vfDao = App.getHelper().getDao(VFile.class);

                SharedPreferences sp = App.getInstance().getApplicationContext().getSharedPreferences("SP", Context.MODE_PRIVATE);

                int id = sp.getInt("id", 1);
                this.curItem = vfDao.queryForId(id);
                if (this.curItem != null) {
                } else {
                    this.curItem = vfDao.queryBuilder().queryForFirst();
                }

            }

            if(this.curItem!=null && this.curItem.getFolder()!=null){
                playVFile(this.curItem);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


    }

    public int getLen(){
        if(this.curItem!=null && this.curItem.getFolder()!=null && this.curItem.getFolder().getFiles()!=null){
            return  this.curItem.getFolder().getFiles().size();
        }
        return 0;
    }
    public void playVFile(VFile vfile) {
        String curCat = this.typeIdMap.get( vfile.getFolder().getTypeId());
        if(curCat==null && this.typeIdMap.values().size()>0){
            curCat = this.typeIdMap.values().iterator().next();
        }
        if(curCat!=null){
            this.setCurCat(curCat);
            int curFolderIndex=0;

            int curfolderId = vfile.getFolder().getId();
            if(this.curCatList==null || this.curCatList.size()==0){
                this.curCatList = loadCatFolderList(vfile.getFolder().getTypeId());
            }

            for(int i=0;i< this.curCatList.size();i++){
                if( this.curCatList.get(i).getId()==curfolderId){
                    curFolderIndex=i;
                    break;
                }
            }

            this.setCurFocusFolderIndex(curFolderIndex);

        }

        this.play(vfile);
    }

    public void setUIs(GsyTvVideoView videoView,
                       View gridView, RecyclerView numTabRecyclerView, RecyclerView qTabRecyclerView, RecyclerView foldersRecyclerView) {
        this.videoView = videoView;
        this.girdView = gridView;
        this.qTabRecyclerView = qTabRecyclerView;
        this.foldersRecyclerView=foldersRecyclerView;
    }

    public String getCoverUrl() {
        if (this.curItem instanceof VFile)
            return ((VFile) (this.curItem)).getFolder().getCoverUrl();
        return "";
    }

    public String getName() {
        if (this.curItem instanceof VFile)
            return ((VFile) (this.curItem)).getFolder().getName();
        return "";
    }


    public PlayerController hideMenu() {
        this.girdView.setVisibility(View.GONE);
        return this;
    }


    public int getCurIndex() {
        return this.curIndex;
    }

    public PlayerController setCurIndex(int i) {
        this.curIndex = i;
        if(this.curItem!=null && this.curItem.getFolder()!=null && this.curItem.getFolder().getFiles()!=null && i<this.curItem.getFolder().getFiles().size()){
            this.curItem=this.curItem.getFolder().getFiles().toArray(new VFile[]{})[i];
            storePosition(curItem);

            if (this.numAdapter != null)
                this.numAdapter.notifyDataSetChanged();
        }

        return this;
    }

    public PlayerController play(Folder folder, int position) {
        if (folder != null) {
            this.setCurIndex(position);
            PlayerController.getInstance().play(folder.getFiles().toArray(new VFile[]{})[position]);
        }
        return this;
    }

    public void setCurCat(String curCat) {

        if(this.curCat!=null&&this.curCat.equals(curCat)){
            return;
        }
        this.curCat = curCat;
        Integer typeId = allTypeMap.get(curCat);
        this.setCurCatId(typeId);


        this.timerCat.cancel();
        this.timerCat = new Timer();

        timerCat.schedule(new TimerTask() {
            @Override
            public void run() {

                List<Folder> catItemList = loadCatFolderList(PlayerController.this.curCatId);
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        PlayerController.this.setCurCatList(catItemList);
                        if(PlayerController.this.curCatList.size()>0)
                            ((HorizontalGridView)PlayerController.this.foldersRecyclerView).setSelectedPosition(0);
                        PlayerController.this.foldersAdapter.notifyDataSetChanged();
                    }
                });


            }
        },500);//延时1s执行

    }

    public Map<String, Integer> getAllTypeMap() {
        return allTypeMap;
    }



    public String getCurCat() {
        return curCat;
    }

    public void play(Folder folder, int position, int i) {
        this.setCurFocusFolderIndex(position);
        if (folder.getFiles().size() > i) {
            this.setCurIndex(i);
            this.play(folder.getFiles().toArray(new VFile[]{})[i]);
        }

    }

    public void setCurFocusFolderIndex(int folderPosition) {
        int p = this.curFocusFolderIndex;
        this.curFocusFolderIndex = folderPosition;
        this.curFolder = curCatList.get(folderPosition);
        this.foldersAdapter.notifyItemChanged(p);
        this.foldersAdapter.notifyItemChanged(folderPosition);
        //this.timer2.purge();
        this.timer2.cancel();
        this.timer2 = new Timer();

        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                VFile[] numFiles = PlayerController.this.curFolder != null ? PlayerController.this.curFolder.getFiles().toArray(new VFile[]{}) : null;

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        PlayerController.this.setNumFiles(numFiles);
                        PlayerController.this.numAdapter.notifyDataSetChanged();
                        qTabRecyclerView.setVisibility(PlayerController.this.curFolder.getTypeId()>=200&& PlayerController.this.curFolder.getTypeId() <300?View.VISIBLE:View.GONE);

                    }
                });
            }
        },500);

    }

    public VFile[] getNumFiles() {
        return numFiles;
    }

    public void setNumFiles(VFile[] numFiles) {
        this.numFiles = numFiles;
    }

    public Folder getCurFolder() {

        if (this.curFolder != null) return this.curFolder;
        return null;
    }

    public PlayerController play(int position) {
        Folder folder = getCurFolder();
        this.play(folder, position);
        return this;
    }

    public boolean isFolderPositionSelected(int position) {

        if (this.curCat != null && this.curItem != null) {
            return getCurCatList().get(position).getId() == this.curItem.getFolder().getId();
        }
        return false;
    }

    public int getCurCatId() {
        return curCatId;
    }

    public void setCurCatId(int curCatId) {
        this.curCatId = curCatId;
    }


    public boolean isNumberSelect(int i) {
        return (this.curItem != null && this.curFolder != null && this.curItem.getFolder().getId() == this.curFolder.getId() && i == this.curIndex);
    }

    public void play() {
        if(this.curItem!=null)
             this.play(this.curItem);
    }

    @JSONField(serialize = false)
    private String[] rates = new String[]{"1080","720","540"};

    private int curRateIndex=0;
    public String[] getRates() {
        return  rates;
    }

    public boolean getRate(int position) {
        return  curRateIndex == position;
    }

    public PlayerController setCurRateIndex(int position) {
        this.curRateIndex = position;
        qAdapter.notifyDataSetChanged();
        this.play();
        return this;
    }

    public String getRate() {
        return rates[curRateIndex];
    }

    public void doFav() {
       this.curFolder.setIsFav(this.curFolder.getIsFav()>0?0:1);
        try {
            App.getHelper().getDao(Folder.class).update(this.curFolder);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        this.foldersAdapter.notifyItemChanged(this.curFocusFolderIndex);

    }


}