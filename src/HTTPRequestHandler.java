import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by maxkr on 06-Feb-17.
 */
public class HTTPRequestHandler extends Thread {
    private Socket socket;
    HTTPRequestHandler(Socket s){
        socket = s;
    }

    public void run(){
        try {
            DataInputStream incomingData = new DataInputStream(socket.getInputStream());
            DataOutputStream outgoingData = new DataOutputStream(socket.getOutputStream());
            int timeout = 0; //if the expected amount of messages does not arrive, timeout will exit the while loop

            while (incomingData.available() > 0 || (timeout < 1000)) {//
                byte[] buf = new byte[1024];
                incomingData.read(buf);
                timeout ++;

                //check if the msg has arrived
                if (!isNoInput(buf)) {
                    outgoingData.write(buf);
                    System.out.printf("TCP echo request from %s", socket.getInetAddress().getHostAddress());
                    System.out.printf(" using port %d\n", socket.getPort());
                }
            }

            //close the connection
            incomingData.close();
            outgoingData.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // check if the message from the client has been read
    private static boolean isNoInput(byte[] b){
        if(b.length == 0)
            return true;
        for(byte current : b){
            if(current !=  0)
                return false;
        }
        return true;
    }
}
