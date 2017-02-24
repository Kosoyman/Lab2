import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Simple multi-threaded HTTP-Echo server
 * @author Peter Danielsson, pd222dj@student.lnu.se
 */
public class HTTPServer
{
    private static final int BUFSIZE = 1024;
    private static final int MYPORT = 8888;

    public static void main(String[] args) {

        // Print out message at startup
        System.out.printf("HTTP-Server running on port %d ...\n", MYPORT);

        try
        {
            // Create a socket for communication
            ServerSocket socket = new ServerSocket(MYPORT);

            // Endless main-loop
            while (true)
            {
                // Wait for a client to connect
                Socket clientSocket = socket.accept();

                // Create a new thread for handling the connection
                Thread clientConnection = new Thread(new ClientConnectionThread(clientSocket, BUFSIZE));
                clientConnection.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}

/**
 * A Client Connection handler-class that is used by the HTTP-Server
 * to create Threads for each Client connection
 * @author Peter Danielsson, pd222dj@student.lnu.se
 */
class ClientConnectionThread implements Runnable
{
    private Socket clientSocket;
    private int buffSize;

    ClientConnectionThread(Socket socket, int buffSize)
    {
        this.clientSocket = socket;
        this.buffSize = buffSize;
    }

    @Override
    public void run()
    {
        try
        {
            // Get raw byte-streams for Input/Output
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            // Used as a temporary storage when receiving data
            ByteArrayOutputStream temp = new ByteArrayOutputStream();

            // Wait for a message
            while(in.available() == 0);

            // Small delay so data can get through
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Keep reading from input-stream as long as we have bytes to process.
            while (in.available() != 0)
            {
                // Read input-stream and store it in temporary buffer
                byte[] buf = new byte[buffSize];

                int readBytes = in.read(buf);

                // In case we don't fill the the whole buffer, we want to remove zeroes.
                if (readBytes < buffSize)
                {
                    buf = Arrays.copyOf(buf, readBytes);
                }

                temp.write(buf);

            }

            System.out.printf("HTTP request from %s using port %d \n", clientSocket.getInetAddress(), clientSocket.getPort());

            // For debugging purposes
            System.out.println(new String(temp.toByteArray(), "UTF-8"));

            processRequest(temp.toByteArray(), out);

            System.out.printf("Closing connection for %s on port %d \n", clientSocket.getInetAddress(), clientSocket.getPort());
            this.clientSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Help-method for sending back a response. This is just for testing purposes
     * @param req - request
     */
    private void processRequest(byte[] req, OutputStream out) throws IOException {

        // Make string-version of request
        String request = new String(req, "UTF-8");

        Scanner requestScanner = new Scanner(request);

        // Grab the first word in the HTTP-request which is the method
        String reqMeth = requestScanner.next();

        String destinationFilePath = null;
        HTTPResponseConstructor rc;
        String fileToGet = requestScanner.next().substring(1),
                HTTP = requestScanner.next();

        /*
        if the request is not GET, PUT, or POST, it will be handled in SetResponse method
        */
        //check if http is ok
        if(!HTTP.equals("HTTP/1.1")) {
            destinationFilePath = "http/resources/StatusPages/505.html";
            rc = new HTTPResponseConstructor(destinationFilePath);
            rc.setPath();
            rc.setExtension();
            rc.forceStatusCode("505 HTTP Version Not Supported");
        }

        else if (reqMeth.equals("GET")) {
            destinationFilePath = fileToGet;
            rc = new HTTPResponseConstructor(destinationFilePath);
            rc.setPath();
            rc.setExtension();
            rc.setStatusCode();
        }
            /*
            the rest needed to be separated because now we do not want to compute the status code
            but rather force it to be of a certain value
            */

        //if HTTP is ok but the request is not get
        else {
            String verdict = null;
            if (reqMeth.equals("POST") || reqMeth.equals("PUT")){

                if (reqMeth.equals("POST")) {
                    uploadImage(req, null);
                    destinationFilePath = "uploads/StatusPages/201.html";
                    verdict = "201 Created";

                } else if (reqMeth.equals("PUT")) {
                    destinationFilePath = requestScanner.next().substring(1);

                    if (!destinationFilePath.contains("secretDir")) {
                        uploadImage(req, destinationFilePath);
                        destinationFilePath = "uploads/StatusPages/201.html";
                        verdict = "201 Created";
                    }

                    else
                        destinationFilePath = "secretDir";
                }

            }

            //if HTTP is ok but the request is neither get, nor post, nor put
            else {
                //checking for other standard HTTP requests that we didn't implement
                if (reqMeth.equals("HEAD") || reqMeth.equals("DELETE") || reqMeth.equals("OPTIONS") || reqMeth.equals("CONNECT")) {
                    destinationFilePath = "http/resources/StatusPages/501.html";
                    verdict = "501 Not Implemented";
                    //any other request is to be considered a bad request
                } else {
                    destinationFilePath = "http/resources/StatusPages/400.html";
                    verdict = "400 Bad Request";
                }
            }

            rc = new HTTPResponseConstructor(destinationFilePath);
            rc.setPath();
            rc.setExtension();
            rc.forceStatusCode(verdict);
        }

        rc.setHeader();
        String header = rc.getHeader();
        byte[] response = setResponse(rc.getStatusCode(), rc.getPath());
        out.write(header.getBytes());
        out.write(response);
    }

    /**
     *Form a response of the specific HTML-file depending on its existence and accessibility
     * @param status - status code of the HTTP message
     * @param destinationFilePath - path to the required file
     * @return byteArr - byte array containing raw bytes of the file
     */
    private byte[] setResponse(String status, String destinationFilePath){
        byte[] byteArr;

        if (status.equals("200 OK"))
            byteArr = loadFile(destinationFilePath);

        else {
            String statusName = status.split(" ")[0];
            byteArr = loadFile("http/resources/StatusPages/" + statusName +".html");
        }

        return byteArr;
    }

    /**
     * Loads file from a String path name into a byte array
     * @param pathName - path to the required file
     * @return byteArr - byte array containing contents of the file
     */
    private byte[] loadFile(String pathName)
    {
        byte[] byteArr = null;

        try {
            FileInputStream dataFileReader = new FileInputStream(pathName);
            byteArr = new byte[dataFileReader.available()];
            dataFileReader.read(byteArr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArr;
    }

    /**
     * Test-method to extract image (png) binary data from a POST-request and store it as a file
     * @param req byte-array containing full request from client
     * @throws IOException
     */
    private void uploadImage(byte[] req, String path) throws IOException {

        // Make string-version of request, used to extract some data
        String request = new String(req, "UTF-8");

        String filename ="";

        Scanner lineScanner = new Scanner(request);

        // Find name of file
        while(filename.equals("") && lineScanner.hasNextLine())
        {
            String currentRow = lineScanner.nextLine();

            if(currentRow.contains("filename="))
            {
                String unCleanFilename = currentRow.split(" ")[3];
                filename = unCleanFilename.substring(unCleanFilename.indexOf("\"") + 1, unCleanFilename.lastIndexOf("\""));
            }
        }

        // -119 since it will be interpreted as a signed int.
        byte[] pngSignature = {-119, 80, 78, 71, 13, 10, 26, 10};

        byte[] pngIEND = {73, 69, 78, 68};

        int beginningOfImageDataByte = findByteSequenceIndex(req, pngSignature);

        // +4 because of the CRC in the end
        int lastByteofImageData = findByteSequenceIndex(req, pngIEND) + pngIEND.length + 4;

        byte[] imageData = Arrays.copyOfRange(req, beginningOfImageDataByte, lastByteofImageData);

        FileOutputStream file;

        if (path == null)
            file = new FileOutputStream("http/resources/uploads/" + filename);

        else
            file = new FileOutputStream(path + filename);

        file.write(imageData);
        file.close();
    }

    /**
     * Help-method to return the first index of a byte-sequence in a byte-buffer
     * @param buf Byte buffer
     * @param seq Byte sequence to look for
     * @return first index of byte-sequence, -1 if not found.
     */
    private int findByteSequenceIndex(byte[] buf, byte[] seq) {

        boolean found = false;
        int i = 0;

        // Main loop, go through whole buffer if needed
        while (!found && i <= buf.length - seq.length) {

            int j = 0;

            // Check the actual location and elements in front, if they corresponds to the sequence
            while (j < seq.length && buf[i + j] == seq[j])
            {
                j++;
            }
            if (j == seq.length)
            {
                found = true;
            }
            else
            {
                i++;
            }
        }

        if (found)
        {
            return i;
        }
        else
        {
            return -1;
        }
    }
}


