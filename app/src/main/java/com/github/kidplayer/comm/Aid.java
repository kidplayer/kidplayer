package com.github.kidplayer.comm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.j256.ormlite.dao.Dao;
import com.github.kidplayer.data.CatType;
import com.github.kidplayer.data.Drive;
import com.github.kidplayer.data.Folder;
import com.github.kidplayer.data.VFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class Aid {

    private  final static long minFileSize=5*1024*1024; //5M
    public static List<File> searchFiles(File folder, final String keyword) {

        List<File> result = new ArrayList<File>();
        if (folder.isFile() && folder.length()>=minFileSize )
            result.add(folder);

        File[] subFolders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }
                if (file.getName().toLowerCase().matches(keyword)) {
                    return true;
                }
                return false;
            }
        });

        if (subFolders != null) {
            for (File file : subFolders) {
                if (file.isFile() && file.length()>=minFileSize) {
                    result.add(file);
                } else {
                    result.addAll(searchFiles(file, keyword));
                }
            }
        }

        return result;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        fin.close();
        return ret;
    }


    public static void scanAllDrive(RunCron.Period period, ArrayList<Integer> housekeepTypeIdList,
                                    Map<Integer, Boolean> validFoldersMap, Map<String, Boolean> validAidsMap) throws Exception {

        for (Drive root : Utils.getSysAllDriveList()) {

            File rootDir = new File(root.getP());

            File[] aidDirs = rootDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory()&&!pathname.getName().equals("_");
                }
            });

            App.getHelper().getDao(Drive.class).createOrUpdate(root);


            CatType catType = new CatType();
            catType.setStatus("A");
            catType.setJob(period.getId());
            catType.setName("Local");
            catType.setTypeId(10);
            App.getCatTypeDao().createOrUpdate(catType);

            housekeepTypeIdList.add(0);
            for (File aidDir : aidDirs) {
                scanFolder(period,10, root, aidDir, validFoldersMap, validAidsMap);
            }
            File rootFile = new File(rootDir.getAbsolutePath()+"/_");
            if(rootFile.exists()&&rootFile.isDirectory()){
                aidDirs = rootFile.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });

                for (File aidDir : aidDirs) {
                    scanFolder(period,10, root, aidDir, validFoldersMap, validAidsMap);
                }
            }


        }


    }

    public static void scanFolder(RunCron.Period period, Integer typeId, Drive root, File aidDir, Map<Integer, Boolean> validFoldersMap, Map<String, Boolean> validAidsMap) throws Exception {

        if (validAidsMap.get(aidDir.getName()) != null) return;

        String divfile = aidDir.getAbsolutePath() + File.separator + aidDir.getName() + ".dvi";

        List<File> matchFiles = searchFiles(aidDir, ".*\\.(mp4|rmvb|flv|mpeg|avi|mkv)");

        String title = aidDir.getName();
        String coverURL = null;
        String bvid = null;
        int seq = matchFiles.size();
        if (matchFiles.size() > 0) {
            if (new File(divfile).exists()) {
                String content = getStringFromFile(divfile);
                try {
                    JSONObject jsonObj = JSON.parseObject(content);
                    title = (String) jsonObj.get("Title");
                    if (title == null) title = (String) jsonObj.get("title");
                    coverURL = (String) jsonObj.get("CoverURL");
                    bvid = (String) jsonObj.get("Bid");
                    if (bvid == null) bvid = (String) jsonObj.get("bvid");
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }
        } else return;

        Collections.sort(matchFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {

                String n1 = f1.getName();
                String n2 = f2.getName();
                int maxLen = n1.length() > n2.length() ? n1.length() : n2.length();

                while (n1.length() < maxLen) {
                    n1 = " " + n1;
                }
                while (n2.length() < maxLen) {
                    n2 = " " + n2;
                }
                return n1.compareTo(n2);

            }
        });

        Dao<Folder, Integer> folderDao = App.getHelper().getDao(Folder.class);

        Folder folder = folderDao.queryBuilder().where().eq("aid", aidDir.getName()).and().eq("root_id", root.getId()).queryForFirst();

        if (folder == null) {

            folder = new Folder();
            folder.setName(title);
            folder.setRoot(root);
            folder.setP(aidDir.getName());
            folder.setCoverUrl(coverURL);
            folder.setAid(aidDir.getName());
            folder.setBvid(bvid);
            folder.setTypeId(typeId);
            folder.setOrderSeq(seq--);
            folder.setJob(period.getId());
            folderDao.createOrUpdate(folder);

        }
        validFoldersMap.put(folder.getId(), true);
        validAidsMap.put(folder.getAid(), true);

        Dao<VFile, Integer> vFileDao = App.getHelper().getDao(VFile.class);

        int orderN = 0;
        for (File file : matchFiles) {
            try {
                String path = file.getAbsolutePath().substring(aidDir.getAbsolutePath().length() + 1);

                VFile vfile = vFileDao.queryBuilder().where().eq("p", path.replaceAll("'", "''")).and()
                        .eq("folder_id", folder.getId()).queryForFirst();

                if (vfile == null) {


                    vfile = new VFile();
                    vfile.setP(path);
                    vfile.setName(file.getName());
                    vfile.setBvid(bvid);
                    vfile.setFolder(folder);

                    try {
                        String num = path.split(File.separator)[0];
                        vfile.setPage(Integer.parseInt(num));
                    }catch (Throwable ee){

                        try {
                            String num = file.getName().replaceAll("\\..+","").replaceAll("[^\\d]","");
                            vfile.setPage(Integer.parseInt(num));
                        }catch (Throwable eee){

                        }

                    }


                }
                vfile.setOrderSeq(orderN++);
                vFileDao.createOrUpdate(vfile);

            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }


}

