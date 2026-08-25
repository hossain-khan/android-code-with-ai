package dev.hossain.codematex.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            SessionDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate1To2_addsMessageIdColumn() {
        helper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO sessions (id, topic, title, summary, messageCount, lastActiveAt, modelUsed) " +
                    "VALUES ('s1', 'KOTLIN', 'Title', 'Summary', 1, 1, 'model')",
            )
            db.execSQL(
                "INSERT INTO messages (sessionId, type, content, timestamp, orderIndex) " +
                    "VALUES ('s1', 'user', 'hello', 1, 0)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, SessionDatabase.MIGRATION_1_2).use { db ->
            val cursor = db.query("SELECT messageId FROM messages")
            cursor.use {
                assertTrue(cursor.moveToFirst())
                assertEquals("", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate2To3_mapsEnumNamesToStableIds() {
        helper.createDatabase(TEST_DB_NAME, 2).use { db ->
            db.execSQL(
                "INSERT INTO sessions (id, topic, title, summary, messageCount, lastActiveAt, modelUsed) " +
                    "VALUES ('s1', 'KOTLIN', 'Title', 'Summary', 2, 1, 'model')",
            )
            db.execSQL(
                "INSERT INTO sessions (id, topic, title, summary, messageCount, lastActiveAt, modelUsed) " +
                    "VALUES ('s2', 'CORRUPT_TOPIC', 'Title', 'Summary', 1, 1, 'model')",
            )
            db.execSQL(
                "INSERT INTO messages (sessionId, messageId, type, content, timestamp, orderIndex) " +
                    "VALUES ('s1', 'm1', 'user', 'hello', 1, 0)",
            )
            db.execSQL(
                "INSERT INTO messages (sessionId, messageId, type, content, timestamp, orderIndex) " +
                    "VALUES ('s1', 'm2', 'corrupt_type', 'world', 1, 1)",
            )
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, SessionDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT topic FROM sessions ORDER BY id").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("kotlin", cursor.getString(0))
                assertTrue(cursor.moveToNext())
                assertEquals("unknown", cursor.getString(0))
            }

            db.query("SELECT type FROM messages ORDER BY messageId").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("user", cursor.getString(0))
                assertTrue(cursor.moveToNext())
                assertEquals("unknown", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate1To3_runsAllMigrations() {
        helper.createDatabase(TEST_DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO sessions (id, topic, title, summary, messageCount, lastActiveAt, modelUsed) " +
                    "VALUES ('s1', 'PYTHON', 'Title', 'Summary', 1, 1, 'model')",
            )
            db.execSQL(
                "INSERT INTO messages (sessionId, type, content, timestamp, orderIndex) " +
                    "VALUES ('s1', 'agent', 'hello', 1, 0)",
            )
        }

        helper
            .runMigrationsAndValidate(
                TEST_DB_NAME,
                3,
                true,
                SessionDatabase.MIGRATION_1_2,
                SessionDatabase.MIGRATION_2_3,
            ).use { db ->
                db.query("SELECT topic FROM sessions").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("python", cursor.getString(0))
                }
                db.query("SELECT messageId, type FROM messages").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("", cursor.getString(0))
                    assertEquals("agent", cursor.getString(1))
                }
            }
    }

    private companion object {
        private const val TEST_DB_NAME = "test-sessions.db"
    }
}
