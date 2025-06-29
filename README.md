# Capacitor Foreground Location Plugin

A robust Capacitor plugin that provides foreground location tracking with optional API service integration. This plugin enables continuous location tracking with a foreground service, real-time updates to your Ionic/Capacitor app, and optional batching of location data to remote API endpoints.

## Features

- 🚀 **Foreground Service**: Continuous location tracking with persistent notification
- 📍 **High Accuracy**: Support for multiple location priority levels
- 📱 **Real-time Updates**: Live location updates via event listeners
- 🌐 **API Integration**: Batch and send location data to remote endpoints with retry logic
- ⚡ **Power Efficient**: Configurable intervals and priorities for battery optimization
- 🔄 **Resilient API Service**: Circuit breaker pattern and exponential backoff
- 💾 **Memory Safe**: Configurable buffer management
- 🛡️ **Permission Management**: Comprehensive permission handling for all platforms
- 📊 **Service Monitoring**: Real-time service and API status monitoring
- 🎯 **Cross-platform**: Full support for Android and iOS (web stub implementation)

## Installation

```bash
npm install foreground-location
npx cap sync
```

## Platform Setup

### Android Configuration

Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
```

### iOS Configuration

Add the following to your `ios/App/App/Info.plist`:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to track your location in the foreground</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs location access to track your location continuously</string>
<key>UIBackgroundModes</key>
<array>
  <string>location</string>
</array>
```

## Quick Start

### Basic Location Tracking

```typescript
import { ForeGroundLocation } from 'foreground-location';

// Check and request permissions
const checkPermissions = async () => {
  const permissions = await ForeGroundLocation.checkPermissions();

  if (permissions.location !== 'granted') {
    const requested = await ForeGroundLocation.requestPermissions();
    if (requested.location !== 'granted') {
      throw new Error('Location permission required');
    }
  }
};

// Start location tracking
const startTracking = async () => {
  try {
    await checkPermissions();

    // Add location listener
    await ForeGroundLocation.addListener('locationUpdate', (location) => {
      console.log('New location:', location);
      // Handle location update in your app
    });

    // Start the foreground service
    await ForeGroundLocation.startForegroundLocationService({
      interval: 10000, // Update every 10 seconds
      fastestInterval: 5000, // But not faster than 5 seconds
      priority: 'HIGH_ACCURACY',
      notification: {
        title: 'Location Tracking',
        text: 'Tracking your location in the background',
        icon: 'location_on', // Optional: your notification icon
      },
    });

    console.log('Location tracking started');
  } catch (error) {
    console.error('Failed to start location tracking:', error);
  }
};

// Stop location tracking
const stopTracking = async () => {
  try {
    await ForeGroundLocation.stopForegroundLocationService();
    await ForeGroundLocation.removeAllListeners();
    console.log('Location tracking stopped');
  } catch (error) {
    console.error('Failed to stop location tracking:', error);
  }
};
```

### Location Tracking with API Integration

```typescript
import { ForeGroundLocation } from 'foreground-location';

const startTrackingWithAPI = async () => {
  try {
    await checkPermissions();

    // Listen for location updates
    await ForeGroundLocation.addListener('locationUpdate', (location) => {
      console.log('Real-time location:', location);
      // Update your UI immediately
    });

    // Listen for service status changes
    await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
      console.log('Service status:', status);
      if (status.error) {
        console.error('Service error:', status.error);
      }
    });

    // Start tracking with API integration
    await ForeGroundLocation.startForegroundLocationService({
      interval: 30000, // 30 seconds
      fastestInterval: 15000,
      priority: 'HIGH_ACCURACY',
      notification: {
        title: 'Location Tracking Active',
        text: 'Sending location data to server',
      },
      api: {
        url: 'https://api.yourservice.com/locations',
        type: 'POST',
        header: {
          Authorization: 'Bearer your-token',
          'Content-Type': 'application/json',
        },
        apiInterval: 5, // Send to API every 5 minutes
        additionalParams: {
          userId: 'user123',
          deviceId: 'device456',
        },
      },
    });

    console.log('Location tracking with API started');
  } catch (error) {
    console.error('Failed to start tracking:', error);
  }
};
```

## API Reference

### Methods

#### `checkPermissions(): Promise<LocationPermissionStatus>`

Check the current permission status for location services.

**Returns:**

```typescript
interface LocationPermissionStatus {
  location: PermissionState; // 'granted' | 'denied' | 'prompt'
  backgroundLocation: PermissionState; // Android 10+ background location
  notifications: PermissionState; // Android 13+ notification permission
}
```

