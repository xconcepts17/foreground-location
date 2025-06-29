import { WebPlugin } from '@capacitor/core';

import type {
  ForeGroundLocationPlugin,
  LocationPermissionStatus,
  LocationResult,
  ApiServiceStatus
} from './definitions';

export class ForeGroundLocationWeb extends WebPlugin implements ForeGroundLocationPlugin {

  async checkPermissions(): Promise<LocationPermissionStatus> {
    if (typeof navigator === 'undefined') {
      throw this.unavailable('Navigator not available');
    }

    if (!navigator.permissions) {
      throw this.unavailable('Permissions API not available in this browser');
    }

    if (!navigator.geolocation) {
      throw this.unavailable('Geolocation API not available in this browser');
    }

    try {
      const permission = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
      return {
        location: permission.state as any,
        backgroundLocation: 'denied',
        notifications: 'denied'
      };
    } catch (error) {
      // Fallback for browsers that support geolocation but not permissions query
      return {
        location: 'prompt',
        backgroundLocation: 'denied',
        notifications: 'denied'
      };
    }
  }

  async requestPermissions(): Promise<LocationPermissionStatus> {
    throw this.unimplemented('Background location service not supported on web. Use getCurrentLocation() for one-time location access.');
  }

  async startForegroundLocationService(): Promise<void> {
    throw this.unimplemented('Foreground location service not supported on web. Use getCurrentLocation() for one-time location access.');
  }

  async stopForegroundLocationService(): Promise<void> {
    throw this.unimplemented('Foreground location service not supported on web.');
  }

  async isServiceRunning(): Promise<{ isRunning: boolean }> {
    return { isRunning: false };
  }

  async getCurrentLocation(): Promise<LocationResult> {
    if (typeof navigator === 'undefined') {
      throw this.unavailable('Navigator not available');
    }

    if (!navigator.geolocation) {
      throw this.unavailable('Geolocation API not available in this browser');
    }

    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            altitude: position.coords.altitude || undefined,
            bearing: position.coords.heading || undefined,
            speed: position.coords.speed || undefined,
            timestamp: new Date(position.timestamp).toISOString(),
          });
        },
        (error) => {
          let errorMessage = 'Location error';
          switch (error.code) {
            case error.PERMISSION_DENIED:
              errorMessage = 'Location permission denied';
              break;
            case error.POSITION_UNAVAILABLE:
              errorMessage = 'Location position unavailable';
              break;
            case error.TIMEOUT:
              errorMessage = 'Location request timed out';
              break;
            default:
              errorMessage = `Location error: ${error.message}`;
              break;
          }
          reject(errorMessage);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 300000,
        }
      );
    });
  }

  async updateLocationSettings(): Promise<void> {
    throw this.unimplemented('Location service settings not supported on web.');
  }

  async getApiServiceStatus(): Promise<ApiServiceStatus> {
    throw this.unimplemented('API service not supported on web.');
  }

  async clearApiBuffers(): Promise<void> {
    throw this.unimplemented('API service not supported on web.');
  }

  async resetApiCircuitBreaker(): Promise<void> {
    throw this.unimplemented('API service not supported on web.');
  }
}
