/*
Game Battleship
Listen Class receives messages from Players and changes status
@author  Aseem Mehta   (am1435@rit.edu)
*/


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.lang.*;

public class Listen implements Runnable {

   public static int port = 63001; // port to listen on
   public static String p1_IP;// Player 1 IP Address
   public static  double p1_Time;

   // standard constructor assigns the values to port
   public Listen(int thePort)
   {
      port = thePort;
   }

   // Sends the IP of Player 2 to Player 1
   public void sendIP(String p1, String p2){
     try {
       Sender.address = p1;
       // Send Player 2 address to Player 1
       Sender.sendMessage("Code:02" + p2);
     }catch(IOException ex){
       ex.printStackTrace();
     }
   }

   // Decodes the messages based on codes 
   public void program(String msg, String packAddress){
     String b = msg;
     if (msg.substring(0,7).equals("Code:01")){
       if(p1_IP == null){
         p1_IP = packAddress;
         p1_Time = System.currentTimeMillis();
         // Start Timer in Backend
       }else if(!packAddress.equals(p1_IP)){
         System.out.println(p1_IP+"   "+packAddress);
         sendIP(p1_IP,packAddress);
         p1_IP = null;
       }
     }
   }

   // listens to the ipaddress and reports when a message arrived
   public void receiveUDPMessage() throws IOException {
      byte[] buffer=new byte[1024];
      // create and initialize the socket
      DatagramSocket socket=new DatagramSocket(port);

      while(true){
         DatagramPacket packet=new DatagramPacket(buffer,buffer.length);
	        // blocking call.... waits for next packet
         socket.receive(packet);
         InetAddress packetAddress = packet.getAddress();
         byte[] pack = packetAddress.getAddress();
         String packAddress = (pack[0]&0xFF)+"."+(pack[1]&0xFF)+"."+(pack[2]&0xFF)+"."+(pack[3]&0xFF);

         String msg=new String(packet.getData(),packet.getOffset(),packet.getLength());
         System.out.println(msg);
         program(msg,packAddress);

         // give us a way out if needed
         if("EXIT".equals(msg)) {
            System.out.println("No more messages. Exiting : "+msg);
            break;
        }
      }
      //close up ship
      socket.close();
    }

   // the thread runnable.  just starts listening.
   @Override
   public void run(){
     try {
       receiveUDPMessage();
     }catch(IOException ex){
       ex.printStackTrace();
     }
   }
}
