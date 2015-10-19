package com.evol.crypt;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.evol.misc.Util;

public class CryptFactory {

	private static final Map<String, String> crypts = new HashMap<String, String>() {
		/**
		 * auto generated
		 */
		private static final long serialVersionUID = 8738157774510030046L;

		{
			putAll(AesCrypt.getCiphers());
			putAll(Chacha20Crypt.getCiphers());
			//putAll(Rc4md5Crypt.getCiphers());
			// TODO: other crypts
		}
	};

	private static Logger logger = Logger.getLogger(CryptFactory.class.getName());

	/*
	 * public static boolean isCipherExisted(String methodName) { return
	 * (crypts.get(methodName) != null); }
	 */

	public static ICrypt getCrypt(String methodName, String password) {
		try {
			String cipherClsName = crypts.get(methodName);
			Class<?> cipherCls = Class.forName(cipherClsName);
			Class<?>[] neededParamCls = new Class<?>[2];
			neededParamCls[0] = String.class;
			neededParamCls[1] = String.class;
			Constructor<?> constructor = cipherCls.getConstructor(neededParamCls);

			Object[] paramObjs = new Object[2];
			paramObjs[0] = methodName;
			paramObjs[1] = password;
			Object obj = constructor.newInstance(paramObjs);
			return (ICrypt) obj;
		} catch (Exception e) {
			logger.warning(Util.getErrorMessage(e));
		}
		return null;
	}
}
