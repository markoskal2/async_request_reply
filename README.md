# Asynchronous request-reply between many clients/servers (RPC)

An asynchronous request-reply protocol implementation supporting asynchronous communication scheme. 
Client discovers server using UDP multicast, and communicates with the server over TCP/IP.

When a server is discovered, transmiting requests and getting replies between the two sides
is over TCP/IP.

There can be scenarios where the server crashes while it is executing the RPC call, thus client should be
aware of what was the state of the server before it crashed, so that it will know what action to 
take when the server comes up. This can be handled by different semantics provided by the RPC system.
In this implementation at most once semantics is used by the client side, while the server handles clients 
failures without assuming restart.

Client can discover/communicate with many servers wile requests are distributed uniformly to the available servers.

Tested with a ping-pong application where client sends 10 requests back-to-back without delay and waits server replies.
Server replies after waiting T = 500 ms. Servers and clients are added automatically after T = 1 min.




