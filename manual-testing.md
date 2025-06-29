# Manual Testing Scenarios for Foreground Location Plugin

This document provides comprehensive manual testing scenarios for the Capacitor Foreground Location Plugin. These tests should be performed after installing the plugin in a test application to ensure all functionality works correctly under various conditions.

## Prerequisites

### Test Environment Setup

- Test device: Android (API 23+) and iOS (13+)
- Test app with the plugin installed and configured
- Remote API endpoint for testing API integration (optional)
- Location testing tools (GPS spoofing apps if needed)
- Network monitoring tools (optional)

### Required Permissions in Test App

**Android**: `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
```

**iOS**: `Info.plist`

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>This app needs location access to track your location</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>This app needs location access continuously</string>
```

---

## Test Scenario 1: Initial Installation and Setup

### Scenario 1.1: Fresh Installation

**Objective**: Verify plugin installs correctly and initializes properly

**Steps**:

1. Install plugin: `npm install foreground-location`
2. Sync with native platforms: `npx cap sync`
3. Build and run the test app
4. Import plugin in your test component
5. Call `ForeGroundLocation.checkPermissions()`

**Expected Results**:

- Plugin installs without errors
- App builds successfully
- Plugin is accessible in JavaScript
- `checkPermissions()` returns permission status object

**Pass/Fail Criteria**:

- ✅ Pass: Plugin imports successfully, methods are accessible
- ❌ Fail: Import errors, undefined methods, build failures

---

## Test Scenario 2: Permission Management

### Scenario 2.1: Initial Permission Check

**Objective**: Verify permission status checking works correctly

**Steps**:

1. On fresh install, call `checkPermissions()`
2. Note the returned permission states
3. Verify all required permissions are included in response

**Expected Results**:

```javascript
{
  location: "prompt" | "denied" | "granted",
  backgroundLocation: "prompt" | "denied" | "granted",
  notifications: "prompt" | "denied" | "granted"
}
```

### Scenario 2.2: Permission Request Flow

**Objective**: Test requesting permissions from user

**Steps**:

1. Call `requestPermissions()`
2. Handle system permission dialogs appropriately
3. Grant location permissions when prompted
4. Check permission status after granting

**Expected Results**:

- System permission dialogs appear
- Permission status updates correctly after user action
- Background location prompt appears on Android 10+ after location granted

### Scenario 2.3: Permission Denial Handling

**Objective**: Test behavior when permissions are denied

**Steps**:

1. Call `requestPermissions()`
2. Deny location permissions in system dialog
3. Try to start location service
4. Verify appropriate error handling

**Expected Results**:

- Service start fails with `PERMISSION_DENIED` error
- App doesn't crash
- User receives appropriate error message

---

## Test Scenario 3: Basic Location Service Operations

### Scenario 3.1: Start Location Service - Basic Configuration

**Objective**: Test starting foreground location service with minimal config

**Test Code**:

```javascript
const options = {
  interval: 60000,
  fastestInterval: 30000,
  priority: 'HIGH_ACCURACY',
  notification: {
    title: 'Location Tracking',
    text: 'App is tracking your location',
  },
};

