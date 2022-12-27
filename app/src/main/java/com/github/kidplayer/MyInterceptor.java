package com.github.kidplayer;

import androidx.annotation.NonNull;

import com.j256.ormlite.dao.Dao;
import com.github.kidplayer.comm.App;
import com.github.kidplayer.comm.Utils;
import com.github.kidplayer.data.CatType;
import com.github.kidplayer.data.Folder;
import com.github.kidplayer.data.VFile;
import com.github.kidplayer.proxy.CacheItem;
import com.github.kidplayer.proxy.MemCacheManager;
import com.github.kidplayer.sync.SyncCenter;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.Resolver;
import com.yanzhenjie.andserver.framework.ExceptionResolver;
import com.yanzhenjie.andserver.framework.body.StreamBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;
import com.yanzhenjie.andserver.http.ResponseBody;
import com.yanzhenjie.andserver.util.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.kidplayer.download.Log;
import com.github.kidplayer.download.M3u8Exception;

@Resolver
public class MyInterceptor implements ExceptionResolver {


    @Override
    public void onResolve(@NonNull HttpRequest request, @NonNull HttpResponse response, @NonNull Throwable e) {
        System.out.println("OK");

        if (request.getURI().contains("/api/pick")) {
            String tmp1 = request.getURI().split("/api/pick/")[1];
            String name = tmp1.split("/", 2)[0];
            String url = tmp1.split("/", 2)[1];
            url = url.replace(":/", "://").replace("///", "//");

            try {
                response.setBody(new StringBody(pick(name, url)));
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            response.setBody(new StringBody("Error"));
            return;
        } else if (request.getURI().indexOf("/api/autoRate") > -1) {
            String url = request.getURI().split("/api/autoRate/")[1];
            url = url.replace(":/", "://");
            try {
                autoRate(request, response, url);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        if ("HEAD".equalsIgnoreCase(request.getMethod().toString())) {
            response.setHeader("content-type", "video/mp2t");
            response.setHeader("access-control-allow-headers", "X-Requested-With");
            response.setHeader("access-control-allow-methods", "POST, GET, OPTIONS");
            response.setHeader("access-control-allow-origin", "*");
            response.setHeader(" Content-Length", "381681");

        }
        if (request.getURI().contains("/api/m3u8proxy")) {
            String url = request.getURI().split("/api/m3u8proxy/")[1];
            try {
                url = url.replace(":/", "://");
                m3u8Proxy(request, response, url);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }else
        if (request.getURI().contains("/api/speech")) {
            String url = request.getURI().split("/api/speech/")[1];
            try {
                url = url.replace(":/", "://");
                sppech2(request, response, url);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }


    }


    String pick(
            @RequestParam(name = "name", required = true) String name,
            @RequestParam(name = "url", required = true) String url
    ) throws IOException, InterruptedException, SQLException {


        Dao<Folder, Integer> folderDao = App.getHelper().getDao(Folder.class);
        Dao<VFile, Integer> vFileDao = App.getHelper().getDao(VFile.class);
        Folder folder = null;

        folder = folderDao.queryBuilder().where().eq("typeId", 1).and().eq("name", name).queryForFirst();

        if (folder == null) {
            folder = new Folder();
            folder.setName(name);
            folder.setTypeId(1);
            folderDao.create(folder);
        }

        int i = 1;
        VFile vfile = vFileDao.queryBuilder().where().eq("folder_id", folder.getId())
                .and().eq("dLink", url).queryForFirst();
        if (vfile == null) {
            vfile = new VFile();
            vfile.setdLink(url);
            vfile.setPage(i++);
            vfile.setFolder(folder);
            vfile.setName("");
            vfile.setTypeId(1);
            vfile.setFolder(folder);
            vFileDao.create(vfile);
        }

        CatType catType = new CatType();
        catType.setStatus("A");
        catType.setName("Manual");
        catType.setTypeId(1);
        App.getCatTypeDao().createOrUpdate(catType);

        SyncCenter.updateScreenTabs();
        PlayerController.getInstance().play(folder, 0);
        return "OK";
    }

    ResponseBody autoRate(
            HttpRequest request, HttpResponse response,
            @RequestParam(name = "url") String url
    ) throws Exception {

        int count = 1;
        int retryCount = 3;
        HttpURLConnection httpURLConnection = null;
        long timeoutMillisecond = 10000L;

        HashMap<String, String> requestHeaderMap = new HashMap<String, String>();
        requestHeaderMap.put("User-Agent", Utils.AGENT);
        String contentType = null;


        InputStream in = null;

        //重试次数判断
        while (count <= retryCount) {
            try {
                httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                for (Map.Entry<String, String> entry : requestHeaderMap.entrySet())
                    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                httpURLConnection.setDoInput(true);

                contentType = httpURLConnection.getHeaderField("content-type");
                in = httpURLConnection.getInputStream();

                ByteArrayOutputStream byos = new ByteArrayOutputStream();
                int len = 0;
                byte[] bytes = new byte[8192];
                while ((len = in.read(bytes)) != -1) {
                    byos.write(bytes, 0, len);
                }
                in.close();

                bytes = new byte[byos.size()];
                System.arraycopy(byos.toByteArray(), 0, bytes, 0, bytes.length);
                byos.close();

                if (contentType.toLowerCase().contains("mpegurl")) {
                    String content = new String(bytes);
                    if (content.indexOf("RESOLUTION=1920x1080") > -1) {
                        String[] lines = content.split("\n");
                        StringBuilder sb = new StringBuilder();

                        for (int n = 0; n < lines.length; n++) {
                            String line = lines[n];
                            if (line.startsWith("#EXT-X-STREAM-INF")) {
                                if (line.indexOf("RESOLUTION=1920x1080") == -1) {
                                    n++;
                                    continue;
                                }
                                sb.append(line).append("\n");
                                String absUrl = "";
                                line = lines[++n];
                                if (line.startsWith("/")) {
                                    absUrl = url.substring(0, url.indexOf('/', 9)) + line;
                                } else if (line.matches("^(http|https)://.+")) {
                                    absUrl = line;
                                } else {
                                    absUrl = url.substring(0, url.lastIndexOf("/") + 1) + line;
                                }
                                sb.append(absUrl);
                                sb.append("\n");
                            } else {
                                sb.append(line);
                                sb.append("\n");
                            }


                        }
                        bytes = sb.toString().getBytes();
                    } else {
                        response.sendRedirect(url);
                        return null;
                    }

                }

                StreamBody responseBody = new StreamBody(new ByteArrayInputStream(bytes), bytes.length, MediaType.parseMediaType(contentType));

                response.setBody(responseBody);
                return responseBody;

            } catch (Exception e) {
                e.printStackTrace();

                Log.d("第" + count + "获取链接重试！\t" + url);
                count++;

            } finally {

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }
        }

        if (count > retryCount)
            throw new M3u8Exception("连接超时！");
        return null;
    }


    ResponseBody sppech2(
            HttpRequest request, HttpResponse response,
            @RequestParam(name = "url") String url
    ) throws Exception {

        int count = 1;
        int retryCount = 3;
        HttpURLConnection httpURLConnection = null;
        long timeoutMillisecond = 100000L;

        HashMap<String, String> requestHeaderMap = new HashMap<String, String>();
        requestHeaderMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.106 Safari/537.36");
        String contentType = null;


        InputStream in = null;
        if (url.indexOf("m3u8") == -1) {
            CacheItem item = MemCacheManager.curTsUrl(url);
            if (item != null) {

                Map<String, List<String>> hfs = item.headers;
                for (String name : hfs.keySet()) {
                    if (name != null && !name.equalsIgnoreCase("content-length")) {
                        response.setHeader(name, hfs.get(name).get(0));
                    }
                }

                StreamBody responseBody = new StreamBody(new ByteArrayInputStream(item.data), item.data.length, MediaType.parseMediaType(item.contentType));

                response.setBody(responseBody);
                return responseBody;

            }
            return null;
        }
        //重试次数判断
        while (count <= retryCount) {
            try {
                httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                for (Map.Entry<String, String> entry : requestHeaderMap.entrySet())
                    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                httpURLConnection.setDoInput(true);

                contentType = httpURLConnection.getHeaderField("content-type");
                in = httpURLConnection.getInputStream();

                ByteArrayOutputStream byos = new ByteArrayOutputStream();
                int len = 0;
                byte[] bytes = new byte[8192];
                while ((len = in.read(bytes)) != -1) {
                    byos.write(bytes, 0, len);
                }
                in.close();

                bytes = new byte[byos.size()];
                System.arraycopy(byos.toByteArray(), 0, bytes, 0, bytes.length);
                byos.close();
                String str = new String(bytes);
                boolean widthproxy = str.contains(".m3u8");
                ArrayList<String> tsUrls = new ArrayList<String>();
                boolean isVod = str.contains("#EXTINF:");
                if (contentType.toLowerCase().contains("mpegurl")) {
                    String[] lines = str.split("\n");
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        if (line.startsWith("#")) {
                            sb.append(line);
                        } else {
                            String absUrl = "";

                            if (line.startsWith("/")) {
                                absUrl = url.substring(0,url.indexOf('/',9)) + line;
                            } else if (line.matches("^(http|https)://.+")) {
                                absUrl = line;
                            } else {
                                absUrl = url.substring(0, url.lastIndexOf("/") + 1) + line;
                            }
                            if (widthproxy|| str.contains("#EXT-X-ENDLIST"))
                            sb.append("/api/speech/" + absUrl);
                            else sb.append( absUrl);

                            if (isVod) {
                                tsUrls.add(absUrl);
                            }
                        }

                        sb.append("\n");

                    }

                    if (tsUrls.size() > 0) {
                        MemCacheManager.buildIndexFrom(url, tsUrls);
                    }

                    bytes = sb.toString().getBytes();
                }

                Map<String, List<String>> hfs = httpURLConnection.getHeaderFields();
                for (String name : hfs.keySet()) {
                    //if(name!=null && !name.equalsIgnoreCase("content-length")){
                    //   response.setHeader(name, httpURLConnection.getHeaderField(name));
                    //}
                }

                StreamBody responseBody = new StreamBody(new ByteArrayInputStream(bytes), bytes.length, MediaType.parseMediaType(contentType));

                response.setBody(responseBody);
                return responseBody;

            } catch (Exception e) {
                e.printStackTrace();

                Log.d("第" + count + "获取链接重试！\t" + url);
                count++;

            } finally {

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }
        }

        if (count > retryCount)
            throw new M3u8Exception("连接超时！");
        return null;
    }

   // @GetMapping(path = "/api/m3u8proxy")
    ResponseBody m3u8Proxy(
            HttpRequest request, HttpResponse response,
            @RequestParam(name = "url") String url
    ) throws Exception {

        int count = 1;
        int retryCount = 3;
        HttpURLConnection httpURLConnection = null;
        long timeoutMillisecond = 100000L;

        HashMap<String, String> requestHeaderMap = new HashMap<String, String>();
        requestHeaderMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.106 Safari/537.36");
        String contentType = null;


        InputStream in = null;

        //重试次数判断
        while (count <= retryCount) {
            try {
                httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
                httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                for (Map.Entry<String, String> entry : requestHeaderMap.entrySet())
                    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                httpURLConnection.setDoInput(true);

                contentType = httpURLConnection.getHeaderField("content-type");
                in = httpURLConnection.getInputStream();

                ByteArrayOutputStream byos = new ByteArrayOutputStream();
                int len = 0;
                byte[] bytes = new byte[8192];
                while ((len = in.read(bytes)) != -1) {
                    byos.write(bytes, 0, len);
                }
                in.close();

                bytes = new byte[byos.size()];
                System.arraycopy(byos.toByteArray(), 0, bytes, 0, bytes.length);
                byos.close();

                if (contentType.toLowerCase().contains("mpegurl")) {
                    String[] lines = new String(bytes).split("\n");
                    StringBuilder sb = new StringBuilder();
                    for (String line : lines) {
                        if (line.startsWith("#")) {
                            sb.append(line);
                        } else {
                            String absUrl = "";

                            if (line.startsWith("/")) {
                                absUrl = url.substring(0,url.indexOf('/',9)) + line;
                            } else if (line.matches("^(http|https)://.+")) {
                                absUrl = line;
                            } else {
                                absUrl = url.substring(0, url.lastIndexOf("/") + 1) + line;
                            }
                            sb.append("/api/m3u8proxy/" + absUrl);
                        }

                        sb.append("\n");

                    }
                    bytes = sb.toString().getBytes();
                }

                Map<String, List<String>> hfs = httpURLConnection.getHeaderFields();
                for (String name : hfs.keySet()) {
                     //if(name!=null && !name.equalsIgnoreCase("content-length")){
                      //   response.setHeader(name, httpURLConnection.getHeaderField(name));
                     //}
                }

                StreamBody responseBody = new StreamBody(new ByteArrayInputStream(bytes), bytes.length, MediaType.parseMediaType(contentType));

                response.setBody(responseBody);
                return responseBody;

            } catch (Exception e) {
                e.printStackTrace();

                Log.d("第" + count + "获取链接重试！\t" + url);
                count++;

            } finally {

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

            }
        }

        if (count > retryCount)
            throw new M3u8Exception("连接超时！");
        return null;
    }
}
