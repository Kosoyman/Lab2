/*
  UDPEchoServer.java
  A simple echo server with no error handling
*/


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HTTPServer {
    private static final int BUFSIZE = 1024;
    private static final int MYPORT = 4950;
    private static boolean isStopped = false;
    private static ServerSocket serverSocket;
    static final String HTML_START =
            "<html>" +
                    "<title>HTTP Server in java</title>" +
                    "<body>";

    static final String HTML_END =
            "</body>" +
                    "</html>";

    public static void main(String[] args){
        try {
            //create socket
            serverSocket = new ServerSocket(MYPORT);
            Socket socket = null;
            while (!isStopped) {
                try {
                    //start waiting for connections
                    socket = serverSocket.accept();
                    socket.setReceiveBufferSize(BUFSIZE);
                    socket.setSendBufferSize(BUFSIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                new Thread(new HTTPRequestHandler(socket)).run(); //starts new thread for every connection
            }
            socket.close(); //closes the socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //stops the server
    public static void stop() {
        isStopped = true;
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //checks if the server has been stopped
    public static boolean isStopped()
    {
        return isStopped;
    }
}