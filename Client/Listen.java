/*
Game Battleship
Listen Class receives all the messages and changes status as per the
Code returned. Codes help track the commands given between machines.
@author  Aseem Mehta   (am1435@rit.edu)
*/

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.lang.*;


public class Listen implements Runnable {

   public int port = 63001; // port to listen on
   public static String p2;

   // standard constructor assigns the values to port
   public Listen(int thePort)
   {
      port = thePort;
   }

// Based on the commands changes the status of the game
   public void program(String msg,String packAddress){
     if(msg.substring(0,7).equals("Code:03")){
       // Code:03 signifies Message received from Player 1
       Sender.address = packAddress;
       Sender.status = 1;
       Sender.status = 2;
       Game.player = 2;
     }else if(msg.substring(0,7).equals("Code:02")){
       // Code:02 signifies message received from Server, IP information about Player 2
       p2 = msg.substring(7);
       Sender.address = p2;
       Sender.status = 1;
       Game.player = 1;
     }else if(msg.substring(0,7).equals("Code:04")){
       // Code:04 signifies Opponent is ready and checks if own machine is Ready
       // to play the game
       double t = System.currentTimeMillis();
       while(System.currentTimeMillis() - t<60000 && Game.ownStatus==0){
         System.out.print("");
         // Timer to wait for 60 Sec or till the Status is changed
       }
       if(Game.ownStatus<3){
         System.out.println("Took too long");
       }else{
         Game.status = 4;
         try{
           if(Game.ownStatus==4){
             Sender.sendMessage("Code:04");
           }
         }catch(Exception e){
           e.printStackTrace();
         }
       }
     }else if(msg.substring(0,7).equals("Code:05")){
       // Code:05 signifies Opponent made their move
       int row = Integer.parseInt(msg.substring(7,8));
       int col = Integer.parseInt(msg.substring(8,9));
       if(Game.matrix[row][col].equals(" X")){
         Game.matrix[row][col] = "MM";
         System.out.println("Miss at "+row+col);
       }else{
         Game.matrix[row][col] = "HH";
         Game.OppScore++;
         System.out.println("Hit at "+row+col);
       }
       try{
         Game.status = 5;
         Sender.sendMessage("Code:07"+row+col+Game.matrix[row][col]);
       }catch(Exception e){
         e.printStackTrace();
       }
     }
     else if(msg.substring(0,7).equals("Code:07")){
       // Code:07 returns if the move was a Hit or a Miss
       int row = Integer.parseInt(msg.substring(7,8));
       int col = Integer.parseInt(msg.substring(8,9));
       Game.otherMatrix[row][col] = msg.substring(9);
       Game.status = 7;
       if(msg.substring(9).equals("HH")){
         Game.score++;
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