#### `requestPermissions(): Promise<LocationPermissionStatus>`

Request location and notification permissions from the user.

#### `startForegroundLocationService(options: LocationServiceOptions): Promise<void>`

Start the foreground location service with the specified configuration.

**Parameters:**

```typescript
interface LocationServiceOptions {
  interval?: number; // Update interval in ms (default: 60000)
  fastestInterval?: number; // Fastest interval in ms (default: 30000)
  priority?: 'HIGH_ACCURACY' | 'BALANCED_POWER' | 'LOW_POWER' | 'NO_POWER';
  notification: {
    // REQUIRED for foreground service
    title: string; // Notification title
    text: string; // Notification content
    icon?: string; // Optional icon resource name
  };
  enableHighAccuracy?: boolean; // Enable GPS (default: true)
  distanceFilter?: number; // Min distance in meters for updates
  api?: ApiServiceConfig; // Optional API integration
}
```

#### `stopForegroundLocationService(): Promise<void>`

Stop the foreground location service and clear all location tracking.

#### `isServiceRunning(): Promise<{ isRunning: boolean }>`

Check if the location service is currently running.

#### `getCurrentLocation(): Promise<LocationResult>`

Get a single location update without starting the service.

**Returns:**

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

#### `updateLocationSettings(options: LocationServiceOptions): Promise<void>`

Update the location service settings while it's running.

#### `getApiServiceStatus(): Promise<ApiServiceStatus>`

Get the current status of the API service integration.

**Returns:**

```typescript
interface ApiServiceStatus {
  isEnabled: boolean; // API service is configured and active
  bufferSize: number; // Number of locations waiting to be sent
  isHealthy: boolean; // Service is healthy (not in circuit breaker state)
}
```

#### `clearApiBuffers(): Promise<void>`

Clear all buffered location data waiting to be sent to the API.

#### `resetApiCircuitBreaker(): Promise<void>`

Reset the API service circuit breaker to allow requests again.

### Event Listeners

#### `addListener(eventName: 'locationUpdate', callback: (location: LocationResult) => void): Promise<PluginListenerHandle>`

Listen for real-time location updates.

#### `addListener(eventName: 'serviceStatusChanged', callback: (status: ServiceStatus) => void): Promise<PluginListenerHandle>`

Listen for service status changes.

**ServiceStatus:**

```typescript
interface ServiceStatus {
  isRunning: boolean;
  error?: string; // Error message if service failed
}
```

#### `removeAllListeners(): Promise<void>`

Remove all event listeners.

### Configuration Interfaces

#### `ApiServiceConfig`

```typescript
interface ApiServiceConfig {
  url: string; // API endpoint URL (REQUIRED)
  type?: 'GET' | 'POST' | 'PUT' | 'PATCH'; // HTTP method (default: 'POST')
  header?: Record<string, string>; // HTTP headers
  additionalParams?: Record<string, any>; // Extra parameters for request body
  apiInterval?: number; // Send interval in minutes (default: 5)
}
```

### Error Handling

The plugin provides consistent error codes:

```typescript
const ERROR_CODES = {
  PERMISSION_DENIED: 'PERMISSION_DENIED',
  INVALID_NOTIFICATION: 'INVALID_NOTIFICATION',
  INVALID_PARAMETERS: 'INVALID_PARAMETERS',
  LOCATION_SERVICES_DISABLED: 'LOCATION_SERVICES_DISABLED',
  UNSUPPORTED_PLATFORM: 'UNSUPPORTED_PLATFORM',
} as const;
```

## Advanced Usage Examples

### Service Monitoring

```typescript
import { ForeGroundLocation } from 'foreground-location';

const monitorLocationService = async () => {
  // Check if service is already running
  const { isRunning } = await ForeGroundLocation.isServiceRunning();
  console.log('Service running:', isRunning);

  // Monitor API service status
  const apiStatus = await ForeGroundLocation.getApiServiceStatus();
  console.log('API Status:', {
    enabled: apiStatus.isEnabled,
    buffer: apiStatus.bufferSize,
    healthy: apiStatus.isHealthy,
  });

  // Listen for service status changes
  await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
    if (status.error) {
      console.error('Service error:', status.error);
      // Handle service errors (restart, notify user, etc.)
    }
  });
};
```

### Power-Optimized Tracking

