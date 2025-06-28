import { WebPlugin } from '@capacitor/core';
import type {
  ForeGroundLocationPlugin,
  LocationPermissionStatus,
  LocationServiceOptions,
  LocationResult
} from './definitions';

export class ForeGroundLocationWeb extends WebPlugin implements ForeGroundLocationPlugin {

  async checkPermissions(): Promise<LocationPermissionStatus> {
    if (typeof navigator === 'undefined' || !navigator.permissions) {
      throw this.unavailable('Permissions API not available in this browser.');
    }

    try {
      const permission = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
      return {
        location: permission.state as any,
        backgroundLocation: 'denied',
        notifications: 'denied'
      };
    } catch {
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

  async startForegroundLocationService(_options: LocationServiceOptions): Promise<void> {
    throw this.unimplemented('Foreground location service not supported on web. Use getCurrentLocation() for one-time location access.');
  }

  async stopForegroundLocationService(): Promise<void> {
    throw this.unimplemented('Foreground location service not supported on web.');
  }

  async isServiceRunning(): Promise<{ isRunning: boolean }> {
    return { isRunning: false };
  }

  async getCurrentLocation(): Promise<LocationResult> {
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      throw this.unavailable('Geolocation API not available in this browser.');
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
          reject(`Location error: ${error.message}`);
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 300000,
        }
      );
    });
  }

  async updateLocationSettings(_options: LocationServiceOptions): Promise<void> {
    throw this.unimplemented('Location service settings not supported on web.');
  }
}
