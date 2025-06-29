import type { PermissionState, PluginListenerHandle } from '@capacitor/core';

// Re-export Capacitor types for user convenience
export type { PermissionState, PluginListenerHandle };

export interface ForeGroundLocationPlugin {
  /**
   * Check current permission status
   * @since 1.0.0
   */
  checkPermissions(): Promise<LocationPermissionStatus>;

  /**
   * Request location permissions
   * @since 1.0.0
   */
  requestPermissions(): Promise<LocationPermissionStatus>;

  /**
   * Start foreground location service
   * @since 1.0.0
   */
  startForegroundLocationService(options: LocationServiceOptions): Promise<void>;

  /**
   * Stop foreground location service
   * @since 1.0.0
   */
  stopForegroundLocationService(): Promise<void>;

  /**
   * Check if location service is running
   * @since 1.0.0
   */
  isServiceRunning(): Promise<{ isRunning: boolean }>;

  /**
   * Get current location once
   * @since 1.0.0
   */
  getCurrentLocation(): Promise<LocationResult>;

  /**
   * Update location service settings
   * @since 1.0.0
   */
  updateLocationSettings(options: LocationServiceOptions): Promise<void>;

  /**
   * Get API service status
   * @since 1.0.0
   */
  getApiServiceStatus(): Promise<ApiServiceStatus>;

  /**
   * Clear API service buffers
   * @since 1.0.0
   */
  clearApiBuffers(): Promise<void>;

  /**
   * Reset API service circuit breaker
   * @since 1.0.0
   */
  resetApiCircuitBreaker(): Promise<void>;

  /**
   * Listen for location updates
   * @since 1.0.0
   */
  addListener(
    eventName: 'locationUpdate',
    listenerFunc: (location: LocationResult) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Listen for service status changes
   * @since 1.0.0
   */
  addListener(
    eventName: 'serviceStatusChanged',
    listenerFunc: (status: ServiceStatus) => void,
  ): Promise<PluginListenerHandle>;

  /**
   * Remove all listeners
   * @since 1.0.0
   */
  removeAllListeners(): Promise<void>;
}

export interface LocationServiceOptions {
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

export interface ApiServiceConfig {
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

export interface ApiServiceStatus {
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

export interface LocationResult {
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

export interface LocationPermissionStatus {
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

export interface ServiceStatus {
  /**
   * Whether the service is currently running
   */
  isRunning: boolean;

  /**
   * Error message if service failed
   */
  error?: string;
}

/**
 * Plugin error codes for consistent error handling
 */
export const ERROR_CODES = {
  /**
   * Location permission denied or not granted
   */
  PERMISSION_DENIED: 'PERMISSION_DENIED',

  /**
   * Invalid notification configuration
   */
  INVALID_NOTIFICATION: 'INVALID_NOTIFICATION',

  /**
   * Invalid parameters provided
   */
  INVALID_PARAMETERS: 'INVALID_PARAMETERS',

  /**
   * Location services disabled
   */
  LOCATION_SERVICES_DISABLED: 'LOCATION_SERVICES_DISABLED',

  /**
   * Feature not supported on current platform
   */
  UNSUPPORTED_PLATFORM: 'UNSUPPORTED_PLATFORM'
} as const;

export type ErrorCode = typeof ERROR_CODES[keyof typeof ERROR_CODES];
