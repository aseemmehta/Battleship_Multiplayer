/*
Game Battleship
Game Class conatins the UI of the game, UI is a basic Terminal
and it also contains the information on who plays and what.
@author  Aseem Mehta   (am1435@rit.edu)
*/

import java.lang.*;
import java.util.Scanner;


public class Game {
  private static int grid = 10;
  public static String[][] otherMatrix = new String[grid][grid];
  public static String[][] matrix = new String[grid][grid];
  public static int player;
  public static int status; //4 for player ready, 5 to make Move, 6 to wait
  public static int ownStatus = 3;
  public static int score = 0;
  public static int OppScore = 0;

// To make it easy on the player old screen is cleared
  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

// Prints the opponent and user battleship realtime matrix
  public static void PrintMatrix(String[][] matrix, String[][] otherMatrix){
    clearScreen();
    System.out.println("                     Opponent                                Own            ");
    for(int i = 0; i<2; i++){
      System.out.print("         ");
      for(int idx = 0;idx<matrix.length;idx++){
        System.out.print("  "+idx);
      }
    }
    System.out.println();
    for(int row = 0;row<matrix.length;row++){
      System.out.print("        "+row);
      for(int col = 0;col<otherMatrix[0].length;col++){
        System.out.print(" "+otherMatrix[row][col]);
      }
      System.out.print("        "+row);
      for(int col = 0;col<matrix[0].length;col++){
        System.out.print(" "+matrix[row][col]);
      }
      System.out.println();

    }
  }

// Checks if a Ship can be placed at the spot
  public static boolean shipCheck(String[][] matrix, int row, int col, String placement, int shipSize){
    if(placement.equals("V")){
      if(!((row>=0 &&(row+shipSize-1)<matrix.length)&&(col>=0 && (col)<matrix.length))){
        return false;
      }
      //Checks for Vertical Placement of ship
      for(int i=0; i< shipSize; i++){
        if(!matrix[row+i][col].equals(" X")){
          return false;
        }
      }
    }else if(placement.equals("H")){
      if(!((row>=0 &&(row)<matrix.length)&&(col>=0 && (col+shipSize-1)<matrix.length))){
        return false;
      }
      //Checks for Horizontal Placement of ship
      for(int i=0; i< shipSize; i++){
        if(!matrix[row][col+i].equals(" X")){
          return false;
        }
      }
    }
    return true;
  }


  public static String[][] shipPlace(String[][] matrix, int row, int col, String placement, int shipSize, String name){
    // Helper function assigns ships to specific place
    if(placement.equals("V")){
      for(int i=0; i< shipSize; i++){
        matrix[row+i][col] = name;
      }
    }else{
      for(int i=0; i< shipSize; i++){
        matrix[row][col+i] = name;
      }
    }
    return matrix;
  }


  public static String[][] shipPlacement(String[][] matrix, int shipSize, String name, Scanner input){
    // Requests Ship placements and assigns at the spot
    int grid = matrix.length;
    int row;
    int col;
    String placement;

    while(true){
      try{
        System.out.print("Enter Ship row: ");
        row = input.nextInt();
        System.out.print("Enter Ship column: ");
        col = input.nextInt();
        System.out.print("Enter V for Vertical or H for Horizontal placement: ");
        placement = input.next().toUpperCase();
        if((placement.equals("V") || placement.equals("H")) && (shipCheck(matrix, row, col, placement,shipSize))) {
          break;
        }else{
          System.out.println("Enter Again");
        }
      }catch(Exception e){
        input.next();
        System.out.println("Enter Again");
      }
    }
    matrix = shipPlace(matrix, row, col, placement,shipSize,name);
    return matrix;
  }



// Initializes the Matrix at all X's to start the Game
  public static String[][] initializeGame(String[][] matrix){
    for(int row = 0;row<matrix.length;row++){
      for(int col = 0;col<matrix.length;col++){
        matrix[row][col] = " X";
      }
    }
    return matrix;
  }

