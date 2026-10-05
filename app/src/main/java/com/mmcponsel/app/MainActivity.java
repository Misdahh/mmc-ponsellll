package com.mmcponsel.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.exceptions.GetCredentialException;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    final int BG=Color.rgb(5,12,18), CARD=Color.rgb(11,21,31), WHITE=Color.WHITE,
            MUTED=Color.rgb(190,200,210), YELLOW=Color.rgb(255,212,0),
            GREEN=Color.rgb(21,171,111), RED=Color.rgb(245,65,75);

    android.widget.FrameLayout root;
    LinearLayout mainColumn, content, productList;
    TextView title;
    SharedPreferences sp;
    ArrayList<String[]> products=new ArrayList<>();
    FirebaseAuth auth;
    FirebaseFirestore db;
    boolean adminMode=false, memberMode=false;
    CredentialManager credentialManager;
    CallbackManager callbackManager;
    final ExecutorService authExecutor = Executors.newSingleThreadExecutor();
    MediaPlayer ambientPlayer;
    ImageView profileAvatar;
    static final int REQUEST_PROFILE_PHOTO=1818;
    static final int REQUEST_PRODUCT_IMAGE=1819;
    static final int REQUEST_PRODUCT_VIDEO=1820;
    static final int REQUEST_SERVICE_IMAGE=1821;
    static final int REQUEST_SERVICE_VIDEO=1822;
    Uri pendingProductImage, pendingProductVideo, pendingServiceImage, pendingServiceVideo;
    float gestureDownX, gestureDownY;
    long gestureDownAt;
    String currentRoute="login";
    static final String ADMIN_USERNAME="admin1";
    static final String ADMIN_PASSWORD="miss11";
    static final String ADMIN_EMAIL="admin1@mmcponsel.app";
    AlertDialog offlineDialog;

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(WHITE);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
    /** Visual feedback for every tappable menu/button: scale + alpha while pressed. */
    void addClickFeedback(View v){
        if(v==null) return;
        v.setClickable(true);
        v.setOnTouchListener((view,event)->{
            switch(event.getActionMasked()){
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.78f).setDuration(90).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(140).setInterpolator(new AccelerateDecelerateInterpolator()).start();
                    view.performClick();
                    break;
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120).start();
                    break;
            }
            return true;
        });
    }
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_yellow);addClickFeedback(b);return b;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("mmc",0);
        seed();
        callbackManager=CallbackManager.Factory.create();
        if(isOnline()) showSplash(); else showOfflineScreen();
    }

    boolean isOnline(){
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if(cm==null) return false;
        NetworkInfo ni=cm.getActiveNetworkInfo();
        return ni!=null && ni.isConnected();
    }

    void showOfflineScreen(){
        currentRoute="offline";
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24),dp(24),dp(24),dp(24));
        box.setBackgroundColor(BG);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.mmc_logo_4k);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(logo,new LinearLayout.LayoutParams(-1,dp(190)));
        TextView title=tv("MMC PONSEL",30); title.setGravity(Gravity.CENTER); title.setTypeface(null,Typeface.BOLD); title.setTextColor(YELLOW); box.addView(title);
        TextView msg=tv("Koneksi internet diperlukan.\nMMC PONSEL hanya dapat digunakan secara online.",16); msg.setGravity(Gravity.CENTER); msg.setTextColor(MUTED); box.addView(msg);
        Button retry=btn("↻ Coba Lagi"); box.addView(retry,new LinearLayout.LayoutParams(-1,dp(52)));
        retry.setOnClickListener(v->{ if(isOnline()) showSplash(); else Toast.makeText(this,"Internet masih belum tersambung.",Toast.LENGTH_SHORT).show(); });
        setContentView(box);
    }

    @Override protected void onResume(){
        super.onResume();
        if(currentRoute!=null && !"offline".equals(currentRoute) && !isOnline()) showOfflineScreen();
    }

    void seed(){ /* Katalog tidak lagi ditanam di APK; semua barang berasal dari Firestore. */ }

    void showLoadingScreen(String message, final Runnable next){
        final LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24),dp(24),dp(24),dp(24));
        box.setBackgroundColor(BG);

        ImageView hacker=new ImageView(this);
        hacker.setImageResource(R.drawable.hacker_computer);
        hacker.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        box.addView(hacker,new LinearLayout.LayoutParams(-1,dp(230)));

        TextView brand=tv("MMC PONSEL",28);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setTextColor(YELLOW);
        box.addView(brand,new LinearLayout.LayoutParams(-1,dp(50)));

        TextView msg=tv(message,15);
        msg.setGravity(Gravity.CENTER);
        msg.setTextColor(MUTED);
        box.addView(msg,new LinearLayout.LayoutParams(-1,dp(38)));

        TextView percent=tv("0%",18);
        percent.setGravity(Gravity.CENTER);
        percent.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(percent,new LinearLayout.LayoutParams(-1,dp(42)));

        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(8));
        pp.leftMargin=dp(32); pp.rightMargin=dp(32);
        box.addView(progress,pp);
        setContentView(box);

        final android.os.Handler h=new android.os.Handler();
        final int[] value={0};
        Runnable tick=new Runnable(){
            @Override public void run(){
                value[0]+=2;
                if(value[0]>100)value[0]=100;
                progress.setProgress(value[0]);
                percent.setText(value[0]+"%");
                if(value[0]<100) h.postDelayed(this,28);
                else h.postDelayed(next,220);
            }
        };
        h.post(tick);
    }

    void showLoginSuccessLoading(final Runnable next){
        showLoadingScreen("Login berhasil • menyiapkan aplikasi...",next);
    }

    void showSplash(){
        showLoadingScreen("Memuat MMC PONSEL...", () -> {
            try {
                auth=FirebaseAuth.getInstance();
                db=FirebaseFirestore.getInstance();
                credentialManager=CredentialManager.create(this);
                checkForAppUpdate();
                if(auth.getCurrentUser()!=null) home(); else showLogin();
            } catch(Exception e) {
                showLogin();
            }
        });
    }

    /** Online version check. Metadata is stored in Firestore appConfig/appVersion. */
    void checkForAppUpdate(){
        if(db==null) db=FirebaseFirestore.getInstance();
        final int currentCode=BuildConfig.VERSION_CODE;
        final String currentName=BuildConfig.VERSION_NAME;
        db.collection("appConfig").document("appVersion").get()
            .addOnSuccessListener(d -> {
                if(!d.exists()){
                    Toast.makeText(this,"Versi aplikasi saat ini: "+currentName,Toast.LENGTH_SHORT).show();
                    return;
                }
                Long latestCodeValue=d.getLong("versionCode");
                String latestVersionValue=d.getString("latestVersion");
                String notesValue=d.getString("releaseNotes");
                String urlValue=d.getString("downloadUrl");
                Boolean forceValue=d.getBoolean("forceUpdate");
                final long latestCode = latestCodeValue == null ? 0L : latestCodeValue.longValue();
                final String latestVersion = (latestVersionValue == null || latestVersionValue.trim().isEmpty())
                        ? currentName : latestVersionValue;
                final String notes = notesValue;
                final String url = urlValue;
                final Boolean force = forceValue;
                boolean updateAvailable=latestCode > currentCode;
                if(updateAvailable){
                    AlertDialog.Builder b=new AlertDialog.Builder(this)
                        .setTitle("Update MMC PONSEL tersedia")
                        .setMessage("Versi terbaru: "+latestVersion+"\\nVersi Anda: "+currentName+"\\n\\n"+
                                (notes==null?"Ada pembaruan aplikasi.":notes));
                    final String downloadUrl=url;
                    if(downloadUrl!=null && !downloadUrl.trim().isEmpty()){
                        b.setPositiveButton("Update",(dialog,which)->{
                            try{ startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(downloadUrl))); }
                            catch(Exception e){ Toast.makeText(this,"Link update tidak dapat dibuka.",Toast.LENGTH_LONG).show(); }
                        });
                    }
                    if(Boolean.TRUE.equals(force)) b.setCancelable(false);
                    else b.setNegativeButton("Nanti",null);
                    b.show();
                }else{
                    Toast.makeText(this,"Aplikasi sudah versi terbaru ("+currentName+")",Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                // App remains usable when update metadata cannot be reached.
                Toast.makeText(this,"Tidak dapat mengecek update saat ini.",Toast.LENGTH_SHORT).show();
            });
    }

    void base(String heading){
        currentRoute=routeForHeading(heading);
        root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(BG);
        AnimatedBackgroundView animated=new AnimatedBackgroundView(this);
        root.addView(animated,new android.widget.FrameLayout.LayoutParams(-1,-1));

        mainColumn=new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        mainColumn.setPadding(dp(10),dp(8),dp(10),0);
        root.addView(mainColumn,new android.widget.FrameLayout.LayoutParams(-1,-1));

        title=tv(heading,22); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        mainColumn.addView(title,new LinearLayout.LayoutParams(-1,dp(50)));
        if("home".equals(currentRoute)) {
            // Beranda memakai navigasi bawah yang sama dengan seluruh menu utama.
        } else if("login".equals(currentRoute) || "auth".equals(currentRoute)) {
            // Halaman login memakai tulisan animasi "miss cinta tuhan" sebagai pengganti "hangat".
            if("login".equals(currentRoute)) {
                TextView missCinta=tv("miss cinta tuhan",20);
                missCinta.setGravity(Gravity.CENTER);
                missCinta.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                missCinta.setTextColor(YELLOW);
                missCinta.setPadding(0,dp(2),0,dp(8));
                mainColumn.addView(missCinta,new LinearLayout.LayoutParams(-1,dp(42)));
                missCinta.setAlpha(0.35f);
                missCinta.setScaleX(0.96f);
                missCinta.setScaleY(0.96f);
                missCinta.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(900).start();
                missCinta.postDelayed(new Runnable(){
                    @Override public void run(){
                        missCinta.animate().alpha(0.55f).scaleX(1.02f).scaleY(1.02f).setDuration(700).withEndAction(()->
                                missCinta.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()
                        ).start();
                        missCinta.postDelayed(this,1450);
                    }
                },1450);
            }
            // Tidak ada menu Home/Produk/Service/Profil di atas halaman daftar/lupa password.
        }
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,0,0,dp(18));
        android.view.animation.AlphaAnimation itemFade=new android.view.animation.AlphaAnimation(0f,1f);
        itemFade.setDuration(420);
        LayoutAnimationController lac=new LayoutAnimationController(itemFade,0.07f);
        content.setLayoutAnimation(lac);

        // Semua halaman selain login/auth memakai tata letak konsisten seperti Beranda:
        // area konten dapat di-scroll, navigasi utama tetap berada di bawah, dan halaman
        // non-Beranda memiliki tombol kembali yang jelas.
        if(!"home".equals(currentRoute) && !"login".equals(currentRoute) && !"auth".equals(currentRoute)){
            Button back=btn("← Kembali");
            back.setTextSize(13);
            back.setTextColor(WHITE);
            back.setBackgroundResource(R.drawable.card);
            LinearLayout.LayoutParams backLp=new LinearLayout.LayoutParams(-1,dp(44));
            backLp.setMargins(0,0,0,dp(6));
            content.addView(back,0,backLp);
            back.setOnClickListener(v->navigateBack());
        }

        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.addView(content);
        mainColumn.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        if(!"login".equals(currentRoute) && !"auth".equals(currentRoute)){
            bottom();
        }
        setContentView(root);
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(500).start();
    }

    String routeForHeading(String heading){
        if(heading==null) return "page";
        String h=heading.toLowerCase();
        if(h.contains("login")) return "login";
        if(h.contains("daftar akun") || h.contains("lupa password")) return "auth";
        if(h.contains("kategori") || h.contains("produk")) return "products";
        if(h.contains("service")) return "service";
        if(h.contains("profil")) return "profile";
        if(h.contains("mmc ponsel")) return "home";
        if(h.contains("admin")) return "admin";
        return "page";
    }

    void navigateBack(){
        if("login".equals(currentRoute)) return;
        home();
    }

    void navigateNextMenu(){
        if("login".equals(currentRoute)) return;
        if("home".equals(currentRoute)) category();
        else if("products".equals(currentRoute)) service();
        else if("service".equals(currentRoute)) profile();
        else home();
    }

    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
        switch(event.getActionMasked()){
            case android.view.MotionEvent.ACTION_DOWN:
                gestureDownX=event.getRawX(); gestureDownY=event.getRawY(); gestureDownAt=System.currentTimeMillis();
                break;
            case android.view.MotionEvent.ACTION_UP:
                float dx=event.getRawX()-gestureDownX, dy=event.getRawY()-gestureDownY;
                long dt=Math.max(1,System.currentTimeMillis()-gestureDownAt);
                if(Math.abs(dx)>dp(110) && Math.abs(dx)>Math.abs(dy)*1.25f && dt<900){
                    if(dx>0) navigateBack(); else navigateNextMenu();
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override public void onBackPressed(){
        if("home".equals(currentRoute) || "login".equals(currentRoute)){
            super.onBackPressed();
        } else {
            home();
        }
    }

    void topMenu(){
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0,0,0,dp(6));
        String[] labels={"⌂ Home","📦 Produk","🔧 Service","👤 Profil"};
        for(String label:labels){
            Button b=btn(label);
            b.setTextSize(12);
            b.setTextColor(WHITE);
            b.setBackgroundResource(R.drawable.card);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);
            lp.setMargins(dp(2),0,dp(2),0);
            nav.addView(b,lp);
            if(label.contains("Home")) b.setOnClickListener(v->home());
            else if(label.contains("Produk")) b.setOnClickListener(v->category());
            else if(label.contains("Service")) b.setOnClickListener(v->service());
            else b.setOnClickListener(v->profile());
        }
        mainColumn.addView(nav,new LinearLayout.LayoutParams(-1,dp(55)));
    }

    void showLogin(){
        base("MMC PONSEL • Login");
        startAmbientSound();

        ImageView hackerLogo=new ImageView(this);
        hackerLogo.setImageResource(R.drawable.mmc_real_store);
        hackerLogo.setContentDescription("Foto nyata toko MMC PONSEL");
        hackerLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoLp=new LinearLayout.LayoutParams(-1,dp(175));
        logoLp.gravity=Gravity.CENTER_HORIZONTAL;
        logoLp.bottomMargin=dp(4);
        content.addView(hackerLogo,logoLp);

        // Akses admin dibuat privat: tidak ada tombol/kolom admin di halaman login.
        // Tekan lama logo hacker untuk membuka halaman login admin.
        hackerLogo.setOnLongClickListener(v->{ showAdminLogin(); return true; });

        TextView brand=tv("MMC PONSEL",26);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        brand.setTextColor(YELLOW);
        content.addView(brand,new LinearLayout.LayoutParams(-1,dp(42)));

        TextView sub=tv("Jual • Beli • Service HP",14);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(MUTED);
        content.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));

        EditText username=field("Username");
        EditText pass=field("Password");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Form login dibuat lebih kecil/sedang dan diberi bingkai neon berputar.
        NeonPanel loginPanel=new NeonPanel(this);
        LinearLayout loginFields=new LinearLayout(this);
        loginFields.setOrientation(LinearLayout.VERTICAL);
        loginFields.setPadding(dp(12),dp(10),dp(12),dp(10));
        addCompactField(loginFields,username);
        addCompactField(loginFields,pass);
        loginPanel.addView(loginFields,new android.widget.FrameLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams neonLp=new LinearLayout.LayoutParams(-1,-2);
        neonLp.gravity=Gravity.CENTER_HORIZONTAL;
        neonLp.setMargins(dp(28),dp(8),dp(28),dp(10));
        content.addView(loginPanel,neonLp);

        Button register=btn("📝 Daftar Akun Baru");
        register.setTextColor(WHITE); register.setBackgroundResource(R.drawable.card);
        content.addView(register);

        Button login=btn("🔐 Login");
        content.addView(login);

        Button forgot=btn("🔑 Lupa Password");
        forgot.setTextColor(WHITE); forgot.setBackgroundResource(R.drawable.card);
        content.addView(forgot);

        TextView note=tv("Login menggunakan username dan password.",13);
        note.setGravity(Gravity.CENTER);
        note.setTextColor(MUTED);
        content.addView(note);
        register.setOnClickListener(v->registerAccount());
        forgot.setOnClickListener(v->forgotPassword());
        login.setOnClickListener(v->loginWithUsername(username.getText().toString(),pass.getText().toString()));
    }

    void loginWithUsername(String rawUsername,String password){
        String username=rawUsername==null?"":rawUsername.trim().toLowerCase();
        String pw=password==null?"":password;
        if(username.isEmpty()||pw.isEmpty()){
            Toast.makeText(this,"Username dan password wajib diisi",Toast.LENGTH_LONG).show(); return;
        }
        if(!firebaseReady()) return;

        // Admin dikenali langsung dari username, sehingga tidak pernah masuk ke
        // pencarian username pengguna biasa di Firestore. Password diverifikasi
        // oleh Firebase Authentication, bukan oleh nilai hardcoded di aplikasi.
        if(ADMIN_USERNAME.equals(username)){
            loginAdminFirebase(pw);
            return;
        }

        // Lookup username dilakukan di Cloud Function agar login pertama dari HP lain
        // tidak bergantung pada sesi Firebase/Firestore lokal perangkat.
        java.util.HashMap<String,Object> payload=new java.util.HashMap<>();
        payload.put("username",username);
        FirebaseFunctions.getInstance().getHttpsCallable("lookupUsername").call(payload)
                .addOnSuccessListener(result->{
                    java.util.Map data=(java.util.Map)result.getData();
                    String email=data==null?null:(String)data.get("email");
                    if(email==null||email.trim().isEmpty()){
                        Toast.makeText(this,"Username tidak ditemukan. Silakan daftar akun terlebih dahulu.",Toast.LENGTH_LONG).show();
                        return;
                    }
                    auth.signInWithEmailAndPassword(email,pw).addOnCompleteListener(this,t->{
                        if(t.isSuccessful()) checkUserStatusAndLogin(auth.getCurrentUser());
                        else Toast.makeText(this,"Login gagal: username/password tidak cocok.",Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e->Toast.makeText(this,"Gagal mencari username: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    void activateAdminRole(){
        FirebaseFunctions.getInstance().getHttpsCallable("ensureAdminRole").call()
                .addOnSuccessListener(x->{
                    adminMode=true; memberMode=false;
                    sp.edit().putString("role","admin").putString("name","Admin MMC PONSEL").apply();
                    Toast.makeText(this,"Login admin berhasil",Toast.LENGTH_SHORT).show();
                    showLoginSuccessLoading(() -> adminPanel());
                })
                .addOnFailureListener(e->Toast.makeText(this,"Hak admin belum aktif: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    void registerAccount(){
        base("📝 Daftar Akun MMC PONSEL");
        ImageView hackerLogo=new ImageView(this);
        hackerLogo.setImageResource(R.drawable.mmc_real_store);
        hackerLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        content.addView(hackerLogo,new LinearLayout.LayoutParams(-1,dp(150)));
        EditText name=field("Nama lengkap");
        EditText username=field("Username unik");
        EditText email=field("Email aktif");
        EditText pass=field("Password minimal 6 karakter");
        EditText confirm=field("Ulangi password");
        pass.setInputType(129); confirm.setInputType(129);

        // Form daftar akun memakai ukuran input yang sama dengan login dan neon berputar.
        NeonPanel registerPanel=new NeonPanel(this);
        LinearLayout registerFields=new LinearLayout(this);
        registerFields.setOrientation(LinearLayout.VERTICAL);
        registerFields.setPadding(dp(12),dp(10),dp(12),dp(10));
        addCompactField(registerFields,name);
        addCompactField(registerFields,username);
        addCompactField(registerFields,email);
        addCompactField(registerFields,pass);
        addCompactField(registerFields,confirm);
        registerPanel.addView(registerFields,new android.widget.FrameLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams registerNeonLp=new LinearLayout.LayoutParams(-1,-2);
        registerNeonLp.gravity=Gravity.CENTER_HORIZONTAL;
        registerNeonLp.setMargins(dp(28),dp(8),dp(28),dp(10));
        content.addView(registerPanel,registerNeonLp);

        Button create=btn("Buat Akun"); content.addView(create);
        Button back=btn("Kembali Login"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);
        create.setOnClickListener(v->{
            String n=name.getText().toString().trim();
            String un=username.getText().toString().trim().toLowerCase();
            String e=email.getText().toString().trim();
            String p=pass.getText().toString();
            String c=confirm.getText().toString();
            if(n.isEmpty()||un.isEmpty()||e.isEmpty()||p.length()<6||!p.equals(c)){
                Toast.makeText(this,"Lengkapi data, username, dan password minimal 6 karakter.",Toast.LENGTH_LONG).show(); return;
            }
            if(ADMIN_USERNAME.equals(un)){
                Toast.makeText(this,"Username admin1 khusus untuk admin.",Toast.LENGTH_LONG).show(); return;
            }
            if(!firebaseReady())return;
            java.util.HashMap<String,Object> lookup=new java.util.HashMap<>();
            lookup.put("username",un);
            FirebaseFunctions.getInstance().getHttpsCallable("lookupUsername").call(lookup).addOnSuccessListener(result->{
                java.util.Map resultData=(java.util.Map)result.getData();
                boolean exists=resultData!=null && Boolean.TRUE.equals(resultData.get("exists"));
                if(exists){
                    Toast.makeText(this,"Username sudah digunakan.",Toast.LENGTH_LONG).show(); return;
                }
                auth.createUserWithEmailAndPassword(e,p).addOnCompleteListener(this,t->{
                    if(!t.isSuccessful()){Toast.makeText(this,"Pendaftaran gagal: "+t.getException().getMessage(),Toast.LENGTH_LONG).show();return;}
                    FirebaseUser u=auth.getCurrentUser();
                    java.util.HashMap<String,Object> data=new java.util.HashMap<>();
                    data.put("uid",u.getUid()); data.put("name",n); data.put("username",un); data.put("usernameLower",un); data.put("email",e); data.put("role","user"); data.put("blocked",false); data.put("createdAt",FieldValue.serverTimestamp());
                    db.collection("users").document(u.getUid()).set(data).addOnCompleteListener(x->{
                        if(!x.isSuccessful()){
                            Toast.makeText(this,"Akun dibuat, tetapi profil belum tersimpan.",Toast.LENGTH_LONG).show();
                            auth.signOut();
                            showLogin();
                            return;
                        }
                        Toast.makeText(this,"Akun berhasil dibuat.",Toast.LENGTH_SHORT).show();
                        showLoadingScreen("Akun berhasil dibuat • menyiapkan halaman login...", () -> {
                            auth.signOut();
                            showLogin();
                        });
                    });
                });
            }).addOnFailureListener(e1->Toast.makeText(this,"Gagal memeriksa username: "+e1.getMessage(),Toast.LENGTH_LONG).show());
        });
        back.setOnClickListener(v->showLogin());
    }

    void forgotPassword(){
        base("🔑 Lupa Password • OTP Email");
        EditText account=field("Username atau email akun");
        EditText otp=field("Kode OTP 6 digit");
        EditText p1=field("Password baru (minimal 6 karakter)");
        EditText p2=field("Ulangi password baru");
        otp.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        p1.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        p2.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(account);
        content.addView(otp);
        content.addView(p1);
        content.addView(p2);
        Button send=btn("📧 Kirim OTP ke Email"); content.addView(send);
        Button verify=btn("🔐 Verifikasi OTP & Ganti Password"); content.addView(verify);
        Button back=btn("Kembali Login"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);

        send.setOnClickListener(v->{
            String identifier=account.getText().toString().trim().toLowerCase();
            if(identifier.isEmpty()){Toast.makeText(this,"Masukkan username atau email terlebih dahulu",Toast.LENGTH_LONG).show();return;}
            if(!firebaseReady())return;
            v.setEnabled(false);
            java.util.HashMap<String,Object> payload=new java.util.HashMap<>();
            payload.put("identifier",identifier);
            FirebaseFunctions.getInstance().getHttpsCallable("requestPasswordResetOtp").call(payload)
                    .addOnSuccessListener(result->{
                        java.util.Map data=(java.util.Map)result.getData();
                        String masked=data==null?null:(String)data.get("maskedEmail");
                        Toast.makeText(this,"OTP dikirim ke "+(masked==null?"email akun":masked)+". Periksa Inbox/Spam.",Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e->Toast.makeText(this,"Gagal mengirim OTP: "+friendlyFunctionError(e),Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(x->v.setEnabled(true));
        });

        verify.setOnClickListener(v->{
            String identifier=account.getText().toString().trim().toLowerCase();
            String code=otp.getText().toString().trim();
            String a=p1.getText().toString();
            String b=p2.getText().toString();
            if(identifier.isEmpty()||!code.matches("\\d{6}")||a.length()<6||!a.equals(b)){
                Toast.makeText(this,"Isi username/email, OTP 6 digit, dan password baru dengan benar.",Toast.LENGTH_LONG).show();return;
            }
            if(!firebaseReady())return;
            v.setEnabled(false);
            java.util.HashMap<String,Object> payload=new java.util.HashMap<>();
            payload.put("identifier",identifier); payload.put("code",code); payload.put("newPassword",a);
            FirebaseFunctions.getInstance().getHttpsCallable("verifyPasswordResetOtp").call(payload)
                    .addOnSuccessListener(result->{
                        Toast.makeText(this,"Password berhasil diganti. Silakan login dengan password baru.",Toast.LENGTH_LONG).show();
                        showLogin();
                    })
                    .addOnFailureListener(e->Toast.makeText(this,"OTP gagal diverifikasi: "+friendlyFunctionError(e),Toast.LENGTH_LONG).show())
                    .addOnCompleteListener(x->v.setEnabled(true));
        });
        back.setOnClickListener(v->showLogin());
    }

    String friendlyFunctionError(Exception e){
        String m=e==null?"Terjadi kesalahan.":String.valueOf(e.getMessage());
        if(m.contains("resource-exhausted")) return "Terlalu banyak percobaan/permintaan. Tunggu sebentar lalu coba lagi.";
        if(m.contains("deadline-exceeded")) return "Kode OTP sudah kedaluwarsa. Minta OTP baru.";
        if(m.contains("permission-denied")) return "Kode OTP salah.";
        if(m.contains("not-found")) return "Akun tidak ditemukan.";
        if(m.contains("failed-precondition")) return "Layanan OTP belum dikonfigurasi di server.";
        return m;
    }

    void checkUserStatusAndLogin(FirebaseUser user){
        if(user==null)return;
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(d->{
            if(d.exists() && Boolean.TRUE.equals(d.getBoolean("blocked"))){
                auth.signOut();
                Toast.makeText(this,"Akun Anda diblokir oleh admin.",Toast.LENGTH_LONG).show();
                showLogin(); return;
            }
            onFirebaseLogin(user);
        }).addOnFailureListener(e->onFirebaseLogin(user));
    }

    void loginAdminFirebase(String pw){
        if(pw==null || pw.isEmpty()){
            Toast.makeText(this,"Password admin wajib diisi",Toast.LENGTH_LONG).show();
            return;
        }
        if(!firebaseReady()) return;
        auth.signInWithEmailAndPassword(ADMIN_EMAIL,pw).addOnCompleteListener(this,t->{
            if(!t.isSuccessful()){
                String msg = t.getException()==null ? "Login admin gagal." : t.getException().getMessage();
                Toast.makeText(this,"Login admin gagal: "+msg,Toast.LENGTH_LONG).show();
                return;
            }
            // Firebase Authentication sudah memverifikasi kredensial admin.
            // Custom claim akan dicoba di-refresh, tetapi kegagalan sinkronisasi
            // claim tidak boleh membuat akun admin yang valid terkunci dari panel.
            // Firestore Rules juga mengenali email admin1@mmcponsel.app sebagai
            // fallback server-side sampai claim admin tersedia.
            FirebaseUser adminUser = auth.getCurrentUser();
            if(adminUser==null){ showLogin(); return; }
            adminUser.getIdToken(true).addOnCompleteListener(tokenTask->{
                adminMode=true; memberMode=false;
                sp.edit().putString("role","admin").putString("name","Admin MMC PONSEL").apply();
                Runnable openPanel=()->{
                    Toast.makeText(this,"Login admin berhasil",Toast.LENGTH_SHORT).show();
                    showLoginSuccessLoading(() -> adminPanel());
                };
                FirebaseFunctions.getInstance().getHttpsCallable("ensureAdminRole").call()
                        .addOnSuccessListener(x->openPanel.run())
                        .addOnFailureListener(e->{
                            // Tetap izinkan panel karena email sudah diverifikasi oleh Firebase Auth
                            // dan Security Rules melakukan pemeriksaan admin di sisi server.
                            Toast.makeText(this,"Admin terverifikasi. Sinkronisasi role server akan dicoba lagi otomatis.",Toast.LENGTH_SHORT).show();
                            openPanel.run();
                        });
            });
        });
    }

    void showAdminLogin(){
        base("🔐 Login Admin MMC PONSEL");
        content.addView(tv("Login admin privat menggunakan Firebase Authentication.",16));
        EditText user=field("Username admin");
        EditText pass=field("Password admin");
        pass.setInputType(129);
        content.addView(user);content.addView(pass);
        Button login=btn("Masuk Admin");content.addView(login);
        Button back=btn("Kembali");back.setTextColor(WHITE);back.setBackgroundResource(R.drawable.card);content.addView(back);
        login.setOnClickListener(v->{
            String entered=user.getText().toString().trim().toLowerCase();
            if(!ADMIN_USERNAME.equals(entered)){Toast.makeText(this,"Username admin salah",Toast.LENGTH_LONG).show();return;}
            loginAdminFirebase(pass.getText().toString());
        });
        back.setOnClickListener(v->showLogin());
    }

    void showMemberLogin(){
        base("👥 Login Member MMC PONSEL");
        content.addView(tv("Member dapat menambah barang dan membalas chat pengguna.",16));
        content.addView(tv("Username member: miss • Password dibuat bebas oleh member (minimal 6 karakter).",14));
        EditText user=field("Username member");
        EditText pass=field("Password member");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(user); content.addView(pass);
        Button login=btn("Masuk Member"); content.addView(login);
        Button back=btn("Kembali ke Login Pengguna"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);
        login.setOnClickListener(v->{
            String u=user.getText().toString().trim();
            String pw=pass.getText().toString();
            if(!"miss".equals(u) || pw.length()<6){
                Toast.makeText(this,"Username harus miss dan password minimal 6 karakter",Toast.LENGTH_LONG).show(); return;
            }
            if(!firebaseReady()) return;
            String memberEmail="member.miss@mmcponsel.app";
            auth.signInWithEmailAndPassword(memberEmail,pw).addOnCompleteListener(this,task->{
                if(task.isSuccessful()){ finishMemberLogin(); }
                else {
                    auth.createUserWithEmailAndPassword(memberEmail,pw).addOnCompleteListener(this,create->{
                        if(create.isSuccessful()) finishMemberLogin();
                        else Toast.makeText(this,"Login member gagal. Password harus minimal 6 karakter dan akun member belum dibuat dengan password ini.",Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
        back.setOnClickListener(v->showLogin());
    }

    void finishMemberLogin(){
        memberMode=true; adminMode=false;
        if(auth.getCurrentUser()!=null){
            FirebaseFunctions.getInstance().getHttpsCallable("ensureMemberRole").call()
                    .addOnSuccessListener(result -> auth.getCurrentUser().getIdToken(true).addOnCompleteListener(t->{
                try{
                    if(db==null) db=FirebaseFirestore.getInstance();
                    java.util.HashMap<String,Object> u=new java.util.HashMap<>();
                    u.put("uid",auth.getCurrentUser().getUid()); u.put("name","Member Miss"); u.put("email",auth.getCurrentUser().getEmail()); u.put("role","member"); u.put("updatedAt",FieldValue.serverTimestamp());
                    db.collection("users").document(auth.getCurrentUser().getUid()).set(u,com.google.firebase.firestore.SetOptions.merge());
                }catch(Exception ignored){}
                sp.edit().putString("uid",auth.getCurrentUser().getUid()).putString("name","Member Miss").putString("role","member").apply();
                Toast.makeText(this,"Login member berhasil",Toast.LENGTH_SHORT).show(); memberPanel();
            })).addOnFailureListener(e -> Toast.makeText(this,"Role member belum aktif: "+e.getMessage(),Toast.LENGTH_LONG).show());
        }
    }

    void signInWithGoogle(){
        if(!firebaseReady()) return;
        int id=getResources().getIdentifier("default_web_client_id","string",getPackageName());
        if(id==0){
            Toast.makeText(this,"Tambahkan google-services.json dari Firebase terlebih dahulu.",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            GetGoogleIdOption googleIdOption=new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(id))
                    .build();
            GetCredentialRequest request=new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption).build();
            credentialManager.getCredentialAsync(this,request,new CancellationSignal(),authExecutor,
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override public void onResult(@NonNull GetCredentialResponse response){
                            Credential credential=response.getCredential();
                            runOnUiThread(()->handleGoogleCredential(credential));
                        }
                        @Override public void onError(@NonNull GetCredentialException e){
                            runOnUiThread(()->Toast.makeText(MainActivity.this,"Login Google dibatalkan/gagal: "+e.getLocalizedMessage(),Toast.LENGTH_LONG).show());
                        }
                    });
        } catch(Exception e){
            Toast.makeText(this,"Login Google belum terkonfigurasi: "+e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    void handleGoogleCredential(Credential credential){
        if(credential instanceof CustomCredential &&
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())){
            GoogleIdTokenCredential googleCredential=GoogleIdTokenCredential.createFrom(((CustomCredential)credential).getData());
            firebaseAuthWithGoogle(googleCredential.getIdToken());
        } else {
            Toast.makeText(this,"Jenis kredensial Google tidak didukung.",Toast.LENGTH_LONG).show();
        }
    }

    void firebaseAuthWithGoogle(String idToken){
        if(!firebaseReady()) return;
        AuthCredential credential=GoogleAuthProvider.getCredential(idToken,null);
        auth.signInWithCredential(credential).addOnCompleteListener(this,task->{
            if(task.isSuccessful()) onFirebaseLogin(auth.getCurrentUser());
            else Toast.makeText(this,"Login Google gagal: "+task.getException().getMessage(),Toast.LENGTH_LONG).show();
        });
    }

    void signInWithFacebook(){
        if(!firebaseReady()) return;
        callbackManager=CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,new FacebookCallback<LoginResult>(){
            @Override public void onSuccess(LoginResult result){handleFacebookAccessToken(result.getAccessToken());}
            @Override public void onCancel(){Toast.makeText(MainActivity.this,"Login Facebook dibatalkan.",Toast.LENGTH_SHORT).show();}
            @Override public void onError(FacebookException error){Toast.makeText(MainActivity.this,"Login Facebook gagal: "+error.getMessage(),Toast.LENGTH_LONG).show();}
        });
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile","email"));
    }

    void handleFacebookAccessToken(AccessToken token){
        AuthCredential credential=FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential).addOnCompleteListener(this,task->{
            if(task.isSuccessful()) onFirebaseLogin(auth.getCurrentUser());
            else Toast.makeText(this,"Login Facebook gagal: "+task.getException().getMessage(),Toast.LENGTH_LONG).show();
        });
    }

    boolean firebaseReady(){
        if(auth==null){
            try{auth=FirebaseAuth.getInstance();}catch(Exception e){
                Toast.makeText(this,"Firebase belum terpasang. Tambahkan google-services.json.",Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    void onFirebaseLogin(FirebaseUser user){
        if(user==null)return;
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid()).get().addOnSuccessListener(d->{
            if(d.exists() && Boolean.TRUE.equals(d.getBoolean("blocked"))){
                auth.signOut();
                Toast.makeText(this,"Akun Anda diblokir oleh admin.",Toast.LENGTH_LONG).show();
                showLogin(); return;
            }
            String name=user.getDisplayName()==null?"Pelanggan":user.getDisplayName();
            java.util.HashMap<String,Object> profile=new java.util.HashMap<>();
            profile.put("uid",user.getUid()); profile.put("name",name); profile.put("email",user.getEmail());
            if(!d.exists()) { profile.put("role","user"); profile.put("blocked",false); profile.put("createdAt",FieldValue.serverTimestamp()); }
            db.collection("users").document(user.getUid()).set(profile,com.google.firebase.firestore.SetOptions.merge());
            sp.edit().putString("user",user.getEmail()==null?user.getUid():user.getEmail())
                    .putString("name",name).putString("uid",user.getUid()).apply();
            Toast.makeText(this,"Login berhasil",Toast.LENGTH_SHORT).show();
            showLoginSuccessLoading(() -> home());
        }).addOnFailureListener(e->{
            sp.edit().putString("user",user.getEmail()==null?user.getUid():user.getEmail())
                    .putString("name",user.getDisplayName()==null?"Pelanggan":user.getDisplayName())
                    .putString("uid",user.getUid()).apply();
            showLoginSuccessLoading(() -> home());
        });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(callbackManager!=null) callbackManager.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null) return;
        Uri uri=data.getData();
        if(requestCode==REQUEST_PROFILE_PHOTO){ uploadProfilePhoto(uri); }
        else if(requestCode==REQUEST_PRODUCT_IMAGE){ pendingProductImage=uri; Toast.makeText(this,"Foto barang dipilih",Toast.LENGTH_SHORT).show(); }
        else if(requestCode==REQUEST_PRODUCT_VIDEO){ pendingProductVideo=uri; Toast.makeText(this,"Video barang dipilih",Toast.LENGTH_SHORT).show(); }
        else if(requestCode==REQUEST_SERVICE_IMAGE){ pendingServiceImage=uri; Toast.makeText(this,"Foto servis dipilih",Toast.LENGTH_SHORT).show(); }
        else if(requestCode==REQUEST_SERVICE_VIDEO){ pendingServiceVideo=uri; Toast.makeText(this,"Video servis dipilih",Toast.LENGTH_SHORT).show(); }
    }

    void chooseProfilePhoto(){
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        startActivityForResult(pick,REQUEST_PROFILE_PHOTO);
    }

    void uploadProfilePhoto(Uri uri){
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u==null){showLogin();return;}
        if(db==null)db=FirebaseFirestore.getInstance();
        StorageReference ref=FirebaseStorage.getInstance().getReference().child("profilePhotos").child(u.getUid()).child("profile.jpg");
        Toast.makeText(this,"Mengunggah foto profil...",Toast.LENGTH_SHORT).show();
        ref.putFile(uri).continueWithTask(task->{
            if(!task.isSuccessful() && task.getException()!=null) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(url->{
            db.collection("users").document(u.getUid()).set(new java.util.HashMap<String,Object>(){{put("photoUrl",url.toString());put("updatedAt",FieldValue.serverTimestamp());}},com.google.firebase.firestore.SetOptions.merge());
            sp.edit().putString("photoUrl",url.toString()).apply();
            if(profileAvatar!=null) Glide.with(this).load(url).placeholder(R.drawable.mmc_logo_4k).circleCrop().into(profileAvatar);
            Toast.makeText(this,"Foto profil berhasil diperbarui",Toast.LENGTH_LONG).show();
        }).addOnFailureListener(e->Toast.makeText(this,"Gagal upload foto: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    // ===== Store UI =====
    void home(){
        base("MMC PONSEL");
        startAmbientSound();
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);

        // Header bergaya WhatsApp: hijau, ringkas, dan fokus pada aktivitas utama.
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(14),dp(12),dp(14),dp(12));
        android.graphics.drawable.GradientDrawable headerBg=new android.graphics.drawable.GradientDrawable();
        headerBg.setColor(Color.rgb(18,140,126));
        headerBg.setCornerRadius(dp(18));
        header.setBackground(headerBg);
        TextView welcome=tv("MMC PONSEL",22);
        welcome.setTypeface(null,Typeface.BOLD);
        welcome.setTextColor(Color.WHITE);
        header.addView(welcome);
        TextView online=tv("● Online  •  Jual, Beli & Service HP",13);
        online.setTextColor(Color.WHITE);
        header.addView(online);
        content.addView(header,new LinearLayout.LayoutParams(-1,dp(78)));

        // Foto nyata besar di atas tulisan untuk tampilan beranda yang lebih modern.
        ImageView hero=new ImageView(this);
        hero.setImageResource(R.drawable.mmc_real_store);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable heroBg=new android.graphics.drawable.GradientDrawable();
        heroBg.setCornerRadius(dp(18));
        hero.setBackground(heroBg);
        LinearLayout.LayoutParams heroLp=new LinearLayout.LayoutParams(-1,dp(210));
        heroLp.setMargins(0,dp(10),0,dp(8));
        content.addView(hero,heroLp);

        TextView heroText=tv("Temukan HP yang kamu cari",20);
        heroText.setTypeface(null,Typeface.BOLD);
        heroText.setTextColor(Color.WHITE);
        heroText.setPadding(dp(6),dp(4),dp(6),dp(2));
        content.addView(heroText);
        TextView heroSub=tv("Pilihan HP baru, bekas, rusak, aksesoris, dan service dalam satu aplikasi.",13);
        heroSub.setTextColor(MUTED);
        content.addView(heroSub);

        EditText search=field("Cari HP, merek, atau layanan...");
        addCompactField(content,search);

        TextView quickTitle=tv("Menu cepat",18);
        quickTitle.setTypeface(null,Typeface.BOLD);
        quickTitle.setTextColor(Color.WHITE);
        content.addView(quickTitle);
        gridMenus();

        TextView productsTitle=tv("Produk terbaru",18);
        productsTitle.setTypeface(null,Typeface.BOLD);
        productsTitle.setTextColor(Color.WHITE);
        content.addView(productsTitle);
        productList=new LinearLayout(this);
        productList.setOrientation(LinearLayout.VERTICAL);
        content.addView(productList);
        loadCloudProducts(search);
    }

    void loadCloudProducts(EditText search){
        try{
            if(db==null) db=FirebaseFirestore.getInstance();
            db.collection("products").orderBy("createdAt",Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot,error)->{
                        if(error!=null){ content.addView(tv("Gagal memuat katalog online: "+error.getMessage(),14)); return; }
                        products.clear();
                        productList.removeAllViews();
                        if(snapshot==null || snapshot.isEmpty()){ productList.addView(tv("Belum ada barang di server. Admin dapat menambah barang dari Panel Admin.",15)); return; }
                        String q=search.getText().toString().trim().toLowerCase();
                        for(DocumentSnapshot d:snapshot.getDocuments()){
                            String n=d.getString("name"); String price=d.getString("price"); String condition=d.getString("condition"); String rating=d.getString("rating");
                            String status=d.getString("status");
                            if(n==null || (status!=null && !"published".equals(status))) continue;
                            if(q.isEmpty() || n.toLowerCase().contains(q) || (condition!=null && condition.toLowerCase().contains(q))){
                                String[] row=new String[]{n,price==null?"":price,condition==null?"":condition,rating==null?"0.0":rating};
                                products.add(row); productCard(row, productList);
                            }
                        }
                        if(products.isEmpty()) productList.addView(tv("Produk tidak ditemukan.",15));
                    });
        }catch(Exception e){content.addView(tv("Firebase belum dikonfigurasi: "+e.getMessage(),14));}
    }
    EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setBackgroundResource(R.drawable.edit);return e;}

    void addCompactField(LinearLayout parent, EditText e){
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));
        lp.setMargins(0,dp(4),0,dp(4));
        parent.addView(e,lp);
    }

    class NeonPanel extends android.widget.FrameLayout {
        Paint neonPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        float rotation=0f;
        Runnable animator;
        NeonPanel(android.content.Context c){
            super(c);
            setWillNotDraw(false);
            setPadding(dp(2),dp(2),dp(2),dp(2));
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
            animator=new Runnable(){ @Override public void run(){
                rotation=(rotation+5f)%360f;
                invalidate();
                postDelayed(this,30);
            }};
            post(animator);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            if(w<=0||h<=0) return;
            RectF r=new RectF(dp(3),dp(3),w-dp(3),h-dp(3));
            neonPaint.setStyle(Paint.Style.STROKE);
            neonPaint.setStrokeCap(Paint.Cap.ROUND);
            neonPaint.setStrokeWidth(dp(2));
            int[] colors={Color.rgb(0,229,255),Color.rgb(168,85,247),Color.rgb(255,212,0),Color.rgb(0,255,170)};
            for(int i=0;i<colors.length;i++){
                neonPaint.setColor(colors[i]);
                neonPaint.setAlpha(190);
                neonPaint.setShadowLayer(dp(8),0,0,colors[i]);
                c.drawArc(r,rotation+i*90f,58f,false,neonPaint);
            }
            neonPaint.clearShadowLayer();
        }
    }
    int menuIcon(String key){
        if(key==null) return R.drawable.ic_menu_home;
        String k=key.toLowerCase();
        if(k.contains("baru")) return R.drawable.ic_menu_phone_new;
        if(k.contains("bekas")) return R.drawable.ic_menu_phone_used;
        if(k.contains("rusak")) return R.drawable.ic_menu_phone_broken;
        if(k.contains("servis") || k.contains("service")) return R.drawable.ic_menu_service;
        if(k.contains("jual")) return R.drawable.ic_menu_sell;
        if(k.contains("beli")) return R.drawable.ic_menu_buy;
        if(k.contains("profil")) return R.drawable.ic_menu_profile;
        return R.drawable.ic_menu_home;
    }
    void gridMenus(){
        String[][] m={{"HP Baru","Jual HP baru"},{"HP Bekas","HP bekas"},{"HP Rusak","HP rusak"},{"Servis","Servis HP"},{"Jual HP","Jual perangkat"},{"Beli HP","Belanja HP"}};
        for(int i=0;i<m.length;i+=2){
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for(int j=i;j<Math.min(i+2,m.length);j++){
                String[] x=m[j];
                LinearLayout b=new LinearLayout(this);
                b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER); b.setPadding(dp(6),dp(6),dp(6),dp(6));
                b.setBackgroundResource(R.drawable.card);
                ImageView icon=new ImageView(this); icon.setImageResource(menuIcon(x[0])); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                b.addView(icon,new LinearLayout.LayoutParams(-1,dp(64)));
                TextView title=tv(x[0],14); title.setGravity(Gravity.CENTER); title.setTypeface(null,Typeface.BOLD); b.addView(title,new LinearLayout.LayoutParams(-1,dp(24)));
                TextView sub=tv(x[1],11); sub.setGravity(Gravity.CENTER); sub.setTextColor(MUTED); b.addView(sub,new LinearLayout.LayoutParams(-1,dp(20)));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(132),1); lp.setMargins(dp(3),dp(3),dp(3),dp(3)); row.addView(b,lp);
                if(x[0].equals("Servis")) b.setOnClickListener(v->service());
                else if(x[0].equals("Beli HP")) b.setOnClickListener(v->category());
                else if(x[0].equals("Jual HP")) b.setOnClickListener(v->sell());
                else if(x[0].equals("HP Rusak")) b.setOnClickListener(v->damagedPhones());
                else b.setOnClickListener(v->category());
            }
            content.addView(row);
        }
    }
    void productCard(String[] p){productCard(p,content);}
    void productCard(String[] p, LinearLayout target){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(12),dp(12),dp(12),dp(12));
        android.graphics.drawable.GradientDrawable cardBg=new android.graphics.drawable.GradientDrawable(); cardBg.setColor(Color.rgb(18,30,39)); cardBg.setCornerRadius(dp(16)); c.setBackground(cardBg);
        String imageUrl=p.length>4?p[4]:"", videoUrl=p.length>5?p[5]:"";
        if(imageUrl!=null && !imageUrl.isEmpty()){ ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); Glide.with(this).load(imageUrl).into(im); c.addView(im,new LinearLayout.LayoutParams(-1,dp(190))); }
        TextView n=tv(p[0],17); n.setTypeface(null,Typeface.BOLD); c.addView(n);
        TextView meta=tv(p[1]+" • "+p[2]+" • ⭐ "+p[3],14); meta.setTextColor(MUTED); c.addView(meta);
        if(videoUrl!=null && !videoUrl.isEmpty()){ Button video=btn("▶ Lihat Video Barang"); c.addView(video); video.setOnClickListener(v->openMedia(videoUrl,"video/*")); }
        Button b=btn("Lihat detail / Tambah keranjang"); c.addView(b); b.setOnClickListener(v->detail(p));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(5),0,dp(5)); target.addView(c,lp);
    }
    void openMedia(String url,String type){ try{Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(Uri.parse(url),type); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i);}catch(Exception e){Toast.makeText(this,"Media tidak dapat dibuka",Toast.LENGTH_SHORT).show();} }
    void detail(String[] p){
        base("Detail Produk");
        if(p.length>4 && p[4]!=null && !p[4].isEmpty()){ ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); Glide.with(this).load(p[4]).into(im); content.addView(im,new LinearLayout.LayoutParams(-1,dp(220))); }
        content.addView(tv(p[0],23)); content.addView(tv(p[1]+"\nKondisi: "+p[2]+"\nRating: ⭐ "+p[3]+"\nProduk online MMC PONSEL.",16));
        if(p.length>5 && p[5]!=null && !p[5].isEmpty()){ Button v=btn("▶ Putar Video Produk"); content.addView(v); v.setOnClickListener(x->openMedia(p[5],"video/*")); }
        Button cart=btn("Tambah ke Keranjang Online"); content.addView(cart); Button buy=btn("Beli Sekarang"); content.addView(buy);
        cart.setOnClickListener(v->{ FirebaseUser u=auth==null?null:auth.getCurrentUser(); if(u==null){showLogin();return;} java.util.HashMap<String,Object> data=new java.util.HashMap<>(); data.put("productName",p[0]);data.put("price",p[1]);data.put("condition",p[2]);data.put("imageUrl",p.length>4?p[4]:"");data.put("createdAt",FieldValue.serverTimestamp()); db.collection("users").document(u.getUid()).collection("cart").add(data).addOnSuccessListener(x->Toast.makeText(this,"Keranjang tersimpan online",Toast.LENGTH_SHORT).show()).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show()); });
        buy.setOnClickListener(v->checkout(p));
    }
    void checkout(String[] p){
        base("Checkout & Pembayaran Online");
        content.addView(tv(p[0],20));
        content.addView(tv("Total: "+p[1]+"\nPembayaran aman melalui Midtrans (QRIS, e-wallet, transfer bank, kartu sesuai metode yang aktif di akun merchant).",16));
        EditText addr=field("Alamat pengiriman");content.addView(addr);
        Button pay=btn("💳 Bayar Sekarang via Midtrans");content.addView(pay);
        TextView note=tv("Transaksi dibuat di server MMC PONSEL. Status pembayaran diperbarui melalui webhook gateway.",13);note.setTextColor(MUTED);content.addView(note);
        pay.setOnClickListener(v->{
            FirebaseUser u=auth==null?null:auth.getCurrentUser();
            if(u==null){showLogin();return;}
            if(addr.length()==0){Toast.makeText(this,"Masukkan alamat",0).show();return;}
            long amount=parseRupiah(p[1]);
            if(amount<=0){Toast.makeText(this,"Harga produk tidak valid untuk pembayaran online",Toast.LENGTH_LONG).show();return;}
            java.util.HashMap<String,Object> order=new java.util.HashMap<>();
            order.put("uid",u.getUid());order.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());order.put("email",u.getEmail());
            order.put("productName",p[0]);order.put("price",p[1]);order.put("amount",amount);order.put("condition",p[2]);order.put("address",addr.getText().toString().trim());
            order.put("status","Menunggu Pembayaran");order.put("paymentStatus","pending");order.put("createdAt",FieldValue.serverTimestamp());
            db.collection("orders").add(order).addOnSuccessListener(doc->{
                Toast.makeText(this,"Pesanan dibuat. Membuka pembayaran...",Toast.LENGTH_SHORT).show();
                java.util.HashMap<String,Object> args=new java.util.HashMap<>();args.put("orderId",doc.getId());args.put("grossAmount",amount);args.put("productName",p[0]);args.put("email",u.getEmail()==null?"":u.getEmail());
                FirebaseFunctions.getInstance().getHttpsCallable("createMidtransTransaction").call(args).addOnSuccessListener(result->{
                    Object data=result.getData();
                    if(data instanceof java.util.Map){Object url=((java.util.Map<?,?>)data).get("redirectUrl");if(url!=null){openPaymentUrl(url.toString());return;}}
                    Toast.makeText(this,"Gateway pembayaran tidak mengembalikan link.",Toast.LENGTH_LONG).show();
                }).addOnFailureListener(e->Toast.makeText(this,"Pembayaran belum terhubung ke backend Midtrans: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }).addOnFailureListener(e->Toast.makeText(this,"Gagal membuat pesanan: "+e.getMessage(),Toast.LENGTH_LONG).show());
        });
    }

    long parseRupiah(String value){
        if(value==null)return 0; String digits=value.replaceAll("[^0-9]","");
        if(digits.isEmpty())return 0; try{return Long.parseLong(digits);}catch(Exception e){return 0;}
    }

    void openPaymentUrl(String url){
        try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));startActivity(i);}catch(Exception e){Toast.makeText(this,"Tidak dapat membuka halaman pembayaran",Toast.LENGTH_LONG).show();}
    }

    void damagedPhones(){
        base("HP Rusak — Pilih Kerusakan");
        content.addView(tv("Pilih jenis kerusakan. Gambar ditampilkan di atas setiap nama agar mudah dikenali.",15));
        String[][] items={
                {"Layar Pecah","Kerusakan layar / touchscreen","ic_damage_screen"},
                {"Baterai Rusak","Baterai cepat habis / bengkak","ic_damage_battery"},
                {"Tidak Bisa Nyala","HP mati total / power bermasalah","ic_damage_power"},
                {"Kamera Rusak","Kamera buram / tidak berfungsi","ic_damage_camera"},
                {"Kena Air","Terkena air / cairan","ic_damage_water"},
                {"Software Bermasalah","Bootloop / sistem bermasalah","ic_damage_software"}
        };
        for(int i=0;i<items.length;i+=2){
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER);
            for(int j=i;j<Math.min(i+2,items.length);j++){
                String[] item=items[j];
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER);
                card.setPadding(dp(8),dp(8),dp(8),dp(8)); card.setBackgroundResource(R.drawable.card);
                int res=getResources().getIdentifier(item[2],"drawable",getPackageName());
                ImageView image=new ImageView(this); image.setImageResource(res); image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                card.addView(image,new LinearLayout.LayoutParams(-1,dp(72)));
                TextView title=tv(item[0],15); title.setGravity(Gravity.CENTER); title.setTypeface(null,Typeface.BOLD); card.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
                TextView desc=tv(item[1],11); desc.setGravity(Gravity.CENTER); desc.setTextColor(MUTED); card.addView(desc,new LinearLayout.LayoutParams(-1,dp(34)));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(156),1); lp.setMargins(dp(4),dp(4),dp(4),dp(4)); row.addView(card,lp);
                card.setOnClickListener(v->serviceForm(item[0]));
            }
            content.addView(row);
        }
    }
    void category(){base("Kategori HP");loadCategoryFromServer();}
    void loadCategoryFromServer(){
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("products").orderBy("createdAt",Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot,error)->{
                    if(error!=null){content.addView(tv("Katalog online gagal dimuat: "+error.getMessage(),14));return;}
                    if(content==null)return;
                    // Keep a single live catalog container so every new product appears automatically.
                    LinearLayout liveList=new LinearLayout(this); liveList.setOrientation(LinearLayout.VERTICAL);
                    content.addView(tv("● KATALOG ONLINE — diperbarui otomatis",14));
                    content.addView(liveList);
                    if(snapshot==null || snapshot.isEmpty()){liveList.addView(tv("Belum ada produk online.",16));return;}
                    for(DocumentSnapshot d:snapshot.getDocuments()){
                        String n=d.getString("name"),p=d.getString("price"),c=d.getString("condition"),r=d.getString("rating"),status=d.getString("status");
                        if(n!=null && (status==null || "published".equals(status)))productCard(new String[]{n,p==null?"":p,c==null?"":c,r==null?"0.0":r,d.getString("imageUrl")==null?"":d.getString("imageUrl"),d.getString("videoUrl")==null?"":d.getString("videoUrl"),d.getId()},liveList);
                    }
                });
    }
    void service(){
        base("Servis HP — Online");
        content.addView(tv("Buat pesanan servis dan lihat permintaan servis pengguna secara online.",17));
        for(String s:new String[]{"Layar Pecah / Touchscreen","Ganti Baterai","Tidak Bisa Dinyalakan","Kamera","Water Damage","Software / Unlock","Lainnya"}){
            Button b=btn(s); content.addView(b); b.setOnClickListener(v->serviceForm(s));
        }
        content.addView(tv("Pesanan Servis Online",19));
        content.addView(tv("Pesanan yang dibuat pengguna tampil di sini agar pengguna dapat menawarkan bantuan atau menghubungi pemesan secara pribadi.",14));
        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("serviceRequests").orderBy("createdAt",Query.Direction.DESCENDING).limit(50)
          .addSnapshotListener((snap,e)->{
            if(e!=null){content.addView(tv("Gagal memuat pesanan servis: "+e.getMessage(),14));return;}
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); content.addView(list);
            if(snap==null || snap.isEmpty()){list.addView(tv("Belum ada pesanan servis online.",15));return;}
            FirebaseUser me=auth==null?null:auth.getCurrentUser(); String myUid=me==null?"":me.getUid();
            for(DocumentSnapshot d:snap.getDocuments()){
                String owner=d.getString("uid"), name=d.getString("userName"), type=d.getString("type"), note=d.getString("note"), status=d.getString("status");
                LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackgroundResource(R.drawable.card);
                TextView title=tv("🔧 "+(type==null?"Servis HP":type),17); title.setTypeface(null,Typeface.BOLD); card.addView(title);
                card.addView(tv("Pemesan: "+(name==null?"Pengguna":name),14));
                card.addView(tv("Keluhan: "+(note==null||note.isEmpty()?"-":note),14));
                card.addView(tv("Status: "+(status==null?"Menunggu Diproses":status),13));
                String imageUrl=d.getString("imageUrl"), videoUrl=d.getString("videoUrl");
                if(imageUrl!=null && !imageUrl.isEmpty()){ ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); Glide.with(this).load(imageUrl).into(im); card.addView(im,new LinearLayout.LayoutParams(-1,dp(190))); }
                if(videoUrl!=null && !videoUrl.isEmpty()){ Button vv=btn("▶ Lihat Video Kerusakan"); card.addView(vv); vv.setOnClickListener(v->openMedia(videoUrl,"video/*")); }
                if(owner!=null && !owner.equals(myUid)){
                    Button chat=btn("💬 Hubungi pribadi"); card.addView(chat); String finalName=name==null?"Pengguna":name; String finalOwner=owner; chat.setOnClickListener(v->privateChat(finalOwner,finalName));
                }else if(owner!=null){
                    TextView own=tv("Pesanan Anda",13); own.setTextColor(GREEN); card.addView(own);
                }
                list.addView(card,new LinearLayout.LayoutParams(-1,-2));
            }
          });
    }
    void serviceForm(String type){
        base("Pesan Service Online"); pendingServiceImage=null; pendingServiceVideo=null;
        content.addView(tv("Jenis: "+type,20)); EditText note=field("Keluhan / catatan");content.addView(note);EditText phone=field("Nomor HP");content.addView(phone);
        Button photo=btn("📷 Upload Foto Kerusakan"); Button video=btn("🎥 Upload Video Kerusakan"); content.addView(photo);content.addView(video);
        photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQUEST_SERVICE_IMAGE);});
        video.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("video/*");startActivityForResult(i,REQUEST_SERVICE_VIDEO);});
        Button b=btn("Kirim Permintaan Service Online");content.addView(b);
        b.setOnClickListener(v->{FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}if(phone.length()==0){Toast.makeText(this,"Masukkan nomor HP",0).show();return;} b.setEnabled(false); Toast.makeText(this,"Mengunggah foto/video servis...",Toast.LENGTH_SHORT).show();
            uploadMediaPair("serviceRequests/"+u.getUid()+"/",pendingServiceImage,pendingServiceVideo,(imageUrl,videoUrl)->{java.util.HashMap<String,Object> data=new java.util.HashMap<>();data.put("uid",u.getUid());data.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());data.put("email",u.getEmail());data.put("type",type);data.put("note",note.getText().toString().trim());data.put("phone",phone.getText().toString().trim());data.put("imageUrl",imageUrl);data.put("videoUrl",videoUrl);data.put("status","Menunggu Diproses");data.put("createdAt",FieldValue.serverTimestamp());db.collection("serviceRequests").add(data).addOnSuccessListener(x->{Toast.makeText(this,"Permintaan service + media terkirim online.",Toast.LENGTH_LONG).show();service();}).addOnFailureListener(e->{b.setEnabled(true);Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();});},e->{b.setEnabled(true);Toast.makeText(this,"Upload gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();});
        });
    }
    void sell(){base("Jual HP Online");EditText category=field("Kategori");EditText brand=field("Merek");EditText model=field("Model");EditText condition=field("Kondisi");EditText price=field("Harga yang diinginkan");EditText phone=field("Nomor kontak");for(EditText e:new EditText[]{category,brand,model,condition,price,phone})content.addView(e);Button b=btn("Kirim Penawaran ke Server");content.addView(b);b.setOnClickListener(v->{FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}if(model.length()==0||phone.length()==0){Toast.makeText(this,"Model dan nomor kontak wajib diisi",0).show();return;}java.util.HashMap<String,Object> data=new java.util.HashMap<>();data.put("uid",u.getUid());data.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());data.put("email",u.getEmail());data.put("category",category.getText().toString().trim());data.put("brand",brand.getText().toString().trim());data.put("model",model.getText().toString().trim());data.put("condition",condition.getText().toString().trim());data.put("desiredPrice",price.getText().toString().trim());data.put("phone",phone.getText().toString().trim());data.put("status","Menunggu Ditinjau");data.put("createdAt",FieldValue.serverTimestamp());db.collection("sellRequests").add(data).addOnSuccessListener(x->{Toast.makeText(this,"Penawaran terkirim online",Toast.LENGTH_LONG).show();transactions();}).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());});}
    void bottom(){
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(6),dp(5),dp(6),dp(7)); nav.setBackgroundResource(R.drawable.card);
        String[][] items={{"Beranda","Beranda"},{"Beli","Beli"},{"Servis","Servis"},{"Profil","Profil"}};
        for(String[] item:items){
            LinearLayout cell=new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setGravity(Gravity.CENTER);
            ImageView icon=new ImageView(this); icon.setImageResource(menuIcon(item[0])); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); cell.addView(icon,new LinearLayout.LayoutParams(-1,dp(28)));
            TextView label=tv(item[1],11); label.setGravity(Gravity.CENTER); label.setTextColor(MUTED); cell.addView(label,new LinearLayout.LayoutParams(-1,dp(20)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(54),1); lp.setMargins(dp(2),0,dp(2),0); nav.addView(cell,lp);
            addClickFeedback(cell); cell.setOnClickListener(v->{ if("Beranda".equals(item[1])) home(); else if("Beli".equals(item[1])) category(); else if("Servis".equals(item[1])) service(); else profile(); });
        }
        mainColumn.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));
    }
    void profile(){
        base(adminMode?"Profil Admin":"Profil Saya");
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u==null){showLogin();return;}

        LinearLayout identity=new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.setGravity(Gravity.CENTER_HORIZONTAL);
        identity.setPadding(dp(16),dp(16),dp(16),dp(16));
        identity.setBackgroundResource(R.drawable.card);

        profileAvatar=new ImageView(this);
        profileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams avatarLp=new LinearLayout.LayoutParams(dp(104),dp(104));
        avatarLp.gravity=Gravity.CENTER_HORIZONTAL;
        identity.addView(profileAvatar,avatarLp);
        String cachedPhoto=sp.getString("photoUrl","");
        if(cachedPhoto.length()>0) Glide.with(this).load(cachedPhoto).placeholder(R.drawable.mmc_logo_4k).circleCrop().into(profileAvatar);
        else profileAvatar.setImageResource(R.drawable.mmc_logo_4k);

        String displayName=u.getDisplayName()==null?sp.getString("name","Pelanggan"):u.getDisplayName();
        TextView name=tv(displayName,19); name.setGravity(Gravity.CENTER); name.setTypeface(null,Typeface.BOLD); name.setTextColor(WHITE); identity.addView(name,new LinearLayout.LayoutParams(-1,dp(32)));
        TextView email=tv(u.getEmail()==null?"":u.getEmail(),13); email.setGravity(Gravity.CENTER); email.setTextColor(MUTED); identity.addView(email,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView role=tv(adminMode?"ADMIN • Akun terverifikasi":"MEMBER • Akun online",12); role.setGravity(Gravity.CENTER); role.setTypeface(null,Typeface.BOLD); role.setTextColor(adminMode?YELLOW:GREEN); identity.addView(role,new LinearLayout.LayoutParams(-1,dp(26)));
        content.addView(identity,new LinearLayout.LayoutParams(-1,-2));

        TextView section=tv("Akun & Aktivitas",16); section.setTypeface(null,Typeface.BOLD); section.setTextColor(WHITE); section.setPadding(dp(4),dp(16),dp(4),dp(4)); content.addView(section);
        profileAction("📷","Foto Profil","Ganti foto profil",()->chooseProfilePhoto());
        profileAction("➕","Tambah Barang","Jual HP atau barang melalui profil",()->profileAddProduct());
        profileAction("🔐","Ubah Password","Kelola password akun",()->changePassword());
        profileAction("💾","Data Akun Online","Simpan dan kelola data akun",()->saveProfileOnline());
        profileAction("🧾","Transaksi Saya","Lihat transaksi dan pesanan",()->transactions());

        TextView comm=tv("Komunikasi",16); comm.setTypeface(null,Typeface.BOLD); comm.setTextColor(WHITE); comm.setPadding(dp(4),dp(16),dp(4),dp(4)); content.addView(comm);
        profileAction("🤖","Chat AI","Asisten AI nyata untuk membantu Anda",()->aiChat());
        profileAction("💬","Chat Admin","Hubungi admin MMC PONSEL",()->chatWithAdmin());
        profileAction("🌐","Chat Global","Chat online yang dapat dilihat semua pengguna",()->solutionGroup());
        profileAction("🟢","WhatsApp","Hubungi MMC PONSEL",()->openWhatsApp());

        if(adminMode){
            TextView adminSection=tv("Administrasi",16); adminSection.setTypeface(null,Typeface.BOLD); adminSection.setTextColor(YELLOW); adminSection.setPadding(dp(4),dp(16),dp(4),dp(4)); content.addView(adminSection);
            profileAction("🛡","Panel Admin","Kelola pengguna dan produk",()->adminPanel());
        }else{
            profileAction("👤","Pusat Member","Fitur dan informasi member",()->memberPanel());
        }
        profileAction("↪","Keluar","Keluar dari akun ini",()->logout());

        db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{
            String photoUrl=d.getString("photoUrl");
            if(photoUrl!=null && !photoUrl.isEmpty()){
                sp.edit().putString("photoUrl",photoUrl).apply();
                if(profileAvatar!=null) Glide.with(this).load(photoUrl).placeholder(R.drawable.mmc_logo_4k).circleCrop().into(profileAvatar);
            }
        });
    }

    int profileIcon(String title){
        String k=title==null?"":title.toLowerCase();
        if(k.contains("foto")) return R.drawable.ic_profile_photo;
        if(k.contains("tambah")) return R.drawable.ic_profile_add;
        if(k.contains("password")) return R.drawable.ic_profile_lock;
        if(k.contains("data akun")) return R.drawable.ic_profile_data;
        if(k.contains("transaksi")) return R.drawable.ic_profile_receipt;
        if(k.contains("chat ai")) return R.drawable.ic_profile_ai;
        if(k.contains("chat admin")) return R.drawable.ic_profile_chat;
        if(k.contains("chat global")) return R.drawable.ic_profile_global;
        if(k.contains("whatsapp")) return R.drawable.ic_profile_whatsapp;
        if(k.contains("panel admin")) return R.drawable.ic_profile_admin;
        if(k.contains("pusat member")) return R.drawable.ic_profile_member;
        if(k.contains("keluar")) return R.drawable.ic_profile_logout;
        return R.drawable.ic_menu_profile;
    }

    void profileAction(String icon,String titleText,String subtitle,Runnable action){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12),dp(7),dp(10),dp(7));
        row.setBackgroundResource(R.drawable.card);
        ImageView ic=new ImageView(this); ic.setImageResource(profileIcon(titleText)); ic.setScaleType(ImageView.ScaleType.CENTER_INSIDE); row.addView(ic,new LinearLayout.LayoutParams(dp(44),dp(52)));
        LinearLayout texts=new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.setGravity(Gravity.CENTER_VERTICAL);
        TextView t=tv(titleText,15); t.setTypeface(null,Typeface.BOLD); t.setTextColor(WHITE); texts.addView(t);
        TextView st=tv(subtitle,12); st.setTextColor(MUTED); texts.addView(st);
        row.addView(texts,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView arrow=tv("›",25); arrow.setTextColor(MUTED); arrow.setGravity(Gravity.CENTER); row.addView(arrow,new LinearLayout.LayoutParams(dp(30),dp(52)));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(68)); rp.setMargins(0,dp(4),0,dp(4)); content.addView(row,rp);
        addClickFeedback(row); row.setOnClickListener(v->action.run());
    }

    void changePassword(){
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u==null){showLogin();return;}
        base("🔐 Ubah Password");
        EditText p1=field("Password baru (min. 6 karakter)");
        EditText p2=field("Ulangi password baru");
        p1.setInputType(129);p2.setInputType(129);
        content.addView(p1);content.addView(p2);
        Button save=btn("Simpan Password");content.addView(save);
        save.setOnClickListener(v->{
            String a=p1.getText().toString(),b=p2.getText().toString();
            if(a.length()<6||!a.equals(b)){Toast.makeText(this,"Password minimal 6 karakter dan harus sama",Toast.LENGTH_LONG).show();return;}
            u.updatePassword(a).addOnCompleteListener(this,t->Toast.makeText(this,t.isSuccessful()?"Password berhasil diubah.":"Sesi login sudah lama. Logout lalu login kembali sebelum mengubah password.",Toast.LENGTH_LONG).show());
        });
    }

    void profileAddProduct(){
        if(auth==null||auth.getCurrentUser()==null){showLogin();return;}
        addProductForUser();
    }

    void addProductForUser(){
        base("➕ Tambah Barang Online"); pendingProductImage=null; pendingProductVideo=null;
        EditText name=field("Nama HP / barang"); EditText price=field("Harga, contoh Rp 3.500.000"); EditText condition=field("Kondisi: Baru / Bekas / Rusak"); EditText category=field("Kategori: HP / Aksesoris / Lainnya"); EditText description=field("Deskripsi barang");
        content.addView(name);content.addView(price);content.addView(condition);content.addView(category);content.addView(description);
        Button photo=btn("📷 Upload Foto Barang"); Button video=btn("🎥 Upload Video Barang"); content.addView(photo);content.addView(video);
        TextView hint=tv("Foto/video akan tersimpan di Firebase dan otomatis tampil kepada pengguna lain.",12); hint.setTextColor(MUTED); content.addView(hint);
        photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");startActivityForResult(i,REQUEST_PRODUCT_IMAGE);});
        video.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("video/*");startActivityForResult(i,REQUEST_PRODUCT_VIDEO);});
        Button save=btn("🚀 Publikasikan ke Produk Online");content.addView(save);
        save.setOnClickListener(v->{
            FirebaseUser u=auth.getCurrentUser(); if(name.length()==0||price.length()==0){Toast.makeText(this,"Nama dan harga wajib diisi",Toast.LENGTH_LONG).show();return;}
            save.setEnabled(false); Toast.makeText(this,"Mengunggah media...",Toast.LENGTH_SHORT).show();
            uploadMediaPair("products/"+u.getUid()+"/",pendingProductImage,pendingProductVideo,(imageUrl,videoUrl)->{
                java.util.HashMap<String,Object> d=new java.util.HashMap<>(); d.put("name",name.getText().toString().trim());d.put("price",price.getText().toString().trim());d.put("condition",condition.getText().toString().trim());d.put("category",category.getText().toString().trim());d.put("description",description.getText().toString().trim());d.put("rating","0.0");d.put("ownerUid",u.getUid());d.put("ownerName",u.getDisplayName()==null?"Pengguna":u.getDisplayName());d.put("imageUrl",imageUrl);d.put("videoUrl",videoUrl);d.put("createdAt",FieldValue.serverTimestamp());d.put("status","published");
                db.collection("products").add(d).addOnSuccessListener(x->{Toast.makeText(this,"Barang dan media berhasil dipublikasikan online.",Toast.LENGTH_LONG).show();category();}).addOnFailureListener(e->{save.setEnabled(true);Toast.makeText(this,"Gagal menyimpan: "+e.getMessage(),Toast.LENGTH_LONG).show();});
            },e->{save.setEnabled(true);Toast.makeText(this,"Upload gagal: "+e.getMessage(),Toast.LENGTH_LONG).show();});
        });
    }
    interface MediaDone { void ok(String imageUrl,String videoUrl); }
    interface MediaFail { void fail(Exception e); }
    void uploadMediaPair(String folder,Uri image,Uri video,MediaDone done,MediaFail fail){
        uploadOne(folder,"foto.jpg",image,(iu)->uploadOne(folder,"video.mp4",video,(vu)->done.ok(iu,vu),fail),fail);
    }
    interface UrlDone { void ok(String url); }
    void uploadOne(String folder,String name,Uri uri,UrlDone done,MediaFail fail){
        if(uri==null){done.ok("");return;}
        String mime=getContentResolver().getType(uri); if(mime==null) mime=name.endsWith("mp4")?"video/mp4":"image/jpeg";
        StorageReference ref=FirebaseStorage.getInstance().getReference().child(folder+name);
        ref.putFile(uri, new com.google.firebase.storage.StorageMetadata.Builder().setContentType(mime).build()).continueWithTask(t->{if(!t.isSuccessful()&&t.getException()!=null)throw t.getException();return ref.getDownloadUrl();}).addOnSuccessListener(x->done.ok(x.toString())).addOnFailureListener(fail::fail);
    }

    void saveProfileOnline(){FirebaseUser u=auth.getCurrentUser();if(u==null)return;EditText address=field("Alamat pengiriman");content.addView(address);Button save=btn("Simpan ke Server");content.addView(save);save.setOnClickListener(v->{db.collection("users").document(u.getUid()).set(new java.util.HashMap<String,Object>(){{put("uid",u.getUid());put("name",u.getDisplayName());put("email",u.getEmail());put("address",address.getText().toString().trim());put("updatedAt",FieldValue.serverTimestamp());}},com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener(x->Toast.makeText(this,"Profil tersimpan online",Toast.LENGTH_SHORT).show());});}
    void transactions(){base("Transaksi Online");FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}content.addView(tv("Pesanan",19));db.collection("orders").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada pesanan.",15));for(DocumentSnapshot d:s.getDocuments())content.addView(tv((d.getString("productName")==null?"":d.getString("productName"))+"\n"+(d.getString("price")==null?"":d.getString("price"))+"\nStatus: "+(d.getString("status")==null?"":d.getString("status")),16));});content.addView(tv("Service",19));db.collection("serviceRequests").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada permintaan service.",15));for(DocumentSnapshot d:s.getDocuments())content.addView(tv("🔧 "+d.getString("type")+"\nStatus: "+d.getString("status"),16));});}

    void logout(){try{if(auth!=null)auth.signOut();LoginManager.getInstance().logOut();}catch(Exception ignored){}adminMode=false;memberMode=false;sp.edit().clear().apply();showLogin();}

    void memberPanel(){
        base("👤 Area Member MMC PONSEL");
        TextView badge=tv("MEMBER • Pengguna Biasa",18); badge.setTextColor(GREEN); badge.setTypeface(null,Typeface.BOLD); badge.setBackgroundResource(R.drawable.card); content.addView(badge);
        content.addView(tv("Akses member: lihat katalog, tambah barang melalui Profil, service, transaksi, dan chat.",15));
        Button chats=btn("💬 Chat Admin MMC PONSEL"); content.addView(chats); chats.setOnClickListener(v->chatWithAdmin());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
        Button prof=btn("👤 Kembali ke Profil"); content.addView(prof); prof.setOnClickListener(v->profile());
        Button out=btn("Logout Member"); out.setTextColor(WHITE); out.setBackgroundResource(R.drawable.card); content.addView(out); out.setOnClickListener(v->logout());
    }

    // ===== Admin =====
    void adminPanel(){
        base("🛡 ADMIN MMC PONSEL");
        TextView badge=tv("ADMIN • Kontrol Penuh",18); badge.setTextColor(YELLOW); badge.setTypeface(null,Typeface.BOLD); badge.setBackgroundResource(R.drawable.card); content.addView(badge);
        content.addView(tv("Panel khusus admin. Kelola pengguna, blokir akun, hapus pengguna, dan moderasi produk.",15));
        Button users=btn("🛡 Kelola Pengguna"); content.addView(users); users.setOnClickListener(v->adminUsers());
        Button productsBtn=btn("📦 Kelola & Hapus Produk"); content.addView(productsBtn); productsBtn.setOnClickListener(v->adminProducts());
        Button chats=btn("💬 Chat Pengguna"); content.addView(chats); chats.setOnClickListener(v->adminChats());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
        Button prof=btn("👤 Profil Admin"); content.addView(prof); prof.setOnClickListener(v->profile());
        Button out=btn("Logout Admin"); content.addView(out); out.setOnClickListener(v->logout());
    }

    void adminUsers(){
        base("👥 Kelola Pengguna");
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("users").get().addOnSuccessListener(snap->{
            if(snap.isEmpty()){content.addView(tv("Belum ada profil pengguna.",16));return;}
            for(DocumentSnapshot d:snap.getDocuments()){
                String uid=d.getId(), name=d.getString("name"), email=d.getString("email"), role=d.getString("role");
                boolean blocked=Boolean.TRUE.equals(d.getBoolean("blocked"));
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackgroundResource(R.drawable.card);
                card.addView(tv((name==null?"Pengguna":name)+"\n"+(email==null?"":email)+"\nRole: "+(role==null?"user":role)+" • "+(blocked?"DIBLOKIR":"AKTIF"),15));
                LinearLayout actions=new LinearLayout(this);
                Button block=btn(blocked?"🔓 Buka Blokir":"🚫 Blokir"); actions.addView(block,new LinearLayout.LayoutParams(0,dp(52),1));
                Button del=btn("🗑 Hapus"); actions.addView(del,new LinearLayout.LayoutParams(0,dp(52),1));
                card.addView(actions);content.addView(card);
                block.setOnClickListener(v->setBlocked(uid,!blocked));
                del.setOnClickListener(v->deleteUserAccount(uid));
            }
        }).addOnFailureListener(e->content.addView(tv("Gagal membaca pengguna: "+e.getMessage(),14)));
    }

    void setBlocked(String uid, boolean blocked){
        db.collection("users").document(uid).update("blocked",blocked,"updatedAt",FieldValue.serverTimestamp())
          .addOnSuccessListener(x->{Toast.makeText(this,blocked?"Pengguna diblokir":"Blokir dibuka",Toast.LENGTH_SHORT).show();adminUsers();})
          .addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());
    }

    void deleteUserAccount(String uid){
        new AlertDialog.Builder(this).setTitle("Hapus pengguna?")
                .setMessage("Profil dan akun login pengguna akan dihapus. Tindakan ini tidak dapat dibatalkan.")
                .setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->{
                    FirebaseFunctions.getInstance().getHttpsCallable("adminDeleteUser").call(java.util.Collections.singletonMap("uid",uid))
                      .addOnSuccessListener(x->{Toast.makeText(this,"Pengguna dihapus",Toast.LENGTH_SHORT).show();adminUsers();})
                      .addOnFailureListener(e->Toast.makeText(this,"Gagal menghapus akun: "+e.getMessage(),Toast.LENGTH_LONG).show());
                }).show();
    }

    void adminAddProduct(){
        base((memberMode?"👥 Member":"🛠 Admin")+" • ➕ Tambah Barang");
        EditText name=field("Nama barang"); EditText price=field("Harga, contoh Rp 3.500.000");
        EditText condition=field("Kondisi: Baru / Bekas / Rusak"); EditText rating=field("Rating, contoh 4.8");
        content.addView(name);content.addView(price);content.addView(condition);content.addView(rating);
        Button save=btn("Simpan Barang"); content.addView(save);
        save.setOnClickListener(v->{
            if(name.length()==0 || price.length()==0){Toast.makeText(this,"Nama dan harga wajib diisi",Toast.LENGTH_LONG).show();return;}
            try{
                if(db==null) db=FirebaseFirestore.getInstance();
                java.util.HashMap<String,Object> data=new java.util.HashMap<>();
                data.put("name",name.getText().toString().trim());
                data.put("price",price.getText().toString().trim());
                data.put("condition",condition.getText().toString().trim());
                data.put("rating",rating.length()==0?"0.0":rating.getText().toString().trim());
                data.put("createdAt",FieldValue.serverTimestamp());
                db.collection("products").add(data).addOnSuccessListener(ref->{
                    Toast.makeText(this,"Barang tersimpan ke Firebase",Toast.LENGTH_LONG).show();
                    adminPanel();
                }).addOnFailureListener(e->Toast.makeText(this,"Gagal menyimpan: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }catch(Exception e){Toast.makeText(this,"Firebase belum dikonfigurasi: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        });
    }

    void adminProducts(){
        base("📦 Kelola Produk");
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("products").orderBy("createdAt",Query.Direction.DESCENDING).get().addOnSuccessListener(snapshot->{
            if(snapshot.isEmpty()){content.addView(tv("Belum ada barang di Firestore.",16));return;}
            for(DocumentSnapshot d:snapshot.getDocuments()){
                String n=d.getString("name"), p=d.getString("price"), c=d.getString("condition"), owner=d.getString("ownerName");
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackgroundResource(R.drawable.card);
                card.addView(tv((n==null?"":n)+"\n"+(p==null?"":p)+" • "+(c==null?"":c)+"\nPemilik: "+(owner==null?"":owner),15));
                Button del=btn("🗑 Hapus Produk");card.addView(del);content.addView(card);
                del.setOnClickListener(v->{
                    d.getReference().delete().addOnSuccessListener(x->{Toast.makeText(this,"Produk dihapus",Toast.LENGTH_SHORT).show();adminProducts();})
                     .addOnFailureListener(e->Toast.makeText(this,"Gagal menghapus: "+e.getMessage(),Toast.LENGTH_LONG).show());
                });
            }
        }).addOnFailureListener(e->content.addView(tv("Gagal membaca database: "+e.getMessage(),14)));
    }

    void aiChat(){
        FirebaseUser currentUser=auth==null?null:auth.getCurrentUser();
        if(currentUser==null){ showLogin(); return; }
        base("🤖 Chat AI MMC PONSEL");
        TextView info=tv("Asisten AI online. Tanyakan tentang HP, service, jual-beli, penggunaan aplikasi, atau hal umum lainnya.",14);
        info.setTextColor(MUTED); content.addView(info);

        LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(messages); content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout composer=new LinearLayout(this); composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText input=field("Tulis pertanyaan ke AI...");
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(55),1));
        Button send=btn("Kirim"); composer.addView(send,new LinearLayout.LayoutParams(dp(90),dp(55))); content.addView(composer);

        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("aiChats").document(currentUser.getUid()).collection("messages")
                .orderBy("createdAt",Query.Direction.ASCENDING)
                .addSnapshotListener((snap,e)->{
                    if(e!=null){ Toast.makeText(this,"Gagal memuat chat AI: "+e.getMessage(),Toast.LENGTH_LONG).show(); return; }
                    messages.removeAllViews();
                    if(snap!=null) for(DocumentSnapshot d:snap.getDocuments()){
                        String role=d.getString("role"), text=d.getString("text");
                        String label="assistant".equals(role)?"🤖 AI":"👤 Anda";
                        TextView m=tv(label+"\n"+(text==null?"":text),15);
                        m.setBackgroundResource(R.drawable.card);
                        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(70)); mp.bottomMargin=dp(6); messages.addView(m,mp);
                    }
                    scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
                });

        send.setOnClickListener(v->{
            String text=input.getText().toString().trim();
            if(text.isEmpty()) return;
            send.setEnabled(false);
            input.setText("");
            java.util.HashMap<String,Object> data=new java.util.HashMap<>();
            data.put("message",text);
            FirebaseFunctions.getInstance().getHttpsCallable("chatWithAI").call(data)
                    .addOnSuccessListener(result->{
                        send.setEnabled(true);
                        scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
                    })
                    .addOnFailureListener(err->{
                        send.setEnabled(true);
                        Toast.makeText(this,"AI gagal merespons: "+err.getMessage(),Toast.LENGTH_LONG).show();
                    });
        });
    }

    void solutionGroup(){
        FirebaseUser currentUser=auth==null?null:auth.getCurrentUser();
        if(currentUser==null){ showLogin(); return; }
        base("🌐 Chat Global MMC PONSEL");
        TextView info=tv("Chat global untuk semua pengguna MMC PONSEL. Semua pengguna yang sudah login dapat membaca dan mengirim pesan secara online.",14);
        info.setTextColor(MUTED); content.addView(info);

        LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(messages);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout composer=new LinearLayout(this); composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText input=field("Tulis solusi atau pertanyaan...");
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(55),1));
        Button send=btn("Kirim"); composer.addView(send,new LinearLayout.LayoutParams(dp(90),dp(55)));
        content.addView(composer);

        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("solutionGroup").document("main").collection("messages")
                .orderBy("createdAt",Query.Direction.ASCENDING)
                .addSnapshotListener((snap,e)->{
                    if(e!=null){
                        Toast.makeText(this,"Gagal memuat grup: "+e.getMessage(),Toast.LENGTH_LONG).show();
                        return;
                    }
                    messages.removeAllViews();
                    if(snap!=null){
                        for(DocumentSnapshot d:snap.getDocuments()){
                            String role=d.getString("role");
                            String name=d.getString("name");
                            String text=d.getString("text");
                            String badge="admin".equals(role)?"🛡 Admin":("member".equals(role)?"👤 Member":"👤 Pengguna");
                            TextView m=tv(badge+" • "+(name==null?"Pengguna":name)+"\n"+(text==null?"":text),15);
                            m.setBackgroundResource(R.drawable.card);
                            LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(65));
                            mp.bottomMargin=dp(6); messages.addView(m,mp);
                        }
                    }
                    scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
                });

        send.setOnClickListener(v->{
            String text=input.getText().toString().trim();
            if(text.isEmpty()) return;
            FirebaseUser u=auth==null?null:auth.getCurrentUser();
            if(u==null) return;
            send.setEnabled(false);
            input.setText("");
            java.util.HashMap<String,Object> msg=new java.util.HashMap<>();
            msg.put("uid",u.getUid());
            msg.put("name",memberMode?"Member Miss":(u.getDisplayName()==null?"Admin":u.getDisplayName()));
            msg.put("role",adminMode?"admin":(memberMode?"member":"user"));
            msg.put("text",text);
            msg.put("createdAt",FieldValue.serverTimestamp());
            db.collection("solutionGroup").document("main").collection("messages").add(msg)
                    .addOnSuccessListener(x->{ send.setEnabled(true); })
                    .addOnFailureListener(e->{ send.setEnabled(true); input.setText(text); Toast.makeText(this,"Pesan gagal dikirim: "+e.getMessage(),Toast.LENGTH_LONG).show(); });
        });
    }

    void adminChats(){
        base("💬 Chat Pengguna");
        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("chats").get().addOnSuccessListener(snapshot->{
            if(snapshot.isEmpty()){content.addView(tv("Belum ada chat pengguna.",16));return;}
            for(DocumentSnapshot d:snapshot.getDocuments()){
                String uid=d.getId(); String name=d.getString("userName"); String last=d.getString("lastMessage");
                Button b=btn("👤 "+(name==null?"Pengguna":name)+"\n"+(last==null?"Belum ada pesan":last));
                b.setTextColor(WHITE); b.setBackgroundResource(R.drawable.card); content.addView(b);
                b.setOnClickListener(v->adminChat(uid,name==null?"Pengguna":name));
            }
        }).addOnFailureListener(e->content.addView(tv("Gagal membaca chat: "+e.getMessage(),14)));
    }

    void privateChat(String otherUid,String otherName){
        FirebaseUser me=auth==null?null:auth.getCurrentUser();
        if(me==null){showLogin();return;}
        if(otherUid==null || otherUid.equals(me.getUid())) return;
        String a=me.getUid(), b=otherUid; String chatId=a.compareTo(b)<0?a+"_"+b:b+"_"+a;
        base("💬 Chat Pribadi • "+otherName);
        LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(messages); content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=new LinearLayout(this); composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText input=field("Tulis pesan pribadi..."); composer.addView(input,new LinearLayout.LayoutParams(0,dp(55),1));
        Button send=btn("Kirim"); composer.addView(send,new LinearLayout.LayoutParams(dp(90),dp(55))); content.addView(composer);
        java.util.HashMap<String,Object> thread=new java.util.HashMap<>(); thread.put("participants",java.util.Arrays.asList(a,b)); thread.put("participantNames",java.util.Arrays.asList(sp.getString("name","Pengguna"),otherName)); thread.put("updatedAt",FieldValue.serverTimestamp());
        db.collection("privateChats").document(chatId).set(thread,com.google.firebase.firestore.SetOptions.merge());
        db.collection("privateChats").document(chatId).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING)
          .addSnapshotListener((snap,e)->{
            if(e!=null)return; messages.removeAllViews();
            if(snap!=null) for(DocumentSnapshot d:snap.getDocuments()){ String from=d.getString("fromUid"), text=d.getString("text"); TextView m=tv((a.equals(from)?"Anda: ":otherName+": ")+(text==null?"":text),15); m.setBackgroundResource(R.drawable.card); messages.addView(m); }
            scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
          });
        send.setOnClickListener(v->{ String text=input.getText().toString().trim(); if(text.isEmpty())return; java.util.HashMap<String,Object> msg=new java.util.HashMap<>(); msg.put("fromUid",a); msg.put("toUid",b); msg.put("text",text); msg.put("createdAt",FieldValue.serverTimestamp()); db.collection("privateChats").document(chatId).collection("messages").add(msg).addOnSuccessListener(x->{db.collection("privateChats").document(chatId).update("lastMessage",text,"updatedAt",FieldValue.serverTimestamp());input.setText("");}); });
    }

    void chatWithAdmin(){
        if(auth==null || auth.getCurrentUser()==null){Toast.makeText(this,"Silakan login terlebih dahulu",Toast.LENGTH_LONG).show();return;}
        adminChat(auth.getCurrentUser().getUid(),sp.getString("name","Pelanggan"));
    }

    void adminChat(String uid,String displayName){
        base("💬 Chat • "+displayName);
        LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(messages); content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=new LinearLayout(this); composer.setOrientation(LinearLayout.HORIZONTAL);
        EditText input=field("Tulis pesan..."); composer.addView(input,new LinearLayout.LayoutParams(0,dp(55),1));
        Button send=btn("Kirim"); composer.addView(send,new LinearLayout.LayoutParams(dp(90),dp(55))); content.addView(composer);
        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("chats").document(uid).set(new java.util.HashMap<String,Object>(){{put("userName",displayName);}},com.google.firebase.firestore.SetOptions.merge());
        db.collection("chats").document(uid).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING)
                .addSnapshotListener((snap,e)->{
                    if(e!=null)return; messages.removeAllViews();
                    if(snap!=null) for(DocumentSnapshot d:snap.getDocuments()){
                        String from=d.getString("from"); String text=d.getString("text");
                        TextView m=tv(("admin".equals(from)?"Admin: ":("member".equals(from)?"Member: ":"Pengguna: "))+(text==null?"":text),15);
                        m.setBackgroundResource(R.drawable.card); messages.addView(m);
                    }
                    scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
                });
        send.setOnClickListener(v->{
            String text=input.getText().toString().trim(); if(text.isEmpty())return;
            java.util.HashMap<String,Object> msg=new java.util.HashMap<>(); msg.put("from",adminMode?"admin":(memberMode?"member":"user")); msg.put("text",text); msg.put("createdAt",FieldValue.serverTimestamp());
            db.collection("chats").document(uid).collection("messages").add(msg).addOnSuccessListener(x->{
                java.util.HashMap<String,Object> chat=new java.util.HashMap<>(); chat.put("userName",displayName); chat.put("lastMessage",text); chat.put("updatedAt",FieldValue.serverTimestamp());
                db.collection("chats").document(uid).set(chat,com.google.firebase.firestore.SetOptions.merge()); input.setText("");
            });
        });
    }

    /** Latar belakang animasi ringan: partikel bergerak + garis cahaya MMC PONSEL. */
    static class AnimatedBackgroundView extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final java.util.Random random=new java.util.Random(82);
        final float[] x=new float[34], y=new float[34], vx=new float[34], vy=new float[34], r=new float[34];
        AnimatedBackgroundView(android.content.Context c){
            super(c);
            for(int i=0;i<x.length;i++){x[i]=random.nextFloat();y[i]=random.nextFloat();vx[i]=(random.nextFloat()-.5f)*.0007f;vy[i]=.0004f+random.nextFloat()*.0012f;r[i]=1+random.nextFloat()*4;}
            setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight();
            c.drawColor(Color.rgb(2,7,12));
            long now=System.currentTimeMillis();
            float pulse=(float)(0.5+0.5*Math.sin(now/320.0));

            paint.setTextSize(Math.max(11,getResources().getDisplayMetrics().density*11));
            for(int i=0;i<x.length;i++){
                y[i]+=vy[i]*33f;
                if(y[i]>1.1f){y[i]=-0.1f;x[i]=random.nextFloat();}
                paint.setColor(Color.argb(35+((i*7)%35),70,255,170));
                c.drawText(i%3==0?"01":(i%3==1?"M>":"8F"),x[i]*w,y[i]*h,paint);
            }

            // Stylized anime hacker silhouette: hood, hair, glowing eyes and shoulders.
            float cx=w*.50f, cy=h*.34f, scale=Math.min(w,h)/430f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(150,7,15,24));
            Path hood=new Path();
            hood.moveTo(cx-150*scale,cy+125*scale);
            hood.quadTo(cx-138*scale,cy-92*scale,cx,cy-145*scale);
            hood.quadTo(cx+138*scale,cy-92*scale,cx+150*scale,cy+125*scale);
            hood.quadTo(cx+80*scale,cy+80*scale,cx,cy+105*scale);
            hood.quadTo(cx-80*scale,cy+80*scale,cx-150*scale,cy+125*scale);
            c.drawPath(hood,paint);

            paint.setColor(Color.argb(215,18,28,40));
            c.drawOval(cx-78*scale,cy-82*scale,cx+78*scale,cy+88*scale,paint);
            paint.setColor(Color.argb(190,1,5,9));
            c.drawRect(cx-78*scale,cy-2*scale,cx+78*scale,cy+40*scale,paint);

            paint.setColor(Color.argb((int)(180+55*pulse),255,212,0));
            c.drawOval(cx-54*scale,cy+4*scale,cx-18*scale,cy+15*scale,paint);
            c.drawOval(cx+18*scale,cy+4*scale,cx+54*scale,cy+15*scale,paint);
            paint.setColor(Color.argb((int)(28+40*pulse),255,212,0));
            c.drawCircle(cx,cy+10*scale,95*scale+18*scale*pulse,paint);
            paint.setColor(Color.argb(70,255,212,0));
            c.drawCircle(cx-36*scale,cy+9*scale,18*scale,paint);
            c.drawCircle(cx+36*scale,cy+9*scale,18*scale,paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2,2.2f*scale));
            paint.setColor(Color.argb(85,0,255,180));
            c.drawLine(cx-125*scale,cy+122*scale,cx-185*scale,cy+190*scale,paint);
            c.drawLine(cx+125*scale,cy+122*scale,cx+185*scale,cy+190*scale,paint);
            c.drawLine(cx-185*scale,cy+190*scale,cx-235*scale,cy+205*scale,paint);
            c.drawLine(cx+185*scale,cy+190*scale,cx+235*scale,cy+205*scale,paint);

            float shift=(now%9000L)/9000f*(w+h);
            paint.setStrokeWidth(1.5f);
            paint.setColor(Color.argb(28,255,212,0));
            c.drawLine(shift-h,0,shift,h,paint);
            c.drawLine(shift-h*.55f,h,shift+h*.45f,0,paint);
            paint.setStyle(Paint.Style.FILL);
            postInvalidateDelayed(33);
        }
    }


    // Sound original bergaya Brasil diputar otomatis di halaman login dan area aplikasi.
    void startAmbientSound(){
        try{
            if(ambientPlayer!=null && ambientPlayer.isPlaying()) return;
            if(ambientPlayer!=null){ ambientPlayer.release(); ambientPlayer=null; }
            ambientPlayer=MediaPlayer.create(this,R.raw.mmc_brazil_ambient);
            if(ambientPlayer!=null){
                ambientPlayer.setLooping(true);
                ambientPlayer.setVolume(0.32f,0.32f);
                ambientPlayer.start();
            }
        }catch(Exception ignored){}
    }

    @Override protected void onDestroy(){
        if(ambientPlayer!=null){
            try{ambientPlayer.stop();}catch(Exception ignored){}
            ambientPlayer.release(); ambientPlayer=null;
        }
        if(authExecutor!=null) authExecutor.shutdownNow();
        super.onDestroy();
    }

    void openWhatsApp(){try{String n="6283830655780";Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+n));startActivity(i);}catch(Exception e){Toast.makeText(this,"WhatsApp tidak tersedia",0).show();}}
}
