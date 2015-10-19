package com.evol.crypt;

import java.io.ByteArrayOutputStream;

public interface ICrypt {
	void encrypt(byte[] data, ByteArrayOutputStream stream);

	void encrypt(byte[] data, int length, ByteArrayOutputStream stream);

	void decrypt(byte[] data, ByteArrayOutputStream stream);

	void decrypt(byte[] data, int length, ByteArrayOutputStream stream);

	int getKeyLength();

	int getIVLength();
}
