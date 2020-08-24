/*
Game Battleship
Sender Class sends messages to Server and Opponents
@author  Aseem Mehta   (am1435@rit.edu)
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Sender implements Runnable  {

   public static int port = 63001; // port to send on
   public static String address; // address to send message on

   // standard constructor assigns the values to port and receivers IP address.
   public Sender(int thePort, String receiverIP)
   {
      port = thePort;
      address = receiverIP;
   }

   // function contains the basic structure to send messages
   public static void sendMessage(String message) throws IOException{
     DatagramSocket socket = new DatagramSocket();
     InetAddress group = InetAddress.getByName(address);
     byte[] msg = message.getBytes();
     DatagramPacket packet = new DatagramPacket(msg, msg.length,group, port);
     // let 'er rip
     socket.send(packet);
     socket.close();
   }

   // the thread runnable.
  @Override
  public void run(){
     try {
     }catch(Exception ex){
       ex.printStackTrace();
     }
  }
}
