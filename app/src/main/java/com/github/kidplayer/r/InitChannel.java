package com.github.kidplayer.r;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.github.kidplayer.comm.App;
import com.github.kidplayer.data.Folder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InitChannel {

    public InitChannel() {

        initChannels();
    }

    private void initChannels() {


        SharedPreferences perf = PreferenceManager.getDefaultSharedPreferences(App.getInstance().getApplicationContext());

        Integer[] channels = JSON.parseArray(perf.getString("channels", "[]")).toArray(new Integer[0]);

        if(channels.length>0)return;
        for (long ch : channels) {
            MediaTVProvider.deleteChannel(App.getInstance().getApplicationContext(), ch);

        }

        Map<String, Integer> map = App.getStoreTypeMap();

        List<Long> channelList = new ArrayList<>();
        int i = 0;
        for (String key : map.keySet()) {
            Integer val = map.get(key);

            i++;
            int contentId = 0;

            List<MediaProgram> programs = new ArrayList<>();
            List<Folder> list = VideoProvider.getMovieList(val);

            if (list.size() == 0) continue;

            for (Folder p : list) {

                MediaProgram program = new MediaProgram(p.getName(), p.getName(), p.getCoverUrl(), p.getCoverUrl(),
                        "Movie", "" + p.getId(), p.getId(), Integer.toString(contentId++));
                programs.add(program);

            }
            Log.v("TAG", "add channel "+key+" size:"+programs.size());
            MediaChannel channel = new MediaChannel(key, programs, i == 1);


            try{
                long channelId = MediaTVProvider.addChannel(App.getInstance().getApplicationContext(), channel);
                channelList.add(channelId);
            }catch (Exception e){
                e.printStackTrace();
            }

        }


        perf.edit().putString("channels", JSON.toJSONString(channelList)).apply();


    }

}
