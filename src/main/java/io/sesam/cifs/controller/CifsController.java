package io.sesam.cifs.controller;

import io.sesam.cifs.service.CifsClient;
import io.sesam.cifs.service.FileOrDirectoryInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 *
 * @author Timur Samkharadze
 */
@RestController
public class CifsController {

    @Autowired
    CifsClient cifsClient;

    /**
     * Endpoint to list share content. May be followed by optional path to list content of a subdirectory of share
     * Must ends with directory an will throw an error in case of file-name provided
     * eg  * /list/go/ will list content of shared folder "go"
     *     * /list/go/Csv2Json content of subfolder Csv2Json etc
     * @param shareName name of share
     * @param request servlet request object
     * @return share content
     * @throws IOException if any IO errors occur
     */
    @RequestMapping(value = {"/list/{share}/**"}, method = {RequestMethod.GET})
    public List<FileOrDirectoryInfo> listShareContent(
            @PathVariable("share") String shareName,
            HttpServletRequest request) throws IOException {

        String path = getSharePathFromRequestPath(request);

        List<FileOrDirectoryInfo> listShareContent = this.cifsClient.listShareContent(shareName, path);
        return listShareContent;
    }

    /**
     * Endpoint to download file from given share and path
     * @param shareName
     * @param request
     * @return file content
     * @throws IOException 
     */
    @RequestMapping(value = {"/get/{share}/**"}, method = {RequestMethod.GET})
    public ResponseEntity<Resource> getFile(
            @PathVariable("share") String shareName,
            HttpServletRequest request) throws IOException {
        String path = getSharePathFromRequestPath(request);
        Path downloadFile = cifsClient.downloadFile(shareName, path);
        HttpHeaders headers = new HttpHeaders();
        
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(downloadFile));
        if(Files.exists(downloadFile)){
            Files.delete(downloadFile);
        }
        return ResponseEntity.ok()
            .headers(headers)
            .contentLength(resource.contentLength())
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .body(resource);
        
    }
    /**
     * Utility function to get CIFS path from request
     * @param request
     * @return 
     */
    private String getSharePathFromRequestPath(HttpServletRequest request) {
        final String path
                = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        final String bestMatchingPattern
                = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        String arguments = new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
        return arguments;
    }

}
