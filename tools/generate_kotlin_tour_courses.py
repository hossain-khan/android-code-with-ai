#!/usr/bin/env python3
"""
Kotlin Tour Courses Generator for CodeMateX.

Parses official Kotlin Tour markdown documentation from JetBrains (kotlin-web-site)
and generates two distinct, high-quality guided courses:
1. KotlinTourBeginnerCourseContent.kt (7 chapters: Hello World, Basic Types, Collections, Control Flow, Functions, Classes, Null Safety)
2. KotlinTourIntermediateCourseContent.kt (9 chapters: Extension Functions, Scope Functions, Lambdas with Receiver, Classes & Interfaces, Objects, Open & Special Classes, Properties, Null Safety, Libraries & APIs)

License:
Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)
Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
"""

import argparse
import os
import re
import subprocess
import sys


BEGINNER_TOPICS = [
    (
        "kt-tour-beg-hello-world",
        "Hello World",
        "Your first Kotlin program, entry point main(), variables, and string templates.",
        "kotlin-tour-hello-world.md",
    ),
    (
        "kt-tour-beg-basic-types",
        "Basic Types",
        "Integers, floating-point numbers, booleans, characters, strings, and type inference.",
        "kotlin-tour-basic-types.md",
    ),
    (
        "kt-tour-beg-collections",
        "Collections",
        "Read-only and mutable lists, sets, maps, and common collection operations.",
        "kotlin-tour-collections.md",
    ),
    (
        "kt-tour-beg-control-flow",
        "Control Flow",
        "Conditional expressions with if and when, ranges, for loops, and while loops.",
        "kotlin-tour-control-flow.md",
    ),
    (
        "kt-tour-beg-functions",
        "Functions",
        "Function declarations, parameters, named and default arguments, and lambda expressions.",
        "kotlin-tour-functions.md",
    ),
    (
        "kt-tour-beg-classes",
        "Classes",
        "Classes, properties, member functions, instances, and idiomatic data classes.",
        "kotlin-tour-classes.md",
    ),
    (
        "kt-tour-beg-null-safety",
        "Null Safety",
        "Nullable types, safe calls (?.), Elvis operator (?:), and preventing null pointer exceptions.",
        "kotlin-tour-null-safety.md",
    ),
]

INTERMEDIATE_TOPICS = [
    (
        "kt-tour-int-extension-functions",
        "Extension Functions",
        "Extending existing classes, receiver concepts, and extension-oriented API design.",
        "kotlin-tour-intermediate-extension-functions.md",
    ),
    (
        "kt-tour-int-scope-functions",
        "Scope Functions",
        "Executing code blocks within context of an object using let, run, with, apply, and also.",
        "kotlin-tour-intermediate-scope-functions.md",
    ),
    (
        "kt-tour-int-lambdas-receiver",
        "Lambdas with Receiver",
        "Function types with receiver and building type-safe domain-specific languages (DSLs).",
        "kotlin-tour-intermediate-lambdas-receiver.md",
    ),
    (
        "kt-tour-int-classes-interfaces",
        "Classes and Interfaces",
        "Abstract classes, interfaces, class inheritance, polymorphism, and class delegation.",
        "kotlin-tour-intermediate-classes-interfaces.md",
    ),
    (
        "kt-tour-int-objects",
        "Objects",
        "Singleton object declarations, companion objects, and anonymous object expressions.",
        "kotlin-tour-intermediate-objects.md",
    ),
    (
        "kt-tour-int-open-special-classes",
        "Open and Special Classes",
        "Open classes, sealed hierarchies, enum classes, and zero-cost value classes.",
        "kotlin-tour-intermediate-open-special-classes.md",
    ),
    (
        "kt-tour-int-properties",
        "Properties",
        "Backing fields, extension properties, and delegated properties (by lazy, observable).",
        "kotlin-tour-intermediate-properties.md",
    ),
    (
        "kt-tour-int-null-safety",
        "Advanced Null Safety",
        "Smart casting, safe cast operator (as?), non-null assertions (!!), and platform types.",
        "kotlin-tour-intermediate-null-safety.md",
    ),
    (
        "kt-tour-int-libraries-and-apis",
        "Libraries and APIs",
        "Kotlin standard library capabilities, kotlinx ecosystem libraries, and opt-in APIs.",
        "kotlin-tour-intermediate-libraries-and-apis.md",
    ),
]


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
    text = text.replace('$', "${'$'}")
    text = text.replace('"""', '""${\'"\'}""')
    return text


def clean_code(code_str):
    # Remove JetBrains sample markers
    code_str = re.sub(r'^\s*(?:>\s*)?//sample(?:Start|End)\s*\n?', '', code_str, flags=re.MULTILINE)
    return code_str.strip()


