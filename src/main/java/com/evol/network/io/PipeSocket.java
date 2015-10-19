package com.evol.network.io;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.evol.crypt.CryptFactory;
import com.evol.crypt.ICrypt;
import com.evol.misc.Config;
import com.evol.misc.Util;

public class PipeSocket implements Runnable {
	private Logger logger = Logger.getLogger(PipeSocket.class.getName());

	private Socket _local;
	private Socket _remote;
	private Executor _executor;
	private Config _config;
	private ICrypt _crypt;
	private SocksThread st;

	public PipeSocket(Executor executor, Socket socket, Config config) throws IOException {
		_executor = executor;
		_config = config;
		_local = socket;
		_local.setSoTimeout(_config.get_timeout());
		_crypt = CryptFactory.getCrypt(_config.get_method(), _config.get_password());
	}

	@Override
	public void run() {

		try {
			_remote = new Socket(_config.get_server(), _config.get_server_port());
			_remote.setSoTimeout(_config.get_timeout());
			st = new SocksThread(_local, _remote, _crypt);

		} catch (IOException e) {
			st.close();
			logger.warning(Util.getErrorMessage(e));
			return;
		}

		_executor.execute(st.getLocalWorker());
		_executor.execute(st.getRemoteWorker());
	}

}