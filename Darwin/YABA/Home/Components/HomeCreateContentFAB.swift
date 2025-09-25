//
//  CreateContentFAB.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import SwiftUI

struct HomeCreateContentFAB: View {
    @Binding
    var isActive: Bool
    let onClickAction: (_ type: CreationType) -> Void
    
    var body: some View {
        if #available(iOS 26, *) {
            GlassyHomeCreateContentFAB(
                isActive: $isActive,
                onClickAction: onClickAction
            )
        } else {
            LegacyHomeCreateContentFAB(
                isActive: $isActive,
                onClickAction: onClickAction
            )
        }
    }
}

@available(iOS 26, *)
private struct GlassyHomeCreateContentFAB: View {
    @AppStorage(Constants.preferredFabPositionKey)
    private var preferredPosition: FABPosition = .center
    
    @Namespace private var animation
    
    @Binding
    var isActive: Bool
    let onClickAction: (_ type: CreationType) -> Void
    
    var body: some View {
        ZStack {
            Rectangle()
                .fill()
                .foregroundStyle(Material.ultraThin)
                .blur(radius: 8)
                .opacity(isActive ? 0.6 : 0)
                .onTapGesture {
                    onClickAction(.main)
                }
        }.overlay(
            alignment: preferredPosition == .center
            ? .bottom
            : preferredPosition == .left
            ? .bottomLeading
            : .bottomTrailing
        ) {
            GlassEffectContainer(spacing: 18) {
                VStack(spacing: 16) {
                    if isActive {
                        Button {
                            onClickAction(.bookmark)
                        } label: {
                            fab(isMini: true, type: .bookmark)
                        }
                        .glassEffectID(4, in: animation)
                        
                        Button {
                            onClickAction(.folder)
                        } label: {
                            fab(isMini: true, type: .folder)
                        }
                        .glassEffectID(3, in: animation)

                        Button {
                            onClickAction(.tag)
                        } label: {
                            fab(isMini: true, type: .tag)
                        }
                        .glassEffectID(2, in: animation)
                    }
                    
                    Button {
                        onClickAction(.main)
                    } label: {
                        fab(isMini: false, type: .main)
                    }
                    .glassEffectID(1, in: animation)
                }
                .padding(.bottom)
                .padding(.bottom)
                .padding(.leading, preferredPosition == .left ? 32 : 0)
                .padding(.trailing, preferredPosition == .right ? 32 : 0)
            }.animation(.smooth, value: isActive)
        }
    }
    
    @ViewBuilder
    private func fab(isMini: Bool, type: CreationType) -> some View {
        Group {
            YabaIconView(bundleKey: type.getIcon())
                .foregroundStyle(.white)
                .frame(
                    width: isMini ? 18 : 24,
                    height: isMini ? 18 : 24
                )
                .rotationEffect(Angle(degrees: isMini ? 0 : isActive ? 45 : 0))
        }
        .frame(
            width: isMini ? 48 : 72,
            height: isMini ? 48 : 72
        )
        .glassEffect(.regular.tint(.accentColor.opacity(0.5)).interactive(), in: .circle)
    }
}

private struct LegacyHomeCreateContentFAB: View {
    @AppStorage(Constants.preferredFabPositionKey)
    private var preferredPosition: FABPosition = .center
    
    @Binding
    var isActive: Bool
    let onClickAction: (_ type: CreationType) -> Void

    var body: some View {
        ZStack {
            Rectangle()
                .fill()
                .foregroundStyle(Material.ultraThin)
                .blur(radius: 8)
                .opacity(isActive ? 0.6 : 0)
                .onTapGesture {
                    onClickAction(.main)
                }
        }.overlay(
            alignment: preferredPosition == .center
            ? .bottom
            : preferredPosition == .left
            ? .bottomLeading
            : .bottomTrailing
        ) {
            VStack(spacing: 15) {
                clickableMiniFab(type: .bookmark)
                clickableMiniFab(type: .folder)
                clickableMiniFab(type: .tag)
                Button {
                    onClickAction(.main)
                } label: {
                    fab(isMini: false, type: .main)
                }
            }
            .padding(.bottom)
            .padding(.bottom)
            .padding(.leading, UIDevice.current.userInterfaceIdiom == .pad ? 96 : 0)
            .padding(.leading, preferredPosition == .left ? 32 : 0)
            .padding(.trailing, preferredPosition == .right ? 32 : 0)
        }
    }

    @ViewBuilder
    private func fab(isMini: Bool, type: CreationType) -> some View {
        ZStack(alignment: .center) {
            Circle()
                .frame(
                    width: isMini ? 48 : 72,
                    height: isMini ? 48 : 72
                )
            YabaIconView(bundleKey: type.getIcon())
                .foregroundStyle(.white)
                .frame(
                    width: isMini ? 18 : 24,
                    height: isMini ? 18 : 24
                )
        }
        .shadow(radius: 4)
        .rotationEffect(Angle(degrees: isMini ? 0 : isActive ? 45 : 0))
    }

    @ViewBuilder
    private func clickableMiniFab(type: CreationType) -> some View {
        let duration = switch type {
        case .bookmark:
            isActive ? 0.3 : 0.1
        case .folder:
            0.2
        case .tag:
            isActive ? 0.1 : 0.3
        default:
            0.0
        }

        Button {
            onClickAction(type)
        } label: {
            self.fab(isMini: true, type: type)
                .shadow(radius: 2)
        }
        .scaleEffect(isActive ? 1 : 0)
        .opacity(isActive ? 1 : 0)
        .animation(.easeInOut.delay(duration), value: isActive)
        .transition(.slide)
    }
}

#Preview {
    HomeCreateContentFAB(
        isActive: .constant(true),
        onClickAction: { _ in
            // Do Nothing
        }
    )
}

