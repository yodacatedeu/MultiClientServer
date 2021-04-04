// Additional thread that allows the server to message all clients and issue Server Admin commands.

package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerMessenger implements Runnable{
    
    @Override
    public void run() {
        //System.out.print("Enter message: ");
                Scanner reader = new Scanner(System.in);
                String message = "";
                while (!message.equalsIgnoreCase("exit")) {
                    //System.out.print("> ");
                    message = reader.nextLine();
                    
                    if (message.startsWith("/")) {
                        switch (message.substring(1).toUpperCase()) {
                            case "SHOW CLIENTS":
                                displayClientList();
                                break;
                            case "KICK":
                                int index;
                                System.out.print("Enter the client's index to kick: ");
                                try {
                                    index = reader.nextInt();    
                                    try {
                                        kickClient(index);
                                    } catch (IOException ex) {
                                        System.err.println(ex);
                                    }
                                }
                                catch (InputMismatchException ex) {
                                    System.out.println("Error, index must be an integer.");
                                    reader.nextLine();
                                }
                                break;
                                
                            default:
                                System.out.println("Available commands:");
                                System.out.println("/show clients");
                                System.out.println("/kick");
                        }
                    }
                    else if (!message.isEmpty() && !message.equalsIgnoreCase("EXIT")) {
                        try {
                            sendMessage(message);
                        } catch (IOException ex) {
                            System.err.println(ex);
                        } catch (Exception ex) {
                            Logger.getLogger(ServerMessenger.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }                 
                }
                // Set server state to down
                Server.setUp(false);
        try {
            // Connect final closing socket so the main server loop will close.
            Socket sock = new Socket(Server.getServerIP(), Server.getPort());
            sock.close();
        } catch (IOException ex) {
            System.err.println("Final closing socket failed to connect");
        }
                
    }
    
    public synchronized void sendMessage(String msg) throws IOException, Exception{
        PrintWriter pout;
        for (int i = 0; i < Server.getClientList().size(); i++) {
            if (Server.getClientList().get(i) !=null) {
                pout = new PrintWriter(Server.getClientList().get(i).getClient().getOutputStream(), true);
                pout.println(Server.encrypt("Server: "+ msg));
            }         
        }
    }
    
    public synchronized void displayClientList() {
        for (int i = 0; i < Server.getClientList().size(); i++) {
            System.out.println("[" + i +"]: " + Server.getClientList().get(i).getClient().getInetAddress().getHostAddress() + 
                    " (" + Server.getClientList().get(i).getName() + ") ");
        }
    }
    
    public synchronized void kickClient(int index) throws IOException {
        if (index < 0 || index >= Server.getClientList().size()) {
            System.out.println("Error, index out of bounds.\nTry \"SHOW CLIENTS\" to view the current list.");
        }
        else {
            String name = Server.getClientList().get(index).getName();
            // port = Server.getClientList().get(index).getClient().getPort();
            String ip = Server.getClientList().get(index).getClient().getInetAddress().getHostAddress();
            Server.getClientList().get(index).getClient().close();
            Server.getClientList().remove(index);
            //Server.getClientNames().remove(index);
            System.out.println(new java.util.Date().toString() + "\nYou kicked " + ip + " (" + name + ").");
        }
    }
}
