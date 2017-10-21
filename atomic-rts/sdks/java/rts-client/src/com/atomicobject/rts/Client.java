package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {;
	
	private final long BASE_X = 29L;
	private final long BASE_Y = 29L;
	
	BufferedReader input;
	OutputStreamWriter out;
	LinkedBlockingQueue<Map<String, Object>> updates;
	Map<Long, Unit> units;
	
	GameTile[][] map = new GameTile[59][59];
	

	public Client(Socket socket) {
		updates = new LinkedBlockingQueue<Map<String, Object>>();
		units = new HashMap<Long, Unit>();
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void start() {
		System.out.println("Starting client threads ...");
		new Thread(() -> readUpdatesFromServer()).start();
		new Thread(() -> runClientLoop()).start();
	}
	
	public void readUpdatesFromServer() {
		String nextLine;
		try {
			while ((nextLine = input.readLine()) != null) {
				@SuppressWarnings("unchecked")
				// Get update and add to list of all of them
				Map<String, Object> update = (Map<String, Object>) JSONValue.parse(nextLine.trim());
				updates.add(update);
				
			}
		} catch (IOException e) {
			// exit thread
		}		
	}

	public void runClientLoop() {
		System.out.println("Starting client update/command processing ...");
		try {
			while (true) {
				processUpdateFromServer();
				respondWithCommands();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeStreams();
	}

	private void processUpdateFromServer() throws InterruptedException {
		Map<String, Object> update = updates.take();
		if (update != null) {
			//System.out.println("Processing udpate: " + update);
			@SuppressWarnings("unchecked")
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) update.get("unit_updates");
			addUnitUpdate(unitUpdates);
			@SuppressWarnings("unchecked")
			Collection<JSONObject> tileUpdates = (Collection<JSONObject>) update.get("tile_updates");
			processTileUpdates(tileUpdates);
		}
	}
	
	private void processTileUpdates(Collection<JSONObject> tileUpdates) {
		for (JSONObject tile : tileUpdates) {
			boolean visible = (Boolean) tile.get("visible");
			boolean blocked = (Boolean) tile.get("blocked");
			long x = BASE_X + (Long) tile.get("x");
			long y = BASE_Y + (Long) tile.get("y");
			System.out.println(x + ", " + y);

			map[(int) x][(int) y] = new GameTile();
			map[(int) x][(int) y].setVisible(visible);
			map[(int) x][(int) y].setBlocked(blocked);
			JSONObject resources = (JSONObject) tile.get("resources");
			if (resources != null) {
				String type = (String) resources.get("type");
				long total = (Long) resources.get("total");
				long value = (Long) resources.get("value");
				TileResource tileResource = new TileResource(type, total, value);
				map[(int)x][(int)y].setResource(tileResource);
			}
			
		}
		
	}
	

	private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
		unitUpdates.forEach((unitUpdate) -> {
			Long id = (Long) unitUpdate.get("id");
			String type = (String) unitUpdate.get("type");
			if (!type.equals("base")) {
				units.put(id, new Unit(unitUpdate));
			}
		});
	}

	private void respondWithCommands() throws IOException {
		if (units.size() == 0) return;
		
		JSONArray commands = buildCommandList();		
		sendCommandListToServer(commands);
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCommandList() {
		String[] directions = {"N","E","S","W"};
		String direction = directions[(int) Math.floor(Math.random() * 4)];

		Long[] unitIds = units.keySet().toArray(new Long[units.size()]);
		Long unitId = unitIds[(int) Math.floor(Math.random() * unitIds.length)];

		JSONArray commands = new JSONArray();
		JSONObject command = new JSONObject();	
		command.put("command", "MOVE");
		command.put("dir", direction);
		command.put("unit", unitId);
		commands.add(command);
		return commands;
	}

	@SuppressWarnings("unchecked")
	private void sendCommandListToServer(JSONArray commands) throws IOException {
		JSONObject container = new JSONObject();
		container.put("commands", commands);
		System.out.println("Sending commands: " + container.toJSONString());
		out.write(container.toJSONString());
		out.write("\n");
		out.flush();
	}

	private void closeStreams() {
		closeQuietly(input);
		closeQuietly(out);
	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
