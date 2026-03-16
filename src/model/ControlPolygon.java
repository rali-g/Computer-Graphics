package model;

import java.util.ArrayList;
import java.util.List;

public class ControlPolygon {
    private List<Point2D> points;

    public ControlPolygon() {
        this.points = new ArrayList<>();
    }

    public ControlPolygon(List<Point2D> points) {
        this.points = new ArrayList<>(points);
    }

    public void addPoint(Point2D point) {
        points.add(point);
    }

    public void removePoint(int index) {
        if (index >= 0 && index < points.size()) {
            points.remove(index);
        }
    }

    public Point2D getPoint(int index) {
        return points.get(index);
    }

    public void setPoint(int index, Point2D point) {
        if (index >= 0 && index < points.size()) {
            points.set(index, point);
        }
    }

    public int size() {
        return points.size();
    }

    public List<Point2D> getPoints() {
        return new ArrayList<>(points);
    }

    public void clear() {
        points.clear();
    }

    public int findClosestPoint(double x, double y, double threshold) {
        Point2D target = new Point2D(x, y);
        int closestIndex = -1;
        double minDistance = threshold;

        for (int i = 0; i < points.size(); i++) {
            double dist = points.get(i).distanceTo(target);
            if (dist < minDistance) {
                minDistance = dist;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(points.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    public static ControlPolygon fromJson(String json) {
        ControlPolygon polygon = new ControlPolygon();

        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        if (json.trim().isEmpty()) {
            return polygon;
        }

        int depth = 0;
        int start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String pointJson = json.substring(start, i + 1).trim();
                    Point2D point = parsePoint(pointJson);
                    if (point != null) {
                        polygon.addPoint(point);
                    }
                    start = i + 2;
                }
            }
        }

        return polygon;
    }

    private static Point2D parsePoint(String json) {
        try {
            double x = 0, y = 0;

            int xIndex = json.indexOf("\"x\"");
            if (xIndex >= 0) {
                int colonIndex = json.indexOf(":", xIndex);
                int endIndex = json.indexOf(",", colonIndex);
                if (endIndex < 0) endIndex = json.indexOf("}", colonIndex);
                String xStr = json.substring(colonIndex + 1, endIndex).trim();
                x = Double.parseDouble(xStr);
            }

            int yIndex = json.indexOf("\"y\"");
            if (yIndex >= 0) {
                int colonIndex = json.indexOf(":", yIndex);
                int endIndex = json.indexOf(",", colonIndex);
                if (endIndex < 0) endIndex = json.indexOf("}", colonIndex);
                String yStr = json.substring(colonIndex + 1, endIndex).trim();
                y = Double.parseDouble(yStr);
            }

            return new Point2D(x, y);
        } catch (Exception e) {
            return null;
        }
    }
}
