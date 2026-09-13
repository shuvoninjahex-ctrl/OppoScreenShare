package com.opposcreenshare;

import android.app.*;
import android.os.*;
import android.content.*;
import android.media.*;
import android.media.projection.*;
import android.graphics.*;
import android.hardware.display.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int REQ_CAPTURE=42;
    TextView status;
    Button start;
    String host=""; int port=8765;
    MediaProjection projection; VirtualDisplay display; ImageReader reader;
    ExecutorService exec=Executors.newSingleThreadExecutor();
    volatile boolean streaming=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,48,32,32);
        TextView title=new TextView(this); title.setText("OPPO Screen Share"); title.setTextSize(26);
        status=new TextView(this); status.setText("\nScan the PC QR code in this app, or open a connection link.\n"); status.setTextSize(16);
        start=new Button(this); start.setText("Start Screen Share"); start.setEnabled(false);
        box.addView(title); box.addView(status); box.addView(start); setContentView(box);
        handle(getIntent());
        start.setOnClickListener(v -> requestCapture());
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i); setIntent(i); handle(i);}
    void handle(Intent i){
        Uri u=i.getData(); if(u!=null && "opposhare".equals(u.getScheme())){
            host=u.getQueryParameter("host"); String p=u.getQueryParameter("port");
            try{port=Integer.parseInt(p);}catch(Exception e){}
            if(host!=null){ status.setText("\nPC: "+host+":"+port+"\nPress Start Screen Share."); start.setEnabled(true);}
        }
    }
    void requestCapture(){
        MediaProjectionManagerDummy.request(this);
    }

    static class MediaProjectionManagerDummy {
        static void request(MainActivity a){
            MediaProjectionManager m=(MediaProjectionManager)a.getSystemService(MEDIA_PROJECTION_SERVICE);
            a.startActivityForResult(m.createScreenCaptureIntent(),REQ_CAPTURE);
        }
    }
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(r==REQ_CAPTURE && c==RESULT_OK && d!=null){
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=m.getMediaProjection(c,d); startStream();
        }
    }
    void startStream(){
        if(streaming)return; streaming=true; status.setText("\nStreaming to "+host+":"+port+" …");
        DisplayMetrics dm=getResources().getDisplayMetrics(); int w=Math.min(dm.widthPixels,720), h=(int)(w*(dm.heightPixels/(float)dm.widthPixels));
        reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
        display=projection.createVirtualDisplay("OppoScreenShare",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,null);
        exec.submit(() -> sendFrames(w,h));
    }
    void sendFrames(int w,int h){
        try{
            URL url=new URL("http://"+host+":"+port+"/upload");
            HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setDoOutput(true); c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type","multipart/x-mixed-replace; boundary=frame"); c.setChunkedStreamingMode(0);
            OutputStream out=new BufferedOutputStream(c.getOutputStream());
            while(streaming){
                Image im=reader.acquireLatestImage();
                if(im==null){Thread.sleep(30); continue;}
                try{
                    Image.Plane pl=im.getPlanes()[0]; ByteBuffer buf=pl.getBuffer(); int ps=pl.getPixelStride(), rs=pl.getRowStride();
                    int rowPad=rs-ps*w; Bitmap bmp=Bitmap.createBitmap(w+rowPad/ps,h,Bitmap.Config.ARGB_8888);
                    buf.rewind(); bmp.copyPixelsFromBuffer(buf);
                    Bitmap cropped=Bitmap.createBitmap(bmp,0,0,w,h); bmp.recycle();
                    ByteArrayOutputStream jpg=new ByteArrayOutputStream(); cropped.compress(Bitmap.CompressFormat.JPEG,55,jpg); cropped.recycle();
                    byte[] data=jpg.toByteArray();
                    out.write(("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: "+data.length+"\r\n\r\n").getBytes("UTF-8"));
                    out.write(data); out.write("\r\n".getBytes("UTF-8")); out.flush();
                }finally{im.close();}
            }
            out.close(); c.disconnect();
        }catch(Exception e){runOnUiThread(()->status.setText("\nStream stopped: "+e.getMessage()));}
    }
    @Override protected void onDestroy(){
        streaming=false; try{exec.shutdownNow();}catch(Exception e){}
        if(display!=null)display.release(); if(reader!=null)reader.close(); if(projection!=null)projection.stop();
        super.onDestroy();
    }
}