```typescript
const startEfficientTracking = async () => {
  await ForeGroundLocation.startForegroundLocationService({
    interval: 120000, // 2 minutes for battery saving
    fastestInterval: 60000, // Minimum 1 minute
    priority: 'BALANCED_POWER', // Balance accuracy and battery
    distanceFilter: 10, // Only update if moved 10+ meters
    enableHighAccuracy: false, // Disable GPS for power saving
    notification: {
      title: 'Efficient Tracking',
      text: 'Battery-optimized location tracking',
    },
  });
};
```

### Dynamic Settings Update

```typescript
const switchToHighAccuracy = async () => {
  try {
    // Update settings without stopping the service
    await ForeGroundLocation.updateLocationSettings({
      interval: 5000,
      fastestInterval: 2000,
      priority: 'HIGH_ACCURACY',
      enableHighAccuracy: true,
      notification: {
        title: 'High Accuracy Mode',
        text: 'Precise location tracking active',
      },
    });
    console.log('Switched to high accuracy mode');
  } catch (error) {
    console.error('Failed to update settings:', error);
  }
};
```

### API Integration with Error Handling

```typescript
const robustApiTracking = async () => {
  try {
    await ForeGroundLocation.startForegroundLocationService({
      interval: 30000,
      notification: {
        title: 'Location Sync',
        text: 'Syncing location data to cloud',
      },
      api: {
        url: 'https://api.yourservice.com/locations',
        type: 'POST',
        header: {
          Authorization: 'Bearer ' + (await getAuthToken()),
          'Content-Type': 'application/json',
          'X-Device-ID': await getDeviceId(),
        },
        apiInterval: 3, // Send every 3 minutes
        additionalParams: {
          source: 'mobile-app',
          version: '1.0.0',
        },
      },
    });

    // Monitor API health
    setInterval(async () => {
      const status = await ForeGroundLocation.getApiServiceStatus();

      if (!status.isHealthy) {
        console.warn('API service unhealthy, resetting...');
        await ForeGroundLocation.resetApiCircuitBreaker();
      }

      if (status.bufferSize > 50) {
        console.warn('Buffer getting full:', status.bufferSize);
        // Optionally clear old data or adjust settings
      }
    }, 60000); // Check every minute
  } catch (error) {
    console.error('API tracking setup failed:', error);
  }
};
```

## Implementation Patterns

### React/Ionic Hook

```typescript
import { useEffect, useState } from 'react';
import { ForeGroundLocation, LocationResult } from 'foreground-location';

export const useLocationTracking = () => {
  const [isTracking, setIsTracking] = useState(false);
  const [currentLocation, setCurrentLocation] = useState<LocationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const startTracking = async (options: LocationServiceOptions) => {
    try {
      setError(null);

      // Check permissions
      const permissions = await ForeGroundLocation.checkPermissions();
      if (permissions.location !== 'granted') {
        const requested = await ForeGroundLocation.requestPermissions();
        if (requested.location !== 'granted') {
          throw new Error('Location permission denied');
        }
      }

      // Add listeners
      await ForeGroundLocation.addListener('locationUpdate', (location) => {
        setCurrentLocation(location);
      });

      await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
        setIsTracking(status.isRunning);
        if (status.error) {
          setError(status.error);
        }
      });

      // Start service
      await ForeGroundLocation.startForegroundLocationService(options);
      setIsTracking(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setIsTracking(false);
    }
  };

  const stopTracking = async () => {
    try {
      await ForeGroundLocation.stopForegroundLocationService();
      await ForeGroundLocation.removeAllListeners();
      setIsTracking(false);
      setCurrentLocation(null);
    } catch (err) {
      console.error('Failed to stop tracking:', err);
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      stopTracking();
    };
  }, []);

  return {
    isTracking,
    currentLocation,
    error,
    startTracking,
    stopTracking,
  };
};
```

### Angular Service

```typescript
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ForeGroundLocation, LocationResult, ServiceStatus } from 'foreground-location';

@Injectable({
  providedIn: 'root',
})
export class LocationTrackingService {
  private locationSubject = new BehaviorSubject<LocationResult | null>(null);
  private statusSubject = new BehaviorSubject<ServiceStatus>({ isRunning: false });

  public location$: Observable<LocationResult | null> = this.locationSubject.asObservable();
  public status$: Observable<ServiceStatus> = this.statusSubject.asObservable();

  async startTracking(options: LocationServiceOptions): Promise<void> {
    try {
      // Request permissions
      const permissions = await ForeGroundLocation.requestPermissions();
      if (permissions.location !== 'granted') {
        throw new Error('Location permission required');
      }

      // Setup listeners
      await ForeGroundLocation.addListener('locationUpdate', (location) => {
        this.locationSubject.next(location);
      });

      await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
        this.statusSubject.next(status);
      });

      // Start service
      await ForeGroundLocation.startForegroundLocationService(options);
    } catch (error) {
      console.error('Failed to start location tracking:', error);
      throw error;
    }
  }

  async stopTracking(): Promise<void> {
    await ForeGroundLocation.stopForegroundLocationService();
    await ForeGroundLocation.removeAllListeners();
    this.statusSubject.next({ isRunning: false });
  }

  async getCurrentLocation(): Promise<LocationResult> {
    return await ForeGroundLocation.getCurrentLocation();
  }

  async getApiStatus(): Promise<ApiServiceStatus> {
    return await ForeGroundLocation.getApiServiceStatus();
  }
}
```

