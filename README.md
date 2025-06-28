# Foreground Location Plugin

A Capacitor plugin for continuous location tracking using Android's Foreground Service with FusedLocationProviderClient. This plugin is specifically designed for Android applications that require persistent location updates even when the app is in the background.

## Platform Support

- ✅ **Android**: Full implementation with Foreground Service
- ❌ **iOS**: Service not available (use iOS background location modes)
- ❌ **Web**: Service not available (basic getCurrentLocation only)

## Features

- 🚀 **Foreground Service**: Persistent location tracking on Android
- 🔋 **Battery Optimized**: Uses FusedLocationProviderClient for efficiency
- 🔔 **Persistent Notification**: Required notification for foreground service
- 🎯 **High Accuracy**: Configurable location accuracy priorities
- 📍 **Real-time Updates**: Live location updates via event listeners
- 🛡️ **Permission Management**: Comprehensive permission handling
- ⚙️ **Configurable**: Customizable update intervals and notification

## Installation

```bash
npm install foreground-location-plugin
npx cap sync
```

## Android Configuration

### 1. Update your Android Manifest

Add the following permissions and service declaration to your app's `android/app/src/main/AndroidManifest.xml`:

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

### 2. Google Play Services

Ensure your app includes Google Play Services Location dependency. Add to `android/app/build.gradle`:

```gradle
dependencies {
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    // ... other dependencies
}
```

### 3. Proguard Rules (if using)

Add to `android/app/proguard-rules.pro`:

```proguard
-keep class com.google.android.gms.location.** { *; }
-keep class in.xconcepts.foreground.location.** { *; }
```

## Usage

### Import the Plugin

```typescript
import { ForeGroundLocation } from 'foreground-location-plugin';
```

### Check and Request Permissions

```typescript
// Check current permission status
const permissions = await ForeGroundLocation.checkPermissions();
console.log('Location permission:', permissions.location);
console.log('Background location:', permissions.backgroundLocation);
console.log('Notifications:', permissions.notifications);

// Request all required permissions
try {
  const result = await ForeGroundLocation.requestPermissions();
  if (result.location === 'granted') {
    console.log('Location permissions granted');
  }
} catch (error) {
  console.error('Permission denied:', error);
}
```

### Start Foreground Location Service

```typescript
const startLocationService = async () => {
  try {
    await ForeGroundLocation.startForegroundLocationService({
      interval: 60000, // Update every 60 seconds
      fastestInterval: 30000, // Fastest update every 30 seconds
      priority: 'HIGH_ACCURACY',
      notification: {
        title: 'Location Tracking',
        text: 'Tracking your location for better service',
        icon: 'ic_location', // Optional custom icon
      },
      enableHighAccuracy: true,
      distanceFilter: 10, // Minimum 10 meters movement
    });

    console.log('Location service started');
  } catch (error) {
    console.error('Failed to start location service:', error);
  }
};
```

### Listen for Location Updates

```typescript
// Subscribe to location updates
const locationListener = await ForeGroundLocation.addListener('locationUpdate', (location) => {
  console.log('New location:', {
    latitude: location.latitude,
    longitude: location.longitude,
    accuracy: location.accuracy,
    timestamp: location.timestamp,
  });

  // Update your UI or send to server
  updateLocationOnMap(location);
});

// Subscribe to service status changes
const statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
  console.log('Service status:', status.isRunning);
  if (status.error) {
    console.error('Service error:', status.error);
  }
});
```

### Get Current Location (One-time)

```typescript
const getCurrentLocation = async () => {
  try {
    const location = await ForeGroundLocation.getCurrentLocation();
    console.log('Current location:', location);
    return location;
  } catch (error) {
    console.error('Failed to get location:', error);
  }
};
```

### Check Service Status

```typescript
const checkService = async () => {
  const status = await ForeGroundLocation.isServiceRunning();
  console.log('Service running:', status.isRunning);
};
```

### Update Service Configuration

```typescript
const updateSettings = async () => {
  await ForeGroundLocation.updateLocationSettings({
    interval: 30000, // Update every 30 seconds
    priority: 'BALANCED_POWER',
    notification: {
      title: 'Updated Location Service',
      text: 'Now tracking with updated settings',
    },
  });
};
```

### Stop Location Service

```typescript
const stopLocationService = async () => {
  try {
    await ForeGroundLocation.stopForegroundLocationService();
    console.log('Location service stopped');

    // Remove listeners
    locationListener.remove();
    statusListener.remove();
  } catch (error) {
    console.error('Failed to stop service:', error);
  }
};
```

## API Reference

### Methods

#### `checkPermissions()`

Returns the current permission status for location, background location, and notifications.

**Returns:** `Promise<LocationPermissionStatus>`

#### `requestPermissions()`

Requests all required permissions from the user.

**Returns:** `Promise<LocationPermissionStatus>`

#### `startForegroundLocationService(options)`

Starts the foreground location service with the specified configuration.

**Parameters:**

- `options: LocationServiceOptions` - Service configuration

**Returns:** `Promise<void>`

#### `stopForegroundLocationService()`

Stops the foreground location service.

**Returns:** `Promise<void>`

#### `isServiceRunning()`

Checks if the location service is currently running.

**Returns:** `Promise<{ isRunning: boolean }>`

#### `getCurrentLocation()`

Gets the current location once without starting the service.

**Returns:** `Promise<LocationResult>`

#### `updateLocationSettings(options)`

Updates the configuration of the running location service.

**Parameters:**

- `options: LocationServiceOptions` - New service configuration

**Returns:** `Promise<void>`

