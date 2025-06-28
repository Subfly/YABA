//
//  StatusMenuHelper.swift
//  YABA
//
//  Created by Ali Taha on 17.05.2025.
//

#if targetEnvironment(macCatalyst)
import ServiceManagement

class StatusMenuHelper {
    static func setStatusMenuEnabled() {
        try? SMAppService.loginItem(identifier: "dev.subfly.YABAStatusMenuItem").register()
    }
}
#endif
