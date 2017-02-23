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
            //System.out.println(new String(temp.toByteArray(), "UTF-8"));

            //String request = new String(temp.toByteArray(), "UTF-8").trim();


            //processRequest(request.getBytes(), out);
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

        /*
        if the request is not GET, PUT, or POST, it will be handled in SetResponse method
        */
        switch (reqMeth) {
            case "GET":
                destinationFilePath = requestScanner.next().substring(1);
                break;

            case "POST":
                uploadImage(req, null);
                destinationFilePath = "uploads/successful.html";
                break;

            case "PUT":
                destinationFilePath = requestScanner.next().substring(1);

                if(!destinationFilePath.contains("secretDir")) {
                    uploadImage(req, destinationFilePath);
                    destinationFilePath = "uploads/successful.html";
                }

                else
                    destinationFilePath = "secretDir";

                break;
        }

        HTTPResponseConstructor rc = new HTTPResponseConstructor(destinationFilePath);
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
        byte[] byteArr = null;
        switch (status) {
            case "200 OK": {
                byteArr = loadFile(destinationFilePath);
                break;
            }
            case "404 Not Found": {
                byteArr = loadFile("http/resources/ErrorPages/404.html");
                break;
            }
            case "403 Forbidden": {
                byteArr = loadFile("http/resources/ErrorPages/403.html");
                break;
            }
            case "500 Internal Server Error": {
                byteArr = loadFile("http/resources/ErrorPages/500.html");
                break;
            }
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
        String contentsLength ="";
        String boundaryNumber = "";

        Scanner lineScanner = new Scanner(request);

        // Find boundary number
        while(boundaryNumber.equals(""))
        {
            String currentRow = lineScanner.nextLine();
            if(currentRow.contains("boundary=---------------------------"))
                boundaryNumber = currentRow.replace("Content-Type: multipart/form-data; boundary=---------------------------", "");
        }

        // Find contents length
        while(contentsLength.equals(""))
        {
            String currentRow = lineScanner.nextLine();
            if(currentRow.contains("Content-Length:"))
                contentsLength = currentRow.split(" ")[1];
        }

        // Find name of file
        while(filename.equals(""))
        {
            String currentRow = lineScanner.nextLine();
            if(currentRow.contains("filename="))
            {
                String unCleanFilename = currentRow.split(" ")[3];
                filename = unCleanFilename.substring(unCleanFilename.indexOf("\"") + 1, unCleanFilename.lastIndexOf("\""));
            }
        }

        // Debugging purposes
        System.out.println("The filename is: " + filename);
        System.out.println("Contents-length is: " + Integer.parseInt(contentsLength));
        System.out.println("Boundary-number is: " + boundaryNumber);

        // Find the last part of the header
        int byteCountLastPartHeader = 0,
         lastPieceHeaderLength = 9;

        boolean found = false;

        while (!found)
        {
            byte[] currentBytePiece = Arrays.copyOfRange(req, byteCountLastPartHeader,
                    byteCountLastPartHeader + lastPieceHeaderLength);
            if (new String(currentBytePiece, "UTF-8").equals("image/png"))
                found = true;

            else
                byteCountLastPartHeader++;
        }

        // This string marks the end of binary data
        String endBinaryMarker = "-----------------------------" + boundaryNumber + "--";

        found = false;

        // Find the first byte of the end of binary data marker
        int endBinaryMarkByteCount = 0;
        int endBinaryLinesize = endBinaryMarker.length();

        while (!found)
        {
            byte[] currentBytePiece = Arrays.copyOfRange(req, endBinaryMarkByteCount,
                    endBinaryMarkByteCount + endBinaryLinesize);
            if (new String(currentBytePiece, "UTF-8").equals(endBinaryMarker))
                found = true;

            else
                endBinaryMarkByteCount++;

        }

        // 8 is for the image/png characters, 4 is for the CRLF on two rows
        int lastByteOfHeader = byteCountLastPartHeader + 8 + 4;

        int beginningOfImageDataByte = lastByteOfHeader + 1;

        // Extract image data. -2 because we want to remove the last newline (CRLF)
        byte[] imageData = Arrays.copyOfRange(req, beginningOfImageDataByte, endBinaryMarkByteCount - 2);

        // For debugging purposes
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 1]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 2]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 3]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 4]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 5]);
        System.out.println("beginning of png byte: " + req[beginningOfImageDataByte + 6]);

        System.out.println("Last byte: " + imageData[imageData.length -1]);
        System.out.println("Last byte: " + imageData[imageData.length -2]);
        System.out.println("Last byte: " + imageData[imageData.length -3]);
        System.out.println("Last byte: " + imageData[imageData.length -4]);
        System.out.println("Last byte: " + imageData[imageData.length -5]);
        System.out.println("Last byte: " + imageData[imageData.length -6]);
        System.out.println("Last byte: " + imageData[imageData.length -7]);
        System.out.println("Last byte: " + imageData[imageData.length -8]);
        System.out.println("Last byte: " + imageData[imageData.length -9]);
        System.out.println("Last byte: " + imageData[imageData.length -10]);
        System.out.println("Last byte: " + imageData[imageData.length -11]);
        System.out.println("Last byte: " + imageData[imageData.length -12]);

        System.out.println("Total number of bytes: " + (req.length - beginningOfImageDataByte));

        //System.out.println("IMAGE DATA BELOW");
        //System.out.println(new String(imageData, "UTF-8"));

        FileOutputStream file;

        if (path == null)
            file = new FileOutputStream("http/resources/uploads/" + filename);

        else
            file = new FileOutputStream(path + filename);

        file.write(imageData);
        file.close();
    }

}


