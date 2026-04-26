//
//  AppDelegate.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//

import UserNotifications

#if os(iOS)
import UIKit

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    // Called when user taps a notification
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let id = response.notification.request.content.userInfo["id"] as? String,
           let url = URL(string: "yaba://open?id=\(id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didReceiveDeepLink, object: url)
            }
        } else if let collectionId = response.notification.request.content.userInfo["collectionId"] as? String,
                  let url = URL(string: "yaba://collection?id=\(collectionId.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didReceiveDeepLink, object: url)
            }
        }
        completionHandler()
    }

    // Optional: handle notification while app is foregrounded
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}

#elseif os(macOS)

import AppKit

class AppDelegate: NSObject, NSApplicationDelegate, UNUserNotificationCenterDelegate {

    func applicationDidFinishLaunching(_ notification: Notification) {
        UNUserNotificationCenter.current().delegate = self
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let id = response.notification.request.content.userInfo["id"] as? String,
           let url = URL(string: "yaba://open?id=\(id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didReceiveDeepLink, object: url)
            }
        } else if let collectionId = response.notification.request.content.userInfo["collectionId"] as? String,
                  let url = URL(string: "yaba://collection?id=\(collectionId.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .didReceiveDeepLink, object: url)
            }
        }
        completionHandler()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if #available(macOS 12.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.sound, .badge])
        }
    }
}

#endif

extension Notification.Name {
    static let didReceiveDeepLink = Notification.Name("didReceiveDeepLink")
}
