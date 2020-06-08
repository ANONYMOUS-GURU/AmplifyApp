package com.example.amplifyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.IdentifiedText;
import com.amplifyframework.predictions.models.Polygon;
import com.amplifyframework.predictions.models.Selection;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult;
import com.amplifyframework.predictions.result.IdentifyTextResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_GALLERY_IMAGE = 0;
    static final int PERMISSION_CODE_GALLERY = 2;
    static final int PERMISSION_CODE_CAMERA = 3;
    AtomicReference<String> text = new AtomicReference<>("$$$");
    ArrayList<myPair> bookNameAndCode = new ArrayList<>();
    AtomicReference<IdentifyDocumentTextResult> document = new AtomicReference<>();

    AtomicReference<IdentifyTextResult> documentPlain = new AtomicReference<>();

    private SharedPreferences mPrefs;
    private static final String PREFS_NAME = "PrefsFile";

    Uri image_uri;
    Bitmap imageBitmap;
    Button btn1, btn2, btn3, btn4;
    TextView textView, textView2, textView3, textView4, textView5, textView6;
    ImageView imageView;
    ImageButton imageButton;
    Canvas canvas;
    Paint paint;
    Bitmap tempBitmap;
    double costThreshold=0.35;
    double nextLineConstraint = 0.02;
    double fractionNextLine=0.38;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        textView = findViewById(R.id.txt1);
        textView2 = findViewById(R.id.txt2);
        textView3 = findViewById(R.id.txt3);
        textView4 = findViewById(R.id.txt4);
        textView5 = findViewById(R.id.txt5);
        textView6 = findViewById(R.id.txt6);
        imageView = findViewById(R.id.img1);
        btn1 = findViewById(R.id.snap);
        btn2 = findViewById(R.id.getText);
        btn3 = findViewById(R.id.getCounter);
        btn4 = findViewById(R.id.getPlain);
        imageButton = findViewById(R.id.rotate_image);
        imageButton.setEnabled(false);
        btn2.setEnabled(false);

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage(MainActivity.this);
            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Counter.class);
                startActivity(i);
            }
        });
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotate_image_bitmap(90);
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectText();
                increaseCounter();
                while (text.get().equals("$$$")) {
                    Log.i("MyAmplifyApp", "Wait .. ");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                text.set("$$$");

                tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tempBitmap);
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                List<BoundedKeyValue> keyValueList = document.get().getKeyValues();
                List<IdentifiedText> identifiedTextList = document.get().getLines();
                List<Table> tableList = document.get().getTables();
                List<IdentifiedText> wordsList = document.get().getWords();

                String keyValues = "keyValue - { ";
                for (int i = 0; i < keyValueList.size(); i++) {
                    if (i < keyValueList.size() - 1) {
                        keyValues = keyValues + "{ Key -> " + keyValueList.get(i).getKey() + " | Value = " + keyValueList.get(i).getKeyValue() + " } , ";
                    } else
                        keyValues = keyValues + "{ Key -> " + keyValueList.get(i).getKey() + " | Value = " + keyValueList.get(i).getKeyValue() + " } }";
                }
                Log.i("FullText", keyValues);

                String lines = "Lines - { ";
                for (int i = 0; i < identifiedTextList.size(); i++) {
                    if (i != identifiedTextList.size() - 1)
                        lines = lines + identifiedTextList.get(i).getText() + " \n ";
                    else
                        lines = lines + identifiedTextList.get(i).getText() + " }";
                }

                Log.i("FullText", lines);

                String table = "tables -->  [ ";
                for (int i = 0; i < tableList.size(); i++) {
                    table = table + "{ Row Size = " + tableList.get(i).getRowSize() + " Column size = " + tableList.get(i).getColumnSize() + " Cells --> ";
                    for (int j = 0; j < tableList.get(i).getCells().size(); j++) {
                        table = table + "( " + tableList.get(i).getCells().get(j).getText() + " ) ";
                    }
                    table = table + " }";
                }
                table = table + " ]";
                Log.i("FullText", table);

                String words = "Words-  { ";
                for (int i = 0; i < wordsList.size(); i++) {
                    if (i != wordsList.size() - 1)
                        words = words + wordsList.get(i).getText() + " , ";
                    else
                        words = words + wordsList.get(i).getText() + "}";
                }
                Log.i("FullText", words);

                checkForOrientation(wordsList, identifiedTextList,"ALL");

                textView.setText(keyValues);
                textView2.setText(lines);
                textView3.setText(words);
                textView4.setText(table);
            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectTextPlain();
                increaseCounterPlain();
                while (text.get().equals("$$$")) {
                    Log.i("MyAmplifyApp", "Wait .. ");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                text.set("$$$");

                tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                canvas = new Canvas(tempBitmap);
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                List<IdentifiedText> identifiedTextList = documentPlain.get().getLines();
                List<IdentifiedText> identifiedWords = documentPlain.get().getWords();

                String lines = "Lines - { ";
                for (int i = 0; i < identifiedTextList.size(); i++) {

                    if (i != identifiedTextList.size() - 1)
                        lines = lines + identifiedTextList.get(i).getText() + " \n ";
                    else
                        lines = lines  + identifiedTextList.get(i).getText() + " }";
                    // getBox -- left,bottom, right, top
                }
                Log.i("FullText", lines);

                String words = "Words - { ";
                for (int i = 0; i < identifiedWords.size(); i++) {

                    if (i != identifiedWords.size() - 1)
                        words = words + identifiedWords.get(i).getText() + " , ";
                    else
                        words = words + identifiedWords.get(i).getText() + "}";
                }
                Log.i("FullText", words);

                checkForOrientation(identifiedWords, identifiedTextList,"PLAIN");

                textView.setText("Not Applicable");
                textView2.setText(lines);
                textView3.setText(words);
                textView4.setText("Not Applicable");
            }
        });

        bookNameAndCode.add(new myPair("Hamlet", 0));
        bookNameAndCode.add(new myPair("Ways of Seeing", 1));
        bookNameAndCode.add(new myPair("The Last Man Vol. 3 One Small Step", 2));
        bookNameAndCode.add(new myPair("Canterville Ghost", 3));
        bookNameAndCode.add(new myPair("Romeo and Juliet", 4));
        bookNameAndCode.add(new myPair("Drawing on the Right side of the brain", 5));
        bookNameAndCode.add(new myPair("The Notebook", 6));
        bookNameAndCode.add(new myPair("History of Art", 7));
        bookNameAndCode.add(new myPair("The Notebook 6", 8));
    }

    private void checkForOrientation(List<IdentifiedText> wordsList, List<IdentifiedText> lines,String type) {
        double wordLength=getAverageLengthOfWords(wordsList);
        getMedianSlopeOfText(lines,wordLength);
        if(type.equals("ALL"))
        {
            detectText();
            increaseCounter();
            while (text.get().equals("$$$")) {
                Log.i("Slope", "Wait For new image.. ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            text.set("$$$");

            tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(tempBitmap);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            lines=document.get().getLines();
            wordsList=document.get().getWords();
        }
        else
        {
            detectTextPlain();
            increaseCounterPlain();
            while (text.get().equals("$$$")) {
                Log.i("Slope", "Wait For new image.. ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            text.set("$$$");

            tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(tempBitmap);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            lines=documentPlain.get().getLines();
            wordsList=documentPlain.get().getWords();
        }
        setNextLineConstraint(lines);
        ArrayList<IdentifiedText> words = new ArrayList<>();
        for (int i = 0; i < wordsList.size(); i++) {
            words.add(wordsList.get(i));
        }
        convertToOrder(words);
    }

    private double getAverageLengthOfWords(List<IdentifiedText> wordsList) {
        double length=0;
        for(int i=0;i<wordsList.size();i++)
        {
            length=length+Math.abs(wordsList.get(i).getBox().left-wordsList.get(i).getBox().right);
        }
        return length/wordsList.size();
    }

    private void increaseCounterPlain() {
        if (mPrefs.contains("count_PLAIN")) {
            int countAll;
            countAll = Integer.parseInt(mPrefs.getString("count_PLAIN", "7")) + 1;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("count_PLAIN", String.valueOf(countAll));
            editor.apply();
        } else {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("count_PLAIN", "7");
            editor.apply();
        }
    }
    private void increaseCounter() {
        if (mPrefs.contains("count_ALL")) {
            int countAll;
            countAll = Integer.parseInt(mPrefs.getString("count_ALL", "23")) + 1;
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("count_ALL", String.valueOf(countAll));
            editor.apply();
        } else {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("count_ALL", "23");
            editor.apply();
        }
    }
    public void detectText() {
        Amplify.Predictions.identify(
                TextFormatType.ALL,
                imageBitmap,
                result -> {
                    IdentifyDocumentTextResult identifyResult = (IdentifyDocumentTextResult) result;
                    document.set(identifyResult);
                    text.set(identifyResult.getFullText());
                },
                error -> Log.e("MyAmplifyApp", "Identify failed", error)
        );
    }
    public void detectTextPlain() {
        Amplify.Predictions.identify(
                TextFormatType.PLAIN,
                imageBitmap,
                result -> {
                    IdentifyTextResult identifyResult = (IdentifyTextResult) result;
                    documentPlain.set(identifyResult);
                    text.set(identifyResult.getFullText());
                },
                error -> Log.e("MyAmplifyApp", "Identify failed", error)
        );
    }

    private void setNextLineConstraint(List<IdentifiedText> lines) {
        double separation = 0;
        int lineCount = 0;
        if (lines.size() != 0) {
            for (int i = 0; i < lines.size(); i++) {
                separation += -(lines.get(i).getBox().top - lines.get(i).getBox().bottom);
                Log.v("nextLineConstraint", "sep = " + separation);
                lineCount += 1;
            }
            separation =  (separation / (1.0 * lineCount));
            nextLineConstraint = (separation * fractionNextLine);
            Log.v("nextLineConstraint", "value =  " + nextLineConstraint);
        }
    }
    private void convertToOrder(ArrayList<IdentifiedText> wordsList) {

        ArrayList<myPair> bookAndQuantity = new ArrayList<>();
        // Solution to part 1
        ArrayList<ArrayList<IdentifiedText>> myLines = getAllLines(wordsList);

        // Seeing results in the below for loop
        String finalText = "";
        String line = "";
        for (int i = 0; i < myLines.size(); i++) {
            for (int j = 0; j < myLines.get(i).size(); j++) {
                line = line + myLines.get(i).get(j).getText() + " ";
            }
            finalText = finalText + line + "\n";
            Log.v("Cony ", "line " + i + " = " + line);
            line = "";
        }
        textView5.setText(finalText);
        Log.v("FullText","Modified Lines = "+finalText);
        // After results to part 1


        // Solution to part 2
        for (int i = 0; i < myLines.size(); i++) {
            int quantity = 0;
            String name = "";
            LineContents lineContents = getSeparated(myLines.get(i));
            // return 1 for withNum makes sense 2 for withoutNum makes sense 3 for both makes sense 4 for no num but makes sense and
            // 0 for does not makes sense
            int matchCheck = checkIfMatchToAnyProduct(lineContents.getText());
            Log.v("Cony", "matchCheck status = " + matchCheck);

            if (matchCheck == 1 || matchCheck == 2 || matchCheck == 3) {
                // in case_2b check if numeric element itself does not itself contain a special char.
                // If it does take caution with parsing to Integer
                Log.v("Cony", " matchCheck status shows num at end detected");

                int case_2a, case_2b;
                // 2a ( average separation )
                double mean = 0;
                if (myLines.get(i).size() > 2 && isNotAlpha(myLines.get(i).get(myLines.get(i).size() - 1).getText())) {
                    for (int j = 0; j < myLines.get(i).size() - 2; j++) {
                        mean += myLines.get(i).get(j).getBox().right - myLines.get(i).get(j + 1).getBox().left;
                    }
                    mean /= myLines.get(i).size();
                    if (myLines.get(i).get(myLines.get(i).size() - 2).getBox().right - myLines.get(i).get(myLines.get(i).size() - 1).getBox().left > mean * 1.3)
                        case_2a = 1;
                    else
                        case_2a = -1;
                } else
                    case_2a = 0;

                Log.v("Cony", "case_2a checked = " + case_2a);

                if (myLines.get(i).size() > 2) {
                    Log.v("Cony", "Checking for Special Chars " + myLines.get(i).get(myLines.get(i).size() - 2).getText() + "   and in " + myLines.get(i).get(myLines.get(i).size() - 1).getText());
                    if (isSpecialCharExists(myLines.get(i).get(myLines.get(i).size() - 2).getText(), "end") || isSpecialCharExists(myLines.get(i).get(myLines.get(i).size() - 1).getText(), "in"))
                        case_2b = 1;
                    else
                        case_2b = 0;
                } else
                    case_2b = 0;
                Log.v("Cony", "case_2b checked = " + case_2b);

                // getSeparated returns LineContents  and check on


                String lineWithNum = "";
                String lineWithoutNum = "";
                for (int j = 0; j < lineContents.getText().size(); j++) {
                    lineWithNum = lineWithNum + " " + lineContents.getText().get(j);
                    if (j < lineContents.getText().size() - 1) {
                        lineWithoutNum = lineWithoutNum + " " + lineContents.getText().get(j);
                    }
                }
                lineWithNum = lineWithNum.trim();
                lineWithoutNum = lineWithoutNum.trim();



                if (matchCheck == 1) {
                    quantity = 1;
                    name = lineWithNum;
                } else if (matchCheck == 2) {
                    quantity = Integer.parseInt(lineContents.getText().get(lineContents.getText().size() - 1).trim());
                    name = lineWithoutNum;
                } else {
                    if (case_2a == 1 || case_2b == 1) {
                        quantity = Integer.parseInt(lineContents.getText().get(lineContents.getText().size() - 1).trim());
                        name = lineWithoutNum;
                    } else {
                        quantity = 1;
                        name = lineWithNum;
                    }
                }
                doGraphicOverlay(toRect(lineContents.getRectF()), true);
            } else if (matchCheck == 4) {
                name = "";
                for (int j = 0; j < myLines.get(i).size(); j++) {
                    name = name + " " + removeSpecialChar(myLines.get(i).get(j).getText());
                    name = name.trim();
                }
                quantity = 1;
                doGraphicOverlay(toRect(lineContents.getRectF()), true);
            } else
                doGraphicOverlay(toRect(lineContents.getRectF()), false);

            if (name.length() > 0 && quantity > 0) {
                bookAndQuantity.add(new myPair(getNearestProduct(name), quantity));
                Log.v("Cony", "Adding BNAME =  " + getNearestProduct(name) + "  quantity = " + quantity);
            }
        }

        String txt6 = "";
        for (int i = 0; i < bookAndQuantity.size(); i++) {
            txt6 = txt6 + "Book Name = " + bookAndQuantity.get(i).getBookName() + "\t Quantity = " + bookAndQuantity.get(i).getQuantity() + "\n";
        }
        Log.v("FullText","Orders = "+txt6);
        textView6.setText(txt6);
    }
    private Rect toRect(RectF rectF) {

        int h = imageBitmap.getHeight();
        int w = imageBitmap.getWidth();

        return new Rect((int) rectF.left * w,
                (int) rectF.top * h,
                (int) rectF.right * w,
                (int) rectF.bottom * h);
    }
    private void getMedianSlopeOfText(List<IdentifiedText> lines,double wordLength) {
        double x1, y1, x2, y2, slopeCurr;
        double slope=0;
        int lineCount=0;
        double angle = 90;

        for (int j = 0; j < lines.size(); j++) {
            //getSlope of each line
            List<PointF> points = lines.get(j).getPolygon().getPoints();

//            Log.v("Slope"," left = "+Math.abs(lines.get(j).getBox().left-lines.get(j).getBox().right));
//            Log.v("Slope"," right = "+Math.abs(lines.get(j).getBox().top-lines.get(j).getBox().bottom));
//            Log.v("Slope"," word*2 = "+wordLength*2);

            if(Math.abs(lines.get(j).getBox().left-lines.get(j).getBox().right)>wordLength*2)
            {
                x1 = points.get(0).x;
                x2 = points.get(1).x;
                y1 = points.get(0).y;
                y2 = points.get(1).y;

                slopeCurr = ((y2 - y1) * imageBitmap.getHeight()) / (imageBitmap.getWidth() * (x2 - x1));
                Log.v("Slope", "points found in the line = (" + x1 + "," + y1 + ")  (" + x2 + "," + y2 + ")  and slope found = " + slopeCurr);

                slope=slope+slopeCurr;
                lineCount++;
            }
        }
        if (lineCount > 0) {
            angle = Math.atan(slope/lineCount) * 180 / 3.14;
            Log.v("Slope ", "Median angle = " + angle);
            if (Math.abs(angle) < 500)
            {
                rotate_image_bitmap((int) (-angle));
                Log.v("Slope","Rotating image ");
                Toast.makeText(getApplicationContext(),"Rotating and re-sending image for text capture",Toast.LENGTH_SHORT).show();
            }
        else
            {
                Log.v("Slope","Angle found is large > 60 raise Rotate image toast");
                Toast.makeText(getApplicationContext(),"Rotate Image For better Text Capture",Toast.LENGTH_SHORT).show();
            }
        }
        else if(lines.size()>0)
        {
            angle=0;
            Log.v("Slope"," Text Found and no multiword lines detected");
        }
        else
        {
            Log.v("Slope"," No Text Found");
        }
    }

    private double getMedian(ArrayList<Double> slope) {
        Collections.sort(slope);
        double median;
        if (slope.size() % 2 == 0)
            median = (slope.get(slope.size()/2) + slope.get(slope.size()/2-1))/2;
        else
            median = slope.get(slope.size()/2);
        return median;
    }
    private LineContents getSeparated(ArrayList<IdentifiedText> elements) {

        float left, right, bottom, top;

        left = elements.get(0).getBox().left;
        right = elements.get(0).getBox().right;
        bottom = elements.get(0).getBox().bottom;
        top = elements.get(0).getBox().top;
        String line = removeSpecialChar(elements.get(0).getText()) + " ";

        for (int i = 1; i < elements.size(); i++) {
            line = line + removeSpecialChar(elements.get(i).getText()) + " ";
            if (elements.get(i).getBox().left < left)
                left = elements.get(i).getBox().left;

            if (elements.get(i).getBox().right > right)
                right = elements.get(i).getBox().right;

            if (elements.get(i).getBox().bottom > bottom)
                bottom = elements.get(i).getBox().bottom;

            if (elements.get(i).getBox().top < top)
                top = elements.get(i).getBox().top;
        }
        line = line.trim();
        String[] part = line.split("[^a-zA-Z0-9]+|(?<=[a-zA-Z])(?=[0-9])|(?<=[0-9])(?=[a-zA-Z])");

        ArrayList<String> text = new ArrayList<>(Arrays.asList(part));
        RectF rectF;
        rectF = new RectF(left, top, right, bottom);

        LineContents lineContents = new LineContents(text, rectF);

        return lineContents;

    }
    public void rotate_image_bitmap(int angle) {
        Matrix matrix = new Matrix();
        matrix.setRotate(angle);
        imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
        imageView.setImageBitmap(imageBitmap);
        tempBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(tempBitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawBitmap(imageBitmap, 0, 0, paint);
    }
    private boolean isNotAlpha(String text) {
        text = text.toLowerCase();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) >= 'a' && text.charAt(i) <= 'z')
                return false;
        }
        return true;
    }
    private String getNearestProduct(String name) {
        double min = getCost(name, 0);
        int minCode = 0;
        for (int i = 1; i < bookNameAndCode.size(); i++) {
            if (getCost(name, i) < min) {
                min = getCost(name, i);
                minCode = i;
            }
        }
        return bookNameAndCode.get(minCode).getBookName();
    }
    private String removeSpecialChar(String text) {
        String retText = "";
        char ch;
        for (int i = 0; i < text.length(); i++) {
            ch = text.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                retText = retText + ch;
            }
        }
        return retText;
    }
    private String onlyNumeric(String text) {
        String retText = "";
        char ch;
        for (int i = 0; i < text.length(); i++) {
            ch = text.charAt(i);
            if ((ch > '0' && ch < '9')) {
                retText = retText + ch;
            }
        }
        return retText;
    }
    private boolean endsWithNum(String text) {
        return (text.charAt(text.length() - 1) >= '0' && text.charAt(text.length() - 1) <= '9');
    }
    private void doGraphicOverlay(Rect rect, boolean status) {
        Log.v("Cony", "DOING GRAPHIC OVERLAY");
        //        if (status)
//            paint.setColor(Color.BLUE);
//        else
//            paint.setColor(Color.RED);
//
//        paint.setAlpha(50);
//        canvas.drawRect(rect, paint);
//        imageView.setImageBitmap(tempBitmap);
    }
    private int checkIfMatchToAnyProduct(ArrayList<String> line) {
        double a, b, c;
        a = b = c = 0;
        Log.v("Cony", "checkIfMatchToAnyProduct");
        if (endsWithNum(line.get(line.size() - 1))) {
            Log.v("Cony", "num at end detected");
            String lineWithNum = "";
            String lineWithoutNum = "";
            for (int j = 0; j < line.size(); j++) {

                lineWithNum = lineWithNum + " " + line.get(j);
                lineWithNum = lineWithNum.trim();
                if (j < line.size() - 1) {

                    lineWithoutNum = lineWithoutNum + " " + line.get(j);
                    lineWithoutNum = lineWithoutNum.trim();
                }
            }
            a = checkSimilarity(lineWithNum);
            b = checkSimilarity(lineWithoutNum);
            if (a < costThreshold || b < costThreshold) {
                if (a < b)
                    return 1;
                else if (b < a)
                    return 2;
                else
                    return 3;
            } else
                return 0;

        } else {
            Log.v("Cony", "num at end not detected");
            String lineFull = "";
            for (int j = 0; j < line.size(); j++) {
                lineFull = lineFull + " " + removeSpecialChar(line.get(j));
                lineFull = lineFull.trim();
            }

            c = checkSimilarity(lineFull);
        }

        Log.v("Cony", "checked states a=" + a + "   b=" + b + "   c=" + c);

        if (c < 0.3)
            return 4;
        else
            return 0;
    }
    private double checkSimilarity(String name) {
        Log.v("Cony", "Checking Similarity for =  " + name);
        double min = getCost(name, 0);
        int minCode = 0;
        for (int i = 1; i < bookNameAndCode.size(); i++) {
            if (getCost(name, i) < min) {
                min = getCost(name, i);
                minCode = i;
            }
        }

        Log.v("Cony", "Got Cost = " + min);
        return min;
    }
    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }
    private double getCost(String x, int element) {
        x = x.toLowerCase();
        String y = bookNameAndCode.get(element).getBookName().toLowerCase();

        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j - 1] + costOfSubstitution(x.charAt(i - 1), y.charAt(j - 1)),
                            dp[i - 1][j] + 1),
                            dp[i][j - 1] + 1);
                }
            }
        }
        return (dp[x.length()][y.length()] * 1.0) / y.length();
    }
    private boolean isSpecialCharExists(String text, String position) {
        char ch;
        if (position.equals("start")) {
            ch = text.charAt(0);
            return (ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9');
        } else if (position.equals("end")) {
            ch = text.charAt(text.length() - 1);
            return (ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9');
        } else {
            for (int i = 0; i < text.length(); i++) {
                ch = text.charAt(i);
                if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9'))
                    return true;
            }
        }
        return false;
    }
    private ArrayList<ArrayList<IdentifiedText>> getAllLines(ArrayList<IdentifiedText> elements) {
        Collections.sort(elements, new SortY());
        int h = imageBitmap.getHeight();
        ArrayList<ArrayList<IdentifiedText>> myLines = new ArrayList<>();
        ArrayList<IdentifiedText> tempLine = new ArrayList<>();
        tempLine.add(elements.get(0));
        for (int i = 1; i < elements.size(); i++) {
            if (elements.get(i).getBox().centerY() - elements.get(i - 1).getBox().centerY() > nextLineConstraint) {
                Collections.sort(tempLine, new SortX());
                myLines.add(tempLine);
                tempLine = new ArrayList<>();
                tempLine.add(elements.get(i));
            } else {
                tempLine.add(elements.get(i));
            }
        }
        Collections.sort(tempLine, new SortX());
        myLines.add(tempLine);

        return myLines;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                imageView.setImageURI(image_uri);
                imageView2Bitmap(imageView);


                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
                byte[] byteArray = stream.toByteArray();
                imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView.setImageBitmap(imageBitmap);

                imageButton.setEnabled(true);
                btn2.setEnabled(true);
            }
        } else if (requestCode == REQUEST_GALLERY_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                imageView.setImageURI(selectedImage);
                imageView2Bitmap(imageView);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
                byte[] byteArray = stream.toByteArray();
                imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                imageView.setImageBitmap(imageBitmap);

                imageButton.setEnabled(true);
                btn2.setEnabled(true);
            }
        }
    }
    private void selectImage(Context context) {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose your profile picture");

        builder.setItems(options, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {

                if (options[item].equals("Take Photo")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            requestPermissions(permission, PERMISSION_CODE_CAMERA);
                            dispatchTakePictureIntent();
                        } else {
                            dispatchTakePictureIntent();
                        }
                    } else {
                        dispatchTakePictureIntent();
                    }

                } else if (options[item].equals("Choose from Gallery")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                            String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                            requestPermissions(permissions, PERMISSION_CODE_GALLERY);
                        } else {
                            getImageFromGallery();
                        }
                    } else {
                        getImageFromGallery();
                    }

                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    private void getImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE_GALLERY: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery();
                } else {
                    Toast.makeText(this, "Permission Denied!!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case PERMISSION_CODE_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Camera Permission Denied!!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
    private void imageView2Bitmap(ImageView view) {
        imageBitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();

    }
    private void dispatchTakePictureIntent() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void requestNewImage() {
    }
    private boolean checkReadableImage(Bitmap img) {
        return true;
    }
}


class SortY implements Comparator<IdentifiedText> {
    @Override
    public int compare(IdentifiedText o1, IdentifiedText o2) {
        return (int) (o1.getBox().centerY() * 1000) - (int) (o2.getBox().centerY() * 1000);
    }
}
class SortX implements Comparator<IdentifiedText> {
    @Override
    public int compare(IdentifiedText o1, IdentifiedText o2) {
        return (int) (o1.getBox().centerX() * 1000) - (int) (o2.getBox().centerX() * 1000);
    }
}
class myPair {
    String bookName;
    int quantity;

    myPair(String bookName, int quantity) {
        this.bookName = bookName;
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
class intPair {
    int x, y;

    intPair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
class LineContents {
    private ArrayList<String> text;
    private RectF rectF;

    LineContents(ArrayList<String> text, RectF rectF) {
        this.rectF = rectF;
        this.text = text;
    }

    ArrayList<String> getText() {
        return text;
    }

    RectF getRectF() {
        return rectF;
    }

    public void setRect(RectF rectF) {
        this.rectF = rectF;
    }

    public void setText(ArrayList<String> text) {
        this.text = text;
    }
}
