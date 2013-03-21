import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

public class Component extends UnicastRemoteObject implements ComponentInterface, Runnable {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int id = 0;
	String name;
	String neighbour;
	boolean isActive = true;
	boolean receiveFlag = false;
	boolean elected = false;
	private final Object lock = new Object();
	
	int lastReceivedId = -1;
	
	int tid = -1;
	int ntid = -1;
	int nntid = -1;
	
	public Component(int i, String neighbour) throws RemoteException {
		super();
		
		this.id = i;
		this.neighbour = neighbour;
		this.isActive = true;
		this.receiveFlag = false;
		this.elected = false;
		
		System.out.printf("Process %d connected was created. Neighbours:\n", this.id);
		System.out.println(neighbour);

	}

	public Component(int i, String neighbour, String name) throws RemoteException {
		super();
		
		this.id = i;
		this.name = name;
		this.neighbour = neighbour;
		this.isActive = true;
		this.receiveFlag = false;
		this.elected = false;
		
		System.out.printf("Process %d connected was created. Neighbours:\n", this.id);
		System.out.println(neighbour);

	}

	
	
	
	@Override
	public int receive(int receivedId) throws RemoteException {
		//System.out.printf("Received id: %d\n", receivedId);
		
		
		synchronized(lock){
			if (receiveFlag) {
				try {
					lock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
			lastReceivedId = receivedId;
			receiveFlag = true;
			lock.notify();
		}
		
		return 0;
	}
	
	public void sendMessage(int id) {
		try {
			Remote robj = Naming.lookup(this.neighbour);
			
			ComponentInterface processserver = (ComponentInterface) robj;
			//System.out.printf("Sending id %d to %s\n", id, this.neighbour);
			processserver.receive(id);
		} catch (Exception e) {
			System.out.printf("Error sending message in function sendMessage: %s\n", e.getMessage());
		}
	}
	
	public void runProcess() {
		
		
		if (isActive) {
			activeRun();
		}
		else {
			passiveRun();
		}
	}
	
	private int processLastReceive() {
		//System.out.println("Receiving");
		if (!receiveFlag && !elected) {
			//System.out.println("block enter");
			try {
				lock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println("block released");
		}
		receiveFlag = false;
		lock.notifyAll();
		return lastReceivedId;
	}
	
	public void activeRun() {
		pauseProg("before sending");
		sendMessage(tid);
		
		pauseProg("before receiving");
		synchronized(lock){
			ntid = processLastReceive();
		}
		
		pauseProg("after first receive");
		if (ntid == id)
			this.elected = true;
		
		pauseProg("second send");
		sendMessage(Math.max(tid, ntid));
		
		pauseProg("second receive");
		synchronized(lock){
			nntid = processLastReceive();
		}
		
		pauseProg("chosing if elected");
		if (nntid == id)
			this.elected = true;
		
		pauseProg("schosing if active");
		
		if ( (ntid >= tid) && (ntid >= nntid) ) {
			tid = ntid;
		}
		else {
			isActive = false;
		}
		
		pauseProg("end of active run");
		
	}
	
	public void passiveRun() {
		pauseProg("before passive receive");
		synchronized(lock){
			tid = processLastReceive();
		}
		pauseProg("passive choosing if elected");
		if (tid == id)
			this.elected = true;
		
		pauseProg("passive sending");
		sendMessage(tid);
		
		pauseProg("end of passive run");
	}

	@Override
	public void run() {
		int round = 1;
		tid = id;
		while(!elected) {
			pauseProg("start round " + Integer.toString(round));
			
			runProcess();

			pauseProg("end of  round " + Integer.toString(round));
			round++;
		}
		
		System.out.printf("ELECTED Process with id %d elected\n", id);
		
	}
	
	public void pauseProg(String msg){
		System.out.printf(">>STEP: %s\n", msg);
		System.out.printf("ID: %d, a: %b, r: %b, e %b\n", id, isActive, receiveFlag,  elected);
		System.out.printf("last: %d,  tid: %d, ntid: %d, nntid: %d\n", lastReceivedId, tid, ntid, nntid);
		if (OutputSettings.stepExecution) {
			System.out.println("Press enter to continue...");
			Scanner keyboard = new Scanner(System.in);
			keyboard.nextLine();
		}
		
		}


}
