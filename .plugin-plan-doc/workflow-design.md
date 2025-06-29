# Plan for Ionic Capacitor Foreground Location Plugin

## Plugin Overview

This plugin will provide continuous location tracking using Android's Foreground Service with FusedLocationProviderClient and iOS's background location capabilities, while maintaining a consistent JavaScript API.

## Plugin Structure & Methods

### 1. Core Plugin Interface (`src/definitions.ts`)

#### Primary Methods

```typescript
export interface ForegroundLocationPlugin {
  // Permission Management
  checkPermissions(): Promise<LocationPermissionStatus>;
  requestPermissions(): Promise<LocationPermissionStatus>;

  // Service Lifecycle
  startForegroundLocationService(options: LocationServiceOptions): Promise<void>;
  stopForegroundLocationService(): Promise<void>;
  isServiceRunning(): Promise<{ isRunning: boolean }>;

  // Location Updates
  getCurrentLocation(): Promise<LocationResult>;

  // Event Listeners
  addListener(
    eventName: 'locationUpdate',
    listenerFunc: (location: LocationResult) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'serviceStatusChanged',
    listenerFunc: (status: ServiceStatus) => void,
  ): Promise<PluginListenerHandle>;

  // Configuration
  updateLocationSettings(options: LocationServiceOptions): Promise<void>;
}
```

#### Supporting Interfaces

```typescript
export interface LocationServiceOptions {
  interval: number; // milliseconds (default: 60000)
  fastestInterval: number; // milliseconds (default: 30000)
  priority: 'HIGH_ACCURACY' | 'BALANCED_POWER' | 'LOW_POWER' | 'NO_POWER';
  notification: {
    title: string;
    text: string;
    icon?: string;
  };
  enableHighAccuracy?: boolean;
  distanceFilter?: number; // meters
}

export interface LocationResult {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number;
  bearing?: number;
  speed?: number;
  timestamp: string; // ISO 8601 format
}

export interface LocationPermissionStatus {
  location: PermissionState;
  backgroundLocation: PermissionState;
  notifications: PermissionState;
}

export interface ServiceStatus {
  isRunning: boolean;
  error?: string;
}
```

## 2. Implementation Strategy by Platform

### Android Implementation Plan

#### Service Architecture

- **Main Plugin Class**: `ForegroundLocationPlugin.java`
  - Handles communication between Capacitor and the service
  - Manages permission requests and checks
  - Forwards events from service to JavaScript

- **Foreground Service**: `LocationForegroundService.java`
  - Extends `Service`
  - Manages `FusedLocationProviderClient`
  - Handles notification lifecycle
  - Emits location updates via broadcast or callback mechanism

#### Key Android Methods

```java
// Plugin Methods
@PluginMethod()
public void startForegroundLocationService(PluginCall call)

@PluginMethod()
public void stopForegroundLocationService(PluginCall call)

@PluginMethod()
public void checkPermissions(PluginCall call)

@PluginMethod()
public void requestPermissions(PluginCall call)

@PluginMethod()
public void getCurrentLocation(PluginCall call)

@PluginMethod()
public void isServiceRunning(PluginCall call)

@PluginMethod()
public void updateLocationSettings(PluginCall call)

// Service Communication
private void bindToLocationService()
private void unbindFromLocationService()
private void onLocationReceived(Location location)
```

#### Service Implementation Strategy

1. **Service Binding**: Use bound service pattern for communication
2. **Location Callback**: Implement `LocationCallback` in service
3. **Broadcast Updates**: Send location updates to plugin via LocalBroadcastManager
4. **Notification Management**: Create and update persistent notification
5. **Configuration Updates**: Allow runtime configuration changes

### iOS Implementation Plan

#### Core iOS Methods

