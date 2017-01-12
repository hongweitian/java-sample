/* bcwti
 *
 * Copyright (c) 2010 Parametric Technology Corporation (PTC). All Rights Reserved.
 *
 * This software is the confidential and proprietary information of PTC
 * and is subject to the terms of a software license agreement. You shall
 * not disclose such confidential information and shall use it only in accordance
 * with the terms of the license agreement.
 *
 * ecwti
 */

package ph.rmi.method;


import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;


/**
* The method server object.
* This is an RMI server object that exposes method operations to remote clients
* via the <code>RemoteMethodServer</code> class.
* @see wt.method.RemoteMethodServer
*/
public class RMIMethodServerImpl extends UnicastRemoteObject
   implements RMIMethodServer, Unreferenced, Runnable
{
   private static final long  serialVersionUID = 1L;

   // Configurable properties
   private static final String SERVICE_NAME = RMIMethodServer.SERVER_NAME;
   private static final int RMI_OBJ_ID = 5000;                   // RMI well known object id

   private static final Logger  serverLogger = Logger.getLogger( "ext.rmi.method.server" );

   // Start date - to report in getInfo calls
   private static final Date  startDate = new Date();  // use best approximation of JVM startup, not load time of this class

   // Total invoke calls received
   private static final AtomicInteger  totalInvokeCalls = new AtomicInteger();

   private Random random = null;

   // Block invoke calls until ready
   private static volatile boolean ready = false;

   private static final Object  readyLock = new Object();

   /**
   * Construct a RMI Method Server object.
   * The <code>stop_when_unreferenced</code> argument indicates if
   * <code>WTContext.stop</code> should be called when this RMI server object
   * becomes unreferenced by any remote client (including registry).
   *
   * @param stop_when_unreferenced call <code>WTContext.stop</code> when unreferenced
   */
   public RMIMethodServerImpl ()
      throws RemoteException
   {
      super(RMI_OBJ_ID);
   }

   //
   // MethodServer methods
   //

   /**
   * Dynamically invoke a given method.
   * This is the primary interface to the method server.
   * Arguments and results are passed in special container objects that
   * implement custom marshaling to pass additional context and dispatch
   * the target method while the output marshaling stream is available for
   * sending progress feedback.
   *
   * @param args object containing the target method and its arguments
   * @return result object containing return value or exception
   */
   @Override
   public RMIMethodResult invoke (RMIMethodArgs args)
      throws ClassNotFoundException, NoSuchMethodException
   {
     // now done in MethodArgsReader
//      waitUntilReady();

      try
      {
         // Keep track of total calls
         totalInvokeCalls.incrementAndGet();

         Class<?> target_class = getTargetClass(args);

         // Find the method
         Method method = target_class.getMethod(args.targetMethod, args.argTypes);

         // Return new method result object that will carry out call during marshaling
         RMIMethodResult result = args.newResult();
         result.targetMethod = method;
         result.args = args;
         return result;
      }
      catch (ClassNotFoundException e)
      {
         printException(e, args);
         throw e;
      }
      catch (NoSuchMethodException e)
      {
         printException(e, args);
         StringBuilder buf = new StringBuilder();
         buf.append( args.targetClass ).append( '.' ).append( args.targetMethod ).append( '(' );
         if (args.argTypes != null)
         {
            for (int i = 0; i < args.argTypes.length; i++)
            {
               if (i > 0) buf.append(",");
               buf.append(args.argTypes[i].getName());
            }
         }
         buf.append(")");
         throw new NoSuchMethodException(buf.toString());
      }
      catch (RuntimeException e)
      {
         printException(e, args);
         throw e;
      }
      catch (Error e)
      {
         printException(e, args);
         throw e;
      }
   }

   private Class<?> getTargetClass(RMIMethodArgs args) throws ClassNotFoundException {
       Class<?> result;
       if (args.targetObject == null) {
           result = Class.forName(args.targetClass);
       }
       else {
           result = args.targetObject.getClass();
       }
       return result;
   }

   /**
   * Called when this RMI server object is no longer referenced by any RMI clients.
   * If this server object was constructed with <code>stop_when_unreferenced</code> set to
   * <code>true<code>, then <code>WTContext.stop</code> is called to trigger shutdown.
   */
   @Override
   public void unreferenced ()
   {
      serverLogger.debug( SERVICE_NAME + " appears to be unreferenced." );
   }

   private Random getRandom ()
   {
      if (random == null)
         random = new Random();
      return random;
   }

   // Expire nextServer field
   @Override
   public void run ()
   {
      try
      {
         Thread.sleep(5000);
      }
      catch (InterruptedException e)
      {
         // Ignore
      }
   }

   public static Date  getStartDate()
   {
     return ( startDate );
   }

   public static int  getTotalInvokeCalls()
   {
     return ( totalInvokeCalls.get() );
   }

  // Block until ready
  static void  waitUntilReady()
  {
    if ( ready )
      return;

    synchronized ( readyLock )
    {
      while ( !ready )
      {
        try
        {
          readyLock.wait();
        }
        catch ( InterruptedException e )
        {
          // Ignore
        }
      }
    }
  }

  public static boolean  isReady()
  {
    return ( ready );
  }

  static void  signalReady()
  {
    synchronized ( readyLock )
    {
      ready = true;
      readyLock.notifyAll();
    }
  }

   private static void printException (Throwable t, RMIMethodArgs args)
   {
     try
     {
        final String  target_class;
        if (args.targetObject == null)
           target_class = args.targetClass;
        else
           target_class = args.targetObject.getClass().getName();
        serverLogger.error( target_class + "." + args.targetMethod +
                            " from " + getClientHost(), t );
     }
     catch (Exception e)
     {
       serverLogger.error( "Exception occurred", t );  // should never happen
     }
   }

}
