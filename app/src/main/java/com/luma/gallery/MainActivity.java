package com.luma.gallery;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int PICK_MEDIA = 41;
    private final ArrayList<String> items = new ArrayList<>();
    private final Set<String> favorites = new HashSet<>(), trash = new HashSet<>();
    private GridView grid;
    private MediaAdapter adapter;
    private EditText search;
    private String filter = "all";
    private SharedPreferences prefs;
    private final int burgundy = Color.rgb(105, 24, 48);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(250,248,247));
        getWindow().setNavigationBarColor(Color.rgb(250,248,247));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences("luma_library", MODE_PRIVATE);
        items.addAll(prefs.getStringSet("items", new HashSet<>()));
        favorites.addAll(prefs.getStringSet("favorites", new HashSet<>()));
        trash.addAll(prefs.getStringSet("trash", new HashSet<>()));
        buildUi();
    }
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(burgundy));return b;}
    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,16,18,8);root.setBackgroundColor(Color.rgb(250,248,247));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView brand=text("LUMA",27,burgundy,true);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));Button add=button("＋  Importar");add.setOnClickListener(v->pickMedia());header.addView(add);root.addView(header);
        TextView subtitle=text("Tu espacio para tus recuerdos",13,Color.DKGRAY,false);subtitle.setPadding(0,3,0,14);root.addView(subtitle);
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setHint("Buscar fotos y videos");search.setPadding(14,4,14,4);root.addView(search,new LinearLayout.LayoutParams(-1,52));
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){refresh();}public void afterTextChanged(android.text.Editable e){}});
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(0,10,0,10);addTab(tabs,"Todo","all");addTab(tabs,"Favoritos","favorites");addTab(tabs,"Papelera","trash");root.addView(tabs);
        grid=new GridView(this);grid.setNumColumns(3);grid.setVerticalSpacing(5);grid.setHorizontalSpacing(5);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);adapter=new MediaAdapter();grid.setAdapter(adapter);root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        grid.setOnItemClickListener((p,v,pos,id)->showActions((String)adapter.getItem(pos)));
        grid.setOnItemLongClickListener((p,v,pos,id)->{String uri=(String)adapter.getItem(pos);if(filter.equals("trash")){trash.remove(uri);}else if(!favorites.add(uri)){favorites.remove(uri);}save();refresh();return true;});
        TextView hint=text("Tocá un elemento para opciones · Mantenelo presionado para marcar favorito",11,Color.GRAY,false);hint.setGravity(Gravity.CENTER);hint.setPadding(0,8,0,4);root.addView(hint);setContentView(root);refresh();
    }
    private void addTab(LinearLayout row,String label,String key){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(key.equals(filter)?burgundy:Color.DKGRAY);b.setOnClickListener(v->{filter=key;for(int i=0;i<row.getChildCount();i++)((Button)row.getChildAt(i)).setTextColor((i==0&&filter.equals("all")||i==1&&filter.equals("favorites")||i==2&&filter.equals("trash"))?burgundy:Color.DKGRAY);refresh();});row.addView(b,new LinearLayout.LayoutParams(0,48,1));}
    private void pickMedia(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_MEDIA);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PICK_MEDIA||result!=RESULT_OK||data==null)return;if(data.getClipData()!=null){for(int n=0;n<data.getClipData().getItemCount();n++)addUri(data.getClipData().getItemAt(n).getUri(),data);}else if(data.getData()!=null)addUri(data.getData(),data);save();refresh();}
    private void addUri(Uri uri,Intent data){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String s=uri.toString();if(!items.contains(s))items.add(s);trash.remove(s);}
    private void save(){prefs.edit().putStringSet("items",new HashSet<>(items)).putStringSet("favorites",new HashSet<>(favorites)).putStringSet("trash",new HashSet<>(trash)).apply();}
    private void refresh(){if(adapter!=null)adapter.notifyDataSetChanged();}
    private void showActions(String uri){String[] options=filter.equals("trash")?new String[]{"Restaurar","Eliminar definitivamente"}:new String[]{favorites.contains(uri)?"Quitar de favoritos":"Agregar a favoritos","Mover a papelera"};new android.app.AlertDialog.Builder(this).setTitle("LUMA Gallery").setItems(options,(d,which)->{if(filter.equals("trash")){if(which==0)trash.remove(uri);else{trash.remove(uri);items.remove(uri);favorites.remove(uri);}}else if(which==0){if(!favorites.add(uri))favorites.remove(uri);}else trash.add(uri);save();refresh();}).show();}
    private String nameOf(String value){try(Cursor c=getContentResolver().query(Uri.parse(value),null,null,null,null)){if(c!=null&&c.moveToFirst()){int ix=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(ix>=0)return c.getString(ix);}}catch(Exception ignored){}return "Archivo";}
    private class MediaAdapter extends BaseAdapter{
        private final ArrayList<String> shown=new ArrayList<>();private void build(){shown.clear();String q=search==null?"":search.getText().toString().toLowerCase();for(String s:items){boolean inTrash=trash.contains(s);if(filter.equals("trash")!=inTrash)continue;if(filter.equals("favorites")&&!favorites.contains(s))continue;if(!nameOf(s).toLowerCase().contains(q))continue;shown.add(s);}}
        public int getCount(){build();return shown.size();}public Object getItem(int p){build();return shown.get(p);}public long getItemId(int p){return p;}
        public View getView(int position,View convert,ViewGroup parent){build();String value=shown.get(position);ImageView image=convert instanceof ImageView?(ImageView)convert:new ImageView(MainActivity.this);int w=getResources().getDisplayMetrics().widthPixels/3;image.setLayoutParams(new GridView.LayoutParams(w,w));image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(Color.rgb(231,224,222));try{image.setImageBitmap(getContentResolver().loadThumbnail(Uri.parse(value),new Size(w,w),null));}catch(Exception e){image.setImageDrawable(null);}return image;}
    }
}
