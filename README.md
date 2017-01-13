# OS-Project
This is my project for my 3rd year operating systems module in college. The requirements for this project were to write a Multi-threaded TCP Server Application which allows multiple customers to update their bank accounts and send payments to other bank accounts.

The service should allow the users to:

1. Register with the system
	+ Name
	+ Address
	+ Bank A/C Number
	+ Username
	+ Password
2. Log-in to the banking system from the client application to the server application.
3. Change customer details.
4. Make Lodgements to their Bank Account.
5. Make Withdrawal from their Bank Account (Note: Each User has a credit limit of €1000).
6. View the last ten transactions on their bank account.

#### Server Application Rules
1. The server application should not provide any service to a client application that can complete the authentication.
2. The server should hold a list of valid users of the service and a list of all the users’ previous transactions.
3. When the user logs in they should receive their current balance.

#### Design Decisions
This section lists the different decisions that were made when designing how this application will work.

+ When the client first opens the application it will connect to the server and allow the user to either register a new account, login to an existing account or exit the application.

+ The user must register in order to user this service. They must enter their name, address, account number, username and password as required by the project specification. Their name and address may contain spaces whereas the username and password may not. If the user enters a username or password that contains spaces it will only use the sequence of characters upto the first space. No input can be empty. The account number must be a positive integer. These rules are enforced throughout the application. If they enter an account number which is already used by another account the user will receive an error. If they register correctly they will be brought back to the first menu where they can then login.

+ The user must log into their account in order to access the service. This was required by the project specification. The user needs to enter their , username and password. If they enter all credentials correctly and are not already logged in they will be given access to the service. Otherwise they will be brought back to the first menu where they can attempt to login again.

+ Once the user logs in they will receive their current balance and a range of options listed in the description.

+ The users password are never saved on the server , instead the MD5 hash of the password is saved.

+ There is a credit limit of €1000 outlined in the specification. If the amount they wish to withdraw exceeds either of these they will receive an error message

+ The list of user login details is saved to a separate file called &lt;login&gt;.csv.
The username is the unique identifier for the user. There is a seperate file used for the user details. I keep these seperate in case of comprimise
#### Test Account
There is an account in the system already to test with:

username: ryang
password: ryang

Altertivetly you can register a new user.

#### Server
The server application is hosted on Amazon Web Services as required by the project specification.

#### Running the application
Open a terminal and clone this repository using the following command.
```
git clone https://github.com/FlashGordon95/TCP-Banking-Server.git
```

You can then decide whether you want to run the application locally or remotely.

###### Remotely
To run the server remotely simply start the client as follows.
```
java ie.gmit.sw.client.Requester
```
The default IP address applied in the source code represents a currently running EC2 instance of the bank server

###### Locally
To run the server locally you fill first have to run the server as follows.
Run the server on localhost

In Requester.java edit either the ipaddress value on line 17 to "localhost" or on line 27 change this to "localhost".


#### Conclusion
I believe that I have implemented all requested functionality outlined in the project specification. After completing this project I have a better understanding of multi-threaded programming and socket programming. I also learned through this project how to set up a virtual machine on Amazon Web Services.
