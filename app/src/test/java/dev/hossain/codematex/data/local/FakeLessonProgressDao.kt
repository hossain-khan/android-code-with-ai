package dev.hossain.codematex.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLessonProgressDao : LessonProgressDao {
    private val data = MutableStateFlow<Map<String, LessonProgressEntity>>(emptyMap())

    override fun observeAllProgress(): Flow<List<LessonProgressEntity>> = data.map { it.values.toList() }

    override fun observeCourseProgress(courseId: String): Flow<List<LessonProgressEntity>> =
        data.map { map ->
            map.values.filter { it.courseId == courseId }
        }

    override fun observeLessonProgress(lessonId: String): Flow<LessonProgressEntity?> = data.map { it[lessonId] }

    override suspend fun getLessonProgress(lessonId: String): LessonProgressEntity? = data.value[lessonId]

    override suspend fun upsert(progress: LessonProgressEntity) {
        data.update { it + (progress.lessonId to progress) }
    }

    override suspend fun upsertAll(progressList: List<LessonProgressEntity>) {
        data.update { current ->
            current + progressList.associateBy { it.lessonId }
        }
    }

    override suspend fun deleteCourseProgress(courseId: String) {
        data.update { map -> map.filterValues { it.courseId != courseId } }
    }

    override suspend fun deleteAllProgress() {
        data.value = emptyMap()
    }
}
