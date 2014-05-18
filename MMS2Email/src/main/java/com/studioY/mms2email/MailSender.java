package com.studioY.mms2email;


import android.util.Log;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;

public class MailSender {
    private String user;
    private String password;
    private Session session;
    private Multipart multipart;

    MyAuthenticator authenticator;

    static {
        Security.addProvider(new com.studioY.mms2email.JSSEProvider());
    }

    public MailSender(String user, String password) {
        Log.d("MailSender", "MailSender is created, user : "+ user + ", password : " + password);

        this.user = user;
        this.password = password;

        this.authenticator = new MyAuthenticator(user, password);

        String mailHost = String.format("smtp.%s", user.split("@")[1]);
        Log.d("MailSender", "mailHost : " + mailHost);

        Log.d("MailSender", "Naver");
        Properties props = new Properties();
        if(mailHost.equals("smtp.naver.com")){
            props.put("mail.smtp.user", user);
            props.put("mail.smtp.host", "smtp.naver.com");
            props.put("mail.smtp.port", "465");
//            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.debug", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");

            session = Session.getDefaultInstance(props, authenticator);
        }else{
            props.setProperty("mail.transport.protocol", "smtp");
            props.setProperty("mail.host", mailHost);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.setProperty("mail.smtp.quitwait", "false");
            session = Session.getDefaultInstance(props, authenticator);
        }


        multipart = new MimeMultipart();
    }

    class MyAuthenticator extends Authenticator {
        private String id;
        private String pw;

        public MyAuthenticator(String id, String pw) {
            this.id = id;
            this.pw = pw;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(id, pw);
        }

    }

    public synchronized void sendMail(String subject, String body, String sender, String recipients) throws Exception {
        Log.d("MailSender", "SendMail requested, Subject : " + subject + ", body : " + body + ", sender : " + sender + ", recipients : " + recipients);
        try{
            MimeMessage message = new MimeMessage(session);
            DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));

            message.setSender(new InternetAddress(sender));
            message.setSubject(subject);
            message.setDataHandler(handler);

            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));

            if(multipart.getCount() > 0){
                message.setContent(multipart);
            }

            Transport.send(message);

        }catch(Exception e){

        }
    }

    public class ByteArrayDataSource implements DataSource {
        private byte[] data;
        private String type;

        public ByteArrayDataSource(byte[] data, String type) {
            super();
            this.data = data;
            this.type = type;
        }

        public ByteArrayDataSource(byte[] data) {
            super();
            this.data = data;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContentType() {
            if (type == null)
                return "application/octet-stream";
            else
                return type;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data);
        }

        public String getName() {
            return "ByteArrayDataSource";
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Not Supported");
        }
    }

    public void addAttachment(String filename,String subject) throws Exception {
        BodyPart messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);

        multipart.addBodyPart(messageBodyPart);

        BodyPart messageBodyPart2 = new MimeBodyPart();
        messageBodyPart2.setText(subject);

        multipart.addBodyPart(messageBodyPart2);
    }

}
