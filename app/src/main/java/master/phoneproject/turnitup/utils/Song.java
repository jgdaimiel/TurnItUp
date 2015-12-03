package master.phoneproject.turnitup.utils;

/**
 * @author joaquin
 *
 */
public class Song {
	
	private long id;
	private String title;
	private String artist;
	

	public Song(long id, String title, String artist) {
		super();
		this.id = id;
		this.title = title;
		this.artist = artist;
	}


	public long getId() {
		return id;
	}


	public String getTitle() {
		return title;
	}


	public String getArtist() {
		return artist;
	}
		

}
