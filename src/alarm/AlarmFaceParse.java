package alarm;

import CommonMethod.CacheUtil;
import CommonMethod.CommonUtil;
import CommonMethod.HttpUtil;
import NetSDKDemo.HCNetSDK;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xxs
 * @date 2023年01月30日 14:16
 */
public class AlarmFaceParse {
    static final String WEB_URL = CommonUtil.getProperty("WEB_URL");
    private static final Logger logger = LogManager.getLogger(AlarmFaceParse.class);

    // 报警信息回调函数
    public static void alarmFaceInfo(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        // AlarmDataParse.alarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
        if (lCommand == HCNetSDK.COMM_ALARM_ACS) {
            HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
            strACSInfo.write();
            Pointer pACSInfo = strACSInfo.getPointer();
            pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
            strACSInfo.read();
            /**门禁事件的详细信息解析，通过主次类型的可以判断当前的具体门禁类型，例如（主类型：0X5 次类型：0x4b 表示人脸认证通过，
             主类型：0X5 次类型：0x4c 表示人脸认证失败）*/
            if (strACSInfo.dwMajor == 5) {
                if ("4c".equals(Integer.toHexString(strACSInfo.dwMinor))) {
                    logger.error("人脸识别失败");
                } else if ("4b".equals(Integer.toHexString(strACSInfo.dwMinor))) {
                    String userNo = "";
                    int dwEmployeeNo = strACSInfo.struAcsEventInfo.dwEmployeeNo;
                    logger.info("-------------人脸识别成功-Start-------------");
                    String time = strACSInfo.struTime.dwYear + "-" + strACSInfo.struTime.dwMonth + "-" + strACSInfo.struTime.dwDay + " " + strACSInfo.struTime.dwHour + ":" + strACSInfo.struTime.dwMinute + ":" + strACSInfo.struTime.dwSecond;
                    logger.info("[报警时间]：" + time);
                    if (strACSInfo.byAcsEventInfoExtend == 1) {
                        HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND strAcsInfoEx = new HCNetSDK.NET_DVR_ACS_EVENT_INFO_EXTEND();
                        strAcsInfoEx.write();
                        Pointer pAcsInfoEx = strAcsInfoEx.getPointer();
                        pAcsInfoEx.write(0, strACSInfo.pAcsEventInfoExtend.getByteArray(0, strAcsInfoEx.size()), 0, strAcsInfoEx.size());
                        strAcsInfoEx.read();
                        userNo = new String(strAcsInfoEx.byEmployeeNo).trim();
                        logger.info("[考勤状态]：" + strAcsInfoEx.byAttendanceStatus);
                        logger.info("[人员编号1]：" + userNo);
                    }
                    logger.info("[人员编号2]：" + dwEmployeeNo);
                    // 输出有效的设备信息"byMacAddr",
                    String[] keyArr = {"sSerialNumber", "sDeviceName", "sDeviceIP"};
                    Map<String, Object> map = new HashMap<>();
                    for (String key : keyArr) {
                        Object value = pAlarmer.readField(key);
                        if (value instanceof byte[]) {
                            String value1 = new String((byte[]) value).trim();
                            map.put(key, value1);
                            logger.info("[" + key + "]:" + value1);
                        } else {
                            map.put(key, value);
                            logger.info("[" + key + "]:" + value);
                        }
                    }
                    logger.info("-------------人脸识别成功-End-------------\n");

                    // 报警时间和当前时间相差30秒内不发送
                    // 计算时间差
                    long timeDiff = CommonUtil.getTimeDiff(time, CommonUtil.getNowTime());
                    if (timeDiff > 30 * 1000) {
                        logger.error("报警时间和当前时间相差30秒内不发送");
                        return;
                    }

                    // 3秒内同一设备不重复发送
                    String cacheKey = map.get("sSerialNumber") + "_";
                    if (CacheUtil.get(cacheKey) != null && !"".equals(CacheUtil.get(cacheKey))) {
                        logger.error("3秒内同一设备不重复发送");
                        return;
                    }

                    /**
                     * 封装数据，发送到消息队列
                     */
                    String doPost = null;
                    Map<String, Object> paramMap = new HashMap<>();
                    try {
                        paramMap.put("devid", map.get("sSerialNumber"));
                        paramMap.put("devname", map.get("sDeviceName"));
                        paramMap.put("devip", map.get("sDeviceIP"));
                        paramMap.put("openid", userNo);
                        paramMap.put("pattern", "人脸");
                        logger.debug("发送参数:{}\n", paramMap);
                        doPost = HttpUtil.doPost(WEB_URL, new JSONObject(paramMap).toString());
                        CacheUtil.set(cacheKey, "1", 3000);
                    } catch (Exception e) {
                        logger.error("发送失败,请求参数:{}", paramMap, e);
                    }
                    logger.info("返回结果:{}\n", doPost);
                } else {
                    logger.debug("其他次类型");
                }
            } else {
                logger.debug("其他主类型");
            }
        }
    }
}
