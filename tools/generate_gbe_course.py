#!/usr/bin/env python3
"""
Go By Example Course Generator for CodeMateX.

Parses content from the gobyexample repository (https://github.com/mmcgrana/gobyexample)
and generates GoByExampleCourseContent.kt conforming to CodeMateX's LearningModels data structures.

License of Go by Example:
Creative Commons Attribution 3.0 Unported License (CC-BY 3.0)
Copyright Mark McGranaghan and Eli Bendersky.
"""

import argparse
import glob
import os
import re
import subprocess
import sys


DASH_PAT = re.compile(r'-+')
DOCS_PAT = re.compile(r'^(\s*(//|#)\s|\s*//$)')


# Logical grouping of the 85 examples into 7 cohesive chapters
CHAPTER_DEFINITIONS = [
    (
        "gbe-basics-and-types",
        "Basics & Types",
        "Essential Go syntax, types, constants, control flow with for/if/switch, and core collections.",
        [
            "Hello World",
            "Values",
            "Variables",
            "Constants",
            "For",
            "If/Else",
            "Switch",
            "Arrays",
            "Slices",
            "Maps",
        ],
    ),
    (
        "gbe-functions-and-flow",
        "Functions & Control Flow",
        "Functions, multiple return values, variadic functions, closures, recursion, pointers, and runes.",
        [
            "Functions",
            "Multiple Return Values",
            "Variadic Functions",
            "Closures",
            "Recursion",
            "Range over Built-in Types",
            "Pointers",
            "Strings and Runes",
        ],
    ),
    (
        "gbe-structs-methods-interfaces",
        "Structs, Methods & Interfaces",
        "Custom data structures, methods, interfaces, enums, composition, generics, iterators, and errors.",
        [
            "Structs",
            "Methods",
            "Interfaces",
            "Enums",
            "Struct Embedding",
            "Generics",
            "Range over Iterators",
            "Errors",
            "Custom Errors",
        ],
    ),
    (
        "gbe-concurrency",
        "Concurrency & Goroutines",
        "Goroutines, channels, synchronization, select, timeouts, timers, worker pools, waitgroups, and mutexes.",
        [
            "Goroutines",
            "Channels",
            "Channel Buffering",
            "Channel Synchronization",
            "Channel Directions",
            "Select",
            "Timeouts",
            "Non-Blocking Channel Operations",
            "Closing Channels",
            "Range over Channels",
            "Timers",
            "Tickers",
            "Worker Pools",
            "WaitGroups",
            "Rate Limiting",
            "Atomic Counters",
            "Mutexes",
            "Stateful Goroutines",
        ],
    ),
    (
        "gbe-standard-library",
        "Standard Library & Data",
        "Sorting, panic/defer/recover, string utilities, templates, regular expressions, JSON, XML, and time.",
        [
            "Sorting",
            "Sorting by Functions",
            "Panic",
            "Defer",
            "Recover",
            "String Functions",
            "String Formatting",
            "Text Templates",
            "Regular Expressions",
            "JSON",
            "XML",
            "Time",
            "Epoch",
            "Time Formatting / Parsing",
            "Random Numbers",
            "Number Parsing",
        ],
    ),
    (
        "gbe-io-and-files",
        "File System & I/O",
        "URL parsing, crypto hashes, base64, file operations, line filters, path handling, and temporary files.",
        [
            "URL Parsing",
            "SHA256 Hashes",
            "Base64 Encoding",
            "Reading Files",
            "Writing Files",
            "Line Filters",
            "File Paths",
            "Directories",
            "Temporary Files and Directories",
        ],
    ),
    (
        "gbe-systems-and-networking",
        "Systems & Networking",
        "Embed directive, unit testing and benchmarking, CLI args/flags, environment variables, HTTP, TCP, context, and processes.",
        [
            "Embed Directive",
            "Testing and Benchmarking",
            "Command-Line Arguments",
            "Command-Line Flags",
            "Command-Line Subcommands",
            "Environment Variables",
            "Logging",
            "HTTP Client",
            "HTTP Server",
            "TCP Server",
            "Context",
            "Spawning Processes",
            "Exec'ing Processes",
            "Signals",
            "Exit",
        ],
    ),
]


def to_slug(name):
    slug = name.lower().replace(' ', '-').replace('/', '-').replace("'", '')
    return DASH_PAT.sub('-', slug)


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


def escape_kotlin_multiline(text):
    """
    Escapes characters for inclusion inside a Kotlin multiline string literal (\"\"\" ... \"\"\").
    In Kotlin raw multiline strings:
    - `$` must be written as `${'$'}` to prevent variable interpolation.
    - Triple quotes `\"\"\"` must be written as `\"\"${'\"'}\"\"`.
    """
    text = text.replace('$', "${'$'}")
    text = text.replace('"""', '""${\'"\'}""')
    return text


