#!/usr/bin/env python3
"""
Rust By Example Course Generator for CodeMateX.

Parses mdBook content from the rust-by-example repository and generates
RustByExampleCourseContent.kt conforming to CodeMateX's LearningModels data structures.
"""

import argparse
import os
import re
import subprocess
import sys


def parse_summary(summary_lines):
    """
    Parses SUMMARY.md into a hierarchy of chapters and lessons.
    """
    chapters = []
    curr_ch = None
    curr_s1 = None
    curr_s2 = None

    for line in summary_lines:
        line_str = line.rstrip()
        # Top-level chapter: - [Hello World](hello.md)
        m_ch = re.match(r'^- \[([^\]]+)\]\(([^)]+)\)', line_str)
        if m_ch:
            curr_ch = {'title': m_ch.group(1), 'path': m_ch.group(2), 'items': []}
            chapters.append(curr_ch)
            curr_s1 = None
            curr_s2 = None
            continue

        # Level 1 item:    - [Comments](hello/comment.md)
        m_s1 = re.match(r'^    - \[([^\]]+)\]\(([^)]+)\)', line_str)
        if m_s1 and curr_ch is not None:
            curr_s1 = {'title': m_s1.group(1), 'path': m_s1.group(2), 'children': []}
            curr_ch['items'].append(curr_s1)
            curr_s2 = None
            continue

        # Level 2 item:        - [Debug](hello/print/print_debug.md)
        m_s2 = re.match(r'^        - \[([^\]]+)\]\(([^)]+)\)', line_str)
        if m_s2 and curr_s1 is not None:
            curr_s2 = {'title': m_s2.group(1), 'path': m_s2.group(2), 'children': []}
            curr_s1['children'].append(curr_s2)
            continue

        # Level 3 item:            - [Testcase: List](hello/print/print_display/testcase_list.md)
        m_s3 = re.match(r'^            - \[([^\]]+)\]\(([^)]+)\)', line_str)
        if m_s3 and curr_s2 is not None:
            sub3 = {'title': m_s3.group(1), 'path': m_s3.group(2), 'children': []}
            curr_s2['children'].append(sub3)
            continue

    return chapters


def to_id(s):
    """Converts a string to a lowercase hyphen-separated identifier."""
    s = s.lower()
    s = re.sub(r'[^a-z0-9]+', '-', s).strip('-')
    return s


def extract_summary_and_title(raw_md, default_title):
    lines = raw_md.strip().splitlines()
    title = default_title
    for line in lines:
        line = line.strip()
        if line.startswith('# '):
            title = line[2:].strip()
            break

    summary = ""
    for line in lines:
        line = line.strip()
        if not line or line.startswith('#') or line.startswith('```') or line.startswith('*') or line.startswith('['):
            continue
        first_sentence = line.split('. ')[0].rstrip('.') + '.'
        if len(first_sentence) > 10:
            summary = first_sentence
            if len(summary) > 85:
                # Truncate at word boundary to keep line length well within 140 chars
                summary = summary[:85].rsplit(' ', 1)[0] + '...'
            break

    if not summary:
        summary = f"Learn about {title} with runnable Rust examples."

    return title, summary


def rewrite_markdown_links(content, page_rel_path):
    base_url = "https://doc.rust-lang.org/rust-by-example/"
    cur_dir = os.path.dirname(page_rel_path)

    # 1. Strip top heading # Title
    content = re.sub(r'^\s*#\s+[^\n]+\n*', '', content)

    # 2. Rewrite inline links: [label](url)
    def replace_link(match):
        label = match.group(1)
        url = re.sub(r'\s+', '', match.group(2))
        if url.startswith('http://') or url.startswith('https://') or url.startswith('#') or url.startswith('mailto:'):
            return f'[{label}]({url})'
        if url.endswith('.md') or '.md#' in url:
            parts = url.split('#')
            path_part = parts[0]
            anchor_part = ('#' + parts[1]) if len(parts) > 1 else ''
            norm = os.path.normpath(os.path.join(cur_dir, path_part))
            html_path = norm.replace('.md', '.html')
            full_url = base_url + html_path + anchor_part
            return f'[{label}]({full_url})'
        return f'[{label}]({url})'

    content = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', replace_link, content, flags=re.DOTALL)

    # 3. Rewrite reference links: [label]: url
    def replace_ref_link(match):
        label = match.group(1)
        url = re.sub(r'\s+', '', match.group(2))
        if url.startswith('http://') or url.startswith('https://') or url.startswith('#') or url.startswith('mailto:'):
            return f'[{label}]: {url}'
        if url.endswith('.md') or '.md#' in url:
            parts = url.split('#')
            path_part = parts[0]
            anchor_part = ('#' + parts[1]) if len(parts) > 1 else ''
            norm = os.path.normpath(os.path.join(cur_dir, path_part))
            html_path = norm.replace('.md', '.html')
            full_url = base_url + html_path + anchor_part
            return f'[{label}]: {full_url}'
        return f'[{label}]: {url}'

    content = re.sub(r'^\[([^\]]+)\]:\s*([^\n]+)', replace_ref_link, content, flags=re.MULTILINE)
    return content


