package io.sesam.cifs.service;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO representing sesam file request used in XML -> JSON transform
 * @author 100tsa
 */
public class SesamFileRequestEntity {

    private String fileName;
    private String path;
    
    @JsonProperty("root")
    private String xmlRoot;

    public String getXmlRoot() {
        return xmlRoot;
    }

    public void setXmlRoot(String xmlRoot) {
        this.xmlRoot = xmlRoot;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