### Interfaces

#### `LocationServiceOptions`

```typescript
interface LocationServiceOptions {
  interval?: number; // Update interval in milliseconds (default: 60000)
  fastestInterval?: number; // Fastest update interval in milliseconds (default: 30000)
  priority?: 'HIGH_ACCURACY' | 'BALANCED_POWER' | 'LOW_POWER' | 'NO_POWER';
  notification: {
    title: string;
    text: string;
    icon?: string;
  };
  enableHighAccuracy?: boolean;
  distanceFilter?: number; // Minimum distance in meters
}
```

#### `LocationResult`

```typescript
interface LocationResult {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number;
  bearing?: number;
  speed?: number;
  timestamp: string; // ISO 8601 format
}
```

#### `LocationPermissionStatus`

```typescript
interface LocationPermissionStatus {
  location: PermissionState; // 'granted' | 'denied' | 'prompt'
  backgroundLocation: PermissionState;
  notifications: PermissionState;
}
```

### Events

#### `locationUpdate`

Fired when a new location update is received.

**Payload:** `LocationResult`

#### `serviceStatusChanged`

Fired when the service status changes.

**Payload:** `{ isRunning: boolean, error?: string }`

## Android Version Compatibility

### Android 14+ (API 34+)

- Requires `FOREGROUND_SERVICE_LOCATION` permission
- Must declare `foregroundServiceType="location"` in manifest
- Enhanced location permission model

### Android 13+ (API 33+)

- Requires `POST_NOTIFICATIONS` permission for foreground service notification
- Runtime notification permission request

### Android 12+ (API 31+)

- Approximate location option available
- Enhanced permission flow

### Android 10+ (API 29+)

- Requires separate `ACCESS_BACKGROUND_LOCATION` permission
- Background location permission must be requested separately

### Android 8.0+ (API 26+)

- Requires notification channels for foreground service
- Background execution limits

## Best Practices

### 1. Permission Handling

```typescript
// Always check permissions before starting service
const permissions = await ForeGroundLocation.checkPermissions();
if (permissions.location !== 'granted') {
  await ForeGroundLocation.requestPermissions();
}
```

### 2. Battery Optimization

```typescript
// Use appropriate intervals based on use case
const config = {
  interval: 60000, // 1 minute for most apps
  priority: 'BALANCED_POWER', // Good balance of accuracy and battery
  distanceFilter: 10, // Only update if moved 10+ meters
};
```

### 3. User Experience

```typescript
// Provide clear notification messages
const notification = {
  title: 'Delivery Tracking',
  text: 'Tracking delivery route for accurate ETAs',
};
```

### 4. Error Handling

```typescript
try {
  await ForeGroundLocation.startForegroundLocationService(config);
} catch (error) {
  if (error.includes('permission')) {
    // Handle permission issues
    showPermissionDialog();
  } else {
    // Handle other errors
    showErrorMessage(error);
  }
}
```

## Troubleshooting

### Common Issues

1. **Service not starting**
   - Check all required permissions are granted
   - Verify manifest configuration
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

### Debug Tips

```typescript
// Add detailed logging
const locationListener = await ForeGroundLocation.addListener('locationUpdate', (location) => {
  console.log('Location update:', {
    lat: location.latitude,
    lng: location.longitude,
    accuracy: location.accuracy,
    time: new Date(location.timestamp),
  });
});

// Monitor service status
const statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
  console.log('Service status changed:', status);
});
```

## Example Implementation

```typescript
import { ForeGroundLocation } from 'foreground-location-plugin';

class LocationService {
  private isTracking = false;
  private locationListener: any;
  private statusListener: any;

  async startTracking() {
    try {
      // Check permissions
      const permissions = await ForeGroundLocation.checkPermissions();
      if (permissions.location !== 'granted') {
        await ForeGroundLocation.requestPermissions();
      }

      // Set up listeners
      this.locationListener = await ForeGroundLocation.addListener(
        'locationUpdate',
        this.handleLocationUpdate.bind(this),
      );

      this.statusListener = await ForeGroundLocation.addListener(
        'serviceStatusChanged',
        this.handleStatusChange.bind(this),
      );

      // Start service
      await ForeGroundLocation.startForegroundLocationService({
        interval: 60000,
        priority: 'HIGH_ACCURACY',
        notification: {
          title: 'Location Tracking',
          text: 'Tracking your location',
        },
      });

      this.isTracking = true;
      console.log('Location tracking started');
    } catch (error) {
      console.error('Failed to start tracking:', error);
    }
  }

  async stopTracking() {
    try {
      await ForeGroundLocation.stopForegroundLocationService();

      this.locationListener?.remove();
      this.statusListener?.remove();

      this.isTracking = false;
      console.log('Location tracking stopped');
    } catch (error) {
      console.error('Failed to stop tracking:', error);
    }
  }

  private handleLocationUpdate(location: any) {
    console.log('New location:', location);
    // Update your app state, send to server, etc.
  }

  private handleStatusChange(status: any) {
    console.log('Service status:', status);
    if (status.error) {
      console.error('Service error:', status.error);
    }
  }
}

export default LocationService;
```

## License

MIT License - see LICENSE file for details.

## Contributing

Contributions are welcome! Please read the contributing guidelines before submitting PRs.

## Support

For issues and questions:

1. Check the troubleshooting section
2. Search existing issues
3. Create a new issue with detailed information

## Changelog

### 1.0.0

- Initial release
- Android foreground service implementation
- iOS/Web service not available fallbacks
- Comprehensive permission handling
- Real-time location updates
- Configurable service options