def escape_kotlin_multiline(text):
    """
    Escapes characters for inclusion inside a Kotlin multiline string literal (\"\"\" ... \"\"\").
    In Kotlin raw multiline strings:
    - `$` must be written as `${'$'}` to prevent interpolation.
    - Triple quotes `\"\"\"` must be written as `\"\"${'\"'}\"\"` or split.
    """
    # Escape $
    text = text.replace('$', "${'$'}")
    # Escape triple quotes
    text = text.replace('"""', '""${\'"\'}""')
    return text


def parse_markdown_blocks(raw_md, page_rel_path):
    """
    Splits markdown into a list of LessonBlocks:
    - ('markdown', content)
    - ('code', lang, code, runnable)
    """
    cleaned_md = rewrite_markdown_links(raw_md, page_rel_path)

    pattern = re.compile(r'^\`\`\`([^\n]*)\n(.*?)\n\`\`\`', re.MULTILINE | re.DOTALL)
    blocks = []
    last_end = 0

    for match in pattern.finditer(cleaned_md):
        start, end = match.span()
        md_text = cleaned_md[last_end:start].strip()
        if md_text:
            blocks.append(('markdown', md_text))

        fence = match.group(1).strip()
        code_content = match.group(2)
        lang = fence.split(',')[0].strip() if fence else 'text'
        if not lang:
            lang = 'text'

        is_rust = lang in ('rust', 'rs')
        runnable = is_rust
        # If explicitly ignored or set to fail, not runnable
        if any(flag in fence for flag in ('ignore', 'compile_fail', 'no_run', 'edition2015', 'edition2018')):
            runnable = False

        blocks.append(('code', lang, code_content, runnable))
        last_end = end

    trailing_md = cleaned_md[last_end:].strip()
    if trailing_md:
        blocks.append(('markdown', trailing_md))

    return blocks


def get_git_commit(repo_path):
    try:
        res = subprocess.run(
            ['git', '-C', repo_path, 'rev-parse', 'HEAD'],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )
        return res.stdout.strip()
    except Exception:
        return "unknown"


