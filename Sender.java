/*************************************
 * Filename:  Sender.java
 * Names: Bruno Teixeira and Matt Page
 * Student-IDs: 201445705 and 201329303
 * Date:
 *************************************/
import java.util.Random;

public class Sender extends NetworkHost

{

    /*
     * Predefined Constant (static member variables):
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
     *  Message: Used to encapsulate the message coming from application layer
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
    // state information for the sender. 
	
	private int lastSeqNum = 1;
    private Packet lastPacketSent;

    // Also add any necessary methods (e.g. checksum of a String)

    // This is the constructor.  Don't touch!
    public Sender(int entityName,
                       EventList events,
                       double pLoss,
                       double pCorrupt,
                       int trace,
                       Random random)
    {
        super(entityName, events, pLoss, pCorrupt, trace, random);
    }

    // This routine will be called whenever the application layer at the sender
    // has a message to  send.  The job of your protocol is to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving application layer.
    protected void Output(Message message)
    {
		//set up sequence number and a intial ack number to send to the receiver
		int seqNum;
		int ackNum;
		//switch the numbers from the last time the method was called
        if(lastSeqNum == 1){
            lastSeqNum = 0;
			seqNum = lastSeqNum;
			ackNum = 1;
		}
        else{
            lastSeqNum = 1;
			seqNum = lastSeqNum;
			ackNum = 0;
		}
		//create checksum
        int checkSum = creatingChecksum(ackNum, seqNum, message.getData());
		//send with an ackNum of the opposite of the sequence so if the packet is sent back it is not seen as a correct response and we can work with it
        Packet senderPacket = new Packet(seqNum,ackNum,checkSum, message.getData());
		//store the last sent packet so we can send it again if needed
        lastPacketSent = senderPacket;
		//send the packet to the receiver and start the timers
        udtSend(senderPacket);
        startTimer(200);
        
    }
    
    // This routine will be called whenever a packet sent from the receiver 
    // (i.e. as a result of udtSend() being done by a receiver procedure)
    // arrives at the sender.  "packet" is the (possibly corrupted) packet
    // that was sent from the receiver.
    protected void Input(Packet packet)
    {
		//work out the checksum for the incoming packet and check it against the checksum value in the packet to see if its corrupted
		int checkSum = creatingChecksum(packet.getAcknum(), packet.getSeqnum(), packet.getPayload());
		boolean noncorrupted;
		if(checkSum == packet.getChecksum()){
			noncorrupted = true;
		}
		else{
			noncorrupted = false;
		}
		
		// This is supposed to be when its not the ack we are expecting, so send it again and restart the timers
        if(packet.getAcknum() != lastSeqNum){ 
            stopTimer();
            udtSend(lastPacketSent);
            startTimer(200);
        } 
        else{ // When the ackNum is okay, but still need to check for corruption
            if(noncorrupted == false){ // If its corrupted send again and restart timers
                stopTimer();
                udtSend(lastPacketSent);
                startTimer(200);
            }
            else{ // this is when everything is fine and we can stop the timer
                stopTimer();
            }
        }
    }
    
    // This routine will be called when the senders's timer expires (thus 
    // generating a timer interrupt). You'll probably want to use this routine 
    // to control the retransmission of packets. See startTimer() and 
    // stopTimer(), above, for how the timer is started and stopped. 
    protected void TimerInterrupt()
    {
		// resend the packet and restart the timer
		udtSend(lastPacketSent);
		startTimer(200);
		
    }
    
    // This routine will be called once, before any of your other sender-side 
    // routines are called. It should be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of the sender).
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