def parse_segs(lines):
    """
    Splits source/script lines into alternating documentation and code segments.
    """
    segs = []
    last_seen = ''
    for raw_line in lines:
        line = raw_line.replace('\t', '    ')
        if not line.strip():
            last_seen = ''
            continue
        is_doc = bool(DOCS_PAT.match(line))
        new_doc = (last_seen == '') or (last_seen != 'docs' and segs and segs[-1]['docs'] != '')
        new_code = (last_seen == '') or (last_seen != 'code' and segs and segs[-1]['code'] != '')
        if is_doc:
            trimmed = DOCS_PAT.sub('', line)
            if new_doc:
                segs.append({'docs': trimmed, 'code': ''})
            else:
                segs[-1]['docs'] += '\n' + trimmed
            last_seen = 'docs'
        else:
            if new_code:
                segs.append({'docs': '', 'code': line})
            else:
                if not segs[-1]['code']:
                    segs[-1]['code'] = line
                else:
                    segs[-1]['code'] += '\n' + line
            last_seen = 'code'
    return segs


def extract_summary(lines):
    """
    Extracts the leading doc comment at the top of the Go source file.
    """
    summary_lines = []
    for line in lines:
        if line.startswith('//'):
            text = line[2:].strip()
            if text:
                summary_lines.append(text)
        elif line.strip() == '':
            if summary_lines:
                break
        else:
            break
    return ' '.join(summary_lines)


def render_example(base_dir, slug, title):
    d = os.path.join(base_dir, 'examples', slug)
    go_files = glob.glob(d + '/*.go')
    sh_files = glob.glob(d + '/*.sh')
    hash_files = glob.glob(d + '/*.hash')

    if not go_files:
        raise FileNotFoundError(f"Missing .go file in {d}")
    if not sh_files:
        raise FileNotFoundError(f"Missing .sh file in {d}")

    go_code = open(go_files[0], encoding='utf-8').read()
    sh_code = open(sh_files[0], encoding='utf-8').read()

    url_hash = ""
    if hash_files:
        hash_lines = open(hash_files[0], encoding='utf-8').read().splitlines()
        if len(hash_lines) > 1 and hash_lines[1].strip():
            url_hash = hash_lines[1].strip()

    playground_url = f"https://go.dev/play/p/{url_hash}" if url_hash else None

    summary = extract_summary(go_code.splitlines())
    if not summary:
        summary = f"Learn how to use {title} in Go with complete runnable code and explanations."

    sh_segs = parse_segs(sh_code.splitlines())

    blocks = []
    # 1. Lead-in explanation
    blocks.append(('markdown', summary))

    # 2. Main complete Go code snippet
    # testing-and-benchmarking uses main_test.go without a func main()
    is_runnable = slug != 'testing-and-benchmarking'
    # embed-directive relies on local files that cannot run in isolated web sandboxes
    is_pg_runnable = slug not in ('testing-and-benchmarking', 'embed-directive')
    blocks.append(('code', 'go', go_code.strip(), is_runnable, playground_url, is_pg_runnable))

    # 3. Running the program console output & explanation
    sh_md_parts = ["### Running the Program\n"]
    for seg in sh_segs:
        if seg['docs']:
            sh_md_parts.append(seg['docs'] + "\n")
        if seg['code']:
            sh_md_parts.append(f"```console\n{seg['code']}\n```\n")
    blocks.append(('markdown', '\n'.join(sh_md_parts).strip()))

    return summary, blocks


