package CommonMethod;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * @author
 * @create 2022-03-22-11:13
 */
public class CommonUtil {

    private static final Logger logger = LogManager.getLogger(CommonUtil.class);


    // jar包之外的配置文件路径
    final static String CONFIG_PATH = "cfg/config.properties";
    // jar包内的配置文件路径
    final static String CONFIG_PATH_IN = "/cfg/config.properties";

    // SDK时间解析
    public static String parseTime(int time) {
        int year = (time >> 26) + 2000;
        int month = (time >> 22) & 15;
        int day = (time >> 17) & 31;
        int hour = (time >> 12) & 31;
        int min = (time >> 6) & 63;
        int second = (time >> 0) & 63;
        String sTime = year + "-" + month + "-" + day + "-" + hour + ":" + min + ":" + second;
        //        logger.info(sTime);
        return sTime;
    }


    /**
     * 读取配置文件属性
     * 默认从jar包外读取
     * 读取失败时从jar包内读取
     * 读取失败时直接终止程序
     *
     * @param key
     * @return
     */
    public static String getProperty(String key) {
        return getProperty(false, key);
    }

    public static String getProperty(boolean inJar, String key) {
        String value = "";
        try {
            FileInputStream inputStream1 = new FileInputStream(inJar ? CONFIG_PATH_IN : CONFIG_PATH);
            // 输出路径
            Properties properties = new Properties();
            properties.load(inputStream1);
            value = properties.getProperty(key);
            // 为空时返回空字符串
            if (value == null) {
                value = "";
            }
        } catch (FileNotFoundException e) {
            logger.error("找不到配置文件");
            e.printStackTrace();
            if (inJar) {
                // 直接终止程序
                System.exit(0);
            } else {
                return getProperty(true, key);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("读取配置文件失败");
        }
        return value;
    }


    /**
     * 将Map转换为JSON字符串
     */
    public static String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        StringBuilder json = new StringBuilder();
        json.append("{");
        if (map != null && map.size() > 0) {
            for (String key : map.keySet()) {
                json.append("\"" + key + "\":\"" + map.get(key) + "\",");
            }
            json.setCharAt(json.length() - 1, '}');
        } else {
            json.append("}");
        }
        return json.toString();
    }

    /**
     * 获取当前时间 YYYY-MM-DD HH:mm:ss
     * @return
     */
    public static String getNowTime() {
        return DateToString(new Date(), "yyyy-MM-dd HH:mm:ss");
    }
    public static String DateToString(Date date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);//设置日期格式
        return df.format(date);
    }

    public static long getTimeDiff(String timeStart, String timeEnd) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        try {
            Date date = df.parse(timeStart);
            Date nowDate = df.parse(timeEnd);
            return nowDate.getTime() - date.getTime();
        } catch (Exception e) {
            logger.error("时间转换失败", e);
        }
        return 0;
    }
}
