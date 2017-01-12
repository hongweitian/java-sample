package ph.rmi.method;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
* Remote interface to the method server.
* <BR><BR><B>Supported API:</B> false
*/
public interface RMIMethodServer extends Remote
{
   // Name registered with server manager
   static final String SERVER_NAME = "RMIMethodServer";

   /**
    * <B>Supported API:</B> false
    **/
   public RMIMethodResult invoke (RMIMethodArgs method_args)
      throws RemoteException,
         ClassNotFoundException,
         NoSuchMethodException;
 
}
