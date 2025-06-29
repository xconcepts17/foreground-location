import XCTest
@testable import ForeGroundLocationPlugin

class ForeGroundLocationTests: XCTestCase {
    func testLocationErrorCases() {
        // Test that LocationError enum has proper descriptions
        let locationServicesDisabledError = LocationError.locationServicesDisabled
        XCTAssertEqual(locationServicesDisabledError.localizedDescription, "Location services are disabled")
        
        let permissionDeniedError = LocationError.permissionDenied
        XCTAssertEqual(permissionDeniedError.localizedDescription, "Location permission denied")
        
        let locationUnavailableError = LocationError.locationUnavailable
        XCTAssertEqual(locationUnavailableError.localizedDescription, "Location unavailable")
    }
    
    func testPluginMethodsRegistration() {
        // Test that plugin has all required methods registered
        let plugin = ForeGroundLocationPlugin()
        let methodNames = plugin.pluginMethods.map { $0.name }
        
        let expectedMethods = [
            "checkPermissions",
            "requestPermissions", 
            "startForegroundLocationService",
            "stopForegroundLocationService",
            "isServiceRunning",
            "getCurrentLocation",
            "updateLocationSettings"
        ]
        
        for method in expectedMethods {
            XCTAssertTrue(methodNames.contains(method), "Missing method: \(method)")
        }
    }
}
