package it.duccius.musicplayer;

import android.support.v4.widget.DrawerLayout;

import it.duccius.download.DownloadAudio;
import it.duccius.download.DownloadFile;
import it.duccius.download.RowItem;
import it.duccius.musicplayer.R;
import it.duccius.musicplayer.Utilities.MyCallbackInterface;

import it.duccius.maps.MapService;
import it.duccius.maps.NavigationDataSet;
import it.duccius.maps.Placemark;
import it.duccius.maps.Trail;
import it.duccius.maps.TrailColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.view.LayoutInflater;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
 
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MapNavigation extends Activity implements OnCompletionListener, SeekBar.OnSeekBarChangeListener, LocationListener  {

	ProgressDialog progressDialog;
	
	GoogleMap mMap;
	//int _mapType = GoogleMap.MAP_TYPE_HYBRID;
	//int _mapType = GoogleMap.MAP_TYPE_NORMAL;
	
	private ImageButton btnPlay;
	private ImageButton btnForward;
	private ImageButton btnBackward;
	private ImageButton btnNext;
	private ImageButton btnPrevious;
	private ImageButton btnPlaylist;	
	//private ImageButton btnPOIplay;
	private ImageButton btnPOIinfo;
	private ImageButton btnTrails;
	
	private ImageView btnThumbnail;
	private ImageView imgThumbnail;
	
	private ImageButton songThumbnail;
		
	private SeekBar songProgressBar;
	private TextView songTitleLabel;
	private TextView songCurrentDurationLabel;
	private TextView songTotalDurationLabel;
	
	private LinearLayout  footer;
	private LinearLayout  toolbar;
	private LinearLayout timerDisplay;
	
	/*
	 *   http://android-er.blogspot.it/2012/05/add-and-remove-view-dynamically.html
	 *   Di seguito ci sono le dichiarazioni per caricare dinamicamente i fragment
	 */
	FrameLayout mainLayer;
	View mapLayer, thumbnailLayer;
	
	//-------------------------------------------------------------------------------
	private Spinner chooseTrail;
	// Media Player
	private  MediaPlayer mp;
	// Handler to update UI timer, progress bar etc,.
	private Handler mHandler = new Handler();;
	private SongsManager songManager;
	private Utilities utils;
	private int seekForwardTime = 5000; // 5000 milliseconds
	private int seekBackwardTime = 5000; // 5000 milliseconds
	private int currentSongIndex = 0; 
	private boolean isShuffle = false;
	private boolean isRepeat = false;
	// Di default la playlist coincide con gli audio presenti in locale
	//private ArrayList<AudioGuide> _playList = new ArrayList<AudioGuide>();
	// _guides: elenco delle audioguide dispoibili sul server pronte dda scaricare
	private ArrayList<AudioGuide> _guides = new ArrayList<AudioGuide>();
	
	private String _language = "ITA";
	//private TextView textLanguage;			
	public static boolean checkConn = false;		
	
	public String _urlDownloads = Utilities.getUrlDownloads();
	public String _filePath = Utilities.getTempSDFld();
//	public String _tempSdFld = Utilities.getTempSDFldLang(_language);        
//	public String _destSdFld = Utilities.getDestSDFldLang(_language);
	//public String _downloadsFileName = "downloads.xml";
	public String _downloadsSDPath = Utilities.getDownloadsSDPath();

	public String _clickedMarker ;
	public int _clickedMarkerIndex;
	
	public int _timeoutSec = 5;		
	
	ArrayList<AudioGuide> _localAudioGuideListLang = new ArrayList<AudioGuide>();
	ArrayList<AudioGuide> _audioGuideListLang = new ArrayList<AudioGuide>();
//	ArrayList<AudioGuide> _audioToDownloadLang = new ArrayList<AudioGuide>();
	AudioGuideList _audioToDownloadLang = new AudioGuideList();
	
	NavigationDataSet _nDs = new NavigationDataSet();
	String _currentPOIcoords = "";
	
	LocationManager _locationManager;
	Location _location;
	private String provider;
	
	private  ArrayList<Trail> _trails = new  ArrayList<Trail>(); 
	private int _selectedTrail;
	
	private ArrayList<Polyline> _activePolines = new ArrayList<Polyline>();
	
	//----------------------------
	private DrawerLayout mDrawerLayout;
    //----------------------------
    private Context contesto;
    
	public boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    return netInfo != null && netInfo.isConnectedOrConnecting();
	}
	
	public boolean downloadAudioGuideList ()
	{			
		try {
			if( !isOnline())
			{
				return false;
			}
			contesto = this.getBaseContext();
			//______________________________________________________________
			// Scarico il file downloads.xml
            //______________________________________________________________
			ArrayList<String> arL = new ArrayList<String>();			
			arL.add(_urlDownloads);			
			starDownload(arL,_filePath,new MyCallbackInterface() {
            //______________________________________________________________
				
	            @Override
	            public void onDownloadFinished(List<RowItem> rowItems) {
	            	/*
	            	 * Scaricato il file con l'elenco degli audio e dei percorsi
	            	 * preparo tutto per la applicazione
	            	 * poi confronto cosa manca dei file elencati nel file downloads.       	 
	            	 */	            		
	            	leggiFileDownloads();
	            	initializeMap();
	            	setupMediaPlayer();
	            	
	            	checkForUpdates();
	            	
	            	//==========================================================
	            	// Qui chiedo se si vuole scaricare tutti gli audio in una volta o in seguito
	            	askForFileDownload();
	            	//===========================================================	            	
            	 
	            }

			private void askForFileDownload() {
				new AlertDialog.Builder(MapNavigation.this)
				.setTitle(R.string.mapnavigation_download_title)
				.setMessage(R.string.mapnavigation_download_message)
				.setNegativeButton(R.string.mapnavigation_download_nobutton, null)
				.setPositiveButton(R.string.mapnavigation_download_yesbutton, new DialogInterface.OnClickListener()  {

				    public void onClick(DialogInterface arg0, int arg1) {
				    	  ArrayList<String> arL = new ArrayList<String>();
				    	  arL = Utilities.getUrlsToDownload(_audioToDownloadLang);
				    	  starDownload(arL,Utilities.getDestSDFldLang(_language),new MyCallbackInterface() { 			            		  
				    	   @Override
					            public void onDownloadFinished(List<RowItem> rowItems) {
				    		   // Questo metodo serve ad aggiornare i dati dopo il download di nuovi file
				    		   		checkForUpdates();
					            }
					        });	
				    }
				}).create().show();
			}
	        });
			return true;
			
		} catch (Exception e) {
			Log.d("downloadMapItemes()", e.getMessage());
			return false;
		} 
	}
