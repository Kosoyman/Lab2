import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by maxkr on 15-Feb-17.
 */
public class HTTPResponseConstructor {

    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM, yyyy hh:mm:ss a z");
    private final String serverDir = null; //directory that contains server files
    private String inputDir = null;

    HTTPResponseConstructor(String path){
        inputDir = path;
    }

    /* constructs and returns string http response based on status code */
    public String setResponse(){

        String code = DetermineStatusCode();

        if (code.equals("200 OK")) {
            File f = new File (makePath(inputDir));
            return "HTTP/1.1 " + code + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Status-Encoding: UTF-8\n" +
                    "Content-Length: " + getContentLength(f) + '\n' +
                    "Last-Modified: " + getLastModified(f) + '\n' +
                    "Server: home-made diamond\n" +
                    "Connection: close";
        }

        else {
            //TODO: make sure that this counts as a proper "negative" response, we might need to add more info in the header
            return  "HTTP/1.1 " + code + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Status-Encoding: UTF-8\n" +
                    "Server: home-made diamond\n" +
                    "Connection: close";
        }
    }

    /* determines the status code by trying to access the specified file */
    private String DetermineStatusCode(){
        String ok = "200 OK",
                forbidden = "403 Forbidden",
                notFound = "404 Not Found",
                serverErr = "500 Internal Server Error";

        try {
            File file = new File(makePath(inputDir));
            boolean exists = file.exists(),
                    readable = file.canRead(),
                    //checks if file is directory, I'm sure it's useful but not sure how to utilize it just yet
                    isDirectory = file.isDirectory();
            if (!exists)
                return notFound;

            else if(!readable)
                return forbidden;

            else
                return ok;

        }catch (SecurityException e){
            e.printStackTrace();
            return serverErr;
        }
    }

    /*
    returns String representation of current date with format specified in the fields
    TODO: right now returns one hour smaller than current time, fix if time allows
    */
    private String getDate(){
        final Date currentTime = new Date();
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (sdf.format(currentTime));
    }

    /* returns String representation of file length in bytes, needed for the HTTP header */
    private String getContentLength(File file){
        return String.valueOf(file.length());
    }

    /* returns String representation of file last modified date in fields, needed for the HTTP header */
    private String getLastModified(File file){
        return sdf.format(file.lastModified());
    }

    /*
    combines the path to the server folder with the path specified by the client,
    ensures correct merging of two parts, and correct format (e.g. slashes, no redundancy in the path),
    returns String representation for convenience
    */
    private String makePath(String lastPiece){
        return Paths.get(serverDir, lastPiece).normalize().toString();
    }
}
