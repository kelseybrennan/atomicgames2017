package com.atomicobject.rts;

public class TileResource {
	String type;
	int total;
	int value;
	
	public TileResource(String type, int total, int value) {
		this.type = type;
		this.total = total;
		this.value = value;
	}


	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
