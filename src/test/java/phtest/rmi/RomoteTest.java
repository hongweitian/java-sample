package phtest.rmi;

import ph.rmi.method.RMIRemoteMethodServer;

public class RomoteTest {
	

	public static void main(String arg[]) throws Exception {
		
		Class[] cls = { String.class };
		Object[] obj = { "Pan Pan" };
		
		Object retObj = RMIRemoteMethodServer.getDefault().invoke("testFun", TestObj.class.getName(), null, cls, obj);
		
		System.out.println("retObj - " + retObj);
		
	}

}
