//
//  SixDigitPinInput.swift
//  YABA
//
//  Six-digit PIN — Compose `SixDigitPinRow` parity (masked dots when not focused on slot).
//

import SwiftUI

struct SixDigitPinInput: View {
    @Binding
    var pin: String

    var forceAllRed: Bool

    @FocusState
    private var focused: Bool

    private let pinLength = 6

    var body: some View {
        ZStack {
            TextField("", text: $pin)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($focused)
                .opacity(0.001)
                .frame(width: 1, height: 1)
                .accessibilityHidden(true)

            HStack(spacing: 8) {
                Spacer()
                ForEach(0 ..< pinLength, id: \.self) { index in
                    slot(index: index)
                }
                Spacer()
            }
            .contentShape(Rectangle())
            .onTapGesture {
                focused = true
            }
        }
        .onChange(of: pin) { _, newValue in
            let digits = newValue.filter(\.isNumber)
            pin = String(digits.prefix(pinLength))
        }
    }

    private func slot(index: Int) -> some View {
        let chars = Array(pin)
        let ch = index < chars.count ? String(chars[index]) : ""
        let showDot = !ch.isEmpty && !focused
        return ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(forceAllRed ? Color(.systemRed).opacity(0.15) : Color(.secondarySystemFill))
            if showDot {
                Circle()
                    .fill(Color.primary)
                    .frame(width: 10, height: 10)
            } else if !ch.isEmpty {
                Text(ch)
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(.primary)
            }
        }
        .frame(width: 44, height: 48)
    }
}
