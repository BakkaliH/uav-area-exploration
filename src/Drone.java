import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by Anurag on 6/4/2016.
 */

public class Drone extends Thread {

	ArrayList<Point> allVertexList, futureVertexList, coveredVertexList;
	double range;
	int droneId, color, swirlLength;
	boolean isAlive;
	Grid grid;
	Point startPoint, currentPoint;
	int gridPartitions;
	Random rand = new Random();
	int[] adjacentNodes;
	float totalDistance, speed;
	Object vertexListLock = new Object();
	Object allListLock = new Object();
	ArrayList<ArrayList<Point>> sharedVertexList = new ArrayList<ArrayList<Point>>();
	ArrayList<String> path = new ArrayList<>();
	private long droneTraversalTime;

	public Drone() {
		futureVertexList = new ArrayList<>();
		coveredVertexList = new ArrayList<>();
		adjacentNodes = new int[4];
		for (int i = 0; i < 4; i++)
			adjacentNodes[i] = -1;

		isAlive = true;
	}

	public Drone(int id, Grid grid, int color, double range, int speed, Point startPosition) {
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
		Thread droneList = new Thread(this, "listHandler");
		droneMotion.start();
		droneList.start();
	}

	public ArrayList<Point> getFutureVertexList() {

		ArrayList<Point> futureList = new ArrayList<>();
		int x = currentPoint.getX();
		int y = currentPoint.getY();

		int[] direction = getDirection(x, y);

		if (direction == null) {
			return null;
		}

		int a = 0, b = 0, c = 0, d = 0;
		if (direction[0] == 0 || direction[0] == 2) {
			a = y;
			d = -1 + direction[0];
			b = y + d * (1 - direction[1] * 2);
			c = x + d;
		}

		if (direction[0] == 1 || direction[0] == 3) {
			a = x;
			d = 2 - direction[0];
			b = x + (-1 * d) * (1 - direction[1] * 2);
			c = y + d;
		}

		boolean swirl = true;
		while (c >= 0 && c <= gridPartitions) {
			if (swirl) {
				if (direction[0] % 2 == 0) {
					futureList.add(grid.getPoint(c, a));
					futureList.add(grid.getPoint(c, b));
				} else {
					futureList.add(grid.getPoint(a, c));
					futureList.add(grid.getPoint(b, c));
				}
				swirl = false;
			} else {
				if (direction[0] % 2 == 0) {
					futureList.add(grid.getPoint(c, b));
					futureList.add(grid.getPoint(c, a));
				} else {
					futureList.add(grid.getPoint(b, c));
					futureList.add(grid.getPoint(a, c));
				}
				swirl = true;
			}
			// TODO
			for (int i = 1; i < swirlLength; i++) {
				futureList.add(grid.getPoint(b + i * (a - b), c));
			}
			c = c + d;
		}

		return futureList;
	}

