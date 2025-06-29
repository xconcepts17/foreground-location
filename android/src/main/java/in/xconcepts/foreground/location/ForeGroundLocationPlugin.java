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
        
        // Register broadcast receivers
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
            locationUpdateReceiver, 
            new IntentFilter(LocationForegroundService.ACTION_LOCATION_UPDATE)
        );
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
            serviceStatusReceiver, 
            new IntentFilter(LocationForegroundService.ACTION_SERVICE_STATUS)
        );
    }

    @Override
    protected void handleOnDestroy() {
        // Unregister broadcast receivers
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(locationUpdateReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(serviceStatusReceiver);
        
        // Unbind service
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
        
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
     * Expected format (parameters sent directly):
     * {
     *   "notification": {
     *     "title": "Location Tracking Active",    // REQUIRED
     *     "text": "Recording your route...",      // REQUIRED  
     *     "icon": "ic_location"                   // OPTIONAL - drawable resource name
     *   },
     *   "interval": 60000,                        // OPTIONAL - update interval in ms
     *   "fastestInterval": 30000,                 // OPTIONAL - fastest interval in ms
     *   "priority": "HIGH_ACCURACY"               // OPTIONAL - location priority
     * }
     * 
     * Alternative format (with options wrapper - for backward compatibility):
     * {
     *   "options": {
     *     "notification": { ... },
     *     "interval": 60000,
     *     ...
     *   }
     * }
     * 
     * Common issues:
     * - Missing notification.title or notification.text
     * - Empty strings for title or text
     * - Custom icon not found in drawable resources
     * - Permissions not granted before calling this method
     */
    @PluginMethod
    public void startForegroundLocationService(PluginCall call) {
        if (getLocationPermissionState() != PermissionState.GRANTED) {
            call.reject("Location permission required");
            return;
        }

        // Get parameters directly from call data (not wrapped in options object)
        JSObject options;
        try {
            // Try to get options object first (for backward compatibility)
            options = call.getObject("options");
            if (options == null) {
                // If no options object, use call data directly
                options = call.getData();
            }
        } catch (Exception e) {
            // If getting options fails, use call data directly
            options = call.getData();
            Log.d(TAG, "Using call data directly instead of options object");
        }
        
        // Use comprehensive validation
        if (!validateAndExtractNotification(call, options)) {
            return; // Error already sent in validation method
        }
        
        JSObject notification = options.getJSObject("notification");
        String title = notification.getString("title");
        String text = notification.getString("text");

        Intent serviceIntent = new Intent(getContext(), LocationForegroundService.class);
        
        // Add configuration to intent using validated values
        serviceIntent.putExtra("notificationTitle", title);
        serviceIntent.putExtra("notificationText", text);
        serviceIntent.putExtra("notificationIcon", notification.getString("icon")); // Can be null
        
        // Get interval values safely
        long updateInterval = 60000L; // Default 60 seconds
        if (options.has("interval")) {
            try {
                updateInterval = options.getLong("interval");
            } catch (Exception e) {
                Log.w(TAG, "Invalid interval value, using default", e);
            }
        }
        
        long fastestInterval = 30000L; // Default 30 seconds
        if (options.has("fastestInterval")) {
            try {
                fastestInterval = options.getLong("fastestInterval");
            } catch (Exception e) {
                Log.w(TAG, "Invalid fastestInterval value, using default", e);
            }
        }
        
        serviceIntent.putExtra("updateInterval", updateInterval);
        serviceIntent.putExtra("fastestInterval", fastestInterval);
        serviceIntent.putExtra("priority", options.getString("priority", "HIGH_ACCURACY"));

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
            call.reject("Location service is not running");
            return;
        }

        // Get parameters directly from call data (support both options object and direct parameters)
        JSObject options;
        try {
            options = call.getObject("options");
            if (options == null) {
                options = call.getData();
            }
        } catch (Exception e) {
            options = call.getData();
        }
        
        JSObject notification = options.getJSObject("notification");
        
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
        
        // Get interval values safely
        long interval = 60000L; // Default 60 seconds
        if (options.has("interval")) {
            try {
                interval = options.getLong("interval");
            } catch (Exception e) {
                Log.w(TAG, "Invalid interval value, using default", e);
            }
        }
        
        long fastestInterval = 30000L; // Default 30 seconds
        if (options.has("fastestInterval")) {
            try {
                fastestInterval = options.getLong("fastestInterval");
            } catch (Exception e) {
                Log.w(TAG, "Invalid fastestInterval value, using default", e);
            }
        }
        
        String priorityStr = options.getString("priority", "HIGH_ACCURACY");
        
        int priority = convertPriority(priorityStr);
        
        locationService.updateServiceConfiguration(title, text, icon, interval, fastestInterval, priority);
        call.resolve();
    }

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

    // Helper method to safely extract notification configuration
    private boolean validateAndExtractNotification(PluginCall call, JSObject options) {
        if (options == null) {
            call.reject("Options parameter is required");
            return false;
        }
        
        JSObject notification = options.getJSObject("notification");
        
        // Debug logging for troubleshooting
        Log.d(TAG, "validateAndExtractNotification called");
        Log.d(TAG, "Options received: " + options.toString());
        logNotificationConfig(notification);
        
        // Enhanced validation for notification configuration
        if (notification == null) {
            call.reject("Notification configuration is required. Please provide notification object with title and text properties.");
            return false;
        }
        
        if (notification.length() == 0) {
            call.reject("Notification configuration cannot be empty. Please provide title and text properties.");
            return false;
        }
        
        // Validate required notification properties
        String title = notification.getString("title");
        String text = notification.getString("text");
        
        if (title == null || title.trim().isEmpty()) {
            call.reject("Notification title is required in notification configuration.");
            return false;
        }
        
        if (text == null || text.trim().isEmpty()) {
            call.reject("Notification text is required in notification configuration.");
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
}
