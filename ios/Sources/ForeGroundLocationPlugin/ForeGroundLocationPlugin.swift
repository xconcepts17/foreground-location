import Foundation
import Capacitor
import CoreLocation

/**
 * Foreground Location Plugin for iOS
 * Limited iOS implementation - Foreground service pattern not available
 */
@objc(ForeGroundLocationPlugin)
public class ForeGroundLocationPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ForeGroundLocationPlugin"
    public let jsName = "ForeGroundLocation"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startForegroundLocationService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopForegroundLocationService", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isServiceRunning", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCurrentLocation", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "updateLocationSettings", returnType: CAPPluginReturnPromise)
    ]
    
    private let implementation = ForeGroundLocation()

    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        let locationManager = CLLocationManager()
        let authStatus = locationManager.authorizationStatus
        
        let permissionState: String
        switch authStatus {
        case .notDetermined:
            permissionState = "prompt"
        case .denied, .restricted:
            permissionState = "denied"
        case .authorizedWhenInUse, .authorizedAlways:
            permissionState = "granted"
        @unknown default:
            permissionState = "prompt"
        }
        
        call.resolve([
            "location": permissionState,
            "backgroundLocation": permissionState == "granted" && authStatus == .authorizedAlways ? "granted" : "denied",
            "notifications": "denied"
        ])
    }

    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        let locationManager = CLLocationManager()
        
        switch locationManager.authorizationStatus {
        case .notDetermined:
            // For iOS, we can only request when-in-use permission
            // Background location requires additional setup in Info.plist
            locationManager.requestWhenInUseAuthorization()
            
            // Since we can't easily wait for the delegate callback in this context,
            // we'll return the current state and recommend checking again
            call.resolve([
                "location": "prompt",
                "backgroundLocation": "denied", 
                "notifications": "denied"
            ])
        case .denied, .restricted:
            call.resolve([
                "location": "denied",
                "backgroundLocation": "denied",
                "notifications": "denied"
            ])
        case .authorizedWhenInUse:
            call.resolve([
                "location": "granted",
                "backgroundLocation": "denied", // Would need always authorization
                "notifications": "denied"
            ])
        case .authorizedAlways:
            call.resolve([
                "location": "granted",
                "backgroundLocation": "granted",
                "notifications": "denied"
            ])
        @unknown default:
            call.resolve([
                "location": "prompt",
                "backgroundLocation": "denied",
                "notifications": "denied"
            ])
        }
    }

    @objc func startForegroundLocationService(_ call: CAPPluginCall) {
        call.unimplemented("Foreground location service pattern not available on iOS. Use iOS background location modes instead.")
    }

    @objc func stopForegroundLocationService(_ call: CAPPluginCall) {
        call.unimplemented("Foreground location service pattern not available on iOS.")
    }

    @objc func isServiceRunning(_ call: CAPPluginCall) {
        call.resolve(["isRunning": false])
    }

    @objc func getCurrentLocation(_ call: CAPPluginCall) {
        implementation.getCurrentLocation { result in
            switch result {
            case .success(let location):
                call.resolve([
                    "latitude": location.coordinate.latitude,
                    "longitude": location.coordinate.longitude,
                    "accuracy": location.horizontalAccuracy,
                    "altitude": location.altitude,
                    "bearing": location.course >= 0 ? location.course : nil,
                    "speed": location.speed >= 0 ? location.speed : nil,
                    "timestamp": ISO8601DateFormatter().string(from: location.timestamp)
                ])
            case .failure(let error):
                call.reject("Location error: \(error.localizedDescription)")
            }
        }
    }

    @objc func updateLocationSettings(_ call: CAPPluginCall) {
        call.unimplemented("Location service settings not supported on iOS.")
    }
}
