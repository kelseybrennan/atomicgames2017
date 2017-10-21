package com.atomicobject.rts;

public class TileResource {
	String type;
	long total;
	long value;
	
	public TileResource(String type, long total, long value) {
		this.type = type;
		this.total = total;
		this.value = value;
	}


	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
