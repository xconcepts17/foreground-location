# Quick Setup Guide - Foreground Location Plugin

## 🚀 Quick Start (5 Minutes)

### Step 1: Install the Plugin

```bash
npm install foreground-location-plugin
npx cap sync
```

### Step 2: Update Android Manifest

Add to your `android/app/src/main/AndroidManifest.xml` (before `<application>`):

```xml
<!-- Location permissions -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

Add inside `<application>`:

```xml
<service
    android:name="in.xconcepts.foreground.location.LocationForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="location" />
```

### Step 3: Update build.gradle

Add to your `android/app/build.gradle` dependencies:

```gradle
implementation 'com.google.android.gms:play-services-location:21.0.1'
```

### Step 4: Basic Implementation

```typescript
import { ForeGroundLocation } from 'foreground-location-plugin';

// Request permissions
const permissions = await ForeGroundLocation.requestPermissions();

// Start tracking
await ForeGroundLocation.startForegroundLocationService({
  interval: 60000,
  priority: 'HIGH_ACCURACY',
  notification: {
    title: 'Location Tracking',
    text: 'Tracking your location',
  },
});

// Listen for updates
const listener = await ForeGroundLocation.addListener('locationUpdate', (location) => {
  console.log('New location:', location);
});
```

## ⚠️ Important Notes

1. **Android Only**: This plugin only works on Android. iOS and Web will show "service not available"
2. **Google Play Policy**: Background location requires special approval
3. **Battery Usage**: High accuracy mode uses more battery
4. **Persistent Notification**: Required by Android for foreground services

## 🔧 Testing

1. Test on physical Android device (not emulator)
2. Grant all location permissions when prompted
3. Check notification appears when service starts
4. Verify location updates in console

## 📱 Platform Support

- ✅ Android (API 23+)
- ❌ iOS (use native iOS background location)
- ❌ Web (use Geolocation API)

## 🆘 Common Issues

**Service not starting?**

- Check all permissions granted
- Verify manifest configuration
- Ensure Google Play Services available

**No location updates?**

- Enable device location services
- Check location permissions
- Try different accuracy priority

**Permission denied?**

- Request permissions in correct order
- Handle rationale explanations
- Guide users to app settings

## 📚 Full Documentation

See README.md for complete API documentation and advanced usage examples.
