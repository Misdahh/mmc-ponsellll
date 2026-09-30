package com.mmcponsel.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialException;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
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
    final Executor authExecutor = Executors.newSingleThreadExecutor();
    static final int REQ_PROFILE_PHOTO = 1207;
    static final int REQ_PRODUCT_IMAGE = 1208;
    Uri selectedProductImage=null;

    int currentAccent=YELLOW, currentAccent2=Color.rgb(255,235,120), currentBg=BG;
    final java.util.ArrayList<String> pageHistory=new java.util.ArrayList<>();
    String currentPage="";
    boolean suppressHistory=false;
    int currentParticle=Color.rgb(255,212,0), currentLine=Color.rgb(255,212,0), currentPattern=0;

    static class PageTheme {
        int accent, accent2, bg, particle, line, pattern;
        PageTheme(int a,int a2,int b,int p,int l,int pat){accent=a;accent2=a2;bg=b;particle=p;line=l;pattern=pat;}
    }

    PageTheme themeFor(String heading){
        String h=heading==null?"":heading.toLowerCase();
        if(h.contains("checkout")||h.contains("pembayaran")) return new PageTheme(Color.rgb(0,210,160),Color.rgb(130,255,220),Color.rgb(3,18,18),Color.rgb(0,235,175),Color.rgb(0,180,150),3);
        if(h.contains("service")) return new PageTheme(Color.rgb(65,160,255),Color.rgb(145,205,255),Color.rgb(4,13,25),Color.rgb(70,170,255),Color.rgb(50,120,220),2);
        if(h.contains("profil")||h.contains("developer")) return new PageTheme(Color.rgb(190,105,255),Color.rgb(225,180,255),Color.rgb(14,7,24),Color.rgb(205,120,255),Color.rgb(140,80,210),4);
        if(h.contains("transaksi")||h.contains("pesanan")) return new PageTheme(Color.rgb(255,135,55),Color.rgb(255,205,130),Color.rgb(24,10,5),Color.rgb(255,145,65),Color.rgb(205,95,35),5);
        if(h.contains("kategori")||h.contains("barang")||h.contains("produk")) return new PageTheme(Color.rgb(255,90,145),Color.rgb(255,175,205),Color.rgb(24,6,15),Color.rgb(255,100,155),Color.rgb(205,55,105),1);
        if(h.contains("admin")) return new PageTheme(Color.rgb(255,70,85),Color.rgb(255,160,165),Color.rgb(25,6,9),Color.rgb(255,75,90),Color.rgb(205,45,60),6);
        if(h.contains("member")||h.contains("grup")) return new PageTheme(Color.rgb(65,220,125),Color.rgb(150,255,190),Color.rgb(4,20,12),Color.rgb(70,225,130),Color.rgb(40,165,95),7);
        if(h.contains("chat")) return new PageTheme(Color.rgb(80,175,255),Color.rgb(160,220,255),Color.rgb(4,12,24),Color.rgb(80,180,255),Color.rgb(50,120,205),8);
        if(h.contains("login")) return new PageTheme(YELLOW,Color.rgb(255,240,145),BG,YELLOW,Color.rgb(220,180,0),0);
        return new PageTheme(YELLOW,Color.rgb(255,240,145),BG,YELLOW,Color.rgb(220,180,0),0);
    }

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(WHITE);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
    Button btn(String s){
        Button b=new Button(this);
        b.setText(s); b.setTextColor(Color.BLACK); b.setTextSize(14); b.setAllCaps(false);
        android.graphics.drawable.GradientDrawable gd=new android.graphics.drawable.GradientDrawable();
        gd.setColor(currentAccent); gd.setCornerRadius(dp(16));
        b.setBackground(gd); b.setPadding(dp(12),dp(4),dp(12),dp(4));
        return b;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        sp=getSharedPreferences("mmc",0);
        seed();
        callbackManager=CallbackManager.Factory.create();
        showSplash();
    }

    void seed(){ /* Katalog tidak lagi ditanam di APK; semua barang berasal dari Firestore. */ }

    void showSplash(){
        final LinearLayout splash=new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(BG);

        TextView logo=tv("MMC",52);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        logo.setTextColor(YELLOW);
        TextView brand=tv("PONSEL",28);
        brand.setGravity(Gravity.CENTER);
        brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        TextView sub=tv("Jual • Beli • Service HP",15);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(MUTED);

        ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(190),dp(6));
        pp.topMargin=dp(28);
        splash.addView(logo,new LinearLayout.LayoutParams(-1,dp(70)));
        splash.addView(brand,new LinearLayout.LayoutParams(-1,dp(50)));
        splash.addView(sub,new LinearLayout.LayoutParams(-1,dp(40)));
        splash.addView(progress,pp);
        setContentView(splash);

        Animation fade=new AlphaAnimation(0f,1f);
        fade.setDuration(750);
        fade.setInterpolator(new AccelerateDecelerateInterpolator());
        logo.startAnimation(fade);

        ScaleAnimation scale=new ScaleAnimation(0.82f,1f,0.82f,1f,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        scale.setDuration(850);
        scale.setInterpolator(new AccelerateDecelerateInterpolator());
        brand.startAnimation(scale);

        new android.os.Handler().postDelayed(() -> {
            try {
                auth=FirebaseAuth.getInstance();
                db=FirebaseFirestore.getInstance();
                credentialManager=CredentialManager.create(this);
                if(auth.getCurrentUser()!=null) home(); else showLogin();
            } catch(Exception e) {
                showLogin();
            }
        },1800);
    }

    void base(String heading){
        PageTheme pt=themeFor(heading);
        currentAccent=pt.accent; currentAccent2=pt.accent2; currentBg=pt.bg;
        currentParticle=pt.particle; currentLine=pt.line; currentPattern=pt.pattern;

        root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(currentBg);

        AnimatedBackgroundView animated=new AnimatedBackgroundView(this,currentBg,currentParticle,currentLine,currentPattern);
        root.addView(animated,new android.widget.FrameLayout.LayoutParams(-1,-1));

        mainColumn=new LinearLayout(this);
        mainColumn.setOrientation(LinearLayout.VERTICAL);
        mainColumn.setPadding(dp(10),dp(8),dp(10),0);
        root.addView(mainColumn,new android.widget.FrameLayout.LayoutParams(-1,-1));

        if(!suppressHistory){
            if(currentPage==null || !heading.equals(currentPage)){
                if(currentPage!=null && !currentPage.isEmpty()) pageHistory.add(currentPage);
                currentPage=heading;
            }
        }
        suppressHistory=false;

        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0,0,0,dp(2));
        boolean showBack=!heading.toLowerCase().contains("mmc ponsel") && !heading.toLowerCase().contains("login") && !heading.equals("MMC PONSEL");
        if(showBack){
            Button backBtn=new Button(this);
            backBtn.setText("‹"); backBtn.setTextSize(34); backBtn.setTextColor(WHITE); backBtn.setAllCaps(false);
            android.graphics.drawable.GradientDrawable bd=new android.graphics.drawable.GradientDrawable();
            bd.setColor(Color.argb(45,255,255,255)); bd.setCornerRadius(dp(18)); backBtn.setBackground(bd);
            header.addView(backBtn,new LinearLayout.LayoutParams(dp(54),dp(52)));
            backBtn.setOnClickListener(v->goBack());
        }
        title=tv(heading,22); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setTextColor(currentAccent);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(55),1));
        mainColumn.addView(header,new LinearLayout.LayoutParams(-1,dp(58)));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,0,0,dp(10));
        android.view.animation.AlphaAnimation itemFade=new android.view.animation.AlphaAnimation(0f,1f);
        itemFade.setDuration(420);
        itemFade.setInterpolator(new AccelerateDecelerateInterpolator());
        LayoutAnimationController lac=new LayoutAnimationController(itemFade,0.07f);
        lac.setOrder(LayoutAnimationController.ORDER_NORMAL);
        content.setLayoutAnimation(lac);
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.addView(content);
        mainColumn.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        title.setAlpha(0f); title.setTranslationY(dp(-12));
        title.animate().alpha(1f).translationY(0f).setDuration(520).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    void goBack(){
        if(pageHistory.isEmpty()){
            if(auth!=null && auth.getCurrentUser()!=null) { suppressHistory=true; home(); } else { suppressHistory=true; showLogin(); }
            return;
        }
        String target=pageHistory.remove(pageHistory.size()-1);
        suppressHistory=true;
        if(target.contains("Kategori")) category();
        else if(target.contains("Service HP — Online")) service();
        else if(target.contains("Pesan Service")) service();
        else if(target.contains("Detail Produk")) home();
        else if(target.contains("Checkout")) home();
        else if(target.contains("Jual HP")) sell();
        else if(target.contains("Transaksi")) transactions();
        else if(target.contains("Profil")) profile();
        else if(target.contains("Developer")) developerPage();
        else if(target.contains("Chat")) chatWithAdmin();
        else if(target.contains("Admin")) adminPanel();
        else if(target.contains("Member")) memberPanel();
        else if(target.contains("Grup Solusi")) solutionGroup();
        else if(target.contains("Login Admin")) showAdminLogin();
        else if(target.contains("Login Member")) showMemberLogin();
        else if(target.equals("MMC PONSEL")) home();
        else showLogin();
    }

    @Override public void onBackPressed(){
        if(!pageHistory.isEmpty()){ goBack(); return; }
        if(auth!=null && auth.getCurrentUser()!=null){ home(); return; }
        super.onBackPressed();
    }

    void showLogin(){
        base("Masuk ke MMC PONSEL");

        ImageView hero=new ImageView(this);
        hero.setImageResource(R.drawable.login_hero_mmc);
        hero.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable heroBg=new android.graphics.drawable.GradientDrawable();
        heroBg.setColor(Color.rgb(7,16,25)); heroBg.setCornerRadius(dp(24));
        heroBg.setStroke(dp(1),Color.rgb(65,82,95)); hero.setBackground(heroBg);
        hero.setClipToOutline(true);
        content.addView(hero,new LinearLayout.LayoutParams(-1,dp(190)));

        TextView welcome=tv("Selamat datang kembali",24);
        welcome.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        welcome.setTextColor(WHITE);
        welcome.setPadding(dp(10),dp(16),dp(10),dp(2));
        content.addView(welcome);

        TextView sub=tv("Masuk untuk belanja HP, pesan service, dan melihat transaksi kamu.",14);
        sub.setTextColor(MUTED);
        sub.setPadding(dp(10),0,dp(10),dp(12));
        content.addView(sub);

        LinearLayout authCard=new LinearLayout(this);
        authCard.setOrientation(LinearLayout.VERTICAL);
        authCard.setPadding(dp(12),dp(10),dp(12),dp(10));
        android.graphics.drawable.GradientDrawable authBg=new android.graphics.drawable.GradientDrawable();
        authBg.setColor(Color.argb(225,11,21,31)); authBg.setCornerRadius(dp(22));
        authBg.setStroke(dp(1),Color.rgb(48,64,76)); authCard.setBackground(authBg);
        TextView quick=tv("PILIH CARA MASUK",12); quick.setTextColor(YELLOW); quick.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        authCard.addView(quick);

        Button google=btn("Masuk dengan Google");
        google.setTextColor(Color.rgb(20,25,30));
        android.graphics.drawable.GradientDrawable googleBg=new android.graphics.drawable.GradientDrawable();
        googleBg.setColor(Color.WHITE); googleBg.setCornerRadius(dp(16)); google.setBackground(googleBg);
        authCard.addView(google,new LinearLayout.LayoutParams(-1,dp(52)));

        Button facebook=btn("Masuk dengan Facebook");
        facebook.setTextColor(WHITE);
        android.graphics.drawable.GradientDrawable fbBg=new android.graphics.drawable.GradientDrawable();
        fbBg.setColor(Color.rgb(34,100,220)); fbBg.setCornerRadius(dp(16)); facebook.setBackground(fbBg);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(52)); fp.topMargin=dp(10); authCard.addView(facebook,fp);

        TextView secure=tv("🔒 Login aman melalui Firebase Authentication",12);
        secure.setTextColor(MUTED); secure.setGravity(Gravity.CENTER);
        authCard.addView(secure,new LinearLayout.LayoutParams(-1,dp(38)));
        content.addView(authCard,new LinearLayout.LayoutParams(-1,-2));

        TextView or=tv("atau",13); or.setTextColor(MUTED); or.setGravity(Gravity.CENTER);
        content.addView(or,new LinearLayout.LayoutParams(-1,dp(42)));

        Button admin=btn("Login Admin"); admin.setTextColor(WHITE); admin.setBackgroundResource(R.drawable.card);
        content.addView(admin,new LinearLayout.LayoutParams(-1,dp(50)));
        admin.setOnClickListener(v->showAdminLogin());

        Button member=btn("Login Member"); member.setTextColor(WHITE); member.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(50)); mp.topMargin=dp(10); content.addView(member,mp);
        member.setOnClickListener(v->showMemberLogin());

        TextView note=tv("Belum punya akun? Daftar atau masuk dengan Google untuk memulai.\nData password tidak disimpan di aplikasi.",12);
        note.setTextColor(MUTED); note.setGravity(Gravity.CENTER); note.setPadding(dp(8),dp(14),dp(8),dp(18));
        content.addView(note);

        google.setOnClickListener(v->signInWithGoogle());
        facebook.setOnClickListener(v->signInWithFacebook());
    }

    void showAdminLogin(){
        base("🔐 Login Admin MMC PONSEL");
        content.addView(tv("Panel khusus pengelola toko",18));
        EditText user=field("Username admin");
        EditText pass=field("Password admin");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(user); content.addView(pass);
        Button login=btn("Masuk Admin"); content.addView(login);
        Button back=btn("Kembali ke Login Pengguna"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);
        login.setOnClickListener(v->{
            String u=user.getText().toString().trim();
            String pw=pass.getText().toString();
            if(!"miss".equalsIgnoreCase(u) || pw.length()<6){
                Toast.makeText(this,"Username harus miss dan password minimal 6 karakter",Toast.LENGTH_LONG).show();
                return;
            }
            try{
                if(!firebaseReady()) return;
                String adminEmail="miss@mmcponsel.app";
                auth.signInWithEmailAndPassword(adminEmail,pw).addOnCompleteListener(this,task->{
                    if(task.isSuccessful()){
                        adminMode=true;
                        Toast.makeText(this,"Login admin berhasil",Toast.LENGTH_SHORT).show();
                        adminPanel();
                    } else {
                        Toast.makeText(this,"Login admin gagal. Pastikan akun miss@mmcponsel.app sudah dibuat di Firebase Authentication dan Email/Password aktif.",Toast.LENGTH_LONG).show();
                    }
                });
            }catch(Exception e){Toast.makeText(this,"Firebase belum dikonfigurasi: "+e.getMessage(),Toast.LENGTH_LONG).show();}
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
            credentialManager.getCredentialAsync(request,new CancellationSignal(),authExecutor,
                    new CredentialManagerCallback<Credential, GetCredentialException>() {
                        @Override public void onResult(@NonNull Credential credential){
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
            try {
                GoogleIdTokenCredential googleCredential=GoogleIdTokenCredential.createFrom(((CustomCredential)credential).getData());
                firebaseAuthWithGoogle(googleCredential.getIdToken());
            } catch(GoogleIdTokenParsingException e){
                Toast.makeText(this,"Token Google tidak valid.",Toast.LENGTH_LONG).show();
            }
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
        sp.edit().putString("user",user.getEmail()==null?user.getUid():user.getEmail())
                .putString("name",user.getDisplayName()==null?"Pelanggan":user.getDisplayName())
                .putString("uid",user.getUid()).apply();
        Toast.makeText(this,"Login berhasil",Toast.LENGTH_SHORT).show();
        home();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(callbackManager!=null) callbackManager.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_PRODUCT_IMAGE && resultCode==RESULT_OK && data!=null && data.getData()!=null){ selectedProductImage=data.getData(); try{getContentResolver().takePersistableUriPermission(selectedProductImage,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} Toast.makeText(this,"Foto produk dipilih",Toast.LENGTH_SHORT).show(); }
        if(requestCode==REQ_PROFILE_PHOTO && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            Uri uri=data.getData();
            try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
            sp.edit().putString("profile_photo_uri",uri.toString()).apply();
            Toast.makeText(this,"Foto profil berhasil diganti",Toast.LENGTH_SHORT).show();
            profile();
        }
    }

    // ===== Store UI =====
    LinearLayout panel(){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14),dp(14),dp(14),dp(14)); c.setBackgroundResource(R.drawable.card);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(6),0,dp(6));
        c.setLayoutParams(lp); return c;
    }
    TextView section(String text){
        TextView t=tv(text,19); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); t.setTextColor(WHITE);
        t.setPadding(dp(4),dp(14),dp(4),dp(8)); return t;
    }
    Button chip(String text, boolean selected){
        Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(12); b.setTextColor(selected?Color.BLACK:WHITE);
        b.setPadding(dp(10),0,dp(10),0); b.setMinHeight(dp(40));
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(selected?currentAccent:Color.argb(70,255,255,255)); g.setCornerRadius(dp(20)); b.setBackground(g); return b;
    }
    TextView badge(String text){
        TextView t=tv(text,11); t.setTextColor(Color.BLACK); t.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable(); g.setColor(currentAccent); g.setCornerRadius(dp(12)); t.setBackground(g);
        t.setPadding(dp(8),dp(3),dp(8),dp(3)); return t;
    }
    TextView imagePlaceholder(String emoji, String label){
        TextView t=tv(emoji+"\n"+label,30); t.setGravity(Gravity.CENTER); t.setTextColor(WHITE);
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(Color.argb(95,255,255,255)); g.setCornerRadius(dp(14)); t.setBackground(g);
        t.setPadding(dp(8),dp(8),dp(8),dp(8)); return t;
    }
    void home(){
        base("MMC PONSEL");
        LinearLayout top=panel();
        LinearLayout brand=new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=tv("📱",34); mark.setGravity(Gravity.CENTER); brand.addView(mark,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        TextView n=tv("MMC PONSEL",25); n.setTypeface(null,Typeface.BOLD); n.setTextColor(YELLOW); names.addView(n);
        TextView s=tv("Jual Beli HP & Service",13); s.setTextColor(MUTED); names.addView(s); brand.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView bell=tv("🔔",24); bell.setGravity(Gravity.CENTER); brand.addView(bell,new LinearLayout.LayoutParams(dp(52),dp(52)));
        top.addView(brand);
        TextView hello=tv("Halo, "+sp.getString("name","Pelanggan")+" 👋",17); hello.setTextColor(WHITE); top.addView(hello);
        content.addView(top);

        EditText search=field("🔍  Cari HP, merek, atau layanan...");
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(54)); slp.setMargins(0,dp(8),0,dp(4)); content.addView(search,slp);

        LinearLayout hero=panel();
        TextView ht=tv("HP BARU • BEKAS • RUSAK",22); ht.setTypeface(null,Typeface.BOLD); ht.setTextColor(YELLOW); hero.addView(ht);
        TextView hs=tv("JUAL BELI & SERVICE\nHarga transparan • Proses mudah • Dukungan Miss Cell",14); hs.setTextColor(WHITE); hero.addView(hs);
        Button buyNow=btn("Belanja HP Sekarang →"); hero.addView(buyNow); buyNow.setOnClickListener(v->category());
        content.addView(hero);

        content.addView(section("Layanan Utama"));
        gridMenus();
        content.addView(section("Produk Terbaru"));
        productList=new LinearLayout(this); productList.setOrientation(LinearLayout.VERTICAL); content.addView(productList);
        loadCloudProducts(search);
        bottom();
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
                            if(n==null) continue;
                            if(q.isEmpty() || n.toLowerCase().contains(q) || (condition!=null && condition.toLowerCase().contains(q))){
                                String imageUrl=d.getString("imageUrl");
                                String[] row=new String[]{n,price==null?"":price,condition==null?"":condition,rating==null?"0.0":rating,imageUrl==null?"":imageUrl,d.getId()};
                                products.add(row); productCard(row, productList);
                            }
                        }
                        if(products.isEmpty()) productList.addView(tv("Produk tidak ditemukan.",15));
                    });
        }catch(Exception e){content.addView(tv("Firebase belum dikonfigurasi: "+e.getMessage(),14));}
    }
    EditText field(String h){EditText e=new EditText(this);e.setHint(h);e.setTextColor(WHITE);e.setHintTextColor(MUTED);e.setBackgroundResource(R.drawable.edit);return e;}
    void gridMenus(){
        String[][] m={{"📱 HP Baru","Garansi resmi"},{"♻ HP Bekas","Layak & hemat"},{"🛠 HP Rusak","Untuk sparepart"},{"🔧 Service","Miss Cell"},{"🛒 Jual HP","Jual perangkat"},{"🛍 Beli HP","Belanja online"}};
        LinearLayout row=null; int i=0;
        for(String[] x:m){
            if(i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);content.addView(row);}
            Button b=btn(x[0]+"\n"+x[1]);b.setTextColor(WHITE);b.setTextSize(13);b.setBackgroundResource(R.drawable.card);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(88),1);lp.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(b,lp);
            if(x[0].contains("Service"))b.setOnClickListener(v->service());
            else if(x[0].contains("Beli"))b.setOnClickListener(v->category());
            else if(x[0].contains("Jual"))b.setOnClickListener(v->sell());
            else b.setOnClickListener(v->category());
            i++;
        }
    }
    void productCard(String[] p){productCard(p,content);}
    void productCard(String[] p, LinearLayout target){
        LinearLayout c=panel(); c.setOrientation(LinearLayout.HORIZONTAL);
        ImageView pic=new ImageView(this); pic.setScaleType(ImageView.ScaleType.CENTER_CROP); pic.setBackgroundResource(R.drawable.card);
        if(p.length>4 && p[4]!=null && !p[4].isEmpty()){ com.bumptech.glide.Glide.with(this).load(p[4]).into(pic); } else { pic.setImageResource(android.R.drawable.ic_menu_gallery); }
        c.addView(pic,new LinearLayout.LayoutParams(dp(92),dp(116)));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setPadding(dp(10),0,0,0);
        TextView n=tv(p[0],16); n.setTypeface(null,Typeface.BOLD); info.addView(n);
        TextView cond=badge((p[2]==null||p[2].isEmpty())?"Normal":p[2]); LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-2,dp(28)); blp.setMargins(0,dp(4),0,dp(4)); info.addView(cond,blp);
        TextView price=tv(p[1],19); price.setTypeface(null,Typeface.BOLD); price.setTextColor(YELLOW); info.addView(price);
        TextView rating=tv("⭐ "+p[3]+"  •  Siap diproses",12); rating.setTextColor(MUTED); info.addView(rating);
        Button b=btn("Lihat Detail"); info.addView(b,new LinearLayout.LayoutParams(-1,dp(42))); b.setOnClickListener(v->detail(p));
        c.addView(info,new LinearLayout.LayoutParams(0,-2,1)); target.addView(c);
    }
    void detail(String[] p){
        base("Detail Produk");content.addView(tv(p[0],23));content.addView(tv(p[1]+"\nKondisi: "+p[2]+"\nRating: ⭐ "+p[3]+"\nData produk diambil dari server MMC PONSEL.",16));
        if(p.length>4 && p[4]!=null && !p[4].isEmpty()){ ImageView iv=new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP); com.bumptech.glide.Glide.with(this).load(p[4]).into(iv); content.addView(iv,new LinearLayout.LayoutParams(-1,dp(240))); }
        Button cart=btn("Tambah ke Keranjang Online");content.addView(cart);Button buy=btn("Beli Sekarang");content.addView(buy); Button reviews=btn("⭐ Rating & Ulasan"); content.addView(reviews); reviews.setOnClickListener(v->reviews(p));
        cart.setOnClickListener(v->{
            FirebaseUser u=auth==null?null:auth.getCurrentUser(); if(u==null){showLogin();return;}
            java.util.HashMap<String,Object> data=new java.util.HashMap<>(); data.put("productName",p[0]);data.put("price",p[1]);data.put("condition",p[2]);data.put("createdAt",FieldValue.serverTimestamp());
            db.collection("users").document(u.getUid()).collection("cart").add(data).addOnSuccessListener(x->Toast.makeText(this,"Keranjang tersimpan online",Toast.LENGTH_SHORT).show()).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());
        });
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

    void addWelcomeCard(){
        String name=sp.getString("name","").trim();
        if(name.isEmpty() && auth!=null && auth.getCurrentUser()!=null && auth.getCurrentUser().getDisplayName()!=null) name=auth.getCurrentUser().getDisplayName().trim();
        if(name.isEmpty()) name="Sahabat MMC PONSEL";
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14),dp(14),dp(14),dp(14));
        card.setBackgroundResource(R.drawable.card);
        TextView h=tv("💛 SELAMAT DATANG DI MMC PONSEL",19);
        h.setTypeface(null,Typeface.BOLD);
        h.setTextColor(YELLOW);
        card.addView(h);
        TextView msg=tv("Halo, "+name+"!\n\nTerima kasih sudah berkunjung dan mempercayai MMC PONSEL. Kami senang dapat menemani Anda dalam jual beli HP, service, dan kebutuhan perangkat Anda. Semoga pengalaman Anda di aplikasi ini nyaman, mudah, dan menyenangkan.\n\nSalam hangat,\nMiss Cell — Developer & Pencipta MMC PONSEL 💛",15);
        msg.setTextColor(WHITE);
        card.addView(msg);
        content.addView(card);
        card.setAlpha(0f);
        card.setTranslationY(dp(12));
        card.animate().alpha(1f).translationY(0f).setDuration(650).setStartDelay(120).start();
    }

    void addDeveloperCard(){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackgroundResource(R.drawable.card);
        TextView h=tv("👨‍💻 DEVELOPER / PENCIPTA",18);h.setTypeface(null,Typeface.BOLD);h.setTextColor(YELLOW);card.addView(h);
        ImageView photo=new ImageView(this);
        photo.setImageResource(R.drawable.developer_miss_cell);
        photo.setContentDescription("Foto Developer Miss Cell");
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams photoLp=new LinearLayout.LayoutParams(-1,dp(230));
        photoLp.setMargins(0,dp(8),0,dp(8));
        card.addView(photo,photoLp);
        TextView caption=tv("Miss Cell — Developer & Pencipta MMC PONSEL",17);caption.setTypeface(null,Typeface.BOLD);caption.setTextColor(YELLOW);card.addView(caption);
        card.addView(tv("Terima kasih sudah menggunakan MMC PONSEL.\n\nWhatsApp: 083830655780\nFacebook: misdah\nTikTok: @tegaris82\nYouTube: @tegaris82",15));
        Button wa=btn("WhatsApp Developer");wa.setTextColor(WHITE);wa.setBackgroundResource(R.drawable.card);card.addView(wa);wa.setOnClickListener(v->openDeveloperWhatsApp());
        LinearLayout socials=new LinearLayout(this);socials.setOrientation(LinearLayout.HORIZONTAL);
        Button fb=btn("Facebook");fb.setTextColor(WHITE);fb.setBackgroundResource(R.drawable.card);socials.addView(fb,new LinearLayout.LayoutParams(0,dp(50),1));fb.setOnClickListener(v->openUrl("https://facebook.com/misdah"));
        Button tk=btn("TikTok");tk.setTextColor(WHITE);tk.setBackgroundResource(R.drawable.card);socials.addView(tk,new LinearLayout.LayoutParams(0,dp(50),1));tk.setOnClickListener(v->openUrl("https://www.tiktok.com/@tegaris82"));
        Button yt=btn("YouTube");yt.setTextColor(WHITE);yt.setBackgroundResource(R.drawable.card);socials.addView(yt,new LinearLayout.LayoutParams(0,dp(50),1));yt.setOnClickListener(v->openUrl("https://www.youtube.com/@tegaris82"));
        card.addView(socials);content.addView(card);
    }

    void openDeveloperWhatsApp(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/6283830655780")));}catch(Exception e){Toast.makeText(this,"WhatsApp tidak tersedia",0).show();}}
    void openUrl(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Link tidak dapat dibuka",0).show();}}
    void category(){
        base("Kategori HP");
        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL);
        for(String x:new String[]{"Semua","HP Baru","HP Bekas","HP Rusak"}){Button b=chip(x,x.equals("Semua"));chips.addView(b,new LinearLayout.LayoutParams(0,dp(44),1));b.setOnClickListener(v->Toast.makeText(this,"Filter: "+x,Toast.LENGTH_SHORT).show());}
        content.addView(chips);
        content.addView(section("Pilih Merek"));
        LinearLayout brands=new LinearLayout(this); brands.setOrientation(LinearLayout.HORIZONTAL);
        for(String x:new String[]{" Apple","Samsung","Xiaomi","OPPO","vivo"}){Button b=chip(x,false);brands.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}
        content.addView(brands); content.addView(section("Katalog HP")); loadCategoryFromServer();
    }
        void service(){
        base("Service HP — Miss Cell");
        TextView intro=tv("Service profesional • Teknisi • Estimasi jelas",16);intro.setTextColor(MUTED);content.addView(intro);
        for(String s:new String[]{"Layar Pecah / Touchscreen","Ganti Baterai","Tidak Bisa Dinyalakan","Kamera","Water Damage","Software / Unlock","Lainnya"}){
            LinearLayout c=panel(); c.setOrientation(LinearLayout.HORIZONTAL); TextView icon=imagePlaceholder("🔧",""); c.addView(icon,new LinearLayout.LayoutParams(dp(64),dp(64)));
            LinearLayout inf=new LinearLayout(this);inf.setOrientation(LinearLayout.VERTICAL);inf.setPadding(dp(10),0,0,0);inf.addView(tv(s,16));inf.addView(tv("Klik untuk pesan service",12));c.addView(inf,new LinearLayout.LayoutParams(0,-2,1));
            c.setOnClickListener(v->serviceForm(s)); content.addView(c);
        }
    }
        void serviceForm(String type){base("Pesan Service Online");content.addView(tv("Jenis: "+type,20));EditText note=field("Keluhan / catatan");content.addView(note);EditText phone=field("Nomor HP");content.addView(phone);Button b=btn("Kirim Permintaan ke Server");content.addView(b);b.setOnClickListener(v->{FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}if(phone.length()==0){Toast.makeText(this,"Masukkan nomor HP",0).show();return;}java.util.HashMap<String,Object> data=new java.util.HashMap<>();data.put("uid",u.getUid());data.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());data.put("email",u.getEmail());data.put("type",type);data.put("note",note.getText().toString().trim());data.put("phone",phone.getText().toString().trim());data.put("status","Menunggu Diproses");data.put("createdAt",FieldValue.serverTimestamp());db.collection("serviceRequests").add(data).addOnSuccessListener(x->{Toast.makeText(this,"Permintaan service terkirim online",Toast.LENGTH_LONG).show();transactions();}).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());});}
    void sell(){
        base("Jual HP Online");
        content.addView(tv("Jual HP kamu dengan mudah ke MMC PONSEL",18));
        for(String[] f:new String[][]{{"Kategori","Pilih kategori"},{"Merek","Pilih merek"},{"Model","Contoh: iPhone 13"},{"Kondisi","Baru / Bekas / Rusak"},{"Harga","Harga yang diinginkan"},{"Nomor kontak","Nomor WhatsApp"}}){content.addView(field(f[1]));}
        Button b=btn("Kirim Penawaran");content.addView(b);b.setOnClickListener(v->Toast.makeText(this,"Isi formulir lengkap untuk mengirim penawaran.",Toast.LENGTH_SHORT).show());
    }
        void bottom(){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(4),dp(4),dp(4),dp(6));
        String[] a={"⌂\nBeranda","🛒\nBeli","🔧\nService","👤\nProfil"};
        for(String s:a){Button b=btn(s);b.setTextColor(s.contains("Beranda")?currentAccent:WHITE);b.setTextSize(12);b.setBackgroundColor(Color.TRANSPARENT);nav.addView(b,new LinearLayout.LayoutParams(0,dp(62),1));
            if(s.contains("Service"))b.setOnClickListener(v->{suppressHistory=true;service();});if(s.contains("Profil"))b.setOnClickListener(v->{suppressHistory=true;profile();});if(s.contains("Beli"))b.setOnClickListener(v->{suppressHistory=true;category();});if(s.contains("Beranda"))b.setOnClickListener(v->{suppressHistory=true;home();});}
        mainColumn.addView(nav);
    }
        void profile(){
        base("Profil");
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u==null){showLogin();return;}

        // Elegant profile hero: cover, avatar, account identity and quick stats.
        android.widget.FrameLayout hero=new android.widget.FrameLayout(this);
        android.graphics.drawable.GradientDrawable heroBg=new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(40,18,65),Color.rgb(20,10,35),Color.rgb(7,14,22)});
        heroBg.setCornerRadius(dp(24)); hero.setBackground(heroBg);
        hero.setPadding(dp(16),dp(18),dp(16),dp(16));
        LinearLayout heroCol=new LinearLayout(this); heroCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout identity=new LinearLayout(this); identity.setGravity(Gravity.CENTER_VERTICAL);
        android.widget.FrameLayout avatarWrap=new android.widget.FrameLayout(this);
        ImageView avatar=new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable avatarBg=new android.graphics.drawable.GradientDrawable();
        avatarBg.setColor(Color.rgb(34,24,48)); avatarBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        avatar.setBackground(avatarBg); avatar.setClipToOutline(true);
        String savedPhoto=sp.getString("profile_photo_uri","");
        boolean photoSet=false;
        if(!savedPhoto.isEmpty()){
            try{avatar.setImageURI(Uri.parse(savedPhoto));photoSet=true;}catch(Exception ignored){}
        }
        if(!photoSet && u.getPhotoUrl()!=null){try{avatar.setImageURI(u.getPhotoUrl());photoSet=true;}catch(Exception ignored){}}
        if(!photoSet){avatar.setImageResource(android.R.drawable.ic_menu_myplaces);avatar.setPadding(dp(18),dp(18),dp(18),dp(18));}
        avatarWrap.addView(avatar,new android.widget.FrameLayout.LayoutParams(dp(82),dp(82)));
        TextView camera=tv("✎",18); camera.setGravity(Gravity.CENTER); camera.setTextColor(Color.BLACK);
        android.graphics.drawable.GradientDrawable cameraBg=new android.graphics.drawable.GradientDrawable(); cameraBg.setColor(currentAccent); cameraBg.setShape(android.graphics.drawable.GradientDrawable.OVAL); camera.setBackground(cameraBg);
        android.widget.FrameLayout.LayoutParams camLp=new android.widget.FrameLayout.LayoutParams(dp(32),dp(32),Gravity.BOTTOM|Gravity.END); camLp.setMargins(0,0,dp(-2),dp(-2)); avatarWrap.addView(camera,camLp);
        camera.setOnClickListener(v->pickProfilePhoto());
        identity.addView(avatarWrap,new LinearLayout.LayoutParams(dp(92),dp(92)));

        LinearLayout idText=new LinearLayout(this); idText.setOrientation(LinearLayout.VERTICAL); idText.setPadding(dp(10),0,0,0);
        TextView name=tv(u.getDisplayName()==null||u.getDisplayName().trim().isEmpty()?"Pelanggan":u.getDisplayName(),22); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); name.setTextColor(WHITE);
        idText.addView(name);
        TextView email=tv(u.getEmail()==null?"":u.getEmail(),13); email.setTextColor(MUTED); idText.addView(email);
        TextView badge=tv("  MEMBER MMC PONSEL  ",11); badge.setTextColor(Color.BLACK); badge.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable badgeBg=new android.graphics.drawable.GradientDrawable(); badgeBg.setColor(currentAccent); badgeBg.setCornerRadius(dp(20)); badge.setBackground(badgeBg);
        LinearLayout.LayoutParams badgeLp=new LinearLayout.LayoutParams(-2,dp(30)); badgeLp.setMargins(0,dp(8),0,0); idText.addView(badge,badgeLp);
        identity.addView(idText,new LinearLayout.LayoutParams(0,-2,1));
        heroCol.addView(identity);

        TextView change=tv("📷  Ganti foto profil",14); change.setTextColor(currentAccent2); change.setGravity(Gravity.CENTER_VERTICAL); change.setPadding(0,dp(14),0,0);
        change.setOnClickListener(v->pickProfilePhoto()); heroCol.addView(change);
        hero.addView(heroCol,new android.widget.FrameLayout.LayoutParams(-1,-2));
        content.addView(hero,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout stats=panel(); stats.setOrientation(LinearLayout.HORIZONTAL); stats.setGravity(Gravity.CENTER);
        addProfileStat(stats,"📦","Pesanan"); addProfileStat(stats,"🔧","Service"); addProfileStat(stats,"⭐","Member");
        content.addView(stats);

        for(String[] item:new String[][]{{"👤","Data Akun"},{"📍","Alamat Pengiriman"},{"💳","Metode Pembayaran"},{"🔔","Notifikasi"},{"📦","Transaksi Saya"},{"💬","Chat Admin"},{"👨‍💻","Tentang MMC PONSEL"}}){
            Button b=btn(item[0]+"   "+item[1]+"   ›"); b.setTextColor(WHITE); b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT); b.setBackgroundResource(R.drawable.card); content.addView(b);
            if(item[1].contains("Transaksi"))b.setOnClickListener(v->transactions());
            else if(item[1].contains("Chat"))b.setOnClickListener(v->chatWithAdmin());
            else if(item[1].contains("Tentang"))b.setOnClickListener(v->developerPage());
            else if(item[1].contains("Data"))b.setOnClickListener(v->saveProfileOnline());
        }
        Button wa=btn("💬  WhatsApp MMC PONSEL"); content.addView(wa); wa.setOnClickListener(v->openWhatsApp());
        Button out=btn("Keluar Akun"); out.setTextColor(Color.WHITE); out.setBackgroundResource(R.drawable.card); content.addView(out); out.setOnClickListener(v->logout());
        bottom();
    }

    void addProfileStat(LinearLayout parent,String icon,String label){
        LinearLayout s=new LinearLayout(this); s.setOrientation(LinearLayout.VERTICAL); s.setGravity(Gravity.CENTER); s.addView(tv(icon,22)); TextView l=tv(label,11); l.setTextColor(MUTED); s.addView(l); parent.addView(s,new LinearLayout.LayoutParams(0,dp(68),1));
    }

    void pickProfilePhoto(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,REQ_PROFILE_PHOTO);
    }

        void saveProfileOnline(){FirebaseUser u=auth.getCurrentUser();if(u==null)return;EditText address=field("Alamat pengiriman");content.addView(address);Button save=btn("Simpan ke Server");content.addView(save);save.setOnClickListener(v->{db.collection("users").document(u.getUid()).set(new java.util.HashMap<String,Object>(){{put("uid",u.getUid());put("name",u.getDisplayName());put("email",u.getEmail());put("address",address.getText().toString().trim());put("updatedAt",FieldValue.serverTimestamp());}},com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener(x->Toast.makeText(this,"Profil tersimpan online",Toast.LENGTH_SHORT).show());});}
    void transactions(){
        base("Transaksi Saya");FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}
        content.addView(section("Pesanan HP"));
        db.collection("orders").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada pesanan.",15));for(DocumentSnapshot d:s.getDocuments()){LinearLayout c=panel();c.addView(tv("📱 "+(d.getString("productName")==null?"Produk":d.getString("productName")),17));c.addView(tv((d.getString("price")==null?"":d.getString("price"))+"\nStatus: "+(d.getString("status")==null?"":d.getString("status")),14));content.addView(c);}});
        content.addView(section("Service"));
        db.collection("serviceRequests").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada permintaan service.",15));for(DocumentSnapshot d:s.getDocuments()){LinearLayout c=panel();c.addView(tv("🔧 "+d.getString("type"),17));c.addView(tv("Status: "+d.getString("status"),14));content.addView(c);}});
    }
        void developerPage(){
        base("👨‍💻 Developer — Miss Cell");
        addDeveloperCard();
        TextView info=tv("Hubungi developer untuk bantuan aplikasi, pengembangan fitur, integrasi server, atau kerja sama.",15);info.setTextColor(MUTED);content.addView(info);
    }

    void logout(){try{if(auth!=null)auth.signOut();LoginManager.getInstance().logOut();}catch(Exception ignored){}adminMode=false;memberMode=false;sp.edit().clear().apply();showLogin();}

    void memberPanel(){
        base("👥 Member MMC PONSEL");
        content.addView(tv("Login sebagai member • Tambah barang & balas chat pengguna",16));
        Button add=btn("➕ Tambah Barang"); content.addView(add); add.setOnClickListener(v->adminAddProduct());
        Button chats=btn("💬 Balas Chat Pengguna"); content.addView(chats); chats.setOnClickListener(v->adminChats());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
        Button out=btn("Logout Member"); out.setTextColor(WHITE); out.setBackgroundResource(R.drawable.card); content.addView(out); out.setOnClickListener(v->logout());
    }

    // ===== Admin =====
    void adminPanel(){
        base("🛠 Admin MMC PONSEL");
        content.addView(tv("Login sebagai admin • Kelola katalog & chat pengguna",16));
        Button add=btn("➕ Tambah Barang"); content.addView(add); add.setOnClickListener(v->adminAddProduct());
        Button chats=btn("💬 Balas Chat Pengguna"); content.addView(chats); chats.setOnClickListener(v->adminChats());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
        Button productsBtn=btn("📦 Lihat Barang di Database"); content.addView(productsBtn); productsBtn.setOnClickListener(v->adminProducts());
        Button out=btn("Logout Admin"); out.setTextColor(WHITE); out.setBackgroundResource(R.drawable.card); content.addView(out); out.setOnClickListener(v->logout());
    }

    void adminAddProduct(){
        selectedProductImage=null;
        base((memberMode?"👥 Member":"🛠 Admin")+" • ➕ Tambah Barang");
        EditText name=field("Nama produk *"); EditText price=field("Harga * — contoh Rp 3.500.000");
        EditText category=field("Kategori / Subkategori — HP / iPhone / Samsung / Sparepart / Aksesoris");
        EditText condition=field("Kondisi * — Baru / Bekas / Rusak"); EditText brand=field("Merek");
        EditText model=field("Model / Tipe"); EditText color=field("Warna"); EditText ram=field("RAM — contoh 8 GB");
        EditText storage=field("Penyimpanan — contoh 256 GB"); EditText stock=field("Stok");
        EditText sku=field("SKU / Kode barang"); EditText location=field("Lokasi barang");
        EditText shipping=field("Pengiriman — J&T / SiCepat / COD / lainnya");
        EditText damage=field("Catatan kerusakan — isi jika kondisi Rusak");
        EditText description=field("Deskripsi lengkap barang");
        content.addView(name);content.addView(price);content.addView(category);content.addView(condition);content.addView(brand);
        content.addView(model);content.addView(color);content.addView(ram);content.addView(storage);content.addView(stock);
        content.addView(sku);content.addView(location);content.addView(shipping);content.addView(damage);content.addView(description);
        Button photo=btn("📷 Pilih Foto Produk"); content.addView(photo);
        TextView selected=tv("Belum ada foto dipilih",13); selected.setTextColor(MUTED); content.addView(selected);
        photo.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); startActivityForResult(i,REQ_PRODUCT_IMAGE); });
        Button save=btn("Simpan Barang ke Marketplace"); content.addView(save);
        save.setOnClickListener(v->{
            if(name.length()==0 || price.length()==0){Toast.makeText(this,"Nama dan harga wajib diisi",Toast.LENGTH_LONG).show();return;}
            if(db==null) db=FirebaseFirestore.getInstance();
            java.util.HashMap<String,Object> data=new java.util.HashMap<>();
            data.put("name",name.getText().toString().trim()); data.put("price",price.getText().toString().trim());
            data.put("category",category.getText().toString().trim()); data.put("condition",condition.getText().toString().trim());
            data.put("brand",brand.getText().toString().trim()); data.put("model",model.getText().toString().trim());
            data.put("color",color.getText().toString().trim()); data.put("ram",ram.getText().toString().trim());
            data.put("storage",storage.getText().toString().trim()); data.put("stock",stock.getText().toString().trim());
            data.put("sku",sku.getText().toString().trim()); data.put("location",location.getText().toString().trim());
            data.put("shipping",shipping.getText().toString().trim()); data.put("damageNotes",damage.getText().toString().trim());
            data.put("description",description.getText().toString().trim()); data.put("rating",0.0);
            data.put("sellerUid",auth!=null&&auth.getCurrentUser()!=null?auth.getCurrentUser().getUid():"");
            data.put("createdAt",FieldValue.serverTimestamp());
            if(selectedProductImage==null){ saveProductData(data); } else {
                Toast.makeText(this,"Mengunggah foto produk...",Toast.LENGTH_SHORT).show();
                StorageReference ref=FirebaseStorage.getInstance().getReference().child("products/"+System.currentTimeMillis()+".jpg");
                ref.putFile(selectedProductImage).continueWithTask(t->ref.getDownloadUrl()).addOnSuccessListener(url->{data.put("imageUrl",url.toString());saveProductData(data);}).addOnFailureListener(e->Toast.makeText(this,"Upload foto gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }
        });
    }
    void saveProductData(java.util.HashMap<String,Object> data){ db.collection("products").add(data).addOnSuccessListener(ref->{Toast.makeText(this,"Barang berhasil ditambahkan",Toast.LENGTH_LONG).show();adminPanel();}).addOnFailureListener(e->Toast.makeText(this,"Gagal menyimpan: "+e.getMessage(),Toast.LENGTH_LONG).show()); }

    void reviews(String[] p){
        base("⭐ Rating & Ulasan");
        content.addView(tv(p[0],21));
        db.collection("reviews").whereEqualTo("productId",p.length>5?p[5]:"").get().addOnSuccessListener(s->{ if(s.isEmpty()) content.addView(tv("Belum ada ulasan.",15)); for(DocumentSnapshot d:s.getDocuments()){ LinearLayout c=panel(); c.addView(tv("⭐ "+d.getLong("rating")+"  "+(d.getString("userName")==null?"Pembeli":d.getString("userName")),15)); c.addView(tv(d.getString("text")==null?"":d.getString("text"),14)); content.addView(c); }});
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u!=null){ EditText review=field("Tulis ulasan setelah transaksi selesai..."); content.addView(review); LinearLayout stars=new LinearLayout(this); for(int i=1;i<=5;i++){ final int score=i; Button b=btn("⭐ "+i); stars.addView(b,new LinearLayout.LayoutParams(0,48,1)); b.setOnClickListener(v->{java.util.HashMap<String,Object> r=new java.util.HashMap<>();r.put("productId",p.length>5?p[5]:"");r.put("productName",p[0]);r.put("uid",u.getUid());r.put("userName",u.getDisplayName()==null?"Pembeli":u.getDisplayName());r.put("rating",score);r.put("text",review.getText().toString().trim());r.put("createdAt",FieldValue.serverTimestamp());db.collection("reviews").add(r).addOnSuccessListener(x->{Toast.makeText(this,"Ulasan berhasil dikirim",Toast.LENGTH_SHORT).show();reviews(p);});}); } content.addView(stars); }
    }

    void adminProducts(){
        base("📦 Barang Database");
        if(db==null) db=FirebaseFirestore.getInstance();
        db.collection("products").get().addOnSuccessListener(snapshot->{
            if(snapshot.isEmpty()){content.addView(tv("Belum ada barang di Firestore.",16));return;}
            for(DocumentSnapshot d:snapshot.getDocuments()){
                String n=d.getString("name"); String p=d.getString("price"); String c=d.getString("condition"); String r=d.getString("rating");
                content.addView(tv((n==null?"":n)+"\n"+(p==null?"":p)+" • "+(c==null?"":c)+" • ⭐ "+(r==null?"":r),16));
            }
        }).addOnFailureListener(e->content.addView(tv("Gagal membaca database: "+e.getMessage(),14)));
    }

    void solutionGroup(){
        if(!adminMode && !memberMode){
            Toast.makeText(this,"Grup khusus Admin & Member",Toast.LENGTH_LONG).show();
            return;
        }
        base("👥 Grup Solusi MMC PONSEL");
        TextView info=tv("Ruang khusus Admin & Member untuk bertukar solusi, pengalaman service, ide jual beli, dan membantu perkembangan MMC PONSEL. Semua anggota grup dapat membaca dan membalas pesan.",14);
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
                            TextView m=tv(("admin".equals(role)?"🛠 Admin":"👥 Member")+" • "+(name==null?"Anggota":name)+"\n"+(text==null?"":text),15);
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
            java.util.HashMap<String,Object> msg=new java.util.HashMap<>();
            msg.put("uid",u.getUid());
            msg.put("name",memberMode?"Member Miss":(u.getDisplayName()==null?"Admin":u.getDisplayName()));
            msg.put("role",adminMode?"admin":"member");
            msg.put("text",text);
            msg.put("createdAt",FieldValue.serverTimestamp());
            db.collection("solutionGroup").document("main").collection("messages").add(msg)
                    .addOnSuccessListener(x->input.setText(""))
                    .addOnFailureListener(e->Toast.makeText(this,"Pesan gagal dikirim: "+e.getMessage(),Toast.LENGTH_LONG).show());
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

    /** Latar halaman bertema: partikel, pola, dan garis cahaya berbeda tiap halaman. */
    static class AnimatedBackgroundView extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final java.util.Random random=new java.util.Random(82);
        final float[] x=new float[20], y=new float[20], vx=new float[20], vy=new float[20], r=new float[20];
        final int bg, particle, line, pattern;
        AnimatedBackgroundView(android.content.Context c,int bg,int particle,int line,int pattern){
            super(c); this.bg=bg; this.particle=particle; this.line=line; this.pattern=pattern;
            for(int i=0;i<x.length;i++){x[i]=random.nextFloat();y[i]=random.nextFloat();vx[i]=(random.nextFloat()-.5f)*0.00045f;vy[i]=(random.nextFloat()-.5f)*0.00035f;r[i]=2+random.nextFloat()*6;}
            paint.setStrokeWidth(1.2f); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c); c.drawColor(bg);
            long now=System.currentTimeMillis(); float w=getWidth(),h=getHeight();
            for(int i=0;i<x.length;i++){
                x[i]+=vx[i]*33f; y[i]+=vy[i]*33f;
                if(x[i]<-.05f||x[i]>1.05f)vx[i]*=-1; if(y[i]<-.05f||y[i]>1.05f)vy[i]*=-1;
                float px=x[i]*w,py=y[i]*h; int alpha=35+(int)(30*Math.sin(now/500.0+i));
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(Math.max(18,Math.min(75,alpha)),Color.red(particle),Color.green(particle),Color.blue(particle)));
                c.drawCircle(px,py,r[i],paint);
            }
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2f);
            int a=20+(int)(10*Math.sin(now/900.0)); paint.setColor(Color.argb(a,Color.red(line),Color.green(line),Color.blue(line)));
            float shift=(now%7000L)/7000f*(w+h);
            if(pattern%3==0){ c.drawLine(shift-h,0,shift,h,paint); c.drawLine(shift-h*0.55f,h,shift+h*0.45f,0,paint); }
            else if(pattern%3==1){
                float gap=Math.max(80,Math.min(150,w/5));
                for(float xx=-h;xx<w+h;xx+=gap)c.drawLine(xx,0,xx+h,h,paint);
            } else {
                float cx=w*.78f, cy=h*.22f, pulse=70+(float)(20*Math.sin(now/700.0));
                c.drawCircle(cx,cy,pulse,paint); c.drawCircle(cx,cy,pulse*1.7f,paint);
                c.drawLine(0,h*.82f,w,h*.18f,paint);
            }
            if(pattern>=3){
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1f);
                float cx=w*.18f,cy=h*.78f, rr=35+(float)(12*Math.sin(now/800.0));
                c.drawCircle(cx,cy,rr,paint); c.drawCircle(cx,cy,rr*1.8f,paint);
            }
            postInvalidateDelayed(33);
        }
    }

    void openWhatsApp(){try{String n="6283830655780";Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+n));startActivity(i);}catch(Exception e){Toast.makeText(this,"WhatsApp tidak tersedia",0).show();}}
}
