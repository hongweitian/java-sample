package phtest.common;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ph.common.SimpleDiffer;

public class SimpleDifferTest {
	
	@Test
    public void testA() throws Exception {

        Class<Tmp> c = Tmp.class;

        Field f = c.getField("a");
        Type t = f.getGenericType();
        Class<?> cls = f.getType();
        System.out.println(t instanceof Class);
        System.out.println(t == cls);
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            for (Type type : pt.getActualTypeArguments()) {
                System.out.println("Parameter: " + type);
            }
        } else if (t instanceof GenericArrayType) {
            GenericArrayType gat = (GenericArrayType) t;
            System.out.println("Component: " + gat.getGenericComponentType());
        } else {
            System.out.println("non generic");
        }
/*
        for(TypeVariable<Class<List<String>>> typeVariable : f.getGenericType().)
        {
            for(Type type : typeVariable.getBounds())
            {
                System.out.println(type);
            }
        }
*/
//        f.getType().getTypeParameters()


        System.out.println("Test");

        ClassA obj1 = new ClassA();
        obj1.a = 1;
        obj1.b = "Hello world";
        obj1.messageList = new ArrayList<String>();
        obj1.messageList.add("String 1");
        obj1.messageList.add("String 2");
        obj1.nameMap = new HashMap<String, ClassB>();
        ClassB tmp1 = new ClassB();
        tmp1.id = 1;
        tmp1.name = "Tonio";
        tmp1.num = 10;
        obj1.nameMap.put("tonio", tmp1);
        tmp1 = new ClassB();
        tmp1.id = 2;
        tmp1.name = "Douglas";
        tmp1.num = 20;
        obj1.nameMap.put("douglas", tmp1);

        ClassA obj2 = new ClassA();
        obj2.foo = 1;
        obj2.a = 2;
        obj2.b = "G'bye world";
        obj2.messageList = new ArrayList<String>();
        obj2.messageList.add("String 1");
        obj2.messageList.add("String two");
        obj2.messageList.add("String 3");
        obj2.nameMap = new HashMap<String, ClassB>();
        obj2.bool = false;
        tmp1 = new ClassB();
        tmp1.id = 1;
        tmp1.name = "Tonio";
        tmp1.num = 10;
        obj2.nameMap.put("tonio", tmp1);
        tmp1 = new ClassB();
        tmp1.id = 4;
        tmp1.name = "Douglas";
        tmp1.num = 21;
        obj2.nameMap.put("douglas", tmp1);

        SimpleDiffer dg = new SimpleDiffer();
        Map<String, SimpleDiffer.State> diffs = dg.compare(obj1, obj2);
        System.out.println("======================================");
        //org.apache.commons.lang.
        //System.out.println(BeanUtils.describe(obj1));
        for (String key : diffs.keySet()) {
        	System.out.print(key + "=" + diffs.get(key));
            System.out.print(", base=" + SimpleDiffer.getProperty(obj1, key));
            System.out.print(", working=" + SimpleDiffer.getProperty(obj2, key));
            System.out.println();
        }
    }
	
	@Test
    public void test2() throws Exception {
        ClassA obj1 = new ClassA();
        obj1.a = 1;
        obj1.b = "Hello world";
        obj1.nameMap = new HashMap<String, ClassB>();
        ClassB tmp1 = new ClassB();
        tmp1.id = 1;
        tmp1.name = "Tonio";
        tmp1.num = 10;
        obj1.nameMap.put("tonio", tmp1);
        tmp1 = new ClassB();
        tmp1.id = 2;
        tmp1.name = "Douglas";
        tmp1.num = 20;
        obj1.nameMap.put("douglas", tmp1);

        ClassA obj2 = new ClassA();
        obj2.foo = 1;
        obj2.a = 2;
        obj2.b = "G'bye world";
        obj2.nameMap = new HashMap<String, ClassB>();
        obj2.bool = false;
        tmp1 = new ClassB();
        tmp1.id = 1;
        tmp1.name = "Tonio";
        tmp1.num = 10;
        tmp1 = new ClassB();
        tmp1.id = 2;
        tmp1.name = "Douglas";
        tmp1.num = 20;
        obj2.nameMap.put("douglas", tmp1);

        SimpleDiffer dg = new SimpleDiffer();
        Map<String, SimpleDiffer.State> diffs = dg.compare(obj1, obj2);
        System.out.println("======================================");
        //org.apache.commons.lang.
        //System.out.println(BeanUtils.describe(obj1));
        for (String key : diffs.keySet()) {
        	System.out.print(key + "=" + diffs.get(key));
            System.out.print(", base=" + SimpleDiffer.getProperty(obj1, key));
            System.out.print(", working=" + SimpleDiffer.getProperty(obj2, key));
            System.out.println();
        }
    }
	
	
	@Test
    public void test3() throws Exception {
		Map<String, Object> objA = new HashMap<String, Object>();
		Map<String, Object> objAA = new HashMap<String, Object>();
		Map<String, Object> objAAA = new HashMap<String, Object>();
		objA.put("C1", objAA);
		objAA.put("C1", objAAA);
		
		objA.put("P1", "V1");
		objA.put("P2", "V2");
		objAA.put("P1", "V1");
		objAAA.put("P1", "V1");

		Map<String, Object> objB = new HashMap<String, Object>();
		Map<String, Object> objBB = new HashMap<String, Object>();
		Map<String, Object> objBBB = new HashMap<String, Object>();
		objB.put("C1", objBB);
		objBB.put("C1", objBBB);
		
		objB.put("P1", "V1");
		objB.put("P2", "V2");
		objBB.put("P1", "V1");
		objBBB.put("P1", "V1-1");
		
        SimpleDiffer dg = new SimpleDiffer();
        Map<String, SimpleDiffer.State> diffs = dg.compare(objA, objB);
        System.out.println("======================================");
        //org.apache.commons.lang.
        //System.out.println(BeanUtils.describe(obj1));
        for (String key : diffs.keySet()) {
        	System.out.print(key + "=" + diffs.get(key));
            System.out.print(", base=" + SimpleDiffer.getProperty(objA, key));
            System.out.print(", working=" + SimpleDiffer.getProperty(objB, key));
            System.out.println();
        }
    }
	
	public class Tmp {
        public List<?> a;

		public List<?> getA() {
			return a;
		}

		public void setA(List<?> a) {
			this.a = a;
		}
    }
	
    public class ClassB {
        public long id;
        public String name;
        public int num;
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getNum() {
			return num;
		}
		public void setNum(int num) {
			this.num = num;
		}
    }

    public class ClassAA {
        public int foo = 1;

		public int getFoo() {
			return foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}
    }

    public class ClassA extends ClassAA {
        private int a;
        public String b;
        public List<String> messageList;
        public Map<String, ClassB> nameMap;

        private boolean bool = true;
        
        public String getB() {
			return b;
		}

		public void setB(String b) {
			this.b = b;
		}

		public List<String> getMessageList() {
			return messageList;
		}

		public void setMessageList(List<String> messageList) {
			this.messageList = messageList;
		}

		public Map<String, ClassB> getNameMap() {
			return nameMap;
		}

		public void setNameMap(Map<String, ClassB> nameMap) {
			this.nameMap = nameMap;
		}

		public void setA(int a) {
			this.a = a;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public int getA() {
            return a;
        }

        public boolean isBool() {
            return bool;
        }
    }
}
