/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class SwitchStatistics {
    private static final String TAG = "SwitchStatistics";
    private static String sRefFilename = "statistics.xml";
    private static final Integer SAVE_THRESHOLD = 10;

    private static SwitchStatistics mInstance;
    private Context mContext;
    private HashMap<String, Integer> mStartTrace = new HashMap<String, Integer>();
    private SwitchConfiguration mConfiguration;
    private int mLaunchCount;

    public static SwitchStatistics getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SwitchStatistics(context);
        }
        return mInstance;
    }

    private SwitchStatistics(Context context) {
        mContext = context;
        mConfiguration = SwitchConfiguration.getInstance(mContext);
    }

    public void traceStartIntent(Intent intent) {
        if (mConfiguration.mLaunchStatsEnabled) {
            mLaunchCount++;
            ComponentName name = intent.getComponent();
            String pPkgName = name.getPackageName();
            Integer count = mStartTrace.get(pPkgName);
            if (count == null) {
                mStartTrace.put(pPkgName, 1);
            } else {
                mStartTrace.put(pPkgName, count + 1);
            }
            if (mLaunchCount == SAVE_THRESHOLD) {
                mLaunchCount = 0;
                saveStatistics();
            }
        }
    }

    public List<String> getTopmostLaunches(int count) {
        Comparator<Entry<String, Integer>> valueComparator = new Comparator<Entry<String,Integer>>() {
            @Override
            public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
                Integer v1 = e1.getValue();
                Integer v2 = e2.getValue();
                return -v1.compareTo(v2);
            }
        };

        Set<Entry<String, Integer>> entries = mStartTrace.entrySet();
        List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(entries);
        Collections.sort(listOfEntries, valueComparator);
        LinkedHashMap<String, Integer> sortedByValue = new LinkedHashMap<String, Integer>(listOfEntries.size());
        for(Entry<String, Integer> entry : listOfEntries){
            sortedByValue.put(entry.getKey(), entry.getValue());
        }

        int i = 0;
        List<String> topLaunches = new ArrayList<String>();
        for (String pkgName : sortedByValue.keySet()) {
            topLaunches.add(pkgName);
            i++;
            if (i == count) {
                break;
            }
        }
        return topLaunches;
    }

    public void clear() {
        mStartTrace.clear();
        if (getSaveFile().exists()) {
            getSaveFile().delete();
        }
    }

    private File getSaveFile() {
        return new File(mContext.getFilesDir(), sRefFilename);
    }

    public void saveStatistics() {
        if (!mConfiguration.mLaunchStatsEnabled) {
            return;
        }

        if (getSaveFile().exists()) {
            getSaveFile().delete();
        }

        //traceStats();

        try {
            FileOutputStream fos = new FileOutputStream(getSaveFile());
            XmlSerializer xmlSerializer = Xml.newSerializer();
            xmlSerializer.setOutput(fos, "UTF-8");
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            xmlSerializer.startDocument(null, Boolean.valueOf(true));

            xmlSerializer.startTag(null, "packages");
            for (String pkgName : mStartTrace.keySet()) {
                xmlSerializer.startTag(null, "package");
                xmlSerializer.attribute(null, "name", pkgName);
                xmlSerializer.attribute(null, "count", String.valueOf(mStartTrace.get(pkgName)));
                xmlSerializer.endTag(null, "package");
            }
            xmlSerializer.endTag(null, "packages");

            xmlSerializer.endDocument();
            xmlSerializer.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "saveStatistics", e);
        }
    }

    public void loadStatistics() {
        if (!mConfiguration.mLaunchStatsEnabled) {
            return;
        }
        if (!getSaveFile().exists()) {
            return;
        }
        mStartTrace.clear();

        try {
            XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();

            FileInputStream fIs = new FileInputStream(getSaveFile());
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(fIs, null);
            parser.nextTag();
            parseXML(parser);
        } catch (Exception e) {
            Log.e(TAG, "loadStatistics", e);
        }
        //traceStats();
    }

    private void addSavedTraceEntry(String packageName, Integer count) {
        mStartTrace.put(packageName, count);
    }

    private void parseXML(XmlPullParser parser) throws XmlPullParserException,
            IOException {
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
            case XmlPullParser.START_DOCUMENT:
                break;
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                if (name.equalsIgnoreCase("package")) {
                    int count = parser.getAttributeCount();
                    String packageName = null;
                    Integer packageCount = 0;
                    for (int a = 0; a < count; a++) {
                        String key = parser.getAttributeName(a);
                        String value = parser.getAttributeValue(a);
                        if (key.equals("name")) {
                            packageName = value;
                        } else if (key.equals("count")) {
                            packageCount = Integer.valueOf(value);
                        }
                    }
                    if (packageName != null && packageCount != 0) {
                        addSavedTraceEntry(packageName, packageCount);
                    }
                }
                break;
            case XmlPullParser.END_TAG:
                break;
            }
            eventType = parser.next();
        }
    }
}
