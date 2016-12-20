package com.sarriaroman.PhotoViewer;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;
import com.squareup.picasso.Picasso;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PhotoActivity extends Activity {
	private PhotoViewAttacher mAttacher;
	private ImageView photo;
	private String imageUrl;
	private ImageButton closeBtn;
	private ImageButton shareBtn;
	private JSONObject options;
	private int shareBtnVisibility;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getApplication().getResources().getIdentifier("activity_photo", "layout", getApplication().getPackageName()));
		// Load the Views
		findViews();

		try {
			options = new JSONObject(this.getIntent().getStringExtra("options"));
			shareBtnVisibility = options.getBoolean("share") ? View.VISIBLE : View.INVISIBLE;
		} catch(JSONException exception) {
			shareBtnVisibility = View.VISIBLE;
		}
		shareBtn.setVisibility(shareBtnVisibility);

		// Change the Activity Title
		final String actTitle = this.getIntent().getStringExtra("title");
		imageUrl = this.getIntent().getStringExtra("url");

		// Set Button Listeners
		closeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		shareBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				HttpDownloader httpDownloader = new HttpDownloader();
				int result = httpDownloader.downFile(imageUrl, "Download/", actTitle);
				if(result==-1){
					Toast.makeText(getApplicationContext(), "儲存錯誤", Toast.LENGTH_LONG).show();
				}else if(result==0){
					Toast.makeText(getApplicationContext(), "儲存成功", Toast.LENGTH_LONG).show();
				}else if(result==1){
					Toast.makeText(getApplicationContext(), "儲存成功", Toast.LENGTH_LONG).show();
				}
			}
		});

		try {
			loadImage();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Find and Connect Views
	 *
	 */
	private void findViews() {
		// Buttons first
		closeBtn = (ImageButton) findViewById( getApplication().getResources().getIdentifier("closeBtn", "id", getApplication().getPackageName()) );
		shareBtn = (ImageButton) findViewById( getApplication().getResources().getIdentifier("shareBtn", "id", getApplication().getPackageName()) );
		// Photo Container
		photo = (ImageView) findViewById( getApplication().getResources().getIdentifier("photoView", "id", getApplication().getPackageName()) );
		mAttacher = new PhotoViewAttacher(photo);
		// Title TextView
		//titleTxt = (TextView) findViewById( getApplication().getResources().getIdentifier("titleTxt", "id", getApplication().getPackageName()) );
	}


	private void hideLoadingAndUpdate() {
		photo.setVisibility(View.VISIBLE);
        shareBtn.setVisibility(shareBtnVisibility);
		mAttacher.update();
	}

	private void loadImage() throws MalformedURLException {
		if( imageUrl.startsWith("http") ) {
		Picasso.with(this)
				.load(imageUrl)
				.resize(1024, 0)
				.onlyScaleDown()
				.into(photo, new com.squareup.picasso.Callback() {
					@Override
					public void onSuccess() {
						hideLoadingAndUpdate();
					}

					@Override
					public void onError() {
						Toast.makeText(getApplicationContext(), "Error loading image.", Toast.LENGTH_LONG).show();
						finish();
					}
				});
	} else if ( imageUrl.startsWith("data:image")){
            String base64String = imageUrl.substring(imageUrl.indexOf(",")+1);
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            photo.setImageBitmap(decodedByte);
            hideLoadingAndUpdate();
        } else {
            photo.setImageURI(Uri.parse(imageUrl));
            hideLoadingAndUpdate();
        }
	}
	
	public class HttpDownloader {
		private URL url = null;
		public String download(String urlStr) {
			StringBuffer sb = new StringBuffer();
			String line = null;
			BufferedReader buffer = null;
			try {
				url = new URL(urlStr);
				HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
				buffer = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
				while ((line = buffer.readLine()) != null) {
					sb.append(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					buffer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		}
		
		public int downFile(String urlStr, String path, String fileName) {
			InputStream inputStream = null;
			Log.i("................", "fileName");
			try {
				FileUtils fileUtils = new FileUtils();
				if (fileUtils.isFileExist(path + fileName)) {
					return 1;
				} else {
					inputStream = getInputStreamFromUrl(urlStr);
					File resultFile = fileUtils.write2SDFromInput(path,fileName, inputStream);
					if (resultFile == null) {
						return -1;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			} finally {
				try {
					inputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return 0;
		}
		
		public InputStream getInputStreamFromUrl(String urlStr) throws MalformedURLException, IOException {
			url = new URL(urlStr);
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			InputStream inputStream = urlConn.getInputStream();
			return inputStream;
		}
		
		public class FileUtils {
			private String SDPATH;
			public String getSDPATH() {
				return SDPATH;
			}
			public FileUtils() {
				//得到當前外部存放裝置的目錄	// /SDCARD
				SDPATH = Environment.getExternalStorageDirectory() + "/";
			}
			/*** 在SD卡上創建檔	* @throws IOException*/
			public File creatSDFile(String fileName) throws IOException {
				File file = new File(SDPATH + fileName);
				file.createNewFile();
				return file;
			}

			/*** 在SD卡上創建目錄* @param dirName*/
			public File creatSDDir(String dirName) {
				File dir = new File(SDPATH + dirName);
				dir.mkdirs();
				return dir;
			}

			/*** 判斷SD卡上的資料夾是否存在*/
			public boolean isFileExist(String fileName){
				File file = new File(SDPATH + fileName);
				return file.exists();
			}

			/*** 將一個InputStream裡面的資料寫入到SD卡中*/
			public File write2SDFromInput(String path,String fileName,InputStream input){
				File file = null;
				OutputStream output = null;
				try{
					creatSDDir(path);
					file = creatSDFile(path + fileName);
					output = new FileOutputStream(file);
					byte buffer [] = new byte[4 * 1024];
					while((input.read(buffer)) != -1){
						output.write(buffer);
					}
					output.flush();
				}
				catch(Exception e){
					e.printStackTrace();
				}
				finally{
					try{
						output.close();
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
				return file;
			}
		}		
	}	
}
