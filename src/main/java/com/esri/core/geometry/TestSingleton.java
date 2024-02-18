package com.esri.core.geometry;

import java.util.ArrayList;
import java.util.List;

public class TestSingleton {
	private static TestSingleton instance = null;
	public List<Integer> clipPolygon2_diyTestArray = new ArrayList<Integer>();
	public void init() {
		for (int i = 0; i < 60; i++) {
			clipPolygon2_diyTestArray.add(0);
		}
	}
	public void print() {
		for (int i = 0; i < clipPolygon2_diyTestArray.size(); i++) {
			System.out.println("Branch " + i + "\t: " + clipPolygon2_diyTestArray.get(i));
		}
	}
	public static TestSingleton getInstance() {
		if (instance == null) {
			instance = new TestSingleton();
			// instance.init();
		}
		return instance;
	}
}
