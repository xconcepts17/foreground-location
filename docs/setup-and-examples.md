# Setup and Examples Guide

This comprehensive guide provides detailed setup instructions and practical examples for the Capacitor Foreground Location plugin. Learn how to implement location tracking with foreground services and optional API integration.

## Table of Contents

- [Installation and Setup](#installation-and-setup)
- [Permission Management](#permission-management)
- [Basic Implementation](#basic-implementation)
- [Advanced Features](#advanced-features)
- [API Integration Examples](#api-integration-examples)
- [Framework-Specific Examples](#framework-specific-examples)
- [Production Considerations](#production-considerations)
- [Troubleshooting](#troubleshooting)

## Installation and Setup

### 1. Install the Plugin

```bash
npm install foreground-location
npx cap sync
```

### 2. Platform Configuration

#### Android Setup

**Add permissions to `android/app/src/main/AndroidManifest.xml`:**

```xml
<!-- Location permissions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Foreground service permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<!-- Notification permission (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Internet for API integration -->
<uses-permission android:name="android.permission.INTERNET" />
```

**Optional: Add custom notification icon to `android/app/src/main/res/drawable/`:**

```xml
<!-- ic_location_tracking.xml -->
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

#### iOS Setup

**Add usage descriptions to `ios/App/App/Info.plist`:**

```xml
<dict>
  <!-- Other keys... -->

  <!-- Location permissions -->
  <key>NSLocationWhenInUseUsageDescription</key>
  <string>This app needs location access to track your position while actively using the app</string>

  <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>This app needs continuous location access to provide location-based services</string>

  <!-- Background modes for location tracking -->
  <key>UIBackgroundModes</key>
  <array>
    <string>location</string>
  </array>

  <!-- Optional: Support for background app refresh -->
  <key>UIBackgroundRefreshStatusAvailable</key>
  <true/>
</dict>
```

### 3. Build and Sync

```bash
# Build the project
npm run build

# Sync with native platforms
npx cap sync

# Open in native IDEs for testing
npx cap open ios
npx cap open android
```

## Permission Management

### Basic Permission Handling

```typescript
import { ForeGroundLocation, LocationPermissionStatus } from 'foreground-location';

class PermissionManager {
  /**
   * Check current permission status
   */
  async checkPermissions(): Promise<LocationPermissionStatus> {
    try {
      const permissions = await ForeGroundLocation.checkPermissions();
      console.log('Current permissions:', {
        location: permissions.location,
        backgroundLocation: permissions.backgroundLocation,
        notifications: permissions.notifications,
      });
      return permissions;
    } catch (error) {
      console.error('Failed to check permissions:', error);
      throw error;
    }
  }

  /**
   * Request all required permissions
   */
  async requestPermissions(): Promise<boolean> {
    try {
      const permissions = await ForeGroundLocation.requestPermissions();

      // Check if basic location permission is granted
      if (permissions.location !== 'granted') {
        throw new Error('Location permission is required for this feature');
      }

      // Log permission status
      console.log('Permission results:', {
        location: permissions.location,
        backgroundLocation: permissions.backgroundLocation,
        notifications: permissions.notifications,
      });

      return true;
    } catch (error) {
      console.error('Permission request failed:', error);
      return false;
    }
  }

  /**
   * Handle permission states with user-friendly messages
   */
  async handlePermissions(): Promise<boolean> {
    const permissions = await this.checkPermissions();

    switch (permissions.location) {
      case 'granted':
        console.log('✅ Location permission granted');
        return true;

      case 'denied':
        console.log('❌ Location permission denied permanently');
        // Show instructions to enable in settings
        this.showPermissionInstructions();
        return false;

      case 'prompt':
        console.log('🔄 Requesting location permission...');
        return await this.requestPermissions();

      default:
        console.log('❓ Unknown permission state');
        return false;
    }
  }

  private showPermissionInstructions(): void {
    // Implement UI to guide user to app settings
    alert('Please enable location permissions in your device settings to use this feature.');
  }
}
```

## Basic Implementation

### Simple Location Tracking

```typescript
import { ForeGroundLocation, LocationResult, ServiceStatus } from 'foreground-location';

class BasicLocationService {
  private permissionManager = new PermissionManager();
  private locationListener: any = null;
  private statusListener: any = null;

  /**
   * Start basic location tracking
   */
  async startTracking(): Promise<void> {
    try {
      // 1. Handle permissions
      const hasPermissions = await this.permissionManager.handlePermissions();
      if (!hasPermissions) {
        throw new Error('Location permissions required');
      }

      // 2. Set up event listeners
      await this.setupListeners();

      // 3. Start the foreground service
      await ForeGroundLocation.startForegroundLocationService({
        interval: 30000, // Update every 30 seconds
        fastestInterval: 15000, // But not faster than 15 seconds
        priority: 'HIGH_ACCURACY',
        notification: {
          title: 'Location Tracking Active',
          text: 'Tracking your location in the background',
          icon: 'ic_location_tracking', // Optional custom icon
        },
        enableHighAccuracy: true,
        distanceFilter: 5, // Only update if moved 5+ meters
      });

      console.log('✅ Location tracking started successfully');
    } catch (error) {
      console.error('❌ Failed to start location tracking:', error);
      throw error;
    }
  }

  /**
   * Set up event listeners for location and status updates
   */
  private async setupListeners(): Promise<void> {
    // Listen for location updates
    this.locationListener = await ForeGroundLocation.addListener(
      'locationUpdate',
      this.handleLocationUpdate.bind(this),
    );

    // Listen for service status changes
    this.statusListener = await ForeGroundLocation.addListener(
      'serviceStatusChanged',
      this.handleStatusChange.bind(this),
    );
  }

  /**
   * Handle incoming location updates
   */
  private handleLocationUpdate(location: LocationResult): void {
    console.log('📍 New location update:', {
      coordinates: `${location.latitude}, ${location.longitude}`,
      accuracy: `${location.accuracy}m`,
      timestamp: location.timestamp,
      speed: location.speed ? `${location.speed} m/s` : 'N/A',
      altitude: location.altitude ? `${location.altitude}m` : 'N/A',
    });

    // Update your app's UI or store the location
    this.updateLocationDisplay(location);
  }

  /**
   * Handle service status changes
   */
  private handleStatusChange(status: ServiceStatus): void {
    console.log('🔄 Service status changed:', status);

    if (status.error) {
      console.error('❌ Service error:', status.error);
      this.handleServiceError(status.error);
    }

    if (!status.isRunning) {
      console.log('⏹️ Location service stopped');
    }
  }

  /**
   * Handle service errors
   */
  private handleServiceError(error: string): void {
    // Implement error handling based on your app's needs
    switch (error) {
      case 'LOCATION_SERVICES_DISABLED':
        alert('Please enable location services in your device settings');
        break;
      case 'PERMISSION_DENIED':
        alert('Location permission was revoked. Please re-enable it.');
        break;
      default:
        console.error('Unknown service error:', error);
    }
  }

  /**
   * Update your app's location display
   */
  private updateLocationDisplay(location: LocationResult): void {
    // Implement based on your UI framework
    // For example, update a map, show coordinates, etc.
  }

  /**
   * Stop location tracking
   */
  async stopTracking(): Promise<void> {
    try {
      // Stop the location service
      await ForeGroundLocation.stopForegroundLocationService();

      // Remove event listeners
      if (this.locationListener) {
        this.locationListener.remove();
        this.locationListener = null;
      }

      if (this.statusListener) {
        this.statusListener.remove();
        this.statusListener = null;
      }

      console.log('✅ Location tracking stopped successfully');
    } catch (error) {
      console.error('❌ Failed to stop location tracking:', error);
      throw error;
    }
  }

  /**
   * Get current service status
   */
  async getServiceStatus(): Promise<{ isRunning: boolean }> {
    return await ForeGroundLocation.isServiceRunning();
  }

  /**
   * Get a single location update without starting the service
   */
  async getCurrentLocation(): Promise<LocationResult> {
    const hasPermissions = await this.permissionManager.handlePermissions();
    if (!hasPermissions) {
      throw new Error('Location permissions required');
    }

    return await ForeGroundLocation.getCurrentLocation();
  }
}
```

## Advanced Features

### Power Management and Dynamic Settings

```typescript
class AdvancedLocationService extends BasicLocationService {
  private currentMode: 'efficient' | 'accurate' | 'custom' = 'efficient';

  /**
   * Start with battery-efficient settings
   */
  async startEfficientMode(): Promise<void> {
    await this.startTracking();

    await ForeGroundLocation.updateLocationSettings({
      interval: 120000, // 2 minutes
      fastestInterval: 60000, // 1 minute minimum
      priority: 'BALANCED_POWER',
      enableHighAccuracy: false,
      distanceFilter: 20, // 20 meters
      notification: {
        title: 'Efficient Tracking',
        text: 'Battery-optimized location tracking',
      },
    });

    this.currentMode = 'efficient';
    console.log('🔋 Switched to efficient mode');
  }

  /**
   * Switch to high accuracy mode
   */
  async switchToAccurateMode(): Promise<void> {
    try {
      await ForeGroundLocation.updateLocationSettings({
        interval: 10000, // 10 seconds
        fastestInterval: 5000, // 5 seconds
        priority: 'HIGH_ACCURACY',
        enableHighAccuracy: true,
        distanceFilter: 1, // 1 meter
        notification: {
          title: 'High Accuracy Mode',
          text: 'Precise location tracking active',
        },
      });

      this.currentMode = 'accurate';
      console.log('🎯 Switched to accurate mode');
    } catch (error) {
      console.error('Failed to switch to accurate mode:', error);
    }
  }

  /**
   * Automatically adjust settings based on speed
   */
  private handleLocationUpdate(location: LocationResult): void {
    super.handleLocationUpdate(location);

    // Auto-adjust based on movement speed
    if (location.speed !== undefined) {
      if (location.speed > 15 && this.currentMode !== 'accurate') {
        // High speed detected, switch to accurate mode
        this.switchToAccurateMode();
      } else if (location.speed < 2 && this.currentMode !== 'efficient') {
        // Stationary or slow movement, switch to efficient mode
        this.startEfficientMode();
      }
    }
  }

  /**
   * Get comprehensive service information
   */
  async getServiceInfo(): Promise<any> {
    const [serviceStatus, apiStatus] = await Promise.all([
      ForeGroundLocation.isServiceRunning(),
      ForeGroundLocation.getApiServiceStatus(),
    ]);

    return {
      service: serviceStatus,
      api: apiStatus,
      mode: this.currentMode,
      timestamp: new Date().toISOString(),
    };
  }
}
```

## API Integration Examples

### Basic API Integration

```typescript
import { ForeGroundLocation, ApiServiceConfig } from 'foreground-location';

class ApiLocationService extends BasicLocationService {
  private apiConfig: ApiServiceConfig;

  constructor(apiEndpoint: string, authToken: string) {
    super();
    this.apiConfig = {
      url: apiEndpoint,
      type: 'POST',
      header: {
        Authorization: `Bearer ${authToken}`,
        'Content-Type': 'application/json',
        'X-Client': 'mobile-app',
      },
      apiInterval: 5, // Send data every 5 minutes
      additionalParams: {
        deviceId: this.getDeviceId(),
        appVersion: '1.0.0',
      },
    };
  }

  /**
   * Start tracking with API integration
   */
  async startApiTracking(): Promise<void> {
    try {
      const hasPermissions = await this.permissionManager.handlePermissions();
      if (!hasPermissions) {
        throw new Error('Location permissions required');
      }

      await this.setupListeners();

      // Start with API configuration
      await ForeGroundLocation.startForegroundLocationService({
        interval: 30000,
        fastestInterval: 15000,
        priority: 'HIGH_ACCURACY',
        notification: {
          title: 'Location Sync Active',
          text: 'Syncing location data to server',
        },
        api: this.apiConfig,
      });

      // Monitor API service
      this.startApiMonitoring();

      console.log('✅ API location tracking started');
    } catch (error) {
      console.error('❌ Failed to start API tracking:', error);
      throw error;
    }
  }

  /**
   * Monitor API service health
   */
  private startApiMonitoring(): void {
    setInterval(async () => {
      try {
        const status = await ForeGroundLocation.getApiServiceStatus();

        console.log('📊 API Status:', {
          enabled: status.isEnabled,
          buffer: status.bufferSize,
          healthy: status.isHealthy,
        });

        // Handle unhealthy API service
        if (!status.isHealthy) {
          console.warn('⚠️ API service unhealthy, attempting reset...');
          await ForeGroundLocation.resetApiCircuitBreaker();
        }

        // Handle buffer overflow
        if (status.bufferSize > 100) {
          console.warn('⚠️ API buffer overflow, clearing old data...');
          await ForeGroundLocation.clearApiBuffers();
        }
      } catch (error) {
        console.error('Failed to monitor API status:', error);
      }
    }, 60000); // Check every minute
  }

  /**
   * Update API configuration
   */
  async updateApiConfig(newConfig: Partial<ApiServiceConfig>): Promise<void> {
    this.apiConfig = { ...this.apiConfig, ...newConfig };

    // Restart service with new config
    await ForeGroundLocation.updateLocationSettings({
      api: this.apiConfig,
    });
  }

  private getDeviceId(): string {
    // Implement device ID generation
    return 'device-' + Math.random().toString(36).substr(2, 9);
  }
}
```

### Advanced API Integration with Auth Refresh

```typescript
class SecureApiLocationService extends ApiLocationService {
  private authToken: string = '';
  private refreshToken: string = '';
  private tokenExpiryTime: number = 0;

  constructor(apiEndpoint: string, initialAuthToken: string, refreshToken: string) {
    super(apiEndpoint, initialAuthToken);
    this.authToken = initialAuthToken;
    this.refreshToken = refreshToken;
    this.scheduleTokenRefresh();
  }

  /**
   * Schedule automatic token refresh
   */
  private scheduleTokenRefresh(): void {
    setInterval(async () => {
      if (Date.now() >= this.tokenExpiryTime - 300000) {
        // 5 minutes before expiry
        await this.refreshAuthToken();
      }
    }, 60000); // Check every minute
  }

  /**
   * Refresh authentication token
   */
  private async refreshAuthToken(): Promise<void> {
    try {
      const response = await fetch('https://api.yourservice.com/auth/refresh', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.refreshToken}`,
        },
      });

      if (response.ok) {
        const data = await response.json();
        this.authToken = data.accessToken;
        this.tokenExpiryTime = Date.now() + data.expiresIn * 1000;

        // Update API configuration with new token
        await this.updateApiConfig({
          header: {
            ...this.apiConfig.header,
            Authorization: `Bearer ${this.authToken}`,
          },
        });

        console.log('🔐 Auth token refreshed successfully');
      } else {
        throw new Error('Token refresh failed');
      }
    } catch (error) {
      console.error('❌ Failed to refresh auth token:', error);
      // Handle auth failure (logout user, show login screen, etc.)
    }
  }

  /**
   * Handle API service errors with retry logic
   */
  protected handleServiceError(error: string): void {
    super.handleServiceError(error);

    if (error.includes('401') || error.includes('403')) {
      // Authentication error, attempt token refresh
      this.refreshAuthToken();
    }
  }
}
```

## Framework-Specific Examples

### React/Ionic Implementation

```typescript
import React, { useState, useEffect, useCallback } from 'react';
import {
  IonButton,
  IonContent,
  IonHeader,
  IonPage,
  IonTitle,
  IonToolbar,
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonItem,
  IonLabel,
  IonIcon,
  IonBadge
} from '@ionic/react';
import { locationOutline, playOutline, stopOutline } from 'ionicons/icons';
import { ForeGroundLocation, LocationResult, ApiServiceStatus } from 'foreground-location';

const LocationTracker: React.FC = () => {
  const [isTracking, setIsTracking] = useState(false);
  const [currentLocation, setCurrentLocation] = useState<LocationResult | null>(null);
  const [apiStatus, setApiStatus] = useState<ApiServiceStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Custom hook for location tracking
  const useLocationTracking = () => {
    const startTracking = useCallback(async () => {
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
        await ForeGroundLocation.addListener('locationUpdate', (location: LocationResult) => {
          setCurrentLocation(location);
          console.log('📍 Location update:', location);
        });

        await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
          setIsTracking(status.isRunning);
          if (status.error) {
            setError(status.error);
          }
        });

        // Start service
        await ForeGroundLocation.startForegroundLocationService({
          interval: 10000,
          fastestInterval: 5000,
          priority: 'HIGH_ACCURACY',
          notification: {
            title: 'Location Tracking',
            text: 'Tracking your location'
          },
          api: {
            url: 'https://api.example.com/locations',
            type: 'POST',
            header: {
              'Authorization': 'Bearer your-token',
              'Content-Type': 'application/json'
            },
            apiInterval: 2
          }
        });

        setIsTracking(true);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
        setIsTracking(false);
      }
    }, []);

    const stopTracking = useCallback(async () => {
      try {
        await ForeGroundLocation.stopForegroundLocationService();
        await ForeGroundLocation.removeAllListeners();
        setIsTracking(false);
        setCurrentLocation(null);
        setApiStatus(null);
      } catch (err) {
        console.error('Failed to stop tracking:', err);
      }
    }, []);

    return { startTracking, stopTracking };
  };

  const { startTracking, stopTracking } = useLocationTracking();

  // Monitor API status
  useEffect(() => {
    if (!isTracking) return;

    const checkApiStatus = async () => {
      try {
        const status = await ForeGroundLocation.getApiServiceStatus();
        setApiStatus(status);
      } catch (error) {
        console.error('Failed to check API status:', error);
      }
    };

    const interval = setInterval(checkApiStatus, 30000);
    checkApiStatus(); // Initial check

    return () => clearInterval(interval);
  }, [isTracking]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (isTracking) {
        stopTracking();
      }
    };
  }, [isTracking, stopTracking]);

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Location Tracker</IonTitle>
        </IonToolbar>
      </IonHeader>

      <IonContent className="ion-padding">
        {/* Control Buttons */}
        <IonCard>
          <IonCardHeader>
            <IonCardTitle>
              <IonIcon icon={locationOutline} /> Location Service
            </IonCardTitle>
          </IonCardHeader>
          <IonCardContent>
            <IonButton
              expand="block"
              color={isTracking ? "danger" : "primary"}
              onClick={isTracking ? stopTracking : startTracking}
            >
              <IonIcon icon={isTracking ? stopOutline : playOutline} slot="start" />
              {isTracking ? 'Stop Tracking' : 'Start Tracking'}
            </IonButton>

            {error && (
              <div style={{ color: 'var(--ion-color-danger)', marginTop: '10px' }}>
                Error: {error}
              </div>
            )}
          </IonCardContent>
        </IonCard>

        {/* Current Location */}
        {currentLocation && (
          <IonCard>
            <IonCardHeader>
              <IonCardTitle>Current Location</IonCardTitle>
            </IonCardHeader>
            <IonCardContent>
              <IonItem>
                <IonLabel>
                  <h3>Coordinates</h3>
                  <p>{currentLocation.latitude.toFixed(6)}, {currentLocation.longitude.toFixed(6)}</p>
                </IonLabel>
              </IonItem>
              <IonItem>
                <IonLabel>
                  <h3>Accuracy</h3>
                  <p>{currentLocation.accuracy.toFixed(1)} meters</p>
                </IonLabel>
              </IonItem>
              {currentLocation.speed !== undefined && (
                <IonItem>
                  <IonLabel>
                    <h3>Speed</h3>
                    <p>{(currentLocation.speed * 3.6).toFixed(1)} km/h</p>
                  </IonLabel>
                </IonItem>
              )}
              <IonItem>
                <IonLabel>
                  <h3>Last Update</h3>
                  <p>{new Date(currentLocation.timestamp).toLocaleString()}</p>
                </IonLabel>
              </IonItem>
            </IonCardContent>
          </IonCard>
        )}

        {/* API Status */}
        {apiStatus && (
          <IonCard>
            <IonCardHeader>
              <IonCardTitle>API Service Status</IonCardTitle>
            </IonCardHeader>
            <IonCardContent>
              <IonItem>
                <IonLabel>Service Status</IonLabel>
                <IonBadge color={apiStatus.isEnabled ? "success" : "medium"}>
                  {apiStatus.isEnabled ? "Enabled" : "Disabled"}
                </IonBadge>
              </IonItem>
              <IonItem>
                <IonLabel>Health Status</IonLabel>
                <IonBadge color={apiStatus.isHealthy ? "success" : "warning"}>
                  {apiStatus.isHealthy ? "Healthy" : "Unhealthy"}
                </IonBadge>
              </IonItem>
              <IonItem>
                <IonLabel>Buffer Size</IonLabel>
                <IonBadge color={apiStatus.bufferSize > 50 ? "warning" : "primary"}>
                  {apiStatus.bufferSize}
                </IonBadge>
              </IonItem>
            </IonCardContent>
          </IonCard>
        )}
      </IonContent>
    </IonPage>
  );
};

export default LocationTracker;
```

### Angular Service Implementation

```typescript
import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, interval, Subscription } from 'rxjs';
import {
  ForeGroundLocation,
  LocationResult,
  ServiceStatus,
  ApiServiceStatus,
  LocationServiceOptions,
} from 'foreground-location';

@Injectable({
  providedIn: 'root',
})
export class LocationTrackingService implements OnDestroy {
  private locationSubject = new BehaviorSubject<LocationResult | null>(null);
  private statusSubject = new BehaviorSubject<ServiceStatus>({ isRunning: false });
  private apiStatusSubject = new BehaviorSubject<ApiServiceStatus | null>(null);

  private locationListener: any = null;
  private statusListener: any = null;
  private apiMonitorSubscription: Subscription | null = null;

  // Public observables
  public location$: Observable<LocationResult | null> = this.locationSubject.asObservable();
  public status$: Observable<ServiceStatus> = this.statusSubject.asObservable();
  public apiStatus$: Observable<ApiServiceStatus | null> = this.apiStatusSubject.asObservable();

  /**
   * Start location tracking with configuration
   */
  async startTracking(options: LocationServiceOptions): Promise<void> {
    try {
      // Request permissions
      const permissions = await ForeGroundLocation.requestPermissions();
      if (permissions.location !== 'granted') {
        throw new Error('Location permission required');
      }

      // Setup listeners
      await this.setupListeners();

      // Start the service
      await ForeGroundLocation.startForegroundLocationService(options);

      // Start API monitoring if API is configured
      if (options.api) {
        this.startApiMonitoring();
      }

      console.log('✅ Location tracking started');
    } catch (error) {
      console.error('❌ Failed to start location tracking:', error);
      throw error;
    }
  }

  /**
   * Stop location tracking
   */
  async stopTracking(): Promise<void> {
    try {
      await ForeGroundLocation.stopForegroundLocationService();
      await this.cleanup();
      console.log('✅ Location tracking stopped');
    } catch (error) {
      console.error('❌ Failed to stop location tracking:', error);
      throw error;
    }
  }

  /**
   * Get current location once
   */
  async getCurrentLocation(): Promise<LocationResult> {
    const permissions = await ForeGroundLocation.checkPermissions();
    if (permissions.location !== 'granted') {
      throw new Error('Location permission required');
    }
    return await ForeGroundLocation.getCurrentLocation();
  }

  /**
   * Update location service settings
   */
  async updateSettings(options: LocationServiceOptions): Promise<void> {
    await ForeGroundLocation.updateLocationSettings(options);
  }

  /**
   * Clear API buffers
   */
  async clearApiBuffers(): Promise<void> {
    await ForeGroundLocation.clearApiBuffers();
  }

  /**
   * Reset API circuit breaker
   */
  async resetApiCircuitBreaker(): Promise<void> {
    await ForeGroundLocation.resetApiCircuitBreaker();
  }

  /**
   * Setup event listeners
   */
  private async setupListeners(): Promise<void> {
    this.locationListener = await ForeGroundLocation.addListener('locationUpdate', (location: LocationResult) => {
      this.locationSubject.next(location);
    });

    this.statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status: ServiceStatus) => {
      this.statusSubject.next(status);
    });
  }

  /**
   * Start monitoring API service status
   */
  private startApiMonitoring(): void {
    this.apiMonitorSubscription = interval(30000).subscribe(async () => {
      try {
        const status = await ForeGroundLocation.getApiServiceStatus();
        this.apiStatusSubject.next(status);
      } catch (error) {
        console.error('Failed to check API status:', error);
      }
    });
  }

  /**
   * Cleanup listeners and subscriptions
   */
  private async cleanup(): Promise<void> {
    if (this.locationListener) {
      this.locationListener.remove();
      this.locationListener = null;
    }

    if (this.statusListener) {
      this.statusListener.remove();
      this.statusListener = null;
    }

    if (this.apiMonitorSubscription) {
      this.apiMonitorSubscription.unsubscribe();
      this.apiMonitorSubscription = null;
    }

    await ForeGroundLocation.removeAllListeners();

    // Reset subjects
    this.statusSubject.next({ isRunning: false });
    this.apiStatusSubject.next(null);
  }

  /**
   * OnDestroy lifecycle hook
   */
  ngOnDestroy(): void {
    this.cleanup();
  }
}
```

### Vue.js Composition API

```typescript
import { ref, onMounted, onUnmounted, computed } from 'vue';
import {
  ForeGroundLocation,
  LocationResult,
  ServiceStatus,
  ApiServiceStatus,
  LocationServiceOptions,
} from 'foreground-location';

export function useLocationTracking() {
  const isTracking = ref(false);
  const currentLocation = ref<LocationResult | null>(null);
  const serviceStatus = ref<ServiceStatus>({ isRunning: false });
  const apiStatus = ref<ApiServiceStatus | null>(null);
  const error = ref<string | null>(null);

  let locationListener: any = null;
  let statusListener: any = null;
  let apiMonitorInterval: number | null = null;

  // Computed properties
  const locationDisplay = computed(() => {
    if (!currentLocation.value) return null;

    return {
      coordinates: `${currentLocation.value.latitude.toFixed(6)}, ${currentLocation.value.longitude.toFixed(6)}`,
      accuracy: `${currentLocation.value.accuracy.toFixed(1)}m`,
      speed: currentLocation.value.speed ? `${(currentLocation.value.speed * 3.6).toFixed(1)} km/h` : 'N/A',
      timestamp: new Date(currentLocation.value.timestamp).toLocaleString(),
    };
  });

  const apiStatusDisplay = computed(() => {
    if (!apiStatus.value) return null;

    return {
      status: apiStatus.value.isEnabled ? 'Enabled' : 'Disabled',
      health: apiStatus.value.isHealthy ? 'Healthy' : 'Unhealthy',
      bufferWarning: apiStatus.value.bufferSize > 50,
    };
  });

  /**
   * Start location tracking
   */
  const startTracking = async (options: LocationServiceOptions) => {
    try {
      error.value = null;

      // Check permissions
      const permissions = await ForeGroundLocation.checkPermissions();
      if (permissions.location !== 'granted') {
        const requested = await ForeGroundLocation.requestPermissions();
        if (requested.location !== 'granted') {
          throw new Error('Location permission denied');
        }
      }

      // Setup listeners
      locationListener = await ForeGroundLocation.addListener('locationUpdate', (location: LocationResult) => {
        currentLocation.value = location;
      });

      statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status: ServiceStatus) => {
        serviceStatus.value = status;
        isTracking.value = status.isRunning;
        if (status.error) {
          error.value = status.error;
        }
      });

      // Start service
      await ForeGroundLocation.startForegroundLocationService(options);

      // Monitor API if configured
      if (options.api) {
        startApiMonitoring();
      }

      isTracking.value = true;
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error';
      isTracking.value = false;
    }
  };

  /**
   * Stop location tracking
   */
  const stopTracking = async () => {
    try {
      await ForeGroundLocation.stopForegroundLocationService();
      await cleanup();
      isTracking.value = false;
    } catch (err) {
      console.error('Failed to stop tracking:', err);
    }
  };

  /**
   * Start API monitoring
   */
  const startApiMonitoring = () => {
    apiMonitorInterval = window.setInterval(async () => {
      try {
        const status = await ForeGroundLocation.getApiServiceStatus();
        apiStatus.value = status;
      } catch (error) {
        console.error('Failed to check API status:', error);
      }
    }, 30000);
  };

  /**
   * Cleanup resources
   */
  const cleanup = async () => {
    if (locationListener) {
      locationListener.remove();
      locationListener = null;
    }

    if (statusListener) {
      statusListener.remove();
      statusListener = null;
    }

    if (apiMonitorInterval) {
      clearInterval(apiMonitorInterval);
      apiMonitorInterval = null;
    }

    await ForeGroundLocation.removeAllListeners();

    currentLocation.value = null;
    serviceStatus.value = { isRunning: false };
    apiStatus.value = null;
    error.value = null;
  };

  // Lifecycle hooks
  onMounted(() => {
    // Component mounted
  });

  onUnmounted(() => {
    cleanup();
  });

  return {
    // State
    isTracking,
    currentLocation,
    serviceStatus,
    apiStatus,
    error,

    // Computed
    locationDisplay,
    apiStatusDisplay,

    // Methods
    startTracking,
    stopTracking,
    cleanup,
  };
}
```

## Production Considerations

### Performance Optimization

```typescript
class ProductionLocationService extends BasicLocationService {
  private performanceMetrics = {
    locationUpdates: 0,
    apiCalls: 0,
    errors: 0,
    startTime: Date.now(),
  };

  /**
   * Production-ready configuration
   */
  async startProductionTracking(): Promise<void> {
    const config: LocationServiceOptions = {
      // Balanced settings for production
      interval: 60000, // 1 minute intervals
      fastestInterval: 30000, // Minimum 30 seconds
      priority: 'BALANCED_POWER', // Balance accuracy and battery
      distanceFilter: 10, // 10 meters minimum movement
      enableHighAccuracy: false, // Disable GPS for battery

      notification: {
        title: 'Location Services',
        text: 'App is tracking your location',
        icon: 'location_on',
      },

      api: {
        url: process.env.API_ENDPOINT!,
        type: 'POST',
        header: {
          Authorization: `Bearer ${await this.getValidToken()}`,
          'Content-Type': 'application/json',
          'X-App-Version': process.env.APP_VERSION || '1.0.0',
          'X-Platform': this.getPlatform(),
        },
        apiInterval: 10, // Send every 10 minutes
        additionalParams: {
          userId: await this.getUserId(),
          deviceId: await this.getDeviceId(),
          sessionId: this.generateSessionId(),
        },
      },
    };

    await this.startTracking();
    await ForeGroundLocation.updateLocationSettings(config);

    this.startPerformanceMonitoring();
  }

  /**
   * Monitor performance metrics
   */
  private startPerformanceMonitoring(): void {
    setInterval(() => {
      const uptime = Date.now() - this.performanceMetrics.startTime;
      const avgUpdatesPerHour = (this.performanceMetrics.locationUpdates / uptime) * 3600000;

      console.log('📊 Performance Metrics:', {
        uptime: `${Math.round(uptime / 60000)} minutes`,
        locationUpdates: this.performanceMetrics.locationUpdates,
        apiCalls: this.performanceMetrics.apiCalls,
        errors: this.performanceMetrics.errors,
        avgUpdatesPerHour: Math.round(avgUpdatesPerHour),
      });

      // Send metrics to analytics service
      this.sendAnalytics('location_service_performance', this.performanceMetrics);
    }, 600000); // Every 10 minutes
  }

  protected handleLocationUpdate(location: LocationResult): void {
    super.handleLocationUpdate(location);
    this.performanceMetrics.locationUpdates++;

    // Validate location data
    if (this.isValidLocation(location)) {
      this.processValidLocation(location);
    } else {
      console.warn('⚠️ Invalid location data received:', location);
      this.performanceMetrics.errors++;
    }
  }

  private isValidLocation(location: LocationResult): boolean {
    return (
      location.latitude >= -90 &&
      location.latitude <= 90 &&
      location.longitude >= -180 &&
      location.longitude <= 180 &&
      location.accuracy > 0 &&
      location.accuracy < 1000
    );
  }

  private async getValidToken(): Promise<string> {
    // Implement token validation and refresh logic
    return 'valid-token';
  }

  private getPlatform(): string {
    // Detect platform
    return 'unknown';
  }

  private async getUserId(): Promise<string> {
    // Get user ID from your auth system
    return 'user-id';
  }

  private async getDeviceId(): Promise<string> {
    // Generate or retrieve persistent device ID
    return 'device-id';
  }

  private generateSessionId(): string {
    return 'session-' + Math.random().toString(36).substr(2, 9);
  }

  private sendAnalytics(event: string, data: any): void {
    // Send to your analytics service
  }

  private processValidLocation(location: LocationResult): void {
    // Process valid location data
  }
}
```

### Error Handling and Recovery

```typescript
class RobustLocationService extends ProductionLocationService {
  private retryCount = 0;
  private maxRetries = 3;
  private backoffDelay = 1000;

  /**
   * Start with automatic retry on failure
   */
  async startWithRetry(): Promise<void> {
    while (this.retryCount < this.maxRetries) {
      try {
        await this.startProductionTracking();
        this.retryCount = 0; // Reset on success
        return;
      } catch (error) {
        this.retryCount++;
        console.error(`❌ Start attempt ${this.retryCount} failed:`, error);

        if (this.retryCount >= this.maxRetries) {
          throw new Error(`Failed to start after ${this.maxRetries} attempts: ${error}`);
        }

        // Exponential backoff
        const delay = this.backoffDelay * Math.pow(2, this.retryCount - 1);
        console.log(`⏳ Retrying in ${delay}ms...`);
        await this.sleep(delay);
      }
    }
  }

  /**
   * Handle service errors with recovery
   */
  protected handleServiceError(error: string): void {
    super.handleServiceError(error);

    // Implement specific recovery strategies
    switch (error) {
      case 'LOCATION_SERVICES_DISABLED':
        this.handleLocationServicesDisabled();
        break;
      case 'PERMISSION_DENIED':
        this.handlePermissionDenied();
        break;
      default:
        this.handleGenericError(error);
    }
  }

  private async handleLocationServicesDisabled(): Promise<void> {
    console.log('🔧 Attempting to handle disabled location services...');

    // Wait and retry
    await this.sleep(5000);

    try {
      const permissions = await ForeGroundLocation.checkPermissions();
      if (permissions.location === 'granted') {
        console.log('🔄 Attempting to restart service...');
        await this.startWithRetry();
      }
    } catch (error) {
      console.error('Failed to recover from disabled location services:', error);
    }
  }

  private async handlePermissionDenied(): Promise<void> {
    console.log('🔧 Handling permission denied...');

    // Show user guidance
    this.showPermissionGuidance();

    // Set up permission monitoring
    this.monitorPermissionChanges();
  }

  private handleGenericError(error: string): void {
    console.log('🔧 Handling generic error:', error);

    // Schedule retry
    setTimeout(() => {
      this.startWithRetry();
    }, 30000); // Retry after 30 seconds
  }

  private showPermissionGuidance(): void {
    // Show user-friendly permission guidance
    alert('Please enable location permissions in your device settings to continue tracking.');
  }

  private monitorPermissionChanges(): void {
    const checkPermissions = async () => {
      try {
        const permissions = await ForeGroundLocation.checkPermissions();
        if (permissions.location === 'granted') {
          console.log('✅ Permissions restored, restarting service...');
          await this.startWithRetry();
          clearInterval(permissionCheck);
        }
      } catch (error) {
        console.error('Permission monitoring error:', error);
      }
    };

    const permissionCheck = setInterval(checkPermissions, 10000); // Check every 10 seconds

    // Stop monitoring after 5 minutes
    setTimeout(() => {
      clearInterval(permissionCheck);
    }, 300000);
  }

  private sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
}
```

## Troubleshooting

### Common Issues and Solutions

| Issue                | Symptoms                                 | Cause                         | Solution                                                     |
| -------------------- | ---------------------------------------- | ----------------------------- | ------------------------------------------------------------ |
| Service won't start  | Error on startForegroundLocationService  | Missing notification config   | Ensure notification.title and notification.text are provided |
| No location updates  | Service running but no location events   | Permission issues             | Check permissions with checkPermissions()                    |
| High battery drain   | Device heating up, battery draining fast | Too frequent updates          | Increase interval, use BALANCED_POWER priority               |
| API data not sending | Buffer size increasing, no API calls     | Network/authentication issues | Check API endpoint, verify auth token                        |
| Location inaccurate  | Large accuracy values                    | Wrong settings                | Use HIGH_ACCURACY priority, enable GPS                       |
| App crashes on start | App closes when starting service         | Native permission issues      | Ensure all required permissions in manifest                  |

### Debug Utilities

```typescript
class LocationDebugger {
  /**
   * Comprehensive system check
   */
  static async runSystemCheck(): Promise<void> {
    console.log('🔍 Running location system diagnostics...');

    try {
      // Check permissions
      const permissions = await ForeGroundLocation.checkPermissions();
      console.log('📋 Permissions:', permissions);

      // Check service status
      const serviceStatus = await ForeGroundLocation.isServiceRunning();
      console.log('⚙️ Service Status:', serviceStatus);

      // Try getting current location
      try {
        const location = await ForeGroundLocation.getCurrentLocation();
        console.log('📍 Current Location:', location);
      } catch (error) {
        console.log('❌ Current Location Error:', error);
      }

      // Check API status
      try {
        const apiStatus = await ForeGroundLocation.getApiServiceStatus();
        console.log('🌐 API Status:', apiStatus);
      } catch (error) {
        console.log('❌ API Status Error:', error);
      }

      console.log('✅ System check completed');
    } catch (error) {
      console.error('❌ System check failed:', error);
    }
  }

  /**
   * Monitor location service health
   */
  static startHealthMonitoring(): void {
    setInterval(async () => {
      try {
        const timestamp = new Date().toISOString();
        const serviceStatus = await ForeGroundLocation.isServiceRunning();
        const apiStatus = await ForeGroundLocation.getApiServiceStatus();

        console.log(`🏥 Health Check [${timestamp}]:`, {
          service: serviceStatus.isRunning ? '✅' : '❌',
          api: apiStatus.isEnabled ? '✅' : '❌',
          healthy: apiStatus.isHealthy ? '✅' : '⚠️',
          buffer: apiStatus.bufferSize,
        });

        // Alert on issues
        if (!serviceStatus.isRunning) {
          console.warn('⚠️ Location service is not running!');
        }

        if (apiStatus.isEnabled && !apiStatus.isHealthy) {
          console.warn('⚠️ API service is unhealthy!');
        }

        if (apiStatus.bufferSize > 100) {
          console.warn('⚠️ API buffer is getting full:', apiStatus.bufferSize);
        }
      } catch (error) {
        console.error('❌ Health monitoring error:', error);
      }
    }, 60000); // Every minute
  }
}

// Usage
LocationDebugger.runSystemCheck();
LocationDebugger.startHealthMonitoring();
```

### Testing Utilities

```typescript
class LocationTester {
  /**
   * Test location service lifecycle
   */
  static async testLifecycle(): Promise<void> {
    console.log('🧪 Testing location service lifecycle...');

    try {
      // Test permissions
      console.log('1. Testing permissions...');
      const permissions = await ForeGroundLocation.checkPermissions();
      if (permissions.location !== 'granted') {
        const requested = await ForeGroundLocation.requestPermissions();
        console.log('Permission result:', requested);
      }

      // Test service start
      console.log('2. Testing service start...');
      await ForeGroundLocation.startForegroundLocationService({
        interval: 5000,
        notification: {
          title: 'Test Location Service',
          text: 'Testing location tracking',
        },
      });

      // Wait for location updates
      console.log('3. Waiting for location updates...');
      await new Promise((resolve) => {
        let updateCount = 0;
        ForeGroundLocation.addListener('locationUpdate', (location) => {
          updateCount++;
          console.log(`Location update ${updateCount}:`, location);

          if (updateCount >= 3) {
            resolve(undefined);
          }
        });
      });

      // Test service stop
      console.log('4. Testing service stop...');
      await ForeGroundLocation.stopForegroundLocationService();
      await ForeGroundLocation.removeAllListeners();

      console.log('✅ Lifecycle test completed successfully');
    } catch (error) {
      console.error('❌ Lifecycle test failed:', error);
    }
  }

  /**
   * Test API integration
   */
  static async testApiIntegration(apiUrl: string): Promise<void> {
    console.log('🧪 Testing API integration...');

    try {
      await ForeGroundLocation.startForegroundLocationService({
        interval: 10000,
        notification: {
          title: 'API Test',
          text: 'Testing API integration',
        },
        api: {
          url: apiUrl,
          type: 'POST',
          header: {
            'Content-Type': 'application/json',
          },
          apiInterval: 1, // Send every minute for testing
        },
      });

      // Monitor API status
      const checkInterval = setInterval(async () => {
        const status = await ForeGroundLocation.getApiServiceStatus();
        console.log('API Status:', status);
      }, 10000);

      // Run for 2 minutes
      setTimeout(async () => {
        clearInterval(checkInterval);
        await ForeGroundLocation.stopForegroundLocationService();
        console.log('✅ API test completed');
      }, 120000);
    } catch (error) {
      console.error('❌ API test failed:', error);
    }
  }
}
```

This comprehensive setup and examples guide provides everything you need to implement the Capacitor Foreground Location plugin correctly in your application. Remember to always test thoroughly on real devices and handle edge cases appropriately for production use.

useEffect(() => {
return () => {
// Cleanup on unmount
if (locationListener) {
locationListener.remove();
}
if (isTracking) {
ForeGroundLocation.stopLocationTracking();
}
};
}, [locationListener, isTracking]);

const startTracking = async () => {
try {
// Setup location listener
const listener = await ForeGroundLocation.addListener('locationUpdate', (location) => {
setCurrentLocation(location);
console.log('Location update received:', location);
});
setLocationListener(listener);

      // Configure API service
      const apiConfig: ApiServiceConfig = {
        baseUrl: 'https://your-api.com',
        endpoint: '/api/locations',
        method: 'POST',
        headers: {
          'Authorization': 'Bearer your-token',
          'Content-Type': 'application/json'
        },
        batchSize: 10,
        retryAttempts: 3,
        retryDelay: 1000,
        timeout: 30000,
        circuitBreakerThreshold: 5,
        bufferSize: 100
      };

      // Start tracking
      await ForeGroundLocation.startLocationTracking({
        interval: 10000,
        fastestInterval: 5000,
        priority: 'HIGH_ACCURACY',
        apiService: apiConfig
      });

      setIsTracking(true);

      // Start monitoring API status
      monitorApiStatus();

    } catch (error) {
      console.error('Failed to start tracking:', error);
    }

};

const stopTracking = async () => {
try {
await ForeGroundLocation.stopLocationTracking();

      if (locationListener) {
        locationListener.remove();
        setLocationListener(null);
      }

      setIsTracking(false);
      setApiStatus(null);

    } catch (error) {
      console.error('Failed to stop tracking:', error);
    }

};

const monitorApiStatus = () => {
const checkStatus = async () => {
try {
const status = await ForeGroundLocation.getApiServiceStatus();
setApiStatus(status);
} catch (error) {
console.error('Failed to get API status:', error);
}
};

    // Check immediately and then every 30 seconds
    checkStatus();
    const interval = setInterval(checkStatus, 30000);

    // Cleanup interval when tracking stops
    setTimeout(() => {
      if (!isTracking) {
        clearInterval(interval);
      }
    }, 100);

};

const clearBuffers = async () => {
try {
await ForeGroundLocation.clearApiBuffers();
console.log('Buffers cleared');
} catch (error) {
console.error('Failed to clear buffers:', error);
}
};

const resetCircuitBreaker = async () => {
try {
await ForeGroundLocation.resetApiCircuitBreaker();
console.log('Circuit breaker reset');
} catch (error) {
console.error('Failed to reset circuit breaker:', error);
}
};

return (
<IonPage>
<IonHeader>
<IonToolbar>
<IonTitle>Location Tracker</IonTitle>
</IonToolbar>
</IonHeader>
<IonContent>
<IonItem>
<IonLabel>
<h2>Tracking Status</h2>
<p>{isTracking ? 'Active' : 'Inactive'}</p>
</IonLabel>
</IonItem>

        {currentLocation && (
          <IonItem>
            <IonLabel>
              <h2>Current Location</h2>
              <p>Lat: {currentLocation.latitude.toFixed(6)}</p>
              <p>Lng: {currentLocation.longitude.toFixed(6)}</p>
              <p>Accuracy: {currentLocation.accuracy}m</p>
              <p>Time: {new Date(currentLocation.timestamp).toLocaleString()}</p>
            </IonLabel>
          </IonItem>
        )}

        {apiStatus && (
          <IonItem>
            <IonLabel>
              <h2>API Service Status</h2>
              <p>Active: {apiStatus.isActive ? 'Yes' : 'No'}</p>
              <p>Buffered: {apiStatus.bufferedCount} locations</p>
              <p>Circuit Breaker: {apiStatus.circuitBreakerOpen ? 'Open' : 'Closed'}</p>
              {apiStatus.lastSuccessfulCall && (
                <p>Last Success: {new Date(apiStatus.lastSuccessfulCall).toLocaleString()}</p>
              )}
              {apiStatus.lastError && (
                <p>Last Error: {apiStatus.lastError}</p>
              )}
            </IonLabel>
          </IonItem>
        )}

        <IonButton
          expand="block"
          onClick={isTracking ? stopTracking : startTracking}
          color={isTracking ? 'danger' : 'primary'}
        >
          {isTracking ? 'Stop Tracking' : 'Start Tracking'}
        </IonButton>

        {apiStatus && (
          <>
            <IonButton expand="block" fill="outline" onClick={clearBuffers}>
              Clear API Buffers
            </IonButton>

            {apiStatus.circuitBreakerOpen && (
              <IonButton expand="block" fill="outline" onClick={resetCircuitBreaker}>
                Reset Circuit Breaker
              </IonButton>
            )}
          </>
        )}
      </IonContent>
    </IonPage>

);
};

export default LocationTracker;
```

## Error Handling

### Comprehensive Error Handling Example

```typescript
import { ForeGroundLocation } from '@xconcepts/foreground-location';

class RobustLocationService {
  private maxRetries = 3;
  private retryCount = 0;

  async startTrackingWithErrorHandling() {
    try {
      await this.attemptStartTracking();
    } catch (error) {
      await this.handleStartError(error);
    }
  }

  private async attemptStartTracking() {
    try {
      await ForeGroundLocation.startLocationTracking({
        interval: 10000,
        fastestInterval: 5000,
        priority: 'HIGH_ACCURACY',
        apiService: {
          baseUrl: 'https://api.example.com',
          endpoint: '/locations',
          method: 'POST',
          headers: { Authorization: 'Bearer token' },
          batchSize: 10,
          retryAttempts: 3,
          timeout: 30000,
        },
      });

      this.retryCount = 0; // Reset on success
    } catch (error) {
      throw error;
    }
  }

  private async handleStartError(error: any) {
    console.error('Location tracking error:', error);

    if (this.retryCount < this.maxRetries) {
      this.retryCount++;
      console.log(`Retrying... Attempt ${this.retryCount}/${this.maxRetries}`);

      // Wait before retry
      await new Promise((resolve) => setTimeout(resolve, 2000 * this.retryCount));

      try {
        await this.attemptStartTracking();
      } catch (retryError) {
        await this.handleStartError(retryError);
      }
    } else {
      // Max retries reached
      console.error('Max retries reached. Location tracking failed.');
      this.handleFinalError(error);
    }
  }

  private handleFinalError(error: any) {
    // Handle different error types
    if (error.message?.includes('permission')) {
      console.error('Permission denied. Please grant location permissions.');
      // Show permission request dialog
    } else if (error.message?.includes('GPS')) {
      console.error('GPS is disabled. Please enable location services.');
      // Show GPS enable prompt
    } else if (error.message?.includes('network')) {
      console.error('Network error. API service may not be available.');
      // Try without API service
      this.startWithoutApi();
    } else {
      console.error('Unknown error occurred:', error);
      // Show generic error message
    }
  }

  private async startWithoutApi() {
    try {
      console.log('Starting location tracking without API service...');
      await ForeGroundLocation.startLocationTracking({
        interval: 10000,
        fastestInterval: 5000,
        priority: 'HIGH_ACCURACY',
        // No apiService configuration
      });
      console.log('Location tracking started without API service');
    } catch (error) {
      console.error('Failed to start even without API service:', error);
    }
  }
}
```

## Best Practices

### Performance Optimization

1. **Choose appropriate intervals**:
   - For real-time tracking: 5-10 seconds
   - For periodic updates: 30-60 seconds
   - For battery saving: 2-5 minutes

2. **Use suitable priority levels**:
   - `HIGH_ACCURACY`: For navigation apps
   - `BALANCED_POWER_ACCURACY`: For most apps
   - `LOW_POWER`: For background tracking
   - `NO_POWER`: For passive location updates

3. **Configure API batching**:
   - Small batches (5-10): For real-time requirements
   - Large batches (20-50): For efficient network usage

### Memory Management

```typescript
// Monitor and manage buffer sizes
const monitorMemoryUsage = async () => {
  const status = await ForeGroundLocation.getApiServiceStatus();

  if (status.bufferedCount > 80) {
    // 80% of default buffer size
    console.warn('Buffer getting full, consider clearing or increasing batch frequency');

    // Option 1: Clear buffers
    await ForeGroundLocation.clearApiBuffers();

    // Option 2: Adjust configuration (restart with smaller buffer)
    // await restartWithSmallerBuffer();
  }
};
```

### Security Considerations

```typescript
// Secure API configuration
const getSecureApiConfig = async (): Promise<ApiServiceConfig> => {
  const token = await getSecureToken(); // From secure storage

  return {
    baseUrl: await getApiEndpoint(), // From secure config
    endpoint: '/locations',
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Client-Version': getAppVersion(),
      'X-Request-ID': generateRequestId(),
    },
    batchSize: 10,
    retryAttempts: 3,
    timeout: 30000,
  };
};

// Rotate tokens periodically
const rotateAuthToken = async () => {
  try {
    const newToken = await refreshAuthToken();

    // Stop current tracking
    await ForeGroundLocation.stopLocationTracking();

    // Restart with new token
    const newConfig = await getSecureApiConfig();
    await ForeGroundLocation.startLocationTracking({
      interval: 10000,
      fastestInterval: 5000,
      priority: 'HIGH_ACCURACY',
      apiService: newConfig,
    });
  } catch (error) {
    console.error('Failed to rotate auth token:', error);
  }
};
```

### Testing and Debugging

```typescript
// Debug mode configuration
const DEBUG_MODE = process.env.NODE_ENV === 'development';

const getDebugApiConfig = (): ApiServiceConfig => ({
  baseUrl: DEBUG_MODE ? 'https://api-dev.example.com' : 'https://api.example.com',
  endpoint: '/locations',
  method: 'POST',
  headers: {
    Authorization: `Bearer ${getToken()}`,
    'Content-Type': 'application/json',
    ...(DEBUG_MODE && { 'X-Debug-Mode': 'true' }),
  },
  batchSize: DEBUG_MODE ? 3 : 10, // Smaller batches in debug
  retryAttempts: DEBUG_MODE ? 1 : 3,
  timeout: DEBUG_MODE ? 10000 : 30000,
});

// Enhanced logging for debugging
const debugLocationUpdate = (location: any) => {
  if (DEBUG_MODE) {
    console.log('Location Debug Info:', {
      coordinates: `${location.latitude}, ${location.longitude}`,
      accuracy: `${location.accuracy}m`,
      timestamp: new Date(location.timestamp).toISOString(),
      speed: location.speed ? `${location.speed} m/s` : 'N/A',
      bearing: location.bearing ? `${location.bearing}°` : 'N/A',
    });
  }
};
```

This completes the comprehensive setup and examples guide for the Capacitor Foreground Location plugin with API service integration.

### 3. iOS Configuration

#### Update Info.plist

```xml
<!-- ios/App/App/Info.plist -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to track your route.</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs location access to track your route in the background.</string>

<!-- If using background location -->
<key>UIBackgroundModes</key>
<array>
    <string>location</string>
</array>
```

## Basic Usage Examples

### Example 1: Simple Location Tracking

```typescript
import { ForeGroundLocation } from 'foreground-location';
import type { LocationResult } from 'foreground-location';

export class LocationService {
  private locationListener: any;

  async startTracking() {
    try {
      // Check and request permissions
      const permissions = await ForeGroundLocation.checkPermissions();

      if (permissions.location !== 'granted') {
        const requestResult = await ForeGroundLocation.requestPermissions();
        if (requestResult.location !== 'granted') {
          throw new Error('Location permission denied');
        }
      }

      // Start location service
      await ForeGroundLocation.startForegroundLocationService({
        notification: {
          title: 'Location Tracking',
          text: 'Tracking your location in the background',
          icon: 'ic_location',
        },
        interval: 30000, // 30 seconds
        fastestInterval: 15000, // 15 seconds
        priority: 'HIGH_ACCURACY',
      });

      // Listen for location updates
      this.locationListener = await ForeGroundLocation.addListener('locationUpdate', (location: LocationResult) => {
        console.log('New location:', {
          lat: location.latitude,
          lng: location.longitude,
          accuracy: location.accuracy,
          time: location.timestamp,
        });

        // Process location data
        this.handleLocationUpdate(location);
      });

      console.log('Location tracking started successfully');
    } catch (error) {
      console.error('Failed to start location tracking:', error);
      throw error;
    }
  }

  async stopTracking() {
    try {
      await ForeGroundLocation.stopForegroundLocationService();

      if (this.locationListener) {
        this.locationListener.remove();
        this.locationListener = null;
      }

      console.log('Location tracking stopped');
    } catch (error) {
      console.error('Failed to stop location tracking:', error);
    }
  }

  private handleLocationUpdate(location: LocationResult) {
    // Store location data locally
    // Update UI
    // Send to analytics, etc.
  }
}
```

### Example 2: Distance-Based Tracking

```typescript
export class DistanceBasedTracking {
  async startDistanceTracking() {
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Distance Tracking',
        text: 'Recording significant movement',
      },
      interval: 60000, // 1 minute baseline
      fastestInterval: 30000, // 30 seconds minimum
      priority: 'BALANCED_POWER', // Save battery
      distanceFilter: 50, // Only update every 50 meters
    });

    await ForeGroundLocation.addListener('locationUpdate', (location) => {
      console.log(`Moved at least 50m: ${location.latitude}, ${location.longitude}`);
    });
  }
}
```

### Example 3: Battery-Optimized Tracking

```typescript
export class BatteryOptimizedTracking {
  async startLowPowerTracking() {
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Low Power Tracking',
        text: 'Conserving battery while tracking',
      },
      interval: 300000, // 5 minutes
      fastestInterval: 180000, // 3 minutes
      priority: 'LOW_POWER', // Network-based location
      distanceFilter: 100, // Only significant movement
    });
  }
}
```

## API Service Integration

### Example 4: Basic API Integration

```typescript
export class APILocationService {
  async startWithAPI() {
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Route Recording',
        text: 'Uploading your journey to the cloud',
      },
      interval: 30000,
      priority: 'HIGH_ACCURACY',

      // API service configuration
      api: {
        url: 'https://api.yourcompany.com/locations',
        type: 'POST',
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer YOUR_API_TOKEN',
          'X-Device-ID': await this.getDeviceId(),
        },
        additionalParams: {
          userId: await this.getCurrentUserId(),
          sessionId: this.generateSessionId(),
          appVersion: '1.0.0',
        },
        apiInterval: 5, // Send data every 5 minutes
      },
    });

    // Monitor API service
    setInterval(async () => {
      const status = await ForeGroundLocation.getApiServiceStatus();
      console.log('API Status:', {
        enabled: status.isEnabled,
        bufferSize: status.bufferSize,
        healthy: status.isHealthy,
      });

      if (!status.isHealthy) {
        console.warn('API service is unhealthy - check network connection');
      }
    }, 60000); // Check every minute
  }

  private async getDeviceId(): Promise<string> {
    // Implementation to get unique device ID
    return 'device-123';
  }

  private async getCurrentUserId(): Promise<string> {
    // Implementation to get current user ID
    return 'user-456';
  }

  private generateSessionId(): string {
    return `session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}
```

### Example 5: Advanced API Configuration with Authentication

```typescript
export class AuthenticatedAPIService {
  private authToken: string = '';

  async startWithAuthenticatedAPI() {
    // Get fresh auth token
    this.authToken = await this.refreshAuthToken();

    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Secure Tracking',
        text: 'Securely uploading location data',
      },
      interval: 20000,
      api: {
        url: 'https://secure-api.yourcompany.com/v1/locations',
        type: 'POST',
        header: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${this.authToken}`,
          'X-API-Version': '1.0',
          'X-Client-Platform': 'mobile-app',
        },
        additionalParams: {
          userId: await this.getCurrentUserId(),
          deviceInfo: await this.getDeviceInfo(),
          trackingMode: 'high-accuracy',
          timestamp: new Date().toISOString(),
        },
        apiInterval: 3, // Send every 3 minutes for real-time tracking
      },
    });

    // Set up token refresh
    this.setupTokenRefresh();
  }

  private async refreshAuthToken(): Promise<string> {
    // Your authentication logic
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: this.getStoredRefreshToken() }),
    });

    const data = await response.json();
    return data.accessToken;
  }

  private setupTokenRefresh() {
    // Refresh token every 50 minutes (assuming 1-hour expiry)
    setInterval(
      async () => {
        try {
          this.authToken = await this.refreshAuthToken();
          console.log('Auth token refreshed successfully');

          // Update the running service with new token (restart with new config)
          await this.restartWithNewToken();
        } catch (error) {
          console.error('Failed to refresh auth token:', error);
          // Handle auth failure - maybe stop tracking or show user notification
        }
      },
      50 * 60 * 1000,
    );
  }

  private async restartWithNewToken() {
    // Restart service with new authentication token
    await ForeGroundLocation.stopForegroundLocationService();
    await this.startWithAuthenticatedAPI();
  }
}
```

### Example 6: API Error Handling and Recovery

```typescript
export class ResilientAPIService {
  async startResilientTracking() {
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Smart Tracking',
        text: 'Intelligent location sync with failover',
      },
      interval: 15000,
      api: {
        url: 'https://primary-api.yourcompany.com/locations',
        type: 'POST',
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer YOUR_TOKEN',
        },
        apiInterval: 2, // Frequent sync
      },
    });

    // Monitor and handle API issues
    this.startAPIMonitoring();
  }

  private startAPIMonitoring() {
    setInterval(async () => {
      const status = await ForeGroundLocation.getApiServiceStatus();

      if (!status.isHealthy) {
        console.warn('API service is unhealthy');

        // Check if buffer is getting too large
        if (status.bufferSize > 100) {
          console.warn('Large buffer detected, may need intervention');

          // Option 1: Clear buffers to prevent memory issues
          // await ForeGroundLocation.clearApiBuffers();

          // Option 2: Reset circuit breaker to retry immediately
          await ForeGroundLocation.resetApiCircuitBreaker();

          // Option 3: Switch to backup API endpoint
          await this.switchToBackupAPI();
        }
      } else if (status.bufferSize === 0) {
        console.log('API service healthy, all data synced');
      }
    }, 30000); // Check every 30 seconds
  }

  private async switchToBackupAPI() {
    console.log('Switching to backup API endpoint');

    await ForeGroundLocation.stopForegroundLocationService();

    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Backup Sync',
        text: 'Using backup server for location sync',
      },
      interval: 15000,
      api: {
        url: 'https://backup-api.yourcompany.com/locations',
        type: 'POST',
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer YOUR_TOKEN',
        },
        apiInterval: 5, // Less frequent on backup
      },
    });
  }
}
```

## Advanced Configuration

### Example 7: Dynamic Configuration Updates

```typescript
export class DynamicLocationService {
  private currentMode: 'normal' | 'high-accuracy' | 'battery-saver' = 'normal';

