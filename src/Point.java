import org.json.simple.JSONObject;

/**
 * Created by Anurag on 6/4/2016.
 */
public class Point {
	private int x, y;
	private String color;
	private int droneId;
	private int accessCount;
	private boolean pointLock;
	private String[] colors = { "#ec5148", "#617db4", "#668f3c", "#c6583e", "#b956af", "#c3dfc6", "#ff9966", "#f9e200",
			" #ccf2ff" };
	boolean startPoint = false;

	public Point(int x, int y, int color) {
		this.x = x;
		this.y = y;
		accessCount = 0;
		this.color = colors[color];
	}

	public int getAccessCount() {
		return accessCount;
	}

	public void setStartPoint() {
		startPoint = true;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public String getColor() {
		return color;
	}

	public void setCovered(int drone) {
		// System.out.println(x + " : " + y + " (" + drone + ")");
		this.droneId = drone;
		// this.color = colors[drone];
		accessCount++;
		if (accessCount > 1 && !startPoint) {
			color = colors[0];
		}
		pointLock = false;
	}

	public void setColor(int color) {
		this.color = colors[color];
	}

	public int getDroneId() {
		return droneId;
	}

	public void setDroneId(int droneId) {
		this.droneId = droneId;
	}

	public synchronized boolean getLock() {
		if (!pointLock) {
			pointLock = true;
			return true;
		} else {
			// System.out.println("found locked");
			return false;
		}
	}

	public synchronized void releaseLock() {
		pointLock = false;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getJSON() {

		JSONObject pointObj = new JSONObject();
		pointObj.put("id", "" + x + y);
		pointObj.put("label", "" + x + y);
		pointObj.put("x", y);
		pointObj.put("y", x);
		if (startPoint) {
			pointObj.put("size", 3);
		} else {
			pointObj.put("size", 2);
		}
		pointObj.put("color", color);
		return pointObj;

	}
}
