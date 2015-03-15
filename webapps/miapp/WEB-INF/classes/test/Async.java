/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.select.Elements;

/**
 *
 * @author administrador
 */
public class Async extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, ParseException {
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String method = request.getMethod().toLowerCase();
        System.out.println("method = " + method);
        boolean doOutput = false;
        if ("post".equals(method) || "put".equals(method) || "delete".equals(method) || "trace".equals(method)) {
            try (ServletInputStream r = request.getInputStream()) {
                byte buff[] = new byte[300000];
                int cant;
                while ((cant = r.read(buff)) != -1) {
                    baos.write(buff, 0, cant);
                }
                baos.flush();
                baos.close();
            }
            doOutput = true;
        }
        String host = request.getParameter("host");
        Utils.getInstance().getManagers(host);
        String port = request.getParameter("port");
        String urlS = URLDecoder.decode(request.getParameter("url"), "US-ASCII");
        System.out.println("url = " + urlS);
        System.out.println("query = " + request.getQueryString());
        URL url = new URL(urlS);
        //la base del sitio pedido
        String base = url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : "");

        //la url que representa a el host donde esta montada esta app
        String thishost = "http://" + request.getLocalName() + ":" + request.getLocalPort() + "/";
        System.out.println("thishost = " + thishost);
//        System.out.println("request server name = " + request.getServerName());
//        boolean debug = false;
//        if (host != null) {
////            debug = true;
//            thishost = "http://ostest.tt:8080/";
//        } else {
//            thishost = "http://test-truetest.rhcloud.com/";
//        }

        HttpURLConnection openConnection;
        if (host == null) {
            openConnection = (HttpURLConnection) url.openConnection();
        } else {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, Integer.parseInt(port))); // or whatever your proxy is
            openConnection = (HttpURLConnection) url.openConnection(proxy);
        }
        openConnection.setConnectTimeout(0);
        openConnection.setReadTimeout(0);
        openConnection.setRequestMethod(request.getMethod());
        //por si acaso no cambia automatico cuando cambio el metodo

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                String headerValue = headers.nextElement();
                if (headerName != null && !"accept-encoding".equals(headerName.toLowerCase())) {
                    openConnection.addRequestProperty(headerName, headerValue);
                }
            }
        }
        openConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.102 Safari/537.36");
//        openConnection.connect();
        if (doOutput) {
            openConnection.setDoOutput(true);
            try (OutputStream out = openConnection.getOutputStream()) {
                out.write(baos.toByteArray());
                out.flush();
            }
        }
        boolean nis = false;
        InputStream inputStream = null;
        try {
            inputStream = openConnection.getInputStream();
        } catch (IOException e) {
            nis = true;
        }
        String contentType = openConnection.getContentType();
        System.out.println("contentType basico = " + contentType);
//        String guessContentTypeFromName = HttpURLConnection.guessContentTypeFromName(urlS);
        ContentType ct = null;
        if (contentType != null) {
            ct = new ContentType(contentType);
        }
        //usod de jsoup para modificar la pag para que todas las url se dirijan a este servidor
        Document parse = null;//
        if (ct != null && ct.match("text/html") && !nis) {
            String charset = ct.getParameter("charset");
            System.out.println("charset = " + charset);
            parse = Jsoup.parse(inputStream, charset, urlS);
            parse.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            String arrayURLAtt[] = new String[]{"src", "data", "cite"};
            for (String attr : arrayURLAtt) {
                Elements linksatt = parse.select("[" + attr + "]");
                for (Element link : linksatt) {
                    link.attr(attr, link.attr("abs:" + attr));
                }
            }
            //para mandar los post y get de los formularios a travez de este proxy html
            //FIXME poner iframe
            String attr[] = new String[]{"action", "href", "src"};
            for (String att : attr) {
                String select;
                switch (att) {
                    case "href":
                        select = "a[" + att + "],link[" + att + "]";
                        break;
                    case "src":
                        select = "iframe[" + att + "],script[" + att + "]";
                        break;
                    default:
                        select = "[" + att + "]";
                        break;
                }
                Elements links = parse.select(select);
                for (Element link : links) {
                    String abs = link.attr("abs:" + att);
                    if (host == null) {
                        String finalUrl = thishost + "?url=" + URLEncoder.encode(abs, "US-ASCII") + "&d=true";
                        link.attr(att, finalUrl);
                    } else {
                        link.attr(att, thishost + "?url=" + URLEncoder.encode(abs, "US-ASCII") + "&host=" + host + "&port=" + port);
                    }
                }
            }
        }

        Map<String, List<String>> headerFields = openConnection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String header = entry.getKey();
//            System.out.println("header = " + header);
            List<String> list = entry.getValue();
            for (String headerValue : list) {
                if (header != null) {
//                    System.out.println("header = " + header);
                    if ("location".equals(header.toLowerCase()) || "referer".equals(header.toLowerCase())) {
                        if (host == null) {
                            headerValue = "http://ostest.tt:8081/?url=" + headerValue + "&d=true";
                        } else {
                            headerValue = "http://test-truetest.rhcloud.com/?url=" + headerValue + "&host=" + host + "&port=" + port;
                        }
                    }
                    if ("host".equals(header.toLowerCase())) {
                        headerValue = url.getHost() + ":" + (url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
                    }
                    if (!"content-length".equals(header.toLowerCase())) {
                        headerValue = url.getHost() + ":" + (url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
                    }
//                    System.out.println("headerValue = " + headerValue);
                    if (!"content-length".equals(header.toLowerCase()) && !"content-encoding".equals(header.toLowerCase())) {
                        response.addHeader(header, headerValue);
                    }
                }
            }
        }

//        response.setStatus(openConnection.getResponseCode());
//        System.out.println("content = " + openConnection.getContentType());
        response.setContentType(openConnection.getContentType());
        if (parse != null) {
            try (PrintWriter writer = response.getWriter()) {
                StringBuilder builder = new StringBuilder(parse.outerHtml());
                String cabecera = "<head>";
                int indexOf = builder.indexOf(cabecera);
                if (indexOf != -1) {
                    String scripText = "<script id=\"prox\" >(function(open) {\n"
                            + "   XMLHttpRequest.prototype.open = function(method, url, async, user, pass) {\n"
                            + "     open.call(this, method, '" + thishost + "?url=" + base + "'+encodeURIComponent(url)+'" + (host == null ? "&d=true" : "&host=" + host + "&port=" + port) + "', true, user, pass);\n"
                            + "   };\n"
                            + " })(XMLHttpRequest.prototype.open);"
                            + ""
                            + "window.location.origin='" + base + "'</script>";
                    builder.insert(indexOf + cabecera.length(), scripText);
                }
                writer.print(builder);
                writer.flush();
            }
        } else {
            try (OutputStream out = response.getOutputStream(); BufferedInputStream in = new BufferedInputStream((openConnection.getInputStream()))) {
                byte buff[] = new byte[300000];
                int cant;
                while ((cant = in.read(buff)) != -1) {
                    out.write(buff, 0, cant);
                }
                out.flush();
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(Async.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (ParseException ex) {
            Logger.getLogger(Async.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
