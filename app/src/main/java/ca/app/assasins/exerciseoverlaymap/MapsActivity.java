package ca.app.assasins.exerciseoverlaymap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;

import ca.app.assasins.exerciseoverlaymap.databinding.ActivityMapsBinding;
import ca.app.assasins.exerciseoverlaymap.util.DistanceUtil;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private enum Letters {
        A, B, C, D
    }

    private static final int POLYGON_SIDES = 4;
    private static final int POLYLINE_SIDES = 4;
    private GoogleMap mMap;
    private Polygon shape;
    private Polyline line;
    private final List<Polyline> lines = new ArrayList<>();
    private final List<Marker> textMarkers = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ca.app.assasins.exerciseoverlaymap.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMinZoomPreference(10);

        LatLng toronto = new LatLng(43.6532, -79.3832);
        mMap.addMarker(new MarkerOptions().position(toronto).title("Marker in Toronto"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(toronto));

        // apply long press to add marker to the map
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                Letters[] letters = Letters.values();

                if (markers.size() == POLYLINE_SIDES) {
                    clearMap();
                }

                MarkerOptions optionsMarker = new MarkerOptions().position(latLng)
                        .title(letters[markers.size()].name());

                markers.add(mMap.addMarker(optionsMarker));

                if (markers.size() == POLYGON_SIDES) {
                    this.drawPolygonAndPolylineShape();

                }
            }

            private void drawLineFromTwoMarkers(LatLng fromLatLng, LatLng toLatLng) {

                PolylineOptions polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.RED);
                polylineOptions.clickable(true);
                polylineOptions.zIndex(1000);
                polylineOptions.width(15);
                polylineOptions.add(fromLatLng, toLatLng);

                line = mMap.addPolyline(polylineOptions);
                line.setTag(line.getId());
                lines.add(line);
            }

            private void drawPolygonAndPolylineShape() {
                Projection mapProjection = mMap.getProjection();

                ArrayList<Point> screenPoints = new ArrayList<>();

                PolygonOptions polygonOptions = new PolygonOptions()
                        .fillColor(getColor(R.color.green_transp_35))
                        .strokeWidth(5);

                for (int i = 0; i < POLYGON_SIDES; i++) {
                    screenPoints.add(mapProjection.toScreenLocation(markers.get(i).getPosition()));
                }

                ArrayList<Point> convexHullPoints = new ConvexHull().convexHull(screenPoints);
                ArrayList<LatLng> convexHullLocationPoints = new ArrayList<>(convexHullPoints.size());

                for (Point screenPoint : convexHullPoints) {
                    LatLng location = mapProjection.fromScreenLocation(screenPoint);
                    convexHullLocationPoints.add(location);
                }

                for (int i = 0; i < convexHullLocationPoints.size(); i++) {

                    polygonOptions.add(convexHullLocationPoints.get(i));

                    if (i == convexHullLocationPoints.size() - 1) {
                        break;
                    }
                    drawLineFromTwoMarkers(convexHullLocationPoints.get(i), convexHullLocationPoints.get(i + 1));
                }

                drawLineFromTwoMarkers(convexHullLocationPoints.get(convexHullLocationPoints.size() - 1), convexHullLocationPoints.get(0));

                shape = mMap.addPolygon(polygonOptions);
                shape.setClickable(true);
            }

            // Clear all markers from the map
            private void clearMap() {
                for (Marker marker : markers) {
                    marker.remove();
                }
                markers.clear();
                shape.remove();

                for (Polyline polyline : lines) {
                    polyline.remove();
                }

                for (Marker textMarker : textMarkers) {
                    textMarker.remove();
                }
                textMarkers.clear();

                lines.clear();
                line.remove();

                shape = null;
                line = null;
            }
        });

        // On polyline click listener show the distance in the line clicked
        mMap.setOnPolylineClickListener(polyline -> {

            LatLng pointA = polyline.getPoints().get(0);
            LatLng pointB = polyline.getPoints().get(polyline.getPoints().size() - 1);

            String distance = new DistanceUtil().showDistance(pointA, pointB);
            System.out.println(distance);
            LatLng midPoint = SphericalUtil.interpolate(pointA, pointB, 0.5);

            Marker markerForDistanceText = addText(this, mMap, midPoint, distance, 2, 14);
            textMarkers.add(markerForDistanceText);

        });

        // On Polygon click listener show the distance in the middle
        mMap.setOnPolygonClickListener(polygon -> {
            double distance = 0.0;

            for (int i = 0; i < lines.size(); i++) {
                if (i == lines.size() - 1) {
                    Polyline polyline = lines.get(lines.size() - 1);
                    distance += SphericalUtil.computeDistanceBetween(polyline.getPoints().get(0), polyline.getPoints().get(1));
                    break;
                }
                Polyline polyline = lines.get(i);
                distance += SphericalUtil.computeDistanceBetween(polyline.getPoints().get(0), polyline.getPoints().get(1));

                System.out.println("Distance SUM: " + distance);
            }

            double latitude = 0.0;
            double longitude = 0.0;

            for (int i = 0; i < polygon.getPoints().size() - 1; i++) {
                latitude += polygon.getPoints().get(i).latitude;
                longitude += polygon.getPoints().get(i).longitude;
            }
            // Get the mid point X
            LatLng centerPoint = new LatLng((latitude * 0.5) / 2, (longitude * 0.5) / 2);

            Marker markerForDistanceText = addText(this, mMap, centerPoint, new DistanceUtil().formatNumber(distance), 2, 18);
            textMarkers.add(markerForDistanceText);
        });
    }

    // Marker as a textField
    public Marker addText(final Context context, final GoogleMap map,
                          final LatLng location, final String text, final int padding,
                          final int fontSize) {
        Marker marker;

        if (context == null || map == null || location == null || text == null || fontSize <= 0) {
            return null;
        }

        final TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(fontSize);
        textView.setTextColor(Color.BLUE);

        final Paint paintText = textView.getPaint();

        final Rect boundsText = new Rect();
        paintText.getTextBounds(text, 0, textView.length(), boundsText);
        paintText.setTextAlign(Paint.Align.CENTER);

        final Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        final Bitmap bmpText = Bitmap.createBitmap(boundsText.width() + 2
                * padding, boundsText.height() + 2 * padding, conf);

        final Canvas canvasText = new Canvas(bmpText);
        paintText.setColor(Color.BLUE);
        paintText.setTextAlign(Paint.Align.CENTER);

        canvasText.drawText(text, (float) canvasText.getWidth() / 2,
                canvasText.getHeight() - padding - boundsText.bottom, paintText);

        final MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
                .anchor(0.5f, 1);

        marker = map.addMarker(markerOptions);

        return marker;
    }
}