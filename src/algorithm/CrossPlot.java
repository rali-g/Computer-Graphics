package algorithm;

import model.Point2D;
import model.ControlPolygon;
import java.util.ArrayList;
import java.util.List;

public class CrossPlot {

    private double tension = 1.0;
    private int samplesPerSegment = 50;

    public CrossPlot() {
    }

    public CrossPlot(double tension) {
        this.tension = Math.max(0.0, Math.min(2.0, tension));
    }

    public void setTension(double tension) {
        this.tension = Math.max(0.0, Math.min(2.0, tension));
    }

    public double getTension() {
        return tension;
    }

    public void setSamplesPerSegment(int samples) {
        this.samplesPerSegment = Math.max(10, samples);
    }

    public List<Point2D> computeCurve(List<Point2D> interpolationPoints) {
        List<Point2D> curvePoints = new ArrayList<>();

        if (interpolationPoints == null || interpolationPoints.size() < 2) {
            if (interpolationPoints != null) {
                curvePoints.addAll(interpolationPoints);
            }
            return curvePoints;
        }

        int n = interpolationPoints.size();

        List<Point2D> tangents = computeTangents(interpolationPoints);

        List<Double> alphas = computeAlphas(interpolationPoints);

        for (int i = 0; i < n - 1; i++) {
            Point2D p0 = interpolationPoints.get(i);
            Point2D p3 = interpolationPoints.get(i + 1);

            Point2D t0 = tangents.get(i);
            Point2D t1 = tangents.get(i + 1);

            double alpha0 = alphas.get(i);
            double alpha1 = alphas.get(i + 1);

            // Q₁ = P₀ + (1/3) * α₀ * T₀
            Point2D p1 = p0.add(t0.multiply(alpha0 / 3.0));

            // Q₂ = P₃ - (1/3) * α₁ * T₁
            Point2D p2 = p3.add(t1.multiply(-alpha1 / 3.0));

            List<Point2D> controlPoints = new ArrayList<>();
            controlPoints.add(p0);
            controlPoints.add(p1);
            controlPoints.add(p2);
            controlPoints.add(p3);

            List<Point2D> segmentPoints = DeCasteljau.generateCurve(controlPoints, samplesPerSegment);

            if (i == 0) {
                curvePoints.addAll(segmentPoints);
            } else {
                curvePoints.addAll(segmentPoints.subList(1, segmentPoints.size()));
            }
        }

        return curvePoints;
    }

    public List<List<Point2D>> computeControlPolygons(List<Point2D> interpolationPoints) {
        List<List<Point2D>> allControlPolygons = new ArrayList<>();

        if (interpolationPoints == null || interpolationPoints.size() < 2) {
            return allControlPolygons;
        }

        int n = interpolationPoints.size();
        List<Point2D> tangents = computeTangents(interpolationPoints);
        List<Double> alphas = computeAlphas(interpolationPoints);

        for (int i = 0; i < n - 1; i++) {
            Point2D p0 = interpolationPoints.get(i);
            Point2D p3 = interpolationPoints.get(i + 1);
            Point2D t0 = tangents.get(i);
            Point2D t1 = tangents.get(i + 1);
            double alpha0 = alphas.get(i);
            double alpha1 = alphas.get(i + 1);

            Point2D p1 = p0.add(t0.multiply(alpha0 / 3.0));
            Point2D p2 = p3.add(t1.multiply(-alpha1 / 3.0));

            List<Point2D> controlPoints = new ArrayList<>();
            controlPoints.add(p0);
            controlPoints.add(p1);
            controlPoints.add(p2);
            controlPoints.add(p3);

            allControlPolygons.add(controlPoints);
        }

        return allControlPolygons;
    }

    private List<Point2D> computeTangents(List<Point2D> points) {
        List<Point2D> tangents = new ArrayList<>();
        int n = points.size();

        for (int i = 0; i < n; i++) {
            Point2D tangent;

            if (i == 0) {
                tangent = computeDirection(points.get(0), points.get(1));
            } else if (i == n - 1) {
                tangent = computeDirection(points.get(n - 2), points.get(n - 1));
            } else {
                tangent = computeDirection(points.get(i - 1), points.get(i + 1));
            }

            tangents.add(tangent);
        }

        return tangents;
    }

    private Point2D computeDirection(Point2D from, Point2D to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 1e-10) {
            return new Point2D(1, 0);
        }

        return new Point2D(dx / length, dy / length);
    }

    private List<Double> computeAlphas(List<Point2D> points) {
        List<Double> alphas = new ArrayList<>();
        int n = points.size();

        for (int i = 0; i < n; i++) {
            double alpha;

            if (i == 0) {
                alpha = points.get(0).distanceTo(points.get(1)) * tension;
            } else if (i == n - 1) {
                alpha = points.get(n - 2).distanceTo(points.get(n - 1)) * tension;
            } else {
                double d1 = points.get(i).distanceTo(points.get(i - 1));
                double d2 = points.get(i).distanceTo(points.get(i + 1));
                alpha = (d1 + d2) / 2.0 * tension;
            }

            alphas.add(alpha);
        }

        return alphas;
    }

    public static double crossRatio(Point2D a, Point2D b, Point2D c, Point2D d) {
        double ac = a.distanceTo(c);
        double bd = b.distanceTo(d);
        double bc = b.distanceTo(c);
        double ad = a.distanceTo(d);

        if (Math.abs(bc * ad) < 1e-10) {
            return Double.POSITIVE_INFINITY;
        }

        return (ac * bd) / (bc * ad);
    }

    public static String pointsToJson(List<Point2D> points) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(points.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    public String generateFullResponse(List<Point2D> interpolationPoints) {
        List<Point2D> curvePoints = computeCurve(interpolationPoints);
        List<List<Point2D>> controlPolygons = computeControlPolygons(interpolationPoints);

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"curve\":").append(pointsToJson(curvePoints)).append(",");
        sb.append("\"controlPolygons\":[");
        for (int i = 0; i < controlPolygons.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(pointsToJson(controlPolygons.get(i)));
        }
        sb.append("],");
        sb.append("\"tension\":").append(tension).append(",");
        sb.append("\"interpolationPoints\":").append(pointsToJson(interpolationPoints));
        sb.append("}");

        return sb.toString();
    }
}
