//
//  MarkdownSelectableTextView.swift
//  YABACore
//
//  SwiftUI `Text` + `textSelection(.enabled)` often shows only an action sheet without highlighted
//  selection on iOS. UIKit `UITextView`/`UITextField` patterns provide real selection (see discussion at
//  https://medium.com/@itsuki.enjoy/swiftui-mastering-text-selection-a11e1f9bd54f ).
//

#if canImport(UIKit)
import SwiftUI
import UIKit

// MARK: - Read-only selectable text view

final class MarkdownSelectableReadonlyTextView: UITextView {
    /// Bounds can be `{0,0}` on first SwiftUI layout passes; caching avoids layout thrash.
    private var lastAppliedContainerInnerWidth: CGFloat = 0

    override func layoutSubviews() {
        super.layoutSubviews()
        // Without matching `NSTextContainer` width to the view width, TextKit lays out “unbounded”
        // intrinsic width → one very long logical line / horizontal clipping in SwiftUI.
        let insetWidth =
            bounds.width - textContainerInset.left - textContainerInset.right
        guard insetWidth.isFinite else { return }
        let inner = max(1, insetWidth)

        textContainer.widthTracksTextView = true

        guard abs(inner - lastAppliedContainerInnerWidth) > 0.25 else {
            return
        }
        lastAppliedContainerInnerWidth = inner
        textContainer.size = CGSize(width: inner, height: .greatestFiniteMagnitude)

        invalidateIntrinsicContentSize()
    }

    override func caretRect(for position: UITextPosition) -> CGRect { .zero }

    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        switch action {
        case #selector(UIResponderStandardEditActions.copy(_:)),
            #selector(UIResponderStandardEditActions.select(_:)),
            #selector(UIResponderStandardEditActions.selectAll(_:)):
            return true
        default:
            if action == Selector(("_share:")) {
                return true
            }
            return false
        }
    }

    override func buildMenu(with builder: any UIMenuBuilder) {
        builder.remove(menu: .autoFill)
        super.buildMenu(with: builder)
    }
}

// MARK: - Representable for attributed payloads

struct MarkdownSelectableTextRepresentable: UIViewRepresentable {
    var attributedText: NSAttributedString
    var textAlignment: NSTextAlignment

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> MarkdownSelectableReadonlyTextView {
        let tv = MarkdownSelectableReadonlyTextView(frame: .zero, textContainer: nil)
        tv.delegate = context.coordinator
        tv.isEditable = false
        tv.isSelectable = true
        tv.backgroundColor = .clear
        tv.textContainerInset = .zero
        tv.textContainer.lineFragmentPadding = 0
        tv.textContainer.widthTracksTextView = true
        tv.isScrollEnabled = false
        tv.adjustsFontForContentSizeCategory = true
        tv.autocorrectionType = .no
        tv.textContentType = nil
        tv.dataDetectorTypes = []
        tv.tintColor = .label
        tv.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        tv.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return tv
    }

    func updateUIView(_ tv: MarkdownSelectableReadonlyTextView, context _: Context) {
        tv.textAlignment = textAlignment
        if tv.attributedText != attributedText {
            tv.attributedText = attributedText
            tv.invalidateIntrinsicContentSize()
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        func textView(_: UITextView, shouldInteractWith _: URL, in _: NSRange) -> Bool {
            false
        }

        func textView(_: UITextView, shouldChangeTextIn _: NSRange, replacementText _: String) -> Bool {
            false
        }
    }
}

#endif

#if canImport(UIKit)
extension Font.Weight {
    fileprivate var uiKitWeight: UIFont.Weight {
        switch self {
        case .ultraLight: return .ultraLight
        case .thin: return .thin
        case .light: return .light
        case .regular: return .regular
        case .medium: return .medium
        case .semibold: return .semibold
        case .bold: return .bold
        case .heavy: return .heavy
        case .black: return .black
        default: return .regular
        }
    }
}
#endif

// MARK: - Plain selectable wrappers (semantic font)

struct MarkdownSelectablePlainText: View {
    private var textStorage: String
    private var semanticFont: MarkdownSemanticFont
    private var weight: Font.Weight
    private var monospaced: Bool
    private var foreground: Color?

    init(
        verbatim: String,
        semantic: MarkdownSemanticFont,
        weight: Font.Weight = .regular,
        monospaced: Bool = false,
        foreground: Color? = nil
    ) {
        textStorage = verbatim
        semanticFont = semantic
        self.weight = weight
        self.monospaced = monospaced
        self.foreground = foreground
    }

#if canImport(UIKit)
    var body: some View {
        let fgUIColor =
            foreground.map { UIColor($0) }
            ?? UIColor.label
        let uf = semanticFont.uiFont(weight: weight.uiKitWeight, monospaced: monospaced)
        let attr = NSMutableAttributedString(string: textStorage, attributes: [
            .font: uf,
            .foregroundColor: fgUIColor,
        ])
        MarkdownSelectableTextRepresentable(attributedText: attr, textAlignment: .natural)
            .frame(maxWidth: .infinity, alignment: .leading)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityLabel(textStorage)
    }
#else
    var body: some View {
        let styled = semanticFont.swiftUIFont.weight(weight)
        let mono = monospaced ? styled.monospaced() : styled
        return Text(textStorage)
            .font(mono)
            .foregroundStyle(foreground ?? .primary)
            .textSelection(.enabled)
    }
#endif
}
