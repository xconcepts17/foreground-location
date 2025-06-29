# Setup and Examples - Foreground Location Plugin

## 🚀 Quick Setup Guide

### Step 1: Install the Plugin

```bash
npm install foreground-location
npx cap sync
```

### Step 2: Update Android Manifest

Add to your `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Location permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

    <!-- Foreground service permissions -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <!-- Notification permission for Android 13+ -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <!-- Background location permission for Android 10+ -->
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Google Play Services -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application>
        <!-- Your existing application content -->

        <!-- Add this service declaration -->
        <service
            android:name="in.xconcepts.foreground.location.LocationForegroundService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="location" />
    </application>

</manifest>
```

### Step 3: Update build.gradle

Add to your `android/app/build.gradle` dependencies:

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    // ... other dependencies
}
```

### Step 4: Basic Implementation

```typescript
import { ForeGroundLocation } from 'foreground-location';

// Request permissions
const permissions = await ForeGroundLocation.requestPermissions();

if (permissions.location === 'granted') {
  // Start location service
  await ForeGroundLocation.startForegroundLocationService({
    interval: 60000,
    priority: 'HIGH_ACCURACY',
    notification: {
      title: 'Location Tracking',
      text: 'Tracking your location',
    },
  });
}
```

## 📖 Complete Usage Example

```typescript
import {
  ForeGroundLocation,
  PluginListenerHandle,
  LocationResult,
  ServiceStatus,
  LocationPermissionStatus,
} from 'foreground-location';

/**
 * Complete example of the Foreground Location Plugin
 * Demonstrates the complete workflow for Android location tracking
 */

class LocationTracker {
  private isTracking = false;
  private locationListener: PluginListenerHandle | null = null;
  private statusListener: PluginListenerHandle | null = null;

  /**
   * Initialize and start location tracking
   */
  async startLocationTracking() {
    try {
      console.log('Starting location tracking...');

      // Step 1: Check current permissions
      const permissions = await ForeGroundLocation.checkPermissions();
      console.log('Current permissions:', permissions);

      // Step 2: Request permissions if needed
      if (permissions.location !== 'granted') {
        console.log('Requesting permissions...');
        const requestResult = await ForeGroundLocation.requestPermissions();

        if (requestResult.location !== 'granted') {
          throw new Error('Location permission is required');
        }
      }

      // Step 3: Set up event listeners
      this.setupListeners();

      // Step 4: Start the foreground service
      await ForeGroundLocation.startForegroundLocationService({
        interval: 60000, // Update every minute
        fastestInterval: 30000, // Fastest update every 30 seconds
        priority: 'HIGH_ACCURACY',
        notification: {
          title: 'Location Tracking Active',
          text: 'Tracking your location for better service',
          icon: 'ic_location_tracking', // Optional custom icon
        },
        enableHighAccuracy: true,
        distanceFilter: 10, // Minimum 10 meters movement
      });

      this.isTracking = true;
      console.log('✅ Location tracking started successfully');
    } catch (error) {
      console.error('❌ Failed to start location tracking:', error);
      throw error;
    }
  }

