package com.jiaozhu.earphonereciver

import com.jiaozhu.earphonereciver.model.SharedModel.dao
import com.jiaozhu.earphonereciver.comm.PrefSupport.Companion.context
import dealString
import fi.iki.elonen.NanoHTTPD

class HttpService(port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response? {
        var msg =
            "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'><html><body><h1>插入阅读数据</h1>\n"
        var parms: Map<String, String> = HashMap()
        session.parseBody(parms)
        parms = session.parms
        println(session.parms["msg"])
        msg += "<form action='/' method='post'>\n"
        msg += "<p><textarea  type='text' name='msg' cols='150' rows='30'></textarea></p>\n"
        msg += "<input type='submit' value='提交'/></form>\n"
        if (parms["msg"] != null) {
            msg += "<p>${parms["msg"]?.substring(0, 50)}...</p>插入成功</p>"
            try {
                context.dealString(parms["msg"], dao)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return newFixedLengthResponse("$msg</body></html>\n")
    }
}