package ca.uwaterloo.cs349.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import android.widget.Toast;



@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";
    // drawing path
    public ArrayList<Draw> paths = new ArrayList();
    public ArrayList<Draw> undo = new ArrayList();
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    public int strokewidth = 6;
    public int paintColor = Color.BLUE;
    public boolean isErase = false;
    Draw eraseDraw;
    boolean last_action_is_erase = false;
    // image to display
    Bitmap bitmap;
    int width, height;


    private Canvas mCanvas;
    public Path mPath;
    public Paint mPaint;

    public PDFimage(Context context) {
        super(context);

        setFocusable(true);
        setFocusableInTouchMode(true);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(paintColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(strokewidth);
        mCanvas = new Canvas();
        mPath = new Path();

    }


    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        for (Draw draw : paths){
            mPaint.setColor(draw.color);
            mPaint.setStrokeWidth(draw.strokeWidth);
            canvas.drawPath(draw.path, mPaint);
        }
        canvas.drawPath(mPath, mPaint);
        super.onDraw(canvas);
    }

    private void touch_start(float x, float y) {

        if (!isErase) {
            Draw draw = new Draw(paintColor, strokewidth, mPath);
            paths.add(draw);
        }
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }

    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        if (isErase) checkIntersect();
        else last_action_is_erase = false;
        mPath = new Path();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void undo () {
        if (last_action_is_erase) {
            paths.add(undo.remove(undo.size()-1));
        } else {
            undo.add(paths.remove(paths.size() - 1));
        }
        invalidate();
    }

    public void redo () {
        if (last_action_is_erase) {
            undo.add(paths.remove(paths.size() - 1));
        } else {
            paths.add(undo.remove(undo.size() - 1));
        }
        invalidate();
    }

    public void checkIntersect() {
        Region region1 = new Region();
        Path pathNew = eraseDraw.path;
        Region clip = new Region(0, 0, width, height);
        region1.setPath(pathNew, clip);

        for (Draw draw: paths) {
            Region region2 = new Region();
            region2.setPath(draw.path, clip);
            if (region1.op(region2,Region.Op.INTERSECT)) {
                undo.add(draw);
                paths.remove(draw);
                last_action_is_erase = true;
                break;
            }
        }
    }
}
