package alarm;


import CommonMethod.CommonUtil;
import CommonMethod.osSelect;
import NetSDKDemo.HCNetSDK;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;


public class Alarm {
    // 布防模式
    static final boolean isAlarm = "BF".equals(CommonUtil.getProperty("MODE"));
    private static final Logger logger = LogManager.getLogger(Alarm.class);
    static HCNetSDK hCNetSDK = null;
    static int lUserID = -1;// 用户句柄 实现对设备登录
    static int lAlarmHandle = -1;// 报警布防句柄
    static int lAlarmHandle_V50 = -1; // v50报警布防句柄
    static int lListenHandle = -1;// 报警监听句柄
    static FMSGCallBack_V31 fMSFCallBack_V31 = null;
    static FMSGCallBack fMSFCallBack = null;

    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean createSDKInstance() {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    if (osSelect.isWindows())
                        // win系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "\\lib\\HCNetSDK.dll";
                    else if (osSelect.isLinux())
                        // Linux系统加载库路径
                        strDllPath = System.getProperty("user.dir") + "/lib/libhcnetsdk.so";
                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                } catch (Exception ex) {
                    logger.info("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * @param args
     */
    public static void main(String[] args) throws InterruptedException {

        if (hCNetSDK == null) {
            if (!createSDKInstance()) {
                logger.info("Load SDK fail");
                return;
            }
        }
        // linux系统建议调用以下接口加载组件库
        if (osSelect.isLinux()) {
            HCNetSDK.BYTE_ARRAY ptrByteArray1 = new HCNetSDK.BYTE_ARRAY(256);
            HCNetSDK.BYTE_ARRAY ptrByteArray2 = new HCNetSDK.BYTE_ARRAY(256);
            // 这里是库的绝对路径，请根据实际情况修改，注意改路径必须有访问权限
            String strPath1 = System.getProperty("user.dir") + "/lib/libcrypto.so.1.1";
            String strPath2 = System.getProperty("user.dir") + "/lib/libssl.so.1.1";

            System.arraycopy(strPath1.getBytes(), 0, ptrByteArray1.byValue, 0, strPath1.length());
            ptrByteArray1.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArray1.getPointer());

            System.arraycopy(strPath2.getBytes(), 0, ptrByteArray2.byValue, 0, strPath2.length());
            ptrByteArray2.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(4, ptrByteArray2.getPointer());

            String strPathCom = System.getProperty("user.dir") + "/lib/";
            HCNetSDK.NET_DVR_LOCAL_SDK_PATH struComPath = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
            System.arraycopy(strPathCom.getBytes(), 0, struComPath.sPath, 0, strPathCom.length());
            struComPath.write();
            hCNetSDK.NET_DVR_SetSDKInitCfg(2, struComPath.getPointer());
        }

        /**初始化*/
        hCNetSDK.NET_DVR_Init();
        /**加载日志*/
        // hCNetSDK.NET_DVR_SetLogToFile(3, "./sdklog", true);
        // 设置报警回调函数
        if (isAlarm) {
            if (fMSFCallBack_V31 == null) {
                fMSFCallBack_V31 = new FMSGCallBack_V31();
                Pointer pUser = null;
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                    logger.info("设置回调函数失败!");
                    return;
                } else {
                    logger.info("设置回调函数成功!");
                }
            }
        }
        /** 设备上传的报警信息是COMM_VCA_ALARM(0x4993)类型，
         在SDK初始化之后增加调用NET_DVR_SetSDKLocalCfg(enumType为NET_DVR_LOCAL_CFG_TYPE_GENERAL)设置通用参数NET_DVR_LOCAL_GENERAL_CFG的byAlarmJsonPictureSeparate为1，
         将Json数据和图片数据分离上传，这样设置之后，报警布防回调函数里面接收到的报警信息类型为COMM_ISAPI_ALARM(0x6009)，
         报警信息结构体为NET_DVR_ALARM_ISAPI_INFO（与设备无关，SDK封装的数据结构），更便于解析。*/

        HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG struNET_DVR_LOCAL_GENERAL_CFG = new HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG();
        struNET_DVR_LOCAL_GENERAL_CFG.byAlarmJsonPictureSeparate = 1;   // 设置JSON透传报警数据和图片分离
        struNET_DVR_LOCAL_GENERAL_CFG.write();
        Pointer pStrNET_DVR_LOCAL_GENERAL_CFG = struNET_DVR_LOCAL_GENERAL_CFG.getPointer();
        hCNetSDK.NET_DVR_SetSDKLocalCfg(17, pStrNET_DVR_LOCAL_GENERAL_CFG);

        if (isAlarm) {
            Alarm.login_V40(CommonUtil.getProperty("DEVICE_IP"), Short.parseShort(CommonUtil.getProperty("DEVICE_PORT")), CommonUtil.getProperty("DEVICE_USER"), CommonUtil.getProperty("DEVICE_PASSWORD"));  // 登录设备
            Alarm.setAlarm();// 报警布防，和报警监听二选一即可
        } else {
            Alarm.startListen(CommonUtil.getProperty("LISTEN_IP"), Short.parseShort(CommonUtil.getProperty("LISTEN_PORT")));// 报警监听，不需要登陆设备
        }
        while (true) {
            // 保持连接状态
            System.out.println("输入STOP按回车停止监听：\n");
            Scanner scanner = new Scanner(System.in);
            String str = scanner.nextLine();
            if (str.equals("STOP")) {
                break;
            }
        }
        Alarm.logout();
        // 释放SDK
        hCNetSDK.NET_DVR_Cleanup();
        return;
    }


