import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Maxim Kravchenko
 * Created on 15-Feb-17, last modofied on 18-Feb-17.
 */
class HTTPResponseConstructor {

    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM, yyyy hh:mm:ss a z");
    private String inputDir;

    HTTPResponseConstructor(String path){
        inputDir = path;
    }

    /* constructs and returns string http response based on status code */
    String setResponse(){

        String code = GetStatusCode();

        if (code.equals("200 OK")) {
            File f = new File (GetPath(inputDir));
            return "HTTP/1.1 " + code + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Content-Type: text/html; charset=UTF-8\n" +
                    "Content-Encoding: UTF-8\n" +
                    "Content-Length: " + getContentLength(f) + '\n' +
                    "Last-Modified: " + getLastModified(f) + '\n' +
                    "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n" +
                    "ETag: \"3f80f-1b6-3e1cb03b\"\n" +
                    "Accept-Ranges: bytes\n" +
                    "Connection: close\n\n";
        }

        else {
            //TODO: make sure that this counts as a proper "negative" response, we might need to add more info in the header
            return "HTTP/1.1 " + code + '\n' +
                    "Date: " + getDate() + '\n' +
                    "Content-Type: text/html; charset=UTF-8\n" +
                    "Content-Encoding: UTF-8\n" +
                    "Server: Apache/1.3.3.7 (Unix) (Red-Hat/Linux)\n" +
                    "ETag: \"3f80f-1b6-3e1cb03b\"\n" +
                    "Accept-Ranges: bytes\n" +
                    "Connection: close\n\n";
        }
    }

    /* determines the status code by trying to access the specified file */
    String GetStatusCode(){
        String ok = "200 OK",
                forbidden = "403 Forbidden",
                notFound = "404 Not Found",
                serverErr = "500 Internal Server Error";

        try {
            File file = new File(GetPath(inputDir));
            boolean exists = file.exists(),
                    readable = file.canRead();

            if (!exists)
                return notFound;

            else if(!readable)
                return forbidden;

            else
                return ok;

        }catch (SecurityException e){
            e.printStackTrace();
            return serverErr;
        }catch(NullPointerException n){
            return forbidden;
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
    String GetPath(String lastPiece){
        return Paths.get("http/resources", lastPiece).normalize().toString();
    }
}
