package com.evol.network.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.evol.misc.Config;
import com.evol.misc.Util;

public abstract class SocketHandlerBase implements Runnable, ISocketHandler {
	private Logger logger = Logger.getLogger(SocketHandlerBase.class.getName());
	private static final int BUFFER_SIZE = 16384;
	protected Selector _selector;
	protected Config _config;
	protected List<ChangeRequest> _pendingRequest = new LinkedList<ChangeRequest>();
	protected Map<Object, Object> _pendingData = new HashMap<>();
	protected ConcurrentMap<SocketChannel, PipeWorker> _pipes = new ConcurrentHashMap<>();
	protected ByteBuffer _readBuffer = ByteBuffer.allocate(BUFFER_SIZE);

	protected abstract Selector initSelector() throws IOException;

	protected abstract boolean processPendingRequest(ChangeRequest request);

	protected abstract void processSelect(SelectionKey key);

	public SocketHandlerBase(Config config) throws IOException, InvalidAlgorithmParameterException {
		_config = config;
		_selector = initSelector();
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized (_pendingRequest) {
					Iterator<ChangeRequest> changes = _pendingRequest.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = changes.next();
						if (!processPendingRequest(change))
							break;
						changes.remove();
					}
				}
				_selector.select();
				Iterator<?> selectedKeys = _selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();
					if (!key.isValid()) {
						continue;
					}
					processSelect(key);
				}
			} catch (Exception e) {
				logger.warning(Util.getErrorMessage(e));

			}
		}
	}

	protected void createWriteBuffer(SocketChannel socketChannel) {
		synchronized (_pendingData) {
			if (!_pendingData.containsKey(socketChannel)) {
				List<?> queue = new ArrayList<Object>();
				_pendingData.put(socketChannel, queue);
			}
		}
	}

	protected void cleanUp(SocketChannel socketChannel) {
		try {
			socketChannel.close();
		} catch (IOException e) {
			logger.info(Util.getErrorMessage(e));
		}
		SelectionKey key = socketChannel.keyFor(_selector);
		if (key != null) {
			key.cancel();
		}
		if (_pendingData.containsKey(socketChannel)) {
			_pendingData.remove(socketChannel);
		}
	}

	@Override
	public void send(ChangeRequest request, byte[] data) {
		synchronized (_pendingRequest) {
			_pendingRequest.add(request);
			switch (request.type) {
			case ChangeRequest.CHANGE_SOCKET_OP:
				synchronized (_pendingData) {
					@SuppressWarnings("unchecked")
					List<ByteBuffer> queue = (List<ByteBuffer>) _pendingData.get(request.socket);
					if (queue != null) {
						queue.add(ByteBuffer.wrap(data));
					} else {
						logger.warning(Util.getErrorMessage(new Throwable("Socket is closed!")));
					}
				}
				break;
			}
		}
		_selector.wakeup();
	}

	@Override
	public void send(ChangeRequest request) {
		send(request, null);
	}

	public void close() {
		try {
			_selector.close();
		} catch (IOException e) {
			logger.warning(Util.getErrorMessage(e));
		}
	}
}
