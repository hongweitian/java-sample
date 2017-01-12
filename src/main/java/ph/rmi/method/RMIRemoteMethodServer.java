package ph.rmi.method;

import java.io.InterruptedIOException;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;
import java.io.WriteAbortedException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.rmi.ServerRuntimeException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class RMIRemoteMethodServer {

	private static final Logger verboseClientLogger = Logger.getLogger( "ext.rmi.method.client" );
	
	public static boolean ServerFlag = false;

	public static String HOSTNAME = "localhost";

	public static int PORT = 5000;

    private int activeCalls;
    private final Object activeCallsLock = new Object();
    private int maxActiveCalls;
    
	private static final AtomicLong idNumberSequence = new AtomicLong();
	
    protected RMIRemoteMethodServer() {
    }
    
    protected RMIRemoteMethodServer(String hostName) {
    	HOSTNAME = hostName;
    }
    
    public static RMIRemoteMethodServer getDefault() {
    	return new RMIRemoteMethodServer();
    }
    
    public static RMIRemoteMethodServer getDefault(String hostName) {
    	return new RMIRemoteMethodServer(hostName);
    }

    public Object invoke
    (
       String target_method,
       String target_class,
       Object target_object,
       Class[] arg_types,
       Object[] args
    )
       throws RemoteException, InvocationTargetException
    {
       try
       {
          // Ignore specified target_class if an instance is given
          if (target_object != null)
             target_class = target_object.getClass().getName();

          RMIMethodArgs invoke_args = new RMIMethodArgs();;

          invoke_args.targetMethod = target_method;
          invoke_args.targetObject = target_object;
          invoke_args.targetClass = target_class;
          invoke_args.argTypes = arg_types;
          invoke_args.argObjects = args;

          invoke_args.redirects = Integer.MAX_VALUE;

          // Do it...
          RMIMethodResult result = null;
          RMIMethodServer method_server = getMethodServer();

          while (true)
          {
             // Don't start a call if the current thread is interrupted
             // Check and clear interrupt in two steps - see WTThread.isInterrupted
             if (Thread.currentThread().isInterrupted())
             {
                Thread.interrupted();
                throw new InterruptedException("线程调用被中断");
             }

             Throwable server_exception = null;
             try
             {
                {
                  synchronized ( activeCallsLock )
                  {
                     if ( ++activeCalls > maxActiveCalls )
                        maxActiveCalls = activeCalls;
                  }
                }
                try
                {
                   result = method_server.invoke(invoke_args);
                }
                finally
                {
                   synchronized (activeCallsLock)
                   {
                      --activeCalls;
                   }
                }
                server_exception = result.serverException;
             }
             catch (RemoteException e)
             {
                // Unwrap server exceptions thrown during arg or result marshaling
                Throwable detail = e.detail;
                if (detail != null)
                {
                   if ((e instanceof ServerException) ||
                       (e instanceof ServerError) ||
                       (e instanceof ServerRuntimeException))
                   {
                      server_exception = detail;
                      if (server_exception instanceof UnmarshalException)
                      {
                         // Continue with check for abort exceptions thrown during server deserialization
                         detail = ((RemoteException)detail).detail;
                      }
                   }
                   // Unwrap abort exceptions thrown during serialization (see MethodResult.writeExternal).
                   // Aborts during client arg writing or server response writing are double wrapped.
                   // Aborts thrown during server arg reading or client result reading are single wrapped.
                   if (detail instanceof WriteAbortedException)
                   {
                      // Dig real exception out of the remote exception
                      int abort_depth = 1;
                      server_exception = detail;
                      Exception abort_detail = ((WriteAbortedException)detail).detail;
                      if (abort_detail != null)
                      {
                         server_exception = abort_detail;
                         if (abort_detail instanceof ObjectStreamException)
                            abort_depth++;  // Double wrapped in java.io.ObjectOutputStream
                         if (abort_detail instanceof WriteAbortedException)
                         {
                            abort_detail = ((WriteAbortedException)abort_detail).detail;
                            if (abort_detail != null)
                               server_exception = abort_detail;
                         }
                         if (server_exception instanceof InvocationTargetException)
                         {
                            Throwable t = ((InvocationTargetException)server_exception).getTargetException();
                            if (t != null)
                               server_exception = t;
                         }
                      }

                      // Check if write actually aborted in client side marshalling
                      if ((e instanceof MarshalException) ||
                          ((e instanceof UnmarshalException) && (abort_depth == 1)))
                      {
                         // Traceback of local marshaling exception isn't as useful as the real exception
                         throw server_exception;
                      }
                      else
                        verboseClientLogger.debug( "received server exception." );
                   }
                }

                // If it's not a wrapped server exception, try to recover from RMI failure
                if (server_exception == null)
                {
                   // Check for server side RemoteExceptions that had no detail exception - no retry
                   if ((e instanceof ServerException) ||
                       (e instanceof ServerError) ||
                       (e instanceof ServerRuntimeException))
                      throw e;

                   if (detail != null)
                   {
                      // Check for interrupted io exception - no retry
                      if (detail instanceof InterruptedIOException)
                         throw e;

                      // Check for serialization exception - no retry
                      if (detail instanceof ObjectStreamException)
                      {
                         throw e;
                      }

                      // Check for class not found exception - no retry
                      if (e.detail instanceof ClassNotFoundException)
                         throw e;
                   }

                   throw e;
                }
             }

             if (server_exception != null)
             {
                if (server_exception instanceof RuntimeException)
                {
                   throw new ServerRuntimeException("服务器例外",
                      (RuntimeException)server_exception.fillInStackTrace());
                }
                else if (server_exception instanceof Exception)
                {
                   // Rethrow server exception wrapped in InvocationTargetException
                   throw new InvocationTargetException(server_exception.fillInStackTrace());
                }
                else if (server_exception instanceof Error)
                {
                   throw new ServerError("服务器例外",
                      (Error)server_exception.fillInStackTrace());
                }
                else // There is nothing else, but just to be safe...
                {
                   // Rethrow server exception wrapped in InvocationTargetException
                   throw new InvocationTargetException(server_exception.fillInStackTrace());
                }
             }

             // Done, no need to repeat
             break;
          }
          // Return the final result
          return result.result;
       }
       catch (InvocationTargetException | RuntimeException | ThreadDeath e)
       {
          throw e;
       }
       catch (Throwable t)
       {
          throw new RuntimeException(t);
       }
    }

	public RMIMethodServer getMethodServer() throws RemoteException {
		try {
			Registry registry = getRegistry();
			RMIMethodServer methodServer = (RMIMethodServer)registry.lookup(RMIMethodServer.SERVER_NAME);
			return methodServer;
		} catch (Exception e) {
			throw new RemoteException("Connect Error.", e);
		}
	}

	protected synchronized Registry getRegistry()
			throws RemoteException {
		Registry currentRegistry = null;
		try {
			currentRegistry = LocateRegistry.getRegistry(HOSTNAME, PORT);
			verboseClientLogger.debug((new StringBuilder()).append("Registry = ").append(currentRegistry).toString());
		} catch (SecurityException securityexception) {
			throw securityexception;
		} catch (Exception exception) {
			currentRegistry = null;
			throw (RemoteException) new RemoteException("Connect Fail.",
					exception).fillInStackTrace();
		}
		return currentRegistry;
	}
}