## Best Practices

### Permission Management

- Always check permissions before starting location services
- Handle permission denial gracefully with user-friendly messaging
- Request permissions in context when user understands why they're needed

### Battery Optimization

- Use `BALANCED_POWER` priority for general use cases
- Increase intervals (60+ seconds) for background tracking
- Set `distanceFilter` to avoid unnecessary updates
- Disable `enableHighAccuracy` when precise GPS isn't needed

### API Integration

- Use reasonable `apiInterval` values (5-15 minutes)
- Implement proper authentication token refresh
- Monitor `bufferSize` to prevent memory issues
- Handle circuit breaker states appropriately

### Error Handling

- Always wrap plugin calls in try-catch blocks
- Listen for `serviceStatusChanged` events
- Provide user feedback for service failures
- Implement retry logic for critical operations

## Troubleshooting

### Common Issues

| Issue                | Cause                       | Solution                                             |
| -------------------- | --------------------------- | ---------------------------------------------------- |
| Service not starting | Missing notification config | Provide `notification.title` and `notification.text` |
| Permission denied    | User denied location access | Call `requestPermissions()` and handle denial        |
| High battery usage   | Too frequent updates        | Increase `interval` and use `BALANCED_POWER`         |
| API data not sending | Network/auth issues         | Check API status and reset circuit breaker           |
| Location inaccurate  | Wrong priority setting      | Use `HIGH_ACCURACY` and `enableHighAccuracy: true`   |

### Debug Information

```typescript
// Get comprehensive debug info
const debugInfo = async () => {
  const permissions = await ForeGroundLocation.checkPermissions();
  const serviceStatus = await ForeGroundLocation.isServiceRunning();
  const apiStatus = await ForeGroundLocation.getApiServiceStatus();

  console.log('Debug Info:', {
    permissions,
    serviceRunning: serviceStatus.isRunning,
    apiEnabled: apiStatus.isEnabled,
    bufferSize: apiStatus.bufferSize,
    apiHealthy: apiStatus.isHealthy,
  });
};
```

## Platform Differences

### Android

- Requires foreground service with persistent notification
- Background location permission needed for Android 10+
- Notification permission required for Android 13+
- Uses Google Play Services Fused Location Provider

### iOS

- Uses Core Location framework
- Requires usage description in Info.plist
- Background location requires additional entitlements
- Location accuracy may be limited based on authorization level

### Web

- Stub implementation that throws `unavailable` errors
- Use Capacitor's Geolocation plugin for web support

## Data Privacy

### Location Data Handling

- Location data is only stored temporarily in memory buffers
- API integration sends data to your specified endpoints only
- No data is sent to plugin developers or third parties
- Clear buffers regularly to minimize data retention

### Compliance Considerations

- Ensure your API endpoints comply with GDPR, CCPA, etc.
- Implement user consent mechanisms in your app
- Provide clear privacy notices about location tracking
- Allow users to opt-out and delete their data

## Performance Monitoring

```typescript
// Monitor performance metrics
const monitorPerformance = async () => {
  setInterval(async () => {
    const apiStatus = await ForeGroundLocation.getApiServiceStatus();
    const serviceStatus = await ForeGroundLocation.isServiceRunning();

    // Log metrics for monitoring
    console.log('Performance Metrics:', {
      timestamp: new Date().toISOString(),
      serviceRunning: serviceStatus.isRunning,
      apiBufferSize: apiStatus.bufferSize,
      memoryUsage: (performance as any).memory?.usedJSHeapSize,
    });
  }, 300000); // Every 5 minutes
};
```

## Migration Guide

### From version 0.0.x to 1.0.x

- Update method names to use `startForegroundLocationService()` instead of `startLocationTracking()`
- Add required `notification` configuration
- Update API configuration structure
- Handle new permission states