//	public void checkNewAudio()
//	{
//		//leggiFileDownloads();
//		// il metodo checkForUpdates() aggiorna:
//		// - _localAudioGuideListLang:	elenco di audioguide presenti nella scheda SD per una determinata lingua
//		// - _audioGuideListLang:		elenco di audioguide disponibili sul server per una determinata lingua
//		// - _audioToDownloadLang:		elenco di audioguide presenti sul server ma non presenti su SD per una determinata lingua
//		checkForUpdates();
//		
//		//initializeMap();
//		//setupMediaPlayer();
//	}

	private void leggiFileDownloads() {
		File f = new File(_downloadsSDPath);
		if(f.exists()) {  			
			_nDs = MapService.getNavigationDataSet("file://"+_downloadsSDPath);
			_nDs.sort();
			
			_trails =  MapService._trails;
		}
	}
	public void setupMediaPlayer()
	{
		//_playList = _localAudioGuideListLang;
		checkEmptyAGList();
			
		//textLanguage.setText(_language);
				
		// Mediaplayer
		mp = new MediaPlayer();		
		utils = new Utilities();
		
		// Listeners
		songProgressBar.setOnSeekBarChangeListener(this); // Important
		mp.setOnCompletionListener(this); // Important						
					
	}
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("PROVA");
		contesto = this.getBaseContext();

		//setContentView(R.layout.sena);	
		setContentView(R.layout.sena_fragments);
						
		SharedPreferences settings = getSharedPreferences("SenaVetus", 0);  		
		
	    songManager = new SongsManager(_language);		
	  
		//###############################	    
	    
		// getAudioGuideList(): recupera downloads.xml e contestualmente scarico gli .mp3 nuovi, valorizzando:
		// - _localAudioGuideListLang:	elenco di audioguide presenti nella scheda SD per una determinata lingua
		// - _audioGuideListLang:		elenco di audioguide disponibili sul server per una determinata lingua
		// - _audioToDownloadLang:		elenco di audioguide presenti sul server ma non presenti su SD per una determinata lingua_audioToDownloadLang, _guides, _audioGuideListLang, _localAudioGuideListLang
	    // - _nDs
	    // - _trails
	    
		if (!getAudioGuideList())
		{
			Toast.makeText(getApplicationContext(), "Impossibile connettersi al server. Verificare la connessione.", Toast.LENGTH_LONG).show();
			return;
		}
			
		getCurrentLocation();		
	    
		getViewElwments();
		/**
		 * Play button click event
		 * plays a song and changes button to pause image
		 * pauses a song and changes button to play image
		 * */
		btnPlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//hidePOIbtns();
				// check for already playing
				if(_localAudioGuideListLang != null && _localAudioGuideListLang.size()>0){
				if(mp.isPlaying()){
					if(mp!=null){
						mp.pause();
						// Changing button image to play button
						btnPlay.setImageResource(R.drawable.btn_play);
					}
				}else{
					// Resume song
					if(mp!=null){
						mp.start();
						// Changing button image to pause button
						btnPlay.setImageResource(R.drawable.btn_pause);
					}
				}
				}				
			}
		});
		
		/**
		 * Forward button click event
		 * Forwards song specified seconds
		 * */
		btnForward.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//hidePOIbtns();
				if(_localAudioGuideListLang != null && _localAudioGuideListLang.size()>0){
					// get current song position				
					int currentPosition = mp.getCurrentPosition();
					// check if seekForward time is lesser than song duration
					if(currentPosition + seekForwardTime <= mp.getDuration()){
						// forward song
						mp.seekTo(currentPosition + seekForwardTime);
					}else{
						// forward to end position
						mp.seekTo(mp.getDuration());
					}
				}
			}
		});
		
		/**
		 * Backward button click event
		 * Backward song to specified seconds
		 * */
		btnBackward.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//hidePOIbtns();
				if(_localAudioGuideListLang != null && _localAudioGuideListLang.size()>0){
					// get current song position				
					int currentPosition = mp.getCurrentPosition();
					// check if seekBackward time is greater than 0 sec
					if(currentPosition - seekBackwardTime >= 0){
						// forward song
						mp.seekTo(currentPosition - seekBackwardTime);
					}else{
						// backward to starting position
						mp.seekTo(0);
					}
				}
			}
		});
		
		/**
		 * Next button click event
		 * Plays next song by taking currentSongIndex + 1
		 * */
		btnNext.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {								
				
				if(_localAudioGuideListLang != null && _localAudioGuideListLang.size()>0){	
					// check if next song is there or not
					if(currentSongIndex < (_localAudioGuideListLang.size() - 1)){
						playSong(currentSongIndex + 1);
						currentSongIndex = currentSongIndex + 1;
					}else{
						// play first song
						playSong(0);
						currentSongIndex = 0;
					}
				}					
			}
		});
		
		/**
		 * Back button click event
		 * Plays previous song by currentSongIndex - 1
		 * */
		btnPrevious.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//hidePOIbtns();
				if(_localAudioGuideListLang != null && _localAudioGuideListLang.size()>0)
				{
					if(currentSongIndex > 0){
						playSong(currentSongIndex - 1);
						currentSongIndex = currentSongIndex - 1;
					}else{
						// play last song
						playSong(_localAudioGuideListLang.size() - 1);
						currentSongIndex = _localAudioGuideListLang.size() - 1;
					}
				}
			}
		});
		
		/**
		 * Button Click event for Play list click event
		 * Launches list activity which displays list of songs
		 * Per semplificare l'interfaccia ho tolto la possibilit� di scegliere i brani dalla playlist
		 * */
		btnPOIinfo.setOnClickListener(new View.OnClickListener() 
		{
			
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(getApplicationContext(), DownloadAudio.class);
				
		        Bundle b = new Bundle();
		        b.putSerializable("_playList", _localAudioGuideListLang);
		        b.putSerializable("_audioToDownloadLang", _audioToDownloadLang.getAudioGuides());		        
		        b.putString("language", _language);		 
		        // Add the bundle to the intent.
		        i.putExtras(b);
				startActivityForResult(i, 3);	
				//finish();
			}
		});
		/*
		 * Questo bottone deve mostrare una lista con i possibili percorsi da seguire
		 */
		btnTrails.setOnClickListener(new View.OnClickListener() 
		{			
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(getApplicationContext(), TrailList.class);
				
		        Bundle b = new Bundle();
		        b.putSerializable("_trails", _trails);		        	
		        // Add the bundle to the intent.
		        i.putExtras(b);
				startActivityForResult(i, 2);	
				
				
				//finish();
			}
		});
		btnPlaylist.setOnClickListener(new View.OnClickListener() 
		{
			
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(getApplicationContext(), PlayListAudio.class);
							
		        Bundle b = new Bundle();
		        b.putSerializable("_playList", _localAudioGuideListLang);
		        b.putInt("currentSongIndex", currentSongIndex);
		        b.putSerializable("_localAudioGuideListLang", _localAudioGuideListLang);		        
		        b.putString("language", _language);		 
		        // Add the bundle to the intent.
		        i.putExtras(b);
				startActivityForResult(i, 1);	
				//finish();
			}
		});
		
		btnThumbnail.setOnClickListener(new View.OnClickListener() 
		{
			
			@Override
			public void onClick(View arg0) {				
				btnThumbnail.setVisibility(View.GONE);				
			}
		});
	}
		
	// Provo a scaricare una nuova versione del file,
	// se non ci riesco allora cerco di usare una versione gi� presente in locale
	// se non ho neanche questa opzione restituisco false.
	private boolean getAudioGuideList() {
		boolean downloadOk = downloadAudioGuideList();
		if (downloadOk)
		{	
			return true;
		}
		else
		{
			File picFolder = new File(_downloadsSDPath);
			if (picFolder.exists())
			{
				Toast.makeText(getApplicationContext(), "Impossibile connettersi al server. Si pu� comunque procedere con una versione obsoleta dei file.", Toast.LENGTH_LONG).show();
				return true;
			}
			else{
				Toast.makeText(getApplicationContext(), "Impossibile connettersi al server. Verificare che si abbia accesso alla rete, chiudere l'applicazione e riprovare pi� tardi.", Toast.LENGTH_LONG).show();
				
				return false;
			}
		}				
	}
