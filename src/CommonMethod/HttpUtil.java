package CommonMethod; /**
 * Created by Administrator on 2019/10/11 0011.
 */

import sun.net.www.protocol.http.HttpURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtil {
    public static String doPost(String toUrl, String paramJson) {
        try {
            // 请求路径
            // 封装路径信息
            URL url = new URL(toUrl);
            // 建立连接
            URLConnection urlConnection = url.openConnection();
            // 连接对象类型转换
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            // 设定POST请求方法
            httpURLConnection.setRequestMethod("POST");
            // 设定请求头信息
            httpURLConnection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            // 设定请求头信息
            // 开启post请求的输出功能
            httpURLConnection.setDoOutput(true);
            // 获取输出流对象,并写数据
            httpURLConnection.getOutputStream().write(paramJson.getBytes());
            // 获取字节输入流信息
            StringBuffer stringBuffer = null;
            try (InputStream inputStream = httpURLConnection.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                // 读取数据
                String line;
                stringBuffer = new StringBuffer();
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            // 返回数据
            return stringBuffer != null ? stringBuffer.toString() : null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}