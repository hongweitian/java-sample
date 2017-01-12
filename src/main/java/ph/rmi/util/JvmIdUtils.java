/* bcwti
 *
 * Copyright (c) 2013 Parametric Technology Corporation (PTC). All Rights Reserved.
 *
 * This software is the confidential and proprietary information of PTC
 * and is subject to the terms of a software license agreement. You shall
 * not disclose such confidential information and shall use it only in accordance
 * with the terms of the license agreement.
 *
 * ecwti
 */
package ph.rmi.util;


import java.lang.management.ManagementFactory;
import java.net.InetAddress;


/** Small, targeted, non-comprehensive collection of utilities for use in identifying the current JVM.
 */
public final class  JvmIdUtils
{
  /* The implementation of this initializer makes assumptions about the relationship
   * between the name of the RuntimeMXBean and the local JVM id / process id.
   */
  private static final long  START_TIME;
  private static final String  canonicalHostname;
  private static final int  jvmId;
  private static final String  jvmHost;
  static
  {
    START_TIME = ManagementFactory.getRuntimeMXBean().getStartTime();  // should not raise SecurityException in *any* case

    /* If a security manager is in use, try to use wt.security.NetAccess so as to avoid obnoxious attempts to fetch
     * crossdomain.xml from client host when running in the Java Plug-In.  If a security manager is not in use or if
     * wt.security.NetAccess is not present, does not provide the necessary APIs (i.e. is an old version of the class!),
     * or otherwise fails, the normal Java APIs will be tried, catching all exceptions as it goes and falling back to
     * appropriate defaults.  See related comments in UniqueIdentifier source as well.
     */
    String  jvmName = null;
    String  tmpCanonicalHostname = null;
    if ( System.getSecurityManager() != null )
      try
      {
        final Class<?>  netAccessClass = Class.forName( "wt.security.NetAccess" );
        final Object  netAccessObject = netAccessClass.getMethod( "getNetAccess" ).invoke( null );
        try
        {
          final Object  localHostObject = netAccessClass.getMethod( "getLocalHost" ).invoke( netAccessObject );
          tmpCanonicalHostname = (String) netAccessClass.getMethod( "getCanonicalHostName", InetAddress.class ).invoke( netAccessObject, localHostObject );
        }
        catch ( VirtualMachineError e )
        {
          throw e;
        }
        catch ( Throwable t )
        {
          // ignore
        }
        jvmName = (String) netAccessClass.getMethod( "getJvmName" ).invoke( netAccessObject );
      }
      catch ( VirtualMachineError e )
      {
        throw e;
      }
      catch ( Throwable t )
      {
        // ignore
      }

    if ( tmpCanonicalHostname == null )
      try
      {
        tmpCanonicalHostname = InetAddress.getLocalHost().getCanonicalHostName();
      }
      catch ( VirtualMachineError e )
      {
        throw e;
      }
      catch ( Throwable t )
      {
        tmpCanonicalHostname = "localhost";
      }
    canonicalHostname = tmpCanonicalHostname;

    int  tmpJvmId = -1;
    String  tmpJvmHost = canonicalHostname;
    try
    {
      if ( jvmName == null )
        jvmName = ManagementFactory.getRuntimeMXBean().getName();
      if ( jvmName != null )
      {
        final int  atIndex = jvmName.indexOf( '@' );
        final boolean  hasAt = ( atIndex >= 0 );
        final String  localJvmIdString = ( hasAt ? jvmName.substring( 0, atIndex ) : jvmName );
        tmpJvmId = Integer.parseInt( localJvmIdString );  // WARNING: Can throw NumberFormatException
        tmpJvmHost = ( hasAt ? jvmName.substring( atIndex + 1 ) : jvmName );
      }
    }
    catch ( VirtualMachineError e )
    {
      throw e;
    }
    catch ( Throwable t )
    {
      // do nothing...
    }
    jvmId = tmpJvmId;
    jvmHost = tmpJvmHost;
  }

  public static int  getJvmId()
  {
    return ( jvmId );
  }

  public static String  getJvmHost()
  {
    return ( jvmHost );
  }

  /** Returns "localhost" if canonical hostname cannot be obtained,
   *  rather than throwing an exception.
   */
  public static String  getCanonicalHostname()
  {
    return ( canonicalHostname );
  }

  /** Synonym for ManagementFactory.getRuntimeMXBean().getStartTime().
   */
  public static long  getStartTime()
  {
    return ( START_TIME );
  }
}
