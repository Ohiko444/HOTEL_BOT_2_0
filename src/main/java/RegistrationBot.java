import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;

import java.util.List;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Date;

public class RegistrationBot extends TelegramLongPollingBot {
    private Connection connection;
    String id_client_save;
    String choosing_hotel_id_save;
    String choosing_room_id_save;
    String choosing_client_gmail_save;
    int code;
    boolean boolcode = false;

    String data;

    public RegistrationBot() {

        List<BotCommand> botCommandList = new ArrayList<>();
        botCommandList.add(new BotCommand("/start", "начать общение с ботом"));
        botCommandList.add(new BotCommand("/registration", "зарегистрировать данные в боте"));
        botCommandList.add(new BotCommand("/login", "вход в бота"));
        botCommandList.add(new BotCommand("/id", "узнать свой уникальный идентификатор"));
        botCommandList.add(new BotCommand("/choosing", "выбрать отель"));
        botCommandList.add(new BotCommand("/info", "выбрать отель"));

        try {
            this.execute(new SetMyCommands(botCommandList, new BotCommandScopeDefault(), null));
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Hotel", "root", "zerefo98");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = update.getMessage().getChatId();
            String text = message.getText();

            if ("/start".equals(text)) {
                sendReplyKeyboard(chatId, "Добро пожаловать! В этом телеграм-боте Вы сможете ознакомится с отелями, номерами города Казань, найти подходящий номер и забронировать его! ");
            } else if ("/registration".equals(text) || text.startsWith("Регистрация")) {
                processRegistration(chatId, text);
            } else if (text.startsWith("/login") || text.startsWith("Вход")){
                processLogin(chatId, text);
            } else if (text.startsWith("/id")){
                if (id_client_save == null){
                    sendReplyKeyboard(chatId, "Вы не вошли в систему!");
                } else {
                    sendReplyKeyboard(chatId, "Ваш уникальный ID: " + id_client_save);
                }
            } else if ("/choosing".equals(text) || "Выбрать отель".equals(text)) {
                choosing_hotel_id_save = null;
                choosingHotel(chatId);
            } else if (text.startsWith("Код подтверждения") || text.startsWith("Даты")) {
                bookingConfirmation(chatId, text);
            } else if ("/info".equals(text) || "Информация о бронировании".equals(text)){
                infoBooking(chatId, text);
            } else {
                sendReplyKeyboard(chatId, "Неизвестная команда!");
            }
        } else if (update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageID = update.getCallbackQuery().getMessage().getMessageId();
            long chatID = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.split(":")[0].equals("hotel") || callbackData.equals("cancel")){
                if (choosing_hotel_id_save == null) {
                    choosing_hotel_id_save = callbackData.split(": ")[1];
                }
                EditMessageText messageText = choosingRoom(chatID);
                messageText.setChatId(String.valueOf(chatID));
                messageText.setMessageId((int) messageID);

                try {
                    execute(messageText);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            if (callbackData.split(":")[0].equals("room")){
                choosing_room_id_save = callbackData.split(": ")[1];
                dataRoom();

                EditMessageText messageText = new EditMessageText();
                messageText.setChatId(String.valueOf(chatID));
                messageText.setText(data);
                messageText.setMessageId((int) messageID);

                messageText.setReplyMarkup(keyboard());

                try {
                    execute(messageText);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }

            if (callbackData.equals("booking")){
                Random random = new Random();

                if (id_client_save != null) {
                    System.out.println(choosing_client_gmail_save);
                    code = (random.nextInt((99999 - 10000) + 1) + 10000);
                    send(choosing_client_gmail_save, code);
                    bookingConfirmation(chatID, "");
                } else {
                    EditMessageText messageText = new EditMessageText();
                    messageText.setChatId(String.valueOf(chatID));
                    messageText.setText("Вы не вошли в систему!");
                    messageText.setMessageId((int) messageID);

                    try {
                        execute(messageText);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private InlineKeyboardMarkup keyboard(){
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Забронировать");
        button1.setCallbackData("booking");
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Назад");
        button2.setCallbackData("cancel");

        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        rowInLine.add(button1);
        rowInLine.add(button2);
        rowsInLine.add(rowInLine);

        keyboardMarkup.setKeyboard(rowsInLine);
        return keyboardMarkup;
    }

    private void sendReplyKeyboard(Long chatId, String text) {
        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(text).build();


        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        message.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add("Регистрация");
        keyboardFirstRow.add("Вход");

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add("Выбрать отель");
        keyboardSecondRow.add("Информация о бронировании");

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void processRegistration(Long chatId, String text) {
        Random random = new Random();

        if (!text.equals("Регистрация")) {
            text = text.substring(13);
        }
        String[] data = text.split(", ");

        if (data.length != 3) {
            sendMessage(chatId, "Пожалуйста, введите данные в формате: 'Регистрация: ФИО, номер телефона, Gmail'");
            return;
        }

        String fullName = data[0].trim();
        String phoneNumber = data[1].trim();
        String email = data[2].trim();
        String id_client = Integer.toString(random.nextInt((99999 - 10000) + 1) + 10000);
        
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO Client (ID_client, FIO_client, Phone_client, Gmail_client) VALUES (?, ?, ?, ?)");
            statement.setString(1, id_client);
            statement.setString(2, fullName);
            statement.setString(3, phoneNumber);
            statement.setString(4, email);

            statement.executeUpdate();

            sendMessage(chatId, "Спасибо! Ваши данные были сохранены. Для дальнейшей работы с ботом и проверки войдите в учётную запись");
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при сохранении данных. Учтите, что gmail имеет вид: X..X@gmail.com");
        }
    }

    private void processLogin(Long chatId, String text) {

        String[] data = text.split(":");
        if (data.length != 2) {
            sendMessage(chatId, "Пожалуйста, введите данные в формате: 'Вход: номер телефона'");
            return;
        }

        String phoneNumber = data[1].trim();
        
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM Client WHERE Phone_client=?");
            statement.setString(1, phoneNumber);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                sendMessage(chatId, "Вы успешно вошли в систему!");
                id_client_save = Integer.toString(resultSet.getInt("id_client"));
                choosing_client_gmail_save = resultSet.getString("Gmail_client");
            } else {
                sendMessage(chatId, "Введенные данные не найдены. Пожалуйста, проверьте правильность введенных данных.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Произошла ошибка при входе в систему.");
        }
    }

    private void choosingHotel(Long chatId) {
        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text("Выберите отель:").build();


        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT Name_hotel, ID_hotel FROM hotel");

            while (resultSet.next()) {
                String hotelName = resultSet.getString("Name_hotel");
                String hotelID = resultSet.getString("ID_hotel");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(hotelName);
                button.setCallbackData("hotel: " + hotelID);

                List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                rowInLine.add(button);
                rowsInLine.add(rowInLine);
            }

            keyboardMarkup.setKeyboard(rowsInLine);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private EditMessageText choosingRoom(long id){
        EditMessageText message = EditMessageText.builder().chatId(id).text("Выберите номер:").build();

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT Number_room, ID_room, ID_hotel FROM hotel_room");

            while (resultSet.next()) {
                System.out.println(choosing_hotel_id_save);
                if(resultSet.getString("ID_hotel").equals(choosing_hotel_id_save)) {
                    String numberRoom = resultSet.getString("Number_room");
                    String roomID = resultSet.getString("ID_room");

                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(numberRoom);
                    button.setCallbackData("room: " + roomID);

                    List<InlineKeyboardButton> rowInLine = new ArrayList<>();
                    rowInLine.add(button);
                    rowsInLine.add(rowInLine);
                }
            }

            keyboardMarkup.setKeyboard(rowsInLine);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        message.setReplyMarkup(keyboardMarkup);

        return message;
    }

    private void dataRoom() {

        String numberRoom = "";
        String priceRoom = "";
        String numberPersonRoom = "";
        String featuresRoom = "";
        String nameHotel = "";
        String adressHotel = "";

        try {
            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT ID_room, Number_room, Price_room, Number_persons_room, Features_room, ID_hotel FROM hotel_room");

            while (resultSet.next()) {
                if (resultSet.getString("ID_room").equals(choosing_room_id_save)) {
                    numberRoom = resultSet.getString("Number_room");
                    priceRoom = resultSet.getString("Price_room");
                    numberPersonRoom = resultSet.getString("Number_persons_room");
                    featuresRoom = resultSet.getString("Features_room");
                }
            }


            ResultSet resultSet2 = statement.executeQuery("SELECT ID_hotel, Name_hotel, Adress_hotel FROM hotel");

            while (resultSet2.next()) {
                if (resultSet2.getString("ID_hotel").equals(choosing_hotel_id_save)) {
                    nameHotel = resultSet2.getString("Name_hotel");
                    adressHotel = resultSet2.getString("Adress_hotel");
                }
            }

            data = "Вы просматриваете номер " + numberRoom + " в отеле '" + nameHotel + "'\n" +
                    "Адрес отеля: " + adressHotel + "\n" +
                    "Цена номера: " + priceRoom  + "\n" +
                    "Номер расчитан на " + numberPersonRoom + " человек(а)\n" +
                    "Описание номера: " + featuresRoom;

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void bookingConfirmation(Long chatId, String text) {

        Random random = new Random();
        String[] data = text.split(": ");
        Date start_data = null;
        Date end_data = null;

        if (!boolcode){
            sendMessage(chatId, "Сейчас на Вашу почту придёт код подтверждения, если Вы согласны забронировать выбранный номер, то введите код из письма!");
            boolcode = true;
            if (data.length != 2) {
                sendMessage(chatId, "Пожалуйста, введите данные в формате: 'Код подтверждения: код из письма'");
                return;
            }
            if (Integer.parseInt(data[1]) != code) {
                sendMessage(chatId, "Не верный код!");
                return;
            }
            return;
        }

        if (boolcode){
            if (data.length != 3) {
                sendMessage(chatId, "Пожалуйста, введите данные в формате: 'Даты: ГГГГ-ММ-ДД: ГГГГ-ММ-ДД' (1 дата - заселение, 2 - выселение)");
                return;
            }
            start_data = Date.valueOf(data[1]);
            end_data = Date.valueOf(data[2]);

            String id_booking = Integer.toString(random.nextInt((99999 - 10000) + 1) + 10000);

            try {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO Booking (ID_booking, Start_date_booking, End_date_booking, ID_room, ID_client) VALUES (?, ?, ?, ?, ?)");

                if (id_client_save == null || id_client_save.equals("")){
                    sendMessage(chatId, "Вы не вошли в учётную запись!");
                    return;
                }
                if (choosing_room_id_save == null || choosing_room_id_save.equals("")){
                    sendMessage(chatId, "Вы не выбрали номер!");
                    return;
                }

                statement.setString(1, id_booking);
                statement.setString(2, String.valueOf(start_data));
                statement.setString(3, String.valueOf(end_data));
                statement.setString(4, choosing_room_id_save);
                statement.setString(5, id_client_save);

                statement.executeUpdate();

                sendMessage(chatId, "Спасибо! Ваши данные были сохранены. Вы забронировали номер!");
            } catch (SQLException e) {
                e.printStackTrace();
                sendMessage(chatId, "Произошла ошибка при сохранении данных. Даты заняты!");
            }
        }
    }

    private void infoBooking (Long chatId, String text) {

        if(id_client_save != null) {

            StringBuilder result = new StringBuilder();

            try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/Hotel", "root", "zerefo98")) {
                String query = "SELECT Booking.ID_booking, Booking.Start_date_booking, Booking.End_date_booking, Hotel_room.Number_room, Hotel_room.Price_room, Hotel_room.Number_persons_room, Hotel_room.Features_room, Hotel.Name_hotel, Hotel.Adress_hotel " +
                        "FROM Booking " +
                        "JOIN Hotel_room ON Booking.ID_room = Hotel_room.ID_room " +
                        "JOIN Hotel ON Hotel_room.ID_hotel = Hotel.ID_hotel " +
                        "WHERE Booking.ID_client = ?";

                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, id_client_save);

                ResultSet resultSet = statement.executeQuery();

                int i = 1;

                while (resultSet.next()) {
                    Date startBookingDate = resultSet.getDate("Start_date_booking");
                    Date endBookingDate = resultSet.getDate("End_date_booking");
                    String numberRoom = resultSet.getString("Number_room");
                    int roomPrice = resultSet.getInt("Price_room");
                    int roomCapacity = resultSet.getInt("Number_persons_room");
                    String roomFeatures = resultSet.getString("Features_room");
                    String hotelName = resultSet.getString("Name_hotel");
                    String hotelAddress = resultSet.getString("Adress_hotel");

                    result.append("\n\nИнформация о ").append(i).append(" бронировании: \n");

                    result.append("\nНазвание отеля: ").append(hotelName);
                    result.append("\nАдрес отеля: ").append(hotelAddress);
                    result.append("\nНомер комнаты: ").append(numberRoom);
                    result.append("\nДата заезда в номер: ").append(startBookingDate);
                    result.append("\nДата выезда: ").append(endBookingDate);
                    result.append("\nЦена за 1 день: ").append(roomPrice);
                    result.append("\nКоличество людей: ").append(roomCapacity);
                    result.append("\nОписание номера: ").append(roomFeatures);

                    i++;
                }

                sendMessage(chatId, String.valueOf(result));

                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            sendMessage(chatId, "Вы не вошли в систему!");
        }
    }

    public static void send(String gmail, int code) {

        String from = "dorapuzyreva@gmail.com";
        String to = gmail;
        String host = "smtp.gmail.com";
        String smtpPort = "465";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", smtpPort);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(
                properties,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, "bdvbswkblppkpavm");
                    }
                }
        );

        session.setDebug(true);

        try{
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));

            message.setSubject("Подтверждение бронирования");

            MimeMultipart mimeMultipart = new MimeMultipart();
            MimeBodyPart text = new MimeBodyPart();

            text.setText("Ваш код подтверэждения: " + code);

            mimeMultipart.addBodyPart(text);

            message.setContent(mimeMultipart);

            Transport.send(message);
        } catch (Exception error){
            error.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder().chatId(chatId.toString()).text(text).build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "HOTEL BOT";
    }

    @Override
    public String getBotToken() {
        return "6236203145:AAG54l_I53uWNvDrenY09kMvoXyUVURrjns";
    }


    public static void main(String[] args) throws TelegramApiException {
        RegistrationBot bot = new RegistrationBot();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

        try {
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

