package master.phoneproject.turnitup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import master.phoneproject.turnitup.utils.Song;

public class SongAdapter extends BaseAdapter {
	
	private ArrayList<Song> songList;
	private LayoutInflater songInflater;
	
	public SongAdapter(Context context, ArrayList<Song> songList){
		super();
		this.songList = songList;
		songInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return songList.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		//Si el convertView es nulo lo inicializamos, si no lo reutilizamos.
		if(convertView == null){
			System.out.println("convertView == null");
			convertView = songInflater.inflate(R.layout.song_layout, parent, false);
			Wrapper wrapper = new Wrapper(convertView);
			convertView.setTag(wrapper);
		}
		
		Wrapper wrapper = (Wrapper)convertView.getTag();
		Song currentSong = songList.get(position);
		
		wrapper.getTitle().setText(currentSong.getTitle());
		wrapper.getArtist().setText(currentSong.getArtist());
		wrapper.setIndex(position);
		
		return convertView;
	}

	@Override
	public Object getItem(int position) {
		return songList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}
	
	
	/**
	 * Para aumentar la eficiencia del Adapter tenemos la clase Wrapper.
	 * Esta clase cachea las distintas views pertenecientes a cada item de la lista de canciones (que se compone de dos TextViews),
     * para no tener que llamar a 'findViewById', que es más costoso, en cada llamada a getView().
	 * @author joaquin
	 */
	public class Wrapper {
		private View baseView;
		private TextView title;
		private TextView artist;
		private int index;
		
		public Wrapper(View baseView) {
			super();
			this.baseView = baseView;
		}

		public TextView getTitle() {
			//se llama a findViewById() la primera vez, cuando la view es nula
			//para las siquientes llamadas se reutiliza.
			if(title == null)
				title = (TextView)baseView.findViewById(R.id.songTitle);
				
			return title;
		}

		public TextView getArtist() {
			if(artist == null)
				artist = (TextView)baseView.findViewById(R.id.songArtist);
			
			return artist;
		}
		
		public void setIndex(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return index;
		}
	}

}
