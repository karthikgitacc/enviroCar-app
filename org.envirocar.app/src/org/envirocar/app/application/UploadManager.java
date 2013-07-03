/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */

package org.envirocar.app.application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.envirocar.app.exception.FuelConsumptionException;
import org.envirocar.app.storage.DbAdapterLocal;
import org.envirocar.app.storage.DbAdapterRemote;
import org.envirocar.app.storage.Measurement;
import org.envirocar.app.storage.Track;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
/**
 * Manager that can upload a track to the server. 
 * Use the uploadAllTracks function to upload all local tracks. 
 * Make sure that you specify the dbAdapter when instantiating.
 * The default constructor should only be used when there is no
 * other way.
 * 
 */
public class UploadManager {

	private static final String TAG = "uploadmanager";

	private String url = ECApplication.BASE_URL + "/users/%1$s/tracks";

	private DbAdapterLocal dbAdapterLocal;
	private DbAdapterRemote dbAdapterRemote;
	private Context context;
	
	/**
	 * Normal constructor for this manager. Specify the context and the dbadapter.
	 * @param dbAdapter The dbadapter (most likely the local one)
	 * @param ctx The context.
	 */
	public UploadManager(Context ctx) {
		this.context = ctx;
		this.dbAdapterLocal = (DbAdapterLocal) ((ECApplication) context).getDbAdapterLocal();
		this.dbAdapterRemote = (DbAdapterRemote) ((ECApplication) context).getDbAdapterRemote();
	}

	/**
	 * This methods uploads all local tracks to the server
	 */
	public void uploadAllTracks() {
		
//		ArrayList<Track> trackList = dbAdapter.getAllTracks();
//
//		if (trackList.size() == 0) {
//			Log.d(TAG, "No stored tracks in local db found.");
//			return;
//		}
//
//		HashMap<String,String> trackJsonList = new HashMap<String,String>();
//
//		for (Track track : trackList) {
//			// Prevent emtpy tracks from being uploaded.
//			if (track.getNumberOfMeasurements() > 0) {
//				trackJsonList.put(track.getId(), createTrackJson(track));
//			}
//		}
//
//		Log.i("Size", String.valueOf(trackJsonList.size()));
//
//		objList = new ArrayList<JSONObject>();
//
//		for (String trackJsonString : trackJsonList.values()) {
//			obj = null;
//
//			try {
//				obj = new JSONObject(trackJsonString);
//				savetoSdCard(obj);
//				objList.add(obj);
//
//			} catch (JSONException e) {
//				Log.e(TAG, "Error parsing measurement string to JSON object.");
//				e.printStackTrace();
//			}
//
//		}
//		if (objList.size() > 0) {
//			Log.d("obd2", "Uploading: " + objList.size() + " tracks.");
//			new UploadAsyncTask().execute();
//		}
		new UploadAsyncTask().execute();
	}

