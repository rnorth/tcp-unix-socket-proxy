package org.rnorth.tcpunixsocketproxy;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basic factory that produces named daemon threads.
 */
class DaemonThreadFactory implements ThreadFactory {

    private final AtomicInteger THREAD_ID_COUNTER = new AtomicInteger();

    @Override
    public Thread newThread(Runnable r) {
        final Thread thread = new Thread(r);
        thread.setName("tcp-unix-socket-proxy-daemon-thread-" + THREAD_ID_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
