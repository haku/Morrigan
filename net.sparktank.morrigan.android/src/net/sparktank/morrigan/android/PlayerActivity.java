package net.sparktank.morrigan.android;

import net.sparktank.morrigan.android.model.ServerReference;
import net.sparktank.morrigan.android.model.impl.ServerReferenceImpl;
import net.sparktank.morrigan.android.tasks.PlaypauseTask;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class PlayerActivity extends Activity {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	ServerReference serverReference = new ServerReferenceImpl(TempConstants.serverUrl);
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Activity methods.
	
    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        hookUpButtons();
    }
    
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Buttons.
    
    private void hookUpButtons () {
    	Button cmd;
    	
    	cmd = (Button) findViewById(R.id.btnPlaypause);
    	cmd.setOnClickListener(new BtnPlaypause_OnClick());
    	
    	cmd = (Button) findViewById(R.id.btnNext);
    	cmd.setOnClickListener(new BtnNext_OnClick());
    }
    
    class BtnPlaypause_OnClick implements OnClickListener {
		@Override
		public void onClick(View v) {
			// Toast.makeText(getApplicationContext(), "TODO: Playpause.", Toast.LENGTH_LONG).show();
			
			PlaypauseTask playpauseTask = new PlaypauseTask(PlayerActivity.this, PlayerActivity.this.serverReference);
			playpauseTask.execute();
		}
    }
    
    class BtnNext_OnClick implements OnClickListener {
    	@Override
    	public void onClick(View v) {
    		Toast.makeText(getApplicationContext(), "TODO: Next.", Toast.LENGTH_LONG).show();
    	}
    }
    
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
