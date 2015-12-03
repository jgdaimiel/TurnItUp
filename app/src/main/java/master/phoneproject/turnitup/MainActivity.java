package master.phoneproject.turnitup;

import java.util.ArrayList;

import master.phoneproject.turnitup.MusicService.MusicBinder;
import master.phoneproject.turnitup.SongAdapter.Wrapper;
import master.phoneproject.turnitup.utils.Song;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.content.ServiceConnection;

public class MainActivity extends Activity implements MediaPlayerControl {
	
								/************ATRIBUTOS**************/
	
	private ArrayList<Song> songList;
	private ListView songView;
	private MusicService musicService;
	private MusicController musicController;
	//Intent que usaremos para conectar la activity con el servicio.
	private Intent playIntent;
	//Atributo para el estado de la conexión con el servicio.
	private boolean musicBound = false;	
	//Atributo que representa la conexión con el servicio.
	private ServiceConnection musicConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			//Obtenemos el servicio, haciendo uso de la clase interna MusicBinder del servicio.
			MusicBinder binder = (MusicBinder)service;
			musicService = binder.getService();
			
			//Le pasamos la lista de canciones al servicio.
			musicService.setSongList(songList);
			
			//Conexión con el servicio establecida.
			musicBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			//Conexión con el servicio perdida.
			musicBound = false;
			Log.d("ServiceConnection", "onServiceDisconnected");
		}
	};
	
	
	
								/****************MÉTODOS****************/
	
	
	/**Métodos del ciclo de vida de la Activity**/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		 
		setContentView(R.layout.activity_main);

		//Obtenemos el componente ListView del layout. Inicializamos y obtenemos la lista de canciones.
		songView = (ListView)findViewById(R.id.songList);
		songList = new ArrayList<Song>();
		getSongList();
		
		//Instanciamos el adaptador y se lo asociamos a la ListView.
		SongAdapter adapter = new SongAdapter(this,songList);
		songView.setAdapter(adapter);
		//Inicializamos el MediaController
		setMusicController();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		//Cuando la aplicación vaya a ser destruida, cortamos la unión con el servicio,
		//al ser ésta la única activity unida al servicio, éste será eliminado también.
		if(musicBound){
			unbindService(musicConnection);
			musicBound = false;
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//Establecemos la unión entre la activity principal y el servicio.
		if(playIntent == null) {
			playIntent = new Intent(this, MusicService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
		}	
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	
	
	/**Métodos de la Action Bar**/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.		
		if(item.getItemId() == R.id.menu_shuffle){
			musicService.setShuffle(item);
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	
	
	
	/**Métodos de la clase**/
	
	/**
	 * Método que se ejecuta en el 'onClick' al pulsar una canción de la lista de canciones.
	 * @param view Representa la View del elemento de la lista que se ha pulsado (song_layout.xml).
	 */
	public void onClickSong(View view) {
		//Recuperamos el wrapper asociado al objeto Song en el que hemos pulsado
		//y le pasamos al servicio la información necesaria para poder reproducirla.
		Wrapper wrapper = (Wrapper)view.getTag();
		musicService.setSongIndex(wrapper.getIndex());
		musicService.playSong();
		musicController.show();
	}
	
	/**
	 * Método para instanciar e inicializar la clase que proporciona los controles del reproductor.
	 * Este método también registra los "listeners" para cuando hacemos 'click' en el botón siguiente canción y anterior canción.
	 */
	public void setMusicController() {
		musicController = new MusicController(this);
		musicController.setPrevNextListeners(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				playNext();
				
			}
		}, new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				playPrev();
				
			}
		});
		
		musicController.setMediaPlayer(this);
		musicController.setAnchorView(songView);
		musicController.setEnabled(true);
	}
	
	/**
	 * Método para leer las canciones de la memoria externa y almacenarlas en el atributo "songList".
	 */
	public void getSongList(){
		long id;
		String title;
		String artist;
		String[] columns = {
				android.provider.MediaStore.Audio.Media._ID,
				android.provider.MediaStore.Audio.Media.TITLE,
				android.provider.MediaStore.Audio.Media.ARTIST
				};
		
		//Obtenemos los datos de audio usando un ContentProvider y los guardamos en un objeto Cursor.
		ContentResolver songsResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor = songsResolver.query(musicUri, columns, null, null, null);
		
		if(cursor != null && cursor.moveToFirst()){
			//Obtenemos los índices de las columnas para acceder a los datos de las canciones
			int idIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
			int titleIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int artistIndex = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
			
			//Iteramos sobre el conjunto de resultados devueltos, creando una nueva canción y añadiéndola a la lista.
			do{
				id = cursor.getLong(idIndex);
				title = cursor.getString(titleIndex);
				artist = cursor.getString(artistIndex);
				songList.add(new Song(id,title,artist));
			}
			while(cursor.moveToNext());
		}
	}


	
	/**Métodos de la interfaz MediaPlayerControl**/
	
	@Override
	public void start() {
		musicService.start();
	}

	@Override
	public void pause() {
		musicService.pausePlayer();	
	}

	@Override
	public int getDuration() {
		return musicService.getDur();
	}

	@Override
	public int getCurrentPosition() {	
		return musicService.getPos();
	}

	@Override
	public void seekTo(int pos) {
		musicService.seek(pos);		
	}

	@Override
	public boolean isPlaying() {
		return musicService.isPlaying();
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		return 0;
	}
	
	
	
	/**Métodos para pasar de canción**/
	
	//Siguiente canción
	private void playNext() {
		musicService.playNext();
	}
	
	//Anterior canción
	private void playPrev() {
		musicService.playPrev();
	}
	
	
}
