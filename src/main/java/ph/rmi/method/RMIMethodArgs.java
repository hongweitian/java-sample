package ph.rmi.method;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.log4j.Logger;

public class RMIMethodArgs
   implements Externalizable
{
   
   private static final Logger  clientLogger = Logger.getLogger( "ext.rmi.method.client" );
   private static final Logger serverLogger = Logger.getLogger( "ext.rmi.method.server" );
   
   static final long  serialVersionUID = 5;
   
   // Tell server we will be wrapping a buffered object stream on results.
   // Set by RemoteMethodServer class intialization if unable to use fully
   // buffered socket stream.
   static boolean BUFFER_RESULT;

   // The target method and arguments
   boolean bufferResult = BUFFER_RESULT;
   public String targetMethod;  // used outside package
   public String targetClass;  // used outside package
   public Object targetObject;  // used outside package
   public Class argTypes[];  // used outside package (but only from tests)
   public Object argObjects[];  // used outside package

   // Number of times server load exception has caused redirection
   int redirects;

   public RMIMethodArgs ()
   {
      super();
   }

   // Allow subclasses to use their own corresponding result class
   public RMIMethodResult newResult ()
   {
      return new RMIMethodResult();
   }

   // Called during marshaling on server side
   @Override
   public void readExternal (ObjectInput input_stream)
      throws IOException, ClassNotFoundException
   {
      // Dynamically load reader to prevent client-side loading of server-only
      // classes in VMs that perform early/complete class resolution at load time.
	  RMIMethodArgsReader reader = new RMIMethodArgsReader();
      // Delegate server-side reading to reader instance
      reader.readExternal(this, input_stream);
   }

   // Called during marshaling on the client side
   @Override
   public void writeExternal (ObjectOutput output_stream)
      throws IOException
   {
      try
      {
         // Indicate need to wrap result stream in buffered stream
         output_stream.writeBoolean(bufferResult);

         // Send number of redirects that have taken place
         output_stream.writeInt(redirects);

         // Send target method and class names
         output_stream.writeUTF(targetMethod);
         output_stream.writeUTF(targetClass);

         if ( targetObject != null )
           serverLogger.debug( "sending target object." );

         // Send target object
         output_stream.writeObject(targetObject);

         if ( argObjects != null )
           serverLogger.debug( "sending arguments." );

         // Send Object array
         if (argObjects == null)
            output_stream.writeInt(0);
         else
         {
            output_stream.writeInt(argObjects.length);
            for (int i = 0; i < argObjects.length; i++)
            {
               output_stream.writeUTF(argTypes[i].getName());
               output_stream.writeObject(argObjects[i]);
            }
         }
      }
      finally
      {
         output_stream.flush();
      }

      clientLogger.debug( "waiting for response..." );
   }
}
