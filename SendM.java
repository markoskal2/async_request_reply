
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class SendM {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        int mcPort = 8888;
        String mcIPStr = "224.0.0.251"; //NA ALLAKSEI SE 224.1.1.1 OTAN EINAI NA TO TREXOUME SE LINUX
        MulticastSocket multiMix = new MulticastSocket(mcPort);

        
        
        InetAddress mcIPAddress = InetAddress.getByName(mcIPStr);
        //multiMix.setTimeToLive(0);
        multiMix.joinGroup(mcIPAddress);
        byte[] msg = "client".getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, mcIPAddress, mcPort);
        packet.setAddress(mcIPAddress);
        packet.setPort(mcPort);
        for(int i=0;i<10;i++){
            System.out.println("Sent a  multicast message.");
            Thread.sleep(200);
            multiMix.send(packet);
        }
        multiMix.leaveGroup(mcIPAddress);
        multiMix.close();

        System.out.println("Exiting application");
    }
    
}