	private class UploadAsyncTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... params) {
			
			//probably unnecessary
			if(dbAdapterLocal.getNumberOfStoredTracks() == 0)
				this.cancel(true);
			
			String username = ((ECApplication) context).getUser().getUsername();
			String token = ((ECApplication) context).getUser().getToken();
			String urlL = String.format(url, username);


			//iterate through the list of tracks :)
			for(Track t : dbAdapterLocal.getAllTracks()){
				JSONObject trackJSONObject = null;
				try {
					trackJSONObject = createTrackJson(t);
				} catch (JSONException e) {
					//the track wasn't JSON serializable. shouldn't occur.
					this.cancel(true);
					((ECApplication) context).createNotification("General Track error (JSON) Please contact envirocar.org");
				}
				//try next track if the track has no measurements
				if(t.getNumberOfMeasurements() == 0)
					continue;
				
				//save the track into a json file
				savetoSdCard(trackJSONObject,t.getId());
				//upload
				String httpResult = sendHttpPost(urlL, trackJSONObject, token, username);
				if (!httpResult.equals("-1")) {
					((ECApplication) context).createNotification("success");
					dbAdapterLocal.deleteTrack(t.getId());
					t.setId(httpResult);
					dbAdapterRemote.insertTrackWithMeasurements(t);
				}else{
					((ECApplication) context).createNotification("Upload failed with http code" + httpResult);
				}
				
			}
			
//			for (JSONObject object : objList) {
//				int statusCode = sendHttpPost(urlL, object, token, username);
//				if (statusCode != -1 && statusCode == 201) {
//					((ECApplication) context).createNotification("success");
//					// TODO remove tracks from local storage if upload was
//					// successful
//					// TODO method dbAdapter.removeTrackFromLocalDb(Track)
//					// needed
//					// }
//				}else{
//					((ECApplication) context).createNotification("Upload failed with http code" + statusCode);
//				}
//			}
			return null;
		}

	}

	/**
	 * Converts Track Object into track.create.json string
	 * 
	 * @return
	 * @throws JSONException 
	 */
	private JSONObject createTrackJson(Track track) throws JSONException {

		String trackName = track.getName();
		String trackDescription = track.getDescription();
		String trackSensorName = track.getSensorID();

		String trackElementJson = String
				.format("{ \"type\":\"FeatureCollection\",\"properties\": {\"name\": \"%s\", \"description\": \"%s\", \"sensor\": \"%s\"}, \"features\": [",
						trackName, trackDescription, trackSensorName);

		ArrayList<Measurement> measurements = track.getMeasurements();
		ArrayList<String> measurementElements = new ArrayList<String>();

		for (int i = 0; i < measurements.size(); i++) {
			String lat = String.valueOf(measurements.get(i).getLatitude());
			String lon = String.valueOf(measurements.get(i).getLongitude());
			DateFormat dateFormat1 = new SimpleDateFormat("y-MM-d");
			DateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss");
			dateFormat1.setTimeZone(TimeZone.getTimeZone("UTC"));
			dateFormat2.setTimeZone(TimeZone.getTimeZone("UTC"));
			String time = dateFormat1.format(measurements.get(i)
					.getMeasurementTime())
					+ "T"
					+ dateFormat2.format(measurements.get(i)
							.getMeasurementTime()) + "Z";
			String co2 = "0", consumption = "0";
			try {
				co2 = String.valueOf(track.getCO2EmissionOfMeasurement(i));
				consumption = String.valueOf(track
						.getFuelConsumptionOfMeasurement(i));
			} catch (FuelConsumptionException e) {
				e.printStackTrace();
			}

			String maf = String.valueOf(measurements.get(i).getMaf());
			String speed = String.valueOf(measurements.get(i).getSpeed());
			String measurementJson = String
					.format("{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Point\", \"coordinates\": [ %s, %s ] }, \"properties\": { \"time\": \"%s\", \"sensor\": \"%s\", \"phenomenons\": { \"CO2\": { \"value\": %s }, \"Consumption\": { \"value\": %s }, \"MAF\": { \"value\": %s }, \"Speed\": { \"value\": %s}} } }",
							lon, lat, time, trackSensorName, co2, consumption,
							maf, speed);
			measurementElements.add(measurementJson);
		}

		String measurementElementsJson = TextUtils.join(",",
				measurementElements);
		Log.d("measurementElem", measurementElementsJson);
		String closingElementJson = "]}";

		String trackString = String.format("%s %s %s", trackElementJson,
				measurementElementsJson, closingElementJson);
		Log.d("Track", trackString);
		
		
		return new JSONObject(trackString);
	}

	/**
	 * Uploads the json object to the server
	 * 
	 * @param url
	 *            Url
	 * @param jsonObjSend
	 *            The Json Object
	 * @param xToken
	 *            Token
	 * @param xUser
	 *            Username
	 * @return Server response status code
	 */
	private String sendHttpPost(String url, JSONObject jsonObjSend, String xToken,
			String xUser) {

		try {
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpPost httpPostRequest = new HttpPost(url);

			StringEntity se = new StringEntity(jsonObjSend.toString());
			se.setContentType("application/json");
			Log.d(TAG + "SE", jsonObjSend.toString());

			// Set HTTP parameters
			httpPostRequest.setEntity(se);
			httpPostRequest.setHeader("Content-type", "application/json");
			httpPostRequest.setHeader("X-Token", xToken);
			httpPostRequest.setHeader("X-User", xUser);

			HttpResponse response = (HttpResponse) httpclient
					.execute(httpPostRequest);
			
			String location = "";
			Header[] h = response.getAllHeaders();
			for (int i = 0; i< h.length; i++){
				if( h[i].getName().equals("Location")){
					location += h[i].getValue();
					break;
				}
			}
			
			String trackid = location.substring(location.lastIndexOf("/")+1, location.length());


			String statusCode = String.valueOf(response.getStatusLine()
					.getStatusCode());

			Log.d(TAG, String.format("%s: %s", statusCode));

			if(statusCode.equals(201)){
				return trackid;
			} else {
				return "-1";
			}

		} catch (Exception e) {
			Log.e(TAG, "Error occured while sending JSON file to server.");
			e.printStackTrace();
			return "-1";
		}

	}

	/**
	 * Saves a json object to the sd card
	 * 
	 * @param obj
	 *            the object to save
	 */
	private void savetoSdCard(JSONObject obj, String fileid) {
		File log = new File(context.getExternalFilesDir(null),"envirocar_track"+fileid+".json");
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), true));
			out.write(obj.toString());
			out.flush();
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Error saving tracks to SD card.", e);
		}
	}

}