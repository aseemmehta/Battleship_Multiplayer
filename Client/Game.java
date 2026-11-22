/*
Game Battleship
Game Class conatins the UI of the game, UI is a basic Terminal
and it also contains the information on who plays and what.
@author  Aseem Mehta   (am1435@rit.edu)
*/

import java.lang.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;


public class Game {
  // Every state a client can be in. Sender moves the client out of SEARCHING,
  // Listen reacts to network messages, Game handles user input and timeouts.
  public enum State {
    SEARCHING, PAIRED, PLACING_SHIPS, READY,
    MY_TURN, AWAITING_RESULT, OPPONENT_TURN,
    WON, LOST, TIMEOUT, OPPONENT_NOT_READY, NO_OPPONENT
  }

  private static int grid = 10;
  private static final int NO_OF_SHIPS = 3;
  // Ships have sizes 3, 4 and 5, so a player wins by hitting every one of the 12 ship cells
  public static final int TOTAL_SHIP_CELLS = totalShipCells(NO_OF_SHIPS);
  // How long we wait for the opponent (ready signal, result, next move)
  public static volatile int timeoutMs = 60000;

  public static String[][] otherMatrix = new String[grid][grid];
  public static String[][] matrix = new String[grid][grid];
  public static volatile int player;          // 1 = starts, 2 = waits for first shot
  public static final AtomicReference<State> state = new AtomicReference<State>(State.SEARCHING);
  public static volatile boolean oppReady = false; // set by Listen on Code:04
  public static volatile int score = 0;       // hits I made on the opponent
  public static volatile int OppScore = 0;    // hits the opponent made on me

  // Sum of ship sizes: ship n has size 2+n
  public static int totalShipCells(int noOfShips){
    int total = 0;
    for(int ship=1; ship<=noOfShips; ship++){
      total += 2+ship;
    }
    return total;
  }

  // Polls a condition until it is true or the time runs out. Returns true if the condition became true.
  public static boolean waitFor(BooleanSupplier condition, long millis){
    long end = System.currentTimeMillis() + millis;
    while(!condition.getAsBoolean()){
      if(System.currentTimeMillis() >= end){
        return false;
      }
      try{
        Thread.sleep(50);
      }catch(InterruptedException e){
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return true;
  }

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

// Prints both boards and the current score
  public static void showBoard(){
    PrintMatrix(matrix,otherMatrix);
    System.out.println("My Score: "+ score +" / "+ TOTAL_SHIP_CELLS);
    System.out.println("Opponent Score: "+ OppScore +" / "+ TOTAL_SHIP_CELLS);
  }

  // Asks the player for a cell that has not been shot at yet
  public static int[] readShot(Scanner input){
    while(true){
      try{
        System.out.print("Enter hit row: ");
        int row = input.nextInt();
        System.out.print("Enter hit column: ");
        int col = input.nextInt();
        if(row>=0 && row < otherMatrix.length && col>=0 && col<otherMatrix[row].length && otherMatrix[row][col].equals(" X")) {
          return new int[]{row, col};
        }else{
          System.out.println("Enter Again");
        }
      }catch(Exception e){
        input.next();
        System.out.println("Enter Again");
      }
    }
  }

// Main game loop. Runs until the state machine reaches WON, LOST or TIMEOUT.
// The Listen thread makes the transitions caused by network messages.
  public static void move(Scanner input){
    while(true){
      State s = state.get();
      switch(s){
        case MY_TURN: {
          showBoard();
          System.out.println("Your Move");
          int[] shot = readShot(input);
          // Change state BEFORE sending, so a fast Code:07 reply can not be overwritten
          state.set(State.AWAITING_RESULT);
          try{
            Sender.sendMessage("Code:05"+shot[0]+shot[1]);
          }catch(Exception e){
            e.printStackTrace();
          }
          break;
        }
        case AWAITING_RESULT:
          // Listen moves us on when Code:07 arrives (OPPONENT_TURN, or WON on the last hit)
          if(!waitFor(() -> state.get()!=State.AWAITING_RESULT, timeoutMs)){
            state.compareAndSet(State.AWAITING_RESULT, State.TIMEOUT);
          }
          break;
        case OPPONENT_TURN:
          showBoard();
          // Listen moves us on when Code:05 arrives (MY_TURN, or LOST on the last hit)
          if(!waitFor(() -> state.get()!=State.OPPONENT_TURN, timeoutMs)){
            state.compareAndSet(State.OPPONENT_TURN, State.TIMEOUT);
          }
          break;
        case WON:
          showBoard();
          System.out.println("Winner!");
          return;
        case LOST:
          showBoard();
          System.out.println("You Lose!");
          return;
        case TIMEOUT:
          System.out.println("Opponent took too long");
          return;
        default:
          return;
      }
    }
  }

// Starts the game
  public static void playGAME(){
    Scanner input = new Scanner(System.in);
    state.set(State.PLACING_SHIPS);
    matrix = initializeGame(matrix);
    otherMatrix = initializeGame(otherMatrix);
    System.out.println("Game Initialized");
    PrintMatrix(matrix,otherMatrix);
    matrix = assignShips(matrix,NO_OF_SHIPS,otherMatrix, input);
    System.out.println("Ready, Informing other player");
    state.set(State.READY);
    try{
      Sender.sendMessage("Code:04");
    }catch(Exception e){
      e.printStackTrace();
    }
    // Wait for the opponent's Code:04 (or for their first shot, which also proves they are ready)
    boolean opponentReady = waitFor(() -> oppReady || state.get()!=State.READY, timeoutMs);
    if(opponentReady){
      // Player 1 shoots first. compareAndSet keeps a first shot that Listen already handled.
      state.compareAndSet(State.READY, player==1 ? State.MY_TURN : State.OPPONENT_TURN);
      move(input);
      System.out.println("Game Over");
    }else{
      state.set(State.OPPONENT_NOT_READY);
      System.out.println("Opponent not ready");
    }
    input.close();
  }
}
