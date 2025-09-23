package ru.openfs.lbapi.domain.message

import jakarta.enterprise.context.ApplicationScoped
import ru.openfs.lbapi.infrastructure.adapter.SoapAdapter
import ru.openfs.lbapi.api3.GetAccountSharedPostCategories
import ru.openfs.lbapi.api3.GetAccountSharedPostCategoriesResponse
import ru.openfs.lbapi.api3.GetClientSharedPosts
import ru.openfs.lbapi.api3.GetClientSharedPostsResponse
import ru.openfs.lbapi.api3.GetSharedPostsCategories
import ru.openfs.lbapi.api3.GetSharedPostsCategoriesResponse
import ru.openfs.lbapi.api3.SoapAccountSharedPostCategories
import ru.openfs.lbapi.api3.SoapFilter
import ru.openfs.lbapi.api3.SoapOrderby
import ru.openfs.lbapi.api3.SoapSharedPost
import ru.openfs.lbapi.api3.SoapSharedPostsCategory

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

    fun getSharedPosts(sessionId: String): List<SoapSharedPost?> =
        soapAdapter.withSession(sessionId).request<GetClientSharedPostsResponse> {
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