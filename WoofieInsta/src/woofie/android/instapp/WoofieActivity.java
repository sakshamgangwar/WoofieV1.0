package woofie.android.instapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import neuroph.android.example.R;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class WoofieActivity extends Activity implements OnTouchListener {

	private final int SELECT_PHOTO = 1;
	private final int LOADING_DATA_DIALOG = 2;
	private final int RECOGNIZING_IMAGE_DIALOG = 3;

	private TextView txtAnswer;
	private ScrollView scrollView;
	private Button button;
	private Button showButton;
	private ImageView v1;
	private ImageView v2;
	private ImageView v3;
	private RelativeLayout screen;
	
	private String imgPath = null;
	private String hashTags = null;

	private Bitmap bitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		txtAnswer = (TextView) findViewById(R.id.txtAnswer);
		screen = (RelativeLayout) findViewById(R.id.screen);
		button = (Button) findViewById(R.id.btn);
		showButton = (Button) findViewById(R.id.btn1);
		scrollView = (ScrollView) findViewById(R.id.scrollView1);
		v1 = (ImageView) findViewById(R.id.imageView1);
		v2 = (ImageView) findViewById(R.id.imageView2);
		v3 = (ImageView) findViewById(R.id.imageView3);
		button.setVisibility(View.INVISIBLE);
		scrollView.setVisibility(View.INVISIBLE);
		showButton.setVisibility(View.INVISIBLE);
		v1.setVisibility(View.INVISIBLE);
		v2.setVisibility(View.INVISIBLE);
		v3.setVisibility(View.INVISIBLE);
		screen.setOnTouchListener(this);

	}

	public String getImageRecognized(String filePath) {
		String result = null;
		try {

			ClassLoader classLoader = WoofieActivity.class.getClassLoader();
			URL resource = classLoader
					.getResource("com/mashape/unirest/http/Unirest.class");
			System.out.println(resource);

			HttpResponse<JsonNode> response = Unirest
					.post("https://camfind.p.mashape.com/image_requests")
					.header("X-Mashape-Key",
							"O0WzJhllX9mshPaatJRhjhZUVuz7p1v94IgjsnBv9SUCKSvGVI")
					.field("focus[x]", "480").field("focus[y]", "640")
					.field("image_request[altitude]", "27.912109375")
					.field("image_request[image]", new File(filePath))
					.field("image_request[language]", "en")
					.field("image_request[latitude]", "35.8714220766008")
					.field("image_request[locale]", "en_US")
					.field("image_request[longitude]", "14.3583203002251")
					.asJson();

			JsonNode body = response.getBody();
			String token = body.getObject().get("token").toString();

			response = Unirest
					.get("https://camfind.p.mashape.com/image_responses/"
							+ token)
					.header("X-Mashape-Key",
							"O0WzJhllX9mshPaatJRhjhZUVuz7p1v94IgjsnBv9SUCKSvGVI")
					.header("Accept", "application/json").asJson();

			while (response.getBody().getObject().get("status").toString()
					.equals("not completed")) {
				try {
					Thread.sleep(100);
					response = Unirest
							.get("https://camfind.p.mashape.com/image_responses/"
									+ token)
							.header("X-Mashape-Key",
									"O0WzJhllX9mshPaatJRhjhZUVuz7p1v94IgjsnBv9SUCKSvGVI")
							.header("Accept", "application/json").asJson();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (response.getBody().getObject().get("status").toString()
					.equals("completed")) {
				result = response.getBody().getObject().get("name").toString();
			}

		} catch (Exception e) {
		}
		return result;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

		switch (requestCode) {
		case SELECT_PHOTO:
			if (resultCode == RESULT_OK) {
				try {
					Uri selectedImage = imageReturnedIntent.getData();
					// get file path of selected image
					String filePath = getRealPathFromURI(selectedImage);

					InputStream imageStream = getContentResolver()
							.openInputStream(selectedImage);
					bitmap = Bitmap.createBitmap(BitmapFactory
							.decodeStream(imageStream));
					// show image
					Drawable drawable = new BitmapDrawable(getResizedBitmap(bitmap, 300, 300)); 
					
					txtAnswer.setCompoundDrawablesWithIntrinsicBounds(null,
							drawable, null, null);
					// show image name
					String tags = getImageRecognized(filePath);
					
					
					txtAnswer.setText("Tags Found: "
							+ tags);
					
					imgPath = filePath;
					hashTags = tags;
					
					button.setVisibility(View.VISIBLE);
					showButton.setVisibility(View.VISIBLE);
					
					

					
					
				} catch (FileNotFoundException fnfe) {
					Log.d("Neuroph Android Example", "File not found");
				}
			}
		}
	}
	
	public void showSuggestions(View view){
		if(scrollView.getVisibility()==View.INVISIBLE){
			scrollView.setVisibility(View.VISIBLE);
			v1.setVisibility(View.VISIBLE);
			v2.setVisibility(View.VISIBLE);
			v3.setVisibility(View.VISIBLE);
		}else{
			scrollView.setVisibility(View.INVISIBLE);
			v1.setVisibility(View.INVISIBLE);
			v2.setVisibility(View.INVISIBLE);
			v3.setVisibility(View.INVISIBLE);
		}
		
	}
	
	public void sendMessage(View view) {
		String type = "image/*";
		String mediaPath = imgPath;
		createInstagramIntent(type, mediaPath, hashTags);
		
	}
	private void createInstagramIntent(String type, String mediaPath, String caption){

	    // Create the new Intent using the 'Send' action.
	    Intent share = new Intent(Intent.ACTION_SEND);

	    // Set the MIME type
	    share.setType(type);

	    // Create the URI from the media
	    File media = new File(mediaPath);
	    Uri uri = Uri.fromFile(media);

	    // Add the URI and the caption to the Intent.
	    share.putExtra(Intent.EXTRA_STREAM, uri);
	    share.putExtra(Intent.EXTRA_TEXT, caption);

	    // Broadcast the Intent.
	    startActivity(Intent.createChooser(share, "Share to"));
	}
	
	public Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {

		int width = bm.getWidth();

		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;

		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation

		Matrix matrix = new Matrix();

		// resize the bit map

		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap

		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);

		return resizedBitmap;

	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Intent imageIntent = new Intent(Intent.ACTION_PICK);
		imageIntent.setType("image/*");
		// show gallery
		startActivityForResult(imageIntent, SELECT_PHOTO);
		return false;
	}

	public String getRealPathFromURI(Uri contentUri) {

		// converts uri to file path, converts /external/images/media/9 to
		// /sdcard/neuroph/fish.jpg
		String[] projection = { MediaColumns.DATA };
		Cursor cursor = managedQuery(contentUri, projection, null, null, null);
		int columnIndex = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
		cursor.moveToFirst();

		return cursor.getString(columnIndex);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog dialog;

		if (id == LOADING_DATA_DIALOG) {
			dialog = new ProgressDialog(this);
			dialog.setTitle("My Woofie");
			dialog.setMessage("Loading data...");
			dialog.setCancelable(false);

			return dialog;
		} else if (id == RECOGNIZING_IMAGE_DIALOG) {
			dialog = new ProgressDialog(this);
			dialog.setTitle("My Woofie");
			dialog.setMessage("Recognizing image...");
			dialog.setCancelable(false);

			return dialog;
		}
		return null;
	}

}