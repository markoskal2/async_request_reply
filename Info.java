/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Markos
 */
public class Info {
    private String name;
    private String ip;
    private int port;
    private int busy;
    
    
    public Info(String name, String ip, int port,int busy){
        setName(name);
        setIp(ip);
        setPort(port);
        setBusy(busy);
    }
    
    public String getName(){return name;}
    public String getIp(){return ip;}
    public int getPort(){return port;}
    public int getBusy(){return busy;}
    @Override
    public String toString(){
        return "Svcid:\n\t"+name+"\n\t"+ip+":"+String.valueOf(port)+"/";
    }
    
    
    private void setPort(int port){this.port = port;}
    private void setName(String name){this.name =name;}
    private void setIp(String ip){this.ip = ip;}
    private void setBusy(int busy){this.busy=busy;}
    
}
