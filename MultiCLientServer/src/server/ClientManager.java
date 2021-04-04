// Authenticates client and is responsible for adding and removing the client from the ClientList.
// Spawns the Response thread

package server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
//import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientManager implements Runnable {
    private final Socket client;
    private String name;
    
    public ClientManager(Socket c) {
        client = c;
    }
    
    @Override
    public void run() {
        try {     
            System.out.println(new java.util.Date().toString() + "\nA client from IP address " + client.getInetAddress().getHostAddress()  + " is attempting to connect.\nAuthenticating...");
            
            PrintWriter pout = new 
                    PrintWriter(client.getOutputStream(), true);
            
            //Check BlackList
            if (isOnBlacklist()) {
                pout.println(Server.encrypt("You are currently blacklisted.  Connection refused."));
                client.close();
                System.out.println(client.getInetAddress().getHostAddress() + " was on the blacklist.  Their connection has been refused.");
            }
            else {    
                // Authenticate Client
                pout.println(Server.encrypt("Please enter password: "));
                InputStream in = client.getInputStream(); 

                BufferedReader bin = new
                    BufferedReader(new InputStreamReader(in));

                 String userInput = Server.decrypt(bin.readLine());
                 for (int i = 3; i > 0 && (userInput == null || !userInput.equals(Server.getPassword())); i--) {
                     pout.println(Server.encrypt("Incorrect password.  Try again (" + i  + " attemps left)."));
                     userInput = Server.decrypt(bin.readLine());
                 }

                 if (userInput != null && userInput.equals(Server.getPassword())) {
                     //String clientName;
                     pout.println(Server.encrypt("Please enter a name: "));
                     int characterLimit = 10;
                     name = Server.decrypt(bin.readLine());
                     while (name.length() > characterLimit || name.equalsIgnoreCase("server") || name.equalsIgnoreCase("exit") || name.contains("\n") || !isNameAvailable(name)) {
                         pout.println(Server.encrypt("Invalid or unavailable name, please try a different name (character limit "+ characterLimit + "): "));
                         name = Server.decrypt(bin.readLine());
                     }

                     addNewClient();

                    Response r = new Response(client, name);
                    Thread thrd = new Thread(r);
                    thrd.start();

                    // Let user know they have connected
                    pout.println(Server.encrypt("\nConnected on: "+ new java.util.Date().toString() + 
                            "\nName: " + name + "\nOther connected users: " + (Server.getClientList().size()-1)  
                            //"\nServer port: " + client.getLocalPort() + 
                            //"\nClient port: " + client.getPort() +
                            /*"\nType EXIT to exit.*/ + "\n"));

                    System.out.println(client.getInetAddress().getHostAddress() + " (" + name + ") has successfully connected.");
                    //System.out.println("Enter \"EXIT\" to exit.\n");

                    thrd.join();
                    // close socket 
                    if (!client.isClosed()) {
                        client.close();
                        System.err.println(client.getInetAddress().getHostAddress() + " (" + name + ") has unexpectedly lost connection.");
                    }

                    removeClient(client);
                 }
                 else {
                     pout.println(Server.encrypt("Denied."));
                     client.close();
                     System.out.println(client.getInetAddress().getHostAddress() + " was denied access.");
                     if (userInput != null)
                        System.out.println("Last attempted password: " + userInput);
                     else
                        System.out.println("Last attempted password was null.");
                     addClientToBlacklist();                  
                     System.out.println("This IP address has been added to the blacklist.");
                 }
            }               
        } catch (IOException | InterruptedException ex) {
            System.err.println("In ClientManager: " + ex);
        } catch (Exception ex) {
            System.err.println("In ClientManager: Can't decrypt client input or connection was closed before completing authentication.\nThis socket has been closed.");
            try {
                client.close();
            } catch (IOException ex1) {
                System.out.println("Failed to close socket after unexpected disconnect in ClientManager.");
            }
        }  
    }
    
    public synchronized void addNewClient() throws IOException {
            Server.getClientList().add(new ClientObj(client, name));
            //Server.getClientNames().add(name);
    }
    
    public synchronized void removeClient(Socket client) {
        boolean removed = false;
        for (int i = 0; !removed && i < Server.getClientList().size(); i++) {
            if (Server.getClientList().get(i).getClient().equals(client)) {
                Server.getClientList().remove(i);
                removed = true;
            }
        }
    }
    
    public synchronized boolean isNameAvailable(String name) {
        boolean found = false;
        for (int i = 0; !found && i < Server.getClientList().size(); i++) {
            if (Server.getClientList().get(i).getName().equals(name))
                found = true;
        }
        return !found;
    }
    
    private synchronized boolean isOnBlacklist() throws FileNotFoundException, IOException {    
       return Server.getBlackList().contains(client.getInetAddress().getHostAddress());
    }
    
    private synchronized void addClientToBlacklist() throws IOException {
        Server.getBlackList().add(client.getInetAddress().getHostAddress());
    }
}
