import Flutter
import UIKit

class SceneDelegate: FlutterSceneDelegate, UIDocumentInteractionControllerDelegate {
  private let shareChannelName = "today_store/share"
  private var documentController: UIDocumentInteractionController?

  override func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    super.scene(scene, willConnectTo: session, options: connectionOptions)

    guard let flutterVC = window?.rootViewController as? FlutterViewController else {
      return
    }

    let channel = FlutterMethodChannel(
      name: shareChannelName,
      binaryMessenger: flutterVC.binaryMessenger
    )

    channel.setMethodCallHandler { [weak self] call, result in
      guard let self = self else {
        result(false)
        return
      }

      switch call.method {
      case "shareImageToInstagram":
        guard let args = call.arguments as? [String: Any],
              let filePath = args["filePath"] as? String
        else {
          result(
            FlutterError(
              code: "INVALID_PATH",
              message: "filePath is required",
              details: nil
            )
          )
          return
        }
        result(self.shareImageToInstagram(filePath: filePath))
      case "isAppInstalled":
        guard let args = call.arguments as? [String: Any],
              let app = args["app"] as? String
        else {
          result(
            FlutterError(
              code: "INVALID_APP",
              message: "app is required",
              details: nil
            )
          )
          return
        }
        result(self.isAppInstalled(app: app))
      case "openApp":
        guard let args = call.arguments as? [String: Any],
              let app = args["app"] as? String
        else {
          result(
            FlutterError(
              code: "INVALID_APP",
              message: "app is required",
              details: nil
            )
          )
          return
        }
        result(self.openApp(app: app))
      default:
        result(FlutterMethodNotImplemented)
      }
    }
  }

  private func shareImageToInstagram(filePath: String) -> Bool {
    let sourceUrl = URL(fileURLWithPath: filePath)
    guard FileManager.default.fileExists(atPath: sourceUrl.path) else {
      return false
    }
    guard let instagramUrl = URL(string: "instagram://app"),
          UIApplication.shared.canOpenURL(instagramUrl)
    else {
      return false
    }

    let tempName = "today_store_instagram_\(Int(Date().timeIntervalSince1970 * 1000)).igo"
    let targetUrl = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(tempName)

    do {
      let data = try Data(contentsOf: sourceUrl)
      try data.write(to: targetUrl, options: .atomic)
    } catch {
      return false
    }

    let controller = UIDocumentInteractionController(url: targetUrl)
    controller.uti = "com.instagram.exclusivegram"
    controller.annotation = ["InstagramCaption": "Today Store에서 만든 콘텐츠예요"]
    controller.delegate = self
    documentController = controller

    guard let rootView = window?.rootViewController?.view else {
      return false
    }

    return controller.presentOpenInMenu(from: rootView.bounds, in: rootView, animated: true)
  }

  private func isAppInstalled(app: String) -> Bool {
    switch app {
    case "instagram":
      guard let url = URL(string: "instagram://app") else { return false }
      return UIApplication.shared.canOpenURL(url)
    case "daangn":
      if let u = URL(string: "daangn://"), UIApplication.shared.canOpenURL(u) { return true }
      if let u = URL(string: "karrot://"), UIApplication.shared.canOpenURL(u) { return true }
      return false
    case "naver":
      return smartPlaceLaunchUrls().contains { UIApplication.shared.canOpenURL($0) }
    default:
      return false
    }
  }

  private func smartPlaceLaunchUrls() -> [URL] {
    ["smartplace://", "naversmartplace://"].compactMap { URL(string: $0) }
  }

  private func openApp(app: String) -> Bool {
    switch app {
    case "naver":
      for url in smartPlaceLaunchUrls() where UIApplication.shared.canOpenURL(url) {
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
        return true
      }
      return false
    default:
      return false
    }
  }

  func documentInteractionControllerViewControllerForPreview(
    _ controller: UIDocumentInteractionController
  ) -> UIViewController {
    return window?.rootViewController ?? UIViewController()
  }
}