  /**
   * Set up event listeners for location updates and service status
   */
  private async setupListeners() {
    // Listen for location updates
    this.locationListener = await ForeGroundLocation.addListener('locationUpdate', (location: LocationResult) => {
      console.log('📍 New location update:', {
        latitude: location.latitude,
        longitude: location.longitude,
        accuracy: location.accuracy,
        timestamp: new Date(location.timestamp),
        speed: location.speed,
        bearing: location.bearing,
      });

      // Handle location update
      this.handleLocationUpdate(location);
    });

    // Listen for service status changes
    this.statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status: ServiceStatus) => {
      console.log('🔄 Service status changed:', status);

      if (status.isRunning) {
        console.log('✅ Location service is running');
      } else {
        console.log('⏹️ Location service stopped');
      }

      if (status.error) {
        console.error('❌ Service error:', status.error);
        this.handleServiceError(status.error);
      }
    });
  }

  /**
   * Handle incoming location updates
   */
  private handleLocationUpdate(location: LocationResult) {
    // Update UI
    this.updateLocationDisplay(location);

    // Send to server
    this.sendLocationToServer(location);

    // Update local storage
    this.saveLocationLocally(location);
  }

  /**
   * Handle service errors
   */
  private handleServiceError(error: string) {
    console.error('Service error:', error);

    // Show user notification
    this.showErrorNotification(error);

    // Attempt to restart if needed
    if (error.includes('permission')) {
      this.handlePermissionError();
    }
  }

  /**
   * Get current location without starting service
   */
  async getCurrentLocation(): Promise<LocationResult | null> {
    try {
      const location = await ForeGroundLocation.getCurrentLocation();
      console.log('📍 Current location:', location);
      return location;
    } catch (error) {
      console.error('❌ Failed to get current location:', error);
      return null;
    }
  }

  /**
   * Check if service is running
   */
  async checkServiceStatus(): Promise<boolean> {
    try {
      const status = await ForeGroundLocation.isServiceRunning();
      console.log('Service status:', status.isRunning);
      return status.isRunning;
    } catch (error) {
      console.error('Failed to check service status:', error);
      return false;
    }
  }

  /**
   * Update location service settings
   */
  async updateLocationSettings(newSettings: Partial<LocationServiceOptions>) {
    try {
      await ForeGroundLocation.updateLocationSettings({
        interval: newSettings.interval || 60000,
        priority: newSettings.priority || 'HIGH_ACCURACY',
        notification: {
          title: 'Location Settings Updated',
          text: 'Location tracking with new settings',
          icon: newSettings.notification?.icon,
        },
      });

      console.log('✅ Location settings updated');
    } catch (error) {
      console.error('❌ Failed to update settings:', error);
    }
  }

  /**
   * Stop location tracking
   */
  async stopLocationTracking() {
    try {
      console.log('Stopping location tracking...');

      // Stop the service
      await ForeGroundLocation.stopForegroundLocationService();

      // Remove listeners
      if (this.locationListener) {
        this.locationListener.remove();
        this.locationListener = null;
      }

      if (this.statusListener) {
        this.statusListener.remove();
        this.statusListener = null;
      }

      this.isTracking = false;
      console.log('✅ Location tracking stopped successfully');
    } catch (error) {
      console.error('❌ Failed to stop location tracking:', error);
      throw error;
    }
  }

  /**
   * Check and request all required permissions
   */
  async checkAndRequestPermissions(): Promise<LocationPermissionStatus> {
    try {
      // Check current permissions
      const current = await ForeGroundLocation.checkPermissions();
      console.log('Current permissions:', current);

      // Request if needed
      if (
        current.location !== 'granted' ||
        current.backgroundLocation !== 'granted' ||
        current.notifications !== 'granted'
      ) {
        console.log('Requesting permissions...');
        const result = await ForeGroundLocation.requestPermissions();
        console.log('Permission result:', result);
        return result;
      }

      return current;
    } catch (error) {
      console.error('Permission error:', error);
      throw error;
    }
  }

  // Helper methods
  private updateLocationDisplay(location: LocationResult) {
    // Update your UI with new location
    console.log('Updating UI with location:', location);
  }

  private async sendLocationToServer(location: LocationResult) {
    // Send location to your backend
    console.log('Sending location to server:', location);
  }

  private saveLocationLocally(location: LocationResult) {
    // Save to local storage
    localStorage.setItem('lastLocation', JSON.stringify(location));
  }

  private showErrorNotification(error: string) {
    // Show error to user
    console.error('Showing error to user:', error);
  }

  private handlePermissionError() {
    // Guide user to settings
    console.log('Handling permission error');
  }

  // Getters
  get isLocationTracking(): boolean {
    return this.isTracking;
  }
}

// Export for use in your app
export default LocationTracker;

// Usage in your component/service
const locationTracker = new LocationTracker();

// Start tracking
await locationTracker.startLocationTracking();

// Get current location
const location = await locationTracker.getCurrentLocation();

