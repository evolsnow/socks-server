package com.evol.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.evol.misc.Config;
import com.evol.misc.Util;
import com.evol.network.io.PipeSocket;

/**
 * blocking local for shadowsocks
 */
public class LocalServer implements Runnable {
	private Config _config;
	private ServerSocket _serverSocket;
	private Executor _executor;
	private Logger logger = Logger.getLogger(LocalServer.class.getName());

	public LocalServer(Config config) throws IOException, InvalidAlgorithmParameterException {
		_config = config;
		_serverSocket = new ServerSocket(_config.get_local_port(), 128);
		_executor = Executors.newCachedThreadPool();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Socket localSocket = _serverSocket.accept();
				PipeSocket pipe = new PipeSocket(_executor, localSocket, _config);
				_executor.execute(pipe);
			} catch (IOException e) {
				logger.warning(Util.getErrorMessage(e));
			}
		}
	}

	public void close() {
		try {
			_serverSocket.close();
		} catch (IOException e) {
			logger.warning(Util.getErrorMessage(e));
		}
	}

}
