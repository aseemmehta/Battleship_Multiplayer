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
     if(msg.startsWith("Code:03")){
       // Code:03 signifies Message received from Player 1, we are Player 2
       if(Game.state.get()==Game.State.SEARCHING){
         Sender.address = packAddress;
         Game.player = 2;
         Game.state.set(Game.State.PAIRED);
       }
     }else if(msg.startsWith("Code:02")){
       // Code:02 signifies message received from Server, IP information about Player 2
       if(Game.state.get()==Game.State.SEARCHING){
         p2 = msg.substring(7);
         Sender.address = p2;
         Game.player = 1;
         Game.state.set(Game.State.PAIRED);
       }
     }else if(msg.startsWith("Code:04")){
       // Code:04 signifies Opponent is ready. Only remember it here, Game decides when to start,
       // so this thread is never blocked and no reply is needed (both sides send Code:04).
       Game.oppReady = true;
     }else if(msg.startsWith("Code:05")){
       // Code:05 signifies Opponent made their move
       Game.State current = Game.state.get();
       if(current != Game.State.READY && current != Game.State.OPPONENT_TURN){
         return; // late or duplicate shot, not our turn to be shot at
       }
       int row = Integer.parseInt(msg.substring(7,8));
       int col = Integer.parseInt(msg.substring(8,9));
       String cell = Game.matrix[row][col];
       if(cell.startsWith("S")){
         Game.matrix[row][col] = "HH";
         Game.OppScore++;
         System.out.println("Hit at "+row+col);
       }else if(cell.equals(" X")){
         Game.matrix[row][col] = "MM";
         System.out.println("Miss at "+row+col);
       }
       try{
         Sender.sendMessage("Code:07"+row+col+Game.matrix[row][col]);
       }catch(Exception e){
         e.printStackTrace();
       }
       // All ships hit: we lose right now, we do not wait for our next turn
       Game.State next = Game.OppScore>=Game.TOTAL_SHIP_CELLS ? Game.State.LOST : Game.State.MY_TURN;
       Game.state.compareAndSet(current, next);
     }
     else if(msg.startsWith("Code:07")){
       // Code:07 returns if the move was a Hit or a Miss
       if(Game.state.get()!=Game.State.AWAITING_RESULT){
         return; // late or duplicate result
       }
       int row = Integer.parseInt(msg.substring(7,8));
       int col = Integer.parseInt(msg.substring(8,9));
       Game.otherMatrix[row][col] = msg.substring(9);
       if(msg.substring(9).equals("HH")){
         Game.score++;
       }
       Game.State next = Game.score>=Game.TOTAL_SHIP_CELLS ? Game.State.WON : Game.State.OPPONENT_TURN;
       Game.state.compareAndSet(Game.State.AWAITING_RESULT, next);
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
