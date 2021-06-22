package ca.uwaterloo.cs349.pdfreader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;




// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so read this carefully.

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;
    public boolean isOnHighlightMode = false;
    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    Button previous;
    Button next;

    int width, height;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    public ArrayList<ArrayList<Draw>> page_path = new ArrayList<>();
    public ArrayList<ArrayList<Draw>> page_undo = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);

        pageImage = new PDFimage(this);
        pageImage.width = layout.getMeasuredWidth();
        pageImage.height = layout.getMeasuredHeight();
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        previous.setOnClickListener(this);
        next.setOnClickListener(this);



        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(0);
            //closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo_button:
                if (((pageImage.paths.size() > 0) && (!pageImage.last_action_is_erase)) ||
                        ((pageImage.undo.size() > 0) && pageImage.last_action_is_erase)) {
                    pageImage.undo();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Nothing to undo", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

                return true;
            case R.id.redo_button:
                if (((pageImage.undo.size() > 0)  && (!pageImage.last_action_is_erase)) ||
                        ((pageImage.paths.size() > 0) && pageImage.last_action_is_erase)){
                    pageImage.redo();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "Nothing to redo", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_pen:
                if (pageImage.isErase) pageImage.isErase = false;
                if (!isOnHighlightMode) {
                    isOnHighlightMode = true;
                    pageImage.invalidate();
                    pageImage.strokewidth = 30;
                    pageImage.paintColor = Color.YELLOW;
                    pageImage.mPaint.setStrokeWidth(30);
                    pageImage.mPaint.setColor(Color.YELLOW);
                    Toast toast = Toast.makeText(getApplicationContext(), "Highlighter mode on.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    isOnHighlightMode = false;
                    pageImage.invalidate();
                    pageImage.strokewidth = 6;
                    pageImage.paintColor = Color.BLUE;
                    pageImage.mPaint.setStrokeWidth(6);
                    pageImage.mPaint.setColor(Color.BLUE);
                    Toast toast = Toast.makeText(getApplicationContext(), "Annotation mode on.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                return true;
            case R.id.action_eraser:

                pageImage.isErase = true;
                pageImage.eraseDraw = new Draw(Color.TRANSPARENT, 50, pageImage.mPath);
                Toast toast = Toast.makeText(getApplicationContext(), "Eraser mode on.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                

                return true;


        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            int count = 0;
            while (count < pdfRenderer.getPageCount()) {
                page_path.add(null);
                page_undo.add(null);
                count++;
            }
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        setTitle(getString(R.string.app_name_with_index,
                currentPage.getIndex() + 1, pdfRenderer.getPageCount()));
        if (page_path.get(index) == null) {
            pageImage.paths = new ArrayList<>();
            pageImage.undo = new ArrayList<>();
        } else {
            pageImage.paths = page_path.get(index);
            pageImage.undo = page_undo.get(index);
        }
        // Display the page
        pageImage.setImage(bitmap);
    }

    void showPrevious() {
        if (pdfRenderer == null || currentPage == null) {
            return;
        }
        final int index = currentPage.getIndex();
        if (index > 0) {
            if (page_path.get(index) == null) {
                page_path.add(index, pageImage.paths);
                page_undo.add(index, pageImage.undo);
            } else {
                page_path.set(index, pageImage.paths);
                page_undo.set(index, pageImage.undo);
            }
            showPage(index - 1);
        }
    }

    void showNext() {
        if (pdfRenderer == null || currentPage == null) {
            return;
        }
        final int index = currentPage.getIndex();
        if (index + 1 < pdfRenderer.getPageCount()) {
            if (page_path.get(index) == null) {
                page_path.add(index, pageImage.paths);
                page_undo.add(index, pageImage.undo);
            } else {
                page_path.set(index, pageImage.paths);
                page_undo.set(index, pageImage.undo);
            }
            showPage(index + 1);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.previous){
            showPrevious();
        }
        if(v.getId()==R.id.next){
            showNext();
        }

    }
}
