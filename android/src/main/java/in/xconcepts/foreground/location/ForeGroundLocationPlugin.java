package in.xconcepts.foreground.location;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

@CapacitorPlugin(
    name = "ForeGroundLocation",
    permissions = {
        @Permission(
            alias = "location",
            strings = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }
        ),
        @Permission(
            alias = "backgroundLocation",
            strings = { Manifest.permission.ACCESS_BACKGROUND_LOCATION }
        ),
        @Permission(
            alias = "notifications",
            strings = { Manifest.permission.POST_NOTIFICATIONS }
        )
    }
)
public class ForeGroundLocationPlugin extends Plugin {
    private static final String TAG = "ForeGroundLocationPlugin";
    
    private ForeGroundLocation implementation = new ForeGroundLocation();
    private LocationForegroundService locationService;
    private boolean isServiceBound = false;
    private FusedLocationProviderClient fusedLocationClient;

    // Broadcast receiver for location updates
    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationForegroundService.ACTION_LOCATION_UPDATE.equals(intent.getAction())) {
                JSObject locationData = new JSObject();
                locationData.put("latitude", intent.getDoubleExtra("latitude", 0.0));
                locationData.put("longitude", intent.getDoubleExtra("longitude", 0.0));
                locationData.put("accuracy", intent.getDoubleExtra("accuracy", 0.0));
                locationData.put("timestamp", intent.getStringExtra("timestamp"));
                
                if (intent.hasExtra("altitude")) {
                    locationData.put("altitude", intent.getDoubleExtra("altitude", 0.0));
                }
                if (intent.hasExtra("bearing")) {
                    locationData.put("bearing", intent.getDoubleExtra("bearing", 0.0));
                }
                if (intent.hasExtra("speed")) {
                    locationData.put("speed", intent.getDoubleExtra("speed", 0.0));
                }

                notifyListeners("locationUpdate", locationData);
            }
        }
    };

    // Broadcast receiver for service status
    private BroadcastReceiver serviceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationForegroundService.ACTION_SERVICE_STATUS.equals(intent.getAction())) {
                JSObject statusData = new JSObject();
                statusData.put("isRunning", intent.getBooleanExtra(LocationForegroundService.EXTRA_SERVICE_STATUS, false));
                
                String error = intent.getStringExtra(LocationForegroundService.EXTRA_ERROR_MESSAGE);
                if (error != null) {
                    statusData.put("error", error);
                }

                notifyListeners("serviceStatusChanged", statusData);
            }
        }
    };

    // Service connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationForegroundService.LocationServiceBinder binder = 
                (LocationForegroundService.LocationServiceBinder) service;
            locationService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            locationService = null;
            isServiceBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    public void load() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        
        // Unregister existing receivers first (safety)
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(locationUpdateReceiver);
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(serviceStatusReceiver);
        } catch (Exception e) {
            // Ignore if not registered
            Log.d(TAG, "No existing receivers to unregister");
        }
        
        // Register broadcast receivers
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
            locationUpdateReceiver, 
            new IntentFilter(LocationForegroundService.ACTION_LOCATION_UPDATE)
        );
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
            serviceStatusReceiver, 
            new IntentFilter(LocationForegroundService.ACTION_SERVICE_STATUS)
        );
        
        Log.d(TAG, "Plugin loaded and receivers registered");
    }

    @Override
    protected void handleOnDestroy() {
        // Unregister broadcast receivers safely
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(locationUpdateReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering locationUpdateReceiver", e);
        }
        
        try {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(serviceStatusReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering serviceStatusReceiver", e);
        }
        
        // Unbind service safely
        if (isServiceBound) {
            try {
                getContext().unbindService(serviceConnection);
                isServiceBound = false;
                Log.d(TAG, "Service unbound successfully");
            } catch (Exception e) {
                Log.w(TAG, "Error unbinding service", e);
            }
        }
        
        // Clean up references
        locationService = null;
        fusedLocationClient = null;
        
        super.handleOnDestroy();
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject result = new JSObject();
        
        result.put("location", getLocationPermissionState());
        result.put("backgroundLocation", getBackgroundLocationPermissionState());
        result.put("notifications", getNotificationPermissionState());
        
        call.resolve(result);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        // First request basic location permissions
        if (getLocationPermissionState() != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "locationPermissionCallback");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                   getBackgroundLocationPermissionState() != PermissionState.GRANTED) {
            requestPermissionForAlias("backgroundLocation", call, "backgroundLocationPermissionCallback");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                   getNotificationPermissionState() != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "notificationPermissionCallback");
        } else {
            // All permissions already granted
            checkPermissions(call);
        }
    }

    @PermissionCallback
    private void locationPermissionCallback(PluginCall call) {
        if (getLocationPermissionState() == PermissionState.GRANTED) {
            // Continue with background location if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermissionForAlias("backgroundLocation", call, "backgroundLocationPermissionCallback");
            } else {
                checkPermissions(call);
            }
        } else {
            call.reject("Location permission is required for this feature");
        }
    }

    @PermissionCallback
    private void backgroundLocationPermissionCallback(PluginCall call) {
        // Continue with notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAlias("notifications", call, "notificationPermissionCallback");
        } else {
            checkPermissions(call);
        }
    }

    @PermissionCallback
    private void notificationPermissionCallback(PluginCall call) {
        checkPermissions(call);
    }

    /**
     * Start foreground location service with notification configuration
     * 
     * Expected format (standardized approach):
     * {
     *   "notification": {
     *     "title": "Location Tracking Active",    // REQUIRED
     *     "text": "Recording your route...",      // REQUIRED  
     *     "icon": "ic_location"                   // OPTIONAL - drawable resource name
     *   },
     *   "interval": 60000,                        // OPTIONAL - update interval in ms
     *   "fastestInterval": 30000,                 // OPTIONAL - fastest interval in ms
     *   "priority": "HIGH_ACCURACY",              // OPTIONAL - location priority
     *   "distanceFilter": 0,                      // OPTIONAL - distance filter in meters
     *   "api": {                                  // OPTIONAL - API service configuration
     *     "url": "https://api.example.com/locations",  // REQUIRED if api block present
     *     "type": "POST",                         // OPTIONAL - HTTP method (default: POST)
     *     "header": {                             // OPTIONAL - HTTP headers
     *       "Content-Type": "application/json",
     *       "Authorization": "Bearer token"
     *     },
     *     "additionalParams": {                   // OPTIONAL - additional parameters to send
     *       "userId": "123",
     *       "sessionId": "abc"
     *     },
     *     "apiInterval": 5                        // OPTIONAL - API call interval in minutes (default: 5)
     *   }
     * }
     * 
     * Error codes:
     * - PERMISSION_DENIED: Location permission not granted
     * - INVALID_NOTIFICATION: Missing or invalid notification configuration
     * - INVALID_PARAMETERS: Invalid interval or priority values
     */
    @PluginMethod
    public void startForegroundLocationService(PluginCall call) {
        if (getLocationPermissionState() != PermissionState.GRANTED) {
            call.reject("PERMISSION_DENIED", "Location permission is required");
            return;
        }

        // Validate notification configuration
        JSObject notification = call.getObject("notification");
        if (notification == null) {
            call.reject("INVALID_NOTIFICATION", "notification parameter is required");
            return;
        }

        String title = notification.getString("title");
        String text = notification.getString("text");
        
        if (title == null || title.trim().isEmpty()) {
            call.reject("INVALID_NOTIFICATION", "notification.title is required and cannot be empty");
            return;
        }
        
        if (text == null || text.trim().isEmpty()) {
            call.reject("INVALID_NOTIFICATION", "notification.text is required and cannot be empty");
            return;
        }

        Intent serviceIntent = new Intent(getContext(), LocationForegroundService.class);
        
        // Add notification configuration to intent
        serviceIntent.putExtra("notificationTitle", title);
        serviceIntent.putExtra("notificationText", text);
        serviceIntent.putExtra("notificationIcon", notification.getString("icon")); // Can be null
        
        // Get and validate interval values
        long updateInterval = call.getLong("interval", 60000L); // Default 60 seconds
        long fastestInterval = call.getLong("fastestInterval", 30000L); // Default 30 seconds
        
        // Validate intervals
        if (updateInterval < 1000L) {
            call.reject("INVALID_PARAMETERS", "interval must be at least 1000ms");
            return;
        }
        
        if (fastestInterval < 1000L) {
            call.reject("INVALID_PARAMETERS", "fastestInterval must be at least 1000ms");
            return;
        }
        
        if (fastestInterval > updateInterval) {
            call.reject("INVALID_PARAMETERS", "fastestInterval cannot be greater than interval");
            return;
        }
        
        String priority = call.getString("priority", "HIGH_ACCURACY");
        if (!isValidPriority(priority)) {
            call.reject("INVALID_PARAMETERS", "priority must be one of: HIGH_ACCURACY, BALANCED_POWER, LOW_POWER, NO_POWER");
            return;
        }
        
        // Get and validate distance filter
        long distanceFilter = call.getLong("distanceFilter", 0L); // Default 0 = no filter
        if (distanceFilter < 0L) {
            call.reject("INVALID_PARAMETERS", "distanceFilter must be 0 or greater (0 = no filter)");
            return;
        }
        
        serviceIntent.putExtra("updateInterval", updateInterval);
        serviceIntent.putExtra("fastestInterval", fastestInterval);
        serviceIntent.putExtra("priority", priority);
        serviceIntent.putExtra("distanceFilter", distanceFilter);

        // Extract API configuration if provided
        JSObject apiConfig = call.getObject("api");
        if (apiConfig != null) {
            String apiUrl = apiConfig.getString("url");
            if (apiUrl != null && !apiUrl.trim().isEmpty()) {
                serviceIntent.putExtra("apiUrl", apiUrl.trim());
                serviceIntent.putExtra("apiType", apiConfig.getString("type", "POST"));
                
                JSObject headers = apiConfig.getJSObject("header");
                if (headers != null) {
                    serviceIntent.putExtra("apiHeaders", headers.toString());
                }
                
                JSObject additionalParams = apiConfig.getJSObject("additionalParams");
                if (additionalParams != null) {
                    serviceIntent.putExtra("apiAdditionalParams", additionalParams.toString());
                }
                
                Integer apiInterval = apiConfig.getInteger("apiInterval", 5);
                if (apiInterval != null && apiInterval > 0) {
                    serviceIntent.putExtra("apiInterval", apiInterval);
                } else {
                    serviceIntent.putExtra("apiInterval", 5); // Default 5 minutes
                }
                
                Log.d(TAG, "API configuration added to service intent");
            }
        }

        // Start foreground service
        ContextCompat.startForegroundService(getContext(), serviceIntent);
        
        // Bind to service
        getContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        call.resolve();
    }

    @PluginMethod
    public void stopForegroundLocationService(PluginCall call) {
        Intent serviceIntent = new Intent(getContext(), LocationForegroundService.class);
        getContext().stopService(serviceIntent);
        
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }

        call.resolve();
    }

    @PluginMethod
    public void isServiceRunning(PluginCall call) {
        JSObject result = new JSObject();
        result.put("isRunning", isServiceBound && locationService != null && 
                   locationService.isLocationUpdatesActive());
        call.resolve(result);
    }

    @PluginMethod
    public void getCurrentLocation(PluginCall call) {
        if (getLocationPermissionState() != PermissionState.GRANTED) {
            call.reject("Location permission required");
            return;
        }

        implementation.getCurrentLocation(getContext(), new ForeGroundLocation.LocationCallback() {
            @Override
            public void onLocationResult(Location location) {
                JSObject result = new JSObject();
                result.put("latitude", location.getLatitude());
                result.put("longitude", location.getLongitude());
                result.put("accuracy", (double) location.getAccuracy());
                result.put("timestamp", implementation.formatTimestamp(location.getTime()));
                
                if (location.hasAltitude()) {
                    result.put("altitude", location.getAltitude());
                }
                if (location.hasBearing()) {
                    result.put("bearing", (double) location.getBearing());
                }
                if (location.hasSpeed()) {
                    result.put("speed", (double) location.getSpeed());
                }

                call.resolve(result);
            }

            @Override
            public void onLocationError(String error) {
                call.reject("Location error: " + error);
            }
        });
    }

    @PluginMethod
    public void updateLocationSettings(PluginCall call) {
        if (!isServiceBound || locationService == null) {
            call.reject("SERVICE_NOT_RUNNING", "Location service is not running");
            return;
        }

        // Get notification configuration directly from call
        JSObject notification = call.getObject("notification");
        
        // Enhanced validation for notification configuration in updates
        String title = "Location Tracking"; // Default
        String text = "Tracking location"; // Default
        String icon = null;
        
        if (notification != null) {
            // Use provided values if valid, otherwise keep defaults
            String providedTitle = notification.getString("title");
            String providedText = notification.getString("text");
            
            if (providedTitle != null && !providedTitle.trim().isEmpty()) {
                title = providedTitle;
            }
            
            if (providedText != null && !providedText.trim().isEmpty()) {
                text = providedText;
            }
            
            icon = notification.getString("icon");
        }
        
        Log.d(TAG, "Updating service configuration - Title: " + title + ", Text: " + text);
        
        // Get interval values directly from call
        long interval = call.getLong("interval", 60000L); // Default 60 seconds
        long fastestInterval = call.getLong("fastestInterval", 30000L); // Default 30 seconds
        
        // Validate intervals
        if (interval < 1000L) {
            call.reject("INVALID_PARAMETERS", "interval must be at least 1000ms");
            return;
        }
        
        if (fastestInterval < 1000L) {
            call.reject("INVALID_PARAMETERS", "fastestInterval must be at least 1000ms");
            return;
        }
        
        String priorityStr = call.getString("priority", "HIGH_ACCURACY");
        
        if (!isValidPriority(priorityStr)) {
            call.reject("INVALID_PARAMETERS", "priority must be one of: HIGH_ACCURACY, BALANCED_POWER, LOW_POWER, NO_POWER");
            return;
        }
        
        int priority = convertPriority(priorityStr);
        
        // Get distance filter parameter
        long distanceFilter = call.getLong("distanceFilter", 0L); // Default 0 = no filter
        if (distanceFilter < 0L) {
            call.reject("INVALID_PARAMETERS", "distanceFilter must be 0 or greater (0 = no filter)");
            return;
        }
        
        locationService.updateServiceConfiguration(title, text, icon, interval, fastestInterval, priority, distanceFilter);
        call.resolve();
    }

    @PluginMethod
    public void getApiServiceStatus(PluginCall call) {
        if (!isServiceBound || locationService == null) {
            call.reject("SERVICE_NOT_RUNNING", "Location service is not running");
            return;
        }

        JSObject result = new JSObject();
        result.put("isEnabled", locationService.isApiServiceEnabled());
        result.put("bufferSize", locationService.getApiBufferSize());
        result.put("isHealthy", locationService.isApiHealthy());
        
        call.resolve(result);
    }

    @PluginMethod
    public void clearApiBuffers(PluginCall call) {
        if (!isServiceBound || locationService == null) {
            call.reject("SERVICE_NOT_RUNNING", "Location service is not running");
            return;
        }

        locationService.clearApiBuffers();
        call.resolve();
    }

    @PluginMethod
    public void resetApiCircuitBreaker(PluginCall call) {
        if (!isServiceBound || locationService == null) {
            call.reject("SERVICE_NOT_RUNNING", "Location service is not running");
            return;
        }

        locationService.resetApiCircuitBreaker();
        call.resolve();
    }

    // Helper methods

    // Helper method to debug notification configuration
    private void logNotificationConfig(JSObject notification) {
        if (notification == null) {
            Log.d(TAG, "Notification config is null");
            return;
        }
        
        Log.d(TAG, "Notification config received:");
        Log.d(TAG, "  - Length: " + notification.length());
        Log.d(TAG, "  - Keys: " + notification.keys());
        
        if (notification.has("title")) {
            Log.d(TAG, "  - Title: '" + notification.getString("title") + "'");
        } else {
            Log.d(TAG, "  - Title: NOT PROVIDED");
        }
        
        if (notification.has("text")) {
            Log.d(TAG, "  - Text: '" + notification.getString("text") + "'");
        } else {
            Log.d(TAG, "  - Text: NOT PROVIDED");
        }
        
        if (notification.has("icon")) {
            Log.d(TAG, "  - Icon: '" + notification.getString("icon") + "'");
        } else {
            Log.d(TAG, "  - Icon: NOT PROVIDED");
        }
    }

    // Helper method to safely extract and validate notification configuration
    private boolean validateAndExtractNotification(PluginCall call) {
        JSObject notification = call.getObject("notification");
        
        // Debug logging for troubleshooting
        Log.d(TAG, "validateAndExtractNotification called");
        Log.d(TAG, "Call data received: " + call.getData().toString());
        logNotificationConfig(notification);
        
        // Enhanced validation for notification configuration
        if (notification == null) {
            call.reject("INVALID_NOTIFICATION", "Notification configuration is required. Please provide notification object with title and text properties.");
            return false;
        }
        
        // Validate required notification properties
        String title = notification.getString("title");
        String text = notification.getString("text");
        
        if (title == null || title.trim().isEmpty()) {
            call.reject("INVALID_NOTIFICATION", "Notification title is required in notification configuration.");
            return false;
        }
        
        if (text == null || text.trim().isEmpty()) {
            call.reject("INVALID_NOTIFICATION", "Notification text is required in notification configuration.");
            return false;
        }
        
        Log.d(TAG, "Notification configuration validated successfully - Title: " + title + ", Text: " + text);
        return true;
    }

    private PermissionState getLocationPermissionState() {
        return getPermissionState("location");
    }

    private PermissionState getBackgroundLocationPermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return getPermissionState("backgroundLocation");
        }
        return PermissionState.GRANTED; // Not required on older versions
    }

    private PermissionState getNotificationPermissionState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getPermissionState("notifications");
        }
        return PermissionState.GRANTED; // Not required on older versions
    }

    private int convertPriority(String priorityStr) {
        switch (priorityStr) {
            case "HIGH_ACCURACY":
                return com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;
            case "BALANCED_POWER":
                return com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            case "LOW_POWER":
                return com.google.android.gms.location.Priority.PRIORITY_LOW_POWER;
            case "NO_POWER":
                return com.google.android.gms.location.Priority.PRIORITY_PASSIVE;
            default:
                return com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;
        }
    }

    private boolean isValidPriority(String priority) {
        return priority != null && (
            priority.equals("HIGH_ACCURACY") ||
            priority.equals("BALANCED_POWER") ||
            priority.equals("LOW_POWER") ||
            priority.equals("NO_POWER")
        );
    }
}