  async startDynamicTracking() {
    await this.updateConfigurationFor('normal');

    // Set up dynamic configuration based on app state
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        this.switchMode('battery-saver');
      } else {
        this.switchMode('high-accuracy');
      }
    });
  }

  async switchMode(mode: 'normal' | 'high-accuracy' | 'battery-saver') {
    if (this.currentMode === mode) return;

    console.log(`Switching to ${mode} mode`);
    this.currentMode = mode;

    await this.updateConfigurationFor(mode);
  }

  private async updateConfigurationFor(mode: string) {
    const configs = {
      'high-accuracy': {
        notification: {
          title: 'High Accuracy Mode',
          text: 'Precise location tracking active',
        },
        interval: 5000,
        fastestInterval: 2000,
        priority: 'HIGH_ACCURACY' as const,
        distanceFilter: 5,
        api: { apiInterval: 1 },
      },
      normal: {
        notification: {
          title: 'Standard Tracking',
          text: 'Normal location tracking',
        },
        interval: 30000,
        fastestInterval: 15000,
        priority: 'BALANCED_POWER' as const,
        distanceFilter: 20,
        api: { apiInterval: 5 },
      },
      'battery-saver': {
        notification: {
          title: 'Battery Saver Mode',
          text: 'Low power location tracking',
        },
        interval: 300000,
        fastestInterval: 180000,
        priority: 'LOW_POWER' as const,
        distanceFilter: 100,
        api: { apiInterval: 15 },
      },
    };

    const config = configs[mode];
    const baseApiConfig = {
      url: 'https://api.yourcompany.com/locations',
      type: 'POST' as const,
      header: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer YOUR_TOKEN',
      },
      additionalParams: {
        trackingMode: mode,
      },
    };

    await ForeGroundLocation.updateLocationSettings({
      ...config,
      api: { ...baseApiConfig, ...config.api },
    });
  }
}
```

## Error Handling

### Example 8: Comprehensive Error Handling

```typescript
export class ErrorHandlingLocationService {
  async startWithErrorHandling() {
    try {
      await this.initializeLocationService();
    } catch (error) {
      await this.handleLocationError(error);
    }
  }

