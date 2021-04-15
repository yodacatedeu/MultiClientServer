// The main server process: hosts the server socket.
// Maintains list of all connected clients.
// Spawns ServerMessenger, and a ClientManager thread for each client

package server;

import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.InvalidKeyException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Server {
    private static final ArrayList <ClientObj> clientList = new ArrayList<>(10);
    //private static final ArrayList <String> clientNames = new ArrayList<>(10);
    private static ServerSocket sock;
    private static String password;
    private static boolean up;
    private static String serverIP;
    private static int port;
    private static SecretKeySpec secretKey;
    private static byte[] key;
    private static final String secret = "LemonMan";
    private static final ArrayList<String> blackList = new ArrayList<>();
     
    public static void main (String[] args) throws InterruptedException, IOException {
        // Get device's IP address in use
        String deviceIP = null;
        String publicIP = null;
        System.out.println("Getting device's IP address...");
        try (final DatagramSocket socket= new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            deviceIP = socket.getLocalAddress().getHostAddress();
            //System.out.println(deviceIP);
            socket.close();  
            System.out.println("done.");
        } 
        
        // Get public IP address
        System.out.println("Getting public IP address...");
        try {
            URL url_name = new URL("http://bot.whatismyipaddress.com");
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));
            publicIP = sc.readLine();
            //System.out.println(publicIP);
            sc.close();
            System.out.println("done.");
        }
        catch (MalformedURLException e) {
            System.err.println("Failed to get device's public IP address.");
        }
        
        System.out.println("Opening Blacklist file...");
        FileReader inFile = new FileReader("BlackList.txt");
        Scanner fileScanner = new Scanner(inFile);
        String s;
        System.out.println("Converting to array...");
        while (fileScanner.hasNext()) {
            s = fileScanner.next();
            getBlackList().add(s);
        }
        inFile.close();
        System.out.println("Done.");
        //System.out.println(InetAddress.getLocalHost().getHostAddress() +"\n" + InetAddress.getLocalHost().getHostName());
        //serverIP = "192.168.1.129";
        serverIP = deviceIP;
        Scanner reader = new Scanner(System.in);
        System.out.println("\nSet server password: ");
        password = reader.nextLine();
        Thread messenger;
        
        try {
            //sock = new ServerSocket(6013);
            port = 80;
            InetAddress ia;
            //ia = InetAddress.getByName("142.129.175.110");
            ia = InetAddress.getByName(getServerIP());
            sock = new ServerSocket();
            SocketAddress endPoint = new InetSocketAddress(ia, getPort());
            getSock().bind(endPoint);
            up = true;
            System.out.println("\n" + new java.util.Date().toString());
            System.out.println("Server bound to: \nDevice IP address: " + getSock().getInetAddress().getHostAddress() + "\nPort " + getSock().getLocalPort());
            System.out.println("Public IP address: " + publicIP);
            System.out.println("Password: " + getPassword());
            
            messenger = new Thread(new ServerMessenger());
            messenger.start();
            //listen for connections
            while (up) {
                System.out.println("\nAwaiting client connection...");
                Socket client = getSock().accept();
                Thread sThrd = new Thread(new ClientManager(client));
                sThrd.start(); // Pray for garbage collection lol
            }
            messenger.join();
        }
        catch (IOException ioe) {
            System.err.println(ioe);
            System.err.println("Issue in connecting or sending to client.");
        }
        System.out.println("Closing server...");
        // Closing the server 
        // First by closing all sockets and clearing the clientList and namesList
        synchronized (clientList) {
            for (int i = 0; i < clientList.size(); i++) {
                clientList.get(i).getClient().close();
            }
            clientList.clear();
//            synchronized (clientNames) {
//                clientNames.clear();
//            }
        }
        // Close the server Socket itself
        sock.close();
        
        synchronized (blackList) { // just in case any last thread is still accessing the blacklist for some reason.
            //System.out.println("Sorting blacklist...");
            String [] tempArr = new String[blackList.size()];// = (String) 
            blackList.toArray(tempArr);
            //Arrays.sort(tempArr);
            System.out.println("Updating Blacklist file...");
            FileWriter outFile = new FileWriter("Blacklist.txt");
            for (String tempArr1 : tempArr) {
                outFile.write(tempArr1 + "\n");
            }
            outFile.close();
        }
        
        System.out.println("Done.");
        System.out.println("Goodbye :)");
    }

    /**
     * @return the clientList
     */
    public static ArrayList <ClientObj> getClientList() {
        return clientList;
    }

    /**
     * @return the sock
     */
    public static ServerSocket getSock() {
        return sock;
    }

    /**
     * @return the password
     */
    public static String getPassword() {
        return password;
    }

    /**
     * @return the clientNames
     */
//    public static ArrayList <String> getClientNames() {
//        return clientNames;
//    }

    /**
     * @return the up
     */
    public static boolean isUp() {
        return up;
    }

    /**
     * @param aUp the up to set
     */
    public static void setUp(boolean aUp) {
        up = aUp;
    }

    /**
     * @return the serverIP
     */
    public static String getServerIP() {
        return serverIP;
    }

    /**
     * @return the port
     */
    public static int getPort() {
        return port;
    }
    
    public static String encrypt(String strToEncrypt) 
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } 
        catch (Exception e) 
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }
 
    public static String decrypt(String strToDecrypt) throws Exception 
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } 
        catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException | IllegalArgumentException e) 
        {
            System.err.println("Error while decrypting: " + e.toString());
            throw new ArithmeticException();
        }
        //return null;
    }
    
    public static void setKey(String myKey) 
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); 
            secretKey = new SecretKeySpec(key, "AES");
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } 
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the blackList
     */
    public static ArrayList<String> getBlackList() {
        return blackList;
    }
}
