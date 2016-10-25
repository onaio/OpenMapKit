package org.redcross.openmapkit;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.io.Files;
import com.spatialdev.osm.model.OSMElement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.redcross.openmapkit.odkcollect.ODKCollectHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class is responsible for building the pull_csv containing OSM tags to be used
 * inside ODK
 *
 * In the CSV, the columns are arranged in this order: primaryKeyColumn followed by columns in otherColumns
 *
 * The generated CSV is sperated using commas following RFC4180
 *
 * Created by Jason Rogena - jrogena@ona.io on 25/10/2016.
 */

public class PullCsvBuilder {
    private final String csvFilename;
    private final String primaryKeyColumn;
    private final ArrayList<String> otherColumns;
    private CSVParser csvParser;
    private static final String CSV_EXTENSION = ".csv";
    private static final String CHARSET = "UTF-8";
    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';
    private static final String FIELD_BLANK_VALUE = "";

    /**
     * This method performs the update to the Pull CSV file. All logic is run in a thread running
     * asynchronously to the UI thread. Put UI updates in the OnPostUpdateListener
     *
     * @param selectedElement
     * @param osmFilename
     * @return TRUE if it was able to update the Pull CSV
     */
    public static void updateCsv(OSMElement selectedElement, String osmFilename, OnPostUpdateListener onPostUpdateListener) {
        new Task(selectedElement, osmFilename, onPostUpdateListener).execute();
    }

    public PullCsvBuilder(String csvFilename, String primaryKeyColumn, ArrayList<String> otherColumns) {
        this.csvFilename = formatFilename(csvFilename);
        this.primaryKeyColumn = primaryKeyColumn;
        this.otherColumns = otherColumns;
    }

    public PullCsvBuilder(String csvFilename, String primaryKeyColumn, JSONArray otherColumns) throws JSONException {
        this.csvFilename = formatFilename(csvFilename);
        this.primaryKeyColumn = primaryKeyColumn;
        this.otherColumns = jsonColumnsToArrayList(otherColumns);
    }

    private ArrayList<String> jsonColumnsToArrayList(JSONArray columns) throws JSONException {
        ArrayList<String> list = new ArrayList<>();
        if(columns != null) {
            for(int i = 0; i < columns.length(); i++) {
                list.add(columns.getString(i));
            }
        }
        return list;
    }

    /**
     * This method parses the existing CSV file
     * @return
     */
    public void load() throws IOException {
        //check if file exists
        File csvFile = getCsvFile();
        if(csvFile.exists()) {
            //make sure it's not a directory
            if(!csvFile.isDirectory()) {
                csvParser = CSVParser.parse(csvFile, Charset.forName(CHARSET), CSVFormat.RFC4180);
            } else {
                throw new IOException("The path where the Pull CSV file should be has a directory");
            }
        } else {
            //create the file with the header columns
            writeLine(csvFile, getHeaderColumns());

            csvParser = CSVParser.parse(csvFile, Charset.forName(CHARSET), CSVFormat.RFC4180);
        }
    }

    private File getCsvFile() throws IOException {
        String formFileName = ODKCollectHandler.getODKCollectData().getFormFileName();
        return ExternalStorage.getFileFromOdkMediaDir(formFileName, csvFilename);
    }

