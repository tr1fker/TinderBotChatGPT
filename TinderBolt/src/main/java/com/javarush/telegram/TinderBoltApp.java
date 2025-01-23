package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "TestJavaRushTinderAIbot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "7757859344:AAG_pnZzLIb-U0tlCvFFw3HgFXlecMBq1"; //TODO: woдобавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:y8HQgXidYWEPQ52jBuwnJFkblB3T8AScAHOTiNP46pG97Q"; //TODO: aeдобавь токен ChatGPT в кавычках

    private ChatGPTService chatGPT = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode currentDialogMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private UserInfo me;
    private UserInfo she;
    private int questionCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        String messageText = getMessageText();
        //command START
        if (messageText.equals("/start")){
            currentDialogMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String mainText = loadMessage("main");
            sendTextMessage(mainText);
            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt");
            return;
        }
        //command GPT
        if (messageText.equals("/gpt")){
            currentDialogMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String gptText = loadMessage("gpt");
            sendTextMessage(gptText);
            return;
        }
        if (currentDialogMode == DialogMode.GPT && !isMessageCommand()) {
            String promptGPT = loadMessage("gpt");
            Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
            String answer = chatGPT.sendMessage(promptGPT, messageText);
            updateTextMessage(msg, answer);
            return;
        }
        //command DATE
        if (messageText.equals("/date")){
            currentDialogMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String dateText = loadMessage("date");
            sendTextButtonsMessage(dateText,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райн Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }
        if (currentDialogMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage(" Отличный выбор! \uD83D\uDE05 \n*Вы должны пригласить девушку/парня на свидание \uFE0F за 5 сообщений.*\nПервый шаг за вами:");
                String prompt = loadPrompt(query);
                chatGPT.setPrompt(prompt);
                return;
            }
            Message msg = sendTextMessage("Подождите, девушка набирает текст...");
            String answer =  chatGPT.addMessage(messageText);
            updateTextMessage(msg, answer);
            return;
        }
        //command MESSAGE
        if (messageText.equals("/message")){
            currentDialogMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            sendTextButtonsMessage("Пришлите в чат вашу переписку",
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }
        if (currentDialogMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userCharHistory = String.join("\n\n", list);
                Message msg = sendTextMessage("Подождите пару секунд - ChatGPT думает...");
                String answer = chatGPT.sendMessage(prompt, userCharHistory);
                updateTextMessage(msg, answer);
                return;
            }
            list.add(messageText);
            return;
        }
        //command PROFILE
        if (messageText.equals("/profile")){
            currentDialogMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            me = new UserInfo();
            questionCount = 0;
            sendTextMessage("Сколько вам лет?");
            return;
        }
        if (currentDialogMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch(questionCount){
                case 1:
                    me.age = messageText;
                    questionCount = 2;
                    sendTextMessage("Кем вы работаете?");
                    return;
                case 2:
                    me.occupation = messageText;
                    questionCount = 3;
                    sendTextMessage("У вас есть хобби?");
                    return;
                case 3:
                    me.hobby = messageText;
                    questionCount = 4;
                    sendTextMessage("Что вам НЕ нравится в людях?");
                    return;
                case 4:
                    me.annoys = messageText;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    me.goals = messageText;
                    String aboutMyself = me.toString();
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutMyself);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }
        //command OPENER
        if (messageText.equals("/opener")){
            currentDialogMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            she = new UserInfo();
            questionCount = 1;
            sendTextMessage("Имя девушки?");
            return;
        }
        if (currentDialogMode == DialogMode.OPENER && !isMessageCommand()) {
            switch(questionCount){
                case 1:
                    she.name = messageText;
                    questionCount = 2;
                    sendTextMessage("Сколько ей лет?");
                    return;
                case 2:
                    she.age = messageText;
                    questionCount = 3;
                    sendTextMessage("Есть ли у нее хобби и какие?");
                    return;
                case 3:
                    she.hobby = messageText;
                    questionCount = 4;
                    sendTextMessage("Кем она работает?");
                    return;
                case 4:
                    she.occupation = messageText;
                    questionCount = 5;
                    sendTextMessage("Цель знакомства?");
                    return;
                case 5:
                    she.goals = messageText;
                    String aboutFriend = messageText;
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage("Подождите пару секунд - ChatGPT \uD83E\uDDE0думает...");
                    String answer = chatGPT.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }
        }
//        sendTextMessage("*Привет*_!_");
//        sendTextMessage("Вы ввели:" + messageText);
//        sendTextButtonsMessage("Выберите режим:",
//                "Старт", "startButton",
//                "Стоп", "stopButton");
    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
