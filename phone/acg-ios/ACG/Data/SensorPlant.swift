import Foundation
import CoreMotion

/// iOS L0/L1 plant — CoreMotion-backed mirror of Android's SensorPlant.
/// Publishes each reading on sensor.<kind>.value via the shared Mqtt.
@MainActor
final class SensorPlant: ObservableObject {
    struct Reading: Identifiable {
        var id: String { kind }
        let kind: String
        let name: String
        let unit: String
        var values: [Double]
        var ts: Date
    }

    @Published private(set) var readings: [String: Reading] = [:]
    private let motion = CMMotionManager()
    private let pedometer = CMPedometer()

    init() {
        seed()
    }

    func start() {
        if motion.isAccelerometerAvailable {
            motion.accelerometerUpdateInterval = 0.1
            motion.startAccelerometerUpdates(to: .main) { [weak self] d, _ in
                guard let self, let a = d?.acceleration else { return }
                self.update("accelerometer", values: [a.x, a.y, a.z])
            }
        }
        if motion.isGyroAvailable {
            motion.gyroUpdateInterval = 0.1
            motion.startGyroUpdates(to: .main) { [weak self] d, _ in
                guard let self, let r = d?.rotationRate else { return }
                self.update("gyroscope", values: [r.x, r.y, r.z])
            }
        }
        if motion.isMagnetometerAvailable {
            motion.magnetometerUpdateInterval = 0.2
            motion.startMagnetometerUpdates(to: .main) { [weak self] d, _ in
                guard let self, let m = d?.magneticField else { return }
                self.update("magnetic_field", values: [m.x, m.y, m.z])
            }
        }
        if motion.isDeviceMotionAvailable {
            motion.deviceMotionUpdateInterval = 0.2
            motion.startDeviceMotionUpdates(to: .main) { [weak self] d, _ in
                guard let self, let g = d?.gravity else { return }
                self.update("gravity", values: [g.x, g.y, g.z])
            }
        }
        if CMPedometer.isStepCountingAvailable() {
            pedometer.startUpdates(from: Date()) { [weak self] data, _ in
                guard let self, let n = data?.numberOfSteps else { return }
                Task { @MainActor in self.update("step_counter", values: [n.doubleValue]) }
            }
        }
    }

    func stop() {
        motion.stopAccelerometerUpdates()
        motion.stopGyroUpdates()
        motion.stopMagnetometerUpdates()
        motion.stopDeviceMotionUpdates()
        pedometer.stopUpdates()
    }

    // MARK: - helpers
    private func seed() {
        let defs: [(String, String, String)] = [
            ("accelerometer",   "Accelerometer",    "m/s²"),
            ("gyroscope",       "Gyroscope",        "rad/s"),
            ("magnetic_field",  "Magnetometer",     "µT"),
            ("gravity",         "Gravity",          "m/s²"),
            ("step_counter",    "Step counter",     "steps"),
        ]
        for (k, n, u) in defs {
            readings[k] = Reading(kind: k, name: n, unit: u, values: [], ts: .distantPast)
        }
    }

    private func update(_ kind: String, values: [Double]) {
        if var r = readings[kind] {
            r.values = values; r.ts = Date(); readings[kind] = r
            Mqtt.shared.publish(topic: "sensor.\(kind).value", value: values, retained: true, from: "plant")
        }
    }
}
