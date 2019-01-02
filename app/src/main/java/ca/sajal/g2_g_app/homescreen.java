package ca.sajal.g2_g_app;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class homescreen extends AppCompatActivity {
    Animation fromtop;
    ImageButton g2button;
    ImageButton g1button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homescreen);

        //FirebaseDatabase database = FirebaseDatabase.getInstance(); // Calling database variable
        //DatabaseReference myRef = database.getReference("message"); // declaring it as a message type

        //myRef.setValue("Hello, World!"); //displaying the message in database terminal

        g2button = (ImageButton) findViewById(R.id.g2level); //load the imagebutton as a imagebutton variable
        g1button = (ImageButton) findViewById(R.id.g1level);
        // fromtop = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.slideup); //set animation variable to the animation XML class(slideup) with context to this class
        // g2button.startAnimation(fromtop); Run the animation for the button. Different from anim.start() or anim_name.start() because in those cases, the button(refer to splashscreen.java) was set to the
        // animation but here it is not so have to attach it to the button using the line above. The animation was only loaded to "fromtop" animation variable.

        final AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.7F); //AlphaAnimation can be used as a fader. In this case, it will be used to fade a button when clicked.

        TranslateAnimation animate = new TranslateAnimation(8, 8, -155, 150);  /* Instead of using custom animation shown above, more efficient to use built in android animation
        If no built in android animation, then better to create own XML and use AnimationUtils to create own animation. Otherwise, best to use built in animation classes.
        Check out more on android dev or medium website. */
        animate.setDuration(3000);
        animate.setFillAfter(true);
        g2button.startAnimation(animate); /* Used to run the animation for the button only. The button wasn't previously set for any animations so the animate variable would have no use & not run.
        By setting the animation variable to start with the g2button using the above line of code, it links the sliding animation to the button. */

        g1button.setOnClickListener(new View.OnClickListener() { //View.OnClickListener detects if button has been pressed
            @SuppressLint("PrivateApi")
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick); // When the g1button is pressed, the AlphaAnimation runs simulating button click effect by fading
                Intent g1main = new Intent(getApplicationContext(), firebasedata.class); //previously glevelmain.class when included buttons
                startActivity(g1main);
            }
        });

        g2button.setOnClickListener(new View.OnClickListener() { //View.OnClickListener detects if button has been pressed
            @SuppressLint("PrivateApi")
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick); // When the g2button is pressed, the AlphaAnimation runs simulating button click effect by fading
                Intent g2main = new Intent(getApplicationContext(), g2levelmain.class);
                startActivity(g2main);
            }
        });

    }

}

