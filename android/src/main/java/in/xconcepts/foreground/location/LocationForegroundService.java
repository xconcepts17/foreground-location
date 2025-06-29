package in.xconcepts.foreground.location;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationForegroundService extends Service {
    private static final String TAG = "LocationForegroundService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "location_service_channel";
    
    public static final String ACTION_LOCATION_UPDATE = "in.xconcepts.foreground.location.LOCATION_UPDATE";
    public static final String ACTION_SERVICE_STATUS = "in.xconcepts.foreground.location.SERVICE_STATUS";
    public static final String EXTRA_LOCATION_DATA = "location_data";
    public static final String EXTRA_SERVICE_STATUS = "service_status";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private NotificationManager notificationManager;
    private boolean isLocationUpdatesActive = false;
    
    // Service configuration
    private String notificationTitle = "Location Tracking";
    private String notificationText = "Tracking your location in the background";
    private String notificationIcon = null; // Custom icon name from app
    private long updateInterval = 60000; // 1 minute
    private long fastestInterval = 30000; // 30 seconds
    private int priority = Priority.PRIORITY_HIGH_ACCURACY;
    private long distanceFilter = 0; // Distance filter in meters (0 = no filter)

    // API Service Integration
    private APIService apiService;
    private boolean enableApiService = false;
    
    // API Configuration
    private String apiUrl;
    private String apiType;
    private Map<String, String> apiHeaders;
    private JSONObject apiAdditionalParams;
    private int apiIntervalMinutes = 5;

    private final IBinder binder = new LocationServiceBinder();

    public class LocationServiceBinder extends Binder {
        LocationForegroundService getService() {
            return LocationForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationForegroundService created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        apiService = new APIService(this); // Initialize API service

        createNotificationChannel();
        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationForegroundService started");

        if (intent != null) {
            extractConfiguration(intent);
        }

        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();

        // Restart service if killed by system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "LocationForegroundService destroyed");
        stopLocationUpdates();
        
        // Cleanup API service
        if (apiService != null) {
            apiService.shutdown();
        }
        
        super.onDestroy();
    }

    private void extractConfiguration(Intent intent) {
        if (intent.hasExtra("notificationTitle")) {
            notificationTitle = intent.getStringExtra("notificationTitle");
        }
        if (intent.hasExtra("notificationText")) {
            notificationText = intent.getStringExtra("notificationText");
        }
        if (intent.hasExtra("notificationIcon")) {
            notificationIcon = intent.getStringExtra("notificationIcon");
        }
        if (intent.hasExtra("updateInterval")) {
            updateInterval = intent.getLongExtra("updateInterval", 60000);
        }
        if (intent.hasExtra("fastestInterval")) {
            fastestInterval = intent.getLongExtra("fastestInterval", 30000);
        }
        if (intent.hasExtra("priority")) {
            String priorityStr = intent.getStringExtra("priority");
            priority = convertPriority(priorityStr);
        }
        if (intent.hasExtra("distanceFilter")) {
            distanceFilter = intent.getLongExtra("distanceFilter", 0L);
            Log.d(TAG, "Distance filter set to: " + distanceFilter + " meters");
        }
        
        // Extract API configuration
        if (intent.hasExtra("apiUrl")) {
            apiUrl = intent.getStringExtra("apiUrl");
            enableApiService = apiUrl != null && !apiUrl.isEmpty();
            Log.d(TAG, "API Service " + (enableApiService ? "enabled" : "disabled"));
        }
        
        if (enableApiService) {
            if (intent.hasExtra("apiType")) {
                apiType = intent.getStringExtra("apiType");
            }
            if (intent.hasExtra("apiHeaders")) {
                try {
                    String headersJson = intent.getStringExtra("apiHeaders");
                    if (headersJson != null) {
                        JSONObject headersObj = new JSONObject(headersJson);
                        apiHeaders = new HashMap<>();
                        Iterator<String> keys = headersObj.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            apiHeaders.put(key, headersObj.getString(key));
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing API headers", e);
                }
            }
            if (intent.hasExtra("apiAdditionalParams")) {
                try {
                    String paramsJson = intent.getStringExtra("apiAdditionalParams");
                    if (paramsJson != null && !paramsJson.isEmpty()) {
                        apiAdditionalParams = new JSONObject(paramsJson);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing API additional params", e);
                }
            }
            if (intent.hasExtra("apiInterval")) {
                apiIntervalMinutes = intent.getIntExtra("apiInterval", 5);
            }
            
            // Configure API service
            configureApiService();
        }
    }

    private int convertPriority(String priorityStr) {
        if (priorityStr == null) return Priority.PRIORITY_HIGH_ACCURACY;
        
        switch (priorityStr) {
            case "HIGH_ACCURACY":
                return Priority.PRIORITY_HIGH_ACCURACY;
            case "BALANCED_POWER":
                return Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            case "LOW_POWER":
                return Priority.PRIORITY_LOW_POWER;
            case "NO_POWER":
                return Priority.PRIORITY_PASSIVE;
            default:
                return Priority.PRIORITY_HIGH_ACCURACY;
        }
    }

    private void configureApiService() {
        if (enableApiService && apiUrl != null && !apiUrl.isEmpty()) {
            apiService.configure(apiUrl, apiType, apiHeaders, apiAdditionalParams, apiIntervalMinutes);
            Log.d(TAG, "API Service configured successfully");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Continuous location tracking");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(getNotificationIcon())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    private int getNotificationIcon() {
        // Try custom icon first if provided
        if (notificationIcon != null && !notificationIcon.isEmpty()) {
            int customIconResId = getResources().getIdentifier(
                notificationIcon, 
                "drawable", 
                getPackageName()
            );
            
            if (customIconResId != 0) {
                Log.d(TAG, "Using custom notification icon: " + notificationIcon);
                return customIconResId;
            } else {
                Log.w(TAG, "Custom icon not found: " + notificationIcon + ", falling back to app icon");
            }
        }
        
        // Try to get application icon
        try {
            android.content.pm.ApplicationInfo appInfo = getPackageManager().getApplicationInfo(
                getPackageName(), 
                android.content.pm.PackageManager.GET_META_DATA
            );
            
            if (appInfo.icon != 0) {
                Log.d(TAG, "Using application icon for notification");
                return appInfo.icon;
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not get application info", e);
        }
        
        // Final fallback to system location icon
        Log.d(TAG, "Using system default location icon");
        return android.R.drawable.ic_menu_mylocation;
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    processLocationUpdate(location);
                }
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                Log.d(TAG, "Location availability: " + locationAvailability.isLocationAvailable());
                if (!locationAvailability.isLocationAvailable()) {
                    broadcastServiceStatus(false, "Location not available");
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Location permissions not granted");
            broadcastServiceStatus(false, "Location permissions not granted");
            return;
        }

        createLocationRequest();

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            );
            isLocationUpdatesActive = true;
            
            // Start API service if configured
            if (enableApiService) {
                apiService.startApiService();
                Log.d(TAG, "API Service started");
            }
            
            broadcastServiceStatus(true, null);
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when requesting location updates", e);
            broadcastServiceStatus(false, "Security exception: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isLocationUpdatesActive = false;
            
            // Stop API service
            if (apiService != null) {
                apiService.stopApiService();
                Log.d(TAG, "API Service stopped");
            }
            
            broadcastServiceStatus(false, null);
            Log.d(TAG, "Location updates stopped");
        }
    }

    private void createLocationRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use new Builder API (API 31+)
            LocationRequest.Builder builder = new LocationRequest.Builder(priority, updateInterval)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(fastestInterval)
                .setMaxUpdateDelayMillis(updateInterval * 2);
            
            // Add distance filter if specified
            if (distanceFilter > 0) {
                builder.setMinUpdateDistanceMeters((float) distanceFilter);
                Log.d(TAG, "Distance filter applied: " + distanceFilter + " meters");
            }
            
            locationRequest = builder.build();
        } else {
            // Use legacy API (API 23+)
            locationRequest = LocationRequest.create()
                .setPriority(priority)
                .setInterval(updateInterval)
                .setFastestInterval(fastestInterval)
                .setMaxWaitTime(updateInterval * 2);
            
            // Add distance filter for legacy API
            if (distanceFilter > 0) {
                locationRequest.setSmallestDisplacement((float) distanceFilter);
                Log.d(TAG, "Distance filter applied (legacy): " + distanceFilter + " meters");
            }
        }
    }

    private boolean hasLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, 
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void processLocationUpdate(Location location) {
        Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());

        // Create ISO 8601 timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date(location.getTime()));

        // ALWAYS broadcast to Ionic app (regardless of API configuration)
        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        intent.putExtra("accuracy", (double) location.getAccuracy());
        intent.putExtra("timestamp", timestamp);

        if (location.hasAltitude()) {
            intent.putExtra("altitude", location.getAltitude());
        }
        if (location.hasBearing()) {
            intent.putExtra("bearing", (double) location.getBearing());
        }
        if (location.hasSpeed()) {
            intent.putExtra("speed", (double) location.getSpeed());
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        // ADDITIONALLY send to API service if enabled
        if (enableApiService && apiService != null) {
            try {
                JSONObject locationData = new JSONObject();
                locationData.put("latitude", location.getLatitude());
                locationData.put("longitude", location.getLongitude());
                locationData.put("accuracy", (double) location.getAccuracy());
                locationData.put("timestamp", timestamp);

                if (location.hasAltitude()) {
                    locationData.put("altitude", location.getAltitude());
                }
                if (location.hasBearing()) {
                    locationData.put("bearing", (double) location.getBearing());
                }
                if (location.hasSpeed()) {
                    locationData.put("speed", (double) location.getSpeed());
                }

                apiService.addLocationData(locationData);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating location data JSON for API", e);
            }
        }

        // Update notification with current location
        updateNotificationWithLocation(location);
    }

    private void updateNotificationWithLocation(Location location) {
        // String updatedText = String.format(Locale.US, 
        //     "Lat: %.6f, Lng: %.6f", 
        //     location.getLatitude(), 
        //     location.getLongitude()
        // );

        Notification updatedNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setSmallIcon(getNotificationIcon())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();

        notificationManager.notify(NOTIFICATION_ID, updatedNotification);
    }

    private void broadcastServiceStatus(boolean isRunning, String error) {
        Intent intent = new Intent(ACTION_SERVICE_STATUS);
        intent.putExtra(EXTRA_SERVICE_STATUS, isRunning);
        if (error != null) {
            intent.putExtra(EXTRA_ERROR_MESSAGE, error);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Public methods for service control
    public boolean isLocationUpdatesActive() {
        return isLocationUpdatesActive;
    }

    public int getApiBufferSize() {
        return apiService != null ? apiService.getBufferSize() : 0;
    }

    public void clearApiBuffers() {
        if (apiService != null) {
            apiService.clearBuffers();
        }
    }

    public boolean isApiServiceEnabled() {
        return enableApiService;
    }

    public boolean isApiHealthy() {
        return apiService != null ? apiService.isApiHealthy() : false;
    }

    public void resetApiCircuitBreaker() {
        if (apiService != null) {
            apiService.resetCircuitBreaker();
        }
    }

    public void updateServiceConfiguration(String title, String text, String icon, long interval, long fastest, int priority, long distanceFilter) {
        this.notificationTitle = title;
        this.notificationText = text;
        this.notificationIcon = icon;
        this.updateInterval = interval;
        this.fastestInterval = fastest;
        this.priority = priority;
        this.distanceFilter = distanceFilter;

        // Restart location updates with new configuration
        stopLocationUpdates();
        startLocationUpdates();

        // Update notification
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }
}
