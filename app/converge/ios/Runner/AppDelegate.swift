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

    // 2. Lê do arquivo .env empacotado como asset da aplicação.
    // No iOS os assets do Flutter ficam em Frameworks/App.framework/flutter_assets;
    // lookupKey(forAsset:) resolve esse caminho relativo ao bundle principal.
    let assetKey = FlutterDartProject.lookupKey(forAsset: ".env")
    guard let filePath = Bundle.main.path(forResource: assetKey, ofType: nil) else {
      NSLog("GOOGLE_MAPS_API_KEY: .env não encontrado no bundle (%@)", assetKey)
      return
    }

    do {
      let content = try String(contentsOfFile: filePath, encoding: .utf8)
      for line in content.components(separatedBy: .newlines) {
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !trimmed.hasPrefix("#") else { continue }
        let parts = trimmed.split(separator: "=", maxSplits: 1).map(String.init)
        guard parts.count == 2,
              parts[0].trimmingCharacters(in: .whitespaces) == "GOOGLE_MAPS_API_KEY" else { continue }
        let key = parts[1].trimmingCharacters(in: .whitespaces)
        if !key.isEmpty {
          GMSServices.provideAPIKey(key)
          return
        }
      }
      NSLog("GOOGLE_MAPS_API_KEY ausente ou vazia no .env")
    } catch {
      NSLog("Erro ao ler GOOGLE_MAPS_API_KEY do .env no AppDelegate: %@", error.localizedDescription)
    }
  }
}
