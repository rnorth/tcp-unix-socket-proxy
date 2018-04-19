package org.rnorth.tcpunixsocketproxy;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on a TCP port and proxies connections to a UNIX domain socket.
 * <p>
 * Like <code>socat TCP-LISTEN:2375,fork UNIX-CONNECT:/var/tmp/docker.sock</code>, except this is literally the only
 * thing this program does.
 * <p>
 * Purposefully simplistic in implementation, using blocking I/O for simplicity over performance.
 *
 * @author Richard North &lt;rich.north@gmail.com&gt;
 */
public class TcpToUnixSocketProxy {

    private final String listenHostname;
    private final int listenPort;
    private final File unixSocketFile;
    private ServerSocket listenSocket;

    private static final Logger logger = LoggerFactory.getLogger(ProxyPump.class);
    private Thread acceptThread;
    private boolean running = true;

    /**
     * Create an instance of the proxy that will listen for TCP connections on localhost on a
     * random available port.
     * <p>This should be the usual method of instantiating a proxy.</p>
     * @param unixSocketFile the local unix domain socket as a File
     */
    public TcpToUnixSocketProxy(File unixSocketFile) {

        if (!unixSocketFile.exists()) {
            throw new IllegalArgumentException("Socket file does not exist: " + unixSocketFile);
        }

        this.listenHostname = "localhost";
        this.listenPort = 0;
        this.unixSocketFile = unixSocketFile;
    }
    /**
     * Create an instance of the proxy.
     * @param listenHostname hostname to listen on
     * @param listenPort port to listen on, or 0 if an available port should be allocated
     * @param unixSocketFile the local unix domain socket as a File
     */
    public TcpToUnixSocketProxy(String listenHostname, int listenPort, File unixSocketFile) {

        if (!unixSocketFile.exists()) {
            throw new IllegalArgumentException("Socket file does not exist: " + unixSocketFile);
        }

        this.listenHostname = listenHostname;
        this.listenPort = listenPort;
        this.unixSocketFile = unixSocketFile;
    }

    /**
     * Start the proxy
     * @return the proxy's listening socket address
     * @throws IOException on socket binding failure
     */
    public InetSocketAddress start() throws IOException {

        listenSocket = new ServerSocket();
        listenSocket.bind(new InetSocketAddress(listenHostname, listenPort));

        logger.debug("Listening on {} and proxying to {}", listenSocket.getLocalSocketAddress(), unixSocketFile.getAbsolutePath());

        acceptThread = new Thread(() -> {
            while (running) {
                try {
                    Socket incomingSocket = listenSocket.accept();
                    logger.debug("Accepting incoming connection from {}", incomingSocket.getRemoteSocketAddress());

                    AFUNIXSocket outgoingSocket = AFUNIXSocket.newInstance();
                    outgoingSocket.connect(new AFUNIXSocketAddress(unixSocketFile));

                    new ProxyPump(incomingSocket, outgoingSocket);
                } catch (IOException ignored) {
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.setName("tcp-unix-proxy-accept-thread");
        acceptThread.start();

        return (InetSocketAddress) listenSocket.getLocalSocketAddress();
    }

    /**
     * Stop the proxy.
     */
    public void stop() {
        try {
            running = false;
            listenSocket.close();
        } catch (IOException ignored) {
        }
    }
}