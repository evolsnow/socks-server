package com.evol.crypt;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaChaEngine;

public class Chacha20Crypt extends CryptBase {
	public final static String CIPHER_CHACHA20 = "chacha20";

	public Chacha20Crypt(String methodName, String password) {
		super(methodName, password);
	}

	public static Map<String, String> getCiphers() {
		Map<String, String> ciphers = new HashMap<>();
		ciphers.put(CIPHER_CHACHA20, Chacha20Crypt.class.getName());
		return ciphers;
	}

	@Override
	public int getKeyLength() {
		if (_methodName.equals(CIPHER_CHACHA20)) {
			return 32;
		}
		return 0;
	}

	@Override
	public int getIVLength() {
		return 8;
	}

	@Override
	protected StreamCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
		StreamCipher engine = new ChaChaEngine();
		return engine;
	}

	@Override
	protected void _encrypt(byte[] data, ByteArrayOutputStream stream) {
		int BytesProcessedNum;
		byte[] buffer = new byte[data.length];
		BytesProcessedNum = encCipher.processBytes(data, 0, data.length, buffer, 0);
		stream.write(buffer, 0, BytesProcessedNum);

	}

	@Override
	protected void _decrypt(byte[] data, ByteArrayOutputStream stream) {
		int BytesProcessedNum;
		byte[] buffer = new byte[data.length];
		BytesProcessedNum = decCipher.processBytes(data, 0, data.length, buffer, 0);
		stream.write(buffer, 0, BytesProcessedNum);
	}

	@Override
	protected SecretKey getKey() {
		//return new SecretKeySpec(_ssKey.getEncoded(), "AES");
		return _ssKey;
	}

}
