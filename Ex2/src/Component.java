import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Component extends UnicastRemoteObject implements ComponentInterface{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int id = 0;
	String neighbour;
	boolean isActive = true;
	boolean receiveFlag = false;
	boolean elected = false;
	
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

	int lastReceivedId = -1;
	
	@Override
	public int receive(int receivedId) throws RemoteException {
		System.out.printf("Received id: %d", receivedId);
		System.out.println();
		
		lastReceivedId = receivedId;
		receiveFlag = true;
		notifyAll();
		
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
	int tid;
	int ntid;
	int nntid;
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
			System.out.println("block");
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("released");
		}
		System.out.println("return");
		receiveFlag = false;
		return lastReceivedId;
	}
	
	public void activeRun() {
		sendMessage(tid);
		ntid = processLastReceive();
		
		if (ntid == id)
			this.elected = true;
		
		sendMessage(Math.max(tid, ntid));
		nntid = processLastReceive();
		
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
		tid = processLastReceive();
		if (tid == id)
			this.elected = true;
		sendMessage(tid);
	}


}
