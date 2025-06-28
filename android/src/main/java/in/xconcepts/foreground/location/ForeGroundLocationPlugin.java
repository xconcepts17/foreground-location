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

    @PluginMethod
    public void startForegroundLocationService(PluginCall call) {
        if (getLocationPermissionState() != PermissionState.GRANTED) {
            call.reject("Location permission required");
            return;
        }

        JSObject options = call.getObject("options", new JSObject());
        JSObject notification = options.getJSObject("notification");
        
        if (notification == null) {
            call.reject("Notification configuration is required");
            return;
        }

        Intent serviceIntent = new Intent(getContext(), LocationForegroundService.class);
        
        // Add configuration to intent
        serviceIntent.putExtra("notificationTitle", notification.getString("title", "Location Tracking"));
        serviceIntent.putExtra("notificationText", notification.getString("text", "Tracking location"));
        serviceIntent.putExtra("updateInterval", options.getLong("interval", 60000L));
        serviceIntent.putExtra("fastestInterval", options.getLong("fastestInterval", 30000L));
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

        JSObject options = call.getObject("options", new JSObject());
        JSObject notification = options.getJSObject("notification");
        
        String title = notification != null ? notification.getString("title", "Location Tracking") : "Location Tracking";
        String text = notification != null ? notification.getString("text", "Tracking location") : "Tracking location";
        long interval = options.getLong("interval", 60000L);
        long fastestInterval = options.getLong("fastestInterval", 30000L);
        String priorityStr = options.getString("priority", "HIGH_ACCURACY");
        
        int priority = convertPriority(priorityStr);
        
        locationService.updateServiceConfiguration(title, text, interval, fastestInterval, priority);
        call.resolve();
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
