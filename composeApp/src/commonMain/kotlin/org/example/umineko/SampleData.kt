package org.example.umineko

import androidx.compose.runtime.mutableStateListOf
import kotlin.time.Clock

object SampleData {
    val activitiesList = mutableStateListOf(
        ActivityLog("任务已完成", ActivityType.TASK_COMPLETED, Clock.System.now().toEpochMilliseconds()),
        ActivityLog("抵达 WP-1", ActivityType.WAYPOINT_REACHED, Clock.System.now().toEpochMilliseconds() + 1),
        ActivityLog("获得勋章", ActivityType.MEDAL_EARNED, Clock.System.now().toEpochMilliseconds() + 2),
        ActivityLog("系统更新", ActivityType.SYSTEM_UPDATE, Clock.System.now().toEpochMilliseconds() + 3),
        ActivityLog("任务结束", ActivityType.TASK_END, Clock.System.now().toEpochMilliseconds() + 4),
        ActivityLog("我将下班", ActivityType.TASK_END, Clock.System.now().toEpochMilliseconds() + 5),
        ActivityLog("我将洗澡", ActivityType.LIFE, Clock.System.now().toEpochMilliseconds() + 6),
        ActivityLog("我将睡觉", ActivityType.LIFE, Clock.System.now().toEpochMilliseconds() + 7),
    )

    val aircraftList = listOf(
        AircraftInfo("AC-001", "歼-20",  "空中侦察", 39.9042, 116.4074, 8500,  1200, 0.82f, 0.95f, AircraftStatus.ACTIVE),
        AircraftInfo("AC-002", "运-20",  "物资运输", 31.2304, 121.4737, 6000,  780,  0.65f, 0.88f, AircraftStatus.ACTIVE),
        AircraftInfo("AC-003", "直-20",  "搜索救援", 23.1291, 113.2644, 1200,  260,  0.91f, 0.72f, AircraftStatus.STANDBY),
        AircraftInfo("AC-004", "歼-16",  "对地打击", 22.5431, 114.0579, 9000,  1400, 0.43f, 0.81f, AircraftStatus.ACTIVE),
        AircraftInfo("AC-005", "运-9",   "电子战",   30.5728, 104.0668, 7500,  620,  0.28f, 0.60f, AircraftStatus.MAINTENANCE),
        AircraftInfo("AC-006", "歼-15",  "海上巡逻", 29.8683, 121.5440, 5000,  1100, 0.75f, 0.90f, AircraftStatus.ACTIVE),
        AircraftInfo("AC-007", "直-8",   "人员投送", 25.0453, 102.7100, 2500,  280,  0.55f, 0.66f, AircraftStatus.STANDBY),
        AircraftInfo("AC-008", "轰-6K",  "远程轰炸", 36.0611, 103.8343, 10000, 900,  0.60f, 0.77f, AircraftStatus.ACTIVE),
    )
}