## Type Definitions

### Interfaces

#### LocationPermissionStatus

```typescript
interface LocationPermissionStatus {
  /**
   * Fine and coarse location permission status
   */
  location: PermissionState;

  /**
   * Background location permission status (Android 10+)
   */
  backgroundLocation: PermissionState;

  /**
   * Notification permission status (Android 13+)
   */
  notifications: PermissionState;
}
```

#### LocationServiceOptions

```typescript
interface LocationServiceOptions {
  /**
   * Update interval in milliseconds
   * @default 60000
   * @minimum 1000
   */
  interval?: number;

  /**
   * Fastest update interval in milliseconds
   * @default 30000
   * @minimum 1000
   */
  fastestInterval?: number;

  /**
   * Location accuracy priority
   * @default 'HIGH_ACCURACY'
   */
  priority?: 'HIGH_ACCURACY' | 'BALANCED_POWER' | 'LOW_POWER' | 'NO_POWER';

  /**
   * Notification configuration for foreground service (REQUIRED)
   */
  notification: {
    /**
     * Notification title (REQUIRED)
     */
    title: string;
    /**
     * Notification text/content (REQUIRED)
     */
    text: string;
    /**
     * Optional notification icon resource name
     */
    icon?: string;
  };

  /**
   * Enable high accuracy mode
   * @default true
   */
  enableHighAccuracy?: boolean;

  /**
   * Minimum distance in meters to trigger update
   */
  distanceFilter?: number;

  /**
   * API service configuration (optional)
   * If provided, location data will be sent to the specified API endpoint in batches
   */
  api?: ApiServiceConfig;
}
```

#### ApiServiceConfig

```typescript
interface ApiServiceConfig {
  /**
   * API endpoint URL (REQUIRED if api config is provided)
   */
  url: string;

  /**
   * HTTP method to use
   * @default 'POST'
   */
  type?: 'GET' | 'POST' | 'PUT' | 'PATCH';

  /**
   * HTTP headers to include in API requests
   */
  header?: Record<string, string>;

  /**
   * Additional parameters to include in API request body
   */
  additionalParams?: Record<string, any>;

  /**
   * Interval in minutes for sending batched location data to API
   * @default 5
   * @minimum 1
   */
  apiInterval?: number;
}
```

#### ApiServiceStatus

```typescript
interface ApiServiceStatus {
  /**
   * Whether API service is enabled and configured
   */
  isEnabled: boolean;

  /**
   * Number of location points in buffer waiting to be sent
   */
  bufferSize: number;

  /**
   * Whether API service is healthy (not in circuit breaker state)
   */
  isHealthy: boolean;
}
```

#### LocationResult

```typescript
interface LocationResult {
  /**
   * Latitude in decimal degrees
   */
  latitude: number;

  /**
   * Longitude in decimal degrees
   */
  longitude: number;

  /**
   * Accuracy of the location in meters
   */
  accuracy: number;

  /**
   * Altitude in meters (if available)
   */
  altitude?: number;

  /**
   * Bearing in degrees (if available)
   */
  bearing?: number;

  /**
   * Speed in meters per second (if available)
   */
  speed?: number;

  /**
   * Timestamp in ISO 8601 format
   */
  timestamp: string;
}
```

#### ServiceStatus

```typescript
interface ServiceStatus {
  /**
   * Whether the service is currently running
   */
  isRunning: boolean;

  /**
   * Error message if service failed
   */
  error?: string;
}
```

### Type Aliases

#### PermissionState

```typescript
type PermissionState = 'prompt' | 'prompt-with-rationale' | 'granted' | 'denied';
```

#### ErrorCode

```typescript
type ErrorCode =
  | 'PERMISSION_DENIED'
  | 'INVALID_NOTIFICATION'
  | 'INVALID_PARAMETERS'
  | 'LOCATION_SERVICES_DISABLED'
  | 'UNSUPPORTED_PLATFORM';
```

#### PluginListenerHandle

```typescript
interface PluginListenerHandle {
  remove: () => Promise<void>;
}
```

## Support

- **GitHub Issues**: [Report bugs and feature requests](https://github.com/xconcepts17/foreground-location/issues)
- **Documentation**: [Complete setup guide](./docs/setup-and-examples.md)
- **Examples**: [Implementation examples](./docs/setup-and-examples.md#complete-implementation-example)

## License

MIT License - see [LICENSE](./LICENSE) file for details.

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md) for contribution guidelines and development setup.
