package ph.rmi.method;

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.TimeZone;

import ph.rmi.util.JvmIdUtils;
import ph.rmi.util.RMIContext;


public class RMIMethodServerMain implements Runnable {

	private final int PORT = 5000;
//	public static String HOSTNAME = "localhost";
	
	private final String LOAD_OBJECTS = "";

	public static void main(String args[]) throws InterruptedException {
		
		try {
			System.setErr(System.out);
			
			RMIMethodServerMain methodservermain = new RMIMethodServerMain();

			methodservermain.run();

		} catch (Throwable throwable) {
			if (throwable instanceof ExceptionInInitializerError) {
				Throwable throwable1 = ((ExceptionInInitializerError) throwable)
						.getException();
				if (throwable1 != null)
					throwable = throwable1;
			}
			System.exit(1);
		}
	}

	public void run() {
		try {
		System.setErr(System.out);
		
		long timestarts = System.currentTimeMillis();

		String classPath = System.getProperty("java.class.path");
		Object vmVendor = System.getProperty("java.vm.vendor");
		Object vmName = System.getProperty("java.vm.name");
		String javaVersion = System.getProperty("java.version");
		Object vmVersion = System.getProperty("java.vm.version");
		Object archModel = System.getProperty("sun.arch.data.model");
		String sep = System.getProperty("line.separator");
		String osName = System.getProperty("os.name");
		String osArch = System.getProperty("os.arch");
		String osVersion = System.getProperty("os.version");
		System.out
				.println("-------------------------------------------------------------------------------");
		System.out.println((new StringBuilder()).append("Starting ")
				.append(RMIMethodServer.SERVER_NAME).toString());
		System.out.println((new StringBuilder()).append("JVM id: ")
				.append(JvmIdUtils.getJvmId()).toString());
		System.out.println((new StringBuilder())
				.append("JVM: ")
				.append(((String) (vmVendor)))
				.append(", ")
				.append(((String) (vmName)))
				.append(", ")
				.append(javaVersion)
				.append(" (")
				.append(vmVersion)
				.append(archModel == null ? ")" : (new StringBuilder())
						.append("), ").append(((String) (archModel))).append("-bit")
						.toString()).toString());
		System.out.println((new StringBuilder()).append("OS: ").append(osName)
				.append(", ").append(osArch).append(", ").append(osVersion).toString());
		System.out.println((new StringBuilder()).append("Host: ")
				.append(JvmIdUtils.getJvmHost()).append(" (")
				.append(JvmIdUtils.getCanonicalHostname().toLowerCase())
				.append(')').toString());
		if (classPath != null) {
			String lineSep = (new StringBuilder()).append(sep).append("  ")
					.toString();
			StringBuilder stringbuilder = new StringBuilder("Class path =");
			stringbuilder.append(lineSep);
			stringbuilder.append(((String) (classPath)).replaceAll(
					File.pathSeparator, lineSep));
			System.out.println(stringbuilder.toString());
		}
		Object obj = TimeZone.getDefault();
		System.out.println((new StringBuilder()).append("Setting WTContext time zone to ").append(((TimeZone) (obj)).getID()).append("; offset: ").append((double)((TimeZone) (obj)).getOffset(System.currentTimeMillis()) / 3600000D).toString());
		RMIContext.setDefaultTimeZone(((TimeZone) (obj)));
		RMIContext.getContext().setTimeZone(((TimeZone) (obj)));
		Object obj1 = TimeZone.getTimeZone("GMT");
		System.out.println((new StringBuilder()).append("Setting default time zone to ").append(((TimeZone) (obj1)).getID()).append("; offset: ").append((double)((TimeZone) (obj1)).getOffset(System.currentTimeMillis()) / 3600000D).toString());
		TimeZone.setDefault(((TimeZone) (obj1)));
		
		RMIRemoteMethodServer.ServerFlag = true;
		
		Registry registry = LocateRegistry.createRegistry(PORT);
		System.out.println((new StringBuilder())
				.append("Registry created: ").append(obj).toString());
		
		RMIMethodServer methodServer = new RMIMethodServerImpl();

		System.out.println("MethodServer created."); 
		
		registry.rebind(RMIMethodServer.SERVER_NAME, methodServer);
		
		System.out.println("MethodServer bound in registry.");
		
		RMIMethodServerImpl.signalReady();
		
		System.out
		.println((new StringBuilder())
				.append("MethodServer ready (in ")
				.append((double) (System.currentTimeMillis() - timestarts) / 1000D)
				.append(" secs).").toString());
		} catch (Throwable throwable) {
			if (throwable instanceof ExceptionInInitializerError) {
				Throwable throwable1 = ((ExceptionInInitializerError) throwable)
						.getException();
				if (throwable1 != null)
					throwable = throwable1;
			}
			System.out.println("MethodServerMain abort" + throwable);
		}
	}

}
