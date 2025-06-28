import Foundation

@objc public class ForeGroundLocation: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
