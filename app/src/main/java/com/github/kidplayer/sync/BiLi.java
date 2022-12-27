package com.github.kidplayer.sync;

import static com.github.kidplayer.sync.SyncCenter.updateScreenTabs;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.j256.ormlite.dao.Dao;
import com.github.kidplayer.comm.App;
import com.github.kidplayer.comm.RunCron;
import com.github.kidplayer.comm.SSLSocketClient;
import com.github.kidplayer.comm.Utils;
import com.github.kidplayer.data.CatType;
import com.github.kidplayer.data.Folder;
import com.github.kidplayer.data.VFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BiLi {

    private static V8ScriptEngine v8scriptEngine;


    public static String join(String s, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            sb.append(v).append(s);
        }
        return sb.toString();
    }

    private static JSONObject requestData(V8ScriptEngine scriptEngine, String link, OkHttpClient okHttpClient,
                                          List<String> cookies, MediaType JSON) throws IOException {

        for (int k = 0; k < 10; k++) {


            Request request;
            Call call;
            Response response;
            JSONObject json = new JSONObject();

            String e = (String) scriptEngine.eval("Math.random().toString(10).substring(2)");

            String i = (String) scriptEngine.eval("tool.cal('" + link + "'+'@'+" + e + ").toString(10)");

            String phead = (String) scriptEngine.eval("tool.uc('" + link + "'," + "'bilibili')");
            String elink = (String) scriptEngine.eval("tool.encode('" + link + "')");
            json.put("link", elink + "@" + e + "@" + i);

            RequestBody requestBody = RequestBody.create(JSON, String.valueOf(json));

            cookies.add(scriptEngine.eval("_0x5c74a7(-0x27b, -0x284, -0x292, -0x28d)") + "=1");
            String cookieStr = join(";", cookies);
            System.out.println("sendcookie:" + cookieStr);

            request = new Request.Builder().url("https://bilibili.iiilab.com/media")

                    .addHeader("Origin", "https://bilibili.iiilab.com/")
                    .addHeader("Referer", "https://bilibili.iiilab.com/").addHeader("User-Agent", Utils.AGENT)
                    .addHeader("Cookie", cookieStr)// .addHeader("X-Client-Data", xclientdata)
                    .addHeader("accept-patch", phead).post(requestBody).build();
            call = okHttpClient.newCall(request);
            response = call.execute();
            String rsp = response.body().string();
            System.out.println(rsp);
            JSONObject jsonObj = JSONObject.parseObject(rsp);

            if (jsonObj.getString("data") != null && jsonObj.getIntValue("code") == 200) {

                String decode = (String) scriptEngine.eval("tool.decode('" + jsonObj.getString("data") + "')");
                System.out.println(decode);
                String video = ((JSONObject) JSONObject.parseObject(decode).getJSONArray("medias").get(0)).getString("resource_url");
                JSONObject data = new JSONObject();
                data.put("video", video);
                jsonObj.put("data", data);
                return jsonObj;

            }


        }
        return new JSONObject();
    }

    private static List<String> receiveCookies(OkHttpClient okHttpClient, List<String> cookies, String url,
                                               Map<String, String> headerMap, int methodType, RequestBody requestBody) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).addHeader("User-Agent", Utils.AGENT);
        if (headerMap != null) {
            for (String e : headerMap.keySet()) {
                builder.addHeader(e, headerMap.get(e));
            }
        }
        if (methodType == 1)
            builder.post(requestBody);
        Request request = builder.build();

        Call call = okHttpClient.newCall(request);
        Response response = call.execute();

        for (String cookie : response.headers().values("Set-Cookie")) {
            String value = cookie.split(";")[0];
            if (cookies.indexOf(value) > -1)
                continue;
            cookies.add(value);
            System.out.println(cookie);

        }
        return cookies;
    }

    public static  int errorTimes;
    public static JSONObject getVidoInfo(String bvid, Integer p) {

        if(errorTimes>20)return null;
        V8ScriptEngine v8scriptEngine2 = new V8ScriptEngine();

        try {
            //if(v8scriptEngine==null) {

            //  V8ScriptEngine scriptEngine=new V8ScriptEngine();
            InputStream fd1 = App.getInstance().getApplicationContext().getAssets().open("crypto.js");
            InputStream fd2 = App.getInstance().getApplicationContext().getAssets().open("time2.js");


            //scriptEngine.eval(new FileReader(path + "/md5.js"));

            // scriptEngine.eval(new InputStreamReader(fd));
            v8scriptEngine2.eval(fd1);
            v8scriptEngine2.eval(fd2);
            //}


            JSONObject info = getVideoInfo(v8scriptEngine2, "https://www.bilibili.com/video/" + bvid + "?p=" + p + "&spm_id_from=pageDriver");


            info = info.getJSONObject("data");

            if(info==null || info.getString("video")==null) errorTimes++;
            else errorTimes=0;
            return info;
            //return   getResouceData("https://www.bi libili.com/video/" + bvid + "?p=" + p + "&spm_id_from=pageDriver");

        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            try {
                if (v8scriptEngine2 != null) v8scriptEngine2.release();
            } catch (Throwable ee) {
                ee.printStackTrace();
            }
        }
        errorTimes++;
        return null;
    }


    public static JSONObject getVideoInfo(V8ScriptEngine scriptEngine, String link) throws IOException {


        OkHttpClient okHttpClient = new OkHttpClient().newBuilder().connectTimeout(10, TimeUnit.SECONDS)// 设置连接超时时间
                .readTimeout(30, TimeUnit.SECONDS)// 设置读取超时时间
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())// 配置
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier()).build();

        Request request;
        Call call;
        Response response;
        List<String> cookies = new ArrayList<String>();
        receiveCookies(okHttpClient, cookies, "https://bilibili.iiilab.com/", null, 0, null);

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("cookie", join(";", cookies));
        headers.put("origin", "https://bilibili.iiilab.com/");
        headers.put("referer", "https://bilibili.iiilab.com/");

        MediaType JSON = MediaType.parse("application/json;charset=utf-8");

        receiveCookies(okHttpClient, cookies, "https://bilibili.iiilab.com/sponsor", headers, 1,
                RequestBody.create(JSON, String.valueOf("")));

        JSONObject jsonObj = requestData(scriptEngine, link, okHttpClient, cookies, JSON);

        return jsonObj;


    }


    public static void bilibiliVideos(RunCron.Period srcPeriod, final int startTypeId,
                                      ArrayList<Integer> houseKeepTypeIdList,
                                      Dao<Folder, Integer> folderDao,
                                      Dao<VFile, Integer> vFileDao,
                                      Map<Integer, Boolean> validFoldersMap,
                                      Map<String, Boolean> validAidsMap,
                                      String uid
    ) throws IOException, SQLException {

        //uid="1218167447";
        //uid="358543891";
        String resp = Utils.get("https://api.bilibili.com/x/v3/fav/folder/created/list-all?up_mid="+uid+"&jsonp=jsonp");
        JSONObject jsonObj = JSONObject.parseObject(resp);


        JSONArray list = (JSONArray) ((JSONObject) (jsonObj.get("data"))).get("list");

        Collections.sort(list, (l1, l2) -> {
            JSONObject o1 = (JSONObject) l1;
            JSONObject o2 = (JSONObject) l2;
            return o1.getString("title").compareTo(o2.getString("title"));
        });


        StringBuilder sbSearch = new StringBuilder();
        for (int i = 0, d = 0; i < list.size(); i++) {
            JSONObject item = (JSONObject) list.get(i);

            String catName = item.getString("title");
            if (catName.startsWith("@")) {

                if (catName.startsWith("@@")) {
                    sbSearch.append(catName).append("\n");
                }
                continue;
            }

            Integer btypeId = d + startTypeId;
            Integer media_count = (Integer) item.get("media_count");

        if(media_count > 0)
            if ( catName.indexOf("_") == -1) {
                CatType catType = new CatType();
                catType.setStatus("A");
                catType.setJob(srcPeriod.getId());
                catType.setName(catName);
                catType.setTypeId(btypeId);
                App.getCatTypeDao().createOrUpdate(catType);
               // typesMap.put(catName, btypeId);
                d++;
                houseKeepTypeIdList.add(btypeId);

            }

            System.out.println("**目录 ：" + catName + " count:" + media_count);

            int orderSeq = media_count;

            int pn = 1;

            do {

                    resp = Utils.get("https://api.bilibili.com/x/v3/fav/resource/list?media_id=" + item.get("id") + "&pn=" + pn + "&ps=20&keyword=&order=mtime&type=0&tid=0&platform=web&jsonp=jsonp");

                jsonObj = JSONObject.parseObject(resp);
                JSONArray medias = (JSONArray) ((JSONObject) jsonObj.get("data")).get("medias");

                if (medias == null) break;
                for (int j = 0; j < medias.size(); j++) {
                    JSONObject media = ((JSONObject) medias.get(j));
                    String folderTitle = media.getString("title");
                    String title = media.getString("title");
                    String aid = "" + media.getInteger("id");
                    String bvid = media.getString("bvid");
                    String cover = media.getString("cover");
                    int pages = media.getInteger("page");
                    System.out.println(folderTitle);

                    if (folderTitle == null || folderTitle.indexOf("失效") > -1) continue;

                    Folder folder;
                    if (catName.indexOf("_") > -1 ) {
                        CatType catType = App.getCatTypeDao().queryBuilder().where().eq("name", catName.split("_")[0]).queryForFirst();
                        if(catType!=null){

                            btypeId = catType.getTypeId();
                            aid=folderTitle = catName.split("_")[1];
                        }

                    }

                    folder = folderDao.queryBuilder().where().eq("aid", aid).queryForFirst();

                    if (folder == null) {

                        folder = new Folder();


                    }

                    folder.setJob(srcPeriod.getId());
                    folder.setBvid(bvid);
                    folder.setAid(aid);
                    folder.setOrderSeq(orderSeq);
                    folder.setTypeId(btypeId);
                    folder.setName(folderTitle);
                    folder.setCoverUrl(cover);

                    folderDao.createOrUpdate(folder);


                    orderSeq--;

                    validFoldersMap.put(folder.getId(), true);
                    validAidsMap.put(aid, true);

                    for (int k = 1; k <= pages; k++) {

                        VFile vfile = vFileDao.queryBuilder().where()/*.eq("folder_id", folder.getId())
                                .and()*/.eq("bvid", bvid).and().eq("page", k).queryForFirst();
                        if (vfile == null) {
                            vfile = new VFile();
                        }
                        vfile.setFolder(folder);
                        vfile.setBvid(bvid);
                        vfile.setAid("" + aid);
                        vfile.setPage(k);
                        vfile.setOrderSeq(k);
                        vfile.setName(title);
                        vFileDao.createOrUpdate(vfile);


                    }


                }

                if (pn * 20 > media_count){
                    updateScreenTabs();
                    break;
                }
                pn++;

            } while (true);
        }

        SharedPreferences sp = App.getInstance().getApplicationContext().getSharedPreferences("SP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("search", sbSearch.toString().trim());
        editor.apply();
        sp.edit().commit();
    }

    public static void bilibiliVideosSearchByKeyWord(RunCron.Period srcPeriod, final int startTypeId, ArrayList<Integer> housekeepTypeIdList,
                                                     Dao<Folder, Integer> folderDao,
                                                     Dao<VFile, Integer> vFileDao, Map<Integer, Boolean> validFoldersMap,
                                                     Map<String, Boolean> validAidsMap) throws IOException, SQLException {


        SharedPreferences sp = App.getInstance().getApplicationContext().getSharedPreferences("SP", Context.MODE_PRIVATE);
        String search = sp.getString("search", "");
        if (search == null || search.equals("")) return;
        int iiii = 0;
        for (String line : search.split("\n")) {
            String[] names = line.replaceAll("@@", "").split("\\|");
            String name = names[0];

            for (int e = names.length > 1 ? 1 : 0; e < names.length; e++) {
                String keyword = names[e];

                int orderSeq = 10000;

                int pn = 1;
                int total = 0;
                int typeId = ++iiii + startTypeId;

                CatType catType = new CatType();
                catType.setStatus("A");
                catType.setJob(srcPeriod.getId());
                catType.setName(name);
                catType.setTypeId(typeId);
                App.getCatTypeDao().createOrUpdate(catType);

                housekeepTypeIdList.add(typeId);
                do {
                    String resp = Utils.get("https://api.bilibili.com/x/web-interface/search/type?page=1&page_size=50&latform=pc&keyword="
                            // + "%E8%8B%B1%E6%96%87%E5%84%BF%E6%AD%8C"
                            + URLEncoder.encode(keyword, "UTF-8") +
                            "&category_id=&search_type=video");
                    JSONObject jsonObj = JSONObject.parseObject(resp);
                    JSONArray medias = (JSONArray) ((JSONObject) jsonObj.get("data")).get("result");
                    total = ((JSONObject) jsonObj.get("data")).getIntValue("numResults");

                    if (medias == null) break;
                    for (int j = 0; j < medias.size(); j++) {
                        JSONObject media = ((JSONObject) medias.get(j));
                        String title = media.getString("title");
                        Integer aid = media.getInteger("id");
                        String bvid = media.getString("bvid");
                        String cover = "http:" + media.getString("pic");
                        // int pages = media.getInteger("page");
                        System.out.println(title);

                        if (title == null || title.indexOf("失效") > -1) continue;
                        title = title.replaceAll("<.*?>", "");
                        Folder folder;

                        folder = folderDao.queryBuilder().where().eq("aid", aid).and().eq("typeId", typeId).queryForFirst();

                        if (folder == null) {
                            folder = new Folder();
                        }
                        folder.setJob(srcPeriod.getId());

                        folder.setName(title);

                        folder.setCoverUrl(cover);
                        folder.setTypeId(typeId);
                        folder.setOrderSeq(orderSeq);
                        folder.setAid("" + aid);
                        folder.setBvid(bvid);
                        folderDao.createOrUpdate(folder);

                        orderSeq--;

                        validFoldersMap.put(folder.getId(), true);
                        validAidsMap.put(aid.toString(), true);

                        for (int k = 1; k <= 1; k++) {

                            VFile vfile = vFileDao.queryBuilder().where()/*.eq("folder_id", folder.getId())*/
                                    .and().eq("bvid", bvid).and().eq("page", k).queryForFirst();
                            if (vfile == null) {
                                vfile = new VFile();
                            }
                            vfile.setFolder(folder);
                            vfile.setBvid(bvid);
                            vfile.setAid("" + aid);
                            vfile.setPage(k);
                            vfile.setOrderSeq(k);
                            vFileDao.createOrUpdate(vfile);

                        }


                    }

                    if (pn * 50 > total || pn * 50 > 400) break;
                    pn++;

                } while (true);

            }

        }


    }
}
