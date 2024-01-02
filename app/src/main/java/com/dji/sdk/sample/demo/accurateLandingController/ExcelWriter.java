package com.dji.sdk.sample.demo.accurateLandingController;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;


// program to create/append data to excel file using apache poi

import static java.nio.file.StandardOpenOption.APPEND;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.StandardOpenOption;

public class ExcelWriter {

    File filePath;
    Context context;
    String sheetName;
    ArrayList<String> rowHeaders;

    public ExcelWriter(Context context, String fileName) {
        filePath = new File(Environment.getExternalStorageDirectory() + "/" + fileName + ".xls");
//        System.out.println("Path: " + filePath.getAbsolutePath());
        this.context = context;
        rowHeaders = new ArrayList<>();
        sheetName = fileName;
    }

    /**
     * A method to create/update the excel file
     * To use this method you need to set the headers
     *
     * @param values
     */

    public void writeToExcel(String values) {

        String[] splitValues = {"empty"};
        boolean splitRow = values.contains("|");
        if (splitRow) {
            splitValues = values.split("\\|");
        }

        try {

            if (!filePath.exists()) {

                System.out.println("entered 1st if");

                HSSFWorkbook hssfWorkbook = new HSSFWorkbook();
                HSSFSheet hssfSheet = hssfWorkbook.createSheet(sheetName);

                HSSFRow firstRow = hssfSheet.createRow(0);


                if (splitRow) {

                    // headers
                    for (int i = 0; i < rowHeaders.size(); ++i) {
                    firstRow.createCell(i).setCellValue(rowHeaders.get(i));
                    }

                    // actual data
                    HSSFRow dataRow = hssfSheet.createRow(1);

                    dataRow.createCell(0).setCellValue(getFormattedTimestamp());
                    for (int i = 0; i < splitValues.length; ++i) {
                        dataRow.createCell(i+1).setCellValue(splitValues[i]);
                    }

                } else {
                    HSSFCell hssfCell = firstRow.createCell(0);
                    hssfCell.setCellValue(values);
                }

                filePath.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                hssfWorkbook.write(fileOutputStream);
                Toast.makeText(context, "File Created", Toast.LENGTH_SHORT).show();
                fileOutputStream.flush();
                fileOutputStream.close();

            } else {

                System.out.println("enter 2nd if");


                FileInputStream fileInputStream = new FileInputStream(filePath);
                HSSFWorkbook hssfWorkbook = new HSSFWorkbook(fileInputStream);
                HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(0);
                int lastRowNum = hssfSheet.getLastRowNum();

                HSSFRow hssfRow = hssfSheet.createRow(++lastRowNum);
                if (splitRow) {

                    hssfRow.createCell(0).setCellValue(getFormattedTimestamp());
                    for (int i = 0; i < splitValues.length; ++i) {
                        hssfRow.createCell(i+1).setCellValue(splitValues[i]);
                    }

                } else {
                    hssfRow.createCell(0).setCellValue(values);
                }

                fileInputStream.close();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                hssfWorkbook.write(fileOutputStream);
                Toast.makeText(context, "File Updated", Toast.LENGTH_SHORT).show();
                fileOutputStream.close();
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void setRowHeaders(ArrayList<String> headers) {
        rowHeaders = headers;
    }

    public ArrayList<String> getRowHeaders() {
        return rowHeaders;
    }

    public String getFormattedTimestamp(){

        // Get the current timestamp as LocalDateTime
        LocalDateTime timestamp = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            timestamp = LocalDateTime.now();
        }

        // Format the timestamp as a string
        DateTimeFormatter formatter = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        }
        String formattedTimestamp = timestamp.format(formatter);

        return formattedTimestamp;
    }





}

