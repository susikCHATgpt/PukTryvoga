package com.puktryvoga.app;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.content.pm.PackageManager;
import android.view.*;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.*;

public class MainActivity extends Activity {
  final String[] regions = {"Вінницька", "Волинська", "Дніпропетровська", "Донецька", "Житомирська", "Закарпатська", "Запорізька", "Івано-Франківська", "Київська", "Кіровоградська", "Луганська", "Львівська", "Миколаївська", "Одеська", "Полтавська", "Рівненська", "Сумська", "Тернопільська", "Харківська", "Херсонська", "Хмельницька", "Черкаська", "Чернівецька", "Чернігівська", "м. Київ"};
  final int bg=Color.rgb(15,24,33), panel=Color.rgb(29,43,56), white=Color.WHITE;
  FirebaseFirestore db; FirebaseAuth auth; LinearLayout root, list; Spinner regionPicker; boolean admin=false;
  final Map<String,String> statuses=new HashMap<>();
  final Map<String,ListenerRegistration> listeners=new HashMap<>();
  final Set<String> subscriptions=new HashSet<>();
  final Map<String,String> previous=new HashMap<>();
  boolean initialized=false;
  @Override public void onCreate(Bundle b){super.onCreate(b); db=FirebaseFirestore.getInstance();auth=FirebaseAuth.getInstance();
    if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2);
    NotificationChannel channel=new NotificationChannel("alerts","ПукТривога",NotificationManager.IMPORTANCE_HIGH);
    channel.enableVibration(true);channel.setDescription("Жартівливі сповіщення про тривоги");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    subscriptions.addAll(getPreferences(0).getStringSet("regions",new HashSet<>()));
    if(subscriptions.isEmpty())subscriptions.add("Житомирська");
    for(String r:regions)listen(r);
    for(int i=0;i<regions.length;i++)if(subscriptions.contains(regions[i]))FirebaseMessaging.getInstance().subscribeToTopic("region_"+i);
    show();
  }
  TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(white);t.setTextSize(size);t.setPadding(18,15,18,15);return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  void show(){root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);setContentView(root);
    root.addView(text("📡  ПукТривога",26));root.addView(text("Жартівливий застосунок • НЕ офіційні сповіщення про небезпеку",12));
    ScrollView scroll=new ScrollView(this);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout content=new LinearLayout(this);content.setOrientation(1);scroll.addView(content);
    TextView title=text("Статус областей України",20);content.addView(title);
    list=new LinearLayout(this);list.setOrientation(1);content.addView(list);render();
    Button prefs=button("🔔 Мої області для сповіщень");content.addView(prefs);prefs.setOnClickListener(v->chooseRegions());
    if(admin){content.addView(text("КЕРУВАННЯ (АДМІН)",18));
      regionPicker=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,regions);regionPicker.setAdapter(adapter);content.addView(regionPicker);
      Button fart=button("💨 Оголосити пердёжну тривогу");content.addView(fart);fart.setOnClickListener(v->publish("fart"));
      Button burp=button("📢 Оголосити відрижну тривогу");content.addView(burp);burp.setOnClickListener(v->publish("burp"));
      Button clear=button("✅ Відбій");content.addView(clear);clear.setOnClickListener(v->publish("none"));
      Button logout=button("Вийти з адмін-панелі");content.addView(logout);logout.setOnClickListener(v->{auth.signOut();admin=false;show();});
    } else {Button login=button("🔐 Вхід адміністратора");content.addView(login);login.setOnClickListener(v->login());}
  }
  void render(){if(list==null)return;list.removeAllViews();for(String r:regions){String state=statuses.getOrDefault(r,"none");String label=state.equals("fart")?"💨 ПЕРДЁЖНА ТРИВОГА":state.equals("burp")?"📢 ВІДРИЖНА ТРИВОГА":"🟢 Спокійно";
    TextView t=text(r+"  •  "+label,14);t.setBackgroundColor(panel);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(10,3,10,3);list.addView(t,p);}}
  void listen(String r){listeners.put(r,db.collection("alerts").document(r).addSnapshotListener((snap,error)->{
    if(error!=null)return;String value=snap!=null&&snap.exists()?snap.getString("type"):null;
    if(!Arrays.asList("fart","burp","none").contains(value))value="none";
    String old=previous.put(r,value);statuses.put(r,value);render();
    if(old!=null&&!old.equals(value)&&subscriptions.contains(r))notifyUser(r,value);
  }));}
  void notifyUser(String r,String state){String msg=state.equals("fart")?"Пердёжная тревога!":state.equals("burp")?"Відрижна тривога!":"Відбій тривоги";
    Intent intent=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    Notification n=new Notification.Builder(this,"alerts").setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle("ПукТривога • "+r).setContentText(msg).setContentIntent(pi).setAutoCancel(true).build();
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(r.hashCode(),n);
  }
  void chooseRegions(){boolean[] checked=new boolean[regions.length];for(int i=0;i<regions.length;i++)checked[i]=subscriptions.contains(regions[i]);
    new AlertDialog.Builder(this).setTitle("Оберіть області").setMultiChoiceItems(regions,checked,(d,which,isChecked)->checked[which]=isChecked)
      .setPositiveButton("Зберегти",(d,w)->{subscriptions.clear();for(int i=0;i<regions.length;i++){if(checked[i]){subscriptions.add(regions[i]);FirebaseMessaging.getInstance().subscribeToTopic("region_"+i);}else FirebaseMessaging.getInstance().unsubscribeFromTopic("region_"+i);}getPreferences(0).edit().putStringSet("regions",new HashSet<>(subscriptions)).apply();Toast.makeText(this,"Збережено",Toast.LENGTH_SHORT).show();})
      .setNegativeButton("Скасувати",null).show();}
  void login(){LinearLayout form=new LinearLayout(this);form.setOrientation(1);EditText email=new EditText(this);email.setHint("Email");form.addView(email);EditText pass=new EditText(this);pass.setHint("Пароль");pass.setInputType(129);form.addView(pass);
    new AlertDialog.Builder(this).setTitle("Вхід адміністратора").setView(form).setPositiveButton("Увійти",(d,w)->auth.signInWithEmailAndPassword(email.getText().toString().trim(),pass.getText().toString()).addOnCompleteListener(task->{if(task.isSuccessful())checkAdmin();else Toast.makeText(this,"Помилка входу",Toast.LENGTH_LONG).show();})).setNegativeButton("Скасувати",null).show();}
  void checkAdmin(){if(auth.getCurrentUser()==null)return;db.collection("admins").document(auth.getCurrentUser().getUid()).get().addOnSuccessListener(doc->{admin=doc.exists()&&Boolean.TRUE.equals(doc.getBoolean("enabled"));if(!admin)Toast.makeText(this,"Немає прав адміністратора",Toast.LENGTH_LONG).show();show();});}
  void publish(String type){if(!admin||regionPicker==null)return;String region=regionPicker.getSelectedItem().toString();Map<String,Object> data=new HashMap<>();data.put("type",type);data.put("updatedAt",FieldValue.serverTimestamp());
    db.collection("alerts").document(region).set(data).addOnSuccessListener(v->Toast.makeText(this,"Оновлено: "+region,Toast.LENGTH_SHORT).show()).addOnFailureListener(e->Toast.makeText(this,"Немає доступу: "+e.getMessage(),Toast.LENGTH_LONG).show());}
  @Override protected void onDestroy(){super.onDestroy();for(ListenerRegistration l:listeners.values())l.remove();}
}
