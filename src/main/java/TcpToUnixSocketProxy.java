import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on a TCP port and proxies connections to a UNIX domain socket.
 * <p>
 * Like <code>socat TCP-LISTEN:2375,fork UNIX-CONNECT:/var/tmp/docker.sock</code>, except this is literally the only
 * thing this program does.
 * <p>
 * Purposefully simplistic in implementation, likely to be buggy or suboptimal in performance, may contain nuts.
 *
 * @author Richard North &lt;rich.north@gmail.com&gt;
 *
 */
public class TcpToUnixSocketProxy {

    private final String listenHostname;
    private final int listenPort;
    private final String unixSocketPath;
    private ServerSocket listenSocket;

    private static final Logger logger = LoggerFactory.getLogger(ProxyThread.class);

    public TcpToUnixSocketProxy(String listenHostname, int listenPort, String unixSocketPath) throws IOException {

        this.listenHostname = listenHostname;
        this.listenPort = listenPort;
        this.unixSocketPath = unixSocketPath;
    }

    public void start() throws IOException {
        File file = new File(unixSocketPath);

        listenSocket = new ServerSocket();
        listenSocket.bind(new InetSocketAddress(listenHostname, listenPort));

        logger.debug("Listening on {}:{} and proxying to {}", listenHostname, listenPort, unixSocketPath);

        while (true) {
            Socket incomingSocket = listenSocket.accept();
            logger.debug("Accepting incoming connection");

            AFUNIXSocket outgoingSocket = AFUNIXSocket.newInstance();
            outgoingSocket.connect(new AFUNIXSocketAddress(file));

            new ProxyThread(incomingSocket, outgoingSocket);
        }
    }

    public void stop() {
        try {
            listenSocket.close();
        } catch (IOException ignored) { }
    }

    public static void main(String[] args) throws IOException {
        new TcpToUnixSocketProxy("localhost", 2375, "/var/run/docker.sock").start();
    }
}

class ProxyThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ProxyThread.class);

    public ProxyThread(final Socket clientSocket, final Socket serverSocket) throws IOException {

        InputStream fromClient = clientSocket.getInputStream();
        OutputStream toClient = clientSocket.getOutputStream();
        InputStream fromServer = serverSocket.getInputStream();
        OutputStream toServer = serverSocket.getOutputStream();

        // Create a thread to copy data from client to server
        Thread clientToServerThread = new Thread() {
            @Override
            public void run() {
                copyUntilFailure(fromClient, toServer);
                logger.trace("C->S died, closing sockets");
                quietlyClose(serverSocket);
                quietlyClose(clientSocket);
            }
        };
        clientToServerThread.start();

        // Create a thread to copy data back from server to client
        Thread serverToClientThread = new Thread() {
            @Override
            public void run() {
                copyUntilFailure(fromServer, toClient);
                logger.trace("S->C died, closing sockets");
                quietlyClose(serverSocket);
                quietlyClose(clientSocket);
            }
        };
        serverToClientThread.start();

    }

    /*
     * Copy data from a from stream to a to stream, until the from stream ends.
     */
    private void copyUntilFailure(InputStream from, OutputStream to) {
        byte[] buffer = new byte[4096];

        int read;
        try {
            while ((read = from.read(buffer)) != -1) {
                to.write(buffer, 0, read);
                to.flush();
            }
        } catch (IOException ignored) {
            // just return
        }
    }

    /*
     * Close a socket without handling exceptions, which also closes its streams.
     */
    private void quietlyClose(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}