    /**
     * 设备登录V40 与V30功能一致
     *
     * @param ip   设备IP
     * @param port SDK端口，默认设备的8000端口
     * @param user 设备用户名
     * @param psw  设备密码
     */
    public static void login_V40(String ip, short port, String user, String psw) {
        // 注册
        HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();// 设备登录信息
        HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();// 设备信息

        String m_sDeviceIP = ip;// 设备ip地址
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());

        String m_sUsername = user;// 设备用户名
        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());

        String m_sPassword = psw;// 设备密码
        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());

        m_strLoginInfo.wPort = port;
        m_strLoginInfo.bUseAsynLogin = false; // 是否异步登录：0- 否，1- 是
        m_strLoginInfo.byLoginMode = 0;  // ISAPI登录
        m_strLoginInfo.write();

        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
        if (lUserID == -1) {
            logger.info("登录失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            logger.info(ip + ":设备登录成功！");
            return;
        }
    }

    /**
     * 设备登录V30
     *
     * @param ip   设备IP
     * @param port SDK端口，默认设备的8000端口
     * @param user 设备用户名
     * @param psw  设备密码
     */
    public static void login_V30(String ip, short port, String user, String psw) {
        HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        lUserID = hCNetSDK.NET_DVR_Login_V30(ip, port, user, psw, m_strDeviceInfo);
        logger.info("UsID:" + lUserID);
        if ((lUserID == -1) || (lUserID == 0xFFFFFFFF)) {
            logger.info("登录失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            logger.info(ip + ":设备登录成功！");
            return;
        }
    }

    /**
     * 报警布防接口
     *
     * @param
     */
    public static void setAlarm() {
        if (lAlarmHandle < 0)// 尚未布防,需要布防
        {
            // 报警布防参数设置
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel = 0;  // 布防等级
            m_strAlarmInfo.byAlarmInfoType = 1;   // 智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byDeployType = 1;   // 布防类型：0-客户端布防，1-实时布防
            m_strAlarmInfo.write();
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
            logger.info("lAlarmHandle: " + lAlarmHandle);
            if (lAlarmHandle == -1) {
                logger.info("布防失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
                return;
            } else {
                logger.info("布防成功");
            }
        } else {
            logger.info("设备已经布防，请先撤防！");
        }
        return;
    }


    /**
     * 报警布防V50接口，功能和V41一致
     *
     * @param
     */
    public static void setAlarm_V50() {

        if (lAlarmHandle_V50 < 0)// 尚未布防,需要布防
        {
            // 报警布防参数设置
            HCNetSDK.NET_DVR_SETUPALARM_PARAM_V50 m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM_V50();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel = 1;  // 布防等级
            m_strAlarmInfo.byAlarmInfoType = 1;   // 智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byDeployType = 1;   // 布防类型 0：客户端布防 1：实时布防
            m_strAlarmInfo.write();
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V50(lUserID, m_strAlarmInfo, Pointer.NULL, 0);
            logger.info("lAlarmHandle: " + lAlarmHandle);
            if (lAlarmHandle == -1) {
                logger.info("布防失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
                return;
            } else {
                logger.info("布防成功");

            }

        } else {

            logger.info("设备已经布防，请先撤防！");
        }
        return;

    }


    /**
     * 开启监听
     *
     * @param ip   监听IP
     * @param port 监听端口
     */
    public static void startListen(String ip, short port) {
        if (fMSFCallBack == null) {
            fMSFCallBack = new FMSGCallBack();
        }
        lListenHandle = hCNetSDK.NET_DVR_StartListen_V30(ip, port, fMSFCallBack, null);
        if (lListenHandle == -1) {
            logger.info("监听失败" + hCNetSDK.NET_DVR_GetLastError());
            return;
        } else {
            logger.info("监听成功");
        }
    }


    /**
     * 设备撤防，设备注销
     *
     * @param
     */
    public static void logout() {

        if (lAlarmHandle > -1) {
            if (hCNetSDK.NET_DVR_CloseAlarmChan(lAlarmHandle)) {
                logger.info("撤防成功");
            }
        }
        if (lListenHandle > -1) {
            if (hCNetSDK.NET_DVR_StopListen_V30(lListenHandle)) {
                logger.info("停止监听成功");
            }
        }
        if (lUserID > -1) {
            if (hCNetSDK.NET_DVR_Logout(lUserID)) {
                logger.info("注销成功");
            }
        }


        return;
    }


}
