package ru.openfs.lbapi.service

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.api3.*

@ApplicationScoped
class MessageService(
    private val clientService: SoapClientService,
) {

    fun getAccountNotices(sessionId: String): List<SoapAccountNotices?> =
        clientService.withSession(sessionId).request<GetAccountNoticesResponse> {
            GetAccountNotices()
        }.ret

    fun getSharedPostsCategories(sessionId: String): List<SoapSharedPostsCategory?> =
        clientService.withSession(sessionId).request<GetSharedPostsCategoriesResponse> {
            GetSharedPostsCategories()
        }.ret

    fun getAccountSharedPostsCategories(sessionId: String): List<SoapAccountSharedPostCategories?> =
        clientService.withSession(sessionId).request<GetAccountSharedPostCategoriesResponse> {
            GetAccountSharedPostCategories()
        }.ret

    fun getSharedPosts(sessionId: String): List<SoapSharedPost?> =
        clientService.withSession(sessionId).request<GetClientSharedPostsResponse> {
            GetClientSharedPosts().apply {
                this.flt = SoapFilter().apply {
                    //this.pgnum = 1
                    //this.pgsize = 1
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
        }.ret

}