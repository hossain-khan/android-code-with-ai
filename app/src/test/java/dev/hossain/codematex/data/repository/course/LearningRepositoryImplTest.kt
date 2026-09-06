package dev.hossain.codematex.data.repository.course

import com.google.common.truth.Truth.assertThat
import dev.hossain.codematex.data.local.LessonProgressDao
import dev.hossain.codematex.data.local.LessonProgressEntity
import dev.hossain.codematex.data.model.CodingTopic
import dev.hossain.codematex.data.model.LessonStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningRepositoryImplTest {
    private lateinit var dao: FakeLessonProgressDao
    private lateinit var repository: LearningRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeLessonProgressDao()
        repository = LearningRepositoryImpl(dao)
    }

    @Test
    fun `given no progress - getCourses returns 6 bundled courses`() =
        runTest(UnconfinedTestDispatcher()) {
            val courses = repository.getCourses().first()
            assertThat(courses).hasSize(6)
        }

    @Test
    fun `getCourse returns matching course`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourse("kotlin-foundations")
            assertThat(course).isNotNull()
            assertThat(course?.id).isEqualTo("kotlin-foundations")
        }

    @Test
    fun `getCourse returns null for unknown ID`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourse("unknown-course")
            assertThat(course).isNull()
        }

    @Test
    fun `getChapter finds chapter by ID`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapter = repository.getChapter("kotlin-getting-started")
            assertThat(chapter).isNotNull()
            assertThat(chapter?.id).isEqualTo("kotlin-getting-started")
        }

    @Test
    fun `getChapter returns null for unknown ID`() =
        runTest(UnconfinedTestDispatcher()) {
            val chapter = repository.getChapter("unknown-chapter")
            assertThat(chapter).isNull()
        }

    @Test
    fun `getLesson finds lesson by ID`() =
        runTest(UnconfinedTestDispatcher()) {
            val lesson = repository.getLesson("kotlin-hello-world")
            assertThat(lesson).isNotNull()
            assertThat(lesson?.id).isEqualTo("kotlin-hello-world")
        }

    @Test
    fun `getLesson returns null for unknown ID`() =
        runTest(UnconfinedTestDispatcher()) {
            val lesson = repository.getLesson("unknown-lesson")
            assertThat(lesson).isNull()
        }

    @Test
    fun `getCourseForLesson returns parent course`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourseForLesson("kotlin-hello-world")
            assertThat(course).isNotNull()
            assertThat(course?.id).isEqualTo("kotlin-foundations")
        }

    @Test
    fun `getCourseForLesson returns null for unknown lesson`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourseForLesson("unknown-lesson")
            assertThat(course).isNull()
        }

    @Test
    fun `getCourseForTopic returns matching course`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourseForTopic(CodingTopic.KOTLIN)
            assertThat(course).isNotNull()
            assertThat(course?.id).isEqualTo("kotlin-foundations")
        }

    @Test
    fun `getCourseForTopic returns null for topic without course`() =
        runTest(UnconfinedTestDispatcher()) {
            val course = repository.getCourseForTopic(CodingTopic.ANDROID) // Assuming ANDROID doesn't have a bundled course
            assertThat(course).isNull()
        }

    @Test
    fun `getTopicsWithCourses returns 6 topics`() =
        runTest(UnconfinedTestDispatcher()) {
            val topics = repository.getTopicsWithCourses()
            assertThat(topics).hasSize(6)
            assertThat(topics).contains(CodingTopic.KOTLIN)
        }

    @Test
    fun `observeCourseProgress emits correct progress with no stored data`() =
        runTest(UnconfinedTestDispatcher()) {
            val progress = repository.observeCourseProgress("kotlin-foundations").first()
            assertThat(progress.courseId).isEqualTo("kotlin-foundations")
            assertThat(progress.completedLessons).isEqualTo(0)
            assertThat(progress.completedLessonIds).isEmpty()
            assertThat(progress.currentLessonId).isEqualTo("kotlin-hello-world")
        }

    @Test
    fun `observeCourseProgress tracks completed lessons`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonCompleted("kotlin-hello-world")
            val progress = repository.observeCourseProgress("kotlin-foundations").first()
            assertThat(progress.completedLessons).isEqualTo(1)
            assertThat(progress.completedLessonIds).contains("kotlin-hello-world")
            assertThat(progress.currentLessonId).isNotEqualTo("kotlin-hello-world")
        }

    @Test
    fun `observeCourseProgress identifies current lesson`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonStarted("kotlin-variables")
            val progress = repository.observeCourseProgress("kotlin-foundations").first()
            assertThat(progress.currentLessonId).isEqualTo("kotlin-variables")
        }

    @Test
    fun `observeLessonStatus emits NOT_STARTED when no stored data`() =
        runTest(UnconfinedTestDispatcher()) {
            val status = repository.observeLessonStatus("kotlin-hello-world").first()
            assertThat(status).isEqualTo(LessonStatus.NOT_STARTED)
        }

    @Test
    fun `observeLessonStatus emits correct status after marking started`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonStarted("kotlin-hello-world")
            val status = repository.observeLessonStatus("kotlin-hello-world").first()
            assertThat(status).isEqualTo(LessonStatus.IN_PROGRESS)
        }

    @Test
    fun `markLessonStarted persists IN_PROGRESS`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonStarted("kotlin-hello-world")
            val entity = dao.getLessonProgress("kotlin-hello-world")
            assertThat(entity).isNotNull()
            assertThat(entity?.status).isEqualTo(LessonStatus.IN_PROGRESS.name)
        }

    @Test
    fun `markLessonStarted does NOT downgrade a COMPLETED lesson`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonCompleted("kotlin-hello-world")
            repository.markLessonStarted("kotlin-hello-world")
            val entity = dao.getLessonProgress("kotlin-hello-world")
            assertThat(entity?.status).isEqualTo(LessonStatus.COMPLETED.name)
        }

    @Test
    fun `markLessonStarted is no-op for unknown lesson ID`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonStarted("unknown-lesson")
            val entity = dao.getLessonProgress("unknown-lesson")
            assertThat(entity).isNull()
        }

    @Test
    fun `markLessonCompleted persists COMPLETED`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonCompleted("kotlin-hello-world")
            val entity = dao.getLessonProgress("kotlin-hello-world")
            assertThat(entity?.status).isEqualTo(LessonStatus.COMPLETED.name)
        }

    @Test
    fun `markLessonCompleted is no-op for unknown lesson ID`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonCompleted("unknown-lesson")
            val entity = dao.getLessonProgress("unknown-lesson")
            assertThat(entity).isNull()
        }

    @Test
    fun `resetCourseProgress deletes all progress for course`() =
        runTest(UnconfinedTestDispatcher()) {
            repository.markLessonCompleted("kotlin-hello-world")
            assertThat(dao.getLessonProgress("kotlin-hello-world")).isNotNull()

            repository.resetCourseProgress("kotlin-foundations")

            val progress = repository.observeCourseProgress("kotlin-foundations").first()
            assertThat(progress.completedLessons).isEqualTo(0)
            assertThat(dao.getLessonProgress("kotlin-hello-world")).isNull()
        }
}

class FakeLessonProgressDao : LessonProgressDao {
    private val data = MutableStateFlow<Map<String, LessonProgressEntity>>(emptyMap())

    override fun observeCourseProgress(courseId: String): Flow<List<LessonProgressEntity>> =
        data.map { map ->
            map.values.filter {
                it.courseId == courseId
            }
        }

    override fun observeLessonProgress(lessonId: String): Flow<LessonProgressEntity?> = data.map { it[lessonId] }

    override suspend fun getLessonProgress(lessonId: String): LessonProgressEntity? = data.value[lessonId]

    override suspend fun upsert(progress: LessonProgressEntity) {
        data.update { it + (progress.lessonId to progress) }
    }

    override suspend fun deleteCourseProgress(courseId: String) {
        data.update { map -> map.filterValues { it.courseId != courseId } }
    }
}
