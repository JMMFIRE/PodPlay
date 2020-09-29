package com.raywenderlich.podplay.service

import com.raywenderlich.podplay.util.DateUtils
import okhttp3.*
import org.w3c.dom.Node
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

//pg 512

class RssFeedService: FeedService {
    override fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit) {                 //Takes a URL pointing to an RSS file and a callback method. Once loaded, the callback gets loaded with the final RSS feed response

        //pg 513
        val client = OkHttpClient()                                                                 //Create a new instance of HttpClient

        val request = Request.Builder()                                                             //Create an HTTP request using the URL of the RSS file
            .url(xmlFileURL)
            .build()

        client.newCall(request).enqueue(object : Callback {                                         //Pass the request to th client
            override fun onFailure(call: Call, e: IOException) {
                callBack(null)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val dbFactory = DocumentBuilderFactory.newInstance()                        //Document Builder provides a factory used to obtain a parser for XML documents
                        val dBuilder = dbFactory.newDocumentBuilder()
                        val doc = dBuilder.parse(responseBody.byteStream())                         //Called ith the RSS file and results in a top level XML document assigned to doc
                        val rssFeedResponse = RssFeedResponse(episodes =
                        mutableListOf())
                        domToRssFeedResponse(doc, rssFeedResponse)
                        callBack(rssFeedResponse)
                        println(rssFeedResponse)
                        return
                    }
                }
                callBack(null)
            }

        })
    }

    //pg 517
    private fun domToRssFeedResponse(node: Node, rssFeedResponse: RssFeedResponse) {                //Parses top level RSS feed info. Will operate on a single node at a time
        if (node.nodeType == Node.ELEMENT_NODE) {

            val nodeName = node.nodeName
            val parentName = node.parentNode.nodeName
            val grandParentName = node.parentNode.parentNode?.nodeName ?: ""

            //pg 519
            if (parentName == "item" && grandParentName == "channel") {
                val currentItem = rssFeedResponse.episodes?.last()
                if (currentItem != null) {
                    when (nodeName) {
                        "title" -> currentItem.title = node.textContent
                        "description" -> currentItem.description = node.textContent
                        "itunes:duration" -> currentItem.duration = node.textContent
                        "guid" -> currentItem.guid = node.textContent
                        "pubDate" -> currentItem.pubDate = node.textContent
                        "link" -> currentItem.link = node.textContent
                        "enclosure" -> {
                            currentItem.url = node.attributes.getNamedItem("url").textContent
                            currentItem.type = node.attributes.getNamedItem("type").textContent
                        }
                    }
                }
            }

            //pg 517
            if (parentName == "channel") {
                when (nodeName) {
                    "title" -> rssFeedResponse.title = node.textContent
                    "description" -> rssFeedResponse.description = node.textContent
                    "itunes:summary" -> rssFeedResponse.summary = node.textContent
                    "item" -> rssFeedResponse.episodes?.add(RssFeedResponse.EpisodeResponse())
                    "pubDate" -> rssFeedResponse.lastUpdated =
                        DateUtils.xmlDateToDate(node.textContent)
                }
            }
        }

        val nodeList = node.childNodes
        for (i in 0 until nodeList.length) {
            val childNode = nodeList.item(i)
            domToRssFeedResponse(childNode, rssFeedResponse)
        }
    }
}

interface FeedService {

    fun getFeed(xmlFileURL: String, callBack: (RssFeedResponse?) -> Unit)

    companion object {
        val instance: FeedService by lazy {                                                         //Provides a singleton instance of the FeedService
            RssFeedService()
        }
    }
}