  private async initializeLocationService() {
    // Check permissions first
    const permissions = await ForeGroundLocation.checkPermissions();

    if (permissions.location === 'denied') {
      throw new Error('PERMISSION_PERMANENTLY_DENIED');
    }

    if (permissions.location !== 'granted') {
      const result = await ForeGroundLocation.requestPermissions();
      if (result.location !== 'granted') {
        throw new Error('PERMISSION_DENIED');
      }
    }

    // Test location availability
    try {
      await ForeGroundLocation.getCurrentLocation();
    } catch (error) {
      throw new Error('LOCATION_NOT_AVAILABLE');
    }

    // Start service with error handling
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Location Service',
        text: 'Tracking with error handling',
      },
      interval: 30000,
      priority: 'HIGH_ACCURACY',
    });

    // Set up error monitoring
    this.startErrorMonitoring();
  }

  private async handleLocationError(error: Error) {
    console.error('Location service error:', error.message);

    switch (error.message) {
      case 'PERMISSION_DENIED':
        await this.showPermissionDialog();
        break;

      case 'PERMISSION_PERMANENTLY_DENIED':
        await this.showSettingsDialog();
        break;

      case 'LOCATION_NOT_AVAILABLE':
        await this.showLocationDisabledDialog();
        break;

      case 'INVALID_NOTIFICATION':
        console.error('Invalid notification configuration');
        break;

      case 'INVALID_PARAMETERS':
        console.error('Invalid service parameters');
        break;

      default:
        await this.showGenericErrorDialog(error.message);
    }
  }

  private startErrorMonitoring() {
    // Monitor service status
    const statusListener = ForeGroundLocation.addListener('serviceStatus', (status) => {
      if (!status.isRunning && status.error) {
        console.error('Service stopped with error:', status.error);
        this.handleServiceError(status.error);
      }
    });

    // Periodic health check
    setInterval(async () => {
      try {
        const isRunning = await ForeGroundLocation.isServiceRunning();
        if (!isRunning.isRunning) {
          console.warn('Service is not running, attempting restart');
          await this.attemptServiceRestart();
        }
      } catch (error) {
        console.error('Health check failed:', error);
      }
    }, 60000);
  }

  private async attemptServiceRestart() {
    try {
      await ForeGroundLocation.startForegroundLocationService({
        notification: {
          title: 'Location Service Restored',
          text: 'Resuming location tracking',
        },
        interval: 30000,
      });
      console.log('Service restarted successfully');
    } catch (error) {
      console.error('Failed to restart service:', error);
    }
  }

  private async showPermissionDialog() {
    // Show user-friendly permission request dialog
  }

  private async showSettingsDialog() {
    // Guide user to app settings
  }

  private async showLocationDisabledDialog() {
    // Guide user to enable device location
  }

  private async showGenericErrorDialog(errorMessage: string) {
    // Show generic error with details
  }

  private handleServiceError(error: string) {
    // Handle service-specific errors
  }
}
```

## Platform-Specific Setup

### Android-Specific Configuration

```typescript
export class AndroidLocationService {
  async setupAndroidSpecific() {
    // Check Android version and adjust configuration
    const isAndroid10Plus = this.isAndroidVersionAtLeast(10);

    if (isAndroid10Plus) {
      // Android 10+ requires background location permission
      const permissions = await ForeGroundLocation.checkPermissions();

      if (permissions.backgroundLocation !== 'granted') {
        // Show explanation before requesting background permission
        await this.showBackgroundLocationExplanation();

        const result = await ForeGroundLocation.requestPermissions();
        if (result.backgroundLocation !== 'granted') {
          console.warn('Background location not granted, some features may be limited');
        }
      }
    }

    // Configure for Android optimization
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Location Service',
        text: 'Optimized for Android',
        icon: 'ic_location_android', // Android-specific icon
      },
      interval: 15000,
      priority: 'HIGH_ACCURACY',
      // Android-specific optimizations
      distanceFilter: 0, // Let Android handle filtering
    });
  }

  private isAndroidVersionAtLeast(version: number): boolean {
    // Implementation to check Android version
    return true; // Placeholder
  }

  private async showBackgroundLocationExplanation() {
    // Show explanation dialog
  }
}
```

### iOS-Specific Configuration

```typescript
export class iOSLocationService {
  async setupiOSSpecific() {
    // iOS has different location permission model
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'Location Access',
        text: 'iOS location service active',
      },
      interval: 60000, // iOS may batch updates
      priority: 'HIGH_ACCURACY',
      // iOS handles most optimization automatically
    });

    // iOS-specific monitoring
    ForeGroundLocation.addListener('locationUpdate', (location) => {
      // iOS may provide delayed/batched updates
      console.log('iOS location update:', location);
    });
  }
}
```

## Troubleshooting

### Common Issues and Solutions

```typescript
export class TroubleshootingService {
  async diagnoseIssues() {
    console.log('Running location service diagnostics...');

    // 1. Check permissions
    const permissions = await ForeGroundLocation.checkPermissions();
    console.log('Permissions:', permissions);

    // 2. Test basic location access
    try {
      const location = await ForeGroundLocation.getCurrentLocation();
      console.log('Basic location access: OK', location);
    } catch (error) {
      console.error('Basic location access: FAILED', error);
    }

    // 3. Check service status
    const serviceStatus = await ForeGroundLocation.isServiceRunning();
    console.log('Service running:', serviceStatus.isRunning);

    // 4. Check API service if configured
    try {
      const apiStatus = await ForeGroundLocation.getApiServiceStatus();
      console.log('API Service:', {
        enabled: apiStatus.isEnabled,
        bufferSize: apiStatus.bufferSize,
        healthy: apiStatus.isHealthy,
      });
    } catch (error) {
      console.log('API Service: Not configured or unavailable');
    }

    // 5. Performance metrics
    this.startPerformanceMonitoring();
  }

