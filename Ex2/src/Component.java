import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Component extends UnicastRemoteObject implements ComponentInterface{


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int index = 0;
	String neighbour;

	public Component(int i, String neighbour) throws RemoteException {
		super();
		
		this.index = i;
		this.neighbour = neighbour;
		
		System.out.printf("Process %d connected was created. Neighbours:\n", this.index);
		System.out.println(neighbour);

	}

	@Override
	public int receive(int id) throws RemoteException {
		System.out.printf("Received id: %d", id);
		System.out.println();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sendMessage(index);
		return 0;
	}
	
	public void sendMessage(int id) {
		try {
			Remote robj = Naming.lookup(this.neighbour);
			
			ComponentInterface processserver = (ComponentInterface) robj;
			System.out.printf("Sending id %d to %s\n", index, this.neighbour);
			processserver.receive(id);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}


}
