package com.evol.network.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import com.evol.crypt.ICrypt;
import com.evol.misc.SocksStatus;
import com.evol.misc.Util;

public class SocksThread {
	private Logger logger = Logger.getLogger(SocksThread.class.getName());

	private final int BUFFER_SIZE = 16384;
	private final int SOCK5_BUFFER_SIZE = 3;
	private Socket _local;
	private Socket _remote;
	private ICrypt _crypt;
	private SocksStatus _socks5;
	private EncryptSend es;
	private boolean isClosed = false;

	// contract local worker to send data to remote server

	public SocksThread(Socket local, Socket remote, ICrypt crypt) {
		_local = local;
		_remote = remote;
		_crypt = crypt;
		_socks5 = new SocksStatus();
		es = new EncryptSend(_local, _remote, _crypt);
	}

	public Runnable getLocalWorker() {
		return new Runnable() {
			private boolean isFirstPacket = true;

			@Override
			public void run() {
				InputStream reader;
				byte[] sock5buffer = new byte[SOCK5_BUFFER_SIZE];
				byte[] databuffer = new byte[BUFFER_SIZE];
				byte[] buffer;
				int readCount;

				// prepare to read local stream
				try {
					reader = _local.getInputStream();
				} catch (IOException e) {
					logger.info(e.toString());
					return;
				}

				/*
				 * socks5 handshake chrome: 05 01 00 --->> local chrome <<---05
				 * 00 local chrome: 05 01 00 --->> local chrome <<---
				 * 5,0,0,1,0,0,0,0,0,0 local
				 */
				while (true) {
					try {
						if (!_socks5.isReady())
							buffer = sock5buffer;
						else
							buffer = databuffer;

						readCount = reader.read(buffer);
						if (readCount < 1) {
							throw new IOException("chrome socket closed");
						}

						if (!_socks5.isReady()) {
							buffer = _socks5.getResponse(buffer);
							if (!es.sendLocal(buffer, buffer.length, false)) {
								throw new IOException("reply to chrome failed");
							}
							continue;
						}

						if (isFirstPacket) {
							isFirstPacket = false;
							logger.info("connected to: " + Util.getRequestedHostInfo(buffer));
						}

						// after handshake established, try to send data to
						// remote server
						if (!es.sendRemote(buffer, readCount, true)) {
							throw new IOException("remote server closed");
						}
					} catch (IOException e) {
						logger.fine(Util.getErrorMessage(e));
						break;
					}
				}
				//close();
				logger.fine("localworker exit");
			}
		};
	}

	// Contract remote worker to fetch data from server and send it to local
	// client, such as chrome .etc
	public Runnable getRemoteWorker() {
		return new Runnable() {

			@Override
			public void run() {
				InputStream reader;
				int readCount;
				byte[] buffer = new byte[BUFFER_SIZE];

				// get data from server
				try {
					reader = _remote.getInputStream();
				} catch (IOException e) {
					logger.info(e.toString());
					return;
				}

				// transfer data from server to local
				while (true) {
					try {
						readCount = reader.read(buffer);
						if (readCount < 1) {
							throw new IOException("remote socket closed");
						}

						// try to send data to local socket
						if (!es.sendLocal(buffer, readCount, true)) {
							throw new IOException("local socket closed");
						}

					} catch (SocketTimeoutException e) {
						continue;
					} catch (IOException e) {
						logger.fine(Util.getErrorMessage(e));
						break;
					}
				}
				close();
				logger.fine("remote worker exit");
			}
		};
	}

	public void close() {
		if (isClosed)
			return;
		isClosed = true;

		try {
			_local.shutdownInput();
			_local.shutdownOutput();
			_local.close();
		} catch (IOException e) {
			logger.info("local socket failed to close");
		}

		try {
			if (_remote != null) {
				_remote.shutdownInput();
				_remote.shutdownOutput();
				_remote.close();
			}
		} catch (IOException e) {
			logger.info("remote socket failed to close");
		}

	}

}
