package com.dji.sdk.sample.demo.kcgremotecontroller;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by g_arkady on 13/09/18.
 * <p>
 * The idea (and the code) is from here:
 * http://androidblogger.blogspot.com/2010/03/crash-reporter-for-android-slight.html
 */

public class ErrorReporter implements Thread.UncaughtExceptionHandler {

    private static com.dji.sdk.sample.demo.kcgremotecontroller.ErrorReporter S_mInstance;
    String VersionName;
    String PackageName;
    String FilePath;
    String PhoneModel;
    String AndroidVersion;
    String Board;
    String Brand;
    String Device;
    String Display;
    String FingerPrint;
    String Host;
    String ID;
    String Manufacturer;
    String Model;
    String Product;
    String Tags;
    long Time;
    String Type;
    String User;
    HashMap CustomParameters = new HashMap<String, String>();
    private Thread.UncaughtExceptionHandler PreviousHandler;
    private Context CurContext;

    String APP_FOLDER = "/drone_crash/";

    public static com.dji.sdk.sample.demo.kcgremotecontroller.ErrorReporter getInstance() {
        if (S_mInstance == null)
            S_mInstance = new com.dji.sdk.sample.demo.kcgremotecontroller.ErrorReporter();
        return S_mInstance;
    }

    public void AddCustomData(String Key, String Value) {
        CustomParameters.put(Key, Value);
    }

    private String CreateCustomInfoString() {
        String CustomInfo = "";
        Iterator iterator = CustomParameters.keySet().iterator();
        while (iterator.hasNext()) {
            String CurrentKey = (String) iterator.next();
            String CurrentVal = (String) CustomParameters.get(CurrentKey);
            CustomInfo += CurrentKey + " = " + CurrentVal + "\n";
        }
        return CustomInfo;
    }

    public void Init(Context context) {
        PreviousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        CurContext = context;
    }

    public long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    public long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    void RecoltInformations(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi;
            // Version
            pi = pm.getPackageInfo(context.getPackageName(), 0);
            VersionName = pi.versionName;
            // Package name
            PackageName = pi.packageName;
            // Device model
            PhoneModel = android.os.Build.MODEL;
            // Android version
            AndroidVersion = android.os.Build.VERSION.RELEASE;

            Board = android.os.Build.BOARD;
            Brand = android.os.Build.BRAND;
            Device = android.os.Build.DEVICE;
            Display = android.os.Build.DISPLAY;
            FingerPrint = android.os.Build.FINGERPRINT;
            Host = android.os.Build.HOST;
            ID = android.os.Build.ID;
            Model = android.os.Build.MODEL;
            Product = android.os.Build.PRODUCT;
            Tags = android.os.Build.TAGS;
            Time = android.os.Build.TIME;
            Type = android.os.Build.TYPE;
            User = android.os.Build.USER;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String CreateInformationString() {
        RecoltInformations(CurContext);

        String ReturnVal = "";
        ReturnVal += "Version : " + VersionName;
        ReturnVal += "\n";
        ReturnVal += "Package : " + PackageName;
        ReturnVal += "\n";
        ReturnVal += "FilePath : " + FilePath;
        ReturnVal += "\n";
        ReturnVal += "Phone Model" + PhoneModel;
        ReturnVal += "\n";
        ReturnVal += "Android Version : " + AndroidVersion;
        ReturnVal += "\n";
        ReturnVal += "Board : " + Board;
        ReturnVal += "\n";
        ReturnVal += "Brand : " + Brand;
        ReturnVal += "\n";
        ReturnVal += "Device : " + Device;
        ReturnVal += "\n";
        ReturnVal += "Display : " + Display;
        ReturnVal += "\n";
        ReturnVal += "Finger Print : " + FingerPrint;
        ReturnVal += "\n";
        ReturnVal += "Host : " + Host;
        ReturnVal += "\n";
        ReturnVal += "ID : " + ID;
        ReturnVal += "\n";
        ReturnVal += "Model : " + Model;
        ReturnVal += "\n";
        ReturnVal += "Product : " + Product;
        ReturnVal += "\n";
        ReturnVal += "Tags : " + Tags;
        ReturnVal += "\n";
        ReturnVal += "Time : " + Time;
        ReturnVal += "\n";
        ReturnVal += "Type : " + Type;
        ReturnVal += "\n";
        ReturnVal += "User : " + User;
        ReturnVal += "\n";
        ReturnVal += "Total Internal memory : " + getTotalInternalMemorySize();
        ReturnVal += "\n";
        ReturnVal += "Available Internal memory : " + getAvailableInternalMemorySize();
        ReturnVal += "\n";

        return ReturnVal;
    }

    public void uncaughtException(Thread t, Throwable e) {
        String Report = "";
        Date CurDate = new Date();
        Report += "Error Report collected on : " + CurDate.toString();
        Report += "\n";
        Report += "\n";
        Report += "Informations :";
        Report += "\n";
        Report += "==============";
        Report += "\n";
        Report += "\n";
        Report += CreateInformationString();

        Report += "Custom Informations :\n";
        Report += "=====================\n";
        Report += CreateCustomInfoString();

        Report += "\n\n";
        Report += "Stack : \n";
        Report += "======= \n";
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        Report += stacktrace;

        Report += "\n";
        Report += "Cause : \n";
        Report += "======= \n";

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            Report += result.toString();
            cause = cause.getCause();
        }
        printWriter.close();
        Report += "****  End of current Report ***";
        SaveAsFile(Report);
        //SendErrorMail( Report );
        PreviousHandler.uncaughtException(t, e);
    }

//    private void SendErrorMail(Context _context, String ErrorContent) {
//
//        Intent sendIntent = new Intent(Intent.ACTION_SEND);
//        String subject = "CrashReport_MailSubject";//_context.getResources().getString( R.string.CrashReport_MailSubject );
//        String body = "CrashReport_MailBody" +//_context.getResources().getString( R.string.CrashReport_MailBody ) +
//                "\n\n" +
//                ErrorContent +
//                "\n\n";
//        sendIntent.putExtra(Intent.EXTRA_EMAIL,
//                new String[]{"arkady82@gmail.com"});
//        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
//        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//        sendIntent.setType("message/rfc822");
//        _context.startActivityForResult(Intent.createChooser(sendIntent, "Title:"),8);
//        //_context.startActivity(Intent.createChooser(sendIntent, "Title:"));
//    }

