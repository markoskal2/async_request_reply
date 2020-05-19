
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class ReceiveM {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        int mcPort = 8888;
        String mcIPStr = "224.0.0.251";
        MulticastSocket mcSocket = null;
        InetAddress mcIPAddress = null;
        mcIPAddress = InetAddress.getByName(mcIPStr);
        mcSocket = new MulticastSocket(mcPort);
        
        mcSocket.joinGroup(mcIPAddress);
        System.out.println("Multicast Receiver running at: " + mcSocket.getLocalSocketAddress());

        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        try {
            MulticastSocket ms = new MulticastSocket(mcPort);
            ms.joinGroup(mcIPAddress);
            String s;
            while(true) {
              ms.receive(packet);
              s = new String(packet.getData(), 0, packet.getLength());
              System.out.println("Multicast Message: " + s + " from IP: " + packet.getAddress() + " port: " + packet.getPort());
            }
          } catch (SocketException e) {
            System.err.println(e);
          } catch (IOException e) {
            System.err.println(e);
          }
        mcSocket.leaveGroup(mcIPAddress);
        mcSocket.close();
    }
    
}
