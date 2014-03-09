package com.studioY.mms2email;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import net.htmlparser.jericho.Source;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.MessageFormat;

public class MMSMonitor {
    private static final String OUTGOING_MMS = "outgoing mms";
    private static final String INCOMING_MMS = "incoming mms";
    private Context context;
    private ContentResolver contentResolver = null;
    private Handler mmshandler = null;
    private ContentObserver mmsObserver = null;
    public boolean monitorStatus = false;
    int mmsCount = 0;
    AppPreference appPreference;

    public MMSMonitor(final Context mainContext) {
        this.context = mainContext;
        contentResolver = mainContext.getContentResolver();
        mmsObserver = new MMSObserver(mmshandler);
        appPreference = new AppPreference(mainContext);
        Log("", "MMSMonitor :: ***** Start MMS Monitor *****");
    }

    public void startMMSMonitoring() {
        try {
            monitorStatus = false;
            if (!monitorStatus) {
                contentResolver.registerContentObserver(Uri.parse("content://mms-sms"), true, mmsObserver);

                Uri uriMMSURI = Uri.parse("content://mms");
                Cursor mmsCur = context.getContentResolver().query(uriMMSURI, null, "msg_box = 4", null, "_id");
                if (mmsCur != null && mmsCur.getCount() > 0) {
                    mmsCount = mmsCur.getCount();
                    Log("", "MMSMonitor :: Init MMSCount ==" + mmsCount);
                }

                mmsCur.close();
            }
        } catch (Exception e) {
            Log("", "MMSMonitor :: startMMSMonitoring Exception== "+ e.getMessage());
        }
    }


    public void stopMMSMonitoring() {
        try {
            monitorStatus = false;
            if (!monitorStatus){
                contentResolver.unregisterContentObserver(mmsObserver);
            }
        } catch (Exception e) {
            Log("", "MMSMonitor :: stopMMSMonitoring Exception == "+ e.getMessage());
        }
    }

    class MMSObserver extends ContentObserver {

        public MMSObserver(final Handler mmshandle) {
            super(mmshandle);
        }

