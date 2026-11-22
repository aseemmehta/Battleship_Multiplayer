/*
Game Battleship
Sender Class sends messages to Server and Opponents
@author  Aseem Mehta   (am1435@rit.edu)
*/


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.lang.*;

public class Sender implements Runnable  {

   public static int port = 63001; // port to send on
   public static volatile String address; // address to send on

   // standard constructor assigns the values to port and receivers IP address.
   public Sender(int thePort, String addressIP)
   {
      port = thePort;
      address = addressIP;
   }

   // returns the port number
   public static int getPort(){
     return port;
   }


   // function contains the basic structure to send messages
   public static void sendMessage(String message) throws IOException{
     DatagramSocket socket = new DatagramSocket();
     InetAddress group = InetAddress.getByName(address);

     byte[] msg = message.getBytes();
     DatagramPacket packet = new DatagramPacket(msg, msg.length,group, getPort());
     // let 'er rip
     socket.send(packet);
     socket.close();
   }

// Performs the first communication with Server to get opponents IP
   public void play(String message){
     try {
       sendMessage(message);
       // Waits 20 Sec for a Player to Connect before trying again
       Game.waitFor(() -> Game.state.get()!=Game.State.SEARCHING, 20000);
       // Code:03 Player 1 sends Code to the Opponent to connect
       if(Game.state.get()==Game.State.PAIRED && Game.player==1){
         sendMessage("Code:03");
       }else if(Game.state.get()==Game.State.SEARCHING){
         System.out.println("Player not Found");
       }
     }catch(Exception ex){
       ex.printStackTrace();
     }
   }

   public void program()throws IOException{
     System.out.println("Finding Opponent");
     // Code 01: Find Player
     play("Code:01");

   }

   // the thread runnable.
   @Override
   public void run(){
      try {
        int number = 0;
        // Max 5 tries given till the game tries to Find another Player
        while (Game.state.get()==Game.State.SEARCHING && number <5)
        {
          System.out.println("Finding Opponent");
          // Code 01: Find Player
          play("Code:01");

          Thread.sleep(1000);
          number++;
        }
        if(Game.state.get()==Game.State.PAIRED){
          System.out.println("######################################");
          System.out.println("Play Game Now");
          System.out.println("Opponent IP: "+Sender.address);
          System.out.println("######################################");
          Game.playGAME();
        }else{
          Game.state.compareAndSet(Game.State.SEARCHING, Game.State.NO_OPPONENT);
          System.out.println("Exit");
        }

      }catch(Exception ex){
        ex.printStackTrace();
      }
   }
}
