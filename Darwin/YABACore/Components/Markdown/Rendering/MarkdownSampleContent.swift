//
//  MarkdownSampleContent.swift
//  YABACore
//
//  Large reference Markdown for the debug gallery, previews, and fixtures. Written for
//  `ExtendedFlavour` (see `Markdown/markdown-parser/PARSER_COVERAGE_ANALYSIS.md`).
//

import Foundation

public enum MarkdownSampleContent {
    public static let galleryDocument: String = {
        // swiftlint:disable all
        """
        ---
        title: "YABA Markdown Gallery"
        subtitle: "Extended parse stress sample"
        version: 2
        tags:
          - preview
          - debug
        ---

        [ref-example]: https://www.example.com/path?q=1 "Reference title"
        [unicode-ref]: /unicode/路径 "好的"
        [shortcut]: <https://example.org>

        ## YABA native Markdown gallery (preview host)

        This document exercises ATX and Setext headings, thematic and page breaks, front matter, link
        reference definitions, GFM (tables, strikethrough, task lists, autolinks), footnotes,
        definition lists, display math, admonition-style block quotes, custom containers, columns,
        in-document tabs, bibliography, TOC, figures, and yaba-asset images.

        ---

        # H1 — gallery root
        ## H2 with **nested** and *mixed* [inline `code`](https://example.com "title")
        ### H3
        #### H4
        ##### H5
        ###### H6

        Setext level 1
        ===============

        Setext level 2
        ---------------

        Thematic (horizontal) rules:

        * * *

        ---

        Page break (visual line where the parser emits `PageBreak`):

        {page}

        ---

        ## Inlines in one paragraph (Extended)

        CommonMark: **strong**, *emphasis*, `inline code`, [named link](https://example.com "Title"),
        <https://autolink.example>, and a bare email autolink like hello@mail.example when enabled.

        GFM: ~~strikethrough~~, and task lists below.

        Extended: ==highlight==, ++insert++, ^sup^, and $E=mc^2$ for inline math when configured.

        Emoji shortcodes: :smile: :rocket: and ASCII face :-) if ASCII emoticons are on.

        Ruby: {漢字|kanji} and wiki links: [[wikilink]] and [[Other Page|Custom label]].

        Styled text: [badge text]{.pill #id-note style="color:teal"}.

        Spoiler (Discord / Reddit style, single line):
        A sentence with >!hidden **bold** inside!< and more text.

        HTML inline: 2 < 4, copy &amp; entities, and <kbd>Cmd</kbd>+K style keys.

        Footnote callouts in prose[^fn-one] and a second[^fn-two].

        [^fn-one]: Short footnote with `code` inside.
        [^fn-two]: Longer footnote with a list:

        - item in footnote
        - second

        ---

        ## More standard GFM + escapes

        Autolink angle form: <https://github.com>

        Backslash-escaped: \\*not emph\\* and \\`not code\\`.

        ---

        ## Lists

        Tight:

        * Tight A
        * Tight B
        * Tight C

        Ordered:

        1. first
        2. second
        3. third

        Nested:

        - outer
          - inner one
            - deep
        - outer two

        Task (GFM):

        * [x] completed
        * [ ] open
        * [x] **done** with formatting

        Loose (blank line between items):

        - One paragraph in item.

        - Second item, second paragraph.

        ---

        ## Block quote

        > One line.
        > With continuation.
        >
        > > Nested quote with **strong** and `code`.
        >
        > Finishing the outer with a list:
        > - a
        > - b

        Admonition-style (GFM alert subset; parsed if enabled):

        > [!NOTE]
        > Note **body** with `code`.

        > [!WARNING] Titled
        > Warning line one.
        > Line two.

        > [!DANGER]
        > Critical content.

        ---

        ## GFM tables

        | Left | Center | Right |
        |:-----|:------:|------:|
        | L0 | C0 | R0 |
        | `code` | **bold** | *italic* |
        | [link](https://example.com) | ==hl== | ~~strike~~ |

        Minimal:

        | A | B |
        |---|---|
        | 1 | 2 |

        ---

        ## Fenced code (info string, mermaid, attributes)

        ```swift title="Sample" {linenums=1 .swift hl_lines=1-2}
        struct S {
            var x: Int
        }
        ```

        ```mermaid
        flowchart LR
            A[In] --> B[Parser]
            B --> C[AST]
        ```

        ```text
        Plain
        no-highlight
        block
        ```

        ~~~python title="Tilde fence"
        def f():
            return 42
        ~~~

        ---

        ## Indented code (four spaces)

            line1
            line2
                deeper

        ---

        ## Display math

        $$
        \\int_0^1 x^2\\,dx = \\frac{1}{3}
        $$

        ---

        ## Raw HTML block

        <div class="md-html-demo">
        <p>Paragraph with <strong>strong</strong> in HTML.</p>
        <pre>pre</pre>
        </div>

        ---

        ## Definition list

        Apple
        : A kind of **fruit**; also a company.

        Free software
        : Defined with a [link](https://gnu.org) in the line.

        ---

        ## Custom containers

        ::: warning Watch out
        Inner **markdown** in a custom container.
        :::

        ::: tip
        - Bullet
        - Two
        :::

        ---

        ## Directive (block; shape depends on extension)

        {% callout "Demo" type=info %}
        Inner text with **markup** for directive blocks.
        {% endcallout %}

        ---

        ## Columns (extension)

        <columns>
        <column>
        **Left** column.
        - a
        - b
        </column>
        <column width="50%">
        **Right** column and `code`.
        </column>
        </columns>

        ---

        ## In-document tabs

        ::tabs
        :tab: First
        ### Tab A
        - one
        - two

        :tab: Second
        ### Tab B
        More text.

        :tab: Code
        ```json
        { "ok": true, "n": 3 }
        ```
        ::

        ---

        ## Bibliography

        ::bibliography
        {
          "smith2020": { "key": "smith2020", "content": "Smith, J. (2020). *Work*. Journal." },
          "doe21": { "key": "doe21", "content": "Doe, A. (2021). Ref two." }
        }
        ::

        ---

        ## Diagram (non-mermaid)

        ```plantuml
        Alice -> Bob: hi
        ```

        ---

        ## Table of contents

        [TOC]
        :depth=1-4
        :order=asc
        :exclude=#excluded-heading

        ---

        ## Deeper section for TOC { #excluded-heading }

        ### H3 for depth filtering
        This heading id is excluded from TOC when `exclude` matches.

        ---

        ## Figures and images

        ![Standalone caption](yaba-asset://demo-asset-1 "title attr")

        ![Remote when allowed](https://httpbin.org/image/png "remote")

        Inline in a sentence: see ![icon](yaba-asset://demo-asset-2) here.

        ---

        ## Abbreviation

        *[YABA]: Yet Another Bookmarks App

        First YABA use with link shortcuts: [ref-example] and [shortcut].

        ---

        ## End stress block

        Final **paragraph** with footnote for cross-check[^end].

        [^end]: Endnote referencing earlier [^fn-one].

        """
        // swiftlint:enable all
    }()
}