    public void updateOsmElement(OSMElement osmElement, String primaryKey) throws IOException {
        if(primaryKey != null && primaryKey.length() > 0) {
            ArrayList<String> csvHeadings = getHeaderColumns();

            //check if primary key already exists (don't add it if it does)
            boolean pkExists = false;
            Iterator<CSVRecord> recordIterator = csvParser.iterator();
            while(recordIterator.hasNext()) {
                CSVRecord curRecord = recordIterator.next();
                if(curRecord.size() != csvHeadings.size()) {
                    throw new IOException("One CSV row does not match the required number of columns");
                }

                if(primaryKey.equals(curRecord.get(csvHeadings.indexOf(primaryKeyColumn)))) {
                    pkExists = true;
                    break;
                }
            }

            if(!pkExists) {
                ArrayList<String> columnValues = new ArrayList<>();
                for(String curHeading : csvHeadings) {
                    if(curHeading.equals(primaryKeyColumn)) {
                        columnValues.add(primaryKey);
                    } else {
                        String value = FIELD_BLANK_VALUE;
                        if(osmElement.getTags().containsKey(curHeading)) {
                            value = osmElement.getTags().get(curHeading);
                        }
                        columnValues.add(value);
                    }
                }

                if(csvHeadings.size() == columnValues.size()) {
                    File csvFile = getCsvFile();
                    writeLine(csvFile, columnValues);
                }
            }
        }
    }

    private ArrayList<String> getHeaderColumns() {
        ArrayList<String> columns = new ArrayList<>();
        columns.add(primaryKeyColumn);
        columns.addAll(otherColumns);
        return columns;
    }

    /**
     * This method saves the loaded CSV in its file
     * @return
     */
    public void unload() throws IOException {
        if(!csvParser.isClosed()) {
            csvParser.close();
        }
    }

    /**
     * This method ensures the provided filename ends with CSV_EXTENSION regardless of case
     *
     * @param filename  The filename to check
     * @return  The provided filename with the CSV_EXTENSION if it didn't have it already
     */
    private String formatFilename(String filename) {
        if(filename != null) {
            if(!filename.toLowerCase().endsWith(CSV_EXTENSION.toLowerCase())) {
                filename = filename + CSV_EXTENSION;
            }
        }
        return filename;
    }

    private static void writeLine(File f, ArrayList<String> values) throws IOException {
        boolean first = true;

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(SEPARATOR);
            }
            if (QUOTE == ' ') {
                sb.append(formatRfc4180(value));
            } else {
                sb.append(QUOTE).append(formatRfc4180(value)).append(QUOTE);
            }

            first = false;
        }
        sb.append("\n");
        Files.append(sb.toString(), f, Charset.forName(CHARSET));
    }

    private static String formatRfc4180(String value) {
        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;
    }

    private static class Task extends AsyncTask<Void, Void, Boolean> {
        private Exception e;
        private OSMElement selectedElement;
        private String osmFilename;
        private OnPostUpdateListener onPostUpdateListener;

        public Task(OSMElement selectedElement, String osmFilename, OnPostUpdateListener onPostUpdateListener) {
            this.selectedElement = selectedElement;
            this.osmFilename = osmFilename;
            this.onPostUpdateListener = onPostUpdateListener;
            e = null;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            String csvFilename = Settings.singleton().getPullCsvFilename();
            JSONArray tags = Settings.singleton().getPullCsvTags();
            String pkColumn = Settings.singleton().getPullCsvPkColumn();
            if(csvFilename != Settings.DEFAULT_PULL_CSV_FILENAME
                    && pkColumn != Settings.DEFAULT_PULL_CSV_PK_COLUMN
                    && !tags.equals(Settings.DEFAULT_PULL_CSV_TAGS)) {
                try {
                    PullCsvBuilder builder = new PullCsvBuilder(csvFilename, pkColumn, tags);
                    builder.load();
                    builder.updateOsmElement(selectedElement, osmFilename);
                    builder.unload();
                    return true;
                } catch (JSONException e) {
                    this.e = e;
                } catch (IOException e) {
                    this.e = e;
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(onPostUpdateListener != null) {
                if(aBoolean == true) {
                    onPostUpdateListener.onUpdate(aBoolean);
                } else {
                    if(e != null) {
                        onPostUpdateListener.onError(e);
                    } else {
                        onPostUpdateListener.onUpdate(aBoolean);
                    }
                }
            }
        }
    }

    public interface OnPostUpdateListener{
        void onUpdate(boolean status);
        void onError(Exception e);
    }
}
