import { ForeGroundLocation } from './src/index';

/**
 * Example usage of the Foreground Location Plugin
 * This demonstrates the complete workflow for Android location tracking
 */

class LocationTracker {
  private isTracking = false;
  private locationListener: any;
  private statusListener: any;

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
          icon: 'ic_location'
        },
        enableHighAccuracy: true,
        distanceFilter: 10 // Only update if moved 10+ meters
      });

      this.isTracking = true;
      console.log('Location tracking started successfully');

    } catch (error) {
      console.error('Failed to start location tracking:', error);
      throw error;
    }
  }

  /**
   * Stop location tracking and clean up
   */
  async stopLocationTracking() {
    try {
      console.log('Stopping location tracking...');

      // Stop the foreground service
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

      this.isTracking = false;
      console.log('Location tracking stopped');

    } catch (error) {
      console.error('Failed to stop location tracking:', error);
      throw error;
    }
  }

  /**
   * Get current location once without starting the service
   */
  async getCurrentPosition() {
    try {
      const location = await ForeGroundLocation.getCurrentLocation();
      console.log('Current location:', location);
      return location;
    } catch (error) {
      console.error('Failed to get current location:', error);
      throw error;
    }
  }

  /**
   * Check if the location service is currently running
   */
  async checkServiceStatus() {
    try {
      const status = await ForeGroundLocation.isServiceRunning();
      console.log('Service status:', status);
      return status.isRunning;
    } catch (error) {
      console.error('Failed to check service status:', error);
      return false;
    }
  }

  /**
   * Update the location service configuration
   */
  async updateTrackingSettings(newSettings: any) {
    try {
      await ForeGroundLocation.updateLocationSettings(newSettings);
      console.log('Location settings updated');
    } catch (error) {
      console.error('Failed to update settings:', error);
      throw error;
    }
  }

  /**
   * Set up event listeners for location updates and service status
   */
  private async setupListeners() {
    // Listen for location updates
    this.locationListener = await ForeGroundLocation.addListener(
      'locationUpdate',
      (location) => {
        console.log('📍 New location received:', {
          latitude: location.latitude,
          longitude: location.longitude,
          accuracy: location.accuracy,
          timestamp: new Date(location.timestamp).toLocaleString()
        });

        // Handle the location update
        this.handleLocationUpdate(location);
      }
    );

    // Listen for service status changes
    this.statusListener = await ForeGroundLocation.addListener(
      'serviceStatusChanged',
      (status) => {
        console.log('🔄 Service status changed:', status);

        if (status.error) {
          console.error('Service error:', status.error);
          this.handleServiceError(status.error);
        }

        this.handleStatusChange(status);
      }
    );

    console.log('Event listeners set up successfully');
  }

  /**
   * Handle incoming location updates
   */
  private handleLocationUpdate(location: any) {
    // Example: Update UI, send to server, store locally, etc.

    // Update UI (if applicable)
    this.updateLocationUI(location);

    // Send to server (if applicable)
    this.sendLocationToServer(location);

    // Store locally (if applicable)
    this.storeLocationLocally(location);
  }

  /**
   * Handle service status changes
   */
  private handleStatusChange(status: any) {
    if (status.isRunning) {
      console.log('✅ Location service is running');
    } else {
      console.log('❌ Location service stopped');
      this.isTracking = false;
    }
  }

  /**
   * Handle service errors
   */
  private handleServiceError(error: string) {
    console.error('Service error occurred:', error);

    // Handle specific error types
    if (error.includes('permission')) {
      console.log('Permission issue detected');
      // Maybe show permission dialog or guide user to settings
    } else if (error.includes('location')) {
      console.log('Location unavailable');
      // Maybe show message to enable location services
    }
  }

  /**
   * Update UI with new location (placeholder)
   */
  private updateLocationUI(location: any) {
    // Example implementation - update your app's UI
    console.log('Updating UI with location:', location);
  }

  /**
   * Send location to server (placeholder)
   */
  private sendLocationToServer(location: any) {
    // Example implementation - send to your backend
    console.log('Sending location to server:', location);

    // Example POST request
    /*
    fetch('/api/location', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(location)
    });
    */
  }

  /**
   * Store location locally (placeholder)
   */
  private storeLocationLocally(location: any) {
    // Example implementation - store in local storage or database
    console.log('Storing location locally:', location);

    // Example local storage
    /*
    const locations = JSON.parse(localStorage.getItem('locations') || '[]');
    locations.push(location);
    localStorage.setItem('locations', JSON.stringify(locations));
    */
  }

  /**
   * Get tracking status
   */
  get trackingStatus() {
    return this.isTracking;
  }
}

// Export for use
export default LocationTracker;

// Example usage in an Ionic/Angular app:
/*
import LocationTracker from './location-tracker';

export class HomePage {
  private locationTracker = new LocationTracker();

  async startTracking() {
    try {
      await this.locationTracker.startLocationTracking();
      // Update UI to show tracking is active
    } catch (error) {
      // Show error message to user
      console.error('Failed to start tracking:', error);
    }
  }

  async stopTracking() {
    try {
      await this.locationTracker.stopLocationTracking();
      // Update UI to show tracking is stopped
    } catch (error) {
      console.error('Failed to stop tracking:', error);
    }
  }

  async getCurrentLocation() {
    try {
      const location = await this.locationTracker.getCurrentPosition();
      // Use the location
      console.log('Current location:', location);
    } catch (error) {
      console.error('Failed to get location:', error);
    }
  }
}
*/
