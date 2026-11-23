#!/usr/bin/env python3
"""Unit tests for tools/generate_kotlin_tour_courses.py."""

import unittest

from generate_kotlin_tour_courses import parse_topic_content


class TestGenerateKotlinTourCourses(unittest.TestCase):
    def test_blockquote_nested_code_fence_does_not_invert_subsequent_blocks(self):
        sample = """\
> To prevent unwanted modifications, you can create a read-only view of a mutable list:
>
> ```kotlin
>     val shapes: MutableList<String> = mutableListOf("triangle", "square")
>     val shapesLocked: List<String> = shapes
> ```
> This is also called **casting**.
>
{style="tip"}

Lists are ordered so to access an item in a list, use `[]`:

```kotlin
fun main() {
//sampleStart
    val readOnlyShapes = listOf("triangle", "square")
    println(readOnlyShapes[0])
//sampleEnd
}
```
{kotlin-runnable="true" kotlin-min-compiler-version="1.3" id="kotlin-tour-list-access"}
"""
        blocks = parse_topic_content(sample)

        self.assertEqual(len(blocks), 4)
        self.assertEqual(blocks[0][0], "markdown")
        self.assertIn("read-only view", blocks[0][1])

        self.assertEqual(blocks[1][0], "code")
        self.assertEqual(blocks[1][1], "kotlin")
        self.assertEqual(
            blocks[1][2],
            'val shapes: MutableList<String> = mutableListOf("triangle", "square")\n'
            "val shapesLocked: List<String> = shapes",
        )
        self.assertTrue(blocks[1][3])
        self.assertFalse(blocks[1][4])

        self.assertEqual(blocks[2][0], "markdown")
        self.assertIn("This is also called **casting**.", blocks[2][1])
        self.assertIn("Lists are ordered", blocks[2][1])
        self.assertNotIn("style=", blocks[2][1])
        self.assertNotIn("fun main()", blocks[2][1])

        self.assertEqual(blocks[3][0], "code")
        self.assertEqual(blocks[3][1], "kotlin")
        self.assertNotIn("//sampleStart", blocks[3][2])
        self.assertNotIn("//sampleEnd", blocks[3][2])
        self.assertNotIn("kotlin-runnable", blocks[3][2])
        self.assertTrue(blocks[3][3])
        self.assertTrue(blocks[3][4])

    def test_exercise_template_is_not_marked_runnable(self):
        sample = """\
```kotlin
fun main() {
    // Write your code here
}
```
{validate="false" kotlin-runnable="true" id="ex-1"}
"""
        blocks = parse_topic_content(sample)
        self.assertEqual(len(blocks), 1)
        self.assertEqual(blocks[0][0], "code")
        self.assertFalse(blocks[0][3])
        self.assertFalse(blocks[0][4])


if __name__ == "__main__":
    unittest.main()
