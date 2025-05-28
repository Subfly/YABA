//
//  SyncServer.swift
//  YABA
//
//  Created by Ali Taha on 26.05.2025.
//

import Foundation
import UIKit

final class SyncServer: NSObject, NetServiceDelegate {
    private var service: NetService?

    func startAdvertising() {
        let serviceName = UIDevice.current.name.replacingOccurrences(of: " ", with: "-")
        service = NetService(
            domain: "local.",
            type: "_yaba-sync._tcp.",
            name: serviceName,
            port: Int32(Constants.port)
        )
        service?.delegate = self
        service?.publish()
    }

    func stopAdvertising() {
        service?.stop()
        service = nil
    }
    
    func getLocalIPAddress() -> String? {
        var address: String?

        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddr) == 0 else { return nil }

        defer { freeifaddrs(ifaddr) }

        var ptr = ifaddr
        while ptr != nil {
            let interface = ptr!.pointee

            let addrFamily = interface.ifa_addr.pointee.sa_family
            if addrFamily == UInt8(AF_INET) { // IPv4
                let name = String(cString: interface.ifa_name)
                if name == "en0" || name == "en1" || name.contains("en") { // Wi-Fi or Ethernet
                    var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                    getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                                &hostname, socklen_t(hostname.count),
                                nil, socklen_t(0), NI_NUMERICHOST)
                    address = String(cString: hostname)
                    break
                }
            }

            ptr = interface.ifa_next
        }

        return address
    }
}
