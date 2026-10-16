package com.example.daysinc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.daysinc.models.GeofenceBroadcastReceiver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;

public class MapActivity extends AppCompatActivity {

    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private GeofencingClient geofencingClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final double GEOFENCE_RADIUS_METERS = 100.0;
    private static final String GEOFENCE_ID = "STUDY_ZONE_GEOFENCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().setUserAgentValue("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", MODE_PRIVATE));

        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);

        OnlineTileSourceBase hotTileSource = new OnlineTileSourceBase(
                "OSM_HOT",
                0, 19, 256, ".png",
                new String[]{
                        "https://a.tile.openstreetmap.fr/hot/",
                        "https://b.tile.openstreetmap.fr/hot/"
                }
        ) {
            @Override
            public String getTileURLString(long pMapTileIndex) {
                return getBaseUrl() + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                        MapTileIndex.getX(pMapTileIndex) + "/" +
                        MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding;
            }
        };

        map.setTileSource(hotTileSource);
        map.setMultiTouchControls(true);
        map.setBuiltInZoomControls(true);
        map.getController().setZoom(17.0);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);

        MaterialButton btnConfirmAndReturn = findViewById(R.id.btnConfirmAndReturn);
        if (btnConfirmAndReturn != null) {
            btnConfirmAndReturn.setOnClickListener(v -> {
                Toast.makeText(this, "📍 Zona de Actividades confirmada", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        Overlay touchOverlay = new Overlay() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                GeoPoint p = (GeoPoint) mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                setSelectedZone(p);
                return true;
            }
        };
        map.getOverlays().add(touchOverlay);

        checkLocationPermissionAndGetLocation();
    }

    private void setSelectedZone(GeoPoint point) {
        map.getOverlays().clear();

        Marker startMarker = new Marker(map);
        startMarker.setPosition(point);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle("Zona de Actividades Registrada");
        map.getOverlays().add(startMarker);

        Polygon circle = new Polygon(map);
        circle.setPoints(createCirclePoints(point, GEOFENCE_RADIUS_METERS));
        circle.getFillPaint().setColor(0x220000FF);
        circle.getOutlinePaint().setColor(Color.BLUE);
        circle.getOutlinePaint().setStrokeWidth(3f);
        map.getOverlays().add(circle);

        map.invalidate();

        SharedPreferences prefs = getSharedPreferences("StudyZonePrefs", MODE_PRIVATE);
        prefs.edit()
                .putFloat("zone_lat", (float) point.getLatitude())
                .putFloat("zone_lng", (float) point.getLongitude())
                .putBoolean("zone_set", true)
                .apply();

        addGeofence(point);
    }

    private ArrayList<GeoPoint> createCirclePoints(GeoPoint center, double radiusInMeters) {
        ArrayList<GeoPoint> circlePoints = new ArrayList<>();
        int numberOfPoints = 36;
        for (int i = 0; i < numberOfPoints; i++) {
            double angle = Math.toRadians((double) i * 360 / numberOfPoints);
            double latitude = center.getLatitude() + (radiusInMeters / 111320.0) * Math.cos(angle);
            double longitude = center.getLongitude() + (radiusInMeters / (111320.0 * Math.cos(Math.toRadians(center.getLatitude())))) * Math.sin(angle);
            circlePoints.add(new GeoPoint(latitude, longitude));
        }
        return circlePoints;
    }

    @SuppressLint("MissingPermission")
    private void checkLocationPermissionAndGetLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        GeoPoint currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        map.getController().setZoom(17.0);
                        map.getController().animateTo(currentPoint);

                        Marker myLocationMarker = new Marker(map);
                        myLocationMarker.setPosition(currentPoint);
                        myLocationMarker.setTitle("Mi Ubicación Actual");
                        map.getOverlays().add(myLocationMarker);
                        map.invalidate();
                    } else {
                        GeoPoint defaultPoint = new GeoPoint(-20.2133, -70.1503);
                        map.getController().setCenter(defaultPoint);
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void addGeofence(GeoPoint point) {
        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(point.getLatitude(), point.getLongitude(), (float) GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );

        geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "📍 Zona de Actividades guardada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error de geofence: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermissionAndGetLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}