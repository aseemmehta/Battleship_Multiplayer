
/*
Game Battleship
Client Class performs below functions
1. Connects to Server to find Opponent players
2. Connect different players with each other to enjoy a game of Battleship
@author  Aseem Mehta   (am1435@rit.edu)
*/



import java.net.*;
import java.io.*;
import java.util.*;
import java.net.InetAddress;

class Client
{
 public static void main(String args[])
  {
			try
        	{
        System.out.println("Welcome to Battleship");
        // Server Address, All users connect to the Server to find Opponents
        String servAddress = "172.18.0.21";

				// Starting Client Receiver
        Thread listener = new Thread(new Listen(63001));
        listener.start();

				// Starting Client Sender
				Thread sender=new Thread(new Sender(63001,servAddress));
				sender.start();
				while(true){Thread.sleep(1000);}
			}
			catch(Exception ex)
			{
		          ex.printStackTrace();
			}
    }
}