await ForeGroundLocation.startForegroundLocationService(options);
```

**Steps**:

1. Ensure permissions are granted
2. Call `startForegroundLocationService()` with basic options
3. Check system notification area
4. Verify service status with `isServiceRunning()`

**Expected Results**:

- Service starts successfully
- Persistent notification appears
- `isServiceRunning()` returns `{ isRunning: true }`
- Location updates begin

### Scenario 3.2: Stop Location Service

**Objective**: Test stopping the foreground location service

**Steps**:

1. Start location service (from 3.1)
2. Call `stopForegroundLocationService()`
3. Check notification disappears
4. Verify service status

**Expected Results**:

- Service stops successfully
- Notification is removed
- `isServiceRunning()` returns `{ isRunning: false }`
- Location updates stop

### Scenario 3.3: Service Status Monitoring

**Objective**: Test service status checking functionality

**Steps**:

1. Check status when service is stopped
2. Start service and check status
3. Stop service and check status again

**Expected Results**:

- Status accurately reflects current service state
- Status changes appropriately with service operations

---

## Test Scenario 4: Location Data and Event Listeners

### Scenario 4.1: Location Update Events

**Objective**: Test receiving location updates via event listeners

**Test Code**:

```javascript
const locationListener = await ForeGroundLocation.addListener('locationUpdate', (location) => {
  console.log('Location update:', location);
  // Verify location data structure
});
```

**Steps**:

1. Set up location update listener
2. Start location service
3. Move around or use GPS spoofing
4. Monitor location updates in console/UI

**Expected Results**:

- Location updates received regularly
- Location data includes: latitude, longitude, accuracy, timestamp
- Optional fields (altitude, bearing, speed) present when available
- Updates occur at specified interval

### Scenario 4.2: Service Status Change Events

**Objective**: Test service status change notifications

**Test Code**:

```javascript
const statusListener = await ForeGroundLocation.addListener('serviceStatusChanged', (status) => {
  console.log('Service status changed:', status);
});
```

**Steps**:

1. Set up service status listener
2. Start location service
3. Stop location service
4. Force stop service externally (if possible)

**Expected Results**:

- Status change events received when service starts/stops
- Error information included when service fails

### Scenario 4.3: Get Current Location

**Objective**: Test one-time location retrieval

**Steps**:

1. Ensure location permissions granted
2. Call `getCurrentLocation()`
3. Verify returned location data

**Expected Results**:

- Returns single location reading
- Location data format matches event updates
- Works independently of foreground service

---

## Test Scenario 5: App Lifecycle and Resilience

### Scenario 5.1: App Backgrounding

**Objective**: Test behavior when app goes to background

**Steps**:

1. Start location service
2. Press home button to background the app
3. Check notification remains visible
4. Return to app after 2-3 minutes
5. Verify location updates continued

**Expected Results**:

- Service continues running in background
- Notification remains persistent
- Location updates continue while backgrounded
- App receives all updates when returned to foreground

### Scenario 5.2: App Force Close

**Objective**: Test service behavior when app is force closed

**Steps**:

1. Start location service
2. Force close app through system settings or task manager
3. Check if notification remains
4. Restart app
5. Check service status

**Expected Results**:

- **Android**: Service may continue briefly, then stop (expected behavior)
- **iOS**: Service stops when app is terminated (expected behavior)
- App handles restart gracefully
- Service status accurately reflects state

### Scenario 5.3: System Restart

**Objective**: Test behavior after device restart

**Steps**:

1. Start location service
2. Restart device
3. Launch app
4. Check service status
5. Restart service if needed

**Expected Results**:

- Service doesn't auto-restart (expected)
- App detects service is stopped
- Can restart service without issues

---

## Test Scenario 6: Network and API Integration

### Scenario 6.1: API Service Configuration

**Objective**: Test location data API integration

**Test Code**:

```javascript
const options = {
  interval: 30000,
  notification: {
    title: 'Location Tracking',
    text: 'Sending location data to server',
  },
  api: {
    url: 'https://your-test-api.com/locations',
    type: 'POST',
    header: {
      Authorization: 'Bearer your-token',
      'Content-Type': 'application/json',
    },
    additionalParams: {
      userId: '12345',
      sessionId: 'test-session',
    },
    apiInterval: 2, // Send every 2 minutes
  },
};
```

**Steps**:

1. Configure API endpoint for testing
2. Start service with API configuration
3. Monitor API calls on server side
4. Check API service status with `getApiServiceStatus()`

**Expected Results**:

- Location data batched and sent to API
- API requests include configured headers and parameters
- Batch interval respected (data sent every 2 minutes)
- API service status shows enabled and healthy

### Scenario 6.2: API Network Failure Handling

**Objective**: Test behavior when API calls fail

**Steps**:

1. Start service with API configuration
2. Disable internet connection
3. Wait for several location updates
4. Re-enable internet
5. Check API service status and buffer

**Expected Results**:

- Location data buffers when network unavailable
- API service enters unhealthy state
- Data resends when network restored
- Circuit breaker pattern activates after failures

### Scenario 6.3: API Service Management

**Objective**: Test API service control methods

**Steps**:

1. Start service with API enabled
2. Check `getApiServiceStatus()`
3. Call `clearApiBuffers()`
4. Call `resetApiCircuitBreaker()` if needed

**Expected Results**:

- Status accurately reflects API service state
- Buffer clearing works correctly
- Circuit breaker reset restores API service

---

## Test Scenario 7: Error Handling and Edge Cases

### Scenario 7.1: Invalid Configuration

**Objective**: Test error handling for invalid parameters

**Test Cases**:

```javascript
// Missing notification
const invalidOptions1 = {
  interval: 60000,
  // Missing notification
};

// Invalid interval
const invalidOptions2 = {
  interval: 500, // Too short
  notification: { title: 'Test', text: 'Test' },
};

