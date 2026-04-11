//
//  WebSecurityPolicy.swift
//  YABACore
//
//  Reference CSP / Permissions-Policy strings mirroring Compose `YabaWebAndroidSecurity`.
//

import Foundation

/// Reference Content-Security-Policy aligned with `YabaWebAndroidSecurity.CONTENT_SECURITY_POLICY`.
public enum WebSecurityPolicy {
    public static let contentSecurityPolicyReference = """
    default-src 'none'; form-action 'none'; connect-src 'self' blob:; img-src 'self' blob: data: yaba-asset:; script-src 'self' 'wasm-unsafe-eval' 'unsafe-eval'; style-src 'self' 'unsafe-inline' blob:; font-src 'self' data: blob:; worker-src 'self' blob:; media-src 'self' blob:; frame-ancestors 'none'; base-uri 'self'
    """

    /// Reference Permissions-Policy (deny sensitive features by default).
    public static let permissionsPolicyReference =
        "accelerometer=(), ambient-light-sensor=(), autoplay=(), battery=(), camera=(), clipboard-read=(), clipboard-write=(), display-capture=(), document-domain=(), encrypted-media=(), fullscreen=(), gamepad=(), geolocation=(), gyroscope=(), hid=(), idle-detection=(), interest-cohort=(), magnetometer=(), microphone=(), midi=(), payment=(), picture-in-picture=(), publickey-credentials-get=(), screen-wake-lock=(), serial=(), speaker-selection=(), sync-xhr=(), usb=(), xr-spatial-tracking=()"
}
