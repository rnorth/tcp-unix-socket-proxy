# TcpToUnixSocketProxy

Listens on a TCP port and proxies connections to a UNIX domain socket.

Like `socat TCP-LISTEN:2375,fork UNIX-CONNECT:/var/tmp/docker.sock`,
except this is literally the only thing this program does.

Purposefully simplistic in implementation, likely to be buggy or
suboptimal in performance, may contain nuts.
