package com.vish.fando.stock.mediator;

import java.util.ArrayList;
import java.util.List;

public abstract class Initiater {

	private List<Observer> observers = new ArrayList<Observer>();

	public void attach(Observer observer) {
		observers.add(observer);
	}

	protected void notifyObservers() {
		observers.forEach(o -> o.initialise());
	}
}