        public void onChange(final boolean bSelfChange) {
            super.onChange(bSelfChange);
            Log("", "MMSMonitor :: Onchange");

            Thread thread = new Thread() {
                public void run() {
                    try {
                        int currMMSCount = 0;
                        monitorStatus = true;

                        // Getting the mms count
                        Uri uriMMSURI = Uri.parse("content://mms/");
                        Cursor mmsCur = context.getContentResolver().query(uriMMSURI, null, "msg_box = 4 or msg_box = 1", null,"_id");

                        if (mmsCur != null && mmsCur.getCount() > 0) {
                            currMMSCount = mmsCur.getCount();
                        }

                        if(mmsCur == null){
                            Log("", "MMSMonitor :: mmsCur is null" );
                        }else{
                            Log("", "MMSMonitor :: mmsCur is not null, currMMSCount : " + currMMSCount + ", mmsCount : " + mmsCount );
                        }

                        if (currMMSCount >= mmsCount) {
                            String message = "";
                            String direction;
                            String bodyString = "";

                            mmsCount = currMMSCount;
                            mmsCur.moveToLast();

                            // get id , subject
                            String subject = mmsCur.getString(mmsCur.getColumnIndex("sub"));
                            int id = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("_id")));
                            Log("", "MMSMonitor :: _id  == " + id);
                            Log("", "MMSMonitor :: Subject == " + subject);

                            // GET DIRECTION
                            int type = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("m_type")));
                            if (type == 128) {
                                direction = OUTGOING_MMS;
                                Log("", "MMSMonitor :: Type == Outgoing MMS");
                            } else {
                                direction = INCOMING_MMS;
                                Log("", "MMSMonitor :: Type == Incoming MMS");
                            }

                            // Get Sender
                            String selectionAdd = new String("msg_id=" + id);
                            String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
                            Uri uriAddress = Uri.parse(uriStr);
                            Cursor cAdd = context.getContentResolver().query(uriAddress, null,
                                    selectionAdd, null, null);
                            String senderAddr = null;
                            if (cAdd.moveToFirst()) {
                                do {
                                    String number = cAdd.getString(cAdd.getColumnIndex("address"));
                                    if (number != null) {
                                        try {
                                            Long.parseLong(number.replace("-", ""));
                                            senderAddr = number;
                                        } catch (NumberFormatException nfe) {
                                            if (senderAddr == null) {
                                                senderAddr = number;
                                            }
                                        }
                                    }
                                } while (cAdd.moveToNext());
                            }
                            if (cAdd != null) {
                                cAdd.close();
                            }

                            // Get Parts
                            Uri uriMMSPart = Uri.parse("content://mms/part");
                            Cursor curPart = context.getContentResolver()
                                    .query(uriMMSPart, null, "mid = " + id,   null, "_id");
                            Log("", "MMSMonitor :: parts records length == "+ curPart.getCount());
                            curPart.moveToLast();
                            do {

                                if(direction.equals(OUTGOING_MMS)){
                                    Log("", "outgoing mms, do nothing");
                                    break;
                                }

                                //String contentType = curPart.getString(3);
                                //String partId = curPart.getString(0);
                                String contentType = curPart.getString(curPart.getColumnIndex("ct"));
                                String partId = curPart.getString(curPart.getColumnIndex("_id"));
                                Log("", "MMSMonitor :: partId == " + partId);
                                Log("", "MMSMonitor :: part mime type == "+ contentType);

                                // Get the message
                                if (contentType.equalsIgnoreCase("text/html"))
                                {
                                    Log("","MMSMonitor :: ==== Get the html text message start ====");
                                    byte[] messageData = readMMSPart(partId);
                                    if (messageData != null && messageData.length > 0)
                                        message = new String(messageData);

                                    if(message == ""){
                                        Cursor curPart1 = context.getContentResolver()
                                                .query(uriMMSPart, null, "mid = " + id +
                                                        " and _id =" + partId,null, "_id");
                                        for (int i = 0; i < curPart1.getColumnCount(); i++)
                                        {
                                            Log("","MMSMonitor :: Column Name : " +
                                                    curPart1.getColumnName(i));
                                        }
                                        curPart1.moveToLast();
                                        message = curPart1.getString(13);
                                        curPart1.close();
                                    }
                                    Log("","MMSMonitor :: Txt Message == " + message);

                                    Source source = null;
                                    String content = null;

                                    try{
                                        source = new Source(message);
                                        source.fullSequentialParse();
                                        content = source.getTextExtractor().toString();
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }

                                    message = content;
                                    Log("", "MMSMonitor :: after parse == " + message);
                                }

                                // Get the message
                                if (contentType.equalsIgnoreCase("text/plain"))
                                {
                                    Log("","MMSMonitor :: ==== Get the plain text message start ====");
                                    byte[] messageData = readMMSPart(partId);
                                    if (messageData != null && messageData.length > 0)
                                        message = new String(messageData);

                                    if(message == ""){
                                        Cursor curPart1 = context.getContentResolver()
                                                .query(uriMMSPart, null, "mid = " + id +
                                                        " and _id =" + partId,null, "_id");
                                        for (int i = 0; i < curPart1.getColumnCount(); i++)
                                        {
                                            Log("","MMSMonitor :: Column Name : " +
                                                    curPart1.getColumnName(i));
                                        }
                                        curPart1.moveToLast();
                                        message = curPart1.getString(13);
                                        curPart1.close();
                                    }
                                    Log("","MMSMonitor :: Txt Message == " + message);
                                }


                                // Get X-param
                                if (contentType.equalsIgnoreCase("text/X-param")) {
                                    Log("","MMSMonitor :: ==== Get X-param start ====");
                                    Log("","MMSMonitor :: X-param Message");
                                }

                                // Get Image
                                if (isImageType(contentType) == true) {
                                    Log("","MMSMonitor :: ==== Get the Image start ====");
                                    Bitmap bitmap = getMmsImage(partId);

                                    Log("", "MMSMonitor :: Image width : "+ bitmap.getWidth() + " height : " + bitmap.getHeight());

                                    try{
                                        String senderAddress = appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS);
                                        String senderPassword = appPreference.getValue(Commons.SENDER_EMAIL_PASSWORD);
                                        String receiverAddress = appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS);

                                        Log.d("yskang", "Try to send Email using [[" + senderAddress + "]] [[" + senderPassword + "]] to [[" + receiverAddress + "]]" );

                                        GMailSender sender = new GMailSender(senderAddress, senderPassword);

                                        saveBitmapToFileCache(bitmap, context.getCacheDir().toString() + "/temp.jpg");

                                        sender.addAttachment(context.getCacheDir().toString() + "/temp.jpg", message);
                                        sender.sendMail(senderAddr, message, senderAddress, receiverAddress);

                                    }catch (Exception e){
                                        Log("", e.getMessage());
                                    }
                                }

                            } while (curPart.moveToPrevious());

                        }

                    } catch (Exception e) {
                        Log("", "MMSMonitor Exception:: "+ e.getMessage());
                        try {
                            Thread.sleep(5000);
                            onChange(true);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            };
            thread.start();
        }
    }

    private void saveBitmapToFileCache(Bitmap bitmap, String strFilePath) {

        File fileCacheItem = new File(strFilePath);
        OutputStream out = null;

        try
        {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getMmsImage(String _id) {
        Uri partURI = Uri.parse("content://mms/part/" + _id);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = context.getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {}
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }


    private byte[] readMMSPart(String partId) {
        byte[] partData = null;
        Uri partURI = Uri.parse("content://mms/part/" + partId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = null;

        try {

            Log("","MMSMonitor :: Entered into readMMSPart try..");
            ContentResolver mContentResolver = context.getContentResolver();
            is = mContentResolver.openInputStream(partURI);

            byte[] buffer = new byte[256];
            int len = is.read(buffer);
            while (len >= 0) {
                baos.write(buffer, 0, len);
                len = is.read(buffer);
            }
            partData = baos.toByteArray();
            //Log.i("", "Text Msg  :: " + new String(partData));

        } catch (IOException e) {
            Log("", "MMSMonitor :: Exception == Failed to load part data");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log("", "Exception :: Failed to close stream");
                }
            }
        }
        return partData;
    }


    private boolean isImageType(String mime) {
        boolean result = false;
        if (mime.equalsIgnoreCase("image/jpg")
                || mime.equalsIgnoreCase("image/jpeg")
                || mime.equalsIgnoreCase("image/png")
                || mime.equalsIgnoreCase("image/gif")
                || mime.equalsIgnoreCase("image/bmp")) {
            result = true;
        }
        return result;
    }

    public void Log(String tag, String message)   {
        Log.d(tag, message);
    }

}
