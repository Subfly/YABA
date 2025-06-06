//
//  AppDelegate.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//


import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
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
        }
        completionHandler()
    }

    // Optional: handle notification while app is foregrounded
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}

extension Notification.Name {
    static let didReceiveDeepLink = Notification.Name("didReceiveDeepLink")
}
