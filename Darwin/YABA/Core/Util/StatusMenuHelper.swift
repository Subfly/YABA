//
//  StatusMenuHelper.swift
//  YABA
//
//  Created by Ali Taha on 17.05.2025.
//
//  Made with help of Adam Overholtzer
//  See: https://blog.overdesigned.net/posts/2021-08-26-catalyst-status-menu/

#if targetEnvironment(macCatalyst)
import ServiceManagement

class StatusMenuHelper {
    static func setStatusMenuEnabled() {
        try? SMAppService.loginItem(identifier: "dev.subfly.YABAStatusMenuItem").register()
    }
    
    static func setStatusMenuDisabled() {
        try? SMAppService.loginItem(identifier: "dev.subfly.YABAStatusMenuItem").unregister()
    }
}
#endif
