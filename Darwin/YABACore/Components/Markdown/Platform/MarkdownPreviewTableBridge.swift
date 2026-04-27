//
//  MarkdownPreviewTableBridge.swift
//  YABACore
//
//  Table-backed host for KMP `Document` block rows: UIKit (`UITableView`) on iOS family,
//  AppKit (`NSTableView`) on native macOS.
//

import MarkdownParser
import SwiftUI

#if canImport(UIKit)
import UIKit

public struct MarkdownPreviewTable: UIViewControllerRepresentable {
    public var document: Document
    public var configuration: MarkdownPreviewConfiguration
    public var theme: MarkdownThemeTokens?

    public init(
        document: Document,
        configuration: MarkdownPreviewConfiguration = .init(),
        theme: MarkdownThemeTokens? = nil
    ) {
        self.document = document
        self.configuration = configuration
        self.theme = theme
    }

    public func makeUIViewController(context: Context) -> MarkdownPreviewTableViewController {
        let vc = MarkdownPreviewTableViewController()
        _ = vc.view
        vc.apply(document: document, configuration: configuration, theme: theme)
        return vc
    }

    public func updateUIViewController(_ vc: MarkdownPreviewTableViewController, context: Context) {
        vc.apply(document: document, configuration: configuration, theme: theme)
    }
}

public final class MarkdownPreviewTableViewController: UIViewController, UITableViewDelegate {
    private let table = UITableView(frame: .zero, style: .plain)
    private var rowModel: [String: (sourceIndex: Int, node: Node)] = [:]
    private var configuration: MarkdownPreviewConfiguration = .init()
    private var theme: MarkdownThemeTokens?
    private var tocHeadings: [MarkdownTocHeading] = []
    private var dataSource: UITableViewDiffableDataSource<Int, String>!
    private static let cellId = "markdown.kmp.block"

    public init() {
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError() }

    public override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        table.backgroundColor = .clear
        table.separatorStyle = .none
        table.rowHeight = UITableView.automaticDimension
        table.estimatedRowHeight = 80
        table.delegate = self
        table.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(table)
        NSLayoutConstraint.activate([
            table.topAnchor.constraint(equalTo: view.topAnchor),
            table.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            table.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            table.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])
        table.register(UITableViewCell.self, forCellReuseIdentifier: Self.cellId)
        dataSource = UITableViewDiffableDataSource<Int, String>(tableView: table) { [weak self] table, indexPath, id in
            guard
                let self,
                let row = self.rowModel[id]
            else { return UITableViewCell() }
            let v = self.hostingRoot(node: row.node, sourceIndex: row.sourceIndex, rowId: id)
            let cell = table.dequeueReusableCell(withIdentifier: Self.cellId, for: indexPath)
            cell.contentConfiguration = UIHostingConfiguration { v }
                .margins(.vertical, 4)
            cell.selectionStyle = .none
            cell.backgroundColor = .clear
            return cell
        }
        dataSource.defaultRowAnimation = .fade
    }

    @ViewBuilder
    private func hostingRoot(node: Node, sourceIndex: Int, rowId: String) -> some View {
        if let th = self.theme {
            MarkdownKMPNodeBlockView(node: node, tableIndex: sourceIndex, listNesting: 0)
                .id("\(rowId)-\(kmpBlockRenderRevision(node: node))")
                .markdownTocHeadings(tocHeadings)
                .markdownTheme(th)
                .markdownPreviewConfiguration(configuration)
        } else {
            MarkdownKMPNodeBlockView(node: node, tableIndex: sourceIndex, listNesting: 0)
                .id("\(rowId)-\(kmpBlockRenderRevision(node: node))")
                .markdownTocHeadings(tocHeadings)
                .markdownPreviewConfiguration(configuration)
        }
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        UITableView.automaticDimension
    }

    public func apply(
        document: Document,
        configuration: MarkdownPreviewConfiguration,
        theme: MarkdownThemeTokens?
    ) {
        self.configuration = configuration
        self.theme = theme
        self.tocHeadings = MarkdownTocSupport.collectHeadings(from: document)
        var map: [String: (Int, Node)] = [:]
        var ids: [String] = []
        let children = (document as ContainerNode).children
        var j = 0
        for (i, n) in children.enumerated() {
            if n is BlankLine { continue }
            let id = kmpRowIdentifier(index: j, node: n)
            map[id] = (i, n)
            ids.append(id)
            j += 1
        }
        self.rowModel = map.mapValues { (sourceIndex: $0.0, node: $0.1) }
        var snap = NSDiffableDataSourceSnapshot<Int, String>()
        snap.appendSections([0])
        snap.appendItems(ids, toSection: 0)
        dataSource?.apply(snap, animatingDifferences: true)
    }
}

