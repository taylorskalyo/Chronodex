package com.taylorskalyo.chronodex;

public class ChronoEvent implements Comparable<ChronoEvent>{
	private Long id;
	private String name;
	private int color;
	private int startTime; // in minutes
	private int endTime; // in minutes
	private int duration; // in minutes
	
	public ChronoEvent (Long id, String name, int color, int startTime, int endTime) {
		this.id = id;
		this.name = name;
		this.color = color;
		this.startTime = startTime;
		this.endTime = endTime;
		this.duration = endTime-startTime;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getColor() {
		return color;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public int getDuration() {
		return duration;
	}
	
	private String minToTime(int minutes) {
		int hour = minutes/60;
		int min = minutes%60;
		return String.format("%02d:%02d", hour, min);
	}

	@Override
	public int compareTo(ChronoEvent event) {
		return startTime - event.startTime;
	}
	
	@Override
	public String toString() {
		return minToTime(startTime)+"-"+minToTime(endTime)+": "+name;
	}
}
