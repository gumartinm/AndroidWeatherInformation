/**
 * Copyright 2014 Gustavo Martin Morcuende
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.gumartinm.weather.information.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.text.MessageFormat;

import android.content.Context;

import name.gumartinm.weather.information.model.currentweather.Current;
import name.gumartinm.weather.information.model.forecastweather.Forecast;
import timber.log.Timber;


public class PermanentStorage {
    private static final String CURRENT_DATA_FILE = "current.file";
    private static final String FORECAST_DATA_FILE = "forecast.file";
    private static final String WIDGET_CURRENT_DATA_FILE = "current.{0}.file";
    private final Context context;

    public PermanentStorage(final Context context) {
        this.context = context;
    }

    public void saveCurrent(final Current current) {
    	
        try {
			this.saveObject(CURRENT_DATA_FILE, current);
		} catch (FileNotFoundException e) {
			Timber.e(e, "saveCurrent exception: ");
		} catch (IOException e) {
            Timber.e(e, "saveCurrent exception: ");
		}
    }

    public Current getCurrent() {
    	
    	try {
			return (Current) this.getObject(CURRENT_DATA_FILE);
		} catch (final StreamCorruptedException e) {
            Timber.e(e, "getCurrent exception: ");
		} catch (final FileNotFoundException e) {
            Timber.e(e, "getCurrent exception: ");
		} catch (final IOException e) {
            Timber.e(e, "getCurrent exception: ");
		} catch (final ClassNotFoundException e) {
            Timber.e(e, "getCurrent exception: ");
		}
    	
    	return null;
    }

    public void saveWidgetCurrentData(final Current current, final int appWidgetId) {

        final String fileName = MessageFormat.format(WIDGET_CURRENT_DATA_FILE, appWidgetId);
        try {
            this.saveObject(fileName, current);
        } catch (FileNotFoundException e) {
            Timber.e(e, "saveWidgetCurrentData exception: ");
        } catch (IOException e) {
            Timber.e(e, "saveWidgetCurrentData exception: ");
        }
    }

    public Current getWidgetCurrentData(final int appWidgetId) {

        final String fileName = MessageFormat.format(WIDGET_CURRENT_DATA_FILE, appWidgetId);
        try {
            return (Current) this.getObject(fileName);
        } catch (final StreamCorruptedException e) {
            Timber.e(e, "getWidgetCurrentData exception: ");
        } catch (final FileNotFoundException e) {
            Timber.e(e, "getWidgetCurrentData exception: ");
        } catch (final IOException e) {
            Timber.e(e, "getWidgetCurrentData exception: ");
        } catch (final ClassNotFoundException e) {
            Timber.e(e, "getWidgetCurrentData exception: ");
        }

        return null;
    }

    public void removeWidgetCurrentData(final int appWidgetId) {

        final String fileName = MessageFormat.format(WIDGET_CURRENT_DATA_FILE, appWidgetId);

        this.removeFile(fileName);
    }

    public void saveForecast(final Forecast forecast) {

    	try {
			this.saveObject(FORECAST_DATA_FILE, forecast);
		} catch (FileNotFoundException e) {
            Timber.e(e, "saveForecast exception: ");
		} catch (IOException e) {
            Timber.e(e, "saveForecast exception: ");
		}
    }

    public Forecast getForecast() {
        
    	try {
			return (Forecast) this.getObject(FORECAST_DATA_FILE);
		} catch (final StreamCorruptedException e) {
            Timber.e(e, "getForecast exception: ");
		} catch (final FileNotFoundException e) {
            Timber.e(e, "getForecast exception: ");
		} catch (final IOException e) {
            Timber.e(e, "getForecast exception: ");
		} catch (final ClassNotFoundException e) {
            Timber.e(e, "getForecast exception: ");
		}
    	
    	return null;
    }

    private void saveObject(final String fileName, final Object objectToStore)
    		throws FileNotFoundException, IOException {
    	final String temporaryFileName = fileName.concat(".tmp");
    	
        final FileOutputStream tmpPersistFile = this.context.openFileOutput(
        		temporaryFileName, Context.MODE_PRIVATE);
        try {
        	final ObjectOutputStream oos = new ObjectOutputStream(tmpPersistFile);
        	try {
        		oos.writeObject(objectToStore);
        		
        		// Don't fear the fsync!
        		// http://thunk.org/tytso/blog/2009/03/15/dont-fear-the-fsync/
        		tmpPersistFile.flush();
        		tmpPersistFile.getFD().sync();
        	} finally {
        		oos.close();
        	}
        } finally {
        	tmpPersistFile.close();
        }

        this.renameFile(temporaryFileName, fileName);
    }
 
    private Object getObject(final String fileName) throws StreamCorruptedException, FileNotFoundException,
    								  					   IOException, ClassNotFoundException {
    	final InputStream persistFile = this.context.openFileInput(fileName);
    	try {
    		final ObjectInputStream ois = new ObjectInputStream(persistFile);
    		try {
    			return ois.readObject();
    		} finally {
    			ois.close();
    		}
    	} finally {
    		persistFile.close();
    	}
    } 
    
    private void renameFile(final String fromFileName, final String toFileName) throws IOException {
        final File filesDir = this.context.getFilesDir();
        final File fromFile = new File(filesDir, fromFileName);
        final File toFile = new File(filesDir, toFileName);
        if (!fromFile.renameTo(toFile)) {
        	if (!fromFile.delete()) {
        		throw new IOException("PermanentStorage, delete file error");
        	}	
        	throw new IOException("PermanentStorage, rename file error");
        }
    }

    private void removeFile(final String fileName) {
        final File filesDir = this.context.getFilesDir();
        final File file = new File(filesDir, fileName);

        file.delete();
    }
}