def rewrite_doc_links(content):
    base_url = "https://kotlinlang.org/docs/"

    def replace_link(match):
        label = match.group(1)
        url = match.group(2).strip()
        if url.startswith('http://') or url.startswith('https://') or url.startswith('#') or url.startswith('mailto:'):
            return f'[{label}]({url})'
        if url.endswith('.md') or '.md#' in url:
            parts = url.split('#')
            path_part = parts[0]
            anchor_part = ('#' + parts[1]) if len(parts) > 1 else ''
            html_path = path_part.replace('.md', '.html')
            return f'[{label}]({base_url}{html_path}{anchor_part})'
        return f'[{label}]({url})'

    return re.sub(r'\[([^\]]+)\]\(([^)]+)\)', replace_link, content)


def clean_markdown_text(md_text):
    # 1. Strip title comment: [//]: # (title: ...)
    md_text = re.sub(r'\[//\]:\s*#\s*\(\s*title:[^\)]*\)\s*', '', md_text)
    # 2. Strip Writerside tags
    md_text = re.sub(r'<no-index\s*/>', '', md_text)
    md_text = re.sub(r'<web-summary>.*?</web-summary>', '', md_text, flags=re.DOTALL)
    md_text = re.sub(r'<seealso>.*?</seealso>', '', md_text, flags=re.DOTALL)
    md_text = re.sub(r'<list id="tour-nav">.*?</list>', '', md_text, flags=re.DOTALL)
    # 3. Clean deflist tags: <deflist> <def title="Hint"> body </def> </deflist>
    def replace_def(m):
        title = m.group(1)
        body = m.group(2).strip()
        return f"\n> **{title}**: {body}\n"
    md_text = re.sub(r'<deflist[^>]*>\s*<def\s+title="([^"]+)">\s*(.*?)\s*</def>\s*</deflist>', replace_def, md_text, flags=re.DOTALL)
    # 4. Clean attribute blocks like {style="note"} or {style="tip"}
    md_text = re.sub(r'\{\s*style\s*=\s*"note"\s*\}', '', md_text)
    md_text = re.sub(r'\{\s*style\s*=\s*"tip"\s*\}', '', md_text)
    # 5. Clean heading attributes: ## Heading {completion-point="true"} -> ## Heading
    md_text = re.sub(r'^(#{1,6}\s+.*?)\s*\{[^}]*\}\s*$', r'\1', md_text, flags=re.MULTILINE)
    # 6. Clean table separator |---|---| before code blocks
    md_text = re.sub(r'^\s*\|---\|---\|\s*$', '', md_text, flags=re.MULTILINE)
    # 7. Convert relative doc links to official kotlinlang.org URLs
    md_text = rewrite_doc_links(md_text)
    return md_text.strip()


def parse_topic_file(file_path):
    raw_content = open(file_path, 'r', encoding='utf-8').read()

    # Pattern finding ```lang ... ``` and optional trailing attributes {kotlin-runnable="true" ...}
    pattern = re.compile(r'```([^\n]*)\n(.*?)\n```(?:\s*\{([^}]*)\})?', re.MULTILINE | re.DOTALL)
    blocks = []
    last_end = 0

    for match in pattern.finditer(raw_content):
        start, end = match.span()
        md_text = raw_content[last_end:start]
        cleaned_md = clean_markdown_text(md_text)
        if cleaned_md:
            blocks.append(('markdown', cleaned_md))

        fence_lang = match.group(1).strip().lower() or 'kotlin'
        code_content = clean_code(match.group(2))
        attrs = match.group(3) or ''

        # Determine runnability
        is_explicit_runnable = 'kotlin-runnable="true"' in attrs
        is_exercise_template = '// Write your code here' in code_content
        # Non-stdlib imports like kotlinx.datetime require external libraries not in basic playground
        has_unsupported_import = 'import kotlinx.datetime' in code_content
        is_kotlin = fence_lang in ('kotlin', 'kt')

        is_runnable = is_kotlin and not is_exercise_template and not has_unsupported_import
        is_pg_runnable = is_runnable and (is_explicit_runnable or 'fun main()' in code_content)

        blocks.append(('code', fence_lang, code_content, is_runnable, is_pg_runnable))
        last_end = end

    trailing_md = clean_markdown_text(raw_content[last_end:])
    if trailing_md:
        blocks.append(('markdown', trailing_md))

    return blocks


