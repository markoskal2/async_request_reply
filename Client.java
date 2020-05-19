
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class Client {

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            
            Socket skt = new Socket("192.168.1.5", 50000);
            OutputStream out =  skt.getOutputStream();
            
            InputStream in = skt.getInputStream();
            Scanner scan = new Scanner(System.in);
            String input ;
            while(true){
                
               input = scan.nextLine();
               if(input.equals("exit")){
                   break;
               }
               out.write(input.getBytes());
               out.flush();
            }
            skt.shutdownOutput();
            
            
            byte[] receive = new byte[1];
            int read;
            while((read = in.read(receive)) !=-1){
                System.out.println("Received "+read+" bytes: "+new String(receive,0,read));
            }
                skt.shutdownInput();
            
            /*
            out.close();
            skt.shutdownInput();
            skt.close();
            */
        }
        catch(IOException e) {
            e.printStackTrace();
            System.out.print("Whoops! It didn't work!\n");
        }
  }
    
}
