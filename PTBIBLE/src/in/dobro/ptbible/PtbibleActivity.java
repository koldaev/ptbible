package in.dobro.ptbible;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class PtbibleActivity extends Activity implements OnItemSelectedListener  {
	
	private int mSpinnerCount=2;

	private int mSpinnerInitializedCount=0;
	
	SharedPreferences spref;
	SharedPreferences loadspref;
	
	ArrayAdapter<String> adapter;
	ArrayAdapter<String> adapterglav;
	SQLiteDatabase db;
	DBHelper myDbHelper;
	String fortextview = "";
	TextView tv;
	Cursor cursor;
	
	 Integer jglav;

	 Handler h;
	 
	 Integer glavforhundler;
	 Integer currentbookforhundler;
	 ProgressDialog pd;
	 
	Spinner spinner;
	Spinner spinner2;
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ptbible);
		
		mSpinnerInitializedCount = 2;
		
		pd = new ProgressDialog(this);
		h = new Handler() {
		      public void handleMessage(android.os.Message msg) {
		    	 if (msg.what == glavforhundler) {
		    		 Toast.makeText(getApplicationContext(), "pasta de trabalho é salvo", Toast.LENGTH_SHORT).show();
		    		 pd.dismiss();
		    	 }
		    };
		};
		
		
		setTitle("Bíblia");
		
		tv = (TextView)findViewById(R.id.textView1);
		tv.setText("");
		tv.setPadding(10, 10, 10, 10);
		
		loadspref = getPreferences(MODE_PRIVATE);
	    Float textsize = loadspref.getFloat("fontsize", 0);
	    
	    if(textsize > 0) {
	    	tv.setTextSize(textsize);
	    	Toast.makeText(this, "Tamanho da fonte Loaded: " + textsize, Toast.LENGTH_SHORT).show();
	    } else {
	    	tv.setTextSize(18);
	    	Toast.makeText(this, "Aumentar tamanho da fonte: 18.0", Toast.LENGTH_SHORT).show();
	    }
		
		try {
			init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		touchinit();
		
	}


	
	private void touchinit() {
		
		final ScrollView sv = (ScrollView)findViewById(R.id.vscroll);
		
		sv.setLongClickable(true);
		
		registerForContextMenu(sv);
		
		sv.setOnTouchListener(new OnFlingGestureListener() {
				
	        @Override
	        public void onTopToBottom() {
	        	int intpos = sv.getScrollY() - 150;
	        	sv.scrollTo(0, intpos);
	        }

	        @Override
	        public void onRightToLeft() {
	        	sv.fullScroll(View.FOCUS_UP);
	        	Integer book = spinner.getSelectedItemPosition()+1;
	        	Integer glavaminus = spinner2.getSelectedItemPosition();
	        	if(glavaminus > 0) {
	        	bibletextglav(book,glavaminus);
	        	spinner2.setSelection(glavaminus-1);
	        	}
	        }

	        @Override
	        public void onLeftToRight() {
	        	sv.fullScroll(View.FOCUS_UP);
	        	Integer book = spinner.getSelectedItemPosition()+1;
	        	Integer chapters = (Integer)ptbibleproperties.ptbiblechapters.get("ptbible"+book);
	        	Integer glavaplus = spinner2.getSelectedItemPosition()+2;
	        	if((glavaplus-1) < chapters) {
	        	bibletextglav(book,glavaplus);
	        	spinner2.setSelection(glavaplus-1);
	        	}
	        }

	        @Override
	        public void onBottomToTop() {
	        	int intpos = sv.getScrollY() + 150;
	        	sv.scrollTo(0, intpos);
	        }
	     });
	}


	private void init() throws IOException {		
		
		//Button fav = (Button) findViewById(R.id.button1);
		//fav.setOnClickListener(l)
		
		spinner = (Spinner) findViewById(R.id.spinner1);
		spinner2 = (Spinner) findViewById(R.id.spinner2);
		
		myDbHelper = new DBHelper(getApplicationContext(), "ptbible4.jpeg", 23);
		
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ptbibleproperties.ptbiblenames);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 
	     spinner.setAdapter(adapter);
	     spinner.setPrompt("Novo Testamento");
	     
	     spinner.setOnItemSelectedListener(this);
	      
	}

	public void bibletext(int i, int ch) {
		
		Integer chapters = (Integer)ptbibleproperties.ptbiblechapters.get("ptbible"+i);
 		combochapters(i,chapters);
		
		fortextview = "";

		tv.setText("");
		
		try {
		    db = myDbHelper.getWritableDatabase();
		}
		catch (SQLiteException ex){
		    db = myDbHelper.getReadableDatabase();
		}
		finally{

		cursor = db.rawQuery("select * from pttext where bible = " + i + " and chapter = " + ch , null);

 		while(cursor.moveToNext()){
 			fortextview += cursor.getString(cursor.getColumnIndex("poem")) + ".&nbsp;" + cursor.getString(cursor.getColumnIndex("poemtext")) + "<br>" ;
 		}
 		
 		if (cursor != null)
	        cursor.moveToFirst();
 		
 		tv.setText(Html.fromHtml(fortextview));
 		
		}
		
		if(i < 40) {
			setTitle("Velho Testamento");
		} else {
			setTitle("Novo Testamento");
		}
		
		final ScrollView sv = (ScrollView)findViewById(R.id.vscroll);
		sv.scrollTo(0, 0);
		
		myDbHelper.close();
		cursor.close();
		
	}


	private void combochapters(final Integer book, Integer chapters) {
		
		String ptbibleglaves[] = new String[chapters];
		String glavname;
		
		if(chapters != 150) {
			glavname = "capítulo ";
		} else {
			glavname = "SALMO ";
		}
		
		ptbibleglaves[0] = glavname + "1";
		for(int a=1;a<chapters;a++) {
			ptbibleglaves[a] = glavname + (a+1);
		}
		
		adapterglav = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ptbibleglaves);
		adapterglav.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 
	     spinner2.setAdapter(adapterglav);
	     spinner2.setPrompt("Selecione o capítulo");
	     spinner2.setOnItemSelectedListener(this);
	     
	     
	}
	
	public void bibletextglav(int i, int ch) {
				
		fortextview = "";

		tv.setText("");
		
		try {
		    db = myDbHelper.getWritableDatabase();
		}
		catch (SQLiteException ex){
		    db = myDbHelper.getReadableDatabase();
		}
		finally{

		cursor = db.rawQuery("select * from pttext where bible = " + i + " and chapter = " + ch , null);

 		while(cursor.moveToNext()){
 			fortextview += cursor.getString(cursor.getColumnIndex("poem")) + ".&nbsp;" + cursor.getString(cursor.getColumnIndex("poemtext")) + "<br>" ;
 		}
 		
 		if (cursor != null)
	        cursor.moveToFirst();
 		
 		tv.setText(Html.fromHtml(fortextview));
 		
		}
		
		if(i < 40) {
			setTitle("Velho Testamento");
		} else {
			setTitle("Novo Testamento");
		}
		
		cursor.close();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_ptbible, menu);
		
		menu.clear();
		
		menu.add(0, 1, 1, "Salvar esta página");
		menu.add(0, 2, 2, "Baixe a página");
		menu.add(1, 3, 3, "Salve capítulo");
		menu.add(1, 4, 4, "Guarde o livro");
		menu.add(1, 5, 5, "manter a base");
		menu.add(1, 6, 6, "fonte menor");
		menu.add(1, 7, 7, "fonte grande");
		
		return super.onCreateOptionsMenu(menu);
	}
	
	 public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId()) {
		 
		 	case 1:
		 		
		 		addbookmark();
		 	
		 	break;
		 	
		 	case 2:

				loadbookmark();
			 	
			break;
			
		 	case 3:
		 		Savetext();
			break;
				
		 	case 4:
		 		currentbookforhundler = spinner.getSelectedItemPosition()+1;
				glavforhundler = (Integer)ptbibleproperties.ptbiblechapters.get("ptbible"+currentbookforhundler);

				String bookname = ptbibleproperties.ptbiblenames[currentbookforhundler-1];
		    	  
		 	      pd.setTitle(bookname);
		 	      pd.setIndeterminate(true);
		 	      pd.setInverseBackgroundForced(true);
		 	      pd.setCancelable(false);
		 	      pd.setCanceledOnTouchOutside(false);
		 	      pd.setMessage("É exportado para .Txt \r\nPor favor, aguarde ...");
		 	      pd.show();

		 		Thread t = new Thread(new Runnable() {
		 	        public void run() {
		 		booktotext();
		 	       }
		 	      });
		 	      t.start();
			break;
		 	
		 	case 5:
		 		savebase();
			break;
			
		 	case 6:
		 		fontminus();
			break;
				
		 	case 7:
			 	fontplus();
			break;
		 
		 }
		 
	      return super.onOptionsItemSelected(item);
	 }
	
	private void booktotext() {
		
		
		
		try {
		    db = myDbHelper.getWritableDatabase();
		}
		catch (SQLiteException ex){
		    db = myDbHelper.getReadableDatabase();
		}
		finally{
		
			Integer currentbook = spinner.getSelectedItemPosition()+1;
			Integer chapters = (Integer)ptbibleproperties.ptbiblechapters.get("ptbible"+currentbook);
			
			String bookname = ptbibleproperties.ptbiblenames[currentbook-1];
			
		fortextview = "";
		
		fortextview += "\r\n";
		fortextview += bookname;
		fortextview += "\r\n";
		
		for(int i=1;i<=chapters;i++) {
			cursor = db.rawQuery("select * from pttext where bible = " + currentbook + " and chapter = " + i, null);
			
			if(chapters != 150)
				fortextview += "\r\ncapítulo " + i + "\r\n\r\n";
			else
				fortextview += "\r\nSALMO " + i + "\r\n\r\n";

	 		while(cursor.moveToNext()){
	 			fortextview += cursor.getString(cursor.getColumnIndex("poem")) + ". " + cursor.getString(cursor.getColumnIndex("poemtext")) + "\r\n";
	 		}

	 		
	 		if (cursor != null)
		        cursor.moveToFirst();
	 		
	 		h.sendEmptyMessage(i);
	 		
		}
		
		File root = android.os.Environment.getExternalStorageDirectory();
		
		File dir = new File(root.getAbsolutePath() + "/ptbible");
	    dir.mkdirs();
	    String filenametosave = "book" + (spinner.getSelectedItemPosition()+1) + ".txt";
	    File file = new File(dir, filenametosave);
		
	    try {
	        FileOutputStream f = new FileOutputStream(file);
	        PrintWriter pw = new PrintWriter(f);
	        pw.println(fortextview);
	        pw.flush();
	        pw.close();
	        f.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
		String textfortoast = "O livro é mantido em " + dir + "/"+filenametosave;
		//Toast.makeText(this, textfortoast, Toast.LENGTH_SHORT).show();
				 
		
		cursor.close();
		myDbHelper.close();
		
		/*
		cursor = db.rawQuery("select * from pttext where bible = " + currentbook, null);

 		while(cursor.moveToNext()){
 			fortextview += cursor.getString(cursor.getColumnIndex("poem")) + ".&nbsp;" + cursor.getString(cursor.getColumnIndex("poemtext")) + "<br>" ;
 		}
 		
 		if (cursor != null)
	        cursor.moveToFirst();
 		
 		tv.setText(Html.fromHtml(fortextview));
 		*/
		}
		
		final ScrollView sv = (ScrollView)findViewById(R.id.vscroll);
		sv.scrollTo(0, 0);
		
		myDbHelper.close();
		cursor.close();

	}



	private void fontminus() {
			Float currentsize = tv.getTextSize();
			Float sizeminus = currentsize - 2;
			tv.setTextSize(sizeminus);
			spref = getPreferences(MODE_PRIVATE);
		    Editor ed = spref.edit();
		    ed.putFloat("fontsize", sizeminus);
		    ed.commit();
		    Toast.makeText(this, "Novo tamanho da fonte: " + sizeminus, Toast.LENGTH_SHORT).show();
	}



	private void fontplus() {
			Float currentsize = tv.getTextSize();
			Float sizeplus = currentsize + 2;
			tv.setTextSize(sizeplus);
			spref = getPreferences(MODE_PRIVATE);
			Editor ed = spref.edit();
			ed.putFloat("fontsize", sizeplus);
			ed.commit();
			Toast.makeText(this, "Novo tamanho da fonte: " + sizeplus, Toast.LENGTH_SHORT).show();
	}



	@SuppressLint("NewApi")
	private void loadbookmark() {
		loadspref = getPreferences(MODE_PRIVATE);
	    Integer book = loadspref.getInt("book", 1);
	    Integer glav = loadspref.getInt("glav", 1);
	    
	    bibletext(book, glav);

	    mSpinnerInitializedCount = 1;
	    spinner.setSelection(book-1);
	    
	    mSpinnerInitializedCount = 1;
	    spinner2.setSelection(glav-1);
	    
	}


	private void addbookmark() {
		Integer book = spinner.getSelectedItemPosition()+1;
    	Integer glavaplus = spinner2.getSelectedItemPosition()+1;
		
		spref = getPreferences(MODE_PRIVATE);
	    Editor ed = spref.edit();
	    ed.putInt("book", book);
	    ed.commit();
	    ed.putInt("glav", glavaplus);
	    ed.commit();
		Toast.makeText(this, "página salva", Toast.LENGTH_SHORT).show();
	}

	private void savebase() {
		
		File root = android.os.Environment.getExternalStorageDirectory();
		File dir = new File(root.getAbsolutePath() + "/ptbible/");
	    dir.mkdirs();
	    
	    String forbasepath = dir + "/ptbible.db";
	    
	    copyAssets(forbasepath);

	}


	
	private void copyAssets(String forbasepath) {
	    AssetManager assetManager = getAssets();

	        InputStream in = null;
	        OutputStream out = null;
	        try {
	          in = assetManager.open("ptbible4.jpeg");
	          out = new FileOutputStream(forbasepath);
	          copyFile(in, out);
	          in.close();
	          in = null;
	          out.flush();
	          out.close();
	          out = null;
	          
	        String textfortoast = "Banco de dados armazenados em SQLite " + forbasepath;
	  		Toast.makeText(this, textfortoast, Toast.LENGTH_SHORT).show();
	          
	        } catch(IOException e) {
	            Log.e("tag", "Failed to copy asset file: ptbible4.jpeg to " + forbasepath, e);
	        }       

	}
	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}

	@Override
	public void onBackPressed() {

	    //Toast.makeText(this, "Да любите друг друга!", Toast.LENGTH_SHORT).show();
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		

		if(arg0 == spinner) {
			Integer i = arg0.getSelectedItemPosition()+1;
			if (mSpinnerInitializedCount < mSpinnerCount) {
					
			} else {
				bibletext(i,1);
			}
   	 		
		} else if(arg0 == spinner2) {
			Integer book = spinner.getSelectedItemPosition()+1;
			jglav = arg0.getSelectedItemPosition()+1;
			if (mSpinnerInitializedCount < mSpinnerCount) {
				
			} else {
				bibletextglav(book,jglav);
			}
		}
		
		mSpinnerInitializedCount = 3;

	}



	@Override
	public void onNothingSelected(AdapterView<?> arg0) {

	}
	
	void Savetext() {
		
		String input = tv.getText().toString(); 

		File root = android.os.Environment.getExternalStorageDirectory();
		
		File dir = new File(root.getAbsolutePath() + "/ptbible");
	    dir.mkdirs();
	    String filenametosave = "book" + (spinner.getSelectedItemPosition()+1) 
	    		+ "_chapter" + (spinner2.getSelectedItemPosition()+1) + ".txt";
	    File file = new File(dir, filenametosave);
		
	    try {
	        FileOutputStream f = new FileOutputStream(file);
	        PrintWriter pw = new PrintWriter(f);
	        pw.println(input);
	        pw.flush();
	        pw.close();
	        f.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
		
		String textfortoast = "O texto é armazenado em " + dir + "/"+filenametosave;
		Toast.makeText(this, textfortoast, Toast.LENGTH_SHORT).show();
				    
	}
	


}
