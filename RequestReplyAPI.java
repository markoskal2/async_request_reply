

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class RequestReplyAPI {
    static final int REFRESH = 1000;
    static final int WORKERS = 15;
    static final int MAX_CONNECTIONS = 5;
    static final int MINIMUM_WAIT = 25;
    static final int TRIES = 12;
    static final int SIZE = 6;
    static final int BUFFER_SIZE = 1024;
    static final int MAX_MSG=100;
    ArrayList<Info> services = null;
    private String user ;
    private PipedInputStream input ;
    private PipedOutputStream output;
    private Thread miniS;
    
    Map<String,ArrayList<Packet>> jobs = new ConcurrentHashMap<String,ArrayList<Packet>>();
    Map<Integer,Packet> given = new ConcurrentHashMap<Integer,Packet>();
    Map<Integer,Future<Packet>> sent = new ConcurrentHashMap<Integer,Future<Packet>>();
    Random random= new Random();
    ArrayList<String> registered = new ArrayList<>();
    long beginning=0;
    InetAddress ipAddr ;
    int port;
    String desInterface;
    Thread worker ;
    ArrayList<String> myIp= new ArrayList<>();
    ExecutorService exec;
    ExecutorService sendBack;
    static enum SERVICES{PINGPONG,HELLO};
    static enum field{,};
    String OS;
    public  RequestReplyAPI(String user,String OS) {
       this.OS=OS;
        this.user=user;
        exec = Executors.newCachedThreadPool();
        services = new ArrayList<>();
        if(user.equals("server")){
            sendBack = Executors.newCachedThreadPool();
        }
    } 
    
    public String getS(int svid){
        for(SERVICES c: SERVICES.values()){
            if(c.ordinal()==svid)
                return c.toString();
        }
        return null;
    }
    
    private int generateId(){
        int requestId = Math.abs(random.nextInt());
        while(given.containsKey(String.valueOf(requestId))){
            requestId = Math.abs(random.nextInt());
        }
        return requestId;
    }
        
    public int setDiscoveryMulticast(String ipAddr, int port) throws UnknownHostException  {
        this.ipAddr=InetAddress.getByName(ipAddr);
        this.port=port;
        return 0;
    }
    /*--------------------------------------------------------------*/

    public int setDiscoveryMulticast(String ipAddr, int port,String interf) throws UnknownHostException {
        this.ipAddr=InetAddress.getByName(ipAddr);
        this.port=port;
        this.desInterface=interf;
        return 0;
    }
    public Info isIn(ArrayList<Info> ser,String svcid){
        for(Info e: ser ){
            if(e.getName().equals(svcid)){
                return e;
            }
        }
        return null;
    }
    
    /*--------------------------------------------------------------*/
    public int sendRequest(String svcid,byte[] buffer, int len) throws Exception{
        byte[] temp = new byte[len];
        System.arraycopy(buffer, 0, temp, 0, len);
        Info e =null;
        Socket socket=null;
        while(true){
            if(System.currentTimeMillis()-beginning<=REFRESH)
                e = isIn(services,svcid);
            if(e==null){
                System.out.println("Search for service "+svcid);
                
                Callable<ArrayList<Info>> findS= new MultiFind(svcid);
                ArrayList<Info> res =findS.call();
                
                beginning=System.currentTimeMillis();
                if(res==null)
                    return -1;
                /*send it to the 1st service in list(with the desired name)*/
                for(Info er: res){
                    socket=connectTo(er.getIp(),er.getPort());
                    if(socket!=null){
                        e=er;
                        break;
                    }else
                        services.remove(er);

                }
                if(e==null){
                    return -1;
                }

            }else{
                socket=connectTo(e.getIp(),e.getPort());
                if(socket==null){
                    services.remove(e);
                    continue;
                }
            }
            break;
        }
        
//        System.out.println("Sending " +temp.length+" bytes request to service " +svcid+" at "+e.getIp()+":"+e.getPort());
        Callable<Packet> call = new Call(svcid, temp,socket);
        Future <Packet> future = exec.submit(call);
        
        int newId = generateId();
        sent.put(newId,future);
        return newId;
    }
    /*--------------------------------------------------------------*/
    public byte[] getReply(int reqid, byte[] buffer, int len, int timeout) throws InterruptedException{
        int lol=len;
        try{
            Future<Packet> future = sent.get(reqid);
            Packet in = future.get(timeout, TimeUnit.MILLISECONDS);
            if(lol>in.getData().length)
                lol=in.getData().length;
            System.arraycopy(in.getData(), 0, buffer, 0, lol);
            
            sent.remove(reqid);
            return buffer;
        }catch(ExecutionException e){
            e.printStackTrace();
        } catch (TimeoutException ex) {
            ex.printStackTrace();
        }
        
        System.out.println("Can't reach service");
        return null;
    }
    /*--------------------------------------------------------------*/
    public int register(String svcid) throws IOException{
        
        if(registered.contains(svcid))
            return -1;
        
        jobs.put(svcid, new ArrayList<Packet>());
//        ServerSocket srvr = new ServerSocket(0,MAX_CONNECTIONS);
        ServerSocket srvr = new ServerSocket(0);
        Thread newServe = new Thread(new MulticastBeacon(ipAddr, port,srvr.getLocalPort(), svcid,null),svcid);
        newServe.start();
        
        Thread worker = new Thread(new ConnServiceReceive(srvr),svcid+"_M");//master
        worker.start();
        
        registered.add(svcid);
        
        return 0;
    }
    /*--------------------------------------------------------------*/
    public int unregister(String svcid){
        
        if(!registered.contains(svcid))
           return -1;
        registered.remove(svcid);
        Thread run = findThreadByName(svcid);
        Thread run2 = findThreadByName(svcid+"_M");
        if(jobs.containsKey(svcid))jobs.remove(svcid);
        if(given.containsKey(svcid))given.remove(svcid);
//        if(toSend.containsKey(svcid))toSend.remove(svcid);
        run.interrupt();
        run2.interrupt();
        
        return 0;
    }
    /*--------------------------------------------------------------*/
    public int getRequest(String svcid,byte[] buffer, int len) throws UnsupportedEncodingException{
        
        if( !registered.contains(svcid)){
            System.out.println("Not registered");
            return -2;
        }
        if(!jobs.containsKey(svcid) || jobs.get(svcid).size()==0) {
            return -1;
        }

        /*see if the connection for this request is corrupted.
        In case it is remove it from list of jobs and move on 
        to the next one*/
        ArrayList<Packet> et=jobs.get(svcid);
        Packet to=null;
        while(true) {
            Packet e = et.get(0);
            
            if(e != null && reachable(e.getSocket().getInetAddress().getHostAddress() ,MINIMUM_WAIT*100)) {
                to=e;
                break;
            }else{
                jobs.get(svcid).remove(e);
            }
            if(et.size() == 0)
                break;
                
        };
        if(to==null){
            
            return -1;
        }
        int requestId = Math.abs(random.nextInt());
        while(given.containsKey(String.valueOf(requestId))){
            requestId = Math.abs(random.nextInt());
        }
        if(len>to.getData().length)
            len=to.getData().length;
        
//        System.out.println("#"+requestId +" "+new String(to.getData(),"utf-8"));
        
        given.put(requestId,new Packet(svcid,to.getData(), to.getSocket()));
        
        System.arraycopy(to.getData(), 0, buffer, 0, len);
        jobs.get(svcid).remove(to);
        
        return requestId;
    }
    /*--------------------------------------------------------------*/
    public void sendReply(int requestId,byte[] buffer, int len){
        
        if(!given.containsKey(requestId)){
            //System.out.println("No requests with this id waiting to be proccessed");
            return ;
        }
        Packet g = given.get(requestId);
        given.remove(requestId);
        byte[] temp = new byte[len];
        System.arraycopy(buffer, 0, temp, 0, len);
        
        g.setData(temp);
        Thread send = new Thread(new Sender(g));
        sendBack.execute(send);
    };
    /*--------------------------------------------------------------*/
    public Thread findThreadByName(String name){
        //Give you set of Threads
        Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();

        //Iterate over set to find yours
        for(Thread thread : setOfThread){
            if(thread.getName().equals(name)){
                return thread;
            }
        }
        return null;
    }
    /*--------------------------------------------------------------*/
    public class MulticastBeacon implements Runnable {
        String ServiceName=null;
        Socket socket;
        String myName=null;
        String interf;
        InetAddress ipAddr;
        int port,count;
        int listen_port;
        
        ArrayList<String> myIp;
        String findS=null;
        
        public MulticastBeacon(InetAddress ipAddr, int port,int listen_port,String interf,String findS){
            this.myIp = new ArrayList<>();
            this.interf = interf;
            this.findS = findS;
            this.listen_port=listen_port;
            this.ipAddr = ipAddr;
            this.port = port;

        }
        
        @Override
        public void run() {
            this.ServiceName=Thread.currentThread().getName();
            
            
            try(
                MulticastSocket ms = new MulticastSocket(port);
                ) {

                ms.joinGroup(new InetSocketAddress(ipAddr, port), NetworkInterface.getByName(interf));
                ms.setLoopbackMode(false);
                
                InetAddress inet = InetAddress.getLocalHost();
                InetAddress[] ips = InetAddress.getAllByName(inet.getCanonicalHostName());
                
                if (ips  != null ) {
                  for (int i = 0; i < ips.length; i++) {
                    myIp.add(ips[i].getHostAddress()+":"+String.valueOf(listen_port)+":"+ServiceName);
                  }
                }
                System.out.println("Start service beacon:"+ServiceName +" at "+ms.getLocalAddress()+":"+ms.getLocalPort()+":"+ms.getInterface().getHostName());
                /* 0=join/memb 1=ServiceName 2=port 3=busyness*/
                String str= ("join:"+ ServiceName+":"+String.valueOf(listen_port)+":"+jobs.get(ServiceName).size());
                byte[] rec2 = new byte[MAX_MSG];
                System.arraycopy(str.getBytes(), 0, rec2, 0, str.getBytes().length);
                DatagramPacket packetIn = new DatagramPacket(new byte[MAX_MSG], (new byte[MAX_MSG]).length); //tin prwti fora kanoume apostolh aytou kai tis upoloipes lambanoume edw ta mhnymata
                DatagramPacket packetOut = new DatagramPacket(rec2, rec2.length,ipAddr,port);
                
//                packetOut.setData(("join:"+ ServiceName+":"+String.valueOf(listen_port)).getBytes());
                
                packetOut.setData(str.getBytes());
                str= ("memb:"+ ServiceName+":"+String.valueOf(listen_port)+":"+jobs.get(ServiceName).size());
                ms.send(packetOut);
                packetOut.setData(str.getBytes());
                
                String[] prop;
                String newEntry;
                
                long start,end;
                while(!Thread.interrupted()) {
                    start = System.currentTimeMillis();
                    ms.receive(packetIn);
                    
                    prop = ( new String (packetIn.getData(),0,packetIn.getLength())).split(":");
                    if(prop[1].equals("client")){
                        packetOut.setData((str+":"+jobs.size()).getBytes());
                        ms.send(packetOut);
                        continue;
                    }
                    newEntry = packetIn.getAddress().getHostAddress()+":"+prop[2]+":"+prop[1];
                    Info newService  = new Info(prop[1], packetIn.getAddress().getHostAddress(), Integer.parseInt(prop[2]),Integer.parseInt(prop[3]));
                    
                    if(myIp.contains(newEntry)){
                        //dont care about mine
                    }else if(prop[0].equals("join")){
                        if(!services.contains(newService)&& !newService.getName().equals("client")){
                            services.add(newService);   
                        }
                        packetOut.setData((str+":"+jobs.size()).getBytes());
                        ms.send(packetOut);
                    }else if(prop[0].equals("memb") &&  !containsInfo(services, newService)){
                        services.add(newService);
                        System.out.println(ServiceName+".Added new client "+ newEntry+"\n"+newService.toString());
                    }
                }
                ms.leaveGroup(ipAddr);
                ms.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            
        }
    
    
    };
    
    public void close(){
        this.
        exec.shutdownNow();
        if(user.equals("server"))
            sendBack.shutdownNow();
    }
    
    public boolean containsInfo(ArrayList<Info> dict,Info search){
        
        for(Info e : dict){
            if(e.getIp().equals(search.getIp()) && e.getName().equals(search.getName())&& e.getPort()==search.getPort()){
                return true;
            }
        }
        return false;
    }
    
    public class Call implements Callable<Packet>{
        String service;
        byte[] dataToSend;
        String ip;
        Socket skt;
        int port;
        public Call (String service,byte[] data,Socket socket){
            this.service=service;
            this.dataToSend=data;
            this.skt=socket;
            this.ip= ip;
            this.port = port;
        }
        @Override
        public Packet call() {
            
            for(int i=0;i<TRIES;i++){
                try (
                    OutputStream out = skt.getOutputStream();
                    InputStream in = skt.getInputStream();){
                    
                    
                    byte[] numB = new byte[4];
                    int read;
                    /*send data*/
                    
                    
                    System.out.println("Try "+(i+1) +": "+ new String(dataToSend,"utf-8"));
                    out.write(toByteArray(dataToSend.length));
                    out.write(dataToSend);
                    out.flush();
                    /*make service understand that we send all the data for this request*/
                    while(in.read(numB,0,4)==-1){};
                    if((new String(numB,"utf-8")).equals("done") ){
                        
                        skt.shutdownOutput();


                        read = in.read(numB, 0, 4);

                        byte[] buffer = new byte[fromByteArray(numB, 0)];

                        in.read(buffer, 0, buffer.length);
                        return (new Packet(0, buffer, service));
                    }else{
                        System.out.println((new String(numB,"utf-8")));
                        }
                }catch(UnknownHostException ar){
                } catch (IOException ex) {
                }
                
            }
            return null;
        }
    
    
    }
    
    public int fromByteArray(byte[] bytes,int index) {
        return bytes[index + 0] << 24 | (bytes[index +1] & 0xFF) << 16 | (bytes[index +2] & 0xFF) << 8 | (bytes[index +3] & 0xFF);
    }
    //----------------------------------------------------------------------------------------------//
    public byte[] toByteArray(int value) {
        return new byte[] { 
            (byte)(value >> 24),
            (byte)(value >> 16),
            (byte)(value >> 8 ),
            (byte)(value      )};
    }
    
    public boolean reachable(String address,int timeout){
        try {
            // in case of Linux change the 'n' to 'c'
            String tmp;
            if(OS.equals("windows"))
                tmp = "-n";
            else
                tmp = "-c";
            Process p1 = java.lang.Runtime.getRuntime().exec("ping " + tmp + " 1 "+ address);
            return p1.waitFor(timeout,TimeUnit.MILLISECONDS);
        } catch (IOException ex) {}catch (InterruptedException ex) {}
        return false;
    }
    
    public Socket connectTo(String address,int port){
        try {
            Socket skt = new Socket(address,port);
            return skt;
        } catch (IOException ex) {}
        return null;
    
    }
    
    public class MultiFind implements Callable<ArrayList<Info>>{
        String serviceToFind;
        ArrayList<Info> found ;
        
        public MultiFind(String findService){
            this.serviceToFind=findService;
            services = new ArrayList<>();
            found = new ArrayList<>();
        }
        
        @Override
        public ArrayList<Info> call() throws Exception {
            try(
                MulticastSocket ms = new MulticastSocket(port);
                ) {

                ms.joinGroup(new InetSocketAddress(ipAddr, port), NetworkInterface.getByName(desInterface));
                ms.setLoopbackMode(false);
                
                InetAddress inet = InetAddress.getLocalHost();
                InetAddress[] ips = InetAddress.getAllByName(inet.getCanonicalHostName());
                
                if (ips  != null ) {
                  for (int i = 0; i < ips.length; i++) {
                      if(!myIp.contains(ips[i].getHostAddress()+":"+String.valueOf(port)+":"+"client"))
                            myIp.add(ips[i].getHostAddress()+":"+String.valueOf(port)+":"+"client");
                  }
                }
                byte[] temp1 = 	("join:"+ "client").getBytes();
                byte[] temp2 = 	("memb:"+ "client").getBytes();
                DatagramPacket packetIn = new DatagramPacket(new byte[MAX_MSG], (new byte[MAX_MSG]).length); //tin prwti fora kanoume apostolh aytou kai tis upoloipes lambanoume edw ta mhnymata
                DatagramPacket packetOut = new DatagramPacket(temp1, temp1.length,ipAddr,port);
                
//                packetOut.setData(temp1);
                ms.send(packetOut);
//                packetOut.setData(temp2);
                packetOut.setData(temp2);
                
                String[] prop;
                String newEntry;
                
                int count=0;
                ms.setSoTimeout(MINIMUM_WAIT*100);

                long start,end;
                while(!Thread.interrupted()) {
                    start = System.currentTimeMillis();
                    System.out.println("Wait for data");
                    ms.receive(packetIn);
                    String receiver=new String (packetIn.getData(),0,packetIn.getLength());
                    prop = receiver.split(":");
                    System.out.println("###"+receiver);
                    for(String e: prop)
                        System.out.println("~"+e);
                    if(prop[1].equals("client"))
                        continue;
                    newEntry = packetIn.getAddress().getHostAddress()+":"+prop[2]+":"+prop[1];
                    Info newService  = new Info(prop[1], packetIn.getAddress().getHostAddress(), Integer.parseInt(prop[2]),Integer.parseInt(prop[3]));
                    System.out.println(newService.toString());
                    
                    if(myIp.contains(newEntry)){
                        //dont care about mine
                    }else if(prop[0].equals("join")){
                        if(!services.contains(newService)){
                            if(newService.getName().equals(serviceToFind)){
                                found.add(newService);
//                                System.out.println(".#Added new service "+ newEntry+"\n"+newService.toString());
                            }
                            services.add(newService);
                            end = System.currentTimeMillis();
                            if(end-start>MINIMUM_WAIT || count>TRIES){
                                System.out.println("GOOD");
                                break;
                            }
                            count++;
                        }
                        ms.send(packetOut);
                    }else if(prop[0].equals("memb") &&  !containsInfo(services, newService)){
                        if(newService.getName().equals(serviceToFind)){
                            found.add(newService);
                            System.out.println("#Added new service LOL");
                        }
                        services.add(newService);
//                        System.out.println(".Added new service "+ newEntry+"\n"+newService.toString());
                        end = System.currentTimeMillis();
                        if(end-start>MINIMUM_WAIT || count>TRIES){
                            System.out.println("GOOD");
                            break;
                        }
                        count++;
                        
                    }
                }
                ms.leaveGroup(ipAddr);
                ms.close();
            } catch (IOException ex) {
                System.out.println("Ended searching for service");
            }
            
            Collections.sort(found, new Comparator<Info>(){
                @Override
                public int compare(Info o1, Info o2) {
                    return (String.valueOf(o1.getBusy())).compareTo(String.valueOf(o2.getBusy()));
                }
            });
            
            return found;
            
        }
    }
    
    public ArrayList<Info> sort(ArrayList<Info> info){
        
        Collections.sort(info,null);
        
        return null;
    }
    
    public class ConnServiceReceive implements Runnable{
        int port,maxClients ;
        ServerSocket srvr;
        String slaveTo;
        ArrayList<Thread> children ;
        public ConnServiceReceive(ServerSocket socket){
            this.srvr=socket;
            children=new ArrayList<>();
        }
        
        @Override
        public void run() {
            slaveTo= Thread.currentThread().getName();
            slaveTo=slaveTo.replaceAll("_M", "");
            try {
                int total=0;
                while(!Thread.interrupted()){
                    
                    Socket skt = srvr.accept();
                    total++;
                    System.out.println("New connection on service "+slaveTo);
                    Thread client = new Thread(new ClientHandler(skt,slaveTo));
                    client.start();
                    children.add(client);
                    if(total%10==0)
                        for(int i=0;i<children.size();i++){
                            if(!children.get(i).isAlive())
                                children.remove(i);
                    }
                }
                srvr.close();
            } catch (IOException ex) {ex.printStackTrace();
            }
            System.out.println("total conne");
            for(int i=0;i<children.size();i++)
                    children.get(i).interrupt();
        }
    
    
    }
    
    public class ClientHandler implements Runnable {
        Socket socket;
        String name;
        
        public ClientHandler(Socket socket,String name){
            this.socket=socket;
            this.name=name;
        }
        
        @Override
        public void run() {
            
            for(int i=0;i<TRIES;i++){
                try {
                    String input ;
                    InputStream in = socket.getInputStream();
                    
                    OutputStream out =  socket.getOutputStream();
                    byte[] numB = new byte[4];
                    
                    while(in.read(numB, 0, numB.length)==-1){};
                    byte[] buffer = new byte[fromByteArray(numB, 0)];
//                    System.out.println("!!"+buffer.length);
                    
                    if(in.read(buffer, 0, buffer.length)==buffer.length){
                        out.write("done".getBytes());
                    }else{
                        out.write("erro".getBytes());
                        continue;
                    };

                    Packet newP = new Packet(0, buffer,socket);
//                    if(!jobs.containsKey(name))
//                    System.out.println("new jobs added "+ new String(newP.getData(),"utf-8"));
                    jobs.get(name).add(newP);
                    break;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.out.println("Client died and Î™ dont care about it");
                    break;
                }
            }
            
        }
        
        
       
            
    
    
    };
    
    public class MyRandom extends Random{
        public MyRandom(){};
        public int nextNonNegative(){return next(Integer.SIZE-1);}
    }
    public class Sender implements Runnable {
        Packet toSend;
        
        public Sender(Packet packeto){
            this.toSend=packeto;
        }

        @Override
        public void run() {
            OutputStream out =null;
            try {
//                System.out.println("Send reply: "+ new String(toSend.getData(),"utf-8"));
                out = toSend.getSocket().getOutputStream();
                out.write(toByteArray(toSend.getData().length));
                out.write(toSend.getData());
            } catch (IOException ex) {
                ex.printStackTrace();
                //if client dies we dont care just move on with the next sending
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    //same here
                }
            }
        }
        
        
    };
}
