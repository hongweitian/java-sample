package ph.common;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.beanutils.PropertyUtils;

public class SimpleDiffer {
	
    private static final Logger logger = Logger.getLogger(SimpleDiffer.class.getSimpleName());
    
	private static final Set<Class<?>> WRAPPER_TYPES = getWrapperTypes();
    
    public Map<String, State> compare(Object base, Object working) {
    	try {
			return this.compare("", base, working);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
    	return new TreeMap<String, State>(); 
    }

    /**
     * Compare two java objects, Collection type is not supported.
     * @param nodePath
     * @param base
     * @param working
     * @return Key of map is nodePath, expression of variable; Value of map is change state.
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public Map<String, State> compare(String nodePath, Object base, Object working) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    	
    	State state = State.UNKNOW;
        //return value
        Map<String, State> returnMap = new TreeMap<String, State>();
    	Map<String, State> childMap = new TreeMap<String, State>();
        
        if (base == null && working == null) {
        	state = State.UNTOUCHED;
        } else if (base == null && working != null) {
        	state = State.ADDED;
        } else if (base != null && working == null) {
        	state = State.REMOVED; 
        } else {
        	
        	if (base == working) {
            	state = State.UNTOUCHED;
        	} else {
                Class<?> type = base.getClass();
                if (isSimpleType(type)) {
                	if (base.equals(working)) {
                		state = State.UNTOUCHED;
                	} else {
                		state = State.CHANGED;
                	}
                } else if (isMapType(type)) {
                	Map<?, ?> mapBase = (Map<?, ?>)base;
                	Map<?, ?> mapWorking = (Map<?, ?>)working;
                	
                	Collection<?> addedKeys = findAddedKeys(mapBase, mapWorking);
                	Collection<?> removedKeys = findRemovedKeys(mapBase, mapWorking);
                	Collection<?> knownKeys = findKnownKeys(mapBase, mapWorking);
                	//
                	for (Object key : addedKeys) {
                		Map<String, State> subMap = this.compare(nodePath + "(" + key + ")", null, mapWorking.get(key));
                		childMap.putAll(subMap);
                	}
                	for (Object key : removedKeys) {
                		Map<String, State> subMap = this.compare(nodePath + "(" + key + ")", mapBase.get(key), null);
                		childMap.putAll(subMap);
                	}
                	//
                	for (Object key : knownKeys) {
                		Map<String, State> subMap = this.compare(nodePath + "(" + key + ")", mapBase.get(key), mapWorking.get(key));
                		childMap.putAll(subMap);
                	}
                } else if (isNormalBeanType(type)) {
                	Map description = PropertyUtils.describe(working);
                	java.util.Iterator<String> it = description.keySet().iterator();
                    while (it.hasNext()) {
                    	String property = it.next();
                    	if (property.equals("class")) {
                    		continue;
                    	}
        				Object baseValue = PropertyUtils.getProperty(base, property);
        				Object workingValue = PropertyUtils.getProperty(working, property);

        				String subNodePath = nodePath.length()==0? property : nodePath + "." + property;
                		Map<String, State> subMap = this.compare(subNodePath, baseValue, workingValue);
                		childMap.putAll(subMap);
                    }
                } else {
                	logger.severe("Unsupport type=" + type);
                	if (base.equals(working)) {
                		state = State.UNTOUCHED;
                	} else {
                		state = State.CHANGED;
                	}
                }
        	}
        }

        if (state.equals(State.UNKNOW)) {
        	Collection<State> childStates = childMap.values();
    		if (childStates.contains(State.ADDED) || childStates.contains(State.REMOVED) || childStates.contains(State.CHANGED)) {
    			state = State.CHANGED;
    		} else {
    			state = State.UNTOUCHED;
    		}
        }
        
        returnMap.put(nodePath, state);
        returnMap.putAll(childMap);

        return returnMap;
    }
    
    public static Object getParentProperty(Object bean, String nodePath) {
    	if (bean == null || nodePath == null || nodePath.length()==0) {
    		return null;
    	}

    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	
    	return getProperty(bean, nodePath);
    }
    
    public static Object getParentParentProperty(Object bean, String nodePath) {
    	if (bean == null || nodePath == null || nodePath.length()==0) {
    		return null;
    	}

    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	
    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	
    	return getProperty(bean, nodePath);
    }
    
    public static Object getParentParentParentProperty(Object bean, String nodePath) {
    	if (bean == null || nodePath == null || nodePath.length()==0) {
    		return null;
    	}

    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	
    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	nodePath = getParentPath(nodePath);
    	if (nodePath == null) {
    		return null;
    	}
    	
    	return getProperty(bean, nodePath);
    }
    
	public static String getParentPath(String expression) {

		if ((expression == null) || (expression.length() == 0)) {
			return null;
		}
		boolean indexed = false;
		boolean mapped = false;
		for (int i = expression.length()-1; i >= 0; i--) {
			char c = expression.charAt(i);
			if (indexed) {
				if (c == '[') {
					return expression.substring(0, i);
				}
			} else if (mapped) {
				if (c == '(') {
					return expression.substring(0, i);
				}
			} else {
				if (c == '.')
					return expression.substring(0, i);
				if (c == ')') {
					mapped = true;
				} else if (c == ']') {
					indexed = true;
				}
			}
		}
		return null;

	}
    
    /**
     * Get Value Of Special Property By Giving NodePath
     * @param target
     * @param nodePath
     * @return
     */
    public static Object getProperty(Object target, String nodePath) {
    	if (target == null || nodePath == null) {
    		return null;
    	}
    	
    	if (nodePath.length()==0) {
    		return target;
    	}
    	
        try {
			return PropertyUtils.getProperty(target, nodePath);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
        
        return null;
    }

	public static boolean isEqual(final Object a, final Object b)
	{
		if (a != null)
		{
			return a.equals(b);
		}
		else if (b != null)
		{
			return b.equals(a);
		}
		return true;
	}
	
	public static Collection<?> findAddedKeys(Map base, Map working)
	{
		final Set<?> source = working.keySet();
		final Set<?> filter = base.keySet();
		return filteredCopyOf(source, filter);
	}

	public static Collection<?> findRemovedKeys(Map base, Map working)
	{
		final Set<?> source = base.keySet();
		final Set<?> filter = working.keySet();
		return filteredCopyOf(source, filter);
	}

	public static Collection<?> findKnownKeys(Map base, Map working)
	{
		final Set<?> keys = working.keySet();
		final Collection<?> changed = new ArrayList(keys);
		changed.removeAll(findAddedKeys(base, working));
		changed.removeAll(findRemovedKeys(base, working));
		return changed;
	}

	public static <T> Collection<? extends T> filteredCopyOf(
			final Collection<? extends T> source,
			final Collection<? extends T> filter) {
		
		final Collection<T> copy;
		if (source != null) {
			copy = new LinkedList<T>(source);
		} else {
			copy = new LinkedList<T>();
		}
		if (filter != null) {
			copy.removeAll(new ArrayList<T>(filter));
		}
		return copy;
	}

	private static Set<Class<?>> getWrapperTypes()
	{
		final Set<Class<?>> wrapperTypes = new HashSet<Class<?>>();
		wrapperTypes.add(Boolean.class);
		wrapperTypes.add(Character.class);
		wrapperTypes.add(Byte.class);
		wrapperTypes.add(Short.class);
		wrapperTypes.add(Integer.class);
		wrapperTypes.add(Long.class);
		wrapperTypes.add(Float.class);
		wrapperTypes.add(Double.class);
		wrapperTypes.add(Void.class);
		return wrapperTypes;
	}

	public static boolean isPrimitiveWrapperType(final Class<?> clazz)
	{
		return clazz != null && WRAPPER_TYPES.contains(clazz);
	}

	public static boolean isSimpleType(final Class<?> clazz)
	{
		if (clazz != null)
		{
			if (isPrimitiveType(clazz))
			{
				return true;
			}
			else if (isPrimitiveWrapperType(clazz))
			{
				return true;
			}
			else if (clazz.isEnum())
			{
				return true;
			}
			else if (CharSequence.class.isAssignableFrom(clazz))
			{
				return true;
			}
			else if (Number.class.isAssignableFrom(clazz))
			{
				return true;
			}
			else if (Date.class.isAssignableFrom(clazz))
			{
				return true;
			}
			else if (URI.class.equals(clazz))
			{
				return true;
			}
			else if (URL.class.equals(clazz))
			{
				return true;
			}
			else if (Locale.class.equals(clazz))
			{
				return true;
			}
			else if (Class.class.equals(clazz))
			{
				return true;
			}
		}
		return false;
	}
	
	public static boolean isPrimitiveType(final Class<?> clazz)
	{
		return clazz != null && clazz.isPrimitive();
	}
	
	public static boolean isCollectionType(final Class<?> clazz) {
		return Collection.class.isAssignableFrom(clazz);
	}
	
	public static boolean isMapType(final Class<?> clazz) {
		return Map.class.isAssignableFrom(clazz);
	}
	
	public static boolean isNormalBeanType(final Class<?> clazz) {
		return !clazz.isPrimitive() && !clazz.isArray() && !isCollectionType(clazz) && !isMapType(clazz);
	}
	
	public enum State
	{
		
		/**
		 * The value has not been initialed.
		 */
		UNKNOW,
		
		/**
		 * The value has been added to the working object.
		 */
		ADDED,

		/**
		 * The value has been changed compared to the base object.
		 */
		CHANGED,

		/**
		 * The value has been removed from the working object.
		 */
		REMOVED,

		/**
		 * The value is identical between working and base
		 */
		UNTOUCHED,

	}

}
