import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Simple multi-threaded HTTP-Echo server
 * @author Peter Danielsson, pd222dj@student.lnu.se
 */
public class HTTPServer
{
    public static final int BUFSIZE = 1024;
    public static final int MYPORT = 8888;

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
    private int connectionTimeOut;

    public ClientConnectionThread(Socket socket, int buffSize)
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

                in.read(buf);
                temp.write(buf);
            }

            System.out.printf("HTTP request from %s using port %d \n", clientSocket.getInetAddress(), clientSocket.getPort());

            // For debugging purposes
            System.out.println(new String(temp.toByteArray(), "UTF-8"));

            String request = new String(temp.toByteArray(), "UTF-8").trim();


            processRequest(request.getBytes(), out);

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
     * @param req
     */
    private void processRequest(byte[] req, OutputStream out) throws IOException {

        // Make string-version of request
        String request = new String(req, "UTF-8");

        Scanner requestScanner = new Scanner(request);

        // Grab the first word in the HTTP-request which is the method
        String reqMeth = requestScanner.next();

        String destinationFilePath = null;

        /*
        Atm GET is only supported
        if the request is not GET, it will be handled in SetResponse method
        */
        if (reqMeth.equals("GET"))
            destinationFilePath = requestScanner.next().substring(1);


        HTTPResponseConstructor rc = new HTTPResponseConstructor(destinationFilePath);
        String header = rc.getHeader();
        byte[] response = setResponse(rc.getStatusCode(), rc.getPath());
        out.write(header.getBytes());
        out.write(response);
    }

    /*
    Form a response of a standard-header and the specific HTML-file
    */
    private byte[] setResponse(String status, String destinationFilePath){
        byte[] byteArr = null;
        try {
            switch (status) {
                case "200 OK": {
                    FileInputStream dataFileReader = new FileInputStream(destinationFilePath);

                    byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    break;
                }
                case "404 Not Found": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/fileNotFound.html");

                    byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    break;
                }
                case "403 Forbidden": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/forbidden.html");

                    byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    break;
                }
                case "500 Internal Server Error": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/internalError.html");

                    byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    break;
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArr;
    }
}


