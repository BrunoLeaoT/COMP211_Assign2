/*************************************
 * Filename:  Receiver.java
 * Names: Bruno Teixeira and Matt Page
 * Student-IDs: 201445705 and 201329303
 * Date:
 *************************************/
import java.util.Random;

public class Receiver extends NetworkHost

{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *
     * Predefined Member Methods:
     *
     *  void startTimer(double increment):
     *       Starts a timer, which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this in the Sender class.
     *  void stopTimer():
     *       Stops the timer. You should only call this in the Sender class.
     *  void udtSend(Packet p)
     *       Sends the packet "p" into the network to arrive at other host
     *  void deliverData(String dataSent)
     *       Passes "dataSent" up to application layer. Only call this in the
     *       Receiver class.
     *  double getTime()
     *       Returns the current time of the simulator.  Might be useful for
     *       debugging.
     *  String getReceivedData()
     *       Returns a String with all data delivered to receiving process.
     *       Might be useful for debugging. You should only call this in the
     *       Sender class.
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from application layer
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet, which is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      String getPayload()
     *          returns the Packet's payload
     *
     */

    // Add any necessary class variables here. They can hold
    // state information for the receiver.
    private int lastAckNum = 1;
    private Packet lastPacketGot;
	private int lastChecksum = 0;
	
    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public Receiver(int entityName,
                       EventList events,
                       double pLoss,
                       double pCorrupt,
                       int trace,
                       Random random)
    {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }

    
    // This routine will be called whenever a packet from the sender
    // (i.e. as a result of a udtSend() being done by a Sender procedure)
    // arrives at the receiver. Argument "packet" is the (possibly corrupted)
    // packet that was sent from the sender.
    protected void Input(Packet packet)
    {
		//set up ack number as the seq number we expect and then use the ack to send back to the sender
		//switch the numbers from the last time the method was called
		int ackNum;
        if(lastAckNum == 1){
            lastAckNum = 0;
			ackNum = lastAckNum;
		}
        else{
            lastAckNum = 1;
			ackNum = lastAckNum;
		}
		// calculate the checksum from the packet and compare it to the checksum variable in the packet to find if its corrupted
		int checkSum = creatingChecksum(packet.getAcknum(), packet.getSeqnum(), packet.getPayload());
		boolean noncorrupted;
		if(checkSum == packet.getChecksum()){
			noncorrupted = true;
		}
		else{
			noncorrupted = false;
		}
		//if its not corrupted
		if(noncorrupted == true && packet.getSeqnum() == lastAckNum){
			// if its a duplicate packet
			if(checkSum == lastChecksum){
				Packet response = new Packet(packet.getSeqnum(),ackNum,checkSum);
				udtSend(response);
			}
			// if its a new packet we havent received before and needs to be delivered
			else{
				deliverData(packet.getPayload());
				// create new packet to send to sender class
				int newcheckSum = creatingChecksum(ackNum, packet.getSeqnum(), packet.getPayload());
				Packet response = new Packet(packet.getSeqnum(),ackNum,newcheckSum, packet.getPayload());
				udtSend(response);
			}
			// store the packets just in case of duplication
			lastPacketGot = packet;
			lastChecksum = packet.getChecksum();
			
		}
		else{
			// if corrupted return the original packet back to the sender
			udtSend(packet);
		}
		
    }
    

    
    // This routine will be called once, before any of your other receiver-side
    // routines are called. It should be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the receiver).
    protected void Init()
    {
    }
	
	// method for creating the checksum out of the sequence number, ack number and the payload
    private int creatingChecksum(int ackNum, int seqNum, String message){
        String result = addBinary(Integer.toString(seqNum), Integer.toString(ackNum));
		result = addBinary(result,message);
        result = onesComplement(result);
		int checkSum = 0;
        try {
        	checkSum =Integer.parseInt(result);
       }catch (NumberFormatException e){
       } 
		return checkSum;  

    }
	
	//method for adding binary numbers together
    public String addBinary(String a, String b) 
    { 
          
        // Initialize result 
        String result = "";  
          
        // Initialize digit sum 
        int s = 0;          
  
        // Traverse both strings starting  
        // from last characters 
        int i = a.length() - 1, j = b.length() - 1; 
        while (i >= 0 || j >= 0 || s == 1) 
        { 
              
            // Comput sum of last  
            // digits and carry 
            s += ((i >= 0)? a.charAt(i) - '0': 0); 
            s += ((j >= 0)? b.charAt(j) - '0': 0); 
  
            // If current digit sum is  
            // 1 or 3, add 1 to result 
            result = (char)(s % 2 + '0') + result; 
  
            // Compute carry 
            s /= 2; 
  
            // Move to next digits 
            i--; j--; 
        } 
          
    return result; 
    }
	
	//method for getting the ones complement of a binary number
	public static String onesComplement(String a){
		
		String ones = "";
		// For ones complement flip every bit 
        for (int i = 0; i < a.length(); i++) 
        { 
			char ch = a.charAt(i);
            ones += (ch == '0') ? '1' : '0'; 
        } 
		return ones;
	}	


}
