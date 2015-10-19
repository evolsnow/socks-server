package com.evol.crypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import com.evol.misc.Util;

public abstract class CryptBase implements ICrypt {
	// abstract methods
	protected abstract <T> StreamCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException;

	protected abstract SecretKey getKey();

	protected abstract void _encrypt(byte[] data, ByteArrayOutputStream stream);

	protected abstract void _decrypt(byte[] data, ByteArrayOutputStream stream);

	protected final String _methodName;
	protected final SecretKey _key;
	protected final ShadowsocksKey _ssKey;
	protected final int _ivLength;
	protected final int _keyLength;
	protected boolean _encryptIVSet;
	protected boolean _decryptIVSet;
	protected byte[] _encryptIV;
	protected byte[] _decryptIV;
	protected final Lock encLock = new ReentrantLock();
	protected final Lock decLock = new ReentrantLock();
	protected StreamCipher encCipher;
	protected StreamCipher decCipher;
	/*
	 * protected StreamBlockCipher encCipher; protected StreamBlockCipher
	 * decCipher;
	 */
	private Logger logger = Logger.getLogger(CryptBase.class.getName());

	public CryptBase(String methodName, String password) {
		_methodName = methodName.toLowerCase();
		_ivLength = getIVLength();
		_keyLength = getKeyLength();
		_ssKey = new ShadowsocksKey(password, _keyLength);
		_key = getKey();
	}

	protected void setIV(byte[] iv, boolean isEncrypt) {
		if (_ivLength == 0) {
			return;
		}
		if (isEncrypt) {
			_encryptIV = new byte[_ivLength];
			System.arraycopy(iv, 0, _encryptIV, 0, _ivLength);
			try {
				encCipher = getCipher(isEncrypt);
				ParametersWithIV parameterIV = new ParametersWithIV(new KeyParameter(_key.getEncoded()), _encryptIV);
				encCipher.init(isEncrypt, parameterIV);
			} catch (InvalidAlgorithmParameterException e) {
				logger.warning(e.toString());
			}
		} else {
			_decryptIV = new byte[_ivLength];
			System.arraycopy(iv, 0, _decryptIV, 0, _ivLength);
			try {
				decCipher = getCipher(isEncrypt);
				ParametersWithIV parameterIV = new ParametersWithIV(new KeyParameter(_key.getEncoded()), _decryptIV);
				decCipher.init(isEncrypt, parameterIV);
			} catch (InvalidAlgorithmParameterException e) {
				logger.warning(e.toString());
			}
		}
	}

	@Override
	public void encrypt(byte[] data, ByteArrayOutputStream stream) {
		synchronized (encLock) {
			stream.reset();
			if (!_encryptIVSet) {
				_encryptIVSet = true;
				byte[] iv = Util.randomBytes(_ivLength);
				setIV(iv, true);
				try {
					stream.write(iv);
				} catch (IOException e) {
					logger.warning(e.toString());
				}
			}
			_encrypt(data, stream);
		}
	}

	@Override
	public void encrypt(byte[] data, int length, ByteArrayOutputStream stream) {
		byte[] d = new byte[length];
		System.arraycopy(data, 0, d, 0, length);
		encrypt(d, stream);
	}

	@Override
	public void decrypt(byte[] data, ByteArrayOutputStream stream) {
		byte[] temp;
		synchronized (decLock) {
			stream.reset();
			if (!_decryptIVSet) {
				_decryptIVSet = true;
				setIV(data, false);
				temp = new byte[data.length - _ivLength];
				System.arraycopy(data, _ivLength, temp, 0, data.length - _ivLength);
			} else
				temp = data;
		}
		_decrypt(temp, stream);
	}

	@Override
	public void decrypt(byte[] data, int length, ByteArrayOutputStream stream) {
		byte[] d = new byte[length];
		System.arraycopy(data, 0, d, 0, length);
		decrypt(d, stream);
	}

}
