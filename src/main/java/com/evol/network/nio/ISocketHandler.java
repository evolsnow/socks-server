package com.evol.network.nio;

public interface ISocketHandler {
	void send(ChangeRequest request, byte[] data);

	void send(ChangeRequest request);
}
