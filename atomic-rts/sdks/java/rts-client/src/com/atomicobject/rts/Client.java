package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {;

private final long BASE_X = 29L;
private final long BASE_Y = 29L;

private long other_base_x = 0L;
private long other_base_y = 0L;
private boolean other_base_found = false;

BufferedReader input;
OutputStreamWriter out;
LinkedBlockingQueue<Map<String, Object>> updates;
Map<Long, Unit> units;

GameTile[][] map = new GameTile[59][59];
ArrayList<int[]> tilesWithResources = new ArrayList<int[]>();


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
		// Only process this update if it's visible
		boolean visible = (Boolean) tile.get("visible");
		if (visible) {
			boolean blocked = (Boolean) tile.get("blocked");
			long x = BASE_X + (Long) tile.get("x");
			long y = BASE_Y + (Long) tile.get("y");

			// Instantiate tile and set visible and blocked
			if (map[(int) x][(int) y] == null) {
				map[(int) x][(int) y] = new GameTile();
			}
			map[(int) x][(int) y].setVisible(true);
			map[(int) x][(int) y].setBlocked(blocked);

			// Get resource for tile if there is one
			JSONObject resources = (JSONObject) tile.get("resources");
			if (resources != null) {
				String type = (String) resources.get("type");
				long total = (Long) resources.get("total");
				long value = (Long) resources.get("value");
				TileResource tileResource = new TileResource(type, total, value);
				map[(int)x][(int)y].setResource(tileResource);
				
				int[] tmp = {(int)x, (int)y};
				tilesWithResources.add(tmp);
			} else {
				int[] tmp = {(int)x, (int)y};
				if (tilesWithResources.contains(tmp)) {
					tilesWithResources.remove(tmp);
				}
				map[(int)x][(int)y].setResource(null);
			}

			// Get units for tile if they exist
			@SuppressWarnings("unchecked")
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) tile.get("units");
			if (unitUpdates != null) {
				for (JSONObject unit : unitUpdates) {
					// Get type
					String type = (String) unit.get("type");
					long player = (Long) unit.get("player_id");
					if (type.equals("base") && player == 1) {
						other_base_found = true;
						other_base_x = x;
						other_base_y = y;
					}
				}		
			}
		}

	}
	
	for (int[] tile : tilesWithResources) {
		System.out.println("Tile with resource at " + tile[0] + ", " + tile[1]);
	}
}

