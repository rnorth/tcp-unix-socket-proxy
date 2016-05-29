# TcpToUnixSocketProxy

Listens on a TCP port and proxies connections to a UNIX domain socket.

Like `socat TCP-LISTEN:2375,fork UNIX-CONNECT:/var/tmp/docker.sock`,
except this is literally the only thing this program does.

Purposefully simplistic in implementation, potentially buggy or
suboptimal in performance, may contain nuts.

This proxy uses Christian Kohlschütter's [junixsocket](https://github.com/kohlschutter/junixsocket) library
for interaction with Unix sockets.

## Rationale

This was implemented as a short term workaround for incompatibility between netty and Docker for Mac beta's
use of unix domain sockets on OS X - described [here](https://github.com/docker-java/docker-java/issues/537).

### Caveats

This proxy uses a deliberately simple blocking I/O with threads model, purely for simplicity. This clearly
eliminates the performance advantages of using netty in docker-java. However, I feel that in most cases the
performance of the docker API is not a critical factor, and a working but slow solution is preferable in the
short term. Still, once [kqueue support for netty](https://github.com/netty/netty/issues/2448) is in place, that
will become the better solution.

This library is only useful on OS X; Linux unix socket support through epoll is well supported by netty.

## Usage

Instantiate a proxy instance:

    TcpToUnixSocketProxy proxy = new TcpToUnixSocketProxy(new File("/var/run/docker.sock"));

Start it, and obtain the listening address (localhost with a random port by default):

    InetSocketAddress address = proxy.start();

Use the proxy by connecting to localhost on the port given by `address.getPort()`.

Then when the proxy is no longer needed:

    proxy.stop();

## License

See [LICENSE](LICENSE).

## Copyright

Copyright (c) 2016 Richard North.