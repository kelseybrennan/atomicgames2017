package com.atomicobject.rts;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class Unit {

	Long resource;
	String attackType;
	Long health;
	Long range;
	Long attackDamage;
	String type;
	Long speed;
	Long attackCooldown;
	Boolean canAttack;
	Long playerId;
	long x;
	long y;
	Long id;
	String status;
	Long attackCooldownDuration;
	ArrayList<int[]> pathToHome;
	int[] nextDest;

	public Unit(JSONObject json) {
		resource = (Long) json.get("resource");
		attackType = (String) json.get("attack_type");
		health = (Long) json.get("health");
		range = (Long) json.get("range");
		attackDamage = (Long) json.get("attack_damage");
		type = (String) json.get("type");
		speed = (Long) json.get("speed");
		attackCooldown = (Long) json.get("attack_cooldown");
		canAttack = (Boolean) json.get("can_attack");
		playerId = (Long) json.get("player_id");
		x = (Long) json.get("x");
		y = (Long) json.get("y");
		id = (Long) json.get("id");
		status = (String) json.get("status");
		attackCooldownDuration = (Long) json.get("attack_cooldown_duration");
	}

	public void setPathToHome(ArrayList<int[]> pathToHome) {
		this.pathToHome = pathToHome;
		if (pathToHome != null) {
			for (int[] move : pathToHome) {
				System.out.println(move[0] + ", " + move[1]);
			}
		}
	}

	public int[] getNextMoveToHome() {
		if (pathToHome.size() > 0) {
			return pathToHome.remove(pathToHome.size()-1);
		} else { 
			int[] tmp = {-1,-1};
			return tmp;
		}
	}
	
	public void setNextDest(int[] next) {
		this.nextDest = next;
	}
}
