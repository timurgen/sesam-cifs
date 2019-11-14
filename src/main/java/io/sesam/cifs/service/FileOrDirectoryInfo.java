package io.sesam.cifs.service;

/**
 * POJO containing basic info about file/directory
 * @author Timur Samkharadze
 */
public class FileOrDirectoryInfo {
    private String name;
    
    private boolean isDirectory;
    
    private long size;
    
    private long changeTimeWindowsTs;
    
    private String changeTimeString;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIsDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getChangeTimeWindowsTs() {
        return changeTimeWindowsTs;
    }

    public void setChangeTimeWindowsTs(long changeTimeWindowsTs) {
        this.changeTimeWindowsTs = changeTimeWindowsTs;
    }

    public String getChangeTimeString() {
        return changeTimeString;
    }

    public void setChangeTimeString(String changeTimeString) {
        this.changeTimeString = changeTimeString;
    }
    
}
