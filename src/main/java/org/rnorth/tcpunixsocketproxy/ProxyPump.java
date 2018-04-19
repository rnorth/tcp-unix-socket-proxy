package org.rnorth.tcpunixsocketproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * General purpose proxy between two {@link Socket}s. Blocking I/O for raw simplicity; not intended to
 * be particularly performant.
 */
class ProxyPump {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPump.class);
    private static final int COPY_BUFFER_SIZE = 4096;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new DaemonThreadFactory());

    /**
     * Proxy incoming traffic from the client socket to the server socket (and responses
     * back).
     *
     * @param clientSocket the socket the client is connecting to (e.g. a local {@link java.net.ServerSocket})
     * @param serverSocket the socket for connections to the (remote) server
     * @throws IOException if sockets cannot be streamed to/from
     */

    public ProxyPump(final Socket clientSocket, final Socket serverSocket) throws IOException {

        InputStream fromClient = clientSocket.getInputStream();
        OutputStream toClient = clientSocket.getOutputStream();
        InputStream fromServer = serverSocket.getInputStream();
        OutputStream toServer = serverSocket.getOutputStream();

        // Start to copy data from client to server
        EXECUTOR.submit(() -> {
            copyUntilFailure(fromClient, toServer);
            logger.trace("C->S died, closing sockets");
            quietlyClose(serverSocket);
            quietlyClose(clientSocket);
        });

        // Start to copy data back from server to client
        EXECUTOR.submit(() -> {
            copyUntilFailure(fromServer, toClient);
            logger.trace("S->C died, closing sockets");
            quietlyClose(serverSocket);
            quietlyClose(clientSocket);
        });
    }

    /*
     * Copy data from a from stream to a to stream, until the from stream ends.
     */
    private void copyUntilFailure(InputStream from, OutputStream to) {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];

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
