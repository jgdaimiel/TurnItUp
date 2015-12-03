package master.phoneproject.turnitup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import master.phoneproject.turnitup.utils.Song;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;


public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
														 MediaPlayer.OnErrorListener,
														 MediaPlayer.OnCompletionListener,
														 AudioManager.OnAudioFocusChangeListener{
							/****Atributos****/
	
	//Objeto MediaPlayer de Android, que representa el reproductor.
	private MediaPlayer player;
	private AudioManager audioManager;
	//Lista de canciones
	private ArrayList<Song> songList;
	//Posición de la canción actual
	private int songIndex;
	//Título de la canción actual
	private String songTitle;
	//Atributos para la opción de canción aleatoria
	private boolean shuffle;
	private Random random;
	private ArrayList<Integer> randomIndexList;
	//Atributo que representa la clase interna, que hace de Binder.
	private final IBinder musicBind = new MusicBinder();
	//El identificador de la notificación que usaremos.
	private static final int NOTIFY_ID=1;
	//Esta variable la usaremos para controlar las llamadas a getDuration() y getPosition() del MediaPlayer,
	//de manera que no se produzcan en estados inválidos de éste.
	private boolean prepared;
	
	
	
	
	
							/****Métodos****/
	
	
	/**Métodos de Service**/
	
	//Se crea el servicio inicializando el reproductor y otras variables de clase.
	public void onCreate() {
		super.onCreate();
		songIndex = 0;
		songTitle = "";
		shuffle = false;
		audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		initMusicPlayer();
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		//Cuando el servicio sea destruido paramos la ejecución en primer plano del servicio y eliminamos la notificación.
		stopForeground(true);		
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return musicBind;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		//Cuando de desconecten servicio y activity, se para el reproductor y se liberan los recursos.
		audioManager.abandonAudioFocus(this);
		player.stop();
		player.release();
		player = null;	
		return true;
	}


	
	/**Métodos de la clase**/
	
	
	/**Método para inicializar el objeto MusicPlayer, indicándole que mantenga la CPU activa aunque el sistema pase a reposo.
	*También le indicamos que el tipo de audio será música y registramos los listener para los distintos eventos.
	**/
	public void initMusicPlayer() {
		player = new MediaPlayer();
		player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.setOnErrorListener(this);
	}
	
	/**Setters para la lista de canciones y el índice de la canción**/
	public void setSongList(ArrayList<Song> songList) {
		this.songList = songList;
	}
	
	public void setSongIndex(int songIndex) {
		this.songIndex = songIndex;
	}
	
	/**
	 * Método para seleccionar y preparar la canción que se va a reproducir.
	 **/
	public void playSong() {
		Song currentSong;
		long currentSongId;
		Uri currentSongUri;
		
		if(player == null)
			initMusicPlayer();
		
		//reseteamos el MediaPlayer y lo dejamos en estado Idle
		player.reset();		
		prepared = false;
		
		//Selecionamos la canción usando el índice que nos pasó la activity cuando el usuario pulsó en una canción.
		currentSong = songList.get(songIndex);
		currentSongId = currentSong.getId();
		currentSongUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentSongId);
		
		//pasamos al estado Initialized con la llamada a setDataSource().
		try{
			player.setDataSource(getApplicationContext(), currentSongUri);
		}catch(Exception e){
			Log.e("MUSIC SERVICE", "Error setting data source", e);
		}	
		//guardamos el título para mostrarlo luego en la notificación.
		songTitle = currentSong.getTitle();
		
		
		//pasamos al estado Prepared de manera síncrona
		//el motivo es que cuando la activity principal llame a musicController.show(), éste se ejecute con el MediaPlayer en estado Prepared.
		//Si lo hicieramos mediante prepareAsync, devolvería la ejecución inmediatamente, y se ejecutaría la llamada a show() del MediaController.
		//show() internamente llama a getDuration() y getCurrentPosition().
		//Lo ideal quizá sería llamada asíncrona para no bloquear la interfaz, ya que activity y service comparten thread.
		//Aún así la llamada es rápida y no se muestra retraso.
		try {
			player.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Método para conmutar el estado de canción aleatoria.
	 **/
	public void setShuffle(MenuItem item) {
		//Si está activado tenemos que desactivarlo.
		if(shuffle){
			item.setIcon(R.drawable.ic_shuffle_off);
			Toast.makeText(this, R.string.shuffle_off, Toast.LENGTH_SHORT).show();			
			shuffle = false;
		}
		//Si está desactivado lo activamos.
		else{
			item.setIcon(R.drawable.ic_shuffle_on);
			Toast.makeText(this, R.string.shuffle_on, Toast.LENGTH_SHORT).show();				
			shuffle = true;		
			initRandomIndexList(songList.size());
		}		
	}	
	
	/**
	 * Método para inicializar y desordenar la lista que contiene los índices de las canciones.
	 * Usaremos estos índices para reproducir la "siguiente canción" en modo aleatorio.
	 * @param size Tamaño de la lista de índices.
	 */
	private void initRandomIndexList(int size) {
		random = new Random(System.nanoTime());
		randomIndexList = new ArrayList<Integer>(size);			
		for(int i=0;i!=size;i++){
			randomIndexList.add(i);
		}
		Collections.shuffle(randomIndexList, random);
	}

	
	
	
	/**Métodos listeners de MediaPlayer**/
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		playNext();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}

	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		Intent notificationIntent;
		PendingIntent pendIntent;
		Notification.Builder notificationBuilder;
		Notification notification;
		int af;
		
		// Primero pedimos el Audio Focus, si se nos concede pasamos a reproducir el audio.
		af = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		
		if(af == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
			//Reproducimos la canción pasando al estado Started.
			prepared = true;
			mediaPlayer.start();
			
			//Creamos una notificación que permanecerá activa mientras reproduzcamos música.
			notificationIntent = new Intent(getApplicationContext(),MainActivity.class);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			pendIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			notificationBuilder = new Notification.Builder(this);		
			notificationBuilder.setContentIntent(pendIntent);
			notificationBuilder.setSmallIcon(R.drawable.ic_play);
			notificationBuilder.setTicker(songTitle);
			notificationBuilder.setOngoing(true);
			notificationBuilder.setContentTitle("Turn It Up!");
			notificationBuilder.setContentText(songTitle);
			
			notification = notificationBuilder.build();
			
			//Ejecutamos el servicio en primer plano, de manera que el sistema no lo cierre cuando necesite memoria,
			//esto interrumpiría la reproducción de audio y el usuario lo notaría enseguida.
			startForeground(NOTIFY_ID, notification);
		}
	}
	
	
	
	/**Métodos para el control del reproductor**/
	
	public int getPos() {
		if(player != null && prepared)
			return player.getCurrentPosition();
		else 
			return 0;
	}
	
	public int getDur() {
		if(player != null && prepared)
			return player.getDuration();
		else 
			return 0;
	}
	
	public boolean isPlaying() {
		if(player != null)
			return player.isPlaying();
		else
			return false;
	}
	
	public void pausePlayer() {
		if(player != null)
			player.pause();
	}
	
	public void seek(int pos) {
		if(player != null)
			player.seekTo(pos);
	}
	
	public void start() {
		if(player != null)
			player.start();
	}
	
	public void playPrev() {
		songIndex--;
		if(songIndex >= 0){
			playSong();
		}	
	}
	
	public void playNext() {	
		//La canción aleatoria la obtenemos de la lista de índices desordenada.
		//Al mismo tiempo la borramos de la lista para que no vuelva a salir.
		if(shuffle && !randomIndexList.isEmpty()){
			songIndex = randomIndexList.remove(randomIndexList.size()-1);
			playSong();
		}
		else if(!shuffle && songIndex < songList.size()-1){
			songIndex++;
			playSong();
		}
		else{
			//Una vez reproducidas todas las canciones, regeneramos la lista aleatoria
			//por si el usuario quiere volver a reproducirlas en modo aleatorio.
			//Acto seguido paramos el reproductor y liberamos el AudioFocus.
			initRandomIndexList(songList.size());
			if(player != null)
				player.stop();
			audioManager.abandonAudioFocus(this);
		}
	}

	
	
	/**
	 * Método para gestión de cambios de AudioFocus.
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch(focusChange){
			case AudioManager.AUDIOFOCUS_GAIN:
				if(player == null)
					initMusicPlayer();
				else if(!player.isPlaying())
					player.start();
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				if(player.isPlaying())
					player.stop();
				player.release();
				player = null;
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				if(player.isPlaying())
					player.pause();
				break;
		}
	}

	
	/**
	 * Clase que hereda de Binder y que nos permite pasar el servicio a la activity principal, 
	 * esto nos ayudará a crear la unión entre ambos.
	 * @author joaquin
	 */
	public class MusicBinder extends Binder {
		MusicService getService() {
			return MusicService.this;
		}
	}
}
