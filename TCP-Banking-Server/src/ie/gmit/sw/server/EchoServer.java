package ie.gmit.sw.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EchoServer {
  public static void main(String[] args) throws Exception {
    ServerSocket m_ServerSocket = new ServerSocket(2004);
    int id = 0;
    while (true) {
      Socket clientSocket = m_ServerSocket.accept();
      ClientServiceThread cliThread = new ClientServiceThread(clientSocket, id++);
      cliThread.start();
    }
    
  }
}

/**
 * Inner class used for a customers account
 * 
 */
//An inner class for account
  class Account {
 // Create a new lock
 private static Lock lock = new ReentrantLock();

 // Create a condition
 private static Condition newDeposit = lock.newCondition();

 private double balance = 0;

 public double getBalance() {
   return balance;
 }

 public boolean withdraw(double amount) {
   lock.lock();
   System.out.println("Locked the account for "+Thread.currentThread().getName());
   try {
     if (balance < (amount-1000)){
    	 System.out.println("Insufficient funds even with credit limit");
    	 return false;
     }
     else{
	   balance -= amount;
	   System.out.println("\t\t\tWithdraw " + amount +
	     "\t\t" + getBalance());
	   	return true;
	   
     }
   }
   finally {
     lock.unlock(); 
   }
 }

 public boolean deposit(double amount) {
   lock.lock(); 
   System.out.println("Locked the account for "+Thread.currentThread().getName());
   try {
     balance += amount;
     System.out.println("Deposit " + amount +
       "\t\t\t\t\t" + getBalance());
     
     // Signal thread 
     newDeposit.signalAll();
     return true;
   }
   finally {
	 System.out.println("Unlocked account for other threads");
     lock.unlock();
   }
 }

public void setBalance(double balance2) {
	this.balance = balance2;
}
}// end Account class


class ClientServiceThread extends Thread {
  private Socket clientSocket;
  private String message;
  private int clientID = -1;
  private boolean running = true;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private boolean authenticated=false;
  /*
   * These are the user variables.
   * Started by using a Inner class however the varaible within were not thread safe
   * Local variables are thread safe however.
   */
  private String name;
  private String address;
  private String accnum;
  private String username;
  private String password;
  private double balance;
  private static Account account = new Account();
  

  ClientServiceThread(Socket s, int i) {
    clientSocket = s;
    clientID = i;
  }

