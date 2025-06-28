//
//  ReaderModeExtractor.swift
//  YABA
//
//  Created by Ali Taha on 6.06.2025.
//


import Foundation

class ReaderModeExtractor {
    static func extract(from html: String) -> String? {
        let patterns = [
            "<article[\\s\\S]*?</article>",
            "<main[\\s\\S]*?</main>",
            "<div[^>]*>(?:\\s*<p[\\s\\S]*?</p>\\s*){2,}[\\s\\S]*?</div>"
        ]
        
        var candidates: [(html: String, score: Int)] = []
        
        for pattern in patterns {
            let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
            let matches = regex?.matches(in: html, range: NSRange(html.startIndex..., in: html)) ?? []
            
            for match in matches {
                if let range = Range(match.range, in: html) {
                    let snippet = String(html[range])
                    let score = scoreHTML(snippet)
                    candidates.append((snippet, score))
                }
            }
        }
        
        if let best = candidates.max(by: { $0.score < $1.score })?.html {
            return applyReaderHTMLTemplate(to: best)
        }
        
        return nil
    }
    
    private static func scoreHTML(_ html: String) -> Int {
        var score = 0
        
        // Score based on number of paragraphs
        score += countMatches("<p[\\s\\S]*?</p>", in: html) * 10
        
        // Penalize excessive links
        score -= countMatches("<a[\\s\\S]*?</a>", in: html) * 2
        
        // Bonus if class or id includes keywords
        if html.range(of: #"(?i)(id|class)="[^"]*(article|content|main)[^"]*""#, options: .regularExpression) != nil {
            score += 20
        }
        
        // Bonus for tag type
        if html.contains("<article") { score += 20 }
        if html.contains("<main") { score += 10 }

        // Length of text content (capped to +50)
        let plainText = html.replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
        score += min(plainText.count / 100, 50)
        
        return score
    }
    
    private static func countMatches(_ pattern: String, in text: String) -> Int {
        let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
        return regex?.numberOfMatches(in: text, range: NSRange(text.startIndex..., in: text)) ?? 0
    }
    
    private static func applyReaderHTMLTemplate(
        to content: String,
        title: String = "Reader Mode"
    ) -> String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                    padding: 20px;
                    margin: 0;
                    background-color: #1c1c1e;
                    color: #f2f2f7;
                }
                h1, h2, h3, h4 {
                    font-weight: bold;
                    margin-top: 1.5em;
                }
                p {
                    margin-bottom: 1em;
                }
                pre {
                    background: #2c2c2e;
                    padding: 1em;
                    overflow-x: auto;
                    border-radius: 10px;
                    color: #e5e5e5;
                }
                code {
                    font-family: Menlo, monospace;
                    background: #2c2c2e;
                    padding: 2px 6px;
                    border-radius: 5px;
                }
                a {
                    color: #4aa3ff;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                ul, ol {
                    margin-bottom: 1em;
                    padding-left: 1.5em;
                }
                blockquote {
                    margin-left: 0;
                    padding-left: 1em;
                    border-left: 3px solid #666;
                    color: #ccc;
                }
            </style>
            <title>\(title)</title>
        </head>
        <body>
            \(content)
        </body>
        </html>
        """
    }
}