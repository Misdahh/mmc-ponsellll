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
import androidx.credentials.exceptions.GetCredentialException;

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
    MediaPlayer ambientPlayer;

    int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(WHITE);t.setPadding(dp(8),dp(6),dp(8),dp(6));return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setTextSize(14);b.setAllCaps(false);b.setBackgroundResource(R.drawable.button_yellow);return b;}

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
        topMenu();
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,0,0,dp(18));
        android.view.animation.AlphaAnimation itemFade=new android.view.animation.AlphaAnimation(0f,1f);
        itemFade.setDuration(420);
        LayoutAnimationController lac=new LayoutAnimationController(itemFade,0.07f);
        content.setLayoutAnimation(lac);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.addView(content);
        mainColumn.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        title.setAlpha(0f);
        title.animate().alpha(1f).setDuration(500).start();
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
        base("📱 MMC PONSEL");
        startAmbientSound();
        TextView sub=tv("JUAL • BELI • SERVICE HP\nBaru • Bekas • Rusak",18);
        sub.setTextColor(YELLOW); content.addView(sub);

        EditText email=field("Email");
        EditText pass=field("Password");
        pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(email); content.addView(pass);

        Button login=btn("🔐 Login");
        content.addView(login);
        Button register=btn("📝 Daftar Akun Baru");
        register.setTextColor(WHITE); register.setBackgroundResource(R.drawable.card); content.addView(register);
        Button forgot=btn("🔑 Lupa Password");
        forgot.setTextColor(WHITE); forgot.setBackgroundResource(R.drawable.card); content.addView(forgot);

        Button google=btn("🔵 Lanjut dengan Google");
        google.setTextColor(WHITE); google.setBackgroundResource(R.drawable.card); content.addView(google);
        Button facebook=btn("🔷 Lanjut dengan Facebook");
        facebook.setTextColor(WHITE); facebook.setBackgroundResource(R.drawable.card); content.addView(facebook);

        TextView note=tv("Akun tersimpan aman melalui Firebase Authentication. Produk yang dipublikasikan dapat dilihat pengguna lain.",13);
        note.setTextColor(MUTED); content.addView(note);
        addDeveloperCard();

        login.setOnClickListener(v->{
            String e=email.getText().toString().trim();
            String p=pass.getText().toString();
            if(e.isEmpty()||p.isEmpty()){Toast.makeText(this,"Email dan password wajib diisi",Toast.LENGTH_LONG).show();return;}
            if(!firebaseReady())return;
            auth.signInWithEmailAndPassword(e,p).addOnCompleteListener(this,t->{
                if(t.isSuccessful()) checkUserStatusAndLogin(auth.getCurrentUser());
                else Toast.makeText(this,"Login gagal: "+(t.getException()==null?"periksa akun":t.getException().getMessage()),Toast.LENGTH_LONG).show();
            });
        });
        register.setOnClickListener(v->registerAccount());
        forgot.setOnClickListener(v->forgotPassword());
        google.setOnClickListener(v->signInWithGoogle());
        facebook.setOnClickListener(v->signInWithFacebook());
        Button admin=btn("🛠 Login Admin");
        admin.setTextColor(WHITE); admin.setBackgroundResource(R.drawable.card); content.addView(admin);
        admin.setOnClickListener(v->showAdminLogin());
    }

    void registerAccount(){
        base("📝 Daftar Akun MMC PONSEL");
        EditText name=field("Nama lengkap");
        EditText email=field("Email aktif");
        EditText pass=field("Password minimal 6 karakter");
        EditText confirm=field("Ulangi password");
        pass.setInputType(129); confirm.setInputType(129);
        content.addView(name); content.addView(email); content.addView(pass); content.addView(confirm);
        Button create=btn("Buat Akun"); content.addView(create);
        Button back=btn("Kembali Login"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);
        create.setOnClickListener(v->{
            String n=name.getText().toString().trim(), e=email.getText().toString().trim(), p=pass.getText().toString(), c=confirm.getText().toString();
            if(n.isEmpty()||e.isEmpty()||p.length()<6||!p.equals(c)){Toast.makeText(this,"Lengkapi data dan pastikan password sama (min. 6 karakter)",Toast.LENGTH_LONG).show();return;}
            if(!firebaseReady())return;
            auth.createUserWithEmailAndPassword(e,p).addOnCompleteListener(this,t->{
                if(!t.isSuccessful()){Toast.makeText(this,"Pendaftaran gagal: "+t.getException().getMessage(),Toast.LENGTH_LONG).show();return;}
                FirebaseUser u=auth.getCurrentUser();
                java.util.HashMap<String,Object> data=new java.util.HashMap<>();
                data.put("uid",u.getUid()); data.put("name",n); data.put("email",e); data.put("role","user"); data.put("blocked",false); data.put("createdAt",FieldValue.serverTimestamp());
                db.collection("users").document(u.getUid()).set(data).addOnCompleteListener(x->{
                    Toast.makeText(this,"Akun berhasil dibuat",Toast.LENGTH_SHORT).show();
                    checkUserStatusAndLogin(u);
                });
            });
        });
        back.setOnClickListener(v->showLogin());
    }

    void forgotPassword(){
        base("🔑 Lupa Password");
        EditText email=field("Masukkan email akun");
        content.addView(email);
        Button send=btn("Kirim Link Reset Password"); content.addView(send);
        Button back=btn("Kembali Login"); back.setTextColor(WHITE); back.setBackgroundResource(R.drawable.card); content.addView(back);
        send.setOnClickListener(v->{
            String e=email.getText().toString().trim();
            if(e.isEmpty()){Toast.makeText(this,"Masukkan email terlebih dahulu",Toast.LENGTH_LONG).show();return;}
            if(!firebaseReady())return;
            auth.sendPasswordResetEmail(e).addOnCompleteListener(this,t->Toast.makeText(this,
                    t.isSuccessful()?"Link reset password sudah dikirim ke email.":"Gagal mengirim reset: "+t.getException().getMessage(),Toast.LENGTH_LONG).show());
        });
        back.setOnClickListener(v->showLogin());
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

    void showAdminLogin(){
        base("🔐 Login Admin MMC PONSEL");
        content.addView(tv("Akun admin menggunakan Firebase Authentication. Username admin: miss",16));
        EditText user=field("Username admin");
        EditText pass=field("Password admin");
        pass.setInputType(129);
        content.addView(user);content.addView(pass);
        Button login=btn("Masuk Admin");content.addView(login);
        Button back=btn("Kembali");back.setTextColor(WHITE);back.setBackgroundResource(R.drawable.card);content.addView(back);
        login.setOnClickListener(v->{
            if(!"miss".equals(user.getText().toString().trim())){Toast.makeText(this,"Username admin salah",Toast.LENGTH_LONG).show();return;}
            String pw=pass.getText().toString();
            if(pw.length()<6){Toast.makeText(this,"Password tidak valid",Toast.LENGTH_LONG).show();return;}
            if(!firebaseReady())return;
            auth.signInWithEmailAndPassword("miss@mmcponsel.app",pw).addOnCompleteListener(this,t->{
                if(!t.isSuccessful()){Toast.makeText(this,"Login admin gagal. Buat akun admin Firebase dan aktifkan Email/Password.",Toast.LENGTH_LONG).show();return;}
                FirebaseFunctions.getInstance().getHttpsCallable("ensureAdminRole").call()
                        .addOnSuccessListener(x->{adminMode=true;memberMode=false;Toast.makeText(this,"Login admin berhasil",Toast.LENGTH_SHORT).show();adminPanel();})
                        .addOnFailureListener(e->Toast.makeText(this,"Akun ini belum memiliki hak admin: "+e.getMessage(),Toast.LENGTH_LONG).show());
            });
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
            home();
        }).addOnFailureListener(e->{
            sp.edit().putString("user",user.getEmail()==null?user.getUid():user.getEmail())
                    .putString("name",user.getDisplayName()==null?"Pelanggan":user.getDisplayName())
                    .putString("uid",user.getUid()).apply();
            home();
        });
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(callbackManager!=null) callbackManager.onActivityResult(requestCode,resultCode,data);
    }

    // ===== Store UI =====
    void home(){
        base("MMC PONSEL");
        startAmbientSound();
        TextView online=tv("● ONLINE • Data tersambung ke server",13); online.setTextColor(GREEN); content.addView(online);
        addWelcomeCard();
        addDeveloperCard();
        TextView welcome=tv("Halo, "+sp.getString("name","Pelanggan"),18);welcome.setTextColor(YELLOW);content.addView(welcome);
        EditText search=field("Cari HP, merek, atau layanan...");content.addView(search);
        TextView banner=tv("HP BARU • BEKAS • RUSAK\nJUAL BELI & SERVICE\nKatalog dan pesanan tersimpan online",20);banner.setTypeface(null,Typeface.BOLD);banner.setBackgroundResource(R.drawable.card);content.addView(banner);
        gridMenus();content.addView(tv("Produk dari Server",20));
        productList=new LinearLayout(this); productList.setOrientation(LinearLayout.VERTICAL); content.addView(productList);
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
                            if(n==null) continue;
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
    void gridMenus(){String[][] m={{"📱 HP Baru","Jual HP baru"},{"♻ HP Bekas","HP second berkualitas"},{"🛠 HP Rusak","Sparepart / perbaikan"},{"🔧 Service","Miss Cell"},{"🛒 Jual HP","Jual perangkat Anda"},{"💳 Beli HP","Belanja HP"}}; for(String[] x:m){Button b=btn(x[0]+"\n"+x[1]);b.setTextColor(WHITE);b.setBackgroundResource(R.drawable.card);content.addView(b);if(x[0].contains("Service"))b.setOnClickListener(v->service());else if(x[0].contains("Beli"))b.setOnClickListener(v->category());else if(x[0].contains("Jual"))b.setOnClickListener(v->sell());}}
    void productCard(String[] p){productCard(p,content);}
    void productCard(String[] p, LinearLayout target){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(8),dp(8),dp(8),dp(8));c.setBackgroundResource(R.drawable.card);TextView n=tv(p[0],17);n.setTypeface(null,Typeface.BOLD);c.addView(n);c.addView(tv(p[1]+" • "+p[2]+" • ⭐ "+p[3],15));Button b=btn("Lihat detail / Tambah keranjang");c.addView(b);b.setOnClickListener(v->detail(p));target.addView(c);}
    void detail(String[] p){
        base("Detail Produk");content.addView(tv(p[0],23));content.addView(tv(p[1]+"\nKondisi: "+p[2]+"\nRating: ⭐ "+p[3]+"\nData produk diambil dari server MMC PONSEL.",16));
        Button cart=btn("Tambah ke Keranjang Online");content.addView(cart);Button buy=btn("Beli Sekarang");content.addView(buy);
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
    void category(){base("Kategori HP");loadCategoryFromServer();}
    void loadCategoryFromServer(){
        if(db==null)db=FirebaseFirestore.getInstance();
        db.collection("products").orderBy("createdAt",Query.Direction.DESCENDING).get().addOnSuccessListener(snapshot->{
            if(snapshot.isEmpty()){content.addView(tv("Belum ada produk online.",16));return;}
            for(DocumentSnapshot d:snapshot.getDocuments()){String n=d.getString("name"),p=d.getString("price"),c=d.getString("condition"),r=d.getString("rating");if(n!=null)productCard(new String[]{n,p==null?"":p,c==null?"":c,r==null?"0.0":r});}
        }).addOnFailureListener(e->content.addView(tv("Katalog online gagal dimuat: "+e.getMessage(),14)));
    }
    void service(){base("Service HP — Online");content.addView(tv("Form service tersimpan langsung ke server MMC PONSEL",22));for(String s:new String[]{"Layar Pecah / Touchscreen","Ganti Baterai","Tidak Bisa Dinyalakan","Kamera","Water Damage","Software / Unlock","Lainnya"}){Button b=btn(s);content.addView(b);b.setOnClickListener(v->serviceForm(s));}}
    void serviceForm(String type){base("Pesan Service Online");content.addView(tv("Jenis: "+type,20));EditText note=field("Keluhan / catatan");content.addView(note);EditText phone=field("Nomor HP");content.addView(phone);Button b=btn("Kirim Permintaan ke Server");content.addView(b);b.setOnClickListener(v->{FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}if(phone.length()==0){Toast.makeText(this,"Masukkan nomor HP",0).show();return;}java.util.HashMap<String,Object> data=new java.util.HashMap<>();data.put("uid",u.getUid());data.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());data.put("email",u.getEmail());data.put("type",type);data.put("note",note.getText().toString().trim());data.put("phone",phone.getText().toString().trim());data.put("status","Menunggu Diproses");data.put("createdAt",FieldValue.serverTimestamp());db.collection("serviceRequests").add(data).addOnSuccessListener(x->{Toast.makeText(this,"Permintaan service terkirim online",Toast.LENGTH_LONG).show();transactions();}).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());});}
    void sell(){base("Jual HP Online");EditText category=field("Kategori");EditText brand=field("Merek");EditText model=field("Model");EditText condition=field("Kondisi");EditText price=field("Harga yang diinginkan");EditText phone=field("Nomor kontak");for(EditText e:new EditText[]{category,brand,model,condition,price,phone})content.addView(e);Button b=btn("Kirim Penawaran ke Server");content.addView(b);b.setOnClickListener(v->{FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}if(model.length()==0||phone.length()==0){Toast.makeText(this,"Model dan nomor kontak wajib diisi",0).show();return;}java.util.HashMap<String,Object> data=new java.util.HashMap<>();data.put("uid",u.getUid());data.put("userName",u.getDisplayName()==null?"Pelanggan":u.getDisplayName());data.put("email",u.getEmail());data.put("category",category.getText().toString().trim());data.put("brand",brand.getText().toString().trim());data.put("model",model.getText().toString().trim());data.put("condition",condition.getText().toString().trim());data.put("desiredPrice",price.getText().toString().trim());data.put("phone",phone.getText().toString().trim());data.put("status","Menunggu Ditinjau");data.put("createdAt",FieldValue.serverTimestamp());db.collection("sellRequests").add(data).addOnSuccessListener(x->{Toast.makeText(this,"Penawaran terkirim online",Toast.LENGTH_LONG).show();transactions();}).addOnFailureListener(e->Toast.makeText(this,"Gagal: "+e.getMessage(),Toast.LENGTH_LONG).show());});}
    void bottom(){LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);String[] a={"⌂ Beranda","🛒 Beli","🔧 Service","👤 Profil"};for(String s:a){Button b=btn(s);b.setTextColor(WHITE);b.setBackgroundColor(Color.TRANSPARENT);nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));if(s.contains("Service"))b.setOnClickListener(v->service());if(s.contains("Profil"))b.setOnClickListener(v->profile());if(s.contains("Beli"))b.setOnClickListener(v->category());if(s.contains("Beranda"))b.setOnClickListener(v->home());}mainColumn.addView(nav);}
    void profile(){
        base("👤 Profil Saya");
        FirebaseUser u=auth==null?null:auth.getCurrentUser();
        if(u==null){showLogin();return;}
        content.addView(tv((u.getDisplayName()==null?"Pelanggan":u.getDisplayName())+"\n"+(u.getEmail()==null?"":u.getEmail()),20));
        Button add=btn("➕ Tambah Barang / Jual HP"); content.addView(add); add.setOnClickListener(v->profileAddProduct());
        Button change=btn("🔐 Ubah Password"); content.addView(change); change.setOnClickListener(v->changePassword());
        Button account=btn("💾 Data Akun Online"); content.addView(account); account.setOnClickListener(v->saveProfileOnline());
        Button tx=btn("🧾 Transaksi Saya"); content.addView(tx); tx.setOnClickListener(v->transactions());
        Button chat=btn("💬 Chat Admin MMC PONSEL"); content.addView(chat); chat.setOnClickListener(v->chatWithAdmin());
        Button dev=btn("👨‍💻 Developer / Pencipta — Miss Cell"); content.addView(dev); dev.setOnClickListener(v->developerPage());
        Button wa=btn("Chat WhatsApp MMC PONSEL"); content.addView(wa); wa.setOnClickListener(v->openWhatsApp());
        Button out=btn("Logout"); content.addView(out); out.setOnClickListener(v->logout());
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
        base("➕ Tambah Barang");
        EditText name=field("Nama HP / barang");
        EditText price=field("Harga, contoh Rp 3.500.000");
        EditText condition=field("Kondisi: Baru / Bekas / Rusak");
        EditText category=field("Kategori: HP / Aksesoris / Service");
        EditText description=field("Deskripsi barang");
        content.addView(name);content.addView(price);content.addView(condition);content.addView(category);content.addView(description);
        Button save=btn("🚀 Publikasikan ke Produk");content.addView(save);
        save.setOnClickListener(v->{
            FirebaseUser u=auth.getCurrentUser();
            if(name.length()==0||price.length()==0){Toast.makeText(this,"Nama dan harga wajib diisi",Toast.LENGTH_LONG).show();return;}
            java.util.HashMap<String,Object> d=new java.util.HashMap<>();
            d.put("name",name.getText().toString().trim()); d.put("price",price.getText().toString().trim());
            d.put("condition",condition.getText().toString().trim()); d.put("category",category.getText().toString().trim());
            d.put("description",description.getText().toString().trim()); d.put("rating","0.0");
            d.put("ownerUid",u.getUid()); d.put("ownerName",u.getDisplayName()==null?"Pengguna":u.getDisplayName());
            d.put("createdAt",FieldValue.serverTimestamp()); d.put("status","published");
            db.collection("products").add(d).addOnSuccessListener(x->{Toast.makeText(this,"Barang berhasil dipublikasikan dan tampil di Produk.",Toast.LENGTH_LONG).show();home();})
              .addOnFailureListener(e->Toast.makeText(this,"Gagal mempublikasikan: "+e.getMessage(),Toast.LENGTH_LONG).show());
        });
    }

    void saveProfileOnline(){FirebaseUser u=auth.getCurrentUser();if(u==null)return;EditText address=field("Alamat pengiriman");content.addView(address);Button save=btn("Simpan ke Server");content.addView(save);save.setOnClickListener(v->{db.collection("users").document(u.getUid()).set(new java.util.HashMap<String,Object>(){{put("uid",u.getUid());put("name",u.getDisplayName());put("email",u.getEmail());put("address",address.getText().toString().trim());put("updatedAt",FieldValue.serverTimestamp());}},com.google.firebase.firestore.SetOptions.merge()).addOnSuccessListener(x->Toast.makeText(this,"Profil tersimpan online",Toast.LENGTH_SHORT).show());});}
    void transactions(){base("Transaksi Online");FirebaseUser u=auth==null?null:auth.getCurrentUser();if(u==null){showLogin();return;}content.addView(tv("Pesanan",19));db.collection("orders").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada pesanan.",15));for(DocumentSnapshot d:s.getDocuments())content.addView(tv((d.getString("productName")==null?"":d.getString("productName"))+"\n"+(d.getString("price")==null?"":d.getString("price"))+"\nStatus: "+(d.getString("status")==null?"":d.getString("status")),16));});content.addView(tv("Service",19));db.collection("serviceRequests").whereEqualTo("uid",u.getUid()).get().addOnSuccessListener(s->{if(s.isEmpty())content.addView(tv("Belum ada permintaan service.",15));for(DocumentSnapshot d:s.getDocuments())content.addView(tv("🔧 "+d.getString("type")+"\nStatus: "+d.getString("status"),16));});}

    void developerPage(){
        base("👨‍💻 Developer — Miss Cell");
        addDeveloperCard();
        TextView info=tv("Hubungi developer untuk bantuan aplikasi, pengembangan fitur, integrasi server, atau kerja sama.",15);info.setTextColor(MUTED);content.addView(info);
    }

    void logout(){try{if(auth!=null)auth.signOut();LoginManager.getInstance().logOut();}catch(Exception ignored){}adminMode=false;memberMode=false;sp.edit().clear().apply();showLogin();}

    void memberPanel(){
        base("👥 Member MMC PONSEL");
        content.addView(tv("Login sebagai member • Kelola chat dan gunakan Tambah Barang melalui Profil",16));
        Button chats=btn("💬 Balas Chat Pengguna"); content.addView(chats); chats.setOnClickListener(v->adminChats());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
        Button out=btn("Logout Member"); out.setTextColor(WHITE); out.setBackgroundResource(R.drawable.card); content.addView(out); out.setOnClickListener(v->logout());
    }

    // ===== Admin =====
    void adminPanel(){
        base("🛠 Admin MMC PONSEL");
        content.addView(tv("Kontrol pusat • pengguna • produk • chat • Tambah Barang tersedia melalui Profil",16));
        Button users=btn("👥 Kelola Pengguna"); content.addView(users); users.setOnClickListener(v->adminUsers());
        Button productsBtn=btn("📦 Kelola Produk"); content.addView(productsBtn); productsBtn.setOnClickListener(v->adminProducts());
        Button chats=btn("💬 Chat Pengguna"); content.addView(chats); chats.setOnClickListener(v->adminChats());
        Button group=btn("👥 Grup Solusi Admin & Member"); content.addView(group); group.setOnClickListener(v->solutionGroup());
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

            paint.setColor(Color.argb(230,255,212,0));
            c.drawOval(cx-54*scale,cy+4*scale,cx-18*scale,cy+15*scale,paint);
            c.drawOval(cx+18*scale,cy+4*scale,cx+54*scale,cy+15*scale,paint);
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
