package algorithm;

import model.Point2D;
import model.ControlPolygon;
import java.util.ArrayList;
import java.util.List;

public class DeCasteljau {
    public static Point2D evaluate(List<Point2D> controlPoints, double t) {
        if (controlPoints == null || controlPoints.isEmpty()) {
            throw new IllegalArgumentException("Необходими са поне една контролна точка");
        }

        int n = controlPoints.size();

        Point2D[] points = controlPoints.toArray(new Point2D[0]);

        for (int r = 1; r < n; r++) {
            for (int i = 0; i < n - r; i++) {
                points[i] = points[i].lerp(points[i + 1], t);
            }
        }

        return points[0];
    }

    public static List<List<Point2D>> evaluateWithSteps(List<Point2D> controlPoints, double t) {
        if (controlPoints == null || controlPoints.isEmpty()) {
            throw new IllegalArgumentException("Необходими са поне една контролна точка");
        }

        List<List<Point2D>> allLevels = new ArrayList<>();

        List<Point2D> currentLevel = new ArrayList<>(controlPoints);
        allLevels.add(new ArrayList<>(currentLevel));

        while (currentLevel.size() > 1) {
            List<Point2D> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size() - 1; i++) {
                nextLevel.add(currentLevel.get(i).lerp(currentLevel.get(i + 1), t));
            }
            allLevels.add(new ArrayList<>(nextLevel));
            currentLevel = nextLevel;
        }

        return allLevels;
    }

    public static List<Point2D> generateCurve(List<Point2D> controlPoints, int numSamples) {
        List<Point2D> curvePoints = new ArrayList<>();

        for (int i = 0; i <= numSamples; i++) {
            double t = (double) i / numSamples;
            curvePoints.add(evaluate(controlPoints, t));
        }

        return curvePoints;
    }

    public static List<Point2D> generateCurveAdaptive(List<Point2D> controlPoints, double tolerance) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (controlPoints.size() < 2) {
            curvePoints.addAll(controlPoints);
            return curvePoints;
        }

        curvePoints.add(evaluate(controlPoints, 0));
        subdivideAdaptive(controlPoints, 0.0, 1.0, tolerance, curvePoints);

        return curvePoints;
    }

    private static void subdivideAdaptive(List<Point2D> controlPoints,
                                           double t0, double t1,
                                           double tolerance,
                                           List<Point2D> result) {
        double tMid = (t0 + t1) / 2;

        Point2D p0 = evaluate(controlPoints, t0);
        Point2D p1 = evaluate(controlPoints, t1);
        Point2D pMid = evaluate(controlPoints, tMid);

        Point2D linearMid = p0.lerp(p1, 0.5);
        double error = pMid.distanceTo(linearMid);

        if (error > tolerance && (t1 - t0) > 0.001) {
            subdivideAdaptive(controlPoints, t0, tMid, tolerance, result);
            subdivideAdaptive(controlPoints, tMid, t1, tolerance, result);
        } else {
            result.add(p1);
        }
    }

    public static List<Point2D>[] subdivide(List<Point2D> controlPoints, double t) {
        List<List<Point2D>> levels = evaluateWithSteps(controlPoints, t);

        List<Point2D> leftCurve = new ArrayList<>();
        List<Point2D> rightCurve = new ArrayList<>();

        for (List<Point2D> level : levels) {
            leftCurve.add(level.get(0));
        }

        for (int i = levels.size() - 1; i >= 0; i--) {
            List<Point2D> level = levels.get(i);
            rightCurve.add(level.get(level.size() - 1));
        }

        return new List[] { leftCurve, rightCurve };
    }
}
