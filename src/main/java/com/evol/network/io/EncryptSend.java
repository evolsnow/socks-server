package com.evol.network.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import com.evol.crypt.ICrypt;
import com.evol.misc.Util;

public class EncryptSend {

	private Socket _local;
	private Socket _remote;
	private ICrypt _crypt;
	private Logger logger = Logger.getLogger(EncryptSend.class.getName());

	public EncryptSend(Socket local, Socket remote, ICrypt crypt) {
		_local = local;
		_remote = remote;
		_crypt = crypt;
	}

	public boolean sendRemote(byte[] data, int length, boolean toEncrypt) {
		byte[] dataToSend;

		if (toEncrypt) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			_crypt.encrypt(data, length, stream);
			dataToSend = stream.toByteArray();
		} else {
			dataToSend = data;
		}
		try {
			if (length > 0) {
				OutputStream outStream = _remote.getOutputStream();
				outStream.write(dataToSend, 0, dataToSend.length);
			} else {
				logger.info("Nothing to sendRemote!\n");
			}
		} catch (IOException e) {
			logger.info(Util.getErrorMessage(e));
			return false;
		}

		return true;
	}

	public boolean sendLocal(byte[] data, int length, boolean toDecrypt) {
		byte[] dataToSend;

		if (toDecrypt) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			_crypt.decrypt(data, length, stream);
			dataToSend = stream.toByteArray();
		} else {
			dataToSend = data;
		}
		try {
			OutputStream outStream = _local.getOutputStream();
			outStream.write(dataToSend, 0, dataToSend.length);
		} catch (IOException e) {
			logger.info(Util.getErrorMessage(e));
			return false;
		}
		return true;
	}

}
