package com.github.kidplayer.download;


import com.github.kidplayer.comm.App;
import com.github.kidplayer.comm.DocumentsUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.github.kidplayer.download.Constant.FILESEPARATOR;

public class M3u8DownloadProxy {

    private final LinkedBlockingQueue<Runnable> TASKQUEUE;
    private static String M3U8URL = null;

    //要下载的m3u8链接
    private final String DOWNLOADURL;

    //优化内存占用
    private static final BlockingQueue<byte[]> BLOCKING_QUEUE = new LinkedBlockingQueue<>();
    private final int seq;
    private ExecutorService fixedThreadPool;
    private final static ExecutorService fixedThreadPool2 = Executors.newFixedThreadPool(1);

    //线程数
    private int threadCount = 1;

    //重试次数
    private int retryCount = 30;

    //链接连接超时时间（单位：毫秒）
    private long timeoutMillisecond = 100000L;

    //合并后的文件存储目录
    private String dir;


    //已完成ts片段个数
    private int finishedCount = 0;

    //解密算法名称
    private String method;

    //密钥
    private String key = "";

    //密钥字节
    private byte[] keyBytes = new byte[16];

    //key是否为字节
    private boolean isByte = false;

    //IV
    private String iv = "";

    //所有ts片段下载链接
    private LinkedHashSet<String> tsSet = new LinkedHashSet<>();
    private Set<Integer> downloadedSet = new LinkedHashSet<>();
    private Set<Integer> downloadingSet = new LinkedHashSet<>();

    private List<String> LINES = new ArrayList<String>();
    private List<String> STREAM_LINES = new ArrayList<String>();

    //解密后的片段
    private Set<File> finishedFiles = new ConcurrentSkipListSet<>(
            new Comparator<File>() {

                @Override
                public int compare(File o1, File o2) {

                    return Integer.parseInt(o1.getName().replace(".ts", "")) -
                            Integer.parseInt(o2.getName().replace(".ts", ""));

                }
            }
    );

    //已经下载的文件大小
    private BigDecimal downloadBytes = new BigDecimal(0);

    //监听间隔
    private volatile long interval = 0L;

    //自定义请求头
    private Map<String, Object> requestHeaderMap = new HashMap<>();

    private String downloadId;
    private String[] tsArray;

    /**
     * 开始下载视频
     *
     * @return
     * @throws Exception
     */
    public M3u8DownloadProxy start() throws Exception {
        //setThreadCount(30);
        checkField();
        String tsUrl = getTsUrl();
        if (StringUtils.isEmpty(tsUrl))
            Log.i("不需要解密");

        tsArray = tsSet.toArray(new String[]{});
        //startDownload();

        return this;
    }

    public String getM3U8Content(boolean b) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = b ? STREAM_LINES : LINES;
        for (String s : lines) {
            sb.append(s).append("\n");
        }

