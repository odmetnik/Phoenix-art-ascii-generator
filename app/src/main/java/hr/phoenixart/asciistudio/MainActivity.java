package hr.phoenixart.asciistudio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.*;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.*;
import java.util.Collections;

public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final int PICK = 101, SAVE = 102;
    private WebView web;
    private FrameLayout root;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pending;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11,12,16));
        getWindow().setNavigationBarColor(Color.rgb(11,12,16));
        root = new FrameLayout(this);
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(11,12,16));
        root.addView(web, new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new ExportBridge(), "AndroidExport");
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return !HOST.equals(req.getUrl().getHost()) || !"https".equals(req.getUrl().getScheme());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("https".equals(u.getScheme()) && HOST.equals(u.getHost()) && "/index.html".equals(u.getPath())) {
                    try { return new WebResourceResponse("text/html", "UTF-8", getAssets().open("index.html")); }
                    catch(IOException ignored) {}
                }
                if ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) {
                    return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                String[] types = params.getAcceptTypes();
                if(types != null && types.length > 0) {
                    java.util.ArrayList<String> m = new java.util.ArrayList<>();
                    for(String t : types) for(String part : t.split(",")) if(part.trim().contains("/")) m.add(part.trim());
                    if(!m.isEmpty()) i.putExtra(Intent.EXTRA_MIME_TYPES, m.toArray(new String[0]));
                }
                try { startActivityForResult(i, PICK); } catch(Exception e) { fileCallback.onReceiveValue(null); fileCallback=null; }
                return true;
            }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                if(fullscreen != null) { cb.onCustomViewHidden(); return; }
                fullscreen=v; fullCallback=cb; web.setVisibility(View.GONE);
                root.addView(v,new FrameLayout.LayoutParams(-1,-1));
                root.setSystemUiVisibility(5894);
            }
            @Override public void onHideCustomView(){ hideFull(); }
        });
        web.loadUrl("https://"+HOST+"/index.html");
    }
    private void hideFull(){ if(fullscreen==null)return; root.removeView(fullscreen); fullscreen=null; web.setVisibility(View.VISIBLE); root.setSystemUiVisibility(0); if(fullCallback!=null)fullCallback.onCustomViewHidden(); fullCallback=null; }
    public class ExportBridge {
        @JavascriptInterface public void save(String filename,String mime,String data){
            if(data==null || data.length()>100_000_000) { runOnUiThread(()->Toast.makeText(MainActivity.this,"Prevelika datoteka",Toast.LENGTH_LONG).show()); return; }
            final byte[] bytes;
            try { bytes=Base64.decode(data,Base64.DEFAULT); } catch(Exception e){return;}
            runOnUiThread(()->{
                if(pending!=null){Toast.makeText(MainActivity.this,"Izvoz je već u tijeku",Toast.LENGTH_SHORT).show();return;}
                pending=bytes;
                String safeName=filename==null?"phoenix-export":filename.replaceAll("[^a-zA-Z0-9._-]","_");
                String type=mime==null?"application/octet-stream":mime.split(";")[0];
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type).putExtra(Intent.EXTRA_TITLE,safeName);
                try {startActivityForResult(i,SAVE);} catch(Exception e){pending=null;Toast.makeText(MainActivity.this,"Spremanje nije dostupno",Toast.LENGTH_LONG).show();}
            });
        }
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==PICK && fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res,data));fileCallback=null;}
        if(req==SAVE){byte[] b=pending;pending=null;if(res==RESULT_OK&&data!=null&&data.getData()!=null&&b!=null){Uri uri=data.getData();new Thread(()->{String msg;try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("No stream");out.write(b);msg="Datoteka spremljena";}catch(Exception e){msg="Greška pri spremanju";}final String m=msg;runOnUiThread(()->Toast.makeText(this,m,Toast.LENGTH_LONG).show());}).start();}}
    }
    @Override public void onBackPressed(){if(fullscreen!=null)hideFull();else if(web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);web.removeJavascriptInterface("AndroidExport");root.removeView(web);web.destroy();super.onDestroy();}
}