  void sendMessage(String msg)
	{
		try{
			out.writeObject(msg);
			out.flush();
			System.out.println("server> " + msg);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
  public void run() {
    System.out.println("Accepted Client : ID - " + clientID + " : Address - "
        + clientSocket.getInetAddress().getHostName());
    try 
    {
    	out = new ObjectOutputStream(clientSocket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(clientSocket.getInputStream());
		System.out.println("Accepted Client : ID - " + clientID + " : Address - "
		        + clientSocket.getInetAddress().getHostName());
		
		sendMessage("Connection successful");
		do{
			try
			{
				
				System.out.println("client>"+clientID+"  "+ message);
				
				//if (message.equals("bye"))
				while(authenticated == false){
					
						authenticated = loginMenu();
						
						if(message.equals("bye")){
							break;
						}
				
				}
				if (authenticated ==true){
					//presentCustomerMenu
					customerMenu();
				}
				
				
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			
    	}while(!message.equals("bye"));
      
		System.out.println("Ending Client : ID - " + clientID + " : Address - "
		        + clientSocket.getInetAddress().getHostName());
		
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  void customerMenu() throws ClassNotFoundException, IOException{
	  int choice;
	  String loginMenu = "\n Enter choice:  \n"
				+ "1. Change Customer Details  \n"
				+ "2. View Last 10 trasnactions  \n"
				+ "3. Deposit  \n"
				+ "4. Withdraw  \n"
				+ "5. Quit  \n";
		String starterMessage = " _____________________Acme ATM Company___________________\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|\tWelcome to Gordon Allied National Bank\t\t|\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|_______________________________________________________|"+loginMenu;
		sendMessage(starterMessage);
	    
		
		sendMessage("\n\n Your Balance is :"+String.format( "%.2f", account.getBalance() ));
		
		message = (String)in.readObject();
		choice = new Integer(message);
		
		switch(choice){
		case 1:
			//change customer details
			sendMessage("Change Customer Details");
			
			sendMessage("Current Name: "+ name+"\n Enter new name: ");
			message = (String)in.readObject();
			System.out.println(message);
			name = message;
			
			sendMessage("Current Address: "+ address+"\n Enter new address: ");
			message = (String)in.readObject();
			System.out.println(message);
			address = message;
			
			sendMessage("Current Account Number: "+ accnum+"\n Enter new account number (We find your customer record via username): ");
			message = (String)in.readObject();
			System.out.println(message);
			accnum = message;
			
			File file = new File("userDetails.csv");
			// Creates a random access file stream to read from, and optionally to write to
			
            FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

 

            // Acquire an exclusive lock on this channel's file (blocks until lock can be retrieved)

           
            // Attempts to acquire an exclusive lock on this channel's file (returns null or throws

            // an exception if the file is already locked.

            try {
            	 //FileLock lock = channel.tryLock();
            	//open file 
          	  
          	  	String line = "";
                String cvsSplitBy = ",";
            	BufferedReader detailsReader = new BufferedReader(new FileReader(file));
            	
        		  while ((line = detailsReader.readLine()) != null) {
        			  String userDetails[] = line.split(cvsSplitBy);
        			  if(userDetails[0].equals(username)){
        				  removeLineFromFile("userDetails.csv", line);
        				  FileWriter detailsWriter = new FileWriter(file,true);
        				  //we have found the user in the system after login
        				  System.out.println("Found user in system changing details");
        				  userDetails[1]= name ;
        				  userDetails[2] = address;
        				  userDetails[3] = accnum;
        				  
        				  System.out.println("Changing details now");
        				  StringBuilder sb = new StringBuilder();
        			      
        				  
        				  //mtehod used to find the previous customer details and remove them 
        				  
        				  //TODO add a lock
        			      
        			      sb.append(userDetails[0]);
        			      sb.append(',');
        			      sb.append(name);
        			      sb.append(',');
        			      sb.append(address);
        			      sb.append(',');
        			      sb.append(accnum);
        			      sb.append(',');
        			      sb.append(userDetails[4]);
        			      sb.append("\r\n");
        				  System.out.println(sb.toString());
        				  detailsWriter.write(sb.toString());
        				  detailsWriter.close();
        				  break;
        			  }
        			  

            }

				  detailsReader.close();
				  
        		 // lock.release();
    			  channel.close();
            }
            catch (OverlappingFileLockException e) {

                // thrown when an attempt is made to acquire a lock on a a file that overlaps
                // a region already locked by the same JVM or when another thread is already

                // waiting to lock an overlapping region of the same file

                System.out.println("Overlapping File Lock Error: " + e.getMessage());

            }


			break;
		case 2: 
			
			break;
		case 3:
			//TODO refactor into its own method
			//do login
			sendMessage("Deposit");
			depositFunds();
			
			break;
			
		case 4:
			sendMessage("Withdraw");
			//a method which attempts to process a withdrawl of funds from the customers account
			withdrawFunds();
			break;
		case 5:
			sendMessage("Quit!");
			message = (String)in.readObject();
			break;
			
		default:
			sendMessage("Invalid command");
			customerMenu();
		}
  }

/**
 * @throws IOException
 * @throws ClassNotFoundException
 */
private void depositFunds() throws IOException, ClassNotFoundException {
	sendMessage("How much do you want to deposit?");
	
	double prevBalance = account.getBalance();
	boolean transactionSuccessful;
	
	message = (String)in.readObject();
	System.out.println(message);
	// TODO change this to double
	transactionSuccessful =  account.deposit(Double.parseDouble(message));
	
	//if the transaction is successful we want to log the transaction details to the file
	if(transactionSuccessful){
		//log the transaction into the transaction file
		 FileWriter detailsWriter = new FileWriter("userTransactions.csv",true);
		 StringBuilder detailsSB = new StringBuilder();
		  
		  detailsSB.append(username);
		  detailsSB.append(',');
		  detailsSB.append(accnum);
		  detailsSB.append(',');
		  detailsSB.append(Double.parseDouble(message));
		  detailsSB.append(',');
		  detailsSB.append(prevBalance);
		  detailsSB.append(',');
		  detailsSB.append(account.getBalance());
		  detailsSB.append("\r\n");

		  detailsWriter.write(detailsSB.toString());
		  detailsWriter.close();
		  
		  //TODO attempt to change balance in userDetails file
		  
		  
	}
}

/**
 * @throws IOException
 * @throws ClassNotFoundException
 */
private void withdrawFunds() throws IOException, ClassNotFoundException {
	sendMessage("How much do you want to Withdraw?");
	//the balance before any transaction occurs
	double prevBalance = account.getBalance();
	boolean transactionSuccessful;
	
	sendMessage("You have "+prevBalance+" balance and a 1000 credit limit");
	message = (String)in.readObject();
	System.out.println(message);
	// Method which returns 
	transactionSuccessful = account.withdraw(Double.parseDouble(message));
	
	//if the transaction is successful we want to log the transaction details to the file
	if(transactionSuccessful){
		//log the transaction into the transaction file
		 FileWriter detailsWriter = new FileWriter("userTransactions.csv",true);
		 StringBuilder detailsSB = new StringBuilder();
		  
		  detailsSB.append(username);
		  detailsSB.append(',');
		  detailsSB.append(accnum);
		  detailsSB.append(',');
		  detailsSB.append(Double.parseDouble(message));
		  detailsSB.append(',');
		  detailsSB.append(prevBalance);
		  detailsSB.append(',');
		  detailsSB.append(account.getBalance());
		  detailsSB.append("\r\n");

		  detailsWriter.write(detailsSB.toString());
		  detailsWriter.close();
		  
		  //TODO attempt to change balance in userDetails file
	}
	
}
  boolean loginMenu() throws ClassNotFoundException, IOException{
	  int choice;
		String loginMenu = "\n Enter choice:  \n"
				+ "1. Log-into the banking system  \n"
				+ "2. Register with the system  \n"
				+ "3. Quit  \n";
		String starterMessage = " _____________________Acme ATM Company___________________\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|\tWelcome to Gordon Allied National Bank\t\t|\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|_______________________________________________________|"+loginMenu;
		sendMessage(starterMessage);
		
		
		
		message = (String)in.readObject();
		choice = new Integer(message);
		
		switch(choice){
		case 1:
			//do register
			sendMessage("Login");
			if(login() == true){
				
				return true;
			}
			break;
		case 2: 
			//do login
			sendMessage("Register");
			register();
			break;
		case 3:
			sendMessage("Quit!");
			message = (String)in.readObject();
			break;
			
		default:
			sendMessage("Invalid command");
			loginMenu();
			return false;
		}
		return false;
	}
  boolean login() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("Enter username:");
	  message = (String)in.readObject();
	  System.out.println(message);
	  username = message;
	  sendMessage("Password:");
	  message = (String)in.readObject();
	  System.out.println(message);
	  password = message;
	  
	  hashedPassword = getMD5(password);
	  
	  //open file 
	  
	  String line = "";
      String cvsSplitBy = ",";
      BufferedReader br = new BufferedReader(new FileReader("login.csv"));

      try  {
    	  
          while ((line = br.readLine()) != null) {

              // use comma as separator
              String loginDetails[] = line.split(cvsSplitBy);
              if(loginDetails[0].equals(username)){
            	  System.out.println(loginDetails[1]);
            	  System.out.println(hashedPassword);
            	  if(loginDetails[1].equals(hashedPassword)){
            		  BufferedReader detailsReader = new BufferedReader(new FileReader("userDetails.csv"));
            		  while ((line = detailsReader.readLine()) != null) {
            			  String userDetails[] = line.split(cvsSplitBy);
            			  if(userDetails[0].equals(username)){
            				  //we have found the user in the system after login
            				  
            				  name = userDetails[1];
            				  address = userDetails[2];
            				  accnum = userDetails[3];
            				  balance = Double.parseDouble(userDetails[4]);
            				  
            				  account.setBalance(balance);
            				  detailsReader.close();
            				  br.close();
            				  break;
            				  
            			  }
            		  }
            		  sendMessage("Authenticated");
            		  return true;
            	  }
              }

          }
          System.out.println("User not found");
          sendMessage("Login Failed");

      } catch (IOException e) {
          e.printStackTrace();
      }
      finally{
    	  br.close();
      }
	return false;
  }
  void register() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("First off what is your name?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  name = message;
 sendMessage("what is your address?");
	  
	  //TODO clean code
	  message = (String)in.readObject();
	  System.out.println(message);
	  address = message;
 sendMessage(" what is your account number?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  accnum = message;
	  sendMessage("Choose a username?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  username = message;
	  sendMessage("Choose password?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  password = message;
	  
	  balance = (double)(Math.random() * 10) + 1;
	  
	  sendMessage("Account Created");
	  
	  FileWriter pw = new FileWriter("login.csv",true);
      StringBuilder sb = new StringBuilder();
      
      hashedPassword = getMD5(password);
      sb.append(username);
      sb.append(',');
      sb.append(hashedPassword);
      sb.append("\r\n");

      pw.write(sb.toString());
      pw.close();
      
      /*
       * Now we will print the details of the customer to a seperate file
       * This is because I consider it unsafe to have all details in one file
       * 
       */
      FileWriter detailsWriter = new FileWriter("userDetails.csv",true);
      StringBuilder detailsSB = new StringBuilder();
      
      hashedPassword = getMD5(password);
      detailsSB.append(username);
      detailsSB.append(',');
      detailsSB.append(name);
      detailsSB.append(',');
      detailsSB.append(address);
      detailsSB.append(',');
      detailsSB.append(accnum);
      detailsSB.append(',');
      detailsSB.append(balance);
      detailsSB.append("\r\n");

      detailsWriter.write(detailsSB.toString());
      detailsWriter.close();
	  
  }
  /*
   * getMD5()
   * Takes a @param of a string
   * Then gets the bytes for this string 
   * Generates an MD5 using the bytes and returns this as a string
   */
  private String getMD5(String str){
	  String generatedPassword = null;
	  try {
          // Create MessageDigest instance for MD5
          MessageDigest md = MessageDigest.getInstance("MD5");
          //Add password bytes to digest
          md.update(str.getBytes());
          //Get the hash's bytes 
          byte[] bytes = md.digest();
          //This bytes[] has bytes in decimal format;
          //Convert it to hexadecimal format
          StringBuilder sb2 = new StringBuilder();
          for(int i=0; i< bytes.length ;i++)
          {
              sb2.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
          }
          //Get complete hashed password in hex format
          generatedPassword = sb2.toString();
      } 
      catch (NoSuchAlgorithmException e) 
      {
          e.printStackTrace();
      }
	return generatedPassword;
  }
  public void removeLineFromFile(String file, String lineToRemove) throws IOException {

	    try {

	      File inFile = new File(file);

	      if (!inFile.isFile()) {
	        System.out.println("Parameter is not an existing file");
	        return;
	      }
	      //TODO tidy and comment the code
	      //Construct the new file that will later be renamed to the original filename.
	      File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

	      BufferedReader br = new BufferedReader(new FileReader(file));
	      PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

	      String line = null;

	      //Read from the original file and write to the new
	      //unless content matches data to be removed.
	      while ((line = br.readLine()) != null) {

	        if (!line.trim().equals(lineToRemove)) {
	        	System.out.println("Writing line to new file");
	          pw.println(line);
	          pw.flush();
	        }
	        else{
	        	System.out.println("Found line");
	        }
	      }
	      pw.close();
	      br.close();

	      //Delete the original file
	      if (!inFile.delete()) {
	        System.out.println("Could not delete file");
	        System.out.println(inFile.getAbsolutePath());
	        br.close();
	        inFile.delete();
	        return;
	      }
	    //Rename the new file to the filename the original file had.
	      if (!tempFile.renameTo(inFile))
	        System.out.println("Could not rename file");

	    }
	    catch (FileNotFoundException ex) {
	      ex.printStackTrace();
	    }
	    catch (IOException ex) {
	      ex.printStackTrace();
	    }
	    
	    finally{
	    	System.out.println("Removed entry from file");
	    }
  }
}// end ClientServiceThread class 
