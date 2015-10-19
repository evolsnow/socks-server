package com.evol.misc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.SecureRandom;

public class Util {
	public static String getVersion() {
		return "0.1";
	}

	public static String byteArrayToString(byte[] a) {
		StringBuilder sb = new StringBuilder(2 * a.length);
		for (byte b : a) {
			sb.append(String.format("%x", b & 0xff));
		}
		return sb.toString();
	}

	public static byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		new SecureRandom().nextBytes(bytes);
		return bytes;
	}

	public static String getErrorMessage(Throwable e) {
		Writer writer = new StringWriter();
		PrintWriter pwriter = new PrintWriter(writer);
		e.printStackTrace(pwriter);
		return writer.toString();

	}

	public static String getRequestedHostInfo(byte[] data) {
		String info = "";
		int port;
		int neededLength;
		switch (data[0]) {
		case 0x1:
			// IPv4 address, 4 bytes of ip, 2 bytes of port
			neededLength = 6;
			if (data.length > neededLength) {
				port = getPort(data[5], data[6]);
				info = String.format("%d.%d.%d.%d:%d", data[1], data[2], data[3], data[4], port);
			}
			break;
		case 0x3:
			// domain
			neededLength = data[1];
			if (data.length > neededLength + 3) {
				try {
					port = getPort(data[neededLength + 2], data[neededLength + 3]);
					String domain = new String(data, 2, neededLength, "UTF-8");
					info = String.format("%s:%d", domain, port);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			break;
		case 0x4:
			// IP v6 Address, 16 bytes of IP, 2 bytes of port
			neededLength = 18;
			if (data.length > neededLength) {
				port = getPort(data[17], data[18]);
				info = String.format("%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%d", data[1], data[2], data[3], data[4],
						data[5], data[6], data[7], data[8], data[9], data[10], data[11], data[12], data[13], data[14],
						data[15], data[16], port);
			}
			break;
		}
		return info;
	}

	public static short byteToUnsignedByte(byte b) {
		return (short) (b & 0xff);
	}

	public static int getPort(byte b1, byte b2) {
		return byteToUnsignedByte(b1) << 8 | byteToUnsignedByte(b2);
	}

}
