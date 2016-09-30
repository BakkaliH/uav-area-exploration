import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Simulate {
	public static void main(String[] args) {

		// Scanner scr = new Scanner(System.in);
		Random rand = new Random();
		double gridWidth = 280;
		// int noOfDrones = scr.nextInt();
		int noOfDrones = 100;
		double partition = 63;

		Grid grid = new Grid(gridWidth, (int) partition, noOfDrones, 1);
		Drone[] drones = new Drone[noOfDrones];
		ArrayList<Point> startPoints = new ArrayList<>();

		int x = Math.abs(rand.nextInt()) % (int) partition;
		int y = Math.abs(rand.nextInt()) % (int) partition;
		Point newPoint = grid.getPoint(x, y);
		for (int d = 0; d < noOfDrones; d++) {
			while (startPoints.contains(newPoint)) {
				x = Math.abs(rand.nextInt()) % (int) partition;
				y = Math.abs(rand.nextInt()) % (int) partition;
				newPoint = grid.getPoint(x, y);
			}
			drones[d] = new Drone(d + 1, grid, d + 1, 0.75, 2, newPoint);
			startPoints.add(newPoint);
		}

		long startTime = System.currentTimeMillis();
		grid.start();
		for (Drone drone : drones) {
			drone.startDrone();
		}

		try {
			grid.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// prepareJson(grid, drones, startTime);
		displayResults(grid, drones, startTime);

	}

	private static void displayResults(Grid grid, Drone[] drones, long startTime) {
		System.out.println("Simulation finished in " + (System.currentTimeMillis() - startTime));
		double totalDroneDis = 0;
		for (Drone drone : drones) {
			double droneDis = drone.totalDistance;
			totalDroneDis += droneDis;
			System.out.println(drone.droneId + "(" + drone.startPoint.getX() + ", " + drone.startPoint.getY() + ")"
					+ ". " + drone.getTraversalTime() + "  " + droneDis);
		}

		System.out.println("Total Drone Distance: " + totalDroneDis);
		System.out.println("Total Distance: " + grid.totalGridDistance);
		System.out.println("Shortest Distance: " + grid.shortestDistance);
		System.out.println("Collisions: " + grid.collisionCount);
		System.out.println("List sharing: " + grid.listSharing);
		System.out.println("Repetition Percentage: " + grid.repetitionPercentage);
	}

	@SuppressWarnings({ "unchecked" })
	private static void prepareJson(Grid grid, Drone[] drones, long startTime) {

		JSONObject result = new JSONObject();
		result.put("nodes", grid.getGridJSON());
		JSONArray edgeList = new JSONArray();
		for (Drone drone : drones) {
			drone.getJSON(edgeList);
		}
		result.put("edges", edgeList);

		try {
			FileWriter file = new FileWriter("resources/grid.json");
			file.write(result.toJSONString());
			file.close();
			// System.out.println("\nJSON Object: " + result);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
