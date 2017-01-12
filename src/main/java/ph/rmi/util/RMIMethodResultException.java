package ph.rmi.util;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.WriteAbortedException;
import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

/**
* A special exception used internally to the <code>MethodResult</code> class.
* It is used to wrap exceptions thrown by the invoked target method for communication
* back to the client.
*/
public class  RMIMethodResultException
  extends InvocationTargetException
  implements Externalizable
{
   private static final long  serialVersionUID = 8708943081711431837L;

   private Throwable serverException;
   private String serverExceptionMessage;
   private StackTraceElement[] ste;
   private String SERVER_ERROR = "*ERROR* Server side Exception has occured: See MethodServer log for details";

   private static final Logger logger = Logger.getLogger( RMIMethodResultException.class.getName() );

   public RMIMethodResultException ()
   {
   }

   public RMIMethodResultException (Throwable server_exception)
   {
      serverException = server_exception;
      ste = server_exception.getStackTrace();
      serverExceptionMessage = server_exception.getMessage();
      if (serverExceptionMessage == null) {
         serverExceptionMessage = serverException.toString();
      }
   }

   @Override
   public void readExternal (ObjectInput input_stream)
      throws IOException, ClassNotFoundException
   {
      ObjectInputStream ois = new ObjectInputStream((InputStream)input_stream);
      try
      {
         serverExceptionMessage = (String) ois.readObject();
         ste = (StackTraceElement[]) ois.readObject();
         serverException = (Throwable) ois.readObject();
      }
      catch (IOException e)
      {
         if (!(e instanceof WriteAbortedException)) {
            throw new WriteAbortedException((String)null, (Exception)e);
         }
         else { // WriteAbortedException
            throw e; // re-throw
         }
      }

   }

   @Override
   public void writeExternal (ObjectOutput output_stream)
      throws IOException
   {
      ObjectOutputStream osbaos = new ObjectOutputStream(new ByteArrayOutputStream());
      ObjectOutputStream oos = new ObjectOutputStream((OutputStream)output_stream);
      try
      {
         if (serverException instanceof NotSerializableException) {
           logger.error( "", serverException );  // stack server side exceptions for reference
           serverException = null; //  hide serialization exception from client
           ste = null;             //  hide serialization stack trace from client
         }
         else {
           osbaos.writeObject(serverException); // test to see if we can serialize this Throwable
         }
         oos.writeObject(serverExceptionMessage);
         oos.writeObject(ste);
         oos.writeObject(serverException);
         // serverException.printStackTrace();  // stack server side exceptions for reference
      }
      catch (IOException e)
      {
         if (e instanceof NotSerializableException) {
            logger.error( "", e );  // stack server side NotSerializable exceptions for reference
            serverException = null; // null out, exception can't be serialized
            throw new WriteAbortedException (serverExceptionMessage,(Exception)this);
         }
         else {
            throw new WriteAbortedException (e.toString(),(Exception)null);
         }
      }
   }

   @Override
   public String getMessage()
   {
      logger.error(serverExceptionMessage);
      if (serverException==null)
         return SERVER_ERROR;
      else
         return "";
   }

   @Override
   public Throwable fillInStackTrace() {
      Throwable t = super.fillInStackTrace();
      if (ste != null)
         t.setStackTrace(ste);
      return t;
   }

   // overridden method in InvocationTargetException
   @Override
   public Throwable getTargetException()
   {
      return serverException;
   }

   // overridden method in InvocationTargetException
   @Override
   public Throwable getCause()
   {
      return serverException;
   }
}
