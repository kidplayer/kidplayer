package com.github.kidplayer.data;

import com.j256.ormlite.field.DatabaseField;


public class Drive {


    @DatabaseField(generatedId = true)
    int id;
    @DatabaseField(unique = true)
    String p;

    @DatabaseField
    long totalSize;
    @DatabaseField
    long usedSize;

    private boolean isRemoveable;

    public Drive() {
    }

    public Drive(String absolutePath) {
        this.p = absolutePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public boolean isRemoveable() {
        return isRemoveable;
    }

    public void setRemoveable(boolean removeable) {
        isRemoveable = removeable;
    }
}