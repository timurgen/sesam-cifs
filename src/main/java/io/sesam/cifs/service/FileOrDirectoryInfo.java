package io.sesam.cifs.service;

/**
 * POJO containing basic info about file/directory
 * @author Timur Samkharadze
 */
public class FileOrDirectoryInfo {
    private String name;
    
    private boolean isDirectory;

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
    
    
}
