package ca.sajal.g2_g_app;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.animation.Animation;
import android.widget.ImageView;

public class splashscreen extends AppCompatActivity {
    AnimationDrawable anim;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        Thread thread = new Thread(){ //splashscreen using Threads
            public void run(){
                try{
                    ImageView splash = (ImageView) findViewById(R.id.splashimg); //Declare the image ID as an image
                    anim = (AnimationDrawable) splash.getDrawable(); //Load the animated image variable into the animation variable. the XML contains the location for the anim images
                    anim.start(); //Run the splashscreen animation. Since already linked splashscreen image with animation in above line, no need to link again, just run anim.start()
                    sleep(2000); // Keep the splashscreen for 2 seonds
                    Intent intent = new Intent(getApplicationContext(), homescreen.class); // AFter splashscreen, it'll go from splashscreen to MainActivity UI
                    startActivity(intent); // Starts the Intent to go to main page.
                    finish(); //After completing the splashscreen, destroys the program so it doesn't re-run
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        };
        thread.start();

    }
}
