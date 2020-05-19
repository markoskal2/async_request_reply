
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
public class helloWorldMarkos {
    static int MAX_WAIT=50;
    static int TRIES =3;
    
    static enum field{USER_INPUT,NUM_PACKET,TIME_STAMP1,TIME_STAMP2,TIME_STAMP3,TIME_STAMP4};
    /**
     * @param args the command line arguments CLIENT
     */
    public static void main(String[] args) throws InterruptedException{
        
        ArrayList<Thread> th = new ArrayList<>();
            helloWorldMarkos dummy = new helloWorldMarkos();
            Thread runner=new Thread(dummy.new Test());
            Thread temp=runner;
            runner.start();
            for(int i=0;i<4;i++){
                
                runner = new Thread(dummy.new Test());
                runner.start();
                th.add(runner);
                Thread.sleep(1000*60);
            }
            Thread.sleep(3*1000*60);
            for(Thread e : th){
                e.interrupt();
            }   
            temp.join();
    }
    public class Test implements Runnable {

        @Override
        public void run() {
            try {
                String endChar = "@";
                byte[] data = new byte[1024];
                long start,total;
                RequestReplyAPI multi = new RequestReplyAPI("client", "windows");
                multi.setDiscoveryMulticast("224.1.1.1", 8888, "wlan0");
                start = System.currentTimeMillis();
                long timeReceived;
                long timeSent;
                long sendBack;
                long finalTime;
                long totalTravelTime = 0;
                ArrayList<Integer> jobs = new ArrayList<>();
                int valid = 10;
                for(int i=0;i<10;i++){
                    
                    try{
                        timeSent=System.currentTimeMillis();
                        String send ="ping:"+i+":"+(timeSent)+endChar;
                        System.arraycopy(send.getBytes(), 0, data, 0, send.getBytes().length);
                        int req = multi.sendRequest("PING", data, data.length);
                        
                        if(req>0){
                            jobs.add(req);
                            
                        }else{
                            System.out.println("error");
                            valid--;
                        }
                    }
                    catch(TimeoutException e){
                        System.out.println("Timeout exception");
                        valid--;
                        
                    } catch (Exception ex) {
                    }
                    
                }
                for(int i=0;i<10;i++){
                    if(multi.getReply(jobs.get(i), data, data.length, MAX_WAIT*100000) == null){
                        valid--;
                        continue;
                    }
                    
                    String rec = new String(data,"utf-8");
                    rec=rec.substring(0, rec.indexOf(endChar));
                    String[] info = rec.split(":");
                    timeSent=Long.parseLong(info[field.TIME_STAMP1.ordinal()]);
                    timeReceived=Long.parseLong(info[field.TIME_STAMP2.ordinal()]);
                    sendBack=Long.parseLong(info[field.TIME_STAMP3.ordinal()]);
                    finalTime=System.currentTimeMillis();
                    totalTravelTime += finalTime - timeSent;
                    System.out.println("received packet ~"+info[field.USER_INPUT.ordinal()]+"~with slow down:"+"\n"+
                            "\tTime to get:"+TimeUnit.MILLISECONDS.toMillis(timeReceived-timeSent)+"ms\n"+
                            "\tTime stayed in server:"+TimeUnit.MILLISECONDS.toMillis(sendBack-timeReceived)+"ms\n"+
                            "\tTotal travel time:"+TimeUnit.MILLISECONDS.toMillis(finalTime - timeSent)+"ms\n"
                            + "\tAverage Time:"+TimeUnit.MILLISECONDS.toMillis(totalTravelTime/((i+1))) + "ms");
                }
                total = System.currentTimeMillis()-start;
                System.out.println("total time : " + TimeUnit.MILLISECONDS.toSeconds(total) + " sec or "+ total+ " ms");
                System.out.println("Total valid transactions "+ valid);
                multi.close();
            } catch (UnknownHostException ex) {
                Logger.getLogger(helloWorldMarkos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(helloWorldMarkos.class.getName()).log(Level.SEVERE, null, ex);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(helloWorldMarkos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    };
}

