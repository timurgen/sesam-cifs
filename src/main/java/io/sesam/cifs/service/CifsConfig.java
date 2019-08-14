package io.sesam.cifs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author Timur Samkharadze
 */
@ConfigurationProperties
@Component
public class CifsConfig {

    @Value("${CIFS_HOSTNAME:null}")
    private String cifsHostname;
    
    @Value("${CIFS_PORT:445}")
    private int port;
    
    @Value("${CIFS_USERNAME}")
    private String cifsUsername;
    
    @Value("${CIFS_PASSWORD:null}")
    private String password;
    
    @Value("${CIFS_DOMAIN:WORKGROUP}")
    private String domain;

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
                + ", domain=" + domain + '}';
    }

}
