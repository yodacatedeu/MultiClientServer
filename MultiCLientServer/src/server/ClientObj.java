// A client Obj containing the client's socket, name, and other eventual features

package server;

import java.net.Socket;

/**
 *
 * @author aronb
 */
public class ClientObj {
    private Socket client;
    private String name;
    
    public ClientObj(Socket c) {
        client = c;
    }
    
    public ClientObj(Socket c, String nm) {
        this(c);
        name = nm;
    }

    /**
     * @return the client
     */
    public Socket getClient() {
        return client;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    
}
