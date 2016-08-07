package com.loacg.kayo.handlers;

import com.loacg.kayo.BotConfig;
import com.loacg.utils.DateUtil;
import com.loacg.utils.HttpClient;
import com.loacg.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.BotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Project: kayo
 * Author: Sendya <18x@loacg.com>
 * Time: 8/1/2016 5:15 PM
 */
@Component
public class DirectionsHandlers extends TelegramLongPollingBot {

    // STATUS
    private static final int WATING_ORIGIN_STATUS = 0;
    private static final int WATING_DESTINY_STATUS = 1;
    // proxy
    private static final BotOptions options;
    private static Logger logger = LoggerFactory.getLogger(DirectionsHandlers.class);
    private static SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Bind --> Chime on every hour
    private static List<String> chimeChatIds = new ArrayList<>();
    // Bind --> Hitokoto random message
    private static List<String> hitokotoChatIds = new ArrayList<>();
    // Robot start time
    private static long bootTime;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private Integer lastId = 0;

    static {
        options = new BotOptions();
        options.setProxyHost("127.0.0.1");
        options.setProxyPort(1080);

        bootTime = System.currentTimeMillis();
    }

    public DirectionsHandlers() {
        // super(options);
        System.out.println("Start bot in " + BotConfig.USERNAME_KAYO);
    }

    @Override
    public String getBotToken() {
        return BotConfig.TOKEN_KAYO;
    }

