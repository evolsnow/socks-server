package com.evol.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.InvalidAlgorithmParameterException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.evol.misc.Config;
import com.evol.misc.Util;
import com.evol.network.nio.ChangeRequest;
import com.evol.network.nio.PipeWorker;
import com.evol.network.nio.RemoteSocketHandler;
import com.evol.network.nio.SocketHandlerBase;

public class NioLocalServer extends SocketHandlerBase {
	private Logger logger = Logger.getLogger(NioLocalServer.class.getName());
	private ServerSocketChannel _serverChannel;
	private RemoteSocketHandler _remoteSocketHandler;
	private Executor _executor;

	public NioLocalServer(Config config) throws IOException, InvalidAlgorithmParameterException {
		super(config);
		_executor = Executors.newCachedThreadPool();
		// initialize remote socket handler
		_remoteSocketHandler = new RemoteSocketHandler(_config);
		_executor.execute(_remoteSocketHandler);
	}

	@Override
	protected Selector initSelector() throws IOException {
		Selector socketSelector = SelectorProvider.provider().openSelector();
		_serverChannel = ServerSocketChannel.open();
		_serverChannel.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress(_config.get_local_addr(), _config.get_local_port());
		_serverChannel.socket().bind(isa);
		_serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		return socketSelector;
	}

	@Override
	protected boolean processPendingRequest(ChangeRequest request) {
		switch (request.type) {
		case ChangeRequest.CHANGE_SOCKET_OP:
			SelectionKey key = request.socket.keyFor(_selector);
			if ((key != null) && key.isValid()) {
				key.interestOps(request.op);
			} else {
				logger.warning("NioLocalServer::processPendingRequest (drop): " + key + request.socket);
			}
			break;
		case ChangeRequest.CLOSE_CHANNEL:
			cleanUp(request.socket);
			break;
		}
		return true;
	}

	@Override
	protected void processSelect(SelectionKey key) {
		// handle event
		try {
			if (key.isAcceptable()) {
				accept(key);
			} else if (key.isReadable()) {
				read(key);
			} else if (key.isWritable()) {
				write(key);
			}
		} catch (IOException e) {
			cleanUp((SocketChannel) key.channel());
		}
	}

	private void accept(SelectionKey key) throws IOException {
		// local socket established
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(_selector, SelectionKey.OP_READ);

		// prepare local socket write queue
		createWriteBuffer(socketChannel);

		// create pipe between local and remote socket
		PipeWorker pipe = _remoteSocketHandler.createPipe(this, socketChannel, _config.get_server(),
				_config.get_server_port());
		_pipes.putIfAbsent(socketChannel, pipe);
		_executor.execute(pipe);
	}

	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		int readCount;
		PipeWorker pipe = _pipes.get(socketChannel);
		byte[] data;
		if (pipe == null) {
			// should not happen
			cleanUp(socketChannel);
			return;
		}
		_readBuffer.clear();
		try {
			readCount = socketChannel.read(_readBuffer);
		} catch (IOException e) {
			cleanUp(socketChannel);
			return;
		}
		if (readCount == -1) {
			cleanUp(socketChannel);
			return;
		}
		/*
		 * There are two stage of establish Sock5: 1. ACK (3 bytes) 2. HELLO (3
		 * bytes + dst info) as Client sending HELLO, it might contain dst info.
		 * In this case, server needs to send back HELLO response to client and
		 * start the remote socket right away, otherwise, client will wait until
		 * timeout.
		 */
		data = _readBuffer.array();
		if (!pipe.isSock5Initialized()) {
			byte[] temp = pipe.getSocks5Response(data);
			send(new ChangeRequest(socketChannel, ChangeRequest.CHANGE_SOCKET_OP, SelectionKey.OP_WRITE), temp);
			if (readCount > 3) {
				readCount -= 3;
				temp = new byte[readCount];
				System.arraycopy(data, 3, temp, 0, readCount);
				data = temp;
				logger.info("Connected to: " + Util.getRequestedHostInfo(data));
			}
		}
		if (pipe.isSock5Initialized()) {
			pipe.processData(data, readCount, true);
		}
	}

	private void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		synchronized (_pendingData) {
			List<?> queue = (List<?>) _pendingData.get(socketChannel);
			if (queue == null) {
				logger.warning("LocalSocket::write queue = null: " + socketChannel);
				return;
			}
			// write data
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) {
					break;
				}
				queue.remove(0);
			}
			if (queue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	@Override
	protected void cleanUp(SocketChannel socketChannel) {
		super.cleanUp(socketChannel);
		PipeWorker pipe = _pipes.get(socketChannel);
		if (pipe != null) {
			pipe.close();
			_pipes.remove(socketChannel);
			logger.fine("LocalSocket closed: " + pipe.socketInfo);
		} else {
			logger.fine("LocalSocket closed (NULL): " + socketChannel);

		}
	}

	@Override
	public void close() {
		super.close();
		try {
			_serverChannel.close();
			_remoteSocketHandler.close();
			for (PipeWorker p : _pipes.values()) {
				p.close();
			}
		} catch (IOException e) {
			logger.warning(Util.getErrorMessage(e));
		}
	}
}