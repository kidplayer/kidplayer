package com.github.kidplayer.sync;


import android.os.Handler;
import android.os.Looper;

import com.github.kidplayer.PlayerController;
import com.github.kidplayer.comm.Aid;
import com.github.kidplayer.comm.RunCron;
import com.github.kidplayer.data.Folder;
import com.github.kidplayer.data.VFile;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;


public class SyncCenter {


    public static  void syncData(String id) throws SQLException {


        if (RunCron.peroidMap.size()==0) {


            RunCron.addPeriod(new RunCron.Period("bili", "bili", 3l * 24 * 3600 * 1000, true) {
                @Override
                public void doRun(ArrayList<Integer> houseKeepTypeIdList,
                                  Dao<Folder, Integer> folderDao,
                                  Dao<VFile, Integer> vFileDao, Map<Integer, Boolean> keepFoldersMap,Map<String, Boolean> validAidsMap) throws Throwable {
                    BiLi.bilibiliVideos(this, 100, houseKeepTypeIdList, folderDao,
                            vFileDao, keepFoldersMap, validAidsMap
                            ,"358543891"
                    );
                }
            });

            RunCron.addPeriod(new RunCron.Period("tv", "tv", 15l * 24 * 3600 * 1000, true) {
                @Override
                public void doRun(ArrayList<Integer> houseKeepTypeIdList,
                                  Dao<Folder, Integer> folderDao,
                                  Dao<VFile, Integer> vFileDao, Map<Integer, Boolean> keepFoldersMap,Map<String, Boolean> validAidsMap) throws Throwable {
                    TV.liveStream(this, 300, houseKeepTypeIdList, folderDao, vFileDao, keepFoldersMap);
                }
            });

            RunCron.addPeriod(new RunCron.Period("local", "local", 0, true) {
                @Override
                public void doRun(ArrayList<Integer> houseKeepTypeIdList,
                                  Dao<Folder, Integer> folderDao,
                                  Dao<VFile, Integer> vFileDao, Map<Integer, Boolean> keepFoldersMap,Map<String, Boolean> validAidsMap) throws Throwable {

                    Aid.scanAllDrive(this, houseKeepTypeIdList,  keepFoldersMap, validAidsMap);
                }
            });


        }


        RunCron.addToQueue(id);

        RunCron.startRunTasks();


    }

    public static void updateScreenTabs() {

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                PlayerController.getInstance().refreshCats();
            }
        });
    }




}
