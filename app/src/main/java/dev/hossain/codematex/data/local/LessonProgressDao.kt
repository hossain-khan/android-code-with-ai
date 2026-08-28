package dev.hossain.codematex.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {
    /** Observes all persisted lesson progress records for [courseId]. */
    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId")
    fun observeCourseProgress(courseId: String): Flow<List<LessonProgressEntity>>

    /** Observes the persisted progress record for [lessonId], if one exists. */
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    fun observeLessonProgress(lessonId: String): Flow<LessonProgressEntity?>

    /** Returns the persisted progress record for [lessonId], if one exists. */
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun getLessonProgress(lessonId: String): LessonProgressEntity?

    /** Inserts or replaces the progress record identified by its lesson ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    /** Deletes all progress records belonging to [courseId]. */
    @Query("DELETE FROM lesson_progress WHERE courseId = :courseId")
    suspend fun deleteCourseProgress(courseId: String)
}
