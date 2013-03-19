import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String ipAddress = getIp();
		List<String> neighbourList = getNeighbourListFromProperties();
		String processName = getPropertyByName("processName");
		
		System.setSecurityManager(new RMISecurityManager());
		try {
			int index = Integer.parseInt(args[0]);
			int portNum = Integer.parseInt(args[1]);
			
			LocateRegistry.createRegistry(portNum);
						
			System.out.printf("registry started at ip %s and port number %d\n", ipAddress, portNum);
			
			String bindName = "/" + processName + index;
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
			
			Component processServer = new Component(index, neighbourList.get(neighbourRingIndex));
			
			Naming.rebind(bindName, processServer);
			System.out.printf("bind to the name %s \n", bindName);
			
			boolean sent = false;
			while (true) {
				int randomNum = 3000 + (int) (Math.random() * 5000);
				Thread.sleep(randomNum);
				
				// only node 1 sends messages!
				if (processServer.index == 1 && !sent)
				{
					processServer.sendMessage(1);
					sent = true;
				}

			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

}
