
/*
Game Battleship
Class connect different players with each other
@author  Aseem Mehta   (am1435@rit.edu)
*/



import java.net.*;
import java.io.*;
import java.util.*;
import java.net.InetAddress;

class Server
{
 public static void main(String args[])
  {
      System.out.println("Welcome to Battleship");
			try
        	{
				// Get our sending address (perhaps for filtering later...)
				InetAddress localhost = InetAddress.getLocalHost();
        byte[] pack = localhost.getAddress();

				// Starting Receiver
				System.out.println("Starting Receiver...");
        Thread listener = new Thread(new Listen(63001));
				listener.start();

				while(true){Thread.sleep(1000);}
			}
			catch(Exception ex)
			{
		          ex.printStackTrace();
			}
    }
}
