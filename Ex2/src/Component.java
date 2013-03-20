import java.io.IOException;
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

	
	int lastReceivedId = -1;
	
	@Override
	public int receive(int receivedId) throws RemoteException {
		System.out.printf("Received id: %d", receivedId);
		System.out.println();
		
		synchronized(lock){
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
			System.out.printf("Sending id %d to %s\n", id, this.neighbour);
			processserver.receive(id);
		} catch (Exception e) {
			System.out.printf("Error sending message in function sendMessage: %s\n", e.getMessage());
		}
	}
	int tid = -1;
	int ntid = -1;
	int nntid = -1;
	public void runProcess() {
		tid = id;
		
		if (isActive) {
			activeRun();
		}
		else {
			passiveRun();
		}
	}
	
	private int processLastReceive() {
		System.out.println("Receiving");
		if (!receiveFlag) {
			System.out.println("block enter");
			try {
				lock.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("block released");
		}
		receiveFlag = false;
		return lastReceivedId;
	}
	
	public synchronized void activeRun() {
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
		while(!elected) {
			runProcess();
			
			// run step by step
			System.out.printf("Step of %d ended. Press enter...\n", id);
			int read = -1;
			Scanner reader = new Scanner(System.in);
			read=reader.nextInt();
		}
		
		System.out.printf("Process with id %d elected\n", id);
		
	}


}
