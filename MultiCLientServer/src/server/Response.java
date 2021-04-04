// Recieves Response from Client.  It will broadcast this client's message to the server and all other clients.

package server;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Response implements Runnable{

    private final Socket sock;
    //private boolean terminal;
    private final String clientName; 
    
    
    public Response(Socket s, String n) {
        sock = s;
        clientName = n;
        //terminal = false;
    }
    
//    public void setTerminal(boolean b) {
//        terminal = b;
//    }
    
    @Override
    public void run() {
        InputStream in;
        try {
            broadcastMsg(Server.encrypt("Server: " + clientName + " has connected."));
            in = sock.getInputStream();
            
            BufferedReader bin = new
                BufferedReader(new InputStreamReader(in));
            // read date from socket
            String line;
            while (!sock.isClosed() && (line = Server.decrypt(bin.readLine())) != null && !line.equalsIgnoreCase("EXIT")) {
                System.out.println(sock.getInetAddress().getHostAddress() + " (" + clientName + "): " + line);                
                broadcastMsg(Server.encrypt(clientName + ": " + line));
            }        
            
        } catch (IOException ex) {
            System.err.println("IOException in Response.java.");
            //System.err.println("Issue in recieving from client.");
        } catch (Exception ex) {
            System.err.println("Unexpected disconnect from client (in response.java).");;
        }
        
        if (!sock.isClosed()) {
            try {
                sock.close();
            } catch (IOException ex) {
                System.err.println("Strange error in closing socket from response thread.");
            }
            System.out.println(new java.util.Date().toString() + "\n" + sock.getInetAddress().getHostAddress() + " (" + clientName +") has disconnected.");
            try {
                broadcastMsg(Server.encrypt("Server: " + clientName + " has disconnected."));
            } catch (Exception ex) {
                Logger.getLogger(Response.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            try {
                //System.out.println("Client " + sock.getPort() + " (" + clientName +") was kicked by you.");
                broadcastMsg(Server.encrypt("Server: " + clientName + " was kicked."));
            } catch (Exception ex) {
                System.err.println("Unexpected client disconnect or failed decryption (Response.java).");
            }
        }
    }
    
    public synchronized void broadcastMsg(String msg) {
        try {
            PrintWriter pout;
            for (int i = 0; i < Server.getClientList().size(); i++) {
                if (Server.getClientList().get(i) != null && !Server.getClientList().get(i).getClient().equals(sock)) {
                    pout = new PrintWriter(Server.getClientList().get(i).getClient().getOutputStream(), true);
                    pout.println(msg);
                }         
            }
        } catch (IOException ex) {
            System.err.println(ex);
        } 
    }
    
}
