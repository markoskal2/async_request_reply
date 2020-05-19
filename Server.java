
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
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
public class Server {
    
    static final int MINIMUM_WAIT = 25;
    ArrayList<Thread> workers = new ArrayList<>();
    
    public int createHandler(Socket client){
        Thread worker = new Thread(new ClientHandler(client));
        worker.start();
        workers.add(worker);
        return 0;
    }
    
    public class ClientHandler implements Runnable {
        Socket socket;
        String username=null;
        
        public ClientHandler(Socket client){
            socket=client;
        }
        
        @Override
        public void run() {
            //while(!Thread.interrupted()){
                try {
                    String input ;
                    InputStream in = socket.getInputStream();
                    byte[] receive = new byte[1];
                    int read;
                    while((read = in.read(receive)) !=-1 && !socket.isOutputShutdown()){
                        System.out.println("Received "+read+" bytes: "+new String(receive,0,read));
                    }
                    
                    OutputStream out =  socket.getOutputStream();
                    Scanner scan = new Scanner(System.in);
                    
                    while(true){

                       input = scan.nextLine();
                       if(input.equals("exit")){
                           break;
                       }
                       out.write(input.getBytes());
                       out.flush();
                    }
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("Client "+username+" died and Ι dont care about it");

                    //Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            //}
        }
    
    
    };
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        Server dummy = new Server();
        try {
            ServerSocket srvr = new ServerSocket(50000);
            
            while(true){
                Socket skt = srvr.accept();
                System.out.println("New client");
                dummy.createHandler(skt);
                if(false){
                    break;
                };
            }
           srvr.close();
        }
        catch(IOException e) {
            
            e.printStackTrace();
            System.out.print("Whoops! It didn't work!\n");
        }
   }
    
}