//######################################################################
	
	private void initializeMap() {
		//Location currentLocation = getCurrentLocation();
		Location currentLocation = _location;
		//LatLng from = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());		 
		LatLng from = new LatLng(43.327671,11.325371);
		LatLng to = from;
		if(!_nDs.getPlacemarks().isEmpty())
		{
			if (currentLocation != null)
				from = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
			else			
				from = new LatLng(_nDs.getPlacemarks().get(0).getLongitude(),_nDs.getPlacemarks().get(0).getLatitude());
			
			to = new LatLng(_nDs.getPlacemarks().get(0).getLongitude(),_nDs.getPlacemarks().get(0).getLatitude());
		}
		
		setUpMapIfNeeded(from, to);
//--------------		
		if ( null != _trails ){					
			addTrail();	
			if (_selectedTrail>0){
				// Mi posiziono al primo Poi del percorso
				// nel percorso libero non ci sono placemarks
				to = new LatLng(_trails.get(_selectedTrail).getTrailPlacemarks().get(0).getLongitude(),_trails.get(_selectedTrail).getTrailPlacemarks().get(0).getLatitude());
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(to, 15));
			}
		}			
//--------------				
		
		mMap.setOnCameraChangeListener(getCameraChangeListener());
		mMap.setOnMarkerClickListener(getMarkerClickListener());		
	}

