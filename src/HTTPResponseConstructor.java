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

    HTTPResponseConstructor(String path) {
        inputDir = path;
        setPath();
        setStatusCode();
        setHeader();
    }

    /* constructs and returns string http response based on status code */
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

    /* returns the header */
    String getHeader(){
        return header;
    }

    /* determines the status code by trying to access the specified file */
    private void setStatusCode() {
        String ok = "200 OK",
                forbidden = "403 Forbidden",
                notFound = "404 Not Found",
                serverErr = "500 Internal Server Error";

        try {
            File file = new File(getPath());
            boolean exists = file.exists(),
                    readable = file.canRead(),
                    isDirectory = file.isDirectory();

            if (!exists)
                statusCode = notFound;

            else if (!readable)
                statusCode = forbidden;

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

    /* returns status code */
    String getStatusCode() {
        return statusCode;
    }

    /*
    returns String representation of current date with format specified in the fields
    TODO: right now returns one hour smaller than current time, fix if time allows
    */
    private String getDate() {
        final Date currentTime = new Date();
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (sdf.format(currentTime));
    }

    /* returns String representation of file length in bytes, needed for the HTTP header */
    private String getContentLength(File file) {
        return String.valueOf(file.length());
    }

    /* returns String representation of file last modified date in fields, needed for the HTTP header */
    private String getLastModified(File file) {
        return sdf.format(file.lastModified());
    }

    /*
    combines the path to the server folder with the path specified by the client,
    ensures correct merging of two parts, and correct format (e.g. slashes, no redundancy in the path),
    returns String representation for convenience
    */
    private void setPath() {
        pathName = Paths.get("http/resources", inputDir).normalize().toString();
    }

    /* returns path name */
    String getPath(){
        return pathName;
    }

    /*
    gets extension of the file that is requested, right now only determines if a file is png image, otherwise
    returns text/html
    */
    private String getExtension() {

        String extension = inputDir.substring(inputDir.lastIndexOf(".") + 1, inputDir.length());

        if (extension.equals("png"))
            return "image/png";

        else
            return "text/html; charset=UTF-8";
    }
}