    private void SaveAsFile(String ErrorContent) {
        try {
            Random generator = new Random();
            int random = generator.nextInt(99999);
            File file = new File(Environment.getExternalStorageDirectory() + APP_FOLDER +"stack-" + System.currentTimeMillis() + ".stacktrace");
            //file.mkdirs();
            FileOutputStream trace = new FileOutputStream(file);
            trace.write(ErrorContent.getBytes());
            trace.close();
        } catch (Exception e) {
            // ...
        }
    }

    private String[] GetErrorFileList() {
        File dir = new File(FilePath + "/");
        // Try to create the files folder if it doesn't exist
        dir.mkdir();

        // Filter for ".stacktrace" files
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".stacktrace");
            }
        };
        return dir.list(filter);
    }

    private boolean bIsThereAnyErrorFile() {
        String[] errorFileList = GetErrorFileList();
        if (errorFileList == null) return false;
        return GetErrorFileList().length > 0;
    }

    public boolean CheckError(Context _context){
        FilePath = Environment.getExternalStorageDirectory() + APP_FOLDER;
        return bIsThereAnyErrorFile();
    }

    public void deleteAllReports(){
        String[] ErrorFileList = GetErrorFileList();

        for (String curString : ErrorFileList) {
            File curFile = new File(FilePath + "/" + curString);
            curFile.delete();
        }


    }


    public boolean CheckErrorAndSendMail(Context _context) {
        try {
            //FilePath = _context.getFilesDir().getAbsolutePath() !!;
            FilePath = Environment.getExternalStorageDirectory() + APP_FOLDER;
            if (bIsThereAnyErrorFile()) {
                String WholeErrorText = "";
                // on limite à N le nombre d'envois de rapports ( car trop lent )
                String[] ErrorFileList = GetErrorFileList();

                int curIndex = 0;
                final int MaxSendMail = 5;
                for (String curString : ErrorFileList) {
                    if (curIndex++ <= MaxSendMail) {
                        WholeErrorText += "New Trace collected :\n";
                        WholeErrorText += "=====================\n ";
                        String filePath = FilePath + "/" + curString;
                        BufferedReader input = new BufferedReader(new FileReader(filePath));
                        String line;
                        while ((line = input.readLine()) != null) {
                            WholeErrorText += line + "\n";
                        }
                        input.close();
                    }

                    // DELETE FILES !!!!
                    File curFile = new File(FilePath + "/" + curString);
                    curFile.delete();
                }
//                SendErrorMail(_context, WholeErrorText);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}