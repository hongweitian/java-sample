package ph.rmi.util;

import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
* An object output stream extension that allows runtime configuration of the
* default protocol version. The JDK 1.2 introduced a new stream protocol version
* which will cause compatibility problems when read by pre-JDK1.1.7 object stream
* implementations.  This class defaults the stream protocol to it's JDK 1.1 version
* even when run under JDK 1.2.  It allows runtime configuration through Windchill
* property called <code>wt.objectStreamProtocolVersion</code>.
*/
public class RMIObjectOutputStream extends ObjectOutputStream
{
   // Method to invoke to set protocol version
   private static final String METHOD_NAME = "useProtocolVersion";
   private static final Method METHOD;
   private static final Class[] METHOD_ARG_TYPES = { int.class };
   private static final Object[] METHOD_ARGS = { null };

   static
   {
      // Perform static class initialization
      int protocol_version = 1;

      // Attempt to obtain reference to JDK 1.2 useProtocolVersion method
      Method aMethod;
      try
      {
         aMethod = (ObjectOutputStream.class).getMethod(METHOD_NAME, METHOD_ARG_TYPES);
         METHOD_ARGS[0] = protocol_version;
      }
      catch (Throwable e)
      {
         // pre JDK 1.2
         aMethod = null;
      }
      METHOD = aMethod;
   }

   public RMIObjectOutputStream (OutputStream out)
      throws IOException
   {
      super(out);
      if (METHOD != null)
      {
         try
         {
            METHOD.invoke(this, METHOD_ARGS);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }
}
