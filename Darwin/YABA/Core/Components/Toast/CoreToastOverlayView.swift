//
//  CoreToastOverlayView.swift
//  YABA
//
//  Renders `CoreToastManager.shared.visibleToasts` for global hints (private session, PIN, etc.).
//

import Observation
import SwiftUI

struct CoreToastOverlayView: View {
    @Bindable
    private var manager = CoreToastManager.shared

    var body: some View {
        if !manager.visibleToasts.isEmpty {
            VStack(spacing: 8) {
                ForEach(manager.visibleToasts) { toast in
                    toastRow(toast)
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 12)
            .transition(.move(edge: .bottom).combined(with: .opacity))
            .animation(.smooth, value: manager.visibleToasts.map(\.id))
        }
    }

    private func toastRow(_ toast: ToastItem) -> some View {
        HStack(alignment: .center, spacing: 10) {
            YabaIconView(bundleKey: toast.iconType.iconAssetName)
                .frame(width: 22, height: 22)
            Text(toast.message)
                .font(.subheadline)
                .foregroundStyle(.primary)
                .multilineTextAlignment(.leading)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background {
            RoundedRectangle(cornerRadius: 14)
                .fill(.ultraThickMaterial)
        }
        .opacity(toast.isVisible ? 1 : 0)
    }
}
