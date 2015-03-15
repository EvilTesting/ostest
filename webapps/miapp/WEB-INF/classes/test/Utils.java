/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author lu1s0
 */
public class Utils {

    Map<String, CookieManager> managers = new HashMap<>();

    private Utils() {
    }

    public void getManagers(String host) {
        CookieManager get = managers.get(host);
        if (get == null) {
            get = new CookieManager();
            managers.put(host, get);
        }
        CookieHandler.setDefault(get);
    }

    public static Utils getInstance() {
        return UtilsHolder.INSTANCE;
    }

    private static class UtilsHolder {

        private static final Utils INSTANCE = new Utils();
    }
}
