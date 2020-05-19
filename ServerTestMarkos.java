
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class ServerTestMarkos {
    static int SLOW_DOWN = 500;
    static int BYTES = 1024;
    static enum field{USER_INPUT,NUM_PACKET,TIME_STAMP};
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException, SocketException, IOException {
        
        for(NetworkInterface entry : Collections.list(NetworkInterface.getNetworkInterfaces()) ){
            System.out.println(entry.getName()+"**"+entry.getDisplayName());
        };
        RequestReplyAPI multi=null;
        int i=0;
        for(;i<1;i++){
            multi = new RequestReplyAPI("server", "windows");
            //multi.setDiscoveryMulticast("224.1.1.1", 8888, "wlan0","PING_"+i);
            multi.setDiscoveryMulticast("224.1.1.1", 8888, "wlan0");
            multi.register("PING");
        }
        byte[] data = new byte[BYTES];
        String endChar="@";
        long start,total;
        long timeReceived;
        long timeSent;
        long sendBack;
        start = System.currentTimeMillis();
        for(int x=0;x<99999999;x++){
            
                    byte[] buffer = new byte[1024];
                    int id = multi.getRequest("PING", buffer, buffer.length);
                    if(id<0){
                        x--;
                        if(id==-1);
                        else{System.out.println("Closing");multi.close();System.exit(1);}
                    }else{
                        String rec = new String(buffer,"utf-8");
                        String tmp =rec.substring(0, rec.indexOf(endChar));
                        String[] str= tmp.split(":");
                        timeReceived=System.currentTimeMillis();
                        timeSent = Long.parseLong(str[field.TIME_STAMP.ordinal()]);
                        System.out.println("~New job "+"\nTime to arrive:"+TimeUnit.MILLISECONDS.toSeconds(timeReceived-timeSent));
                        System.out.flush();
                        Thread.sleep(SLOW_DOWN);
                        if(str[field.USER_INPUT.ordinal()].equals("ping")){
                            sendBack=System.currentTimeMillis();
                            String helpMe = "pong"+":"+str[field.NUM_PACKET.ordinal()]+":"+timeSent+":"+timeReceived+":"+sendBack+endChar;
                            System.arraycopy(helpMe.getBytes(), 0, data, 0, helpMe.getBytes().length);
                            multi.sendReply(id, data, data.length);
                        }else{
                            multi.sendReply(id, "erro".getBytes(), "erro".getBytes().length);
                        }
                        i++;
                    }
            total = System.currentTimeMillis()-start;
            if(total>4000){
                ServerTestMarkos dummy = new ServerTestMarkos();
                Thread r = new Thread(dummy.new start());
                r.start();
            }
            System.out.println("total time : " + TimeUnit.MILLISECONDS.toSeconds(total) + " sec or "+ total+ " ms");
            System.out.println(x+" Im idiot");
            
        }
        
                System.out.println("Unregistered from service PING");
                multi.unregister("PING");
        System.out.println("Ending Server Session");
        multi.close();
    }
    public class start implements Runnable{

        @Override
        public void run() {
            try {
                ServerTestMarkos dummy = new ServerTestMarkos();
                ArrayList<Thread> sos = new ArrayList<>();
                for(int i=0;i<3;i++){
                    Thread r = new Thread(dummy.new Test());
                        r.start();
                        sos.add(r);
                        Thread.sleep(1000*60);
                }
                Thread.sleep(4*1000*60);
                for(Thread e: sos){
                    e.interrupt();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerTestMarkos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public class Test implements Runnable{

        @Override
        public void run() {
            RequestReplyAPI multi=null;
        int i=0;
        for(;i<1;i++){
                try {
                    multi = new RequestReplyAPI("server", "windows");
                    //multi.setDiscoveryMulticast("224.1.1.1", 8888, "wlan0","PING_"+i);
                    multi.setDiscoveryMulticast("224.1.1.1", 8888, "wlan0");
                    multi.register("PING");
                } catch (UnknownHostException ex) {
                    Logger.getLogger(ServerTestMarkos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(ServerTestMarkos.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        byte[] data = new byte[BYTES];
        String endChar="@";
        long start,total;
        long timeReceived;
        long timeSent;
        long sendBack;
        start = System.currentTimeMillis();
        for(int x=0;x<99999999;x++){
            
                try {
                    byte[] buffer = new byte[1024];
                    int id = multi.getRequest("PING", buffer, buffer.length);
                    if(id<0){
                        if(id==-1);
                        else{System.out.println("Closing");multi.close();System.exit(1);}
                    }else{
                        String rec = new String(buffer,"utf-8");
                        String tmp =rec.substring(0, rec.indexOf(endChar));
                        String[] str= tmp.split(":");
                        timeReceived=System.currentTimeMillis();
                        timeSent = Long.parseLong(str[field.TIME_STAMP.ordinal()]);
                        System.out.println("~New job "+"\nTime to arrive:"+TimeUnit.MILLISECONDS.toSeconds(timeReceived-timeSent));
                        System.out.flush();
                        Thread.sleep(SLOW_DOWN);
                        if(str[field.USER_INPUT.ordinal()].equals("ping")){
                            sendBack=System.currentTimeMillis();
                            String helpMe = "pong"+":"+str[field.NUM_PACKET.ordinal()]+":"+timeSent+":"+timeReceived+":"+sendBack+endChar;
                            System.arraycopy(helpMe.getBytes(), 0, data, 0, helpMe.getBytes().length);
                            multi.sendReply(id, data, data.length);
                        }else{
                            multi.sendReply(id, "erro".getBytes(), "erro".getBytes().length);
                        }
                        i++;
                    }
                    total = System.currentTimeMillis()-start;
                    
                    System.out.println("total time : " + TimeUnit.MILLISECONDS.toSeconds(total) + " sec or "+ total+ " ms");
                    System.out.println(x+" Im idiot");
                    
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ServerTestMarkos.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ServerTestMarkos.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
        System.out.println("Ending Server Session");
        multi.close();
        }
    
    }
}
