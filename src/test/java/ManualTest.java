import org.rnorth.tcpunixsocketproxy.TcpToUnixSocketProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Simple test class for manual testing purposes.
 */
public class ManualTest {

    private static final Logger logger = LoggerFactory.getLogger(ManualTest.class);

    public static void main(String[] args) throws IOException, InterruptedException {

        TcpToUnixSocketProxy proxy = new TcpToUnixSocketProxy(new File("/var/run/docker.sock"));

        InetSocketAddress address = proxy.start();
        logger.info("Listening on {}:{}", address.getHostName(), address.getPort());
        Thread.sleep(60_000);
        proxy.stop();
    }
}
