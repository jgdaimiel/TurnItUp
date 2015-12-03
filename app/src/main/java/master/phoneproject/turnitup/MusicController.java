package master.phoneproject.turnitup;

import android.content.Context;
import android.widget.MediaController;

/**Usaremos esta clase que hereda de MediaController para sobreescribir el método hide() con contenido vacío,
 * así impedimos que se oculten los controles del reproductor cuando esté en pausa.**/
public class MusicController extends MediaController {
	
	public MusicController(Context c) {
		super(c);
	}
	
	public void hide() {}
}
