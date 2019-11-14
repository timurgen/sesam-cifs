package io.sesam.cifs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMB configuration
 *
 * @author Timur Samkharadze
 */
@ConfigurationProperties
@Component
public class CifsConfig {
    /**
     * hostname where SMB/CIFS share located
     */
    @Value("${CIFS_HOSTNAME:null}")
    private String cifsHostname;
    /**
     * port to connect
     */
    @Value("${CIFS_PORT:445}")
    private int port;
    /**
     * username for authentication
     */
    @Value("${CIFS_USERNAME}")
    private String cifsUsername;
    /**
     * password for authentication
     */
    @Value("${CIFS_PASSWORD:null}")
    private String password;
    /**
     * domain configurated user belong to
     */
    @Value("${CIFS_DOMAIN:WORKGROUP}")
    private String domain;
    /**
     * if downloaded files should be deleted after downloading from share
     */
    @Value("${CIFS_DELETE_FILE_AFTER_DOWNLOAD:false}")
    private boolean shouldDeleteFileAfterDownload;

    public String getCifsHostname() {
        return cifsHostname;
    }

    public void setCifsHostname(String hostname) {
        this.cifsHostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCifsUsername() {
        return cifsUsername;
    }

    public void setCifsUsername(String username) {
        this.cifsUsername = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isShouldDeleteFileAfterDownload() {
        return shouldDeleteFileAfterDownload;
    }

    public void setShouldDeleteFileAfterDownload(boolean shouldDeleteFileAfterDownload) {
        this.shouldDeleteFileAfterDownload = shouldDeleteFileAfterDownload;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return "CifsConfig{"
                + "hostname=" + cifsHostname
                + ", port=" + port
                + ", username=" + cifsUsername
                + ", password=" + password.replaceAll(".", "*")
                + ", delete file after download=" + shouldDeleteFileAfterDownload
                + ", domain=" + domain + '}';
    }

}
