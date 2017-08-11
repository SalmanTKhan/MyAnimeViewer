package com.taskdesignsinc.android.myanimeviewer.util;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Comparator;

public final class FileUtils {

    public static final String DEBUG_TAG = FileUtils.class.getSimpleName();

    private static final int BUFFER_SIZE = 8 * 1024; // 8 KB

    private FileUtils() {
    }

    //sorts based on the files name
    public static class SortByFileName implements Comparator<File> {
        AlphanumComparator mInteralComparator = new AlphanumComparator();

        @Override
        public int compare(File f1, File f2) {
            // Sort alphabetically by lower case, which is much cleaner
            return mInteralComparator.compare(f1.getName().toLowerCase(), f2.getName().toLowerCase());
        }
    }

    //sorts based on a file or folder. folders will be listed first
    public static class SortByFolder implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (f1.isDirectory() == f2.isDirectory())
                return 0;
            else if (f1.isDirectory() && !f2.isDirectory())
                return -1;
            else
                return 1;
        }
    }

    public static String getFileExtension(String fileName) {
        String[] splitExtension = fileName.split("\\.");
        if (splitExtension.length > 1) {
            String extension = splitExtension[splitExtension.length - 1];
            return extension.toLowerCase();
        } else {
            return "";
        }
    }

    public static String getFileName(String filePath) {
        String[] split = filePath.split("/");
        if (split.length > 1) {
            String fileName = split[split.length - 1];
            return fileName;
        } else {
            return "";
        }
    }

    public static String dirSize(String path) {

        File dir = new File(path);

        if (dir.exists()) {
            long bytes = getFolderSize(dir);
            if (bytes < 1024) return bytes + " B";
            int exp = (int) (Math.log(bytes) / Math.log(1024));
            String pre = ("KMGTPE").charAt(exp - 1) + "";

            return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
        }

        return "0";
    }

    public static long getFolderSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                if (fileList[i].isDirectory()) {
                    result += getFolderSize(fileList[i]);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }

    public static long getFolderCount(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory())
                    result++;
            }
            return result; // return the file size
        }
        return 0;
    }

    public static boolean isReadable(String ext) {
        return "avi".equalsIgnoreCase(ext) || "mp4".equalsIgnoreCase(ext);
    }

    public static boolean isFont(String fileName) {
        String extension = getExtension(fileName);
        return "ttf".equalsIgnoreCase(extension);
    }

    public static boolean isImageUrl(String url) {
        String extension = URLConnection.guessContentTypeFromName(url);
        return "jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension) || "png".equalsIgnoreCase(extension) || "gif".equalsIgnoreCase(extension) || "bmp".equalsIgnoreCase(extension);
    }

    public static boolean isImage(String ext) {
        String extension = getExtension(ext);
        return "jpg".equalsIgnoreCase(extension) || "jpeg".equalsIgnoreCase(extension) || "png".equalsIgnoreCase(extension) || "gif".equalsIgnoreCase(extension) || "bmp".equalsIgnoreCase(extension);
    }

    public static boolean isVideo(String ext) {
        return "mp4".equalsIgnoreCase(ext) || "avi".equalsIgnoreCase(ext) || "webm".equalsIgnoreCase(ext) || "mpeg".equalsIgnoreCase(ext);
    }

    public static String getExtension(String file) {
        return file.substring(file.lastIndexOf(".")+1);
    }

    public static String stripExtension(String extension, String filename) {
        extension = "." + extension;
        if (filename.endsWith(extension)) {
            return filename.substring(0, filename.length() - extension.length());
        }
        return filename;
    }

    public static boolean validateFileName(String fileName) {
        return fileName.matches("^[^.\\\\/:*?\"<>|]?[^\\\\/:*?\"<>|]*")
                && getValidFileName(fileName).length() > 0;
    }

    public static String getValidFileName(String fileName) {
        String newFileName = fileName.replace(":", "")
                .replace("?", "")
                .replace("\"", "")
                .replace("\\", "")
                .replace("<", "")
                .replace(">", "")
                .replace("\\|", "")
                .replace(":", "")
                .replace(";", "")
                .replace("*", "").replace("/", "-").replaceAll("\\s$", "").trim();
        return newFileName;
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[BUFFER_SIZE];
        while (true) {
            int count = is.read(bytes, 0, BUFFER_SIZE);
            if (count == -1) {
                break;
            }
            os.write(bytes, 0, count);
        }
    }

    public static boolean moveFile(String srcPath, String dstPath) {
        File file = new File(srcPath);
        // Destination directory
        return file.renameTo(new File(dstPath));
    }

    public static void copyDirectory(File sourceLocation, File targetLocation, FilenameFilter pFilternameFilter, FileFilter pFileFilter,
                                     boolean pDeleteSrc)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list(pFilternameFilter);
            for (int i = 0; i < sourceLocation.listFiles(pFileFilter).length; i++) {

                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]), pFilternameFilter, pFileFilter, pDeleteSrc);
            }
            if (pDeleteSrc)
                deleteDirectory(sourceLocation);
        } else {

            InputStream in = new FileInputStream(sourceLocation);

            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }

    }

    /**
     * Overloaded with String path input
     *
     * @param dirPath
     * @return
     */
    public static boolean deleteDirectory(String dirPath) {
        // convert to file handler
        File path = new File(dirPath);
        return deleteDirectory(path);
    }

    /**
     * This function will recursively delete directories and files.
     *
     * @param path File or Directory to be deleted
     * @return true indicates success.
     */
    public static boolean deleteDirectory(File path) {

        // check if path exists
        if (path == null)
            return false;
        if (path.exists()) {

            // is directory
            if (path.isDirectory()) {

                // get list of file in directory
                File[] files = path.listFiles();
                if (files != null)
                    for (int i = 0; i < files.length; i++) {

                        // if it a directory make recursive call
                        // to deleteFile method
                        if (files[i].isDirectory()) {
                            deleteDirectory(files[i]);
                        } else {
                            // delete actual file
                            files[i].delete();
                        }
                    }
            }
        }
        // finally delete the path
        return (path.delete());
    }

    public static void deleteAsync(String... pPaths) {
        if (pPaths == null || pPaths.length == 0)
            return;
        AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... params) {
                String lPath = null;
                for (int i = 0; i < params.length; i++) {
                    lPath = params[i];
                    if (TextUtils.isEmpty(lPath))
                        continue;
                    deleteDirectory(lPath);
                }
                return null;
            }

        };
        AsyncTaskUtils.executeAsyncTask(task, pPaths);
    }

    public static boolean checkMD5(String md5, File updateFile) {
        if (md5 == null || md5.equals("") || updateFile == null || !updateFile.exists() || updateFile.isDirectory()) {
            Log.e(DEBUG_TAG, "MD5 String NULL or UpdateFile NULL");
            return false;
        }

        String calculatedDigest = calculateMD5(updateFile);
        if (calculatedDigest == null) {
            Log.e(DEBUG_TAG, "calculatedDigest NULL");
            return false;
        }
        Log.i(DEBUG_TAG, "checkMD5 for " + updateFile.getName());
        Log.i(DEBUG_TAG, "Calculated digest: " + calculatedDigest);
        Log.i(DEBUG_TAG, "Provided digest: " + md5);

        return calculatedDigest.equalsIgnoreCase(md5);
    }

    public static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(DEBUG_TAG, "Exception while getting Digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            Log.e(DEBUG_TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            WriteLog.appendLog("Unable to process file for MD5, removing file");
            WriteLog.appendLog(Log.getStackTraceString(e));
            updateFile.delete();
            return "";
            //throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal;
    }

    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static File getDisplayImage(String pPath) {
        File dir = new File(pPath);
        if (!dir.isDirectory())
            return dir;
        File[] file = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String fileName) {
                String ext = FileUtils.getFileExtension(fileName);
                return FileUtils.isImage(ext);
            }
        });
        if (file != null) {
            for (File f : file) {
                if (f.getName().contains("cover")) {
                    return f;
                }
            }
            if (file.length > 0)
                return file[0];
        }
        return null;
    }

    public static File findParentWithName(File file, String parentName) {
        if (file != null) {
            if (file.getParentFile() != null) {
                if (file.getParentFile().getName().equals(parentName)) {
                    return file;
                } else {
                    return findParentWithName(file.getParentFile(), parentName);
                }
            }
        }
        return null;
    }

    public static class AlphanumComparator implements Comparator
    {
        private final boolean isDigit(char ch)
        {
            return ch >= 48 && ch <= 57;
        }

        /** Length of string is passed in for improved efficiency (only need to calculate it once) **/
        private final String getChunk(String s, int slength, int marker)
        {
            StringBuilder chunk = new StringBuilder();
            char c = s.charAt(marker);
            chunk.append(c);
            marker++;
            if (isDigit(c))
            {
                while (marker < slength)
                {
                    c = s.charAt(marker);
                    if (!isDigit(c))
                        break;
                    chunk.append(c);
                    marker++;
                }
            } else
            {
                while (marker < slength)
                {
                    c = s.charAt(marker);
                    if (isDigit(c))
                        break;
                    chunk.append(c);
                    marker++;
                }
            }
            return chunk.toString();
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            if (!(o1 instanceof String) || !(o2 instanceof String))
            {
                return 0;
            }
            String s1 = (String)o1;
            String s2 = (String)o2;

            int thisMarker = 0;
            int thatMarker = 0;
            int s1Length = s1.length();
            int s2Length = s2.length();

            while (thisMarker < s1Length && thatMarker < s2Length)
            {
                String thisChunk = getChunk(s1, s1Length, thisMarker);
                thisMarker += thisChunk.length();

                String thatChunk = getChunk(s2, s2Length, thatMarker);
                thatMarker += thatChunk.length();

                // If both chunks contain numeric characters, sort them numerically
                int result = 0;
                if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0)))
                {
                    // Simple chunk comparison by length.
                    int thisChunkLength = thisChunk.length();
                    result = thisChunkLength - thatChunk.length();
                    // If equal, the first different number counts
                    if (result == 0)
                    {
                        for (int i = 0; i < thisChunkLength; i++)
                        {
                            result = thisChunk.charAt(i) - thatChunk.charAt(i);
                            if (result != 0)
                            {
                                return result;
                            }
                        }
                    }
                } else
                {
                    result = thisChunk.compareTo(thatChunk);
                }

                if (result != 0)
                    return result;
            }

            return s1Length - s2Length;
        }
    }
}