// Invalid API URL
const invalidOptions3 = {
  interval: 60000,
  notification: { title: 'Test', text: 'Test' },
  api: {
    url: 'invalid-url',
  },
};
```

**Steps**:

1. Try starting service with each invalid configuration
2. Verify appropriate error messages returned

**Expected Results**:

- Service start fails with descriptive error codes
- App doesn't crash
- Error codes match plugin documentation

### Scenario 7.2: Location Services Disabled

**Objective**: Test behavior when device location services are disabled

**Steps**:

1. Disable location services in device settings
2. Try to start location service
3. Enable location services
4. Try again

**Expected Results**:

- Service fails with `LOCATION_SERVICES_DISABLED` error
- Works correctly once location services enabled

### Scenario 7.3: Memory and Resource Management

**Objective**: Test plugin behavior under resource constraints

**Steps**:

1. Run location service for extended period (2+ hours)
2. Monitor memory usage
3. Generate many rapid location updates
4. Check for memory leaks or crashes

**Expected Results**:

- Memory usage remains stable
- No memory leaks over time
- Performance remains consistent

---

## Test Scenario 8: Different Location Priorities and Settings

### Scenario 8.1: High Accuracy Mode

**Objective**: Test high accuracy location tracking

**Test Code**:

```javascript
const highAccuracyOptions = {
  interval: 10000,
  fastestInterval: 5000,
  priority: 'HIGH_ACCURACY',
  enableHighAccuracy: true,
  distanceFilter: 1, // 1 meter
  notification: {
    title: 'High Accuracy Tracking',
    text: 'GPS active for precise location',
  },
};
```

**Steps**:

1. Start service with high accuracy settings
2. Monitor location accuracy values
3. Check battery usage
4. Compare with other priority modes

**Expected Results**:

- Higher accuracy readings (lower accuracy values)
- More frequent updates
- Higher battery consumption

### Scenario 8.2: Battery Optimized Mode

**Objective**: Test power-efficient location tracking

**Test Code**:

```javascript
const batteryOptimizedOptions = {
  interval: 300000, // 5 minutes
  fastestInterval: 180000, // 3 minutes
  priority: 'LOW_POWER',
  enableHighAccuracy: false,
  distanceFilter: 100, // 100 meters
  notification: {
    title: 'Background Tracking',
    text: 'Efficient location tracking active',
  },
};
```

**Steps**:

1. Start service with battery optimized settings
2. Monitor location accuracy and frequency
3. Check battery usage over time

**Expected Results**:

- Less frequent updates
- Lower accuracy readings
- Better battery efficiency

### Scenario 8.3: Update Location Settings

**Objective**: Test dynamic configuration updates

**Steps**:

1. Start service with initial configuration
2. Call `updateLocationSettings()` with new parameters
3. Verify settings change takes effect

**Expected Results**:

- Service adapts to new settings without restart
- Update intervals change accordingly
- No service interruption

---

## Test Scenario 9: Platform-Specific Testing

### Scenario 9.1: Android-Specific Features

**Objective**: Test Android-specific functionality

**Tests**:

1. **Background Location Permission** (Android 10+):
   - Test background permission request flow
   - Verify service works with background permission
2. **Notification Permissions** (Android 13+):
   - Test notification permission request
   - Verify service notification appears
3. **Battery Optimization**:
   - Test with battery optimization enabled/disabled
   - Verify Doze mode behavior

4. **Notification Customization**:
   - Test custom notification icons
   - Test notification channel behavior

### Scenario 9.2: iOS-Specific Features

**Objective**: Test iOS-specific functionality

**Tests**:

1. **Location Authorization**:
   - Test "When In Use" vs "Always" authorization
   - Verify background location tracking
2. **App Background Modes**:
   - Test location updates in background
   - Verify background app refresh settings impact
3. **iOS Location Accuracy**:
   - Test reduced accuracy mode (iOS 14+)
   - Verify precise location permission

---

## Test Scenario 10: Stress Testing and Performance

### Scenario 10.1: Extended Operation

**Objective**: Test plugin stability over extended periods

**Steps**:

1. Start location service
2. Run for 24+ hours continuously
3. Monitor memory usage, battery drain, accuracy
4. Check for any service failures or restarts

**Expected Results**:

- Service runs stably for extended period
- No memory leaks or performance degradation
- Consistent location accuracy over time

### Scenario 10.2: Rapid Configuration Changes

**Objective**: Test rapid start/stop/reconfigure operations

**Steps**:

1. Rapidly start and stop service (10+ times)
2. Quickly change configuration settings
3. Add/remove event listeners frequently

**Expected Results**:

- No crashes or memory leaks
- Service responds correctly to all operations
- Event listeners work properly

### Scenario 10.3: Multiple Event Listeners

**Objective**: Test multiple simultaneous event listeners

**Steps**:

1. Add multiple location update listeners
2. Add multiple service status listeners
3. Verify all listeners receive events
4. Remove listeners individually

**Expected Results**:

- All listeners receive events simultaneously
- Removing one listener doesn't affect others
- No memory leaks from unused listeners

---

## Test Scenario 11: Notification Interaction

### Scenario 11.1: Notification Behavior

**Objective**: Test foreground service notification

**Steps**:

1. Start location service
2. Check notification appears in status bar
3. Tap notification to open app
4. Try to dismiss notification
5. Check notification persists during service operation

**Expected Results**:

- Notification appears immediately when service starts
- Tapping notification opens the app
- Notification cannot be dismissed while service running
- Notification shows correct title and text

### Scenario 11.2: Custom Notification Icons

**Objective**: Test custom notification icon (Android)

**Test Code**:

```javascript
const options = {
  interval: 60000,
  notification: {
    title: 'Custom Icon Test',
    text: 'Testing custom notification icon',
    icon: 'ic_location_tracking', // Custom icon in res/drawable
  },
};
```

**Steps**:

1. Add custom icon to Android res/drawable
2. Start service with custom icon configuration
3. Check notification displays custom icon

**Expected Results**:

- Custom icon appears in notification
- Falls back to default icon if custom icon not found

---

## Test Scenario 12: Data Validation and Security

### Scenario 12.1: Location Data Validation

**Objective**: Ensure location data integrity and format

**Steps**:

1. Collect location updates for various scenarios
2. Validate data types and format
3. Check timestamp format (ISO 8601)
4. Verify coordinate ranges

**Expected Results**:

- Latitude: -90 to 90 degrees
- Longitude: -180 to 180 degrees
- Accuracy: positive number in meters
- Timestamp: valid ISO 8601 format with timezone
- Optional fields only present when available

### Scenario 12.2: API Data Security

**Objective**: Test secure transmission of location data

**Steps**:

1. Configure API with HTTPS endpoint
2. Monitor network traffic
3. Verify location data transmission
4. Check authentication headers included

**Expected Results**:

- All API calls use HTTPS
- Authentication headers properly included
- Location data properly formatted in requests
- No sensitive data exposed in logs

---

## Test Reporting Template

For each test scenario, document results using this template:

### Test Case: [Scenario Name]

- **Date**: [Test Date]
- **Platform**: [Android/iOS Version]
- **Device**: [Device Model]
- **Plugin Version**: [Version Number]
- **Status**: ✅ PASS / ❌ FAIL / ⚠️ PARTIAL
- **Notes**: [Any observations, issues, or recommendations]
- **Screenshots**: [If applicable]

---

## Common Issues and Troubleshooting

### Issue: Service not starting

**Possible Causes**:

- Missing permissions
- Invalid notification configuration
- Location services disabled

**Debugging Steps**:

1. Check `checkPermissions()` result
2. Verify notification object is complete
3. Check device location settings

### Issue: Location updates not received

**Possible Causes**:

- Event listener not set up correctly
- App in doze mode (Android)
- Location accuracy too high for environment

**Debugging Steps**:

1. Verify event listener setup
2. Check device battery optimization settings
3. Test in open area with clear GPS signal

### Issue: API calls failing

**Possible Causes**:

- Network connectivity issues
- Invalid API configuration
- Server endpoint problems

**Debugging Steps**:

1. Check `getApiServiceStatus()`
2. Verify API endpoint accessibility
3. Monitor network logs
4. Test API endpoint independently

---

## Test Completion Checklist

- [ ] All permission scenarios tested
- [ ] Basic service operations verified
- [ ] Event listeners working correctly
- [ ] App lifecycle scenarios covered
- [ ] API integration tested (if applicable)
- [ ] Error handling verified
- [ ] Platform-specific features tested
- [ ] Performance and stress tests completed
- [ ] Notification behavior verified
- [ ] Data validation confirmed
- [ ] Documentation updated with any issues found

---

## Notes

- Perform tests on both physical devices and emulators when possible
- Test on different Android API levels and iOS versions
- Document any platform-specific behaviors or limitations
- Report bugs with detailed reproduction steps
- Consider automated testing for regression prevention

This comprehensive testing suite ensures the Foreground Location Plugin works reliably across all supported scenarios and edge cases.