private ArrayList<int[]> aStar(Unit unit, int goalX, int goalY) {
	int width = map.length;
	int height = map[0].length;

	Map<GameTile, AStarNode> nodes = new HashMap<GameTile, AStarNode>();
	Comparator fComparator = new Comparator<AStarNode>() {
		public int compare(AStarNode a, AStarNode b) {
			return Integer.compare(a.getFValue(), b.getFValue());
		}
	};

	for(int i = 0; i < width; i++) {
		for(int j = 0; j < height; j++) {
			nodes.put(map[i][j], new AStarNode(i, j));
			if(map[i][j] != null && map[i][j].blocked) {
				nodes.get(map[i][j]).isWall = true;
			}
		}
	}

	List<AStarNode> openList = new ArrayList<AStarNode>();
	List<AStarNode> closedList = new ArrayList<AStarNode>();

	AStarNode destNode = nodes.get(map[goalX][goalY]);
	AStarNode current = nodes.get(map[(int) (unit.x + BASE_X)][(int) (unit.y + BASE_Y)]);
	current.parent = null;
	current.setGValue(0);
	openList.add(current);

	while(!openList.isEmpty()) {
		Collections.sort(openList, fComparator);
		current = openList.get(0);

		if(current.x == destNode.x && current.y == destNode.y) {
			//Calculate Path
			ArrayList<int[]> path = new ArrayList<int[]>();
			AStarNode node = destNode;
			while(node.parent != null) {
				int[] xy = {node.x, node.y};
				path.add(xy);
				node = node.parent;
			}
			return path;
		}

		//Remove from open, add to closed
		openList.remove(current);
		closedList.add(current);

		if(current.x + 1 < width) {  // Check east node
			AStarNode adjNode = nodes.get(map[current.x + 1][current.y]);

			if(!adjNode.isWall && !closedList.contains(adjNode)) {
				if(!openList.contains(adjNode)) {
					adjNode.parent = current;
					adjNode.calculateGValue(current);
					adjNode.calculateHValue(destNode);
					openList.add(adjNode);
				} else {
					if(adjNode.gValue < current.gValue) {
						adjNode.calculateGValue(current);
						current = adjNode;
					}
				}
			}
		}
		if(current.x - 1 > 0) {  //Check west node
			AStarNode adjNode = nodes.get(map[current.x - 1][current.y]);

			if(!adjNode.isWall && !closedList.contains(adjNode)) {
				if(!openList.contains(adjNode)) {
					adjNode.parent = current;
					adjNode.calculateGValue(current);
					adjNode.calculateHValue(destNode);
					openList.add(adjNode);
				} else {
					if(adjNode.gValue < current.gValue) {
						adjNode.calculateGValue(current);
						current = adjNode;
					}
				}
			}
		}
		if(current.y + 1 < height) {  //Check south node
			AStarNode adjNode = nodes.get(map[current.x][current.y + 1]);

			if(!adjNode.isWall && !closedList.contains(adjNode)) {
				if(!openList.contains(adjNode)) {
					adjNode.parent = current;
					adjNode.calculateGValue(current);
					adjNode.calculateHValue(destNode);
					openList.add(adjNode);
				} else {
					if(adjNode.gValue < current.gValue) {
						adjNode.calculateGValue(current);
						current = adjNode;
					}
				}
			}
		}
		if(current.y - 1 > 0) {  //Check north node
			AStarNode adjNode = nodes.get(map[current.x][current.y - 1]);

			if(!adjNode.isWall && !closedList.contains(adjNode)) {
				if(!openList.contains(adjNode)) {
					adjNode.parent = current;
					adjNode.calculateGValue(current);
					adjNode.calculateHValue(destNode);
					openList.add(adjNode);
				} else {
					if(adjNode.gValue < current.gValue) {
						adjNode.calculateGValue(current);
						current = adjNode;
					}
				}
			}
		}
	}
	return null;
}


