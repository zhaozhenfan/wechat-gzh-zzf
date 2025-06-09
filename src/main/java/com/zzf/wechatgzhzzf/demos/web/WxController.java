package com.zzf.wechatgzhzzf.demos.web;

import com.thoughtworks.xstream.XStream;
import com.zzf.wechatgzhzzf.demos.Service.ApiClient;
import com.zzf.wechatgzhzzf.demos.config.ButtonConfig;
import com.zzf.wechatgzhzzf.demos.message.Image;
import com.zzf.wechatgzhzzf.demos.message.PicMessage;
import com.zzf.wechatgzhzzf.demos.message.TextMessage;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class WxController {

    private static final Logger logger = LoggerFactory.getLogger(WxController.class);

    public static final ConcurrentHashMap<String, Future<String>> futureMap = new ConcurrentHashMap<>();
    public static final AtomicInteger atomicCount = new AtomicInteger();
    // GPT 任务线程池
    public static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            10, 10,
            3, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(100), (runnable) -> new Thread(runnable, atomicCount.getAndAdd(1) + "号线程"),
            new ThreadPoolExecutor.AbortPolicy()
    );

    private final ScheduledExecutorService cleaner = Executors.newScheduledThreadPool(1);

    // 初始化清理任务(每5分钟清理过期future)
    @PostConstruct
    public void init() {
        cleaner.scheduleAtFixedRate(() -> {
            futureMap.entrySet().removeIf(entry ->
                    entry.getValue().isDone() // 清理已完成任务
            );
        }, 5, 5, TimeUnit.MINUTES);
    }
    /**
     * 接口超时
     */
    private final Long outTime = 4500L;
    private final TimeUnit outTimeUnit = TimeUnit.MILLISECONDS;

    @Autowired
    private ApiClient apiClient;

    @Autowired
    private ButtonConfig buttonConfig;

    @GetMapping("/")
    public String check(String signature,
                        String timestamp,
                        String nonce,
                        String echostr) {
        // 1)将token\timestamp\nonce三个参数进行字典排序
        logger.info("signature: "+signature);
        logger.info("timestamp: "+timestamp);
        logger.info("nonce: "+nonce);
        logger.info("echostr: "+echostr);

        String token = "zzfgzh";
        List<String> list = Arrays.asList(token, timestamp, nonce);
        // 排序
        Collections.sort(list);
        // 2)将三个参数字符串拼接成一个字符串进行sha1加密
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : list) {
            stringBuilder.append(s);
        }
        // 加密
        try {
            MessageDigest instance = MessageDigest.getInstance("sha1");
            byte[] digest = instance.digest(stringBuilder.toString().getBytes());
            StringBuilder sum = new StringBuilder();
            for (byte b : digest) {
                sum.append(Integer.toHexString((b>>4)&15));
                sum.append(Integer.toHexString(b&15));
            }
            if (!StringUtils.isEmpty(signature) && signature.equals(sum.toString())){
                return echostr;
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/")
    public String receiveMessage(HttpServletRequest request) throws IOException, InterruptedException {
        ServletInputStream inputStream = request.getInputStream();
        Map<String, String> map = new HashMap<>();
        SAXReader reader = new SAXReader();
        //读取request输入流，获得document对象
        try {
            Document document = reader.read(inputStream);
            //获得root节点
            Element root = document.getRootElement();
            //获取所有子节点
            List<Element> elements = root.elements();
            for (Element element : elements) {
                map.put(element.getName(),element.getStringValue());
            }
        } catch (DocumentException e) {
            logger.error("读取request输入流，获得document对象异常",e);
        }
        logger.info("接收消息：" + map.toString());
        String msgType = map.get("MsgType");
        String msgId = map.get("MsgId");
        Future<String> future = futureMap.get(msgId);
        String res = null;

        if (future == null) {
            // 首次请求
            int i = 1;
            future = executor.submit(() -> getReplyMessage(map, msgType));
            try {
                res = future.get(outTime, outTimeUnit);
            } catch (TimeoutException e) {
                futureMap.put(msgId, future); // 存储未完成future
                Thread.sleep(1500L);
            } catch (Exception e) {
                // 错误处理
            }
        } else {
            // 重试请求
            try {
                res = future.get(outTime, outTimeUnit); // 继续等待
                // 无论成功与否都移除future
                futureMap.remove(msgId);

            } catch (TimeoutException e) {
                Thread.sleep(1500L);
            } catch (Exception e) {
                futureMap.remove(msgId);

            }
        }
        return res;
    }

    private String getReplyMessage(Map<String, String> map, String msgType) {
        String message = null;
        switch (msgType){
            case "text":
                //回复消息
                try {
                    message = getReplyTextMessage(map,apiClient.keywordSearch(map.get("Content")));
                } catch (Exception e) {
                    logger.error("getReplyTextMessage异常",e);
                }
                break;
            case "event":
                try {
                    message = handleEvent(map);
                } catch (Exception e) {
                    logger.error("handleEvent异常",e);
                }
                break;
            default:
                break;
        }
        logger.info("回复消息：" + message);
        return message;
    }

    /**
     * 处理事件推送
     * @param map
     * @return
     */
    private String handleEvent(Map<String, String> map) throws Exception {
        String event = map.get("Event");
        switch (event){
            case "CLICK":
                if ("1".equals(map.get("EventKey"))){
                    return getReplyTextMessage(map,buttonConfig.getButton1Content());
                } else if ("2".equals(map.get("EventKey"))){
                    return getReplyPicMessage(map, buttonConfig.getButton2Content());
                } else if ("3".equals(map.get("EventKey"))){
                    return getReplyTextMessage(map, buttonConfig.getButton3Content());
                }
                break;
            default:
                break;
        }
        return null;
    }


    /**
     * 获得回复的文本消息内容
     * @param map
     * @param content
     * @return
     * @throws Exception
     */
    private String getReplyTextMessage(Map<String, String> map, String content) throws Exception {
        TextMessage textMessage = new TextMessage();
        textMessage.setToUserName(map.get("FromUserName"));
        textMessage.setFromUserName(map.get("ToUserName"));
        textMessage.setContent(content);
        textMessage.setCreateTime(System.currentTimeMillis()/1000);

        //XStream将Java对象转换为xml字符串
        XStream xStream = new XStream();
        xStream.processAnnotations(TextMessage.class);
        String xml = xStream.toXML(textMessage);
        return xml;
    }

    /**
     * 获得回复的文本消息内容
     * @param map
     * @param content
     * @return
     * @throws Exception
     */
    private String getReplyPicMessage(Map<String, String> map, String content) throws Exception {
        PicMessage picMessage = new PicMessage();
        picMessage.setToUserName(map.get("FromUserName"));
        picMessage.setFromUserName(map.get("ToUserName"));
        picMessage.setCreateTime(System.currentTimeMillis()/1000);
        Image image = new Image();
        image.setMediaId(content);
        picMessage.setImage(image);

        //XStream将Java对象转换为xml字符串
        XStream xStream = new XStream();
        xStream.processAnnotations(PicMessage.class);
        String xml = xStream.toXML(picMessage);
        return xml;
    }
}
