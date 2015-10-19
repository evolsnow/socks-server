package com.evol.misc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/*
 * Data class for configuration to bring up server
 */
public class Config {
	private String _server;
	private int _server_port;
	private String _local_addr;
	private int _local_port;
	private String _method;
	private String _password;
	private int _timeout;
	private String _logLevel;

	public Config() {
		/*
		 * _server = "127.0.0.1"; _server_port = 443; _local_addr = "127.0.0.0";
		 * _local_port = 1080; _method = "aes-128-cfb"; _password = "barfoo!";
		 * _logLevel = "INFO";
		 */
	}

	public String get_server() {
		return _server;
	}

	public void set_server(String _server) {
		this._server = _server;
	}

	public int get_server_port() {
		return _server_port;
	}

	public void set_server_port(int _server_port) {
		this._server_port = _server_port;
	}

	public String get_local_addr() {
		return _local_addr;
	}

	public void set_local_addr(String _local_addr) {
		this._local_addr = _local_addr;
	}

	public int get_local_port() {
		return _local_port;
	}

	public void set_local_port(int _local_port) {
		this._local_port = _local_port;
	}

	public String get_method() {
		return _method;
	}

	public void set_method(String _method) {
		this._method = _method;
	}

	public String get_password() {
		return _password;
	}

	public void set_password(String _password) {
		this._password = _password;
	}

	public void setLogLevel(String value) {
		_logLevel = value;
		Log.init(getLogLevel());
	}

	public String getLogLevel() {
		return _logLevel;
	}

	public int get_timeout() {
		return _timeout;
	}

	public void set_timeout(int _timeout) {
		this._timeout = _timeout;
	}

	public void loadFromJson(String jsonStr) {
		Gson gson = new Gson();
		JsonObject jObj = gson.fromJson(jsonStr, JsonObject.class);
		_server = jObj.getAsJsonPrimitive("server").getAsString();
		_server_port = jObj.getAsJsonPrimitive("server_port").getAsInt();
		try {
			_local_addr = jObj.getAsJsonPrimitive("local_addr").getAsString();
		} catch (Exception e) {
			_local_addr = "127.0.0.1";
		}
		_local_port = jObj.getAsJsonPrimitive("local_port").getAsInt();
		_method = jObj.getAsJsonPrimitive("method").getAsString();
		_password = jObj.getAsJsonPrimitive("password").getAsString();
		_timeout = jObj.getAsJsonPrimitive("timeout").getAsInt() * 1000;
		_logLevel = "INFO";
		setLogLevel(_logLevel);
	}

	/*
	 * public String saveToJson() { JsonObject jObj = new JsonObject();
	 * jObj.addProperty("server", _server); jObj.addProperty("server_port",
	 * _server_port); jObj.addProperty("local_addr", _local_addr);
	 * jObj.addProperty("local_port", _local_port); jObj.addProperty("method",
	 * _method); jObj.addProperty("password", _password);
	 * jObj.addProperty("log_level", "INFO"); jObj.addProperty("isSocks5Server",
	 * _isSocks5Server);
	 * 
	 * return jObj.toString(); }
	 */
}
