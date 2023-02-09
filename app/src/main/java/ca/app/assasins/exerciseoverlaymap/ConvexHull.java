package ca.app.assasins.exerciseoverlaymap;

import android.graphics.Point;

import java.util.ArrayList;

public class ConvexHull {

    private boolean CCW(Point p, Point q, Point r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y) > 0;
    }

    public ArrayList<Point> convexHull(ArrayList<Point> points) {
        int n = points.size();
        if (n <= 3) return points;

        ArrayList<Integer> next = new ArrayList<>();

        // find the leftmost point
        int leftMost = 0;
        for (int i = 1; i < n; i++)
            if (points.get(i).x < points.get(leftMost).x)
                leftMost = i;
        int p = leftMost, q;
        next.add(p);

        // iterate till p becomes leftMost
        do {
            q = (p + 1) % n;
            for (int i = 0; i < n; i++)
                if (CCW(points.get(p), points.get(i), points.get(q)))
                    q = i;
            next.add(q);
            p = q;
        } while (p != leftMost);

        ArrayList<Point> convexHullPoints = new ArrayList();
        for (int i = 0; i < next.size() - 1; i++) {
            int ix = next.get(i);
            convexHullPoints.add(points.get(ix));
        }

        return convexHullPoints;
    }
}
