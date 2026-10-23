import Flutter
import UIKit
import GoogleMaps

@main
@objc class AppDelegate: FlutterAppDelegate, FlutterImplicitEngineDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    provideGoogleMapsApiKey()
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

  func didInitializeImplicitFlutterEngine(_ engineBridge: FlutterImplicitEngineBridge) {
    GeneratedPluginRegistrant.register(with: engineBridge.pluginRegistry)
  }

  private func provideGoogleMapsApiKey() {
    // 1. Tenta obter de variável de processo/ambiente se definida
    if let envKey = ProcessInfo.processInfo.environment["GOOGLE_MAPS_API_KEY"], !envKey.isEmpty {
      GMSServices.provideAPIKey(envKey)
      return
    }

    // 2. Lê do arquivo .env empacotado como asset da aplicação
    let possiblePaths = [
      Bundle.main.path(forResource: ".env", ofType: nil, inDirectory: "flutter_assets"),
      Bundle.main.path(forResource: ".env", ofType: nil),
      Bundle.main.bundlePath + "/flutter_assets/.env"
    ]

    for path in possiblePaths {
      guard let filePath = path, FileManager.default.fileExists(atPath: filePath) else { continue }
      do {
        let content = try String(contentsOfFile: filePath, encoding: .utf8)
        let lines = content.components(separatedBy: .newlines)
        for line in lines {
          let trimmed = line.trimmingCharacters(in: .whitespaces)
          if !trimmed.isEmpty && !trimmed.hasPrefix("#") && trimmed.contains("=") {
            let parts = trimmed.split(separator: "=", maxSplits: 1).map(String.init)
            if parts.count == 2 && parts[0].trimmingCharacters(in: .whitespaces) == "GOOGLE_MAPS_API_KEY" {
              let key = parts[1].trimmingCharacters(in: .whitespaces)
              if !key.isEmpty {
                GMSServices.provideAPIKey(key)
                return
              }
            }
          }
        }
      } catch {
        NSLog("Erro ao ler GOOGLE_MAPS_API_KEY do .env no AppDelegate: %@", error.localizedDescription)
      }
    }
  }
}
