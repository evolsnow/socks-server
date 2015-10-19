package com.evol.crypt;

import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

public class AesCrypt extends CryptBase {
	public final static String CIPHER_AES_128_CFB = "aes-128-cfb";

	public AesCrypt(String methodName, String password) {
		super(methodName, password);
	}

	public static Map<String, String> getCiphers() {
		Map<String, String> ciphers = new HashMap<>();
		ciphers.put(CIPHER_AES_128_CFB, AesCrypt.class.getName());
		return ciphers;
	}

	@Override
	public int getKeyLength() {
		if (_methodName.equals(CIPHER_AES_128_CFB)) {
			return 16;
		}
		return 0;
	}

	@Override
	public int getIVLength() {
		return 16;
	}

	@Override
	protected SecretKey getKey() {
		return new SecretKeySpec(_ssKey.getEncoded(), "AES");
	}

	@Override
	protected StreamBlockCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
		AESFastEngine engine = new AESFastEngine();
		StreamBlockCipher cipher;
		if (_methodName.equals(CIPHER_AES_128_CFB)) {
			cipher = new CFBBlockCipher(engine, getIVLength() * 8);
		} else {
			throw new InvalidAlgorithmParameterException();
		}
		return cipher;
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
}
