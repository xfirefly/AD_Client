package com.bluberry.config;

public class PkgConfig {
    public static final String customer = "ADX";        //普通广告机, 包月
    //public static final String customer = "TUKEDM";

    /**
     * 直播更新检查的xml
     *
     * @return
     */
    public static final String getUpdateXml() {
        if (customer.equals("AD"))
            return "http://file.fooyou.org/apk/ad/adclient.xml";
        else
            return "http://file.fooyou.org/apk/tuke/adclient.xml";
    }

}
