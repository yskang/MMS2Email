package com.studioY.mms2email.test;

import com.studioY.mms2email.MailSender;

import junit.framework.TestCase;

public class SendMailTest extends TestCase {

    public void testTest(){
        assertTrue(true);
    }


    public void testSendMailToNaver(){
        MailSender mailSender = new MailSender("alskqdyrma@gmail.com", "*******");
        try {
            mailSender.sendMail("This is a test mail", "body of message", "alskqdyrma@gmail.com", "alskqdyrma@naver.com");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
