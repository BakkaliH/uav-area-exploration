import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by Anurag on 6/4/2016.
 */

public class DroneNew extends Thread {

	ArrayList<Point> allVertexList, futureVertexList, coveredVertexList;
	double range;
	int droneId, color, swirlLength;
	boolean isAlive;
	GridNew grid;
	Point startPoint, currentPoint;
	int gridPartitions;
	Random rand = new Random();
	int[] adjacentNodes;
	double totalDistance, speed;
	Object vertexListLock = new Object();
	ArrayList<ArrayList<Point>> sharedVertexList = new ArrayList<ArrayList<Point>>();
	ArrayList<String> path = new ArrayList<>();
	private long droneTraversalTime;

	public DroneNew() {
		futureVertexList = new ArrayList<>();
		coveredVertexList = new ArrayList<>();
		adjacentNodes = new int[4];
		for (int i = 0; i < 4; i++)
			adjacentNodes[i] = -1;

		isAlive = true;
	}

	public DroneNew(int id, GridNew grid, int color, double range, int speed, Point startPosition) {
		this();
		this.droneId = id;
		this.grid = grid;
		this.gridPartitions = grid.gridPartition;
		this.range = range * grid.cellWidth;
		this.speed = speed;
		this.color = color;

		allVertexList = grid.getVertexList();
		startPoint = startPosition;
		startPoint.setStartPoint();
	}

	public void startDrone() {
		Thread droneMotion = new Thread(this, "motionHandler");
		droneMotion.start();
	}

	public boolean getStatus() {
		return isAlive;
	}

	public void crashDrone() {
		isAlive = false;
	}

	public void run() {
		if (Thread.currentThread().getName().equals("motionHandler")) {
			long startTime = System.currentTimeMillis();
			handleMotion();
			droneTraversalTime = System.currentTimeMillis() - startTime;
		}
	}

	public void handleMotion() {
		// int pointsCollected = 0;
		currentPoint = startPoint;
		try {
			calculateList();
			totalDistance = pointDistance(startPoint, futureVertexList.get(0));

			while (futureVertexList.size() > 0) {
				currentPoint = futureVertexList.remove(0);
				totalDistance += grid.cellWidth;
				if (grid.getLock(currentPoint)) {
					// grid.changePosition(this);
					Thread.sleep(200);
					currentPoint.setCovered(this.droneId);
					// pointsCollected++;
					path.add("" + currentPoint.getX() + " " + currentPoint.getY());
					grid.removePosition(this);
				} else {
					System.out.println("did clear");
					futureVertexList.clear();
				}
				if (futureVertexList.size() == 0)
					break;

			}

			grid.markFinished(this.droneId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private double pointDistance(Point a, Point b) {
		return Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2)) * grid.cellWidth;
	}

	public void getVertexList(ArrayList<Point> coveredPoints) {
		synchronized (vertexListLock) {
			sharedVertexList.add(coveredPoints);
			vertexListLock.notify();
		}
	}

	public long getTraversalTime() {
		return droneTraversalTime;
	}

	@SuppressWarnings("unchecked")
	public void getJSON(JSONArray edgeList) {
		int count = edgeList.size();
		JSONObject nodeObj = new JSONObject();

		String[] start = path.remove(0).trim().split(" ");
		for (String end : path) {
			nodeObj.put("id", "" + count++);
			nodeObj.put("source", start[0] + start[1]);
			String temp[] = end.trim().split(" ");
			nodeObj.put("target", temp[0] + temp[1]);
			nodeObj.put("color", grid.getPoint(Integer.parseInt(start[0]), Integer.parseInt(start[1])).getColor());
			nodeObj.put("type", "arrow");
			edgeList.add(nodeObj);
			start = end.trim().split(" ");
			nodeObj = new JSONObject();
		}

	}


	public void calculateList() {
		int i, k = 0, l = 0;
		int m = grid.gridPartition + 1;
		int n = grid.gridPartition + 1;
		int pointVal = 0;
		double totalPoints = Math.pow(grid.gridPartition + 1, 2);
		int pointsPerDrone = (int) ( totalPoints / grid.noOfDrones );
		int rem = (int) (totalPoints % grid.noOfDrones);
		/*if (pointsPerDrone - (int) pointsPerDrone != 0) {
			pointVal = (int) pointsPerDrone;
			pointsPerDrone = 1 + (int) pointsPerDrone;
		}*/

		int droneRangeStart = ((droneId - 1) * pointsPerDrone);
		int droneRangeEnd = (droneRangeStart + (int) pointsPerDrone) - 1;
		if (rem >= droneId) {
			droneRangeStart += (droneId - 1);
			droneRangeEnd += droneId;
		} else
		{
			droneRangeStart += rem;
			droneRangeEnd += rem;
		}
			
		int counter = 0;
		while (k < m && l < n && counter <= droneRangeEnd) {
			for (i = l; i < n; ++i) {
				if (counter >= droneRangeStart && counter <= droneRangeEnd) {
					futureVertexList.add(grid.getPoint(k, i));
				}
				counter++;
			}
			k++;

			for (i = k; i < m; ++i) {
				if (counter >= droneRangeStart && counter <= droneRangeEnd) {
					futureVertexList.add(grid.getPoint(i, n - 1));
				}
				counter++;
			}
			n--;

			if (k < m) {
				for (i = n - 1; i >= l; --i) {
					if (counter >= droneRangeStart && counter <= droneRangeEnd) {
						futureVertexList.add(grid.getPoint(m - 1, i));
					}
					counter++;
				}
				m--;
			}

			if (l < n) {
				for (i = m - 1; i >= k; --i) {
					if (counter >= droneRangeStart && counter <= droneRangeEnd) {
						futureVertexList.add(grid.getPoint(i, l));
					}
					counter++;
				}
				l++;
			}
		}
	}

}
