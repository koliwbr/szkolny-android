/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_EVENTS
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiEvents(override val data: DataLibrus,
                      val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiEvents"
    }

    init {
        apiGet(TAG, "HomeWorks") { json ->
            val events = json.getJsonArray("HomeWorks")

            events?.forEach { eventEl ->
                val event = eventEl.asJsonObject

                val id = event.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(event.getString("Date") ?: return@forEach)
                val topic = event.getString("Content") ?: return@forEach
                val type = event.getJsonObject("Category")?.getInt("Id") ?: -1
                val teacherId = event.getJsonObject("CreatedBy")?.getLong("Id") ?: -1
                val subjectId = event.getJsonObject("Subject")?.getLong("Id") ?: -1
                val teamId = event.getJsonObject("Class")?.getLong("Id") ?: -1

                val lessonNo = event.getInt("LessonNo")
                val lessonRange = data.lessonRanges.singleOrNull { it.lessonNumber == lessonNo }
                val startTime = lessonRange?.startTime
                        ?: Time.fromH_m(event.getString("TimeFrom") ?: return@forEach)

                val eventObject = Event(
                        profileId,
                        id,
                        eventDate,
                        startTime,
                        topic,
                        -1,
                        type,
                        false,
                        teacherId,
                        subjectId,
                        teamId
                )

                val addedDate = Date.fromIso(event.getString("AddDate") ?: return@forEach)

                data.eventList.add(eventObject)
                data.metadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_EVENT,
                                id,
                                profile?.empty ?: false,
                                profile?.empty ?: false,
                                addedDate
                        ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_EVENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}