// Stop tracking
await locationTracker.stopLocationTracking();
```

## 🔔 Notification Icon Configuration

### Icon Priority and Fallback

The plugin uses a smart icon selection system with automatic fallback:

1. **Custom Icon** → 2. **Application Icon** → 3. **System Location Icon**

### Using Application Icon (Default)

```typescript
// This will use your app's icon automatically
await ForeGroundLocation.startForegroundLocationService({
  notification: {
    title: 'Location Tracking',
    text: 'Tracking your location',
    // No icon specified = uses app icon
  },
});
```

### Using Custom Icon

```typescript
// This will use your custom icon
await ForeGroundLocation.startForegroundLocationService({
  notification: {
    title: 'Location Tracking',
    text: 'Tracking your location',
    icon: 'ic_location_custom', // Your custom icon in res/drawable/
  },
});
```

### Custom Icon Setup

#### Step 1: Add Icon Resource

Create your icon file in: `android/app/src/main/res/drawable/ic_location_tracking.xml`

Example vector drawable:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnPrimary">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M12,2C8.13,2 5,5.13 5,9c0,5.25 7,13 7,13s7,-7.75 7,-13c0,-3.87 -3.13,-7 -7,-7zM12,11.5c-1.38,0 -2.5,-1.12 -2.5,-2.5s1.12,-2.5 2.5,-2.5 2.5,1.12 2.5,2.5 -1.12,2.5 -2.5,2.5z"/>
</vector>
```

#### Step 2: Use in Plugin Configuration

```typescript
await ForeGroundLocation.startForegroundLocationService({
  notification: {
    title: 'Location Tracking',
    text: 'Tracking your location',
    icon: 'ic_location_tracking', // Name without file extension
  },
});
```

### Icon Requirements

- **Format:** Vector drawable (XML) recommended, PNG supported
- **Size:** 24x24dp for vector, multiple densities for PNG
- **Color:** Monochrome (white/transparent) for best results with system themes
- **Location:** `android/app/src/main/res/drawable/`

## 🐛 Troubleshooting

### TypeScript Import Issues

If you encounter TypeScript import errors:

```typescript
// ❌ Wrong import - old package name
import { ForeGroundLocation } from 'foreground-location-plugin';

// ✅ Correct import
import { ForeGroundLocation, PluginListenerHandle, LocationResult } from 'foreground-location';
```

### Common Issues

1. **Service not starting**
   - Check all required permissions are granted
   - Verify Android manifest configuration
   - Ensure Google Play Services is available

2. **No location updates**
   - Check device location settings are enabled
   - Verify location permissions are granted
   - Check if device has GPS capability

3. **High battery usage**
   - Increase update intervals
   - Use lower accuracy priority
   - Implement distance filter

4. **Permission denied**
   - Request permissions in correct order
   - Handle permission rationale
   - Guide users to app settings if needed

## 🔧 Advanced Configuration

### Battery Optimized Settings

```typescript
const batteryOptimizedConfig = {
  interval: 300000, // 5 minutes
  fastestInterval: 120000, // 2 minutes
  priority: 'BALANCED_POWER',
  distanceFilter: 50, // 50 meters
  notification: {
    title: 'Background Tracking',
    text: 'Battery optimized location tracking',
  },
};
```

### High Accuracy Settings

```typescript
const highAccuracyConfig = {
  interval: 15000, // 15 seconds
  fastestInterval: 5000, // 5 seconds
  priority: 'HIGH_ACCURACY',
  distanceFilter: 1, // 1 meter
  enableHighAccuracy: true,
  notification: {
    title: 'Precise Tracking',
    text: 'High accuracy location tracking active',
  },
};
```

## 📱 Platform Compatibility

- ✅ **Android 8.0+**: Full support with foreground service
- ❌ **iOS**: Service not available (use iOS background location modes)
- ❌ **Web**: Service not available (basic getCurrentLocation only)

## 🔐 Security Considerations

- Always request permissions before starting service
- Validate location data before processing
- Implement proper error handling
- Follow platform security guidelines
- Document privacy policy for location usage
