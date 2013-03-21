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
		sendMessage(tid);
		
		synchronized(lock){
			ntid = processLastReceive();
		}
		
		if (ntid == id)
			this.elected = true;
		
		sendMessage(Math.max(tid, ntid));
		
		synchronized(lock){
			nntid = processLastReceive();
		}
		
		if (nntid == id)
			this.elected = true;
		
		if ( (ntid >= tid) && (ntid >= nntid) ) {
			tid = ntid;
		}
		else {
			isActive = false;
		}
		
	}
	
	public void passiveRun() {
		synchronized(lock){
			tid = processLastReceive();
		}
		if (tid == id)
			this.elected = true;
		sendMessage(tid);
	}

	@Override
	public void run() {
		tid = id;
		while(!elected) {
			runProcess();
			
			System.out.printf("ID: %d, %s - %s, active: %b, received: %b, elected %b\n", id, name, neighbour, isActive, receiveFlag,  elected);
			
			
			System.out.printf("last: %d,  tid: %d, ntid: %d, nntid: %d\n", lastReceivedId, tid, ntid, nntid);
			// run step by step
			//System.out.printf("Step of %d ended. Press enter...\n", id);
			//int read = -1;
			//Scanner reader = new Scanner(System.in);
			//read=reader.nextInt();
		}
		
		System.out.printf("ELECTED Process with id %d elected\n", id);
		
	}


}
