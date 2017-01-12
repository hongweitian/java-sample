package ph.rmi.method;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.WriteAbortedException;

import org.apache.log4j.Logger;


/**
* Class that performs server-side reading of <code>MethodArgs</code> objects.
* It is responsible for creating a <code>MethodContext</code> object for this
* call immediately, so that it will be available to the <code>readObject</code> or
* <code>readExternal<code> methods of argument classes.
*/
public class RMIMethodArgsReader
{
   private static final Logger serverLogger = Logger.getLogger( "ext.rmi.method.server" );
   
   // Primitive type names
   private static final String BOOLEAN_TYPE = Boolean.TYPE.getName();
   private static final String BYTE_TYPE = Byte.TYPE.getName();
   private static final String CHAR_TYPE = Character.TYPE.getName();
   private static final String SHORT_TYPE = Short.TYPE.getName();
   private static final String INT_TYPE = Integer.TYPE.getName();
   private static final String LONG_TYPE = Long.TYPE.getName();
   private static final String FLOAT_TYPE = Float.TYPE.getName();
   private static final String DOUBLE_TYPE = Double.TYPE.getName();
   private static final String VOID_TYPE = Void.TYPE.getName();

   public void readExternal (RMIMethodArgs args, ObjectInput input_stream)
      throws IOException, ClassNotFoundException
   {
      // don't start deserialization until ready
      RMIMethodServerImpl.waitUntilReady();

      try
      {
         // Read flag indicating result needs wrapping
         args.bufferResult = input_stream.readBoolean();

         // Number of previous load leveling redirects
         args.redirects = input_stream.readInt();

         // Read method and class strings
         args.targetMethod = input_stream.readUTF();
         args.targetClass = input_stream.readUTF();

         serverLogger.debug( "reading target object." );

         // Read target object
         args.targetObject = input_stream.readObject();

         serverLogger.debug( "reading arguments." );

         // Read array of arguments and types
         int length = input_stream.readInt();
         if (length > 0)
         {
            args.argObjects = new Object[length];
            args.argTypes = new Class[length];
            String type_name;
            Class type;
            for (int i = 0; i < length; i++)
            {
               type_name = input_stream.readUTF();
               args.argObjects[i] = input_stream.readObject();
               if (type_name.equals(BOOLEAN_TYPE))
                  type = Boolean.TYPE;
               else if (type_name.equals(BYTE_TYPE))
                  type = Byte.TYPE;
               else if (type_name.equals(CHAR_TYPE))
                  type = Character.TYPE;
               else if (type_name.equals(SHORT_TYPE))
                  type = Short.TYPE;
               else if (type_name.equals(INT_TYPE))
                  type = Integer.TYPE;
               else if (type_name.equals(LONG_TYPE))
                  type = Long.TYPE;
               else if (type_name.equals(FLOAT_TYPE))
                  type = Float.TYPE;
               else if (type_name.equals(DOUBLE_TYPE))
                  type = Double.TYPE;
               else if (type_name.equals(VOID_TYPE))
                  type = Void.TYPE;
               else
                  type = Class.forName(type_name);
               args.argTypes[i] = type;
            }
         }
      }
      catch (WriteAbortedException e)
      {
         if (e.detail != null)
            printException(e.detail, args);
         else
            printException(e, args);
         throw e;
      }
      catch (IOException e)
      {
         printException(e, args);
         throw e;
      }
      catch (RuntimeException e)
      {
         printException(e, args);
         throw e;
      }
      catch (Exception e)
      {
         printException(e, args);
         throw new WriteAbortedException((String)null, e);
      }
      catch (ExceptionInInitializerError e)
      {
         Throwable t = e.getException();
         if (t != null)
            printException(t, args);
         else
            printException(e, args);
         throw e;
      }
      catch (Error e)
      {
         printException(e, args);
         throw e;
      }
   }

   private void printException (Throwable t, RMIMethodArgs args)
   {
     try
     {
        if (args.targetClass != null)
        {
           final String  target_class = getTargetClassName(args);
           serverLogger.error( target_class + "." + args.targetMethod + ": ", t );
        }
        else if (args.targetMethod != null)
        {
           serverLogger.error( args.targetMethod + ": ", t );
        }
     }
     catch (Exception e)
     {
        serverLogger.error( "Exception occured", t );  // should never happen...
     }
   }

   private static String getTargetClassName(RMIMethodArgs args) {
       return getTargetClassName(args.targetObject,args.targetClass);
   }

   private static String getTargetClassName(Object target_object, String target_class) {
       if (target_object == null) {
           return target_class;
       }
       return target_object.getClass().getName();
   }

}