private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
	unitUpdates.forEach((unitUpdate) -> {
		Long id = (Long) unitUpdate.get("id");
		String type = (String) unitUpdate.get("type");
		if (!type.equals("base")) {
			if (units.get(id) == null) {
				units.put(id, new Unit(unitUpdate));
			} else {
				Unit existing = units.get(id);
				existing.resource = (Long) unitUpdate.get("resource");
				existing.attackType = (String) unitUpdate.get("attack_type");
				existing.health = (Long) unitUpdate.get("health");
				existing.range = (Long) unitUpdate.get("range");
				existing.attackDamage = (Long) unitUpdate.get("attack_damage");
				existing.type = (String) unitUpdate.get("type");
				existing.speed = (Long) unitUpdate.get("speed");
				existing.attackCooldown = (Long) unitUpdate.get("attack_cooldown");
				existing.canAttack = (Boolean) unitUpdate.get("can_attack");
				existing.playerId = (Long) unitUpdate.get("player_id");
				existing.x = (Long) unitUpdate.get("x");
				existing.y = (Long) unitUpdate.get("y");
				existing.id = (Long) unitUpdate.get("id");
				existing.status = (String) unitUpdate.get("status");
				existing.attackCooldownDuration = (Long) unitUpdate.get("attack_cooldown_duration");
			}
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
	JSONArray commands = new JSONArray();

	// If we have idle units, give them work to do
	for (Unit unit : units.values()) {
		if (unit.type.equals("worker") && unit.status.equals("idle")) {
			commands.add(assignWorkToWorker(unit));
		}
	}		

	String[] directions = {"N","E","S","W"};
	String direction = directions[(int) Math.floor(Math.random() * 4)];
	Long[] unitIds = units.keySet().toArray(new Long[units.size()]);
	Long unitId = unitIds[(int) Math.floor(Math.random() * unitIds.length)];

	return commands;
}

private JSONObject assignWorkToWorker(Unit unit) {
	JSONObject cmd = new JSONObject();
	String dir = "";
	String command = "";

	// If we have gathered something, send home
	if (unit.resource > 0) {
		command = "MOVE";

		// Only calculate the path home once, then keep popping off next move
		if (unit.path == null) {
			ArrayList<int[]> path = this.aStar(unit, (int) BASE_X, (int) BASE_Y);
			unit.setPath(path);
		}
		int[] nextMove = unit.getNextMoveToHome();
		if (nextMove[0] < 0 || nextMove[1] < 0) {
			dir = this.findNextInvisibleCell(unit);
			unit.setPath(null);
		} else {
			dir = this.getDirectionToMove(nextMove, unit.x+BASE_X, unit.y+BASE_Y);
		}
	} else {
		// See if there's a resource to gather right next to us, and set is as our next destination
		String resourceDir = checkForResources(BASE_X + unit.x, BASE_Y + unit.y, unit);

		// Gather the resource if there's one next to us, otherwise just explore
		if (resourceDir != null) {
			command = "GATHER";
			dir = resourceDir;
			System.out.println("ID = " + unit.id + " Gathering from " + dir);
		} else {
			command = "MOVE";
			/*// If we know where a resource is, go to it
			if (tilesWithResources.size() > 0) {
				int[] tile = tilesWithResources.get(0);
				System.out.println("Tile 0: " + tile[0] + " Tile 1: " + tile[1]);
				ArrayList<int[]> path = this.aStar(unit, tile[0], tile[1]);
				if (path == null) {
					System.out.println("Path is null");
				} else {
				for (int[] move : path ) {
					System.out.println(move[0] + ", " + move[1]);
				}
				}
				
				unit.setPath(path);
				int[] resource = unit.getNextMoveToHome();
				System.out.println("Next coord: " + resource[0] + ", " + resource[1]);
				//dir = this.getDirectionToMove(resource, unit.x+BASE_X, unit.y+BASE_Y);
				dir = findNextInvisibleCell(unit);
				System.out.println(dir);
			} else {*/
				//System.out.println("Exploring");
				dir = findNextInvisibleCell(unit);
			//}
		}
	}

	cmd.put("command", command);
	cmd.put("dir", dir);
	cmd.put("unit", unit.id);
	return cmd;
}

private String getDirectionToMove(int[] coords, long x, long y) {
	// Convert coordinates to direction
	if (coords[0] < x && coords[1] == y) {
		return "W";
	} else if (coords[0] > x && coords[1] == y) {
		return "E";
	} else if (coords[0] == x && coords[1] < y) {
		return "N";
	} else if (coords[0] == x && coords[1] > y) {
		return "S";
	}
	return null;
}


private String findNextInvisibleCell(Unit unit) {
	String[] directions = {"N","E","S","W"};
	String direction = directions[(int) Math.floor(Math.random() * 4)];
	return direction;
}

private String checkForResources(long p_x, long p_y, Unit unit) {
	int x = (int) p_x;
	int y = (int) p_y;

	if (map[x][y+1].getResource() != null) {
		//unit.setNextDest(x, y+1);
		return "S";
	} else if (map[x][y-1].getResource() != null) {
		//unit.setNextDest(x, y-1);
		return "N";
	} else if (map[x+1][y].getResource() != null) {
		//unit.setNextDest(x+1, y);
		return "E";
	} else if (map[x-1][y].getResource() != null) {
		//unit.setNextDest(x-1, y);
		return "W";
	} else {
		return null;
	}
}


@SuppressWarnings("unchecked")
private void sendCommandListToServer(JSONArray commands) throws IOException {
	JSONObject container = new JSONObject();
	container.put("commands", commands);
	//System.out.println("Sending commands: " + container.toJSONString());
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
