package com.studioY.mms2email;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import net.htmlparser.jericho.Source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;

public class MMSMonitor {
    private static final String OUTGOING_MMS = "outgoing mms";
    private static final String INCOMING_MMS = "incoming mms";
    private Context context;
    private ContentResolver contentResolver = null;
    private Handler mmshandler = null;
    private ContentObserver mmsObserver = null;
    private Bitmap bitmap;
    private ArrayList<Bitmap> bitmapArrayList = new ArrayList<Bitmap>();
    public boolean monitorStatus = false;
    private int lastId = 0;
    int mmsCount = 0;
    AppPreference appPreference;
    private int lastSendId = 0;

    public MMSMonitor(final Context mainContext) {
        this.context = mainContext;
        contentResolver = mainContext.getContentResolver();
        mmsObserver = new MMSObserver(mmshandler);
        appPreference = new AppPreference(mainContext);
        Log("", "MMSMonitor :: ***** Start MMS Monitor *****");
    }

    public void startMMSMonitoring() {
        try {
            Log.d("yskang", "---startMMSMonitoring()---");
            monitorStatus = false;
            if (!monitorStatus) {
                contentResolver.registerContentObserver(Uri.parse("content://mms-sms"), true, mmsObserver);

                Uri uriMMSURI = Uri.parse("content://mms");
                Cursor mmsCur = context.getContentResolver().query(uriMMSURI, null, "msg_box = 4 or msg_box = 1", null, "_id");

                Log.d("yskang", "cursor : " + mmsCur + ", count : " + mmsCur.getCount());

                if (mmsCur != null && mmsCur.getCount() > 0) {
                    mmsCount = mmsCur.getCount();
                    Log("", "MMSMonitor :: Init MMSCount ==" + mmsCount);
                    mmsCur.moveToLast();
                    lastSendId = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("_id")));
                    Log("", "init lastSendId : " + lastSendId);
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
            Log("", "MMSMonitor :: Onchange, bSelfChange = " + bSelfChange);

            int tryCount = 0;
            int currMMSCount = 0;
            monitorStatus = true;

            // Getting the mms count
            Uri uriMMSURI = Uri.parse("content://mms/");
            Cursor mmsCursor = context.getContentResolver().query(uriMMSURI, null, "msg_box = 4 or msg_box = 1", null,"_id");

            if (mmsCursor != null && mmsCursor.getCount() > 0) {
                currMMSCount = mmsCursor.getCount();
            }

            Log.d("yskang", "currMMSCount: " + currMMSCount + ", mmsCount: " + mmsCount);

            String message = "";
            String direction;

            if(mmsCount > currMMSCount){
                Log.d("yskang", "some MMS has been deleted!!, Update mmsCount to current count");
                mmsCount = currMMSCount;
                return;
            }

            mmsCount = currMMSCount;
            mmsCursor.moveToLast();

            // get id , subject
            String subject = mmsCursor.getString(mmsCursor.getColumnIndex("sub"));
            int id = Integer.parseInt(mmsCursor.getString(mmsCursor.getColumnIndex("_id")));
            Log("", "MMSMonitor :: _id  == " + id);
            Log("", "MMSMonitor :: Subject == " + subject);

            direction = getDirection(mmsCursor);
            String senderPhoneNumber = getSenderNumber(id);

            // Get Parts
            Uri uriMMSPart = Uri.parse("content://mms/part");
            Cursor currentPartCursor = context.getContentResolver()
                    .query(uriMMSPart, null, "mid = " + id, null, "_id");
            Log("", "MMSMonitor :: parts records length == " + currentPartCursor.getCount());

            if(senderPhoneNumber == null){
                try {
                    Thread.sleep(1000);
                    Log.d("yskang", "no sender number, Call OnChange()");
                    onChange(true);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            int count = 0;
            if (lastSendId < id) {
                Log.d("yskang", String.format("lastSendId: %d, id: %d", lastSendId, id));
                currentPartCursor.moveToLast();
                do {
                    if(direction.equals(OUTGOING_MMS)){
                        Log("", "outgoing mms, do nothing");
                        break;
                    }
                    String contentType = currentPartCursor.getString(currentPartCursor.getColumnIndex("ct"));
                    String partId = currentPartCursor.getString(currentPartCursor.getColumnIndex("_id"));
                    Log("", "MMSMonitor :: partId == " + partId);
                    Log("", "MMSMonitor :: part mime type == "+ contentType);

                    if (contentType.equalsIgnoreCase("text/html")){
                        message = getHTMLText(message, id, uriMMSPart, partId);
                    }else if(contentType.equalsIgnoreCase("text/plain")){
                        message = getPlainText(message, id, uriMMSPart, partId);
                    }
                    getXParam(contentType);
                    getImage(contentType, partId);
                } while (currentPartCursor.moveToPrevious());
                sendEmail(message, senderPhoneNumber);
                if(!bitmapArrayList.isEmpty()){
                    bitmapArrayList.clear();
                }
                id--;
                count++;
                mmsCursor.moveToPrevious();
            }
            lastSendId = lastSendId + count;
            Log.d("yskang", String.format("lastSendId: %d", lastSendId));
            currentPartCursor.close();
            mmsCursor.close();

        }

        private void sendEmail(String message, String senderPhoneNumber){
                try{
                    String senderAddress = appPreference.getValue(Commons.SENDER_EMAIL_ADDRESS);
                    String senderPassword = appPreference.getValue(Commons.SENDER_EMAIL_PASSWORD);
                    String receiverAddress = appPreference.getValue(Commons.RECEIVER_EMAIL_ADDRESS);

                    Log.d("yskang", "Try to send Email using [[" + senderAddress + "]] [[" + senderPassword + "]] to [[" + receiverAddress + "]]");

                    MailSender sender = new MailSender(senderAddress, senderPassword);

                    if(bitmapArrayList.size() != 0){
                        for(int i = 0 ; i < bitmapArrayList.size() ; i++){
                            saveBitmapToFileCache(bitmapArrayList.get(i), context.getCacheDir().toString() + String.format("/temp_%d.jpg", i));
                            sender.addAttachment(context.getCacheDir().toString() + String.format("/temp_%d.jpg", i), message);
                        }
                    }

                    if(senderPhoneNumber != null)
                        sender.sendMail(senderPhoneNumber, message, senderAddress, receiverAddress);

                }catch (Exception e){
                    Log("", e.getMessage());
                }
        }

        private void getImage(String contentType, String partId) {
            if (isImageType(contentType) == true) {
                Log("","MMSMonitor :: ==== Get the Image start ====");
                bitmap = getMmsImage(partId);
                bitmapArrayList.add(bitmap);
                Log("", "MMSMonitor :: Image width : "+ bitmap.getWidth() + " height : " + bitmap.getHeight());
            }
        }

        private void getXParam(String contentType) {
            // Get X-param
            if (contentType.equalsIgnoreCase("text/X-param")) {
                Log("","MMSMonitor :: ==== Get X-param start ====");
                Log("","MMSMonitor :: X-param Message");
            }
        }

        private String getPlainText(String message, int id, Uri uriMMSPart, String partId) {
            Log("","MMSMonitor :: ==== Get the plain text message start ====");
            byte[] messageData = readMMSPart(partId);
            if (messageData != null && messageData.length > 0)
                message = new String(messageData);

            if(message == ""){
                Cursor curPart1 = context.getContentResolver()
                        .query(uriMMSPart, null, "mid = " + id +
                                " and _id =" + partId, null, "_id");
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
            return message;
        }

        private String getHTMLText(String message, int id, Uri uriMMSPart, String partId) {
            Log("","MMSMonitor :: ==== Get the html text message start ====");
            byte[] messageData = readMMSPart(partId);
            if (messageData != null && messageData.length > 0)
                message = new String(messageData);

            if(message == "") {
                Cursor curPart1 = context.getContentResolver()
                        .query(uriMMSPart, null, "mid = " + id +
                                " and _id =" + partId, null, "_id");
                for (int i = 0; i < curPart1.getColumnCount(); i++) {
                    Log("", "MMSMonitor :: Column Name : " +
                            curPart1.getColumnName(i));
                }
                curPart1.moveToLast();
                message = curPart1.getString(13);
                curPart1.close();
            }
            Log("","MMSMonitor :: Txt Message == " + message);

            Source source;
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

            return message;
        }

        private String getSenderNumber(int id) {
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
            return senderAddr;
        }

        private String getDirection(Cursor mmsCur) {
            String direction;// GET DIRECTION
            int type = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("m_type")));
            if (type == 128) {
                direction = OUTGOING_MMS;
                Log("", "MMSMonitor :: Type == Outgoing MMS");
            } else {
                direction = INCOMING_MMS;
                Log("", "MMSMonitor :: Type == Incoming MMS");
            }
            return direction;
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