private void addTrail() {
	
	for(Polyline pl:_activePolines){
		pl.remove();								
	}
	
	_activePolines.clear();
	
	// Ilprimo percorso � sempre quello libero
	if(_selectedTrail >0)
	{
	Trail trail = _trails.get(_selectedTrail);
	
	PolylineOptions rectOptions = createPolyline(trail);
		
	Polyline polyline = mMap.addPolyline(rectOptions);
	
	_activePolines.add(polyline);
	}
}

	private PolylineOptions createPolyline(Trail trail) {
		PolylineOptions rectOptions = new PolylineOptions();
		if(null != trail.getTrailPlacemarks())
		
		for (Placemark pm: trail.getTrailPlacemarks() )
		{
			rectOptions.add(new LatLng(pm.getLongitude(),pm.getLatitude()));			
		}
		rectOptions.width(14);
		//rectOptions.color(Color.rgb(249, 247, 166));
		String colore = TrailColor.GetColor(_trails.indexOf(trail));
		String[] rgb = colore.split(",");
		rectOptions.color(Color.rgb(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2])));					
		
		rectOptions.geodesic(true); // Closes the polyline.
		return rectOptions;
	}
	
	public OnCameraChangeListener getCameraChangeListener()
	{
	    return new OnCameraChangeListener() 
	    {
	        @Override
	        public void onCameraChange(CameraPosition position) 
	        {
	            addItemsToMap(MapNavigation.this._nDs);
	        }
	    };
	}
	//Vedi anche:
	// http://stackoverflow.com/questions/14123243/google-maps-api-v2-custom-infowindow-like-in-original-android-google-maps
	
	//Note that the type "Items" will be whatever type of object you're adding markers for so you'll
	//likely want to create a List of whatever type of items you're trying to add to the map and edit this appropriately
	//Your "Item" class will need at least a unique id, latitude and longitude.
	private HashMap<String, Marker> courseMarkers = new HashMap<String, Marker>();
	
	private Marker _activeMarker;
	private Marker _previousMarker;
	
		private void addItemsToMap(NavigationDataSet items)
		{
		    if(this.mMap != null)
		    {
		        //This is the current user-viewable region of the map
		        LatLngBounds bounds = this.mMap.getProjection().getVisibleRegion().latLngBounds;
		 
		        //Loop through all the items that are available to be placed on the map
		        for(Placemark item : items.getPlacemarks()) 
		        {
		 
		            //If the item is within the the bounds of the screen
		            if(bounds.contains(new LatLng(item.getLongitude(),item.getLatitude())))
		            {
		                //If the item isn't already being displayed
		                if(!courseMarkers.containsKey(item.getTitle()))
		                {
		                    //Add the Marker to the Map and keep track of it with the HashMap
		                    //getMarkerForItem just returns a MarkerOptions object
		                	Marker m = this.mMap.addMarker(getMarkerForItem(item));
		                    this.courseMarkers.put(item.getTitle(), m);
		                }
		            }
		 
		            //If the marker is off screen
		            else
		            {
		                //If the course was previously on screen
		                if(courseMarkers.containsKey(item.getTitle()))
		                {
		                    //1. Remove the Marker from the GoogleMap
		                    courseMarkers.get(item.getTitle()).remove();
		                 
		                    //2. Remove the reference to the Marker from the HashMap
		                    courseMarkers.remove(item.getTitle());
		                }
		            }
		        }
		    }
		}
		private MarkerOptions getMarkerForItem(Placemark item) {
		  LatLng MarkerPos = new LatLng ( item.getLongitude(),item.getLatitude());

		  MarkerOptions mo = new MarkerOptions()

	        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
	        .position(MarkerPos)
	        .title(item.getTitle());	         
			return mo;
			
		}
		private boolean checkReadyToPlay() {
			boolean res = false;
			ArrayList<String> alString = getAdapterSource(_localAudioGuideListLang); 
			_clickedMarkerIndex = alString.indexOf(_clickedMarker);
			if (_clickedMarkerIndex>-1)
			{
				//Se ho l'audio lo trasmetto subito
				playSong(_clickedMarkerIndex);	
				res=true;
			}
			return res;
		}
		
		private void checkReadyToDownload() {
			ArrayList<String> alString = getAdapterSource(_audioToDownloadLang); 
			int clickedMarkerIndex = alString.indexOf(_clickedMarker);
			if (clickedMarkerIndex>-1)
			{
				if (_previousMarker != null && _activeMarker != null &&_previousMarker.getTitle().equals(_activeMarker.getTitle()))
				{
					if(_audioToDownloadLang != null && _audioToDownloadLang.getAudioGuides().size()>0 )
					{					
						
						//playSong(_clickedMarkerIndex);
						
						ArrayList<String> arL = new ArrayList<String>();
						AudioGuide ag = _audioToDownloadLang.getFromPosition(clickedMarkerIndex);
						//String str = ag.getPath();
						String str = Utilities.getMp3UrlFromName(ag.getName());
						arL.add(str);					
						
						starDownload(arL,new MyCallbackInterface() {

				            @Override
				            public void onDownloadFinished(List<RowItem> rowItems) {				                
				            	checkForUpdates();
				            	_activeMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				            	_activeMarker.setSnippet("Click and play" );
				            }
				        });
					}							
				}
				else
				{
					_activeMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));	
					_activeMarker.setSnippet("Click and Download" );
				}					
			}				
		}
		private void starDownload(ArrayList<String> arL, MyCallbackInterface callback) {
			ProgressDialog progressDialog = new ProgressDialog((this));
			progressDialog.setTitle("In progress...");
			progressDialog.setMessage("Loading...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setIndeterminate(false);
			progressDialog.setMax(100);
			progressDialog.setIcon(R.drawable.arrow_stop_down);
			progressDialog.setCancelable(true);
			progressDialog.show();
			DownloadFile df = new DownloadFile(this,_language,progressDialog, callback);
			df.execute(arL);
		}
		private void starDownload(ArrayList<String> arL, String destPath,MyCallbackInterface callback) {
			ProgressDialog progressDialog = new ProgressDialog((this));
			progressDialog.setTitle("In progress...");
			progressDialog.setMessage("Loading...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setIndeterminate(false);
			progressDialog.setMax(100);
			progressDialog.setIcon(R.drawable.arrow_stop_down);
			progressDialog.setCancelable(true);
			progressDialog.show();
			DownloadFile df = new DownloadFile(this,_language,destPath,progressDialog,callback);
			df.execute(arL);
		}		
	/*
	 * Metodo ciamato quando si clicca su un POI della mappa.
	 * Se abbiamo l'audio viene eseguito, altrimenti si prova a scaricarlo.
	 */
	public OnMarkerClickListener getMarkerClickListener()
	{
	    return new OnMarkerClickListener() 
	    {	       
			@Override
			public boolean onMarkerClick(Marker marker) {
			
				//----------
				_previousMarker = _activeMarker;				
				_activeMarker = marker;
				//----------
				
				_clickedMarker = marker.getTitle();								
				
				if (!checkReadyToPlay())
					checkReadyToDownload();
								
				return false;
			}									
	    };	    
	}

	private void updateThumbnail(AudioGuide agMarker) {
		String nome = Environment.getExternalStoragePublicDirectory(ApplicationData.getPicFolder()+"/"+agMarker.getName()+".jpg").toString();				
		Bitmap bmp = BitmapFactory.decodeFile(nome);							
		
		// http://android-er.blogspot.it/2012/05/add-and-remove-view-dynamically.html
		mainLayer = (FrameLayout)findViewById(R.id.main_fragment);
		LayoutInflater inflater = getLayoutInflater();
		thumbnailLayer = inflater.inflate(R.layout.thumbnail_fragment, null);
		
		if (mapLayer != null)
			mainLayer.removeView(mapLayer);
		mainLayer.addView(thumbnailLayer);
		
		imgThumbnail  = (ImageView) findViewById(R.id.imgThumbnail);
		imgThumbnail.setImageBitmap(bmp);
		_thumbnail_ON = true;
		
		toolbar.setVisibility(View.GONE);
	}
	
	boolean _thumbnail_ON;
	
	private ArrayList<String> getAdapterSource(ArrayList<AudioGuide> sourceList) {
		//_sdAudios = getSdAudios();	
		SongsManager sm = new SongsManager(_language);				
		
		ArrayList<String> sdAudiosStrings  = sm.getSdAudioStrings(sourceList);
		return sdAudiosStrings;
	}
	private void setUpMapIfNeeded(LatLng from, LatLng to) {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (mMap == null) {
	    	
	    	// meglio usare getFragmentManager
	    	//https://developers.google.com/maps/documentation/android/
	        //mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
	        //                    .getMap();
	        
	        //-----------------------------------------------------------------------------	    	
	    	addMapFragment();
	        //-----------------------------------------------------------------------------
	    	
	        // Check if we were successful in obtaining the map.
	        if (mMap != null) {
	            // The Map is verified. It is now safe to manipulate the map.
	        	//https://developers.google.com/maps/documentation/android/views
	        	// Move the camera instantly to Sydney with a zoom of 15.
//	        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 15));
//
//	        	// Zoom in, animating the camera.
//	        	mMap.animateCamera(CameraUpdateFactory.zoomIn());
//
//	        	// Zoom out to zoom level 10, animating with a duration of 2 seconds.
//	        	mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);

	        	// Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
	        	CameraPosition cameraPosition = new CameraPosition.Builder()
	        	    .target(from)      // Sets the center of the map to Mountain View
	        	    .zoom(8)                   // Sets the zoom
	        	    .bearing(90)                // Sets the orientation of the camera to east
	        	    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
	        	    .build();                   // Creates a CameraPosition from the builder
	        	mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
	        	
	        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(to, 19));
	        	
	        	mMap.setMyLocationEnabled(true);
	        	mMap.setMapType(Utilities.getMapType());
	        }
	    }
	}

	private void addMapFragment() {
		// http://android-er.blogspot.it/2012/05/add-and-remove-view-dynamically.html
		mainLayer = (FrameLayout)findViewById(R.id.main_fragment);
		LayoutInflater inflater = getLayoutInflater();
		mapLayer = inflater.inflate(R.layout.map_fragment, null);
		mainLayer.addView(mapLayer);
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map1))
		        .getMap();
	}

	@SuppressWarnings("unchecked")
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		//Ritorno dalla pagina della playlist
	   if (requestCode == 1)
	   {		  	
		    try
		    {		   if(resultCode == RESULT_OK) 	{			
		    	
					    	int newPlayListIndex;
					    	newPlayListIndex = intent.getExtras().getInt("currentSongIndex", -1);
					    	if ((newPlayListIndex>-1)){
					    		currentSongIndex =newPlayListIndex;
					    		playSong(currentSongIndex);
					    	}
		    			}
		    }
		    catch(Exception e)
		    {
		    	Log.d("zzzz", e.toString());
		    }
	   }
	   // Ritorno dopo aver selezionato un percorso
	   if (requestCode == 2)
	   {		  	
		    try
		    {		   
		    	if(resultCode == RESULT_OK) 	{			
		    	
		    	_selectedTrail = intent.getIntExtra("_selectedTrail", 0);	
		    	}
		    	initializeMap();
		    }
		    catch(Exception e)
		    {
		    	Log.d("zzzz", e.toString());
		    }
	   }
	// Ritorno dalla pagina di download
	   if (requestCode == 3)
	   {		  	
		   try
		    {		   
			   if(resultCode == RESULT_OK) 	{
			
////					    	if (mp != null)
////					    		mp.release();
//			    	//checkNewAudio();
//			    	int newPlayListIndex;
//			    	newPlayListIndex = intent.getExtras().getInt("currentSongIndex", -1);
//			    	if ((newPlayListIndex>-1)){
//			    		currentSongIndex =newPlayListIndex;
//			    		playSong(currentSongIndex);
//			    	}
				   checkForUpdates();
    			}
		    }
		    catch(Exception e)
		    {
		    	Log.d("zzzz", e.toString());
		    }
	   }
	}
	private void setupAudioThumbnail(String imgName) {
//		imgName = imgName.substring(4);
			Bitmap bmp = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(ApplicationData.getPicFolder()+"/"+imgName+".jpg").toString());		
			songThumbnail.setImageBitmap(bmp);		
	}
	
	/*
	 * Leggo quali audio sono presenti nel file downloads.xml
	 * poi li confronto con quelli presenti nella cartella locale
	 * infine restituisco true se esistono nuovi file da scaricare.
	 * Il confronto viene fatto sul solo ttributo @name
	 * non sono gestite le versioni diverse per audio che hanno lo stesso @name.
	 * Gli eventuali nuovi file sono elencati in _audioToDownloadLang ed il metodo rende true
	 */
	@SuppressWarnings("unchecked")	
	private boolean checkForUpdates() {
		boolean res = false;
		// Prima recupero tutti gli elementi del file downloads.xml e li etto in _guides
		if(songManager.loadGuideList(_guides))
		{
			// Creo una copia di _guides con i soli elementi che si accordano per lingua
			_audioGuideListLang = songManager.guideListByLang(_guides);
			
			// Creo una nuova lista con gli audio presenti nella SD, ricavando il Title dal file downloads.xml
			_localAudioGuideListLang = songManager.getSdAudioList(_audioGuideListLang);
			Collections.sort(_localAudioGuideListLang);
			
			//boolean result = songManager.getAudioToDownload(_localAudioGuideListLang, _audioGuideListLang);
			//_audioToDownloadLang = _audioGuideListLang;
			AudioGuideList localAudioGuideListLang = new AudioGuideList();
			localAudioGuideListLang.setAudioGuides(_localAudioGuideListLang);
			AudioGuideList audioGuideListLang = new AudioGuideList();
			audioGuideListLang.setAudioGuides(_audioGuideListLang);
			
			_audioToDownloadLang = songManager.getAudioToDownload(localAudioGuideListLang,audioGuideListLang );
			
			
			Collections.sort(_audioToDownloadLang);
			
			if (!_audioToDownloadLang.getAudioGuides().isEmpty())
			{
				btnPOIinfo.setVisibility(View.VISIBLE);
				Toast.makeText(getApplicationContext(), "Sono disponibili nuove audiogide\n. Accedi alla sezione 'Aggiornamenti' e clicca su 'Download'.", Toast.LENGTH_SHORT).show();
			}
			else
				btnPOIinfo.setVisibility(View.INVISIBLE);
			res = true;
		}
		return res;
	}

	private ArrayList<AudioGuide> refreshPlayList() {
		ArrayList<AudioGuide> list = new ArrayList<AudioGuide>();
		if ( _localAudioGuideListLang != null && _localAudioGuideListLang.isEmpty())
		{
			list = songManager.getSdAudioList(_audioGuideListLang);
		}	
		else
		{
			list = _localAudioGuideListLang;
		}
		return list;
	}
	private void checkEmptyAGList() {
		if ( _localAudioGuideListLang != null && _localAudioGuideListLang.isEmpty())
		{
			Toast.makeText(getApplicationContext(), "Non � dosponibile nessuna guida audio.\n. Scaricane di nuove dalla sezione 'Aggiornamenti' dal pulsante in alto a destra.", Toast.LENGTH_LONG).show();
		}
	}


	private void getViewElwments() {
		// All player buttons
		btnPlay = (ImageButton) findViewById(R.id.btnPlay);
		btnForward = (ImageButton) findViewById(R.id.btnForward);
		btnBackward = (ImageButton) findViewById(R.id.btnBackward);
		btnNext = (ImageButton) findViewById(R.id.btnNext);
		btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
		btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
		//songThumbnail = (ImageButton) findViewById(R.id.thumbnail);	
		
		//btnPOIdownload  = (ImageButton) findViewById(R.id.btnPOIdownload);		
		//btnPOIplay  = (ImageButton) findViewById(R.id.btnPOIplay);
		btnPOIinfo  = (ImageButton) findViewById(R.id.btnPOIinfo);		
		btnTrails = (ImageButton) findViewById(R.id.btnTrails);
		btnThumbnail  = (ImageButton) findViewById(R.id.thumbnail);
				
		songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
		songTitleLabel = (TextView) findViewById(R.id.songTitle);
		songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
		songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);
		//textLanguage = (TextView) findViewById(R.id.textLanguage);
		
		footer = (LinearLayout) findViewById(R.id.player_footer_bg);
		toolbar = (LinearLayout) findViewById(R.id.toolbar);
		timerDisplay = (LinearLayout) findViewById(R.id.timerDisplay);
		
