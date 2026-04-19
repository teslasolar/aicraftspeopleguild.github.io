import Foundation
import Security

/// iOS Keychain-backed storage for the GitHub PAT. Lives outside
/// iCloud sync (kSecAttrSynchronizable = false) so it doesn't leave
/// the device.
final class TokenStore {
    private let service = "com.aicraftspeopleguild.acg"
    private let account = "gh_pat"

    var token: String? {
        var query: [String: Any] = [
            kSecClass as String:            kSecClassGenericPassword,
            kSecAttrService as String:      service,
            kSecAttrAccount as String:      account,
            kSecReturnData as String:       true,
            kSecMatchLimit as String:       kSecMatchLimitOne,
        ]
        var out: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &out)
        guard status == errSecSuccess, let data = out as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func save(_ value: String?) {
        delete()
        guard let v = value, !v.isEmpty else { return }
        let add: [String: Any] = [
            kSecClass as String:            kSecClassGenericPassword,
            kSecAttrService as String:      service,
            kSecAttrAccount as String:      account,
            kSecValueData as String:        Data(v.utf8),
            kSecAttrAccessible as String:   kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        SecItemAdd(add as CFDictionary, nil)
    }

    func delete() {
        let q: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(q as CFDictionary)
    }

    var hasToken: Bool { token != nil }
}
