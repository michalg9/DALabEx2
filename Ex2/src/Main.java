import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Main {

	public static String getIp() {
		
		try {
			InetAddress IP = InetAddress.getLocalHost();
			String ipString = IP.getHostAddress();
			System.out.println("IP of my system is := " + ipString);
			return ipString;
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
		
	}
	
	public static List<String> getNeighbourListFromProperties() {

		String neighboursString = getPropertyByName("neighbours");
		String[] neighbourArray = neighboursString.split(";");
		List<String> neighbourList = Arrays.asList(neighbourArray);
 
    	return neighbourList;
	}
	
	public static String getPropertyByName(String name) {
		Properties prop = new Properties();

		try {
			// load a properties file
			prop.load(new FileInputStream("config.properties"));

			// get the property value and print it out

			String result = prop.getProperty(name);

			return result;

		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return null;
	}
	
	public static List<String> removeFromTheList(List<String> list, String element) {
		
		int indexToRemove = list.indexOf(element);
		ArrayList<String> filteredList = new ArrayList<String>();
	    for (int i = 0; i < list.size(); i++) {
	    	if (i != indexToRemove) {
	    		filteredList.add(list.get(i));
	        }  
		}
	    
	    return filteredList;
		
	}
	 
	public static ArrayList<Component> createComponents(int numberOfComponents, int[] neighbours) {
		String namePrefix = "//127.0.0.1/ProcessServer";
		
		ArrayList<Component> componentList = new ArrayList<Component>();
		
		for (int i = 1; i <= numberOfComponents; i++) {
			String componentName = namePrefix + Integer.toString(i);
			
			try {
				Component componentServer = new Component(i, null, componentName);
				componentList.add(componentServer);
				bindComponentToTheName(componentServer, componentName);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for (int i = 1; i <= numberOfComponents; i++) {
			Component currentComponent = componentList.get(i-1);
			int neighbourId = neighbours[i-1] - 1;
			Component neighbourComponent = componentList.get(neighbourId);
			
			currentComponent.neighbour = neighbourComponent.name;
			
			componentList.set(i-1, currentComponent);
		}
		
		return componentList;
	}
	
	public static void runAllComponents(ArrayList<Component> componentList) {
		for (Component component : componentList) {
			Thread thread = new Thread(component);
			thread.start();
		}
	}
	
	
	public static void goLocal() {
		try {
			LocateRegistry.createRegistry(1099);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		System.out.printf("registry started at ip %s and port number %d\n", "127.0.0.1", 1099);
		
//		try {
//			String bindName1 = "//127.0.0.1/ProcessServer1";
//			String bindName2 = "//127.0.0.1/ProcessServer2";
//			
//			Component processServer1 = new Component(1, bindName2);
//			Component processServer2 = new Component(2, bindName1);
//			
//			bindComponentToTheName(processServer1, bindName1);
//			bindComponentToTheName(processServer2, bindName2);
//			
//			
//
//			System.out.println("run processs once");
//			Thread thread1 = new Thread(processServer1);
//			Thread thread2 = new Thread(processServer2);
//			thread1.start();
//			thread2.start();
//			System.out.println("finished");
//			
//		} catch (RemoteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		int []  neighbours = {4, 5, 1, 2, 3};
		runAllComponents(createComponents(5, neighbours));
		
		
	}
	
	public static void bindComponentToTheName(Component component, String bindName) {
		
		try {
			Naming.rebind(bindName, component);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.printf("bind to the name %s \n", bindName);
		
		
	}
	public static void goGlobal(int id, int portNum) {
		String ipAddress = getIp();
		List<String> neighbourList = getNeighbourListFromProperties();
		String processName = getPropertyByName("processName");
		
		try {
			LocateRegistry.createRegistry(portNum);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}				
		//System.out.printf("registry started at ip %s and port number %d\n", ipAddress, portNum);
			
		String bindName = "/" + processName + id;
		String currentProcessName = "//" + ipAddress + bindName;	
		System.out.println("Current process name is " + currentProcessName);
		
		int currentRingIndex = -1;
		if (neighbourList.contains(currentProcessName))
		{
			currentRingIndex = neighbourList.indexOf(currentProcessName);
		}
		else
		{
			currentProcessName = "//" + "127.0.0.1" + bindName;
			currentRingIndex = neighbourList.indexOf(currentProcessName);
		}
		
		assert currentRingIndex != -1 : "index of a current machine not found";
		
		int neighbourRingIndex = (currentRingIndex + 1) % neighbourList.size();
		
		Component processServer = null;
		try {
			processServer = new Component(id, neighbourList.get(neighbourRingIndex));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			Naming.rebind(bindName, processServer);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.printf("bind to the name %s \n", bindName);
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		Thread thread = new Thread(processServer);
		thread.run();
		
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		System.setSecurityManager(new RMISecurityManager());
		
		String mode = getPropertyByName("mode");
		
		if (mode.contains("LOCAL")) {
			
			System.out.println("Go local");
			goLocal();
		}
		else {
			int id = Integer.parseInt(args[0]);
			int portNum = Integer.parseInt(args[1]);
			goGlobal(id, portNum);
			
		}
		
		
		
	}

}