def generate_course_content(repo_path, output_path):
    commit_sha = get_git_commit(repo_path)
    examples_txt_path = os.path.join(repo_path, 'examples.txt')

    if not os.path.exists(examples_txt_path):
        print(f"Error: examples.txt not found at {examples_txt_path}", file=sys.stderr)
        sys.exit(1)

    with open(examples_txt_path, 'r', encoding='utf-8') as f:
        all_examples = [line.strip() for line in f if line.strip() and not line.startswith('#')]

    # Verify all 85 examples are accounted for in CHAPTER_DEFINITIONS
    chapter_example_set = set()
    for _, _, _, titles in CHAPTER_DEFINITIONS:
        for t in titles:
            chapter_example_set.add(t)

    missing_in_chapters = set(all_examples) - chapter_example_set
    if missing_in_chapters:
        print(f"Error: Examples missing from CHAPTER_DEFINITIONS: {missing_in_chapters}", file=sys.stderr)
        sys.exit(1)

    lines = []
    lines.append("package dev.hossain.codematex.data.repository.course")
    lines.append("")
    lines.append("import dev.hossain.codematex.data.model.LearningChapter")
    lines.append("import dev.hossain.codematex.data.model.LearningCourse")
    lines.append("import dev.hossain.codematex.data.model.LearningLesson")
    lines.append("import dev.hossain.codematex.data.model.LessonBlock")
    lines.append("")
    lines.append("/**")
    lines.append(" * Bundled 'Go by Example' course directly sourced from the official")
    lines.append(" * [gobyexample](https://github.com/mmcgrana/gobyexample) repository.")
    lines.append(" *")
    lines.append(" * Authors: Mark McGranaghan and Eli Bendersky")
    lines.append(" * License: Creative Commons Attribution 3.0 Unported License (CC-BY 3.0)")
    lines.append(" *   http://creativecommons.org/licenses/by/3.0/")
    lines.append(" *")
    lines.append(f" * Source commit: {commit_sha}")
    lines.append(" * Generated by tools/generate_gbe_course.py")
    lines.append(" */")
    lines.append("object GoByExampleCourseContent {")
    lines.append('    const val COURSE_ID = "go-by-example"')
    lines.append("")
    lines.append("    val course =")
    lines.append("        LearningCourse(")
    lines.append("            id = COURSE_ID,")
    lines.append('            language = "Go",')
    lines.append('            title = "Go by Example",')
    lines.append("            description =")
    lines.append('                """')
    lines.append("                A comprehensive collection of hands-on, annotated examples covering Go syntax,")
    lines.append("                concurrency, standard library, and systems programming.")
    lines.append('                """.trimIndent(),')
    lines.append("            version = 1,")
    lines.append("            chapters =")
    lines.append("                listOf(")

    total_lessons = 0
    total_runnable = 0

    for ch_idx, (ch_id, ch_title, ch_desc, example_titles) in enumerate(CHAPTER_DEFINITIONS, start=1):
        ch_title_escaped = ch_title.replace('"', '\\"')
        ch_desc_escaped = escape_kotlin_multiline(ch_desc).replace('"', '\\"')

        lines.append("                    chapter(")
        lines.append(f'                        id = "{ch_id}",')
        lines.append(f"                        order = {ch_idx},")
        lines.append(f'                        title = "{ch_title_escaped}",')
        lines.append("                        description =")
        lines.append('                            """')
        for d_line in ch_desc_escaped.splitlines():
            lines.append(f'                            {d_line}')
        lines.append('                            """.trimIndent(),')
        lines.append("                        lessons =")
        lines.append("                            listOf(")

        for l_idx, ex_title in enumerate(example_titles, start=1):
            total_lessons += 1
            slug = to_slug(ex_title)
            lesson_id = f"gbe-{slug}"

            summary, blocks = render_example(repo_path, slug, ex_title)
            title_escaped = ex_title.replace('"', '\\"')
            summary_escaped = escape_kotlin_multiline(summary).replace('"', '\\"')

            lines.append("                                LearningLesson(")
            lines.append(f'                                    id = "{lesson_id}",')
            lines.append('                                    chapterId = "",')
            lines.append(f"                                    order = {l_idx},")
            lines.append(f'                                    title = "{title_escaped}",')
            lines.append('                                    summary =')
            lines.append('                                        """')
            for sum_line in summary_escaped.splitlines():
                lines.append(f'                                        {sum_line}')
            lines.append('                                        """.trimIndent(),')
            lines.append("                                    estimatedMinutes = 10,")
            lines.append("                                    blocks =")
            lines.append("                                        listOf(")

            for b in blocks:
                if b[0] == 'markdown':
                    md_text = escape_kotlin_multiline(b[1])
                    lines.append("                                            LessonBlock.Markdown(")
                    lines.append('                                                """')
                    for md_line in md_text.splitlines():
                        lines.append(f"                                                {md_line}")
                    lines.append('                                                """.trimIndent(),')
                    lines.append("                                            ),")
                elif b[0] == 'code':
                    code_lang = b[1]
                    code_text = escape_kotlin_multiline(b[2])
                    code_runnable = "true" if b[3] else "false"
                    playground_url = b[4]
                    is_pg_runnable = "true" if b[5] else "false"

                    if b[5]:
                        total_runnable += 1

                    pg_prop = f',\n                                                playgroundUrl = "{playground_url}"' if playground_url else ''
                    lines.append("                                            LessonBlock.Code(")
                    lines.append(f'                                                language = "{code_lang}",')
                    lines.append("                                                code =")
                    lines.append('                                                    """')
                    for code_line in code_text.splitlines():
                        lines.append(f"                                                    {code_line}")
                    lines.append('                                                    """.trimIndent(),')
                    lines.append(f"                                                runnable = {code_runnable}{pg_prop},")
                    lines.append(f"                                                isPlaygroundRunnable = {is_pg_runnable},")
                    lines.append("                                            ),")

            lines.append("                                        ),")
            lines.append("                                ),")

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

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f"Successfully generated {output_path}")
    print(f"Total chapters: {len(CHAPTER_DEFINITIONS)}")
    print(f"Total lessons: {total_lessons}")
    print(f"Total runnable code snippets: {total_runnable}")


def main():
    parser = argparse.ArgumentParser(description="Generate Go by Example course for CodeMateX.")
    parser.add_argument(
        "--repo-path",
        required=True,
        help="Path to local clone of mmcgrana/gobyexample",
    )
    parser.add_argument(
        "--output",
        default="app/src/main/java/dev/hossain/codematex/data/repository/course/GoByExampleCourseContent.kt",
        help="Output path for GoByExampleCourseContent.kt",
    )

    args = parser.parse_args()
    generate_course_content(args.repo_path, args.output)


if __name__ == '__main__':
    main()
