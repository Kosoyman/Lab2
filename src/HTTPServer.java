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
    public static final int MS_PER_SECOND = 2000;
    public static final int CONNECTION_TIME_OUT = 2 * MS_PER_SECOND; // Connection timeout in ms

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
                Thread clientConnection = new Thread(new ClientConnectionThread(clientSocket, BUFSIZE, CONNECTION_TIME_OUT));
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

    public ClientConnectionThread(Socket socket, int buffSize, int connectionTimeOut)
    {
        this.clientSocket = socket;
        this.buffSize = buffSize;
        this.connectionTimeOut = connectionTimeOut;
    }

    @Override
    public void run()
    {
        try
        {
            // Get raw byte-streams for Input/Output
            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            boolean done = false;

            // Will automatically close connection after a time-out period
            while (!done)
            {
                // Start clock
                long start = System.currentTimeMillis();

                // Used as a temporary storage when receiving data
                ByteArrayOutputStream temp = new ByteArrayOutputStream();

                // Wait for a message or until time out (client has probably closed connection)
                while(in.available() == 0 && (System.currentTimeMillis() - start) < this.connectionTimeOut );

                // Check if we received data or timed out
                if (in.available() != 0)
                {
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

                    /*
                       Half dirty test below to test to send back a web-page to the client.
                       Header currently static, copied from Wikipedias homepage
                     */

                    processRequest(request.getBytes(), out);

                }
                else
                {
                    done = true;
                }
            }

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
        {
            //now only takes the name of the file
            destinationFilePath = requestScanner.next().substring(1);
        }

        HTTPResponseConstructor rc = new HTTPResponseConstructor(destinationFilePath);

        String response = setResponse(rc.setResponse(), rc.GetStatusCode(), rc.GetPath());
        // For debugging purposes
        System.out.println(response);

        out.write(response.getBytes());

    }

    /*
    Form a response of a standard-header and the specific HTML-file
    */
    private String setResponse(String header, String status, String destinationFilePath){
        String response = header;
        try {
            switch (status) {
                case "200 OK": {
                    FileInputStream dataFileReader = new FileInputStream(destinationFilePath);

                    byte[] byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    response += new String(byteArr, "UTF-8");
                    break;
                }
                case "404 Not Found": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/fileNotFound.html");

                    byte[] byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    response += new String(byteArr, "UTF-8");
                    break;
                }
                case "403 Forbidden": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/forbidden.html");

                    byte[] byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    response += new String(byteArr, "UTF-8");
                    break;
                }
                case "500 Internal Server Error": {
                    FileInputStream dataFileReader = new FileInputStream("http/resources/ErrorPages/internalError.html");

                    byte[] byteArr = new byte[dataFileReader.available()];
                    dataFileReader.read(byteArr);

                    response += new String(byteArr, "UTF-8");
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
        return response;
    }
}


