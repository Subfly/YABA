//
//  ToastDuration.swift
//  YABA
//
//  Created by Ali Taha on 1.05.2025.
//


enum ToastDuration {
    case long, short
    
    func getDuration() -> UInt64 {
        switch self {
        case .long:
            8_000_000_000 // 8 seconds in nanoseconds
        case .short:
            4_000_000_000 // 4 seconds in nanoseconds
        }
    }
}
