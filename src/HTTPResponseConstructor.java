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

    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM, yyyy hh:mm:ss a z");
    private String inputDir;
    private String header;
    private String statusCode;
    private String pathName;

    /**
     * Normalizes path to the file, computes the status code for it and construct the HTTP header
     * @param path - path to the desired file
     */
    HTTPResponseConstructor(String path) {
        inputDir = path;
        setPath();
        setStatusCode();
        setHeader();
    }

    /**
     * Constructs http response based on status code and sets the corresponding field
     */
    private void setHeader() {

        if (statusCode.equals("200 OK")) {
            File f = new File(getPath());
            header = "HTTP/1.1 " + statusCode + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Content-Type: " + getExtension() +'\n' +
                    //"Content-Encoding: UTF-8\n" +
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
    private void setStatusCode() {
        String ok = "200 OK",
                forbidden = "403 Forbidden",
                notFound = "404 Not Found",
                serverErr = "500 Internal Server Error";

        try {
            File file = new File(getPath());
            boolean exists = file.exists(),
                    hidden = isHidden(),
                    isDirectory = file.isDirectory();

            if (hidden)
                statusCode = forbidden;

            else if (!exists)
                statusCode = notFound;

            else if (isDirectory) {
                //if the file is a directory, check if it contains index html or htm, otherwise return 404 Not Found
                if (new File(getPath(), "index.html").exists()) {
                    inputDir = inputDir + "/index.html";
                    setPath();
                    setStatusCode();

                } else if (new File(getPath(), "index.htm").exists()) {
                    inputDir = inputDir + "/index.htm";
                    setPath();
                    setStatusCode();
                }
                else
                    statusCode = notFound;

            } else
                statusCode = ok;

        } catch (SecurityException e) {
            e.printStackTrace();
            statusCode = serverErr;

        } catch (NullPointerException n) {
            statusCode = forbidden;
        }
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
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (sdf.format(currentTime));
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
        return sdf.format(file.lastModified());
    }

    /**
     * Combines the path to the server folder with the path specified by the client,
     * ensures correct merging of two parts, and correct format (e.g. correct slashes),
     * and sets corresponding field
     */
    private void setPath() {
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
     * Computes extension of the file that is requested, right now only determines if a file is png image, otherwise
     * considers it a text
     * @return String representation of the file extension
     */
    private String getExtension() {

        String extension = inputDir.substring(inputDir.lastIndexOf(".") + 1, inputDir.length());

        if (extension.equals("png"))
            return "image/png";

        else
            return "text/html; charset=UTF-8";
    }

    /**
     * Checks if the client is trying to access the "secret" directory
     * @return boolean isHidden
     */
    private boolean isHidden(){
        return inputDir.contains("secretDir");
    }
}
