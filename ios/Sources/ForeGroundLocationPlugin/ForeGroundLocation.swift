import Foundation
import CoreLocation

@objc public class ForeGroundLocation: NSObject, CLLocationManagerDelegate {
    private var locationManager: CLLocationManager?
    private var locationCompletion: ((Result<CLLocation, Error>) -> Void)?
    
    public func getCurrentLocation(completion: @escaping (Result<CLLocation, Error>) -> Void) {
        locationManager = CLLocationManager()
        locationManager?.delegate = self
        self.locationCompletion = completion
        
        guard CLLocationManager.locationServicesEnabled() else {
            completion(.failure(LocationError.locationServicesDisabled))
            return
        }
        
        let authStatus = CLLocationManager.authorizationStatus()
        guard authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways else {
            completion(.failure(LocationError.permissionDenied))
            return
        }
        
        locationManager?.desiredAccuracy = kCLLocationAccuracyBest
        locationManager?.requestLocation()
    }
    
    // MARK: - CLLocationManagerDelegate
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first else { return }
        locationCompletion?(.success(location))
        locationCompletion = nil
        
        // Clean up
        locationManager?.delegate = nil
        locationManager = nil
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        locationCompletion?(.failure(error))
        locationCompletion = nil
        
        // Clean up
        locationManager?.delegate = nil
        locationManager = nil
    }
    
    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        // Handle authorization changes if needed
        switch status {
        case .denied, .restricted:
            locationCompletion?(.failure(LocationError.permissionDenied))
            locationCompletion = nil
        default:
            break
        }
    }
}

enum LocationError: Error {
    case locationServicesDisabled
    case permissionDenied
    case locationUnavailable
    
    var localizedDescription: String {
        switch self {
        case .locationServicesDisabled:
            return "Location services are disabled"
        case .permissionDenied:
            return "Location permission denied"
        case .locationUnavailable:
            return "Location unavailable"
        }
    }
}
