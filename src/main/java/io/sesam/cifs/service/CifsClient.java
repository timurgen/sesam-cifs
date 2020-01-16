package io.sesam.cifs.service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Timur Samkharadze
 */
@Component
public class CifsClient {

    private AuthenticationContext authCt;

    private static final List<String> FILTER_DIRS = Arrays.asList(new String[]{".", ".."});

    @Autowired
    private CifsConfig config;

    public CifsClient() {
    }

    @PostConstruct
    public void initClient() {
        this.authCt = new AuthenticationContext(
                config.getCifsUsername(),
                config.getPassword().toCharArray(),
                config.getDomain()
        );
    }

    /**
     *
     * @param share name of SMB/CIFS share
     * @param path path to target folder in given share
     * @return list with share content information
     * @throws IOException if any IO exception occurs
     */
    public List<FileOrDirectoryInfo> listShareContent(String share, String path) throws IOException {
        List<FileOrDirectoryInfo> result = new ArrayList<>(16);
        try (SMBClient client = new SMBClient(); Connection conn = client.connect(config.getCifsHostname(), config.getPort());
                Session session = conn.authenticate(this.authCt);
                DiskShare connectedShare = (DiskShare) session.connectShare(share)) {

            List<FileIdBothDirectoryInformation> list = connectedShare.list(path);
            list.stream().filter((FileIdBothDirectoryInformation sub) -> {
                return !FILTER_DIRS.contains(sub.getFileName());
            }).forEach((FileIdBothDirectoryInformation sub) -> {
                FileOrDirectoryInfo currentObj = new FileOrDirectoryInfo();
                currentObj.setName(sub.getFileName());
                currentObj.setSize(sub.getAllocationSize());
                currentObj.setChangeTimeWindowsTs(sub.getChangeTime().getWindowsTimeStamp());
                currentObj.setChangeTimeString(sub.getChangeTime().toString());
                if (EnumWithValue.EnumUtils.isSet(sub.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
                    currentObj.setIsDirectory(true);
                }
                result.add(currentObj);
            });

        }
        return result;
    }

    /**
     * Method to obtain authenticated SMB session
     *
     * @return SMB Session object
     * @throws IOException if any IOException occurs
     */
    public Session getSession() throws IOException {
        return new SMBClient().connect(config.getCifsHostname(), config.getPort()).authenticate(this.authCt);
    }

    /**
     * Method to download file from given share
     *
     * This method will delete source file after downloading if CIFS_DELETE_FILE_AFTER_DOWNLOAD config var is equal true
     *
     * @param connectedShare connected SMB disk share
     * @param path path to file
     * @return Path object to locally downloaded file
     * @throws IOException
     */
    public Path downloadFile(DiskShare connectedShare, String path) throws IOException {

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path can't be empty");
        }

        if (!connectedShare.fileExists(path)) {
            throw new IOException(String.format("File %s doesn't exist on remote share", path));
        }

        File sharedFile = connectedShare.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
        );
        Path localPath;
        try ( InputStream shareFileInputStream = sharedFile.getInputStream()) {
            String tempDir = System.getProperty("java.io.tmpdir");
            localPath = Paths.get(tempDir, "java-cifs-service", path);
            Files.createDirectories(localPath.getParent());
            Files.copy(shareFileInputStream, localPath, StandardCopyOption.REPLACE_EXISTING);
            sharedFile.close();
        }
        if (config.isShouldDeleteFileAfterDownload()) {
            connectedShare.rm(path);
        }
        return localPath;

    }

    /**
     * Function to delete file at given path
     *
     * @param connectedShare
     * @param path path to resource on share
     * @throws IOException if any IOException occurs
     */
    public void deleteFile(DiskShare connectedShare, String path) throws IOException {
        if (connectedShare.fileExists(path)) {
            connectedShare.rm(path);
        }
    }

    /**
     * Method to move file from one path to another one (in the same share)
     * @param connectedShare DiskShare object
     * @param path sorce file path
     * @param newPath destination path
     */
    public void moveFile(DiskShare connectedShare, String path, String newPath) {
        if (!connectedShare.fileExists(path)) {
            throw new SMBRuntimeException(String.format("file %s doesn't exist", path));
        }

        File sourceFile = connectedShare.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
        );

        File destinationFile = connectedShare.openFile(
                path,
                EnumSet.of(AccessMask.GENERIC_WRITE),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OVERWRITE_IF,
                null
        );

        try {
            copyFile(sourceFile, destinationFile);
            this.deleteFile(connectedShare, path);
        } catch (IOException ioExc) {
            throw new SMBRuntimeException(
                    String.format("couldn't move file %s to %s due to %s", path, newPath, ioExc.getMessage()), ioExc);
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        byte[] buffer = new byte[65536];
        try ( InputStream in = source.getInputStream()) {
            try ( OutputStream out = destination.getOutputStream()) {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