def generate_course_content(
    repo_path,
    output_path,
    class_name,
    course_id,
    course_title,
    course_desc,
    topics,
    commit_sha,
):
    tour_dir = os.path.join(repo_path, 'docs', 'topics', 'tour')

    lines = []
    lines.append("package dev.hossain.codematex.data.repository.course")
    lines.append("")
    lines.append("import dev.hossain.codematex.data.model.LearningChapter")
    lines.append("import dev.hossain.codematex.data.model.LearningCourse")
    lines.append("import dev.hossain.codematex.data.model.LearningLesson")
    lines.append("import dev.hossain.codematex.data.model.LessonBlock")
    lines.append("")
    lines.append("/**")
    lines.append(f" * Bundled '{course_title}' course directly sourced from the official")
    lines.append(" * [JetBrains Kotlin Documentation](https://kotlinlang.org/docs/kotlin-tour-welcome.html) repository.")
    lines.append(" *")
    lines.append(" * License: Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0)")
    lines.append(f" * Source commit: {commit_sha}")
    lines.append(" * Generated by tools/generate_kotlin_tour_courses.py")
    lines.append(" */")
    lines.append(f"object {class_name} {{")
    lines.append(f'    const val COURSE_ID = "{course_id}"')
    lines.append("")
    lines.append("    val course =")
    lines.append("        LearningCourse(")
    lines.append("            id = COURSE_ID,")
    lines.append('            language = "Kotlin",')
    lines.append(f'            title = "{course_title}",')
    lines.append("            description =")
    lines.append('                """')
    for d_line in course_desc.splitlines():
        lines.append(f"                {d_line.strip()}")
    lines.append('                """.trimIndent(),')
    lines.append("            version = 1,")
    lines.append("            chapters =")
    lines.append("                listOf(")

    total_lessons = 0
    total_pg_runnable = 0

    for ch_idx, (ch_id, ch_title, ch_desc, fname) in enumerate(topics, start=1):
        fpath = os.path.join(tour_dir, fname)
        if not os.path.exists(fpath):
            print(f"Error: {fpath} not found!", file=sys.stderr)
            sys.exit(1)

        blocks = parse_topic_file(fpath)
        ch_title_escaped = ch_title.replace('"', '\\"')
        ch_desc_escaped = escape_kotlin_multiline(ch_desc).replace('"', '\\"')

        lines.append("                    chapter(")
        lines.append(f'                        id = "{ch_id}",')
        lines.append(f"                        order = {ch_idx},")
        lines.append(f'                        title = "{ch_title_escaped}",')
        lines.append("                        description =")
        lines.append('                            """')
        lines.append(f"                            {ch_desc_escaped}")
        lines.append('                            """.trimIndent(),')
        lines.append("                        lessons =")
        lines.append("                            listOf(")

        total_lessons += 1
        lesson_id = f"{ch_id}-lesson"
        lesson_title = ch_title_escaped
        lesson_summary = ch_desc_escaped

        lines.append("                                LearningLesson(")
        lines.append(f'                                    id = "{lesson_id}",')
        lines.append('                                    chapterId = "",')
        lines.append("                                    order = 1,")
        lines.append(f'                                    title = "{lesson_title}",')
        lines.append("                                    summary =")
        lines.append('                                        """')
        lines.append(f"                                        {lesson_summary}")
        lines.append('                                        """.trimIndent(),')
        lines.append("                                    estimatedMinutes = 15,")
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
                code_pg_runnable = "true" if b[4] else "false"

                if b[4]:
                    total_pg_runnable += 1

                lines.append("                                            LessonBlock.Code(")
                lines.append(f'                                                language = "{code_lang}",')
                lines.append("                                                code =")
                lines.append('                                                    """')
                for code_line in code_text.splitlines():
                    lines.append(f"                                                    {code_line}")
                lines.append('                                                    """.trimIndent(),')
                lines.append(f"                                                runnable = {code_runnable},")
                lines.append(f"                                                isPlaygroundRunnable = {code_pg_runnable},")
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
    print(f"Total chapters/lessons: {total_lessons}")
    print(f"Total playground runnable snippets: {total_pg_runnable}")


def main():
    parser = argparse.ArgumentParser(description="Generate Kotlin Tour courses for CodeMateX.")
    parser.add_argument(
        "--repo-path",
        required=True,
        help="Path to local clone of JetBrains/kotlin-web-site",
    )
    args = parser.parse_args()

    commit_sha = get_git_commit(args.repo_path)
    print(f"Source commit: {commit_sha}")

    # 1. Beginner Course
    beginner_out = "app/src/main/java/dev/hossain/codematex/data/repository/course/KotlinTourBeginnerCourseContent.kt"
    generate_course_content(
        repo_path=args.repo_path,
        output_path=beginner_out,
        class_name="KotlinTourBeginnerCourseContent",
        course_id="kotlin-tour-beginner",
        course_title="Kotlin Tour: Beginner",
        course_desc="The official beginner tour of Kotlin from JetBrains. Grasp the fundamentals: variables, basic types, collections, control flow, functions, classes, and null safety.",
        topics=BEGINNER_TOPICS,
        commit_sha=commit_sha,
    )

    # 2. Intermediate Course
    intermediate_out = "app/src/main/java/dev/hossain/codematex/data/repository/course/KotlinTourIntermediateCourseContent.kt"
    generate_course_content(
        repo_path=args.repo_path,
        output_path=intermediate_out,
        class_name="KotlinTourIntermediateCourseContent",
        course_id="kotlin-tour-intermediate",
        course_title="Kotlin Tour: Intermediate",
        course_desc="Take your Kotlin understanding to the next level with official JetBrains guidance on extension functions, scope functions, receiver lambdas, delegation, and modern API design.",
        topics=INTERMEDIATE_TOPICS,
        commit_sha=commit_sha,
    )


if __name__ == '__main__':
    main()