def generate_course_content(repo_path, output_path):
    src_dir = os.path.join(repo_path, 'src')
    summary_path = os.path.join(src_dir, 'SUMMARY.md')

    if not os.path.exists(summary_path):
        print(f"Error: SUMMARY.md not found at {summary_path}", file=sys.stderr)
        sys.exit(1)

    with open(summary_path, 'r', encoding='utf-8') as f:
        summary_lines = f.readlines()

    commit_sha = get_git_commit(repo_path)
    chapters = parse_summary(summary_lines)

    lines = []
    lines.append("package dev.hossain.codematex.data.repository.course")
    lines.append("")
    lines.append("import dev.hossain.codematex.data.model.LearningChapter")
    lines.append("import dev.hossain.codematex.data.model.LearningCourse")
    lines.append("import dev.hossain.codematex.data.model.LearningLesson")
    lines.append("import dev.hossain.codematex.data.model.LessonBlock")
    lines.append("")
    lines.append("/**")
    lines.append(" * Bundled 'Rust by Example' course directly sourced from the official")
    lines.append(" * [rust-lang/rust-by-example](https://github.com/rust-lang/rust-by-example) repository.")
    lines.append(" *")
    lines.append(" * License:")
    lines.append(" * Rust by Example is dual-licensed under either of:")
    lines.append(" * - Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)")
    lines.append(" * - MIT License (http://opensource.org/licenses/MIT)")
    lines.append(" * at your option.")
    lines.append(" *")
    lines.append(f" * Source commit: {commit_sha}")
    lines.append(" * Generated by tools/generate_rbe_course.py")
    lines.append(" */")
    lines.append("object RustByExampleCourseContent {")
    lines.append("    const val COURSE_ID = \"rust-by-example\"")
    lines.append("")
    lines.append("    val course =")
    lines.append("        LearningCourse(")
    lines.append("            id = COURSE_ID,")
    lines.append("            language = \"Rust\",")
    lines.append("            title = \"Rust by Example\",")
    lines.append("            description = \"A complete collection of runnable examples illustrating Rust concepts and the standard library.\",")
    lines.append("            version = 1,")
    lines.append("            chapters =")
    lines.append("                listOf(")

    total_chapters = 0
    total_lessons = 0

    for ch_idx, ch in enumerate(chapters, start=1):
        total_chapters += 1
        ch_title = ch['title']
        ch_id = f"rbe-{to_id(ch_title)}"

        # Gather all pages for this chapter
        page_entries = []
        if ch['path']:
            page_entries.append((ch['title'], ch['path'], True))

        def collect_items(item):
            page_entries.append((item['title'], item['path'], False))
            for c in item.get('children', []):
                collect_items(c)

        for it in ch['items']:
            collect_items(it)

        # Chapter description from first page
        first_page_path = os.path.join(src_dir, ch['path']) if ch['path'] else ""
        ch_desc = f"Learn {ch_title} through practical Rust examples."
        if os.path.exists(first_page_path):
            with open(first_page_path, 'r', encoding='utf-8') as f:
                _, ch_desc = extract_summary_and_title(f.read(), ch_title)

        ch_desc_escaped = escape_kotlin_multiline(ch_desc).replace('"', '\\"')

        lines.append("                    chapter(")
        lines.append(f'                        id = "{ch_id}",')
        lines.append(f'                        order = {ch_idx},')
        lines.append(f'                        title = "{ch_title}",')
        lines.append(f'                        description = "{ch_desc_escaped}",')
        lines.append("                        lessons =")
        lines.append("                            listOf(")

        for l_idx, (page_title, page_rel_path, is_root) in enumerate(page_entries, start=1):
            total_lessons += 1
            full_page_path = os.path.join(src_dir, page_rel_path)
            with open(full_page_path, 'r', encoding='utf-8') as f:
                raw_md = f.read()

            extracted_title, extracted_summary = extract_summary_and_title(raw_md, page_title)
            # Use page_title if it is specific, or extracted_title
            effective_title = page_title if not is_root else extracted_title
            effective_title_escaped = effective_title.replace('"', '\\"')
            summary_escaped = escape_kotlin_multiline(extracted_summary).replace('"', '\\"')

            lesson_slug = to_id(os.path.splitext(page_rel_path)[0])
            lesson_id = f"rbe-{lesson_slug}"

            blocks = parse_markdown_blocks(raw_md, page_rel_path)

            lines.append("                                LearningLesson(")
            lines.append(f'                                    id = "{lesson_id}",')
            lines.append('                                    chapterId = "",')
            lines.append(f'                                    order = {l_idx},')
            lines.append(f'                                    title = "{effective_title_escaped}",')
            lines.append(f'                                    summary = "{summary_escaped}",')
            lines.append('                                    estimatedMinutes = 10,')
            lines.append('                                    blocks =')
            lines.append('                                        listOf(')

            for b in blocks:
                if b[0] == 'markdown':
                    md_text = escape_kotlin_multiline(b[1])
                    lines.append('                                            LessonBlock.Markdown(')
                    lines.append('                                                """')
                    for line in md_text.splitlines():
                        lines.append(f'                                                {line}')
                    lines.append('                                                """.trimIndent(),')
                    lines.append('                                            ),')
                elif b[0] == 'code':
                    code_lang = b[1]
                    code_text = escape_kotlin_multiline(b[2])
                    code_runnable = "true" if b[3] else "false"
                    is_rust = code_lang.lower() in ('rust', 'rs')
                    playground_prop = ',\n                                                playgroundUrl = "https://play.rust-lang.org/"' if is_rust else ''
                    lines.append(f'                                            LessonBlock.Code(')
                    lines.append(f'                                                language = "{code_lang}",')
                    lines.append('                                                code =')
                    lines.append('                                                    """')
                    for line in code_text.splitlines():
                        lines.append(f'                                                    {line}')
                    lines.append('                                                    """.trimIndent(),')
                    lines.append(f'                                                runnable = {code_runnable}{playground_prop},')
                    lines.append('                                            ),')

            lines.append('                                        ),')
            lines.append('                                ),')

        lines.append("                            ),")
        lines.append("                    ),")

    lines.append("                ),")
    lines.append("        )")
    lines.append("")
    lines.append("    private fun chapter(")
    lines.append("        id: String,")
    lines.append("        order: Int,")
    lines.append("        title: String,")
    lines.append("        description: String,")
    lines.append("        lessons: List<LearningLesson>,")
    lines.append("    ) = LearningChapter(")
    lines.append("        id = id,")
    lines.append("        courseId = COURSE_ID,")
    lines.append("        order = order,")
    lines.append("        title = title,")
    lines.append("        description = description,")
    lines.append("        lessons = lessons.map { it.copy(chapterId = id) },")
    lines.append("    )")
    lines.append("}")
    lines.append("")

    full_content = "\n".join(lines)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write(full_content)

    print(f"Successfully generated Rust by Example course:")
    print(f"  Target: {output_path}")
    print(f"  Chapters: {total_chapters}")
    print(f"  Lessons: {total_lessons}")
    print(f"  Source Commit: {commit_sha}")


def main():
    parser = argparse.ArgumentParser(description="Generate Rust by Example course for CodeMateX.")
    parser.add_argument(
        "--repo-path",
        default="/Users/hossain/dev/repos/tmp-on-demand/rust-by-example",
        help="Path to local clone of rust-by-example repository",
    )
    parser.add_argument(
        "--output",
        default=os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
            "app", "src", "main", "java", "dev", "hossain", "codematex", "data", "repository", "course",
            "RustByExampleCourseContent.kt"
        ),
        help="Target Kotlin output file path",
    )
    args = parser.parse_args()

    generate_course_content(args.repo_path, args.output)


if __name__ == "__main__":
    main()
