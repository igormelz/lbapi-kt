package ru.openfs.lbapi.domain.message

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*
import ru.openfs.lbapi.domain.message.model.SharedPost
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter

@ApplicationScoped
class MessageService(
    private val soapAdapter: SoapAdapter,
) {

    fun getSharedPostsCategories(sessionId: String): List<SoapSharedPostsCategory?> =
        soapAdapter.withSession(sessionId).request<GetSharedPostsCategoriesResponse> {
            GetSharedPostsCategories()
        }.ret

    fun getAccountSharedPostsCategories(sessionId: String): List<SoapAccountSharedPostCategories?> =
        soapAdapter.withSession(sessionId).request<GetAccountSharedPostCategoriesResponse> {
            GetAccountSharedPostCategories()
        }.ret

    fun getSharedPosts(sessionId: String): List<SharedPost> =
        soapAdapter.withSession(sessionId).request<GetClientSharedPostsResponse> {
            GetClientSharedPosts().apply {
                this.flt = SoapFilter().apply {
                    this.status = "1"
                }
                this.ord.add(
                    SoapOrderby().apply {
                        this.name = "is_auto"
                        this.ascdesc = 1L
                    })
                this.ord.add(
                    SoapOrderby().apply {
                        this.name = "posttime"
                        this.ascdesc = 1L
                    }
                )
            }
        }.ret.map {
            SharedPost(
                postedAt = it.posttime,
                subject = it.subject,
                text = it.text,
            )
        }

}