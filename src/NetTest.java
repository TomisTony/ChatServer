import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class ClientManager {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Server server;
    private ClientReader clientReader;
    private final ClientMessageSender clientMessageSender;

    private boolean isRunning = true;

    public ClientManager(Socket s, Server server) throws IOException {
        socket = s;
        this.server = server;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        clientReader = new ClientReader(this, in);
        clientMessageSender = new ClientMessageSender(this, out);
    }

    public Socket getSocket() {
        return socket;
    }

    public Server getServer() {
        return server;
    }

    public synchronized boolean getIsRunning(){
        return isRunning;
    }

    public synchronized void quit(){
        isRunning = false;
        server.clientQuit(this);
    }


    public void sendMsgToClient(String message) {
        synchronized (clientMessageSender) {
            clientMessageSender.addMessageIntoQueue(message);
            clientMessageSender.notify();
        }
    }
}

class ClientReader extends Thread {
    ClientManager clientManager;
    BufferedReader in;

    ClientReader(ClientManager cm, BufferedReader in) {
        this.clientManager = cm;
        this.in = in;
        start();
    }

    public void run() {
        try {
            while (clientManager.getIsRunning()) {
                String message = in.readLine();
                if (message.equals("END")) break;
                clientManager.getServer().addMessageToMessageSender(message);
            }
        } catch (IOException e) {
            clientManager.quit();
        } finally {
            try {
                clientManager.getSocket().close();
            } catch (IOException e) {
            }
        }
    }
}

class ClientMessageSender extends Thread {
    ClientManager clientManager;
    final PrintWriter out;
    Queue<String> msgQueue = new LinkedList<>();

    ClientMessageSender(ClientManager cm, PrintWriter out) {
        this.clientManager = cm;
        this.out = out;
        start();
    }

    public void addMessageIntoQueue(String message) {
        synchronized (this) {
            msgQueue.add(message);
        }
    }

    public void run() {
        while (clientManager.getIsRunning()) {
            synchronized (this) {
                while (msgQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            synchronized (this) {
                while(!msgQueue.isEmpty()){
                    String msg = msgQueue.poll();
                    try{
                        out.println(msg);
                    } catch (Exception e){
                        clientManager.quit();
                    }
                }
            }
        }
    }
}

class Server {
    static final int PORT = 4657;
    ArrayList<ClientManager> clientManagers = new ArrayList<>();
    final MessageSender messageSender;

    public Server() throws IOException {
        ServerSocket s = new ServerSocket(PORT);

        messageSender = new MessageSender(this);

        try {
            while (true) {
                Socket socket = s.accept();
                synchronized (this) {
                    try {
                        clientManagers.add(new ClientManager(socket, this));
                    } catch (IOException e) {
                        socket.close();
                    }
                }
            }
        } finally {
            s.close();
        }
    }

    public void addMessageToMessageSender(String msg) {
        synchronized (messageSender) {
            messageSender.notify();
            messageSender.addMessageIntoQueue(msg);
        }
    }

    public void sendMessageToAllClinets(String message) {
        synchronized (this) {
            int count = 1;
            for (ClientManager clientManager : clientManagers) {
                clientManager.sendMsgToClient(message + " " + count);
                count += 1;
            }
        }
    }

    public void clientQuit(ClientManager cm){
        synchronized (this){
            clientManagers.remove(cm);
        }
    }

}

class MessageSender extends Thread {
    Queue<String> msgQueue = new LinkedList<>();
    Server server;

    MessageSender(Server server) {
        this.server = server;
        start();
    }

    public void run() {
        while (true) {
            synchronized (this) {
                while (msgQueue.isEmpty()) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            synchronized (this) {
                while(!msgQueue.isEmpty())
                    server.sendMessageToAllClinets(msgQueue.poll());
            }
        }
    }

    public void addMessageIntoQueue(String message) {
        synchronized (this) {
            msgQueue.add(message);
        }
    }

}

public class NetTest {
    public static void main(String[] args) throws IOException {
        Server myServer = new Server();
    }
}