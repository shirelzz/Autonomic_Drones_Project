package com.dji.sdk.sample.demo.accurateLandingController;

import android.content.Context;
import android.content.res.Resources;
import android.util.Xml;

import com.dji.sdk.sample.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ReadFromKmlFile {
    private final Map<String, GPSLocation> coordinatesMap;
    private final Context context;

    public ReadFromKmlFile(Context context) {
        coordinatesMap = new HashMap<>();
        this.context = context;
        readKml();
        print();
    }

    public void print(){
        for (HashMap.Entry<String, GPSLocation> entry : coordinatesMap.entrySet()) {
            String name = entry.getKey();
            GPSLocation coordinate = entry.getValue();

            double latitude = coordinate.getLatitude();
            double longitude = coordinate.getLongitude();
            double altitude = coordinate.getAltitude();

            System.out.println("Name: " + name);
            System.out.println("Latitude: " + latitude);
            System.out.println("Longitude: " + longitude);
            System.out.println("Altitude: " + altitude);
            System.out.println("-------------------");
        }
    }

    public void readKml() {

        Resources resources = context.getResources();
        try {
            // Open the InputStream for the KML file
            InputStream inputStream = resources.openRawResource(R.raw.university_places); // Replace with your KML file name

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            String currentTag = "";
            String currentName = "";
            double currentLatitude = 0;
            double currentLongitude = 0;
            double currentAltitude = 0;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        currentTag = parser.getName();
                        break;
                    case XmlPullParser.TEXT:
                        String text = parser.getText();
                        if ("name".equals(currentTag)) {
                            if(!text.trim().equals("")) {
                                currentName = text.trim();
                            }
                        } else if ("coordinates".equals(currentTag)) {
                            String[] coordinates = text.trim().split(",");
                            if (coordinates.length >= 3) {
                                currentLongitude = Double.parseDouble(coordinates[0]);
                                currentLatitude = Double.parseDouble(coordinates[1]);
                                currentAltitude = Double.parseDouble(coordinates[2]);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("Placemark".equals(parser.getName())) {
                            GPSLocation gpsCoordinate = new GPSLocation();
                            gpsCoordinate.setLatitude(currentLatitude);
                            gpsCoordinate.setLongitude(currentLongitude);
                            gpsCoordinate.setAltitude(currentAltitude);

                            coordinatesMap.put(currentName, gpsCoordinate);
                            currentName = "";
                            currentLatitude = 0;
                            currentLongitude = 0;
                            currentAltitude = 0;
                        }
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
    }
}
