package ph.rmi.util;

import java.util.Hashtable;
import java.util.TimeZone;

// Referenced classes of package wt.util:
//            AppletListener, PHContextNestingException, NullAudioClip, Cache, 
//            WTProperties

public final class RMIContext extends Hashtable {

	private RMIContext(ThreadGroup threadgroup) {
		super(25);
		
		group = threadgroup;
		root = threadgroup;
		
		System.out.println((new StringBuilder())
				.append("New WT Context, thread group = ")
				.append(threadgroup.getName()).append(", ")
				.append(System.identityHashCode(threadgroup)).toString());
	}

	public static RMIContext getContext() {
		return getContext(currentThreadGroup());
	}

	public static RMIContext getContext(ThreadGroup threadgroup)
    {
        RMIContext phcontext = new RMIContext(threadgroup);
        return phcontext;
    }

	public static void setContextGroup(ThreadGroup threadgroup) {
		{
			Thread thread = Thread.currentThread();
			System.out.println((new StringBuilder())
					.append("Setting PHContext, thread = ")
					.append(thread.getName())
					.append(", ")
					.append(System.identityHashCode(thread))
					.append(", group = ")
					.append(threadgroup == null ? "null"
							: (new StringBuilder())
									.append(threadgroup.getName())
									.append(", ")
									.append(System
											.identityHashCode(threadgroup))
									.toString()).toString());
		}
		if (threadgroup == null)
			contextThreadGroup.remove();
		else
			contextThreadGroup.set(threadgroup);
	}

	public static ThreadGroup getContextGroup() {
		return (ThreadGroup) contextThreadGroup.get();
	}
	
	public ThreadGroup getThreadGroup() {
		return group;
	}

	public static ThreadGroup currentThreadGroup() {
		Object obj = getContextGroup();
		if (obj != null)
			return ((ThreadGroup) (obj));
		obj = System.getSecurityManager();
		if (obj != null)
			return ((SecurityManager) (obj)).getThreadGroup();
		else
			return Thread.currentThread().getThreadGroup();
	}

	public void interrupt() {
		Thread thread = Thread.currentThread();
		Thread athread[] = new Thread[group.activeCount()];
		group.enumerate(athread);
		for (int i = 0; i < athread.length; i++) {
			Thread thread1 = athread[i];
			if (thread1 == thread || !thread1.isAlive()
					|| "main".equals(thread1.getName()))
				continue;

			System.out.println((new StringBuilder()).append("Interrupting ")
					.append(thread1).toString());
			thread1.interrupt();
		}

	}

	public void setTimeZone(TimeZone timezone1) {
		timezone = timezone1;
	}

	public static void setDefaultTimeZone(TimeZone timezone1) {
		defaultTimezone = timezone1;
	}

	public TimeZone getTimeZone() {
		if (timezone != null)
			return timezone;
		if (defaultTimezone != null)
			return defaultTimezone;
		else
			return TimeZone.getDefault();
	}

	private static final ThreadLocal contextThreadGroup = new ThreadLocal();

	private static TimeZone defaultTimezone = null;

	private final ThreadGroup group;
	private ThreadGroup root;

	private TimeZone timezone;

}