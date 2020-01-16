package io.sesam.cifs.controller;

import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import org.json.XML;
import io.sesam.cifs.service.CifsClient;
import io.sesam.cifs.service.FileOrDirectoryInfo;
import io.sesam.cifs.service.SesamFileRequestEntity;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
     * May be followed by optional path to list content of a subdirectory of share.
     * <p>
     * Must ends with directory and will throw an error in case of file-name provided.
     * <p>
     * /list/go/ will list content of shared folder "go" * /list/go/Csv2Json content of subfolder Csv2Json etc
     *
     * @param shareName name of share
     * @param sortByField optional sorting key
     * @param request servlet request object
     * @return share content
     * @throws IOException if any IO errors occur
     */
    @RequestMapping(value = {"/list/{share}/**"}, method = {RequestMethod.GET})
    public List<FileOrDirectoryInfo> listShareContent(
            @PathVariable("share") String shareName,
            @RequestParam(name = "sortbyfield", defaultValue = "changetime") String sortByField,
            HttpServletRequest request) throws IOException {

        String path = getSharePathFromRequestPath(request);
        LOG.debug("serving request to path {} on share", path, shareName);
        List<FileOrDirectoryInfo> resultList = this.cifsClient.listShareContent(shareName, path);

        switch (sortByField) {
            case "changetime":
                resultList.sort((obj1, obj2) -> {
                    return (int) (obj1.getChangeTimeWindowsTs() - obj2.getChangeTimeWindowsTs());
                });
                break;
        }
        return resultList;
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
        LOG.debug("serving request to path {} on share", pathToFile, shareName);
        Path downloadFile;
        try ( Session smbSession = cifsClient.getSession();  DiskShare smbShare = (DiskShare) smbSession.connectShare(shareName)) {

            downloadFile = cifsClient.downloadFile(smbShare, pathToFile);
            LOG.debug("downloaded file {} of size {}", downloadFile.toAbsolutePath(),
                    FileUtils.byteCountToDisplaySize(
                            FileUtils.sizeOf(downloadFile.toFile())));
            try ( InputStream fileIo = Files.newInputStream(downloadFile)) {
                response.addHeader("Content-disposition", "attachment;filename=" + downloadFile.getFileName());
                response.setContentType("application/octet-stream");

                IOUtils.copy(fileIo, response.getOutputStream());
                response.flushBuffer();
                LOG.debug("sent response with file {}", downloadFile.getFileName());
            } catch (java.lang.OutOfMemoryError exc) {
                long heapSize = Runtime.getRuntime().totalMemory();
                long heapMaxSize = Runtime.getRuntime().maxMemory();
                long heapFreeSize = Runtime.getRuntime().freeMemory();
                LOG.error("Out of memory occured", exc);
                LOG.warn("current heap size: {}", heapSize);
                LOG.warn("max heap size: {}", heapMaxSize);
                LOG.warn("free memory: {}", heapFreeSize);
            } finally {
                if (Files.exists(downloadFile)) {
                    LOG.debug("delete file {}", downloadFile.getFileName());
                    Files.delete(downloadFile);
                }
            }
        }
        if (Files.exists(downloadFile.getParent())
                && Files.isDirectory(downloadFile.getParent())
                && downloadFile.getParent().toFile().list().length > 0) {
            throw new RuntimeException("temporary file storage is not empty after request");
        }

    }

    /**
     * Endpoint to download XML iDoc's and convert them into JSON and return back as JSON array Uses as a HTTP transform
     * in Sesam appliance
     *
     * @param fileList list of json entities from sesam
     * @param response streamed json data
     * @param shareName SMB share where iDocs are stored
     * @throws IOException
     */
    @RequestMapping(value = {"/loadandtransform/{share}"}, method = {RequestMethod.POST})
    public void transformXmlToJson(
            @RequestBody List<SesamFileRequestEntity> fileList,
            HttpServletResponse response, @PathVariable("share") String shareName) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();
        writer.print('[');

        boolean isFirst = true;
        try ( Session smbSession = cifsClient.getSession();  DiskShare smbShare = (DiskShare) smbSession.connectShare(shareName)) {

            for (SesamFileRequestEntity sharedFile : fileList) {
                if (!isFirst) {
                    writer.print(",");
                }
                isFirst = false;

                Path downloadFile = cifsClient.downloadFile(smbShare,
                        String.format("%s%s", sharedFile.getPath(), sharedFile.getFileName()));

                LOG.debug("downloaded file {} of size {}", downloadFile.toAbsolutePath(),
                        FileUtils.byteCountToDisplaySize(
                                FileUtils.sizeOf(downloadFile.toFile())));

                byte[] xmlData;
                try ( FileInputStream xmlInputStream = new FileInputStream(downloadFile.toFile())) {
                    xmlData = new byte[(int) downloadFile.toFile().length()];
                    xmlInputStream.read(xmlData);
                }

                if (Files.exists(downloadFile)) {
                    LOG.debug("delete file {}", downloadFile.getFileName());
                    Files.delete(downloadFile);
                }

                JSONObject xmlJSONObj = XML.toJSONObject(new String(xmlData, "utf-8"));
                LOG.debug(String.format("parsed JSON content from file %s", downloadFile.getFileName().toString()));
                xmlJSONObj.append("source_file_name", downloadFile.getFileName().toString());
                xmlJSONObj.getJSONObject(sharedFile.getXmlRoot()).write(writer);
            }
        }
        writer.print(']');
        writer.flush();
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
