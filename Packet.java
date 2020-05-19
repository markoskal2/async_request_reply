
import java.net.Socket;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class Packet {
    private int id;
    private byte[] data=null;
    private boolean ready;
    private String svcId=null ;
    private Socket socket=null;
    
    
    public Packet(int id, byte[] buffer,String svid){
        setId(id);
        this.svcId= svid;
        this.data = buffer;
    }
    public Packet(int id, byte[] buffer,Socket socket){
        this.socket=socket;
        setId(id);
        this.data = buffer;
    }
    
    public Packet(String  svid,byte[] buffer, Socket socket){
        this.data=buffer;
        this.socket=socket;
        this.svcId=svid;
    }
    
    public int getId(){return id;}
    
    public Socket getSocket(){return socket;}
    public byte[] getData(){
        if(data==null)
            return null;
        return data;
    }
    
    public String getSvcId(){return svcId;}
    public boolean isReady(){return ready;}
    
    public void setId(int id){this.id = id;}
    public void setData(byte[] buffer){this.data = buffer;}
    public void setSvcId(String svcid){this.svcId = svcid;}
    public void setReady(boolean val){this.ready=val;}
    
    public Packet getPacket(int id, ArrayList<Packet> packets){
        for(Packet entry : packets)
            if(entry.getId()==id) return entry;
        return null;
    }
    
    
}