    @Override
    public String getBotUsername() {
        return BotConfig.USERNAME_KAYO;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();

        Integer time = Integer.valueOf(String.valueOf(DateUtil.getTimeStamp() - 60));

        if (message.getDate() < time) {
            logger.info("User {} call command timeout", message.getFrom().getId());
            return;
        }

        if (message != null && message.hasText()) {
            String text = message.getText();
            logger.info("User {}[{}] call command \"{}\" from \"{}\" , messageId: {}", message.getFrom().getUserName(), message.getFrom().getId(), text, message.getChatId(), message.getMessageId());

            try {
                if (text.startsWith("/start") || text.startsWith("/help")) {
                    this.handleSendHelp(message);
                } else if (text.startsWith("/ping")) {
                    handleSendPong(message);
                } else if (text.startsWith("/whoami")) {
                    handleSendWhoami(message);
                } else if (text.startsWith("/bind")) {
                    handleBindCommand(message);
                } else if (text.startsWith("/unbind")) {
                    System.out.println("Command \"unbind\" not found");
                } else if (text.startsWith("/uptime")) {
                    handleSendUptime(message);
                } else if (text.startsWith("/test")) {
                    handleSendPhoto(message);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Send message
     * default format text markdown
     *
     * @param chatId
     * @param content
     * @return
     * @throws TelegramApiException
     */
    public Message hookSendMessage(String chatId, String content) throws TelegramApiException {
        return hookSendMessage(chatId, content, 0);
    }

    public Message hookSendMessage(String chatId, String content, Integer replyMessageId) throws TelegramApiException {
        return hookSendMessage(chatId, content, replyMessageId, 1);
    }

    /**
     * @param chatId
     * @param content
     * @param replyMessageId
     * @param textFormat     0-无格式化 1-格式化MarkDown 2-格式化Html
     * @return
     * @throws TelegramApiException
     */
    public Message hookSendMessage(String chatId, String content, Integer replyMessageId, Integer textFormat) throws TelegramApiException {
        Message message = null;
        SendMessage response = new SendMessage();
        switch (textFormat) {
            case 1:
                response.enableMarkdown(true);
                break;
            case 2:
                response.enableHtml(true);
                break;
            case 0:
            default:
                break;
        }
        response.setText(content);
        response.setChatId(chatId);
        if (replyMessageId != 0) {
            response.setReplyToMessageId(replyMessageId);
        }

        logger.info("Message: {}", response.getText());

        message = sendMessage(response);
        return message;
    }

    public Message hookEditMessage(String chatId, Integer messageId, String content) throws TelegramApiException {
        return hookEditMessage(chatId, messageId, content, 1);
    }

    /**
     * 编辑已发送的消息
     *
     * @param chatId
     * @param content
     * @param textFormat
     * @return
     * @throws TelegramApiException
     */
    public Message hookEditMessage(String chatId, Integer messageId, String content, Integer textFormat) throws TelegramApiException {
        Message message = null;
        EditMessageText response = new EditMessageText();
        switch (textFormat) {
            case 1:
                response.enableMarkdown(true);
                break;
            case 2:
                response.enableHtml(true);
                break;
            case 0:
            default:
                break;
        }
        response.setText(content);
        response.setChatId(chatId);
        response.setMessageId(messageId);
        return editMessageText(response);
    }

    private void handleControlCommand(Message message) {

    }


    private void handleSendHelp(Message message) throws TelegramApiException {
        StringBuffer sb = new StringBuffer()
                .append("<code>[Command List]</code>:\n\n")
                .append("/help - Show commands\n")
                .append("/ping - Robot is online?\n")
                .append("/whoami - Show your Telegram ID\n\n")
                .append("/bind [command] - Bind delay Scheduled tasks")
                .append("/unbind [command] - Unbind delay Scheduled tasks\n")
                .append("/photo - Get random photo\n")
                .append("/uptime - Robot runtime\n")
                .append("Contact me via @Sendya\n\n")
                .append("Thanks for using Yans Bot (@" + BotConfig.USERNAME_KAYO + ")");

        hookSendMessage(message.getChatId().toString(), sb.toString(), 0, 2);
    }

    private void handleSendWhoami(Message message) throws TelegramApiException {
        StringBuffer sb = new StringBuffer();
        if (!message.isUserMessage()) {
            hookSendMessage(message.getChatId().toString(), sb.append("无法在群组或频道查看自己的 Telegram ID\n请私密 @").append(BotConfig.USERNAME_KAYO).toString() , message.getMessageId(), 0);
        } else {
            hookSendMessage(message.getChatId().toString(), String.format("您的 Telegram ID 为 `%s`", message.getFrom().getId()), message.getMessageId(), 1);
        }
    }

    private void handleSendPong(Message message) throws TelegramApiException {
        hookSendMessage(message.getChatId().toString(), "Pong", message.getMessageId());
    }

    private void handleSendUptime(Message message) throws TelegramApiException {
        hookSendMessage(message.getChatId().toString(),
                String.format(BotConfig.USERNAME_KAYO + " 已经运行了 %s", DateUtil.timeStampAutoShow(DateUtil.getTimeStamp(true) - bootTime)),
                message.getMessageId(), 0);
    }

    public void handleSendPhoto(Message message) throws TelegramApiException {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(message.getChatId().toString());
        //photo.setPhoto("AgADBQADqqcxG-ZUBw6vstLIeXj_r5XusTIABASiieMFwI6rkTUAAgI");
        // AgADBQADq6cxGyUSBw0ICEtTZSFvsP5eszIABGjCCqbNzDY6FjUAAgI
        // AgADBQADq6cxGyUSBw0ICEtTZSFvsP5eszIABCWV_bK0lpXPGDUAAgI
        // AgADBQADq6cxGyUSBw0ICEtTZSFvsP5eszIABChXXUwAAR6BtRc1AAIC
        // AgADBQADq6cxGyUSBw0ICEtTZSFvsP5eszIABEqxT9U7fDRwFTUAAgI
        photo.setPhoto("AgADBQADq6cxGyUSBw0ICEtTZSFvsP5eszIABChXXUwAAR6BtRc1AAIC");
        // photo.setNewPhoto(new File("D:\\Anime_pic\\001\\3.jpg"));
        photo.setCaption("你这个 Hentai ，/test 不是你想玩就能玩！\nSend photo tested..");
        Message message1 = sendPhoto(photo);

        System.out.println(message1);
    }

    /**
     * 绑定来自 Telegram 的定时消息命令
     *
     * @param message
     */
    private void handleBindCommand(Message message) throws TelegramApiException {
        String text = message.getText();
        String botName = "@" + BotConfig.USERNAME_KAYO;
        if (text.indexOf(botName) != -1) {
            text.replace(botName, "");
        }
        String command[] = text.split(" ");
        if (command.length == 2) {

            if ("hitokoto".equals(command[1])) {
                hitokotoChatIds.add(message.getChatId().toString());
            } else if ("chime".equals(command[1])) {
                chimeChatIds.add(message.getChatId().toString());
            }
            hookSendMessage(message.getChatId().toString(), String.format("已成功绑定 `%s` 消息通知", command[1]), message.getMessageId());
        } else {
            hookSendMessage(message.getChatId().toString(), "Unknown command!!\nExample:\n\n/bind hitokoto - 在本群绑定一言\n/bind chime - 整点报时", message.getMessageId());
        }
    }

    /**
     * 定时消息 一言（Hitokoto）
     */
    public void delaySendHitokoto() {
        logger.info("Tasks Hitokoto call , time {}", DateUtil.date2String(new Date()));

        String str = HttpClient.get("http://www.iqi7.com/hitokoto-now/api.php?encode=json&charset=utf8");
        if (StringUtils.isEmpty(str))
            return;

        Map<?, ?> map = JsonUtils.json2Map(str);
        String str2 = String.valueOf(map.get("hitokoto"));
        if (StringUtils.isEmpty(str2))
            return;
        try {
            for (String chatId : hitokotoChatIds) {
                hookSendMessage(chatId, str2);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * 定时消息 整点报时（Chime）
     */
    public void delaySendChime() {
        logger.info("Tasks Chime call , time {}", DateUtil.date2String(new Date()));
        try {
            for (String chatId : chimeChatIds) {
                hookSendMessage(chatId, "*整点报时* 现在时间：" + DateUtil.date2String(new Date(), "yyyy-MM-dd hh:mm"));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

}
