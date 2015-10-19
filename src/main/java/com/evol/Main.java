package com.evol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import com.evol.misc.Config;
import com.evol.network.NioLocalServer;

public class Main {
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(Main.class.getName());
		Config config = parseArguments(args);

		// begin
		try {
			// LocalServer server = new LocalServer(config);
			NioLocalServer server = new NioLocalServer(config);
			Thread t = new Thread(server);
			t.start();
			logger.info("server: " + config.get_server());
			logger.info("local port: " + config.get_local_port());
			t.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// test();

	}

	public static void test() {
		byte b = -127;
		System.out.println((b & 0xff) << 8 | 9);
	}

	private static Config parseArguments(String[] args) {
		Config config = new Config();
		if (args.length == 2 && args[0].equals("-c")) {
			Path path = Paths.get(args[1]);
			try {
				String json = new String(Files.readAllBytes(path));
				config.loadFromJson(json);
				return config;
			} catch (IOException e) {
				System.out.println("Unable to read configuration file: " + args[1]);
				return null;
			}

		} else {
			return null;
		}
	}
}
