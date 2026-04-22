//
//  AnnotationRenderingPayloadBuilder.swift
//  YABACore
//

import Foundation

enum AnnotationRenderingPayloadBuilder {
    /// JSON payload for `YabaEditorBridge.setAnnotations`.
    static func readableJSON(from annotations: [AnnotationModel]) -> String {
        let payload: [[String: Any]] = annotations
            .filter { $0.type == .readable }
            .map {
                [
                    "id": $0.annotationId,
                    "colorRole": colorRoleToken(for: $0.colorRole),
                ]
            }
        return encodeJSONArray(payload)
    }

    /// JSON payload for `YabaPdfBridge.setAnnotations`.
    static func pdfJSON(from annotations: [AnnotationModel]) -> String {
        let payload: [[String: Any]] = annotations.compactMap { annotation in
            guard annotation.type == .pdf else { return nil }
            guard let extras = decodePdfExtras(annotation.extrasJson) else { return nil }
            return [
                "id": annotation.annotationId,
                "colorRole": colorRoleToken(for: annotation.colorRole),
                "startSectionKey": extras.startSectionKey,
                "startOffsetInSection": extras.startOffsetInSection,
                "endSectionKey": extras.endSectionKey,
                "endOffsetInSection": extras.endOffsetInSection,
            ]
        }
        return encodeJSONArray(payload)
    }

    /// JSON payload for `YabaEpubBridge.setAnnotations`.
    static func epubJSON(from annotations: [AnnotationModel]) -> String {
        let payload: [[String: Any]] = annotations.compactMap { annotation in
            guard annotation.type == .epub else { return nil }
            guard let extras = decodeEpubExtras(annotation.extrasJson), !extras.cfiRange.isEmpty else { return nil }
            return [
                "id": annotation.annotationId,
                "colorRole": colorRoleToken(for: annotation.colorRole),
                "cfiRange": extras.cfiRange,
            ]
        }
        return encodeJSONArray(payload)
    }

    private static func encodeJSONArray(_ payload: [[String: Any]]) -> String {
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let json = String(data: data, encoding: .utf8)
        else {
            return "[]"
        }
        return json
    }

    private static func decodePdfExtras(_ extrasJson: String?) -> PdfAnnotationExtras? {
        guard let extrasJson, let data = extrasJson.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(PdfAnnotationExtras.self, from: data)
    }

    private static func decodeEpubExtras(_ extrasJson: String?) -> EpubAnnotationExtras? {
        guard let extrasJson, let data = extrasJson.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(EpubAnnotationExtras.self, from: data)
    }

    private static func colorRoleToken(for color: YabaColor) -> String {
        switch color {
        case .none, .yellow: return "YELLOW"
        case .blue: return "BLUE"
        case .brown: return "BROWN"
        case .cyan: return "CYAN"
        case .gray: return "GRAY"
        case .green: return "GREEN"
        case .indigo: return "INDIGO"
        case .mint: return "MINT"
        case .orange: return "ORANGE"
        case .pink: return "PINK"
        case .purple: return "PURPLE"
        case .red: return "RED"
        case .teal: return "TEAL"
        }
    }
}
