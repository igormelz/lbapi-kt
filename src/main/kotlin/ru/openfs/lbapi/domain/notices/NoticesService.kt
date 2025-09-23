package ru.openfs.lbapi.domain.notices

import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.DbAdapter
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetAccountNotices
import ru.openfs.lbapi.api3.GetAccountNoticesResponse
import ru.openfs.lbapi.api3.SoapAccountNotice
import ru.openfs.lbapi.api3.SoapFilter
import ru.openfs.lbapi.domain.notices.model.UserNoticeInfo

@ApplicationScoped
class NoticesService(
    private val soapAdapter: SoapAdapter,
    private val dbAdapter: DbAdapter
) {

    private val noticeTypes = mapOf(
        1L to "Предупреждение о балансе менее",
        2L to "Напоминание за сутки об окончании обещанного платежа",
        3L to "Ежемесячный отчет о состоянии лицевого счета",
        4L to "Поступление платежа",
        6L to "Получение счета по электронной почте",
        7L to "Уведомление о начале действия пользовательской блокировки",
        8L to "Уведомление об окончании действия пользовательской блокировки"
    )

    fun getAccountNotices(sessionId: String, uid: Long): List<UserNoticeInfo> {
        val notices = getMapNotices(sessionId, uid)
        return noticeTypes.map { noticeType ->
            notices[noticeType.key]?.let { selectedNotice ->
                UserNoticeInfo(
                    type = noticeType.key,
                    description = noticeType.value,
                    email = selectedNotice.isemail == 1L,
                    value = selectedNotice.value,
                )
            } ?: UserNoticeInfo(
                type = noticeType.key,
                description = noticeType.value,
                email = false,
                value = "",
            )

        }
    }

    fun setAccountNotice(
        sessionId: String,
        uid: Long,
        type: Long,
        isEmail: Boolean,
        value: String?
    ): Boolean {
        val existsNotices = getMapNotices(sessionId, uid)
        Log.info("try to modify $uid, $type, $isEmail for ${existsNotices[type]?.isemail == 1L}")
        return if (isEmail)
            dbAdapter.setAccountNotice(uid, type, 0, value ?: "1")
        else
            dbAdapter.delAccountNotice(uid, type)
    }

    fun setAccountNotices(
        sessionId: String,
        uid: Long,
        notices: List<UserNoticeInfo>
    ): Boolean {
        val existsNotices = getMapNotices(sessionId, uid)
        notices.forEach { newNotice ->
            val isExistActive = existsNotices[newNotice.type]?.isemail == 1L
            when {
                newNotice.email && !isExistActive -> {
                    Log.info("try to set $uid, ${newNotice.type}, ${newNotice.email} for ${existsNotices[newNotice.type]?.isemail == 1L}")
                    dbAdapter.setAccountNotice(uid, newNotice.type, 0, newNotice.value ?: "1")
                }

                !newNotice.email && isExistActive -> {
                    Log.info("try to del $uid, ${newNotice.type}, ${newNotice.email} for ${existsNotices[newNotice.type]?.isemail == 1L}")
                    dbAdapter.delAccountNotice(uid, newNotice.type)
                }
            }
        }
        return true
    }


    private fun getMapNotices(sessionId: String, uid: Long): Map<Long, SoapAccountNotice> {
        return soapAdapter.withSession(sessionId).request<GetAccountNoticesResponse> {
            GetAccountNotices().apply {
                this.flt = SoapFilter().apply {
                    this.userid = uid
                }
            }
        }.ret.first().notices.associateBy { it.type }
    }
}