#elseif canImport(AppKit)
import AppKit

public struct MarkdownPreviewTable: NSViewControllerRepresentable {
    public var document: Document
    public var configuration: MarkdownPreviewConfiguration
    public var theme: MarkdownThemeTokens?

    public init(
        document: Document,
        configuration: MarkdownPreviewConfiguration = .init(),
        theme: MarkdownThemeTokens? = nil
    ) {
        self.document = document
        self.configuration = configuration
        self.theme = theme
    }

    public func makeNSViewController(context: Context) -> MarkdownPreviewTableViewControllerAppKit {
        let vc = MarkdownPreviewTableViewControllerAppKit()
        vc.apply(document: document, configuration: configuration, theme: theme)
        return vc
    }

    public func updateNSViewController(_ vc: MarkdownPreviewTableViewControllerAppKit, context: Context) {
        vc.apply(document: document, configuration: configuration, theme: theme)
    }
}

public final class MarkdownPreviewTableViewControllerAppKit: NSViewController, NSTableViewDataSource, NSTableViewDelegate {
    private let table = NSTableView()
    private let scroll = NSScrollView()
    private var rowIds: [String] = []
    private var rowModel: [String: (sourceIndex: Int, node: Node)] = [:]
    private var configuration: MarkdownPreviewConfiguration = .init()
    private var theme: MarkdownThemeTokens?
    private var tocHeadings: [MarkdownTocHeading] = []

    public override func loadView() {
        self.view = NSView()
    }

    public override func viewDidLoad() {
        super.viewDidLoad()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        scroll.hasVerticalScroller = true
        scroll.hasHorizontalScroller = false
        scroll.autohidesScrollers = true
        scroll.borderType = .noBorder
        table.headerView = nil
        table.intercellSpacing = NSSize(width: 0, height: 0)
        table.backgroundColor = .clear
        table.dataSource = self
        table.delegate = self
        if table.tableColumns.isEmpty {
            let col = NSTableColumn(identifier: NSUserInterfaceItemIdentifier("md.col"))
            col.isEditable = false
            table.addTableColumn(col)
        }
        table.columnAutoresizingStyle = .lastColumnOnlyAutoresizingStyle
        if #available(macOS 11.0, *) {
            table.usesAutomaticRowHeights = true
        }
        scroll.documentView = table
        view.addSubview(scroll)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.topAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])
    }

    public func numberOfRows(in tableView: NSTableView) -> Int {
        rowIds.count
    }

    public func tableView(_ tableView: NSTableView, viewFor tableColumn: NSTableColumn?, row: Int) -> NSView? {
        guard row < rowIds.count else { return nil }
        let id = rowIds[row]
        guard let r = rowModel[id] else { return nil }
        let root: AnyView
        if let th = self.theme {
            root = AnyView(
                MarkdownKMPNodeBlockView(node: r.node, tableIndex: r.sourceIndex, listNesting: 0)
                    .id("\(id)-\(kmpBlockRenderRevision(node: r.node))")
                    .markdownTocHeadings(tocHeadings)
                    .markdownTheme(th)
                    .markdownPreviewConfiguration(configuration)
            )
        } else {
            root = AnyView(
                MarkdownKMPNodeBlockView(node: r.node, tableIndex: r.sourceIndex, listNesting: 0)
                    .id("\(id)-\(kmpBlockRenderRevision(node: r.node))")
                    .markdownTocHeadings(tocHeadings)
                    .markdownPreviewConfiguration(configuration)
            )
        }
        let host = NSHostingView(rootView: root)
        host.translatesAutoresizingMaskIntoConstraints = false
        return host
    }

    public func apply(
        document: Document,
        configuration: MarkdownPreviewConfiguration,
        theme: MarkdownThemeTokens?
    ) {
        self.configuration = configuration
        self.theme = theme
        self.tocHeadings = MarkdownTocSupport.collectHeadings(from: document)
        var map: [String: (Int, Node)] = [:]
        var ids: [String] = []
        let children = (document as ContainerNode).children
        var j = 0
        for (i, n) in children.enumerated() {
            if n is BlankLine { continue }
            let id = kmpRowIdentifier(index: j, node: n)
            map[id] = (i, n)
            ids.append(id)
            j += 1
        }
        self.rowIds = ids
        self.rowModel = map.mapValues { (sourceIndex: $0.0, node: $0.1) }
        table.reloadData()
    }
}
#endif