```swift
// Plugin Methods
@objc func startForegroundLocationService(_ call: CAPPluginCall)
@objc func stopForegroundLocationService(_ call: CAPPluginCall)
@objc override func checkPermissions(_ call: CAPPluginCall)
@objc override func requestPermissions(_ call: CAPPluginCall)
@objc func getCurrentLocation(_ call: CAPPluginCall)
@objc func isServiceRunning(_ call: CAPPluginCall)
@objc func updateLocationSettings(_ call: CAPPluginCall)

// Location Management
private func startLocationUpdates()
private func stopLocationUpdates()
private func configureLocationManager()
```

#### iOS Strategy

1. **CLLocationManager**: Use for location updates
2. **Background Modes**: Configure for background location
3. **Significant Location Changes**: For battery optimization
4. **App State Monitoring**: Handle foreground/background transitions

### Web Implementation Plan

#### Web Fallback Methods

```typescript
// Geolocation API with polyfill for service concept
async startForegroundLocationService(options: LocationServiceOptions): Promise<void>
async stopForegroundLocationService(): Promise<void>
async getCurrentLocation(): Promise<LocationResult>

// Watch position for continuous updates
private watchId: number | null = null;
private startWatchingLocation(options: LocationServiceOptions)
private stopWatchingLocation()
```

## 3. Location Update Flow Design

### Foreground App Scenario (Ionic Subscribe Pattern)

```typescript
// In Ionic App
import { ForegroundLocation } from 'foreground-location-plugin';

// Start service and subscribe to updates
await ForegroundLocation.startForegroundLocationService({
  interval: 60000,
  priority: 'HIGH_ACCURACY',
  notification: {
    title: 'Location Tracking',
    text: 'Tracking your location',
  },
});

// Subscribe to location updates
const locationListener = await ForegroundLocation.addListener('locationUpdate', (location) => {
  console.log('New location:', location);
  // Update UI, send to server, etc.
});

// Get immediate location
const currentLocation = await ForegroundLocation.getCurrentLocation();
```

### Background Service Flow (Fusion Method)

1. **Service Initialization**: Start foreground service with notification
2. **FusedLocationProviderClient Setup**: Configure location request parameters
3. **Continuous Updates**: Service receives updates via `LocationCallback`
4. **Event Emission**: Service broadcasts updates to plugin
5. **JavaScript Events**: Plugin forwards updates to registered listeners

## 4. Permission Handling Strategy

### Permission Flow

1. **Initial Check**: `checkPermissions()` - Check current permission state
2. **Request Flow**: `requestPermissions()` - Request all required permissions
3. **Background Permission**: Special handling for Android 10+ background location
4. **Notification Permission**: Handle Android 13+ notification permission

### Permission States

- **location**: Fine/coarse location access
- **backgroundLocation**: Background location access (Android 10+)
- **notifications**: Notification permission (Android 13+)

## 5. Error Handling & Edge Cases

### Error Scenarios

- Location services disabled
- Permissions denied/revoked
- Service fails to start
- Battery optimization interference
- Network connectivity issues

### Handling Strategy

- Emit service status events
- Provide error details in callbacks
- Implement retry mechanisms
- Graceful degradation for missing permissions

## 6. Configuration & Optimization

### Battery Optimization

- Configurable update intervals
- Priority-based location requests
- Smart interval adjustment based on movement
- Stop updates when stationary (optional)

### User Experience

- Clear notification messaging
- Configurable notification content
- Service status indicators
- Easy start/stop controls

## 7. Development Workflow

### Plugin Development Steps

1. Create plugin scaffold with Capacitor CLI
2. Define TypeScript interfaces
3. Implement web fallback
4. Build Android foreground service
5. Implement Android plugin wrapper
6. Build iOS location manager
7. Implement iOS plugin wrapper
8. Test across platforms
9. Generate documentation

### Testing Strategy

- Unit tests for permission handling
- Integration tests for service lifecycle
- Real-device testing for battery impact
- Background/foreground transition testing
- Permission revocation scenarios

This plan provides a comprehensive approach to building a robust foreground location plugin that leverages platform-specific capabilities while maintaining a consistent JavaScript API for Ionic applications.
