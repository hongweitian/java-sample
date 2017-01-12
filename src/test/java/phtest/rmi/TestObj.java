package phtest.rmi;

import org.apache.log4j.Logger;

public class TestObj {

	private static final Logger LOG = Logger.getLogger(TestObj.class.getName());

	public static String testFun(String test) {
		String str = "Hello world - ";
		LOG.debug(str + test);

		return str + test;
	}

}