//		ArrayList<String> trailNames= MapService.getTrailNames();
//		chooseTrail = (Spinner) findViewById(R.id.trails);
		//chooseTrail.setAdapter(new ArrayAdapter<String>(this,
	    //            R.layout.list_item, trailNames));
	        // Set the list's click listener
		//chooseTrail.setOnItemClickListener(new DrawerItemClickListener());

	        
		
	}
	private String getDestSDFld() {
		String sourcePath = Environment.getExternalStorageDirectory().toString()+"/"+ ApplicationData.getAppName()+"/"+_language;
		return sourcePath;
	}
	private String getPicFld() {
		String sourcePath = Environment.getExternalStorageDirectory().toString()+"/"+ ApplicationData.getAppName()+"/";
		return sourcePath;
	}
	private String getAudioName(String url)
	 {
		 String title = "";
		 String [] tokens = url.split("/");
		 title = tokens[tokens.length-1];
		 return title;
	 }
	/**
	 * Function to play a song
	 * @param songIndex - index of song
	 * */
	public void  playSong(int songIndex){
		// Play song
		try {
			
			AudioGuide ag = (AudioGuide) _localAudioGuideListLang.get(songIndex);
			updateThumbnail(ag);					
		
			//String audioPath = ag.getPath();
			String audioPath =getDestSDFld() +File.separator+ getAudioName(ag.getPath());
        	mp.reset();
        	//audioPath = "/mnt/sdcard/Music/02_PortaCamollia.mp3";
			mp.setDataSource(audioPath);
			//http://stackoverflow.com/questions/9008770/media-player-called-in-state-0-error-38-0
			mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			    @Override
			    public void onPrepared(MediaPlayer mp) {
			    	try{
			    	mp.start();
			    	setFooterVisibility(View.VISIBLE);
			    	btnPlay.setImageResource(R.drawable.btn_pause);
			    	AudioGuide ag = (AudioGuide) _localAudioGuideListLang.get(currentSongIndex);
					//updateThumbnail(ag);
			    	}
			    	catch(Exception e)
			    	{
			    		Log.d("playSong()", e.getMessage() + e.getStackTrace());
			    	}
			    }
			});
			mp.prepareAsync();
						
			// set Progress bar values
			songProgressBar.setProgress(0);//
			songProgressBar.setMax(100);
			
			// Updating progress bar
			updateProgressBar();	
			
			//songTitleLabel.setText(ag.getTitle());
			//_activeMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
			
			LatLng poiLatLong = new LatLng(Double.parseDouble(ag.getLng()),Double.parseDouble(ag.getLat()));
			
			mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(poiLatLong, 19));
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setFooterVisibility(int visibility) {
		//btnThumbnail.setVisibility(visibility);
		footer.setVisibility(visibility);
		timerDisplay.setVisibility(visibility);
		
		songProgressBar.setVisibility(visibility);
		//hidePOIbtns();
	}
	
	/**
	 * Update timer on seekbar
	 * */
	public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);        
    }	
	
	/**
	 * Background Runnable thread
	 * */
	private Runnable mUpdateTimeTask = new Runnable() {
		   public void run() {
			   long totalDuration = mp.getDuration();
			   long currentDuration = mp.getCurrentPosition();
			  
			   // Displaying Total Duration time
			   songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
			   // Displaying time completed playing
			   songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));
			   
			   // Updating progress bar
			   int progress = (int)(utils.getProgressPercentage(currentDuration, totalDuration));
			   //Log.d("Progress", ""+progress);
			   songProgressBar.setProgress(progress);
			   
			   // Running this thread after 100 milliseconds
		       mHandler.postDelayed(this, 100);
		   }
		};
		
	/**
	 * 
	 * */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		
	}

	/**
	 * When user starts moving the progress handler
	 * */
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// remove message Handler from updating progress bar
		mHandler.removeCallbacks(mUpdateTimeTask);
    }
	
	/**
	 * When user stops moving the progress hanlder
	 * */
	@Override
    public void onStopTrackingTouch(SeekBar seekBar) {
		mHandler.removeCallbacks(mUpdateTimeTask);
		int totalDuration = mp.getDuration();
		int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
		
		// forward or backward to certain seconds
		mp.seekTo(currentPosition);
		
		// update timer progress again
		updateProgressBar();
    }

	/**
	 * On Song Playing completed
	 * if repeat is ON play same song again
	 * if shuffle is ON play random song
	 * */
	@Override
	public void onCompletion(MediaPlayer arg0) {
		
		// check for repeat is ON or OFF
		if(isRepeat){
			// repeat is on play same song again
			playSong(currentSongIndex);
		} else if(isShuffle){
			// shuffle is on - play a random song
			Random rand = new Random();
			currentSongIndex = rand.nextInt((_localAudioGuideListLang.size() - 1) - 0 + 1) + 0;
			playSong(currentSongIndex);
		} else{
			setFooterVisibility(View.GONE);
			btnPlay.setImageResource(R.drawable.btn_play);
		}
	}
		
	@Override
	 public void onDestroy(){
	 super.onDestroy();
	    mp.release();
	    // http://stackoverflow.com/questions/13854196/application-force-closed-when-exited-android
	    mHandler.removeCallbacks(mUpdateTimeTask);
	 }

	/**
	 * Recupera l'attuale posizione e la assegna a '_location'
	 * Uso 'PASSIVE_PROVIDER' perch� con altre soluzioni ho avuto problemi.
	 * 
	 * http://www.vogella.com/tutorials/AndroidLocationAPI/article.html
	 * http://stackoverflow.com/questions/19621882/getlastknownlocation-returning-null?rq=1
	 */
	private void getCurrentLocation() {
		_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    Criteria criteria = new Criteria();
	    provider = _locationManager.getBestProvider(criteria, false);
	    _locationManager.requestLocationUpdates(provider, 400, 1, this);
	    _location = _locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);	    	    	
	}
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		 if(_location == null)
		    {
		    	_location = location;
		    }		
	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	  @Override
	  protected void onResume() {
	    super.onResume();
	    _locationManager.requestLocationUpdates(provider, 400, 1, this);
	  }
	  @Override
	  public void onBackPressed(){
		    if (_thumbnail_ON) 
		    {
		    	mainLayer = (FrameLayout)findViewById(R.id.main_fragment);
				
				if (thumbnailLayer != null)
					mainLayer.removeView(thumbnailLayer);
				mainLayer.addView(mapLayer);
								
				toolbar.setVisibility(View.VISIBLE);
				
				_thumbnail_ON = false;
		        
		    }else{
		    	new AlertDialog.Builder(this)
		        .setTitle(R.string.mapnavigation_exit_title)
		        .setMessage(R.string.mapnavigation_exit_message)
		        .setNegativeButton(R.string.mapnavigation_exit_nobutton, null)
		        .setPositiveButton(R.string.mapnavigation_exit_yesbutton, new DialogInterface.OnClickListener()  {

		            public void onClick(DialogInterface arg0, int arg1) {
		                MapNavigation.super.onBackPressed();
		            }
		        }).create().show();
		    	
		    }
		}
}