package com.evol.network.nio;

import java.io.ByteArrayOutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import com.evol.crypt.CryptFactory;
import com.evol.crypt.ICrypt;
import com.evol.misc.Config;
import com.evol.misc.SocksStatus;
import com.evol.misc.Util;

public class PipeWorker implements Runnable {
	private Logger logger = Logger.getLogger(PipeWorker.class.getName());
	private SocketChannel _localChannel;
	private SocketChannel _remoteChannel;
	private ISocketHandler _localSocketHandler;
	private ISocketHandler _remoteSocketHandler;
	private SocksStatus _socks5;
	private ICrypt _crypt;
	public String socketInfo;
	private ByteArrayOutputStream _outStream;
	private BlockingQueue<PipeEvent> _processQueue;
	private volatile boolean requestedClose;

	public PipeWorker(ISocketHandler localHandler, SocketChannel localChannel, ISocketHandler remoteHandler,
			SocketChannel remoteChannel, Config config) {
		_localChannel = localChannel;
		_remoteChannel = remoteChannel;
		_localSocketHandler = localHandler;
		_remoteSocketHandler = remoteHandler;
		_crypt = CryptFactory.getCrypt(config.get_method(), config.get_password());
		_socks5 = new SocksStatus();
		_outStream = new ByteArrayOutputStream(16384);
		_processQueue = new LinkedBlockingQueue<PipeEvent>();
		requestedClose = false;
		socketInfo = String.format("Local: %s, Remote: %s", localChannel, remoteChannel);
	}

	public void close() {
		requestedClose = true;
		processData(null, 0, false);
	}

	public boolean isSock5Initialized() {
		return _socks5.isReady();
	}

	public byte[] getSocks5Response(byte[] data) {
		return _socks5.getResponse(data);
	}

	public void processData(byte[] data, int count, boolean isEncrypted) {
		if (data != null) {
			byte[] dataCopy = new byte[count];
			System.arraycopy(data, 0, dataCopy, 0, count);
			_processQueue.add(new PipeEvent(dataCopy, isEncrypted));
		} else {
			_processQueue.add(new PipeEvent());
		}
	}

	@Override
	public void run() {
		PipeEvent event;
		ISocketHandler socketHandler;
		SocketChannel channel;

		while (true) {
			// make sure all the requests in the queue are processed
			if (_processQueue.isEmpty() && requestedClose) {
				logger.fine("PipeWorker closed: " + this.socketInfo);
				if (_localChannel.isOpen()) {
					_localSocketHandler.send(new ChangeRequest(_localChannel, ChangeRequest.CLOSE_CHANNEL));
				}
				if (_remoteChannel.isOpen()) {
					_remoteSocketHandler.send(new ChangeRequest(_remoteChannel, ChangeRequest.CLOSE_CHANNEL));
				}
				break;
			}
			try {
				event = _processQueue.take();

				// check if other thread is requested to clsoe socket
				if (event.data == null) {
					continue;
				}

				// clear stream for new data
				_outStream.reset();
				if (event.isEncrypted) {
					_crypt.encrypt(event.data, _outStream);
					channel = _remoteChannel;
					socketHandler = _remoteSocketHandler;
				} else {
					_crypt.decrypt(event.data, _outStream);
					channel = _localChannel;
					socketHandler = _localSocketHandler;
				}

				// data is ready to send to socket
				ChangeRequest request = new ChangeRequest(channel, ChangeRequest.CHANGE_SOCKET_OP,
						SelectionKey.OP_WRITE);
				socketHandler.send(request, _outStream.toByteArray());

			} catch (InterruptedException e) {
				logger.fine(Util.getErrorMessage(e));
				break;
			}
		}
	}
}
