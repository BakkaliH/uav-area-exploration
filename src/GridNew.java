import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONArray;

/**
 * Created by Anurag on 6/4/2016.
 */
public class GridNew extends Thread {
	double gridSide, cellWidth;
	int gridPartition;
	int noOfDrones, swirlWidth;
	double totalGridDistance, shortestDistance;
	Point[][] gridPoints;
	Object proximityLock = new Object();
	HashMap<DroneNew, Point> droneList = new HashMap<>();
	ArrayList<Integer> finishedDrones = new ArrayList<>();
	DroneNew currentDrone;
	int listSharing;
	int collisionCount;
	public double repetitionPercentage;

	public GridNew(double gridSide, int gridPartition, int noOfDrones, int swirlWidth) {
		this.gridSide = gridSide;
		this.gridPartition = gridPartition;
		this.noOfDrones = noOfDrones;
		this.swirlWidth = swirlWidth;
		this.cellWidth = gridSide / (gridPartition);

		gridPoints = new Point[gridPartition + 1][gridPartition + 1];

		calculateGridPoints();
		totalGridDistance = 2 * (gridPartition + 1) * gridSide;
		calculateShortestDistance();
	}

	public void calculateGridPoints() {

		int l = gridPoints[0].length;
		for (int x = 0; x < l; x++) {
			for (int y = 0; y < l; y++) {
				gridPoints[x][y] = new Point(x, y, 0);
			}
		}

	}

	public void calculateShortestDistance() {
		double val = Math.pow(gridPartition + 1, 2);
		// System.out.println(val);
		shortestDistance = (val - 1) * cellWidth;
	}

	public ArrayList<Point> getVertexList() {
		ArrayList<Point> pointList = new ArrayList<>();

		for (int i = 0; i <= gridPartition; i++) {
			for (int j = 0; j <= gridPartition; j++) {
				pointList.add(gridPoints[i][j]);
			}
		}

		return pointList;
	}

	public Point getRandomPoint() {
		Random rand = new Random();
		int x = Math.abs(rand.nextInt()) % (gridPartition + 1);
		int y = Math.abs(rand.nextInt()) % (gridPartition + 1);

		return getPoint(x, y);
	}

	public Point getPoint(int x, int y) {
		return gridPoints[x][y];
	}

	public void changePosition(DroneNew drone) {
		synchronized (proximityLock) {
			droneList.put(drone, drone.currentPoint);
			currentDrone = drone;
			proximityLock.notify();
		}
	}

	public void removePosition(DroneNew drone) {
		synchronized (proximityLock) {
			droneList.remove(drone);
			proximityLock.notify();
		}
	}

	public synchronized void markFinished(int droneId) {
		finishedDrones.add(droneId);
		System.out.println(droneId + " finished");

		if (finishedDrones.size() == noOfDrones) {
			synchronized (proximityLock) {
				proximityLock.notify();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			synchronized (proximityLock) {
				while (finishedDrones.size() != noOfDrones) {
					proximityLock.wait();

					for (Map.Entry<DroneNew, Point> entry : droneList.entrySet()) {
						if (entry.getKey() != currentDrone) {
							double pointDist = pointDistance(droneList.get(currentDrone), entry.getValue());
							if (pointDist > 0 && pointDist <= currentDrone.range + entry.getKey().range) {
								listSharing++;
								currentDrone.getVertexList((ArrayList<Point>) entry.getKey().coveredVertexList.clone());
								entry.getKey().getVertexList((ArrayList<Point>) currentDrone.coveredVertexList.clone());
							}
						}
					}
				}
			}

			calculateRepetition();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void calculateRepetition() {
		double sumPoints = 0;
		double count = 0;
		for (int x = 0; x <= gridPartition; x++) {
			for (int y = 0; y <= gridPartition; y++) {
				if (gridPoints[x][y].getAccessCount() < 1)
					System.out.println("Alert");
				sumPoints += gridPoints[x][y].getAccessCount();
				count++;
			}
		}
		System.out.println("Sum Points " + sumPoints + ", Count " + count);
		repetitionPercentage = ((sumPoints - count) * 100) / count;
	}

	private double pointDistance(Point a, Point b) {
		if (a == null || b == null)
			return -1;
		return Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2)) * cellWidth;
	}

	public boolean getLock(Point currentPoint) {
		if (currentPoint.getLock()) {
			return true;
		}
		collisionCount++;
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	public JSONArray getGridJSON() {

		JSONArray nodes = new JSONArray();
		for (int i = 0; i <= gridPartition; i++) {
			for (Point point : gridPoints[i]) {
				nodes.add(point.getJSON());
			}
		}
		return nodes;
	}
}
