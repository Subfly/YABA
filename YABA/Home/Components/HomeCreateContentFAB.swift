//
//  CreateContentFAB.swift
//  YABA
//
//  Created by Ali Taha on 8.10.2024.
//

import SwiftUI

struct HomeCreateContentFAB: View {
    @Binding
    private var isActive: Bool
    
    @Binding
    var selectedAppTint: Color
    
    let onClickAction: (_ type: CreationType) -> Void
    
    init(
        isActive: Binding<Bool>,
        selectedAppTint: Binding<Color>,
        onClickAction: @escaping (_: CreationType) -> Void
    ) {
        _isActive = isActive
        _selectedAppTint = selectedAppTint
        self.onClickAction = onClickAction
    }

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
        }.overlay(alignment: .bottom) {
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
            Image(systemName: type.getIcon())
                .foregroundStyle(.white)
                .fontWeight(.bold)
                .font(.system(size: isMini ? 18 : 24))
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
        selectedAppTint: .constant(.accentColor),
        onClickAction: { _ in
            // Do Nothing
        }
    )
}
