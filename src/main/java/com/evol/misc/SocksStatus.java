package com.evol.misc;

/**
 * Provide local socks5 statue and required response
 */
public class SocksStatus {
	public enum STAGE {
		SOCK5_ACK, SOCKS_REPLY, SOCKS_READY
	};

	private STAGE _stage;

	public SocksStatus() {
		_stage = STAGE.SOCK5_ACK;
	}

	public boolean isReady() {
		return _stage == STAGE.SOCKS_READY;
	}

	public byte[] getResponse(byte[] data) {
		byte[] response = null;
		switch (_stage) {
		case SOCK5_ACK:
			if (data[0] != 0x5)
				response = new byte[] { 0, 91 };
			else
				response = new byte[] { 5, 0 };
			_stage = STAGE.SOCKS_REPLY;
			break;
		case SOCKS_REPLY:
			response = new byte[] { 5, 0, 0, 1, 0, 0, 0, 0, 0, 0 };
			_stage = STAGE.SOCKS_READY;
			break;
		default:
			break;
		}
		return response;
	}
}