  public static String[][] assignShips(String[][] matrix, int noOfShips, String[][] otherMatrix, Scanner input){
    for(int ship=1; ship<=noOfShips;ship++){
      matrix = shipPlacement(matrix, 2+ship, "S"+ship,input);
      PrintMatrix(matrix,otherMatrix);
    }
    return matrix;
  }


  // Checks if a Ship can be placed at the spot
    public static boolean hitMissCheck(String[][] matrix, int row, int col, String placement, int shipSize){
      if(placement.equals("V")){
        if(!((row>=0 &&(row+shipSize-1)<matrix.length)&&(col>=0 && (col)<matrix.length))){
          return false;
        }
        //Checks for Vertical Placement of ship
        for(int i=0; i< shipSize; i++){
          if(!matrix[row+i][col].equals(" X")){
            return false;
          }
        }
      }else if(placement.equals("H")){
        if(!((row>=0 &&(row)<matrix.length)&&(col>=0 && (col+shipSize-1)<matrix.length))){
          return false;
        }
        //Checks for Horizontal Placement of ship
        for(int i=0; i< shipSize; i++){
          if(!matrix[row][col+i].equals(" X")){
            return false;
          }
        }
      }
      return true;
    }

// function allows the correct player to make a move and restricts opponent to wait
  public static void move(Scanner input){
    while(true){
      System.out.print("");
      if(Game.status == 5){
        PrintMatrix(matrix,otherMatrix);
        int row;
        int col;
        System.out.println("Your Move");
        while(true){
          try{
            System.out.print("Enter hit row: ");
            row = input.nextInt();
            System.out.print("Enter hit column: ");
            col = input.nextInt();
            if(row>=0 && row < otherMatrix.length && col>=0 && col<otherMatrix[row].length && otherMatrix[row][col].equals(" X")) {
              break;
            }else{
              System.out.println("Enter Again");
            }
          }catch(Exception e){
            input.next();
            System.out.println("Enter Again");
          }
        }
        String output = "Code:05"+row+col;
        if(Game.OppScore>=7){
          System.out.println("You Lose!");
          break;
        }
        try{
          Sender.sendMessage(output);
          Game.status = 6;
          double t = System.currentTimeMillis();
          while(System.currentTimeMillis() - t<60000 && Game.status==6){
            System.out.print("");
          }
          if(Game.status ==7){
            Game.status = 6;
            PrintMatrix(matrix,otherMatrix);
          }else{
            System.out.println("Opponent took too long");
            break;
          }
          System.out.println("My Score: "+ Game.score);
          System.out.println("Opponent Score: "+ Game.OppScore);
          if(Game.score>=7){
            System.out.println("Winner!");
            break;
          }
          t = System.currentTimeMillis();
          while(System.currentTimeMillis() - t<60000 && Game.status==6){
            System.out.print("");
          }
        }catch(Exception e){
          e.printStackTrace();
        }
      }
    }
  }

  // Starts a timeout timer for the user
  public static void otherPlayerWait(){
    double t = System.currentTimeMillis();
    while(System.currentTimeMillis() - t<60000 && Game.status!=4){
      System.out.print("");
    }
    if(Game.status==4 &&Game.ownStatus>2){
      if(player==1){
        Game.status = 5;
      }else if(player==2){
        Game.status = 6;
      }
    }else{
      System.out.println("Opponent not ready");
    }
  }

// Starts the game
  public static void playGAME(){
    Scanner input = new Scanner(System.in);
    int noOfShips = 3;
    matrix = initializeGame(matrix);
    otherMatrix = initializeGame(otherMatrix);
    System.out.println("Game Initialized");
    PrintMatrix(matrix,otherMatrix);
    matrix = assignShips(matrix,noOfShips,otherMatrix, input);
    System.out.println("Ready, Informing other player");
    Game.ownStatus = 3;
    try{
      Sender.sendMessage("Code:04");
    }catch(Exception e){
      e.printStackTrace();
    }
    otherPlayerWait();
    System.out.println("Game Status: "+Game.status);
    if(Game.status!=0){
      move(input);
      System.out.println("Game Over");
    }else{
      System.out.println("Exit");
    }
    input.close();
  }
}