  private startPerformanceMonitoring() {
    let updateCount = 0;
    let lastUpdate = Date.now();

    ForeGroundLocation.addListener('locationUpdate', (location) => {
      updateCount++;
      const now = Date.now();
      const timeSinceLastUpdate = now - lastUpdate;
      lastUpdate = now;

      console.log(`Update #${updateCount}, ${timeSinceLastUpdate}ms since last update`);

      // Check for issues
      if (timeSinceLastUpdate > 120000) {
        // 2 minutes
        console.warn('Long gap between updates detected');
      }

      if (location.accuracy > 100) {
        // 100 meters
        console.warn('Low accuracy location received:', location.accuracy);
      }
    });
  }
}
```

### Debug Configuration

```typescript
export class DebugLocationService {
  async startDebugMode() {
    await ForeGroundLocation.startForegroundLocationService({
      notification: {
        title: 'DEBUG: Location Service',
        text: 'Debug mode active - check console',
      },
      interval: 10000, // Frequent updates for debugging
      fastestInterval: 5000,
      priority: 'HIGH_ACCURACY',
      distanceFilter: 0, // No filtering for debugging

      api: {
        url: 'https://httpbin.org/post', // Test endpoint
        type: 'POST',
        header: {
          'Content-Type': 'application/json',
          'X-Debug': 'true',
        },
        additionalParams: {
          debugMode: true,
          timestamp: new Date().toISOString(),
        },
        apiInterval: 1, // Every minute for testing
      },
    });

    // Detailed logging
    ForeGroundLocation.addListener('locationUpdate', (location) => {
      console.log('DEBUG Location Update:', {
        coordinates: `${location.latitude}, ${location.longitude}`,
        accuracy: `${location.accuracy}m`,
        timestamp: location.timestamp,
        hasAltitude: !!location.altitude,
        hasBearing: !!location.bearing,
        hasSpeed: !!location.speed,
      });
    });

    // Monitor API service in detail
    setInterval(async () => {
      const status = await ForeGroundLocation.getApiServiceStatus();
      console.log('DEBUG API Status:', status);
    }, 30000);
  }
}
```

This comprehensive guide covers all aspects of setting up and using the Foreground Location Plugin with API service integration. Each example is production-ready and includes proper error handling and best practices.
