package it.duccius.maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;


public class NavigationDataSet { 

private ArrayList<Placemark> placemarks = new ArrayList<Placemark>();
private Placemark currentPlacemark;
private Placemark routePlacemark;
private String coordinates;

public void sort()
{
	Collections.sort(placemarks);	
}

public String getCoordFromTitle(String title)
{
	for (Placemark placemark: placemarks)
	{
		if (placemark.getTitle().equals(title))
		{
			return placemark.getCoordinates();			
		}
	}
	return "";
}
public String toString() {
    String s= "";
    for (Iterator<Placemark> iter=placemarks.iterator();iter.hasNext();) {
        Placemark p = (Placemark)iter.next();
        s += p.getTitle() + "\n" + p.getDescription() + "\n\n";
    }
    return s;
}

public void addCurrentPlacemark() {
    placemarks.add(currentPlacemark);
}

public ArrayList<Placemark> getPlacemarks() {
    return placemarks;
}

public void setPlacemarks(ArrayList<Placemark> placemarks) {
    this.placemarks = placemarks;
}

public Placemark getCurrentPlacemark() {
    return currentPlacemark;
}

public void setCurrentPlacemark(Placemark currentPlacemark) {
    this.currentPlacemark = currentPlacemark;
}

public Placemark getRoutePlacemark() {
    return routePlacemark;
}

public void setRoutePlacemark(Placemark routePlacemark) {
    this.routePlacemark = routePlacemark;
}
public String getCoordinates() {
    return coordinates;
}

public void setCoordinates(String coordinates) {
    this.coordinates = coordinates;
}
}
