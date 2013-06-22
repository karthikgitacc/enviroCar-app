package car.io.activity;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import car.io.R;
import car.io.adapter.DbAdapter;
import car.io.adapter.Track;
import car.io.application.ECApplication;
import car.io.views.TYPEFACE;

import com.actionbarsherlock.app.SherlockFragment;

public class ListMeasurementsFragmentLocal extends SherlockFragment {

	private ArrayList<Track> tracksList;
	private TracksListAdapter elvAdapter;
	private DbAdapter dbAdapter;
	private ExpandableListView elv;

	private ProgressBar progress;

	private int itemSelect;

	public View onCreateView(android.view.LayoutInflater inflater,
			android.view.ViewGroup container,
			android.os.Bundle savedInstanceState) {

		dbAdapter = ((ECApplication) getActivity().getApplication())
				.getDbAdapterLocal();

		View v = inflater.inflate(R.layout.list_tracks_layout_local, null);
		elv = (ExpandableListView) v.findViewById(R.id.list);
		progress = (ProgressBar) v.findViewById(R.id.listprogress);

		registerForContextMenu(elv);

		elv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				itemSelect = ExpandableListView.getPackedPositionGroup(id);
				Log.e("obd2", String.valueOf("Selected item: " + itemSelect));
				return false;
			}

		});

		return v;
	};

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getSherlockActivity().getMenuInflater();
		inflater.inflate(R.menu.context_item, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ArrayList<Track> tracks = dbAdapter.getAllTracks();
		Track track = tracks.get(itemSelect);
		switch (item.getItemId()) {
		case R.id.editName:
			Log.e("obd2", "editing track: " + itemSelect);
			Toast.makeText(getActivity(),
					"This function is not supported yet.", Toast.LENGTH_LONG)
					.show();
			// TODO change the name of the track
			return true;
		case R.id.deleteTrack:
			Log.e("obd2", "deleting item: " + itemSelect);
			dbAdapter.deleteTrack(track.getId());
			Toast.makeText(getActivity(), "Track deleted.", Toast.LENGTH_LONG)
					.show();
			tracksList.remove(itemSelect);
			elvAdapter.notifyDataSetChanged();
			return true;
		case R.id.uploadTrack:
			Log.e("obd2", "uploading item: " + itemSelect);
			Toast.makeText(
					getActivity(),
					"This function is not supported yet. Please upload all tracks at once via the menu.",
					Toast.LENGTH_LONG).show();
			// TODO implement this (not "mission critical")
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		elv.setGroupIndicator(getResources().getDrawable(
				R.drawable.list_indicator));
		elv.setChildDivider(getResources().getDrawable(
				android.R.color.transparent));

		this.tracksList = dbAdapter.getAllTracks();
		Log.i("obd", "Number of tracks: " + tracksList.size());
		if (elvAdapter == null)
			elvAdapter = new TracksListAdapter();
		elv.setAdapter(elvAdapter);
		elvAdapter.notifyDataSetChanged();

		// TODO update the list if new track is inserted into the database.

	}

	private class TracksListAdapter extends BaseExpandableListAdapter {

		@Override
		public int getGroupCount() {
			return tracksList.size();
		}

		@Override
		public int getChildrenCount(int i) {
			return 1;
		}

		@Override
		public Object getGroup(int i) {
			return tracksList.get(i);
		}

		@Override
		public Object getChild(int i, int i1) {
			return tracksList.get(i);
		}

		@Override
		public long getGroupId(int i) {
			return i;
		}

		@Override
		public long getChildId(int i, int i1) {
			return i;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getGroupView(int i, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000000 + i) {
				Track currTrack = (Track) getGroup(i);
				View groupRow = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_group_layout, null);
				TextView textView = (TextView) groupRow
						.findViewById(R.id.track_name_textview);
				textView.setText(currTrack.getName());
				groupRow.setId(10000000 + i);
				TYPEFACE.applyCustomFont((ViewGroup) groupRow,
						TYPEFACE.Newscycle(getActivity()));
				return groupRow;
			}
			return view;
		}

		@Override
		public View getChildView(int i, int i1, boolean b, View view,
				ViewGroup viewGroup) {
			if (view == null || view.getId() != 10000100 + i + i1) {
				Track currTrack = (Track) getChild(i, i1);
				View row = ViewGroup.inflate(getActivity(),
						R.layout.list_tracks_item_layout, null);
				TextView start = (TextView) row
						.findViewById(R.id.track_details_start_textview);
				TextView end = (TextView) row
						.findViewById(R.id.track_details_end_textview);
				TextView length = (TextView) row
						.findViewById(R.id.track_details_length_textview);
				TextView car = (TextView) row
						.findViewById(R.id.track_details_car_textview);
				TextView duration = (TextView) row
						.findViewById(R.id.track_details_duration_textview);
				TextView co2 = (TextView) row
						.findViewById(R.id.track_details_co2_textview);

				try {
					DateFormat sdf = DateFormat.getDateTimeInstance();
					DecimalFormat twoDForm = new DecimalFormat("#.##");
					DateFormat dfDuration = new SimpleDateFormat("HH:mm:ss:SSS"); // TODO:
																					// leave
																					// out
																					// millis
																					// when
																					// we
																					// have
																					// other
																					// data
					dfDuration.setTimeZone(TimeZone.getTimeZone("UTC"));
					start.setText(sdf.format(currTrack.getStartTime()) + "");
					end.setText(sdf.format(currTrack.getEndTime()) + "");
					Log.e("duration",
							currTrack.getEndTime() - currTrack.getStartTime()
									+ "");
					Date durationMillis = new Date(currTrack.getEndTime()
							- currTrack.getStartTime());
					duration.setText(dfDuration.format(durationMillis) + "");
					length.setText(twoDForm.format(currTrack.getLengthOfTrack())
							+ " km");
					car.setText(currTrack.getCarManufacturer() + " "
							+ currTrack.getCarModel());
					co2.setText("");
				} catch (Exception e) {

				}

				row.setId(10000100 + i + i1);
				TYPEFACE.applyCustomFont((ViewGroup) row,
						TYPEFACE.Newscycle(getActivity()));
				return row;
			}
			return view;
		}

		@Override
		public boolean isChildSelectable(int i, int i1) {
			return false;
		}

	}

}
