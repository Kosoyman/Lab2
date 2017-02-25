import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Maxim Kravchenko
 * Created on 15-Feb-17, last modofied on 19-Feb-17.
 */
class HTTPResponseConstructor {

    private final SimpleDateFormat SDF = new SimpleDateFormat("EEE, d MMM, yyyy hh:mm:ss a z");
    private String inputDir;
    private String header;
    private String statusCode;
    private String pathName;
    private String extension;

    /**
     * Normalizes path to the file
     * @param path - path to the desired file
     */
    HTTPResponseConstructor(String path) {
        inputDir = path;
    }

    /**
     * Constructs http response based on status code and sets the corresponding field
     */
    void setHeader() {

        if (statusCode.equals("200 OK")) {
            File f = new File(getPath());
            header = "HTTP/1.1 " + statusCode + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Content-Type: " + getExtension() +'\n' +
                    "Content-Length: " + getContentLength(f) + '\n' +
                    "Last-Modified: " + getLastModified(f) + '\n' +
                    "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n" +
                    "ETag: \"3f80f-1b6-3e1cb03b\"\n" +
                    "Accept-Ranges: bytes\n" +
                    "Connection: close\n\n";
        } else {
            header = "HTTP/1.1 " + statusCode + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Content-Type: text/html; charset=UTF-8\n" +
                    "Content-Encoding: UTF-8\n" +
                    "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n" +
                    "ETag: \"3f80f-1b6-3e1cb03b\"\n" +
                    "Accept-Ranges: bytes\n" +
                    "Connection: close\n\n";
        }
    }

    /**
     * Gets the header from the corresponding field
     * @return the header
     */
    String getHeader(){
        return header;
    }

    /**
     * Determines the status code by trying to access the specified file
     */
    void setStatusCode() {
        String ok = "200 OK",
                forbidden = "403 Forbidden",
                notFound = "404 Not Found",
                tooLarge = "413 Payload Too Large",
                tooLong = "414 URI Too Long",
                serverErr = "500 Internal Server Error",
                wrongMedia = "415 Unsupported Media Type",
                legalReason = "451 Unavailable For Legal Reasons";

        try {
            File file = new File(getPath());
            boolean exists = file.exists(),
                    hidden = isHidden(),
                    isDirectory = file.isDirectory(),
                    oversized = file.length() > 2000000 ,
                    lengthy = inputDir.length() > 100,
                    unsupported = extension == null,
                    illegal = getPath().equals("http\\resources\\dir2\\TheAnswer.txt");

            if (oversized)
                statusCode = tooLarge;

            else if (lengthy)
                statusCode = tooLong;

            else if(!exists)
                statusCode = notFound;

            else if(hidden)
                statusCode = forbidden;

            else if(isDirectory) {
                //if the file is a directory, check if it contains index html or htm, otherwise return 404 Not Found
                if (new File(getPath(), "index.html").exists()) {
                    inputDir = inputDir + "/index.html";
                    setPath();
                    setExtension();
                    setStatusCode();

                } else if (new File(getPath(), "index.htm").exists()) {
                    inputDir = inputDir + "/index.htm";
                    setPath();
                    setExtension();
                    setStatusCode();
                }
                else
                    statusCode = notFound;
            }

            else if(unsupported)
                statusCode = wrongMedia;

            else if (illegal)
                statusCode = legalReason;

            else
                statusCode = ok;

        } catch (SecurityException e) {
            e.printStackTrace();
            statusCode = serverErr;

        } catch (NullPointerException n) {
            statusCode = serverErr;
        }
    }

    /**
     * Forces a status code without any computation
     * @param forced - the status code that is to be forced
     */
    void forceStatusCode(String forced){
        statusCode = forced;
    }

    /**
     * Gets the status code from the corresponding field
     * @return Status code
     */
    String getStatusCode() {
        return statusCode;
    }

    /**
     * Calculates and returns the current date and time
     * @return String representation of current date with format specified in the fields
     */
    private String getDate() {
        final Date currentTime = new Date();
        SDF.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (SDF.format(currentTime));
    }

    /**
     * Computes and returns the length of the file
     * @param file - the specified file
     * @return String representation of file length in bytes
     */
    private String getContentLength(File file) {
        return String.valueOf(file.length());
    }

    /**
     * Computes and returns the last modified date
     * @param file - the specified file
     * @return String representation of file last modified date in fields
     */
    private String getLastModified(File file) {
        return SDF.format(file.lastModified());
    }

    /**
     * Combines the path to the server folder with the path specified by the client,
     * ensures correct merging of two parts, and correct format (e.g. correct slashes),
     * and sets corresponding field
     */
    void setPath() {
        pathName = Paths.get("http/resources", inputDir).normalize().toString();
    }

    /**
     * Gets path name from the corresponding field
     * @return Path name
     */
    String getPath(){
        return pathName;
    }

    /**
     * Gets file extension from the corresponding field
     * @return String representation of the file extension
     */
    private String getExtension() {
        return extension;
    }

    /** Computes extension of the file that is requested and sets the corresponding field,
     * right now only determines if a file is png image, html/htm, or a txt otherwise sets it null
     */
    void setExtension() {
        String ext = inputDir.substring(inputDir.lastIndexOf(".") + 1, inputDir.length());;
        switch (ext) {
            case "png":
                extension = "image/png";
                break;
            case "html":
            case "htm":
            case "txt":
                extension = "text/html; charset=UTF-8";
                break;
            default:
                extension = null;
                break;
        }

    }

    /**
     * Checks if the client is trying to access the "secret" directory
     * @return boolean isHidden
     */
    private boolean isHidden(){
        return inputDir.contains("secretDir");
    }

}
