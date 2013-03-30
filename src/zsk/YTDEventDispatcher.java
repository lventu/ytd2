/**
 * 
 */
package zsk;

import java.util.Observable;

/**
 * @author luca
 *
 */
public class YTDEventDispatcher extends Observable {
	
	public void threadNumberChanged(int currentNThread) {
		setChanged();
		notifyObservers(currentNThread);
	}
}