        return sb.toString();
    }


    /**
     * 合并下载好的ts片段
     */
    public void mergeAllTsToMp4() {

        fixedThreadPool2.submit(new Runnable() {
            @Override
            public void run() {


                File file = new File(targetFilePath());
                if (file.exists()) return;
                try {

                    for (int i = 0; i < tsSet.size(); i++) {
                        File f = getTsFile(i);
                        if (!f.exists()) {
                            Log.i("TS no " + i + " no ready for " + f.getAbsolutePath());
                            return;
                        }
                    }

                    OutputStream fileOutputStream = DocumentsUtils.getAppendOutputStream(App.getInstance().getApplicationContext(), file);
                    byte[] b = new byte[4096];
                    for (int i = 0; i < tsSet.size(); i++) {
                        File f = getTsFile(i);
                        InputStream fileInputStream = App.documentInputStream(f);
                        int len;
                        while ((len = fileInputStream.read(b)) != -1) {
                            fileOutputStream.write(b, 0, len);
                        }
                        fileInputStream.close();
                        Log.i("合并完第 " + i + "/" + tsSet.size());
                        //DocumentsUtils.delete(App.getInstance().getApplicationContext(),f);

                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();

                    deleteTses();


                } catch (Exception e) {
                    e.printStackTrace();
                    DocumentsUtils.delete(App.getInstance().getApplicationContext(), file);
                }

            }
        });
    }

    /**
     * 合并下载好的ts片段
     */
    private void deleteTses() {
        try {

            for (int index : downloadedSet) {
                File f = getTsFile(index);
                if (f.exists())
                    DocumentsUtils.delete(App.getInstance().getApplicationContext(), f);

                f = getTempTsFile(index);
                if (f.exists())
                    DocumentsUtils.delete(App.getInstance().getApplicationContext(), f);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public float getPercent() {
        return new BigDecimal(downloadedSet.size())
                .divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .floatValue();
    }

    private String targetFilePath() {
        return dir + FILESEPARATOR + seq + FILESEPARATOR + seq + ".mp4";
    }

    private File getTsFile(int i) {
        return new File(dir + FILESEPARATOR + seq + FILESEPARATOR + i + ".ts");
    }

    private File getTempTsFile(int i) {
        return new File(dir + FILESEPARATOR + seq + FILESEPARATOR + i + ".xy");
    }
    public Object getTsStream( int i) {

        if(getTsFile(i).exists()){
            return getTsFile(i);

        }

        int count = 1;
        HttpURLConnection httpURLConnection = null;
        String urls=tsArray[i];


        InputStream in = null;

        //重试次数判断
        while (count <= retryCount) {
            try {
                log("downing", i);
                URL url = new URL(urls);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                httpURLConnection.setDoInput(true);
                in = httpURLConnection.getInputStream();

                ByteArrayOutputStream byos = new ByteArrayOutputStream();

                int len=0;
                byte[] bytes = new byte[4096];

                if (!urls.endsWith(".ts")) {
                    len = in.read(bytes);
                    //47 40 11 10
                    for (int k = 0; k < len; k++) {
                        // if (71 === t[r] && 71 === t[r + 188] && 71 === t[r + 376])
                        if (bytes[k] == 71 && bytes[k + 188] == 71&& bytes[k + 376] == 71   ) {
                            //in.reset();
                            //in.skip(k);
                            byos.write(bytes,k,len-k);
                            break;
                        }
                    }
                }

                if(len!=-1)
                while ((len = in.read(bytes)) != -1) {
                    byos.write(bytes,0,len);
                }
                in.close();


                int valLen = urls.endsWith(".ts") ? 0 : byos.size() % 188;
                bytes =  new byte[byos.size() - valLen];
                System.arraycopy(byos.toByteArray(), 0, bytes, 0, bytes.length);

                byos.close();



                byte[] decrypt = decrypt(bytes, bytes.length, key, iv, method);

                final byte[] retBytes =decrypt==null?bytes:decrypt;
                downloadedSet.add(i);


                     if( getTsFile(i).getParentFile().exists())
                    fixedThreadPool2.submit(new Runnable() {
                        @Override
                        public void run() {

                            File tsFile = getTsFile(i);
                            if(tsFile.exists())return;
                            DocumentsUtils.mkdirs(App.getInstance().getApplicationContext(), tsFile.getParentFile());
                            try {
                                OutputStream os = App.getInstance().documentStream(tsFile.getAbsolutePath());
                                os.write(retBytes);
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    });


                return retBytes;

            } catch (Exception e) {
e.printStackTrace();
                if (e instanceof InvalidKeyException || e instanceof InvalidAlgorithmParameterException) {
                    Log.e("解密失败！");
                    break;
                }
                Log.d("第" + count + "获取链接重试！\t" + urls);
                count++;


//                        e.printStackTrace();
            } finally {

                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }

                synchronized (downloadingSet) {
                    if (downloadingSet.contains(i))
                        downloadingSet.remove(i);
                }
            }
        }

        if (count > retryCount)
            throw new M3u8Exception("连接超时！");
        finishedCount++;
        return null;
    }
    /**
     * 开启下载线程
     *
     * @param urls ts片段链接
     * @param i    ts片段序号
     * @return 线程
     */
    private Thread getThread2(String urls, int i) {
        return new Thread(() -> {
            int count = 1;
            HttpURLConnection httpURLConnection = null;

            synchronized (downloadingSet) {
                if (downloadingSet.contains(i)) return;
                downloadingSet.add(i);
            }
            File tsfile = getTsFile(i);

            if (tsfile.exists()) {
                downloadedSet.add(i);
                return;
            }

            File xyfile = getTempTsFile(i);

            OutputStream outputStream2 = null;
            InputStream inputStream1 = null;
            OutputStream outputStream1 = null;
            InputStream inputStream3 = null;
            byte[] bytes = new byte[4096];
            DocumentsUtils.mkdirs(App.getInstance().getApplicationContext(), xyfile.getParentFile());

            //重试次数判断
            while (count <= retryCount) {
                try {
                    log("downing", i);
                    //模拟http请求获取ts片段文件
                    URL url = new URL(urls);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                    for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                        httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                    httpURLConnection.setDoInput(true);
                    inputStream3 = httpURLConnection.getInputStream();

                    if (!urls.endsWith(".ts")) {
                        inputStream3.skip(49);
                    }


                    outputStream2 = App.getInstance().documentStream(xyfile.getAbsolutePath());

                    int len;
                    //将未解密的ts片段写入文件
                    while ((len = inputStream3.read(bytes)) != -1) {
                        outputStream2.write(bytes, 0, len);
                        synchronized (this) {
                            downloadBytes = downloadBytes.add(new BigDecimal(len));
                        }
                    }
                    outputStream2.flush();
                    outputStream2.close();
                    inputStream3.close();
                    inputStream1 = App.getInstance().documentInputStream(xyfile);
                    int available = inputStream1.available();
                    if (!urls.endsWith(".ts")) {
                        available=available-16;
                    }
                    //if (bytes.length < available)
                        bytes = new byte[available];
                    inputStream1.read(bytes);

                    inputStream1.close();

                    //outputStream1 = DocumentsUtils.getOutputStream(App.getInstance().getApplicationContext(),tsfile);
                    outputStream1 = App.getInstance().documentStream(tsfile.getAbsolutePath());

                    //开始解密ts片段，这里我们把ts后缀改为了xyz，改不改都一样
                    byte[] decrypt = decrypt(bytes, available, key, iv, method);
                    if (decrypt == null)
                        outputStream1.write(bytes, 0, available);
                    else outputStream1.write(decrypt);

                    outputStream1.close();
                    log("Downed", i);
                    finishedFiles.add(tsfile);
                    downloadedSet.add(i);
                    // DocumentsUtils.delete(App.getInstance().getApplicationContext(),xyfile);
                    log("progress:" + 100.0 * downloadedSet.size() / tsSet.size() + "%", i);

                    break;
                } catch (Exception e) {

                    e.printStackTrace();
                    if (tsfile.exists())
                        DocumentsUtils.delete(App.getInstance().getApplicationContext(), tsfile);
                    if (xyfile.exists())
                        DocumentsUtils.delete(App.getInstance().getApplicationContext(), xyfile);
                    if (e instanceof InvalidKeyException || e instanceof InvalidAlgorithmParameterException) {
                        Log.e("解密失败！");
                        break;
                    }
                    Log.d("第" + count + "获取链接重试！\t" + urls);
                    count++;

                    if (e instanceof InterruptedException) {
                        return;
                    }
//                        e.printStackTrace();
                } finally {
                    try {
                        if (inputStream1 != null)
                            inputStream1.close();
                    } catch (Exception e) {

                    }
                    try {
                        if (inputStream3 != null)
                            inputStream3.close();
                    } catch (Exception e) {

                    }
                    try {
                        if (outputStream1 != null)
                            outputStream1.close();
                    } catch (Exception e) {

                    }
                    try {
                        if (outputStream2 != null)
                            outputStream2.close();
                    } catch (Exception e) {

                    }

                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }

                    synchronized (downloadingSet) {
                        if (downloadingSet.contains(i))
                            downloadingSet.remove(i);
                    }
                }
            }

            if (count > retryCount)
                throw new M3u8Exception("连接超时！");
            finishedCount++;
        });
    }


    /**
     * 获取所有的ts片段下载链接
     *
     * @return 链接是否被加密，null为非加密
     */
    private String getTsUrl() {
        StringBuilder content = getUrlContent(DOWNLOADURL, false);
        //判断是否是m3u8链接
        if (!content.toString().contains("#EXTM3U"))
            throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接！");
        String[] split = content.toString().split("\\n");
        String keyUrl = "";
        boolean isKey = false;

        List<String> lines = content.indexOf("EXT-X-STREAM-INF") > -1 ? STREAM_LINES : LINES;
        for (String s : split) {
            //如果含有此字段，则说明只有一层m3u8链接
            if (s.contains("#EXT-X-KEY") || s.contains("#EXTINF")) {
                isKey = true;
                keyUrl = DOWNLOADURL;
                break;
            } else
                //如果含有此字段，则说明ts片段链接需要从第二个m3u8链接获取
                if (s.contains(".m3u8")) {
                    if (StringUtils.isUrl(s))
                        return s;
                    String relativeUrl = DOWNLOADURL.substring(0, DOWNLOADURL.lastIndexOf("/") + 1);
                    if (s.startsWith("/"))
                        s = s.replaceFirst("/", "");
                    keyUrl = mergeUrl(relativeUrl, s);
                    M3U8URL = keyUrl;

                    lines.add("/api/s/" + downloadId + "/hls.m3u8" );

                    break;
                } else
                    lines.add(s);
        }
        if (StringUtils.isEmpty(keyUrl))
            throw new M3u8Exception("未发现key链接！");
        //获取密钥
        String key1 = isKey ? getKey(keyUrl, content) : getKey(keyUrl, null);
        if (StringUtils.isNotEmpty(key1))
            key = key1;
        else key = null;
        return key;
    }

    /**
     * 获取ts解密的密钥，并把ts片段加入set集合
     *
     * @param url     密钥链接，如果无密钥的m3u8，则此字段可为空
     * @param content 内容，如果有密钥，则此字段可以为空
     * @return ts是否需要解密，null为不解密
     */
    private String getKey(String url, StringBuilder content) {
        StringBuilder urlContent;
        if (content == null || StringUtils.isEmpty(content.toString()))
            urlContent = getUrlContent(url, false);
        else urlContent = content;
        if (!urlContent.toString().contains("#EXTM3U"))
            throw new M3u8Exception(DOWNLOADURL + "不是m3u8链接！");
        String[] split = urlContent.toString().split("\\n");
        for (String s : split) {
            //如果含有此字段，则获取加密算法以及获取密钥的链接
            if (s.contains("EXT-X-KEY")) {
                String[] split1 = s.split(",");
                for (String s1 : split1) {
                    if (s1.contains("METHOD")) {
                        method = s1.split("=", 2)[1];
                        continue;
                    }
                    if (s1.contains("URI")) {
                        key = s1.split("=", 2)[1];
                        continue;
                    }
                    if (s1.contains("URI"))
                        iv = s1.split("=", 2)[1];
                }
            }
        }
        String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
        //将ts片段链接加入set集合
        int tsi = 0;
        for (int i = 0; i < split.length; i++) {
            String s = split[i];
            if (s.contains("#EXTINF")) {
                String s1 = split[++i];
                String tsUrl = StringUtils.isUrl(s1) ? s1 : mergeUrl(relativeUrl, s1);
                tsSet.add(tsUrl);

                LINES.add(s);
                LINES.add("/api/rts/" + URLEncoder.encode(downloadId) + "/" + tsi + "/a.ts");
                tsi++;
            } else {
                if (s.contains("EXT-X-KEY") || s.contains("METHOD") || s.contains("URI") || s.contains("URI")) {

                } else LINES.add(s);
            }
        }
        if (!StringUtils.isEmpty(key)) {
            key = key.replace("\"", "");
            return getUrlContent(StringUtils.isUrl(key) ? key : mergeUrl(relativeUrl, key), true).toString().replaceAll("\\s+", "");
        }
        return null;
    }

    /**
     * 模拟http请求获取内容
     *
     * @param urls  http链接
     * @param isKey 这个url链接是否用于获取key
     * @return 内容
     */
    private StringBuilder getUrlContent(String urls, boolean isKey) {
        int count = 1;
        HttpURLConnection httpURLConnection = null;
        StringBuilder content = new StringBuilder();
        while (count <= retryCount) {
            try {
                URL url = new URL(urls);


                httpURLConnection = (HttpURLConnection) url.openConnection();
                //httpURLConnection.setConnectTimeout((int) timeoutMillisecond);
                //httpURLConnection.setReadTimeout((int) timeoutMillisecond);
                httpURLConnection.setUseCaches(false);
                httpURLConnection.setDoInput(true);

                for (Map.Entry<String, Object> entry : requestHeaderMap.entrySet())
                    httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue().toString());
                String line;
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                if (isKey) {
                    byte[] bytes = new byte[128];
                    int len;
                    len = inputStream.read(bytes);
                    isByte = true;
                    if (len == 1 << 4) {
                        keyBytes = Arrays.copyOf(bytes, 16);
                        content.append("isByte");
                    } else
                        content.append(new String(Arrays.copyOf(bytes, len)));
                    return content;
                }
                while ((line = bufferedReader.readLine()) != null)
                    content.append(line).append("\n");
                bufferedReader.close();
                inputStream.close();
                //Log.i(content);
                break;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("第" + count + "获取链接重试！\t" + urls);
                count++;
//                    e.printStackTrace();
            } finally {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        }
        if (count > retryCount)
            throw new M3u8Exception("连接超时！");
        return content;
    }

    /**
     * 解密ts
     *
     * @param sSrc   ts文件字节数组
     * @param length
     * @param sKey   密钥
     * @return 解密后的字节数组
     */
    private byte[] decrypt(byte[] sSrc, int length, String sKey, String iv, String method) throws Exception {
        if (StringUtils.isNotEmpty(method) && !method.contains("AES"))
            throw new M3u8Exception("未知的算法！");
        // 判断Key是否正确
        if (StringUtils.isEmpty(sKey))
            return null;
        // 判断Key是否为16位
        if (sKey.length() != 16 && !isByte) {
            throw new M3u8Exception("Key长度不是16位！");
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(isByte ? keyBytes : sKey.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] ivByte;
        if (iv.startsWith("0x"))
            ivByte = StringUtils.hexStringToByteArray(iv.substring(2));
        else ivByte = iv.getBytes();
        if (ivByte.length != 16)
            ivByte = new byte[16];
        //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(sSrc, 0, length);
    }

    /**
     * 字段校验
     */
    private void checkField() {
        // if ("m3u8".compareTo(MediaFormat.getMediaFormat(DOWNLOADURL)) != 0)
        //     throw new M3u8Exception(DOWNLOADURL + "不是一个完整m3u8链接！");
        if (threadCount <= 0)
            throw new M3u8Exception("同时下载线程数只能大于0！");
        if (retryCount < 0)
            throw new M3u8Exception("重试次数不能小于0！");
        if (timeoutMillisecond < 0)
            throw new M3u8Exception("超时时间不能小于0！");
        if (StringUtils.isEmpty(dir))
            throw new M3u8Exception("视频存储目录不能为空！");

        finishedCount = 0;
        method = "";
        key = "";
        isByte = false;
        iv = "";
        tsSet.clear();
        finishedFiles.clear();
        downloadedSet.clear();
        downloadBytes = new BigDecimal(0);
    }

    private String mergeUrl(String start, String end) {
        if (end.startsWith("/"))
            end = end.replaceFirst("/", "");
        int position = 0;
        String subEnd, tempEnd = end;
        while ((position = end.indexOf("/", position)) != -1) {
            subEnd = end.substring(0, position + 1);
            if (start.endsWith(subEnd)) {
                tempEnd = end.replaceFirst(subEnd, "");
                break;
            }
            ++position;
        }
        return start + tempEnd;
    }

    public String getDOWNLOADURL() {
        return DOWNLOADURL;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        if (BLOCKING_QUEUE.size() < threadCount) {
            for (int i = BLOCKING_QUEUE.size(); i < threadCount * Constant.FACTOR; i++) {
                try {
                    BLOCKING_QUEUE.put(new byte[Constant.BYTE_COUNT]);
                } catch (InterruptedException ignored) {
                }
            }
        }
        this.threadCount = threadCount;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getTimeoutMillisecond() {
        return timeoutMillisecond;
    }

    public void setTimeoutMillisecond(long timeoutMillisecond) {
        this.timeoutMillisecond = timeoutMillisecond;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }


    public int getFinishedCount() {
        return finishedCount;
    }

    public void setLogLevel(int level) {
        Log.setLevel(level);
    }

    public Map<String, Object> getRequestHeaderMap() {
        return requestHeaderMap;
    }

    public void addRequestHeaderMap(Map<String, Object> requestHeaderMap) {
        this.requestHeaderMap.putAll(requestHeaderMap);
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }


    public M3u8DownloadProxy(String m3u8Url, String downloadId, String dir, int seq, String fileName) throws Exception {
        this.downloadId = downloadId;
        this.DOWNLOADURL = m3u8Url;
        requestHeaderMap.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.106 Safari/537.36");
        this.TASKQUEUE = new LinkedBlockingQueue<Runnable>();
        this.dir = dir;
        this.seq = seq;


    }

    public void pause() {
        mergeAllTsToMp4();
    }

    public Object downloadIndexTs(int index) {


        log("request", index);
        File tsfile = getTsFile(index);

        if (!tsfile.exists() && !downloadingSet.contains(index) || downloadingSet.size() == 0) {
            File file = new File(targetFilePath());
            if (file.exists()) return file;
            synchronized (fixedThreadPool2) {
                if (fixedThreadPool != null) {
                    fixedThreadPool.shutdownNow();
                    fixedThreadPool = null;
                }
                if (fixedThreadPool == null) fixedThreadPool = Executors.newFixedThreadPool(1);
            }



            /*synchronized (this.TASKQUEUE) {
                this.TASKQUEUE.clear();
            }*/
            for (int i = index; i < tsArray.length; i++) {
                File ts = getTsFile(i);

                if (ts.exists()) this.downloadedSet.add(i);
                if (this.downloadedSet.contains(i)) continue;
                String tsUrl = tsArray[i];
                this.fixedThreadPool.submit(getThread2(tsUrl, i));
            }

            for (int i = 0; i < index; i++) {
                File ts = getTsFile(i);

                if (ts.exists()) this.downloadedSet.add(i);
                if (this.downloadedSet.contains(i)) continue;
                String tsUrl = tsArray[i];
                this.fixedThreadPool.submit(getThread2(tsUrl, i));
            }
            fixedThreadPool.shutdown();

        }

        if (tsfile.exists()) return tsfile;
        else if (tsArray[index].endsWith(".ts")) return tsArray[index];
        else {
            int maxWaitTimes = 100000;
            while (maxWaitTimes > 0) {
                if (tsfile.exists())
                    break;

                try {
                    Thread.sleep(1000 * 3);
                    maxWaitTimes--;
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
            }
            log("Response", index);

//tsfile.exists();
            //getThread2(tsUrl,index).run();
            return tsfile;
        }


    }

    private void log(String request, int index) {
        System.out.println(request + ":" + seq + ":" + index);

    }
}

