// $Header: /Windchill/current/wt/method/MethodResult.java 12    11/22/98 9:39p Jdg $

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

import java.io.BufferedInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.WriteAbortedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ph.rmi.util.RMIMethodResultException;
import ph.rmi.util.RMIObjectOutputStream;

/**
 * Class that encapsulates result marshaling for method invocations between
 * client and server. This allows special processing to be incorporated into
 * reply marshaling. To support live feedback and interrupt processing while
 * processing a call, the target method is actually invoked from within the
 * <code>writeExternal</code> method of this class.
 */
public class RMIMethodResult implements Externalizable, Runnable {
	private static final Logger clientLogger = Logger
			.getLogger("ext.rmi.method.client"); // see Javadoc before using
													// LoggerFacade!
	private static final Logger serverLogger = Logger
			.getLogger("ext.rmi.method.server");

	static final long serialVersionUID = 4;

	// Target method of the remote invoke call
	Method targetMethod;

	// Arguments to the remote invoke call
	RMIMethodArgs args;

	// The result object returned by the target invocation
	Object result;

	// Exception thrown during remote processing
	Throwable serverException;

	// Has remote interrupt been performed
	boolean interrupted;

	// Construction is performed by MethodArgs class/subclass factory method
	public RMIMethodResult() {
		super();
	}

	// Called during marshaling on client side
	@Override
	public void readExternal(ObjectInput input_stream) throws IOException,
			ClassNotFoundException {
		// Wrap stream with a buffered stream if necessary
		if (input_stream.readBoolean()) {
			clientLogger.debug("Creating BufferedInputStream.");
			input_stream = new ObjectInputStream(new BufferedInputStream(
					(InputStream) input_stream));
		}

		// Support interruping server thread on local interrupt
		Thread current_thread = Thread.currentThread();

		// Read objects until non-feedback object is receieved
		Object obj;
		while (true) {
			// Check and clear interrupt in two steps - see
			// WTThread.isInterrupted
			if (current_thread.isInterrupted()) {
				Thread.interrupted();
				throw new InterruptedIOException("线程调用被中断");
			}

			obj = input_stream.readObject();

			if (obj != null) {
				if (obj instanceof InvocationTargetException) {
					clientLogger.debug("received server exception.");
					serverException = ((InvocationTargetException) obj)
							.getTargetException();
					break;
				}
			}
			clientLogger.debug("received method result.");
			result = obj;
			break;
		}

	}

	@Override
	public void writeExternal(ObjectOutput output_stream) throws IOException {
		try {
			boolean writing = false;

			try {
				// Wrap stream if necessary
				output_stream.writeBoolean(args.bufferResult);
				if (args.bufferResult)
					output_stream = new RMIObjectOutputStream(
							(OutputStream) output_stream);

				serverLogger.debug("Invoking target method.");

				// Invoke the target method
				result = targetMethod.invoke(args.targetObject, args.argObjects);

				serverLogger.debug("sending result.");

				// Send the target method's result back to client
				writing = true;
				output_stream.writeObject(result);
			} catch (InvocationTargetException e) {
				serverException = e.getTargetException();
				if (serverException instanceof ExceptionInInitializerError) {
					Throwable t = ((ExceptionInInitializerError) serverException).getException();
					if (t != null)
						serverException = t;
				}
			} catch (WriteAbortedException e) {
				if (e.detail != null)
					serverException = e.detail;
			} catch (Exception e) {
				serverException = e;
			} catch (ExceptionInInitializerError e) {
				Throwable t = e.getException();
				if (t != null)
					serverException = t;
				else
					serverException = e;
			} catch (ThreadDeath e) {
				throw e;
			} catch (Throwable t) {
				serverException = t;
			}

			// Abort result writing if an exception was caught
			if (serverException != null) {
				Throwable server_exception = serverException;

				serverLogger.debug("sending server exception.");

				if (writing) {
					if (!(server_exception instanceof Exception))
						server_exception = new InvocationTargetException(
								server_exception);
					throw new WriteAbortedException((String) null,
							(Exception) new RMIMethodResultException(
									server_exception));
				} else {
					output_stream.writeObject(new RMIMethodResultException(
							server_exception));
				}
			}
		} catch (RuntimeException | Error e) {
			// Print trace here to aid debugging of runtime exceptions in server
			printException(e, args);
			throw e;
		} finally {
			
		}

	}

	/**
	 * Interrupt server-side method corresponding to this call
	 */
	@Override
	public void run() {
		// Prevent double interrupt of the same remote method by two local
		// threads
		synchronized (this) {
			if (interrupted)
				return;
			interrupted = false;
		}
	}

	private void printException (Throwable t, RMIMethodArgs args)
	   {
	      final boolean  client_io_exception = false;
	      final String  target_class = ( ( args.targetObject != null ) ? args.targetObject.getClass().getName() : args.targetClass );
	      serverLogger.log( client_io_exception ? Level.INFO : ( ( t instanceof VirtualMachineError ) ? Level.FATAL : Level.ERROR ),
	                        (client_io_exception ? "Informative: " : "") +
	                        target_class + "." + args.targetMethod +
	                        (client_io_exception ? " : " + t.toString() : ""),
	                        (client_io_exception ? null : t) );
	   }

}
