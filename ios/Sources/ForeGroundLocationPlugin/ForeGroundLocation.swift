import Foundation
import CoreLocation

@objc public class ForeGroundLocation: NSObject {
    private var locationManager: CLLocationManager?
    
    public func getCurrentLocation(completion: @escaping (Result<CLLocation, Error>) -> Void) {
        locationManager = CLLocationManager()
        
        guard CLLocationManager.locationServicesEnabled() else {
            completion(.failure(LocationError.locationServicesDisabled))
            return
        }
        
        let authStatus = CLLocationManager.authorizationStatus()
        guard authStatus == .authorizedWhenInUse || authStatus == .authorizedAlways else {
            completion(.failure(LocationError.permissionDenied))
            return
        }
        
        locationManager?.requestLocation()
        
        // Simple implementation - in a real app you'd use proper delegation
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            if let location = self.locationManager?.location {
                completion(.success(location))
            } else {
                completion(.failure(LocationError.locationUnavailable))
            }
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
