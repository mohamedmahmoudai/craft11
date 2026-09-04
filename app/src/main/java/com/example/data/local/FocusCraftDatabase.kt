package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.Goal
import com.example.data.local.entity.Project
import com.example.data.local.entity.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, Project::class, Goal::class],
    version = 7,
    exportSchema = false
)
abstract class FocusCraftDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: FocusCraftDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): FocusCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusCraftDatabase::class.java,
                    "focuscraft_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(FocusDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class FocusDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDatabase(database.taskDao(), database.projectDao(), database.goalDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(taskDao: TaskDao, projectDao: ProjectDao, goalDao: GoalDao) {
            val now = System.currentTimeMillis()
            val initialTasks = listOf(
                Task(
                    id = "t1",
                    title = "مشروع التخرج",
                    subtitle = "تم الإنجاز - 09:00 ص",
                    scheduledTime = "09:00 - 11:00 ص",
                    durationMinutes = 120,
                    isCompleted = true,
                    date = now,
                    categoryColor = "TEAL",
                    isFixed = false,
                    startTime = "09:00",
                    endTime = "11:00"
                ),
                Task(
                    id = "t2",
                    title = "مراجعة تقرير الربع الثالث",
                    subtitle = "كتابة المقال التقني",
                    scheduledTime = "11:30 - 01:00 م",
                    durationMinutes = 90,
                    isCompleted = false,
                    date = now,
                    categoryColor = "ORANGE",
                    isFixed = false,
                    startTime = "11:30",
                    endTime = "13:00"
                ),
                Task(
                    id = "t3",
                    title = "محاضرة الرياضيات (قاعة 402)",
                    subtitle = "حدث ثابت - جامعة",
                    scheduledTime = "02:00 - 03:30 م",
                    durationMinutes = 90,
                    isCompleted = false,
                    date = now,
                    categoryColor = "GREEN",
                    isFixed = true,
                    startTime = "14:00",
                    endTime = "15:30"
                ),
                Task(
                    id = "t4",
                    title = "تدريب رياضي",
                    subtitle = "النادي الصحي",
                    scheduledTime = "06:00 - 07:30 م",
                    durationMinutes = 90,
                    isCompleted = false,
                    date = now + 86400000L,
                    categoryColor = "TEAL",
                    isFixed = false
                ),
                Task(
                    id = "t5",
                    title = "مذاكرة هياكل البيانات",
                    subtitle = "حل مسائل الخوارزميات",
                    scheduledTime = "08:00 - 09:30 م",
                    durationMinutes = 90,
                    isCompleted = false,
                    date = now + 86400000L,
                    categoryColor = "ORANGE",
                    isFixed = false
                )
            )
            taskDao.insertTasks(initialTasks)

            val initialProjects = listOf(
                Project(
                    id = "p1",
                    name = "تطوير تطبيق واكا (Whacka)",
                    description = "إعادة تصميم واجهة المستخدم ونظام الإنتاجية المتكامل",
                    color = "TEAL",
                    progress = 0.75f,
                    status = "ACTIVE",
                    taskCount = 8,
                    completedTaskCount = 6,
                    createdAt = now
                ),
                Project(
                    id = "p2",
                    name = "اللياقة والرياضة البدنية",
                    description = "تمارين رياضية يومية وجدول صحي وتغذية",
                    color = "GREEN",
                    progress = 0.40f,
                    status = "ACTIVE",
                    taskCount = 5,
                    completedTaskCount = 2,
                    createdAt = now
                ),
                Project(
                    id = "p3",
                    name = "قراءة ومراجعة الكتب",
                    description = "قراءة كتب الإنتاجية والتركيز العميق وإدارة الوقت",
                    color = "ORANGE",
                    progress = 0.20f,
                    status = "ACTIVE",
                    taskCount = 4,
                    completedTaskCount = 1,
                    createdAt = now
                )
            )
            projectDao.insertProjects(initialProjects)

            val initialGoals = listOf(
                Goal(
                    id = "g1",
                    title = "إتمام 20 جلسة تركيز هذا الشهر",
                    description = "بناء عادة التركيز العميق بدون مقاطعات يومياً",
                    category = "إنتاجية",
                    targetCount = 20,
                    currentCount = 8,
                    progress = 0.40f,
                    deadline = "نهاية الشهر",
                    status = "ACTIVE",
                    color = "TEAL",
                    createdAt = now
                ),
                Goal(
                    id = "g2",
                    title = "قراءة 3 كتب في تطوير الذات",
                    description = "إتمام كتاب التركيز الفائق والعادات الذرية",
                    category = "تطوير الذات",
                    targetCount = 3,
                    currentCount = 1,
                    progress = 0.33f,
                    deadline = "بعد أسبوعين",
                    status = "ACTIVE",
                    color = "ORANGE",
                    createdAt = now
                ),
                Goal(
                    id = "g3",
                    title = "المشي 10,000 خطوة يومياً",
                    description = "المحافظة على النشاط البدني والصحة العامة",
                    category = "صحة",
                    targetCount = 30,
                    currentCount = 14,
                    progress = 0.47f,
                    deadline = "مستمر",
                    status = "ACTIVE",
                    color = "GREEN",
                    createdAt = now
                )
            )
            goalDao.insertGoals(initialGoals)
        }
    }
}