	public int[] getDirection(int x, int y) {
		int[] direction = new int[2];

		boolean noWay = true;
		for (int i = 0; i < 4; i++) {
			adjacentNodes[i] = 0;
		}

		int maxDis = -1;
		int maxDir = -1;

		// NORTH
		if (x - 1 > -1 && !coveredVertexList.contains(grid.getPoint(x - 1, y))) {
			maxDis = x;
			maxDir = 0;
			adjacentNodes[0] = x;
			noWay = false;
		}

		// EAST
		if (y + 1 <= gridPartitions && !coveredVertexList.contains(grid.getPoint(x, y + 1))) {
			if (maxDis < gridPartitions - y) {
				maxDis = gridPartitions - y;
				maxDir = 1;
			}
			noWay = false;
			adjacentNodes[1] = gridPartitions - y;
		}

		// SOUTH
		if (x + 1 <= gridPartitions && !coveredVertexList.contains(grid.getPoint(x + 1, y))) {
			if (maxDis < gridPartitions - x) {
				maxDis = gridPartitions - x;
				maxDir = 2;
			}
			noWay = false;
			adjacentNodes[2] = gridPartitions - x;
		}

		// WEST
		if (y - 1 > -1 && !coveredVertexList.contains(grid.getPoint(x, y - 1))) {
			if (maxDis < y) {
				maxDis = y;
				maxDir = 3;
			}
			adjacentNodes[3] = y;
			noWay = false;
		}

		if (noWay) {
			return null;
		}

		direction[0] = maxDir;
		direction[1] = 0;
		if (maxDir == 0) {
			if (adjacentNodes[3] < adjacentNodes[1]) {
				direction[1] = 1;
			}
		} else {
			if (adjacentNodes[(maxDir - 1) % 4] < adjacentNodes[(maxDir + 1) % 4]) {
				direction[1] = 1;
			}
		}

		if (direction[0] == 0) {
			if (y == 0) {
				direction[1] = 1;
			} else if (y == gridPartitions) {
				direction[1] = 0;
			}
		}
		if (direction[0] == 2) {
			if (y == 0) {
				direction[1] = 0;
			} else if (y == gridPartitions) {
				direction[1] = 1;
			}
		}
		if (direction[0] == 1) {
			if (x == 0) {
				direction[1] = 1;
			} else if (x == gridPartitions) {
				direction[1] = 0;
			}
		}
		if (direction[0] == 3) {
			if (x == 0) {
				direction[1] = 0;
			} else if (x == gridPartitions) {
				direction[1] = 1;
			}
		}
		return direction;
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
		} else {
			listHandler();
		}
	}

	public void handleMotion() {
		Point lastPoint;
		currentPoint = startPoint;
		boolean startIncluded = false;
		boolean completeFlag = false;
		try {
			while (isAlive && !allVertexList.isEmpty()) {

				futureVertexList = getFutureVertexList();
				while (isAlive && futureVertexList == null) {
					if (!allVertexList.isEmpty()) {
						lastPoint = currentPoint;
						currentPoint = getNearestPoint(lastPoint);
						if (currentPoint == null)
							continue;
						if (currentPoint == lastPoint) {
							allVertexList.remove(currentPoint);
							continue;
						}
						totalDistance += pointDistance(lastPoint, currentPoint);

						futureVertexList = getFutureVertexList();
						if (futureVertexList == null) {
							futureVertexList = new ArrayList<>();
						}
						futureVertexList.add(0, currentPoint);
					} else {
						completeFlag = true;
						break;
					}
				}

				if (completeFlag)
					break;

				if (!startIncluded) {
					futureVertexList.add(0, startPoint);
					startIncluded = true;
				}
				currentPoint = futureVertexList.remove(0);
				boolean notStart = false;
				while (isAlive && currentPoint != null && !coveredVertexList.contains(currentPoint)) {
					coveredVertexList.add(currentPoint);
					allVertexList.remove(currentPoint);
					if (notStart) {
						totalDistance += grid.cellWidth;
					}
					notStart = true;

					if (grid.getLock(currentPoint)) {
						grid.changePosition(this);
						Thread.sleep(200);
						currentPoint.setCovered(this.droneId);
						path.add("" + currentPoint.getX() + " " + currentPoint.getY());
						grid.removePosition(this);
					} else {
						futureVertexList.clear();
					}
					if (futureVertexList.size() == 0)
						break;

					currentPoint = futureVertexList.remove(0);
				}
			}
			
			isAlive = false;
			synchronized (vertexListLock) {
				vertexListLock.notify();
			}
			
			//System.out.println("Finishing at : " + currentPoint);
			//grid.changePosition(this);
			grid.markFinished(this.droneId);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Point getNearestPoint(Point current) {
		Point minPoint = null;
		double mindis = Double.MAX_VALUE;
		double dis = 0;
		synchronized (allListLock) {
			int count = allVertexList.size() - 1;
			while (count > -1) {
				Point point = allVertexList.get(count);
				dis = pointDistance(current, point);
				if (dis < mindis) {
					mindis = dis;
					minPoint = point;
				}
				count--;

				if (count > 300 && mindis < 3 * grid.cellWidth) {
					break;
				}
			}
		}
		return minPoint;
	}

	private double pointDistance(Point a, Point b) {
		double val = Math.sqrt(Math.pow(b.getX() - a.getX(), 2) + Math.pow(b.getY() - a.getY(), 2));
		return val * grid.cellWidth;
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

		String[] start = path.remove(0).split(" ");
		for (String end : path) {
			nodeObj.put("id", "" + count++);
			nodeObj.put("source", start[0] + start[1]);
			String temp[] = end.split(" ");
			nodeObj.put("target", temp[0] + temp[1]);
			nodeObj.put("color", grid.getPoint(Integer.parseInt(start[0]), Integer.parseInt(start[1])).getColor());
			nodeObj.put("type", "arrow");
			edgeList.add(nodeObj);
			start = end.split(" ");
			nodeObj = new JSONObject();
		}

	}

	public void listHandler() {
		ArrayList<Point> newPoints;
		try {
			synchronized (vertexListLock) {
				while (isAlive) {
					vertexListLock.wait();

					while (sharedVertexList.size() > 0) {
						newPoints = sharedVertexList.remove(0);

						for (Point point : newPoints) {
								synchronized (allListLock) {
								if (!coveredVertexList.contains(point)) {
									coveredVertexList.add(point);
									allVertexList.remove(point);
								}
							}
						}
						vertexListLock.wait(50);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
