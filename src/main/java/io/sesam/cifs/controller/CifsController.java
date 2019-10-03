package io.sesam.cifs.controller;

import io.sesam.cifs.service.CifsClient;
import io.sesam.cifs.service.FileOrDirectoryInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Provides funcitons to work with CIFS shares
 *
 * @author Timur Samkharadze
 */
@RestController
public class CifsController {

    @Autowired
    CifsClient cifsClient;

    private static final Logger LOG = LoggerFactory.getLogger(CifsController.class);

    public CifsController() {
        long heapSize = Runtime.getRuntime().totalMemory();
        long heapMaxSize = Runtime.getRuntime().maxMemory();
        long heapFreeSize = Runtime.getRuntime().freeMemory();
        LOG.debug("current heap size: {}", heapSize);
        LOG.debug("max heap size: {}", heapMaxSize);
        LOG.debug("free memory: {}", heapFreeSize);
    }

    /**
     * Endpoint to list share content.
     * <p>
     * May be followed by optional path to list content of a subdirectory of
     * share.
     * <p>
     * Must ends with directory and will throw an error in case of file-name
     * provided.
     * <p>
     * /list/go/ will list content of shared folder "go" * /list/go/Csv2Json
     * content of subfolder Csv2Json etc
     *
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

        return this.cifsClient.listShareContent(shareName, path);
    }

    /**
     * Endpoint to download file from given share and path
     *
     * @param shareName share name
     * @param request HttpServletRequest object
     * @param response stream file content
     * @throws IOException
     */
    @RequestMapping(value = {"/get/{share}/**"}, method = {RequestMethod.GET})
    public void getFile(
            @PathVariable("share") String shareName,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        String pathToFile = getSharePathFromRequestPath(request);
        LOG.debug("serving request {}", pathToFile);
        Path downloadFile = cifsClient.downloadFile(shareName, pathToFile);
        LOG.debug("downloaded file {} of size {}", downloadFile.getFileName(),
                 FileUtils.byteCountToDisplaySize(
                        FileUtils.sizeOf(downloadFile.toFile())));
        try ( InputStream fileIo = Files.newInputStream(downloadFile)) {
            response.addHeader("Content-disposition", "attachment;filename=" + downloadFile.getFileName());
            response.setContentType("application/octet-stream");

            IOUtils.copy(fileIo, response.getOutputStream());
            response.flushBuffer();
        } catch (java.lang.OutOfMemoryError exc) {
            long heapSize = Runtime.getRuntime().totalMemory();
            long heapMaxSize = Runtime.getRuntime().maxMemory();
            long heapFreeSize = Runtime.getRuntime().freeMemory();
            LOG.error("Out of memory occured", exc);
            LOG.warn("current heap size: {}", heapSize);
            LOG.warn("max heap size: {}", heapMaxSize);
            LOG.warn("free memory: {}", heapFreeSize);
        }

        if (Files.exists(downloadFile)) {
            LOG.debug("delete file {}", downloadFile.getFileName());
            Files.delete(downloadFile);
        }

    }

    /**
     * Utility function to get CIFS path from request
     